package net.coagulate.SL.HTTPPipelines;

import net.coagulate.SL.Data.User;
import net.coagulate.SL.Log;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

/**
 *
 * @author Iain Price
 */
public class SSOExchange implements HttpRequestHandler {
   @Override
    public void handle(HttpRequest req, HttpResponse resp, HttpContext hc) {
        try {
            
            String token=req.getRequestLine().getUri().replaceFirst("/SSO/","");
            User user=User.getSSO(token);
            if (user==null) {
                Log.note(this, "SSO Exchange of token failed to return a valid user.");
                resp.addHeader("Location","/");
                resp.setStatusCode(HttpStatus.SC_SEE_OTHER);
                return;
            }
            Log.debug(this,"Successful SSO signon for "+user.toString());
            /*resp.setEntity(new StringEntity(""));
            resp.addHeader("Set-Cookie","coagulateslsessionid="+state.sessionid+"; HttpOnly; Path=/; Domain=coagulate.net; Secure;");
            resp.addHeader("Location","/");
            resp.setStatusCode(HttpStatus.SC_SEE_OTHER);*/
            return;
        } catch (Exception ex) {
            Log.warn("StringHandler","Unexpected exception thrown in page handler",ex);
            resp.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            resp.setEntity(new StringEntity("<html><body><pre><b>500 - Internal Server Error</b></pre><p>Internal Exception, see debug logs</p></body></html>",ContentType.TEXT_HTML));
            return;
        }
    }    

    protected String handleString(State state) {
        return "Hello I'm the SSO Exchange";
    }
    
    protected String header() {
        return "<html><head><title>Coagulate SL Services</title></head><body>"
                + "<h1 align=center>Coagulate SL Services</h1><p><hr>"
                + "Hello<span style='display:block;float:right;'>There</span>"
                + "<hr></p>";
    }
    protected String footer() {
        return "<div style='position:absolute;bottom:5;right:5;left:5;'><hr><span style='display:block;float:right;'>(C) Iain Maltz @ Second Life</span></div></body></html>";
    }
    
   @Override
    public String toString() { return "SSOExchange"; }
    
}
