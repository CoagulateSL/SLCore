package net.coagulate.SL.Pages.HTML;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Iain Price
 */
public class SQLTable extends Table {

	private final String sql;
	private final Object[] params;
	private final DBConnection db;
	@Nonnull
	private final List<Column> columns;

	public SQLTable(DBConnection db, String sql, Object... params) {
		this.db = db;
		this.sql = sql;
		this.params = params;
		columns = new ArrayList<>();
	}

	@Nonnull
	public SQLTable column(String header, String columnname) { return column(header, columnname, null, Alignment.NONE); }

	@Nonnull
	public SQLTable column(String header, String columnname, Renderer renderer) { return column(header, columnname, renderer, Alignment.NONE); }

	@Nonnull
	public SQLTable column(String header, String columnname, Alignment alignment) { return column(header, columnname, null, alignment); }

	@Nonnull
	public SQLTable column(String header, String columnname, Renderer renderer, Alignment alignment) {
		columns.add(new Column(header, columnname, renderer, alignment));
		header(header);
		return this;
	}

	@Nonnull
	public String contentRows(State st) {
		Results results = db.dq(sql, params);
		StringBuilder r = new StringBuilder();
		for (ResultsRow row : results) {
			Map<String, String> rowstr = new HashMap<>();
			for (Column column : columns) { rowstr.put(column.columnname, row.getStringNullable(column.columnname)); }
			r.append(openRow(st, rowstr));
			for (Column column : columns) {
				//System.out.println("On column "+column.columnname);
				//for (String s:row.keySet()) { System.out.println("Exists:"+s); }
				r.append(openCell(column));
				r.append(column.render(st, row.getStringNullable(column.columnname)));
				r.append("</td>");
			}
			r.append("</tr>");
		}
		return r.toString();
	}

	@Nonnull
	public SQLTable rowGenerator(TRGenerator generator) {
		super.rowGenerator(generator);
		return this;
	}

	@Nonnull
	private String openCell(@Nonnull Column column) {
		if (column.alignment == Alignment.LEFT) { return "<td align=left>"; }
		if (column.alignment == Alignment.CENTER) { return "<td align=center>"; }
		if (column.alignment == Alignment.RIGHT) { return "<td align=right>"; }
		return "<td>";
	}

	static class Column {

		final String columnname;
		final String header;
		final Renderer renderer;
		final Alignment alignment;

		Column(String header, String columnname, Renderer renderer, Alignment alignment) {
			this.header = header;
			this.columnname = columnname;
			this.renderer = renderer;
			this.alignment = alignment;
		}

		@Nullable
		public String render(State state, @Nullable String value) {
			if (renderer == null) {
				if (value == null) { return ""; }
				return value;
			}
			return renderer.render(state, value);
		}
	}

}
