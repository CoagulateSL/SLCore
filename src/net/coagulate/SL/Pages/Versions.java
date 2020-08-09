package net.coagulate.SL.Pages;

import net.coagulate.SL.HTTPPipelines.Page;
import net.coagulate.SL.HTTPPipelines.PageMapper.Url;
import net.coagulate.SL.HTTPPipelines.StringHandler;
import net.coagulate.SL.Pages.HTML.Raw;
import net.coagulate.SL.Pages.HTML.State;
import net.coagulate.SL.SL;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class Versions extends StringHandler {

	@Url("/versions")
	public Versions() {}

	// ----- Internal Instance -----
	@Nonnull
	@Override
	protected String handleString(@Nonnull final State state) {
		Page page=new Page();
		page.layout(Page.PAGELAYOUT.CENTERCOLUMN);
		page.add(new Raw(SL.htmlVersionDump().toString()));
		return page.toHtml(state);
	}

}
