package net.coagulate.SL.HTTPPipelines;

import java.util.Map;
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
    
}
