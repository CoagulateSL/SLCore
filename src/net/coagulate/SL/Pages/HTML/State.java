package net.coagulate.SL.Pages.HTML;
import java.util.HashMap;
import java.util.Map;
import net.coagulate.SL.Data.Session;
import net.coagulate.SL.Data.User;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.*;

/** General purpose state storage
 *
 * @author Iain Price
 */
@SuppressWarnings("deprecation")
public class State {
    // We love HTTP :P
    public State(HttpRequest request,HttpResponse response,HttpContext httpcontext) {
        this.request=request;
        this.response=response;
        this.httpcontext=httpcontext;
    }
    private final HttpRequest request; public HttpRequest request() {  return request; }
    private final HttpResponse response; public HttpResponse response() { return response; }
    private final HttpContext httpcontext; public HttpContext context() { return httpcontext; }
    private int returnstatus=HttpStatus.SC_OK; public void status(int httpstatus) { returnstatus=httpstatus; } public int status() { return returnstatus; }
    private String sessionid=null;
    public void sessionId(String set) { sessionid=set; loadSession(); }
    public String sessionId() { return sessionid; }
    
    // We love general purpose KV maps.  more than one of them.
    private final Map<String,Map<String,String>> maps=new HashMap<>();
    
    private Map<String,String> getMap(String mapname) {
        if (!maps.containsKey(mapname)) {
            maps.put(mapname,new HashMap<>());
        }
        return maps.get(mapname);
    }
    public String get(String mapname,String key,String defaultvalue) {
        if (!getMap(mapname).containsKey(key)) { return defaultvalue; }
        return getMap(mapname).get(key);
    }
    public String get(String mapname,String key) { return get(mapname,key,null); }
    public void delete(String mapname,String key) { getMap(mapname).remove(key); }
    public void put(String mapname,String key,String value) { getMap(mapname).put(key,value); }
    public void putMap(String mapname,Map<String,String> map) { maps.put(mapname,map); }
    
    
    // WELL KNOWN MAPS
    // parameters - get/post data
    // cookies - header cookie data
    // parameters is assumed to be default :P legacy behaviour, kinda
    public String get(String key) { return get("parameters",key,""); }
    public void put(String key,String value) { put("parameters",key,value); }
    
    // hmm, getting a bit complex here
    private User user=null;
    public User user() { return user; }
    public void user(User user) { this.user=user; }

    public String getClientIP() {
        //try {
        //HttpInetConnection connection = (HttpInetConnection) httpcontext.getAttribute(ExecutionContext.HTTP_CONNECTION);
        //InetAddress ia = connection.getRemoteAddress();
        //return ia.getCanonicalHostName()+" / "+ia.getHostAddress();
        //} catch (Exception e) { SL.getLogger().log(WARNING,"Exception getting client address",e); }
        Header[] headers = request.getHeaders("X-Forwarded-For");
        if (headers.length==0) { return "UNKNOWN"; }
        if (headers.length>1) { return "MULTIPLE?:"+headers[0].getValue(); }
        return headers[0].getValue();
    }
    public void logout() {
        if (sessionid!=null) {
            Session.get(sessionid).logout();
        }
        sessionid=null;
        user=null;
    }

    private Session session=null;
    
    void loadSession() {
        session = null;
        if (sessionid==null || sessionid.isEmpty() || sessionid.equalsIgnoreCase("none")) { return; }
        session=Session.get(sessionid);
        System.out.println("Loaded session id "+sessionid+" and got "+session);
        if (session!=null) { user=session.user(); } else { sessionid=null; }
    }
}
