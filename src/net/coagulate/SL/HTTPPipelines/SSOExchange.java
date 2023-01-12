package net.coagulate.SL.HTTPPipelines;

import net.coagulate.Core.HTML.Elements.Preformatted;
import net.coagulate.Core.HTML.Page;
import net.coagulate.SL.Data.Session;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.SL;
import net.coagulate.SL.State;
import org.apache.http.HttpStatus;

import javax.annotation.Nonnull;

import static java.util.logging.Level.SEVERE;

/**
 * @author Iain Price
 */
public class SSOExchange {
	
	@UrlPrefix(url="/SSO/", authenticate=false)
	public static void handle(@Nonnull final State state) {
		try {
			
			final String token=state.getUri().replaceFirst("/SSO/","");
			final User user=User.getSSO(token);
			if (user==null) {
				SL.log().warning("SSO Exchange of token failed to return a valid user.");
				Page.page().addHeader("Location","/");
				Page.page().responseCode(HttpStatus.SC_SEE_OTHER);
				return;
			}
			SL.log().info("Successful SSO sign-on for "+user);
			final Session session=Session.create(user);
			state.page().addHeader("Set-Cookie","coagulateslsessionid="+session.token()+"; HttpOnly; Path=/; Secure;");
			state.page().addHeader("Location","/");
			state.page().responseCode(HttpStatus.SC_SEE_OTHER);
		} catch (@Nonnull final Exception ex) {
			SL.log().log(SEVERE,"SSO?",ex);
			state.page().responseCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			// todo
			state.page()
			     .add(new Preformatted().add("500 - Internal Server Error").add("Internal Exception, see debug logs"));
		}
	}
	
	@Nonnull
	@Override
	public String toString() {
		return "SSOExchange";
	}
	
}
