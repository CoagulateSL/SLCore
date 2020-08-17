package net.coagulate.SL.Pages;

import net.coagulate.SL.HTTPPipelines.Url;
import net.coagulate.SL.State;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class Index {

	@Url(url="/")
	public static void index(@Nonnull final State state) {
		state.root().header1("Welcome!");
		/*
		page.layout(Page.PAGELAYOUT.CENTERCOLUMN);
		page.paragraph("Welcome to "+(Config.isOfficial()?"Coagulate Second Life services":SL.brandNameUniversal())+", select a service for more information.").align(Paragraph.ALIGNMENT.CENTER);
		//raw("<table style=\"max-width: 900px;\" align=center>");
		//raw("<tr width=100%><td width=100%><ul style=\"white-space: nowrap;\">");
		if (SL.hasModule("RegionMonitoring")) {
			page.serviceCell("Region Monitoring", "/RegionMonitor").
					add(new Paragraph("Region Monitoring monitors the status of specified regions, polling every minute and logging the data.")).
					add(new Paragraph("Can also use scripted or bot services to collect performance metrics."));
		}

		if (SL.hasModule("GPHUD")) {
			page.serviceCell("<img src=\"/resources/serviceicon-gphud.png\">", "/GPHUD/").
					add(new Paragraph("GPHUD is the 2nd generation role-play HUD.")).
					add(new Paragraph("This is used to implement various game modes at sims."));
		}
		*/
	}

}
