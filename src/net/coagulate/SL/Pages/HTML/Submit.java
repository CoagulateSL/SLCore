package net.coagulate.SL.Pages.HTML;

import java.util.Map;

/**
 * @author Iain Price
 */
public class Submit extends Container {

	final String name;
	String value = null;

	public Submit(String name) {
		this.name = name;
		this.value = name;
		super.add(new Raw(name));
	}

	public Submit(String name, String value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String toHtml(State st) {
		String v = value;
		if (v == null) { v = ""; }
		return "<button type=submit name=\"" + name + "\" value=\"" + v + "\">" +
				super.toHtml(st) +
				"</button>";
	}

	@Override
	public void load(Map<String, String> map) {
		if (value == null) {
			if (map.containsKey(name)) {
				value = map.get(name);
			}
		}
	}
}
