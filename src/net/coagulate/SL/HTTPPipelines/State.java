package net.coagulate.SL.HTTPPipelines;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.coagulate.SL.Data.Session;
import net.coagulate.SL.Data.User;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
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
    HttpRequest request;
    HttpResponse response;
    HttpContext httpcontext;
    Map<String, String> parameters;
    Map<String, String> cookies;
    String sessionid;
    String page;

    void loadSession() {
        session=null;
        if (sessionid!=null) { session=Session.get(sessionid); } 
        if (session!=null) { user=session.user(); }
    }
    Session session=null;
    User user=null;
}
