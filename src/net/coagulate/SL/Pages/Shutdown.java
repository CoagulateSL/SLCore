package net.coagulate.SL.Pages;

import net.coagulate.Core.Exceptions.SystemException;
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

	@Override
	protected void run(@Nonnull final State state, @Nonnull final Page page) {
		final String ip=state.getClientIP();
		if (ip.equals("DIRECT:0:0:0:0:0:0:0:1") || ip.equals("DIRECT:127.0.0.1")) {
			SL.shutdown();
			page.add(new Paragraph("SHUTDOWN INITIATED"));
		} else {
			throw new SystemException("Unauthorised access to shutdown from "+state.getClientIP());
		}
	}


}
