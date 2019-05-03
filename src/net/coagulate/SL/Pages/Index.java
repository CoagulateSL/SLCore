package net.coagulate.SL.Pages;

import net.coagulate.SL.HTTPPipelines.AuthenticatedContainerHandler;
import net.coagulate.SL.HTTPPipelines.Page;
import net.coagulate.SL.HTTPPipelines.PageMapper.Url;
import net.coagulate.SL.Pages.HTML.Paragraph;
import net.coagulate.SL.Pages.HTML.State;

/**
 * @author Iain Price
 */
public class Index extends AuthenticatedContainerHandler {

	@Url("/")
	public Index() {}

	@Override
	protected void run(State state, Page page) {
		page.layout(Page.PAGELAYOUT.CENTERCOLUMN);
		page.paragraph("Welcome to Coagulate Second Life services, select a service for more information.").align(Paragraph.ALIGNMENT.CENTER);
		//raw("<table style=\"max-width: 900px;\" align=center>");
		//raw("<tr width=100%><td width=100%><ul style=\"white-space: nowrap;\">");
		page.serviceCell("Region Monitoring", "/RegionMonitor").
				add(new Paragraph("Region Monitoring monitors the status of specified regions, polling every minute and logging the data.")).
				add(new Paragraph("Can also use scripted or bot services to collect performance metrics."));
        /*
        openServiceCell("Web Chat","/WebChat");
        p("[Placeholder note to self] Web Chat provides a simple interface to Second Life chat over a web page.");
        p("Perfect for use on a simple browser or low power consumption device.  Can use hosted or home-run bot connections.");
        closeServiceCell();
        openServiceCell("Bot Agents","/Bot");
        p("This allows you to operate an automated agent in Second Life (a 'bot').");
        p("This can be used to automate group invites, group ejects, and other features");
        closeServiceCell();
        raw("</ul></td></tr>");
        raw("<tr width=100%><td width=100%><ul style=\"white-space: nowrap;\">");
        */
		page.serviceCell("<img src=\"/resources/serviceicon-gphud.png\">", "/GPHUD/").
				add(new Paragraph("GPHUD is the 2nd generation role-play HUD.")).
				add(new Paragraph("This is used to implement various game modes at sims."));
	}

}
