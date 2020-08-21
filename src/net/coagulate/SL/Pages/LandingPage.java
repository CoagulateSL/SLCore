package net.coagulate.SL.Pages;

import net.coagulate.SL.HTTPPipelines.SLPageTemplate;
import net.coagulate.SL.HTTPPipelines.Url;
import net.coagulate.SL.State;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class LandingPage {

	@Url(url="/LandingPage",authenticate = false)
	public static void logout(@Nonnull final State state) {
		state.page().template(new SLPageTemplate(SLPageTemplate.PAGELAYOUT.CENTERCOLUMN));
		state.root().
				header1("Coagulate SL").
				p("Welcome to the home of Coagulate Second Life Services").
				p("I'll put something here some day, promise");
	}

}
