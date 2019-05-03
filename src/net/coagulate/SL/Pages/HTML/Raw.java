package net.coagulate.SL.Pages.HTML;

import java.util.Map;

/**
 * @author Iain Price
 */
public class Raw implements Element {

	private String content;

	public Raw(String content) { this.content = content; }

	@Override
	public String toHtml(State st) { return content; }

	@Override
	public String toString(State st) { return content; }

	@Override
	public void load(Map<String, String> map) {}

}
