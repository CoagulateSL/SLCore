package net.coagulate.SL.Pages.HTML;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Iain Price
 */
public class TRColorGenerator extends TRGenerator {

	final Map<String,String> valuemap=new HashMap<>();
	private final String columnname;
	private final String defaultcolor;

	public TRColorGenerator(final String columnname,
	                        final String defaultcolor) {
		this.columnname=columnname;
		this.defaultcolor=defaultcolor;
	}

	@Nonnull
	public TRColorGenerator map(final String value,
	                            final String color) {
		valuemap.put(value,color);
		return this;
	}

	@Nonnull
	@Override
	public String render(final State st,
	                     @Nonnull final Map<String,String> row) {
		return "<tr bgcolor=\"#"+getColor(st,row)+"\">";
	}

	private String getColor(final State st,
	                        @Nonnull final Map<String,String> row) {
		if (!row.containsKey(columnname)) { return defaultcolor; }
		final String value=row.get(columnname);
		if (!valuemap.containsKey(value)) { return defaultcolor; }
		return valuemap.get(value);
	}
}
