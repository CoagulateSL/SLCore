package net.coagulate.SL.Pages.HTML;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Iain Price
 */
public class Container implements Element {

	final List<Element> content=new ArrayList<>();

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String toHtml(final State st) {
		final StringBuilder ret=new StringBuilder();
		for (final Element e: content) { ret.append(e.toHtml(st)); }
		return ret.toString();
	}

	@Nonnull
	public String toString(final State st) {
		final StringBuilder ret=new StringBuilder();
		for (final Element e: content) { ret.append(e.toString(st)).append("\n"); }
		return ret.toString();
	}

	@Override
	public void load(final Map<String,String> map) {
		for (final Element e: content) { e.load(map); }
	}

	@Nonnull
	public Container add(final Element element) {
		content.add(element);
		return this;
	}
}
