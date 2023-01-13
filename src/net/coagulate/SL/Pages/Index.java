package net.coagulate.SL.Pages;

import net.coagulate.Core.HTML.Page;
import net.coagulate.SL.Config;
import net.coagulate.SL.HTML.ServiceTile;
import net.coagulate.SL.HTTPPipelines.SLPageTemplate;
import net.coagulate.SL.HTTPPipelines.Url;
import net.coagulate.SL.SL;
import net.coagulate.SL.State;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class Index {
	
	@Url(url="/")
	public static void index(@Nonnull final State state) {
		state.root().header1("Welcome!");
		final Page page=state.page();
		page.template(new SLPageTemplate(SLPageTemplate.PAGELAYOUT.CENTERCOLUMN));
		page.root()
		    .p("Welcome to "+(Config.isOfficial()?"Coagulate Second Life services":SL.brandNameUniversal())+
		       ", select a service for more information.")
		    .align("center");
		for (final ServiceTile tile: SL.getServiceTiles()) {
			page.add(tile);
		}
	}
	
}
