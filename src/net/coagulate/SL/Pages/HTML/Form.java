package net.coagulate.SL.Pages.HTML;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class Form extends Container {
	@Nonnull
	public String toHtml(final State st) {
		return "<form method=post>"+super.toHtml(st)+"</form>";
	}

	@Nonnull
	public Form submit(final String s) {
		add(new Submit(s));
		return this;
	}

	@Nonnull
	public Form submit(final Element e) {
		add(e);
		return this;
	}
}
