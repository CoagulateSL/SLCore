package net.coagulate.SL.HTTPPipelines;

import java.util.Map;
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

    HttpRequest request;
    HttpResponse response;
    HttpContext httpcontext;
    Map<String, String> parameters;
    Map<String, String> cookies;
    String sessionid;

    void loadSession() {
        session=null;
        if (sessionid!=null) { session=Session.get(sessionid); } 
        if (session!=null) { user=session.user(); }
    }
    Session session=null;
    User user=null;
}
