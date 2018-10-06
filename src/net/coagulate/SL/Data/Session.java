package net.coagulate.SL.Data;

import net.coagulate.Core.Tokens;
import net.coagulate.Core.UnixTime;
import net.coagulate.SL.Config;
import net.coagulate.SL.Database.Database;
import net.coagulate.SL.Database.Row;

/**
 *
 * @author Iain Price
 */
public class Session {

    private final String id; public String token() { return id; }
    private final User user; public User user() { return user; }
    private Session(String sessionid,User user) { this.id=sessionid; this.user=user; }
    
    public static Session get(String sessionid) {
        Row user=Database.dqone(false,"select userid,expires from sessions where cookie=? and expires>?",sessionid,UnixTime.getUnixTime());
        if (user==null) {
            //Log.note("Session","Invalid session id presented");
            return null;
        }
        int userid=user.getInt("userid");
        int expires=user.getInt("expires");
        int expiresin=expires-UnixTime.getUnixTime();
        if (expiresin<(Config.SESSIONLIFESPANSECONDS/2)) {
            // refresh
            Database.d("update sessions set expires=? where cookie=?",UnixTime.getUnixTime()+Config.SESSIONLIFESPANSECONDS,sessionid);
        }
        return new Session(sessionid,User.get(userid));
    }
    
    public static Session create(User user) {
        // we abuse this "pipeline" to purge old sessions
        try { Database.d("delete from sessions where expires<?",UnixTime.getUnixTime()); } catch (Exception e) {}
        int userid=user.getId();
        String sessionid=Tokens.generateToken();
        int expires=UnixTime.getUnixTime()+Config.SESSIONLIFESPANSECONDS;
        Database.d("insert into sessions(cookie,userid,expires) values(?,?,?)",sessionid,userid,expires);
        return new Session(sessionid,user);
    }

    public void setUser(User user) {
        Database.d("update sessions set userid=? where cookie=?",user.getId(),token());
    }

    public void logout() {
        Database.d("delete from sessions where cookie=?",token());
    }
}