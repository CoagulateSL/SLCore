package net.coagulate.SL.Pages.HTML;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Iain Price
 */
public class Container implements Element {

	final List<Element> content = new ArrayList<>();

	@Override
	public String toHtml(State st) {
		StringBuilder ret = new StringBuilder();
		for (Element e : content) { ret.append(e.toHtml(st)); }
		return ret.toString();
	}

	public String toString(State st) {
		StringBuilder ret = new StringBuilder();
		for (Element e : content) { ret.append(e.toString(st)).append("\n"); }
		return ret.toString();
	}

	@Override
	public void load(Map<String, String> map) {
		for (Element e : content) { e.load(map); }
	}

	public Container add(Element element) {
		content.add(element);
		return this;
	}
}
