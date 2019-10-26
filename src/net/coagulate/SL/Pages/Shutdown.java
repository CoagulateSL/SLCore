package net.coagulate.SL.Pages;

import net.coagulate.Core.Tools.ExceptionTools;
import net.coagulate.Core.Tools.MailTools;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.SL.Config;
import net.coagulate.SL.HTTPPipelines.ContainerHandler;
import net.coagulate.SL.HTTPPipelines.Page;
import net.coagulate.SL.HTTPPipelines.PageMapper.Url;
import net.coagulate.SL.Maintenance;
import net.coagulate.SL.Pages.HTML.Paragraph;
import net.coagulate.SL.Pages.HTML.Raw;
import net.coagulate.SL.Pages.HTML.State;
import net.coagulate.SL.Pages.HTML.Table;
import net.coagulate.SL.SL;

import javax.mail.MessagingException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Iain Price
 */
public class Shutdown extends ContainerHandler {

	@Url("/shutdown")
	public Shutdown() {super();}

	@Override
	protected void run(State state, Page page) {
		String ip=state.getClientIP();
		if (ip.equals("DIRECT:0:0:0:0:0:0:0:1") || ip.equals("DIRECT:127.0.0.1")) {
			SL.shutdown();
			page.add(new Paragraph("SHUTDOWN INITIATED"));
		} else {
			throw new SystemException("Unauthorised access to shutdown from "+state.getClientIP());
		}
	}


}
