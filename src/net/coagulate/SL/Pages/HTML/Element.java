package net.coagulate.SL.Pages.HTML;

import java.util.Map;

/**
 * @author Iain Price
 */
public interface Element {

	/**
	 * Render this element (and any children inline)
	 */
	public String toHtml(State st);

	/**
	 * Return pertinent data/values only, mostly used for tables and stuff
	 */
	public String toString(State st);

	/**
	 * Load key values from the map, for input elements
	 */
	public void load(Map<String, String> map);

}
