package net.coagulate.SL.Pages;

import net.coagulate.SL.HTTPPipelines.Page;
import net.coagulate.SL.HTTPPipelines.StringHandler;
import net.coagulate.SL.Pages.HTML.Raw;
import net.coagulate.SL.Pages.HTML.State;
import org.apache.http.HttpStatus;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class FourZeroFour extends StringHandler {
	private static String uri;

	public FourZeroFour(final String uri) { FourZeroFour.uri=uri; }

	@Nonnull
	@Override
	protected String handleString(@Nonnull final State state) {
		state.status(HttpStatus.SC_NOT_FOUND);
		return new Page().add(new Raw(
				"<h1 align=center>Four Hundred and Four</h1><br><br><p align=center>As in, 404, Page Not Found</p><br><br><br><br><p align=center>The requested URI ("+uri+")"
						+" "+"was not mapped to a page handler.</p>"))
		                 .toHtml(state);
	}
}
