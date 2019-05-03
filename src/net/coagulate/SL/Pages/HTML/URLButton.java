package net.coagulate.SL.Pages.HTML;

import java.util.Map;

/**
 * @author Iain Price
 */
public class URLButton implements Element {

	private final String label;
	private final String url;

	public URLButton(String label, String url) {
		this.label = label;
		this.url = url;
	}

	@Override
	public String toHtml(State st) {
		return "<a href=\"" + url + "\"><button type=submit>" + label + "</button></a>";
	}

	@Override
	public String toString(State st) { return label; }

	@Override
	public void load(Map<String, String> map) {}

}
