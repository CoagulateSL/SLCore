package net.coagulate.SL.HTTPPipelines;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.coagulate.JSLBot.Log;
import net.coagulate.SL.Data.Session;
import net.coagulate.SL.Data.User;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

/**
 *
 * @author Iain Price
 */
public class State {

    // hmm
    private static final Map<Thread,State> threadstate=new HashMap<>();
    public static State create() {
        synchronized(threadstate) { 
            State s=new State();
            threadstate.put(Thread.currentThread(), s);
            return s;
        }
    }
    public static State get() {
        synchronized(threadstate) { 
            State s=threadstate.get(Thread.currentThread());
            if (s==null) {
                s=new State();
                threadstate.put(Thread.currentThread(),s);
            }
            return s;
        }
    }
    public static void cleanup() {
        Set<Thread> removeme=new HashSet<>();
        synchronized(threadstate) {
            for (Thread t:threadstate.keySet()) {
                if (!t.isAlive()) { removeme.add(t); }
            }
            for (Thread t:removeme) { threadstate.remove(t); }
        }
    }
    public static void destroy() { synchronized(threadstate) { threadstate.remove(Thread.currentThread()); } }
    public String getClientIP() {
        try { 
            HttpInetConnection connection = (HttpInetConnection) httpcontext.getAttribute(ExecutionContext.HTTP_CONNECTION);
            InetAddress ia = connection.getRemoteAddress();        
            return ia.getCanonicalHostName()+" / "+ia.getHostAddress();
        } catch (Exception e) { Log.note("State","Exception getting client address",e); }
        return "UNKNOWN";
    }
    HttpRequest request; //public HttpRequest request() {  return request; }
    HttpResponse response; 
    HttpContext httpcontext;
    Map<String, String> parameters; public String get(String parameter) { String v=parameters.get(parameter); if (v==null) { v=""; } return v; }
    Map<String, String> cookies;
    String sessionid;
    String page;

    void loadSession() {
        session=null;
        if (sessionid!=null) { session=Session.get(sessionid); } 
        if (session!=null) { user=session.user(); }
    }
    Session session=null;
    private User user=null; public User user() { return user; }
    public void user(User user) { session=Session.create(user); this.sessionid=session.token(); this.user=user; }

    public void logout() {
        if (session!=null) {
            session.logout();
        }
        session=null;
        sessionid=null;
        user=null;
    }

}
