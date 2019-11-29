package net.coagulate.SL.Pages.HTML;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;

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
	private final List<Column> columns;

	public SQLTable(DBConnection db, String sql, Object... params) {
		this.db = db;
		this.sql = sql;
		this.params = params;
		columns = new ArrayList<>();
	}

	public SQLTable column(String header, String columnname) { return column(header, columnname, null, Alignment.NONE); }

	public SQLTable column(String header, String columnname, Renderer renderer) { return column(header, columnname, renderer, Alignment.NONE); }

	public SQLTable column(String header, String columnname, Alignment alignment) { return column(header, columnname, null, alignment); }

	public SQLTable column(String header, String columnname, Renderer renderer, Alignment alignment) {
		columns.add(new Column(header, columnname, renderer, alignment));
		header(header);
		return this;
	}

	public String contentRows(State st) {
		Results results = db.dq(sql, params);
		String r = "";
		for (ResultsRow row : results) {
			Map<String, String> rowstr = new HashMap<>();
			for (Column column : columns) { rowstr.put(column.columnname, row.getString(column.columnname)); }
			r += openRow(st, rowstr);
			for (Column column : columns) {
				//System.out.println("On column "+column.columnname);
				//for (String s:row.keySet()) { System.out.println("Exists:"+s); }
				r += openCell(column);
				r += column.render(st, row.getString(column.columnname));
				r += "</td>";
			}
			r += "</tr>";
		}
		return r;
	}

	public SQLTable rowGenerator(TRGenerator generator) {
		super.rowGenerator(generator);
		return this;
	}

	private String openCell(Column column) {
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

		public String render(State state, String value) {
			if (renderer == null) {
				if (value == null) { return ""; }
				return value;
			}
			return renderer.render(state, value);
		}
	}

}
