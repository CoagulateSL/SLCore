package net.coagulate.SL.Pages.HTML;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class Form extends Container {
	@Nonnull
	public String toHtml(State st) {
		return
				"<form method=post>" +
						super.toHtml(st) +
						"</form>";
	}

	@Nonnull
	public Form submit(String s) {
		add(new Submit(s));
		return this;
	}

	@Nonnull
	public Form submit(Element e) {
		add(e);
		return this;
	}
}
