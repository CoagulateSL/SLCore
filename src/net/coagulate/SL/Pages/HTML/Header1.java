package net.coagulate.SL.Pages.HTML;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Iain Price
 */
public class Header1 extends Container {

	@Nullable
	public String name = null;

	public Header1(Element e) { add(e); }

	public Header1(String s) { add(new Raw(s)); }

	@Nonnull
	public Header1 add(Element e) {
		super.add(e);
		return this;
	}

	@Nonnull
	public String toHtml(State st) {
		return
				"<h1" +
						(name == null ? "" : " name=\"" + name + "\"") +
						">" +
						super.toHtml(st) +
						"</h1>";
	}

	@Nonnull
	public Header1 name(String name) {
		this.name = name;
		return this;
	}
}
