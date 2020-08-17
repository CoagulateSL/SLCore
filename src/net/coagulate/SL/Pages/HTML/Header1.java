package net.coagulate.SL.Pages.HTML;

import net.coagulate.SL.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Iain Price
 */
public class Header1 extends Container {

	@Nullable
	public String name;

	public Header1(final Element e) { add(e); }

	public Header1(final String s) { add(new Raw(s)); }

	// ---------- INSTANCE ----------
	@Nonnull
	public String toHtml(final State st) {
		return "<h1"+(name==null?"":" name=\""+name+"\"")+">"+super.toHtml(st)+"</h1>";
	}

	@Nonnull
	public Header1 add(final Element e) {
		super.add(e);
		return this;
	}

	@Nonnull
	public Header1 name(final String name) {
		this.name=name;
		return this;
	}
}
