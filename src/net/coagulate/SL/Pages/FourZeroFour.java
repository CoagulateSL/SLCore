package net.coagulate.SL.Pages;

import net.coagulate.SL.HTTPPipelines.Url;
import net.coagulate.SL.State;
import org.apache.http.HttpStatus;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class FourZeroFour {

	@Url(url="/404",authenticate = false)
	public static void page(@Nonnull final State state) {
		state.page().responseCode(HttpStatus.SC_NOT_FOUND);
		state.root().header1("Four Hundred and Four").align("center").
				p("As in, 404, Page Not Found").align("center").
				p("The requested URI (uri) was not mapped to a page handler").align("center");
	}
}
