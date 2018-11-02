package net.coagulate.SL.Data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.coagulate.Core.Database.LockException;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.Passwords;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.Tokens;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.SL.Config;
import net.coagulate.SL.HTTPPipelines.State;
import net.coagulate.SL.Pricing;
import net.coagulate.SL.SL;

/**
 *
 * @author Iain Price
 */
public class User extends LockableTable {


    String username;

    public String getUsername() { return username; }
    @Override
    public String getTableName() { return "users"; }
    @Override
    public String toString() { return getUsername()+"#"+getId(); }


    public boolean superuser() { return getId()==1 && username.equalsIgnoreCase("Iain Maltz"); }

    private static final Map<Integer,User> users=new HashMap<>();
    
    public static String formatUsername(String username) {
        String original=username;
        // possible formats
        // user.resident
        // user resident
        // user
        // and also we want to formalise case
        username=username.replaceAll("\\."," "); // merge "user.resident" and "user resident"
        // for simplicity, force to "double barrel"
        if (!username.contains(" ")) { username=username+" Resident"; } // merge "user" and "user resident"
        String[] parts=username.split(" ");
        if (parts.length!=2) {
            throw new SystemException("Formatting username '"+original+"' gave '"+username+"' which has "+parts.length+" parts, which is not 2...");
        }
        // convert to "uppercase-first"
        String firstname=parts[0].toLowerCase();
        String lastname=parts[1].toLowerCase();
        firstname=firstname.substring(0,1).toUpperCase()+firstname.substring(1);
        lastname=lastname.substring(0,1).toUpperCase()+lastname.substring(1);
        // only append the surname if /not/ resident
        String output=firstname;
        if (!lastname.equalsIgnoreCase("Resident")) { output=output+" "+lastname; } // redundant ignores case
        return output;
    }
    
    private User(int id,String username) { super(id); this.username=username;}
    
    private static User factory(int id,String username) {
        synchronized(users) {
            if (users.containsKey(id)) { return users.get(id); }
            User u=new User(id,username);
            users.put(id,u);
            return u;
        }
    }
    public static User get(String username,boolean createifnecessary) {
        username=formatUsername(username);
        boolean mandatory=true;
        if (createifnecessary) { mandatory=false; }
        Integer id=SL.getDB().dqi(mandatory,"select id from users where username=?",username);
        if (id!=null) { return factory(id,username); }
        SL.getDB().d("insert into users(username) values(?)",username);
        id=SL.getDB().dqi(false,"select id from users where username=?",username);
        if (id!=null) { return factory(id,username); }
        throw new SystemException("Created user for "+username+" and then couldn't find its id");
    }
    public static User get(int id) {
        String username=SL.getDB().dqs(true,"select username from users where id=?",id);
        return factory(id,username);
    }
    
    public static User get(String username) { return get(username,false); }

    public static User getDeveloperKey(String key) { 
        if (key==null || key.equals("")) {
            return null;
        }
        Integer userid=SL.getDB().dqi(false,"select id from users where developerkey=?",key);
        if (userid==null) { return null; }
        return get(userid);
    }

    public String getDeveloperKey() {
        return dqs(true,"select developerkey from users where id=?",getId());
    }
    
    public boolean isSuperAdmin() {
        Integer isadmin=dqi(true,"select superadmin from users where id=?",getId());
        return isadmin==1;
    }


    
    
    
    
    public String generateSSO() {
        String token=Tokens.generateToken();
        int expires=UnixTime.getUnixTime()+Config.SSOWINDOWSECONDS;
        d("update users set ssotoken=?,ssoexpires=? where "+getIdColumn()+"=?",token,expires,getId());
        return token;
    }
    
    public static User getSSO(String token) {
        // purge old tokens
        SL.getDB().d("update users set ssotoken=null,ssoexpires=null where ssoexpires<?",UnixTime.getUnixTime());
        Integer match=SL.getDB().dqi(false,"select id from users where ssotoken=?",token);
        if (match==null) { return null; }
        SL.getDB().d("update users set ssotoken=null,ssoexpires=null where id=?",match);
        return get(match);
    }

    public void setPassword(String password) throws UserException {
        if (password.length()<6) { throw new UserException("Password not long enough"); }
        d("update users set password=? where id=?",Passwords.createHash(password),getId());
        SL.getLogger().info("User "+this.getUsername()+" has set password from "+State.get().getClientIP());
    }

    public boolean checkPassword(String password) {
        String hash=dqs(true,"select password from users where id=?",getId());
        if (hash==null || hash.isEmpty()) { GPHUD.getLogger().warning("Attempt to log in with a null or empty password hash for user "+getUsername()); return false; }        
        return Passwords.verifyPassword(password,hash);
    }

    public int balance() {
        Integer balance=dqi(false, "select sum(ammount) from journal where userid=?",getId());
        if (balance==null) { return 0; }
        return balance;
    }
    public void bill(int ammount,String description) throws UserException {
        int serial;
        try {serial=lock();} catch (LockException e) { throw new UserException("Your balance is currently being updated elsewhere, please retry in a moment"); }
        try {
            int balance=balance();
            if (balance<ammount) { throw new UserException("Insufficient balance (L$"+balance+") to pay charge L$"+ammount); }
            d("insert into journal(tds,userid,ammount,description) values(?,?,?,?)",UnixTime.getUnixTime(),getId(),-ammount,description);
        }
        finally { unlock(serial); }
    }

    public Set<Subscription> getSubscriptions(Pricing.SERVICE service,boolean activeonly,boolean paidonly) {
        Results res;
        int paiduntilfilter=UnixTime.getUnixTime();
        if (paidonly==false) { paiduntilfilter=0; }
        String activeonlysql;
        if (activeonly) { activeonlysql=" and active=1"; } else { activeonlysql=""; }
        if (service!=null) {
            res=dq("select id from subscriptions where ownerid=? and servicetype=? and paiduntil>?"+activeonlysql,getId(),service.getValue(),paiduntilfilter);
        } else {
            res=dq("select id from subscriptions where ownerid=? and paiduntil>?"+activeonlysql,getId(),paiduntilfilter);
        }
        Set<Subscription> subs=new HashSet<>();
        for (ResultsRow r:res) {
            subs.add(new Subscription(r.getInt("id")));
        }
        return subs;
    }


   
    
}