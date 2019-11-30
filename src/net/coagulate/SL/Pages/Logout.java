package net.coagulate.SL.Pages;

import net.coagulate.SL.HTTPPipelines.Page;
import net.coagulate.SL.HTTPPipelines.PageMapper.Url;
import net.coagulate.SL.HTTPPipelines.StringHandler;
import net.coagulate.SL.Pages.HTML.Raw;
import net.coagulate.SL.Pages.HTML.State;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class Logout extends StringHandler {

	@Url("/Logout")
	public Logout() {super();}

	@Override
	protected String handleString(@Nonnull State state) {
		state.logout();
		return new Page().add(new Raw("<br><br><h3 align=center>Your session has been ended</h3><br><br><br><p align=center><a href=\"/\">Click here to return to the login page</a></p>")).toHtml(state);
	}

}
