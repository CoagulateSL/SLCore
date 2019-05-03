package net.coagulate.SL.Pages.HTML;

/**
 * @author Iain Price
 */
public class Form extends Container {
	public String toHtml(State st) {
		return
				"<form method=post>" +
						super.toHtml(st) +
						"</form>";
	}

	public Form submit(String s) {
		add(new Submit(s));
		return this;
	}

	public Form submit(Element e) {
		add(e);
		return this;
	}
}
