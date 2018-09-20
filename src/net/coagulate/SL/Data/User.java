package net.coagulate.SL.Data;

import java.util.HashMap;
import java.util.Map;
import net.coagulate.JSLBot.Log;
import net.coagulate.SL.Config;
import net.coagulate.SL.Database.Database;
import net.coagulate.SL.HTTPPipelines.State;
import net.coagulate.SL.SystemException;
import net.coagulate.SL.Tools;
import net.coagulate.SL.UserException;

/**
 *
 * @author Iain Price
 */
public class User {

    int id;
    String username;

    public String getUsername() { return username; }
    public int getId() { return id; }
    @Override
    public String toString() { return getUsername()+"#"+getId(); }
    public boolean superuser() { if (id==1 && username.equalsIgnoreCase("Iain Maltz")) { return true; } return false; }

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
    
    private User(int id,String username) { this.id=id; this.username=username;}
    
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
        System.out.println("Get user:"+username);
        boolean mandatory=true;
        if (createifnecessary) { mandatory=false; }
        Integer id=Database.dqi(mandatory,"select id from users where username=?",username);
        if (id!=null) { return factory(id,username); }
        Database.d("insert into users(username) values(?)",username);
        id=Database.dqi(false,"select id from users where username=?",username);
        if (id!=null) { return factory(id,username); }
        throw new SystemException("Created user for "+username+" and then couldn't find its id");
    }
    public static User get(int id) {
        String username=Database.dqs(true,"select username from users where id=?",id);
        return factory(id,username);
    }
    
    public static User get(String username) { return get(username,false); }

    
    
    
    
    
    
    public String generateSSO() {
        String token=Tools.generateToken();
        int expires=Tools.getUnixTime()+Config.SSOWINDOWSECONDS;
        Database.d("update users set ssotoken=?,ssoexpires=? where id=?",token,expires,id);
        return token;
    }
    
    public static User getSSO(String token) {
        // purge old tokens
        Database.d("update users set ssotoken=null,ssoexpires=null where ssoexpires<?",Tools.getUnixTime());
        Integer match=Database.dqi(false,"select id from users where ssotoken=?",token);
        if (match==null) { return null; }
        Database.d("update users set ssotoken=null,ssoexpires=null where id=?",match);
        return get(match);
    }

    public void setPassword(String password) {
        if (password.length()<6) { throw new UserException("Password not long enough"); }
        Database.d("update users set password=? where id=?",Tools.createHash(password),id);
        Log.note(this,"User has set password from "+State.get().getClientIP());
    }

    public boolean checkPassword(String password) {
        String hash=Database.dqs(true,"select password from users where id=?",id);
        return Tools.verifyPassword(password,hash);
    }
    
    
}