package net.coagulate.SL.Pages;

import net.coagulate.Core.Exceptions.User.UserAccessDeniedException;
import net.coagulate.Core.HTML.Elements.Paragraph;
import net.coagulate.SL.HTTPPipelines.Url;
import net.coagulate.SL.SL;
import net.coagulate.SL.State;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class Shutdown  {

	@Url(url="/shutdown",authenticate = false)
	public static void shutdown(@Nonnull final State state) {
		final String ip=State.get().getClientIP();
		if (ip.equals("")) {
			SL.shutdown();
			state.add(new Paragraph("SHUTDOWN INITIATED"));
		}
		else {
			throw new UserAccessDeniedException("Unauthorised access to shutdown from "+State.get().getClientIP());
		}
	}


}
