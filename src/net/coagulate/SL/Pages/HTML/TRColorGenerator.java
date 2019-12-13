package net.coagulate.SL.Pages.HTML;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Iain Price
 */
public class TRColorGenerator extends TRGenerator {

	private final String columnname;
	private final String defaultcolor;
	final Map<String, String> valuemap = new HashMap<>();

	public TRColorGenerator(String columnname, String defaultcolor) {
		this.columnname = columnname;
		this.defaultcolor = defaultcolor;
	}

	@Nonnull
	public TRColorGenerator map(String value, String color) {
		valuemap.put(value, color);
		return this;
	}

	@Nonnull
	@Override
	public String render(State st, @Nonnull Map<String, String> row) {
		return "<tr bgcolor=\"#" + getColor(st, row) + "\">";
	}

	private String getColor(State st, @Nonnull Map<String, String> row) {
		if (!row.containsKey(columnname)) { return defaultcolor; }
		String value = row.get(columnname);
		if (!valuemap.containsKey(value)) { return defaultcolor; }
		return valuemap.get(value);
	}
}
