package net.coagulate.SL.Pages.HTML;

import net.coagulate.SL.State;

import java.util.Map;

/**
 * @author Iain Price
 */
public class Raw implements Element {

	private final String content;

	public Raw(final String content) { this.content=content; }

	// ---------- INSTANCE ----------
	@Override
	public String toHtml(final State st) { return content; }

	@Override
	public String toString(final State st) { return content; }

	@Override
	public void load(final Map<String,String> map) {}

}
