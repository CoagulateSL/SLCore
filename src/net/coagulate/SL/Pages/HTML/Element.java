package net.coagulate.SL.Pages.HTML;

import net.coagulate.SL.State;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * @author Iain Price
 */
public interface Element {

	// ---------- INSTANCE ----------

	/**
	 * Render this element (and any children inline)
	 */
	@Nullable
	String toHtml(State st);

	/**
	 * Return pertinent data/values only, mostly used for tables and stuff
	 */
	@Nullable
	String toString(State st);

	/**
	 * Load key values from the map, for input elements
	 */
	void load(Map<String,String> map);

}
