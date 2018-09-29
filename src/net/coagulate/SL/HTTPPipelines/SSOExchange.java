package net.coagulate.SL.HTTPPipelines;

import static java.util.logging.Level.SEVERE;
import net.coagulate.SL.Data.Session;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.SL;
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
                SL.getLogger().warning("SSO Exchange of token failed to return a valid user.");
                resp.addHeader("Location","/");
                resp.setStatusCode(HttpStatus.SC_SEE_OTHER);
                return;
            }
            SL.getLogger().info("Successful SSO signon for "+user.toString());
            Session session=Session.create(user);
            resp.setEntity(new StringEntity(""));
            resp.addHeader("Set-Cookie","coagulateslsessionid="+session.token()+"; HttpOnly; Path=/; Domain=coagulate.net; Secure;");
            resp.addHeader("Location","/");
            resp.setStatusCode(HttpStatus.SC_SEE_OTHER);
            return;
        } catch (Exception ex) {
            SL.getLogger().log(SEVERE,"Unexpected exception thrown in SSO page handler",ex);
            resp.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            resp.setEntity(new StringEntity("<html><body><pre><b>500 - Internal Server Error</b></pre><p>Internal Exception, see debug logs</p></body></html>",ContentType.TEXT_HTML));
            return;
        }
    }    
   @Override
    public String toString() { return "SSOExchange"; }
    
}
