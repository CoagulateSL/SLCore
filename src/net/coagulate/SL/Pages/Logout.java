package net.coagulate.SL.Pages;

import net.coagulate.SL.HTTPPipelines.Url;
import net.coagulate.SL.State;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class Logout {

	@Url(url="/Logout",authenticate = false)
	public static void logout(@Nonnull final State state) {
		state.logout();
		state.root().
				header3("Your session has been ended").
				p().a("/","Click here to return to the login page").align("center");
	}

}
