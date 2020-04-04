package net.coagulate.SL.Pages;

import net.coagulate.Core.Exceptions.User.UserAccessDeniedException;
import net.coagulate.SL.HTTPPipelines.ContainerHandler;
import net.coagulate.SL.HTTPPipelines.Page;
import net.coagulate.SL.HTTPPipelines.PageMapper.Url;
import net.coagulate.SL.Pages.HTML.Paragraph;
import net.coagulate.SL.Pages.HTML.State;
import net.coagulate.SL.SL;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class Shutdown extends ContainerHandler {

	@Url("/shutdown")
	public Shutdown() {super();}

	// ----- Internal Instance -----
	@Override
	protected void run(@Nonnull final State state,
	                   @Nonnull final Page page) {
		final String ip=state.getClientIP();
		if (ip.equals("")) {
			SL.shutdown();
			page.add(new Paragraph("SHUTDOWN INITIATED"));
		}
		else {
			throw new UserAccessDeniedException("Unauthorised access to shutdown from "+state.getClientIP());
		}
	}


}
