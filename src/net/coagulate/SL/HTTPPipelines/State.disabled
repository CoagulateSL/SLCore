package net.coagulate.SL.HTTPPipelines;
import java.util.HashMap;
import java.util.Map;
import static java.util.logging.Level.WARNING;
import net.coagulate.SL.Data.Session;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.SL;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.*;

/**
 *
 * @author Iain Price
 */
@SuppressWarnings("deprecation")
public class State {
    boolean page_firstinput=true;
    int returnstatus=HttpStatus.SC_OK; public void setStatus(int a) { returnstatus=a; }
    public String getClientIP() {
        /*
        try {
        HttpInetConnection connection = (HttpInetConnection) httpcontext.getAttribute(ExecutionContext.HTTP_CONNECTION);
        InetAddress ia = connection.getRemoteAddress();
        return ia.getCanonicalHostName()+" / "+ia.getHostAddress();
        } catch (Exception e) { SL.getLogger().log(WARNING,"Exception getting client address",e); }*/
        Header[] headers = request.getHeaders("X-Forwarded-For");
        if (headers.length==0) { SL.getLogger().log(WARNING,"Zero X-Forwarded-For headers"); return "UNKNOWN"; }
        if (headers.length>1) { SL.getLogger().log(WARNING,"More than one X-Forwarded-For header?"); }
        return headers[0].getValue();
    }
    HttpRequest request; //public HttpRequest request() {  return request; }
    HttpResponse response; 
    HttpContext httpcontext;
    private Map<String, String> parameters=new HashMap<>(); public String get(String parameter) { String v=parameters.get(parameter); if (v==null) { v=""; } return v; }
    public void put(String parameter,String value) { parameters.put(parameter,value); }
    Map<String, String> cookies;
    String sessionid;
    String page;

    void loadSession() {
        session=null;
        if (sessionid!=null) { session=Session.get(sessionid); } 
        if (session!=null) { user=session.user(); } else { sessionid=null; }
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

    void setSessionId(String sessionid) {
        if (sessionid!=null && sessionid.isEmpty()) { sessionid=null; }
        if (sessionid!=null && sessionid.equalsIgnoreCase("none")) { sessionid=null; }
        this.sessionid=sessionid;
        loadSession();
    }

    void put(Map<String, String> passed) {
        parameters.putAll(passed);
    }

    Map<String, String> getParameters() {
        return parameters;
    }

}
