package net.coagulate.SL.Pages.HTML;

import net.coagulate.SL.State;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class Form extends Container {
	// ---------- INSTANCE ----------
	@Nonnull
	public String toHtml(final State st) {
		return "<form method=post>"+super.toHtml(st)+"</form>";
	}


}
