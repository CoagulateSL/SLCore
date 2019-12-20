package net.coagulate.SL.HTTPPipelines;

import net.coagulate.SL.Data.Session;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.HTTPPipelines.PageMapper.Prefix;
import net.coagulate.SL.SL;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import javax.annotation.Nonnull;

import static java.util.logging.Level.SEVERE;

/**
 * @author Iain Price
 */
public class SSOExchange implements HttpRequestHandler {
	@Prefix("/SSO/")
	public SSOExchange() {super();}

	@Override
	public void handle(@Nonnull final HttpRequest req,
	                   @Nonnull final HttpResponse resp,
	                   final HttpContext hc) {
		try {

			final String token=req.getRequestLine().getUri().replaceFirst("/SSO/","");
			final User user=User.getSSO(token);
			if (user==null) {
				SL.getLogger().warning("SSO Exchange of token failed to return a valid user.");
				resp.addHeader("Location","/");
				resp.setStatusCode(HttpStatus.SC_SEE_OTHER);
				return;
			}
			SL.getLogger().info("Successful SSO signon for "+user);
			final Session session=Session.create(user);
			resp.setEntity(new StringEntity(""));
			resp.addHeader("Set-Cookie","coagulateslsessionid="+session.token()+"; HttpOnly; Path=/; Secure;");
			resp.addHeader("Location","/");
			resp.setStatusCode(HttpStatus.SC_SEE_OTHER);
		}
		catch (@Nonnull final Exception ex) {
			SL.getLogger().log(SEVERE,"SSO?",ex);
			resp.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			resp.setEntity(new StringEntity("<html><body><pre><b>500 - Internal Server Error</b></pre><p>Internal Exception, see debug logs</p></body></html>",
			                                ContentType.TEXT_HTML
			));
		}
	}

	@Nonnull
	@Override
	public String toString() { return "SSOExchange"; }

}
