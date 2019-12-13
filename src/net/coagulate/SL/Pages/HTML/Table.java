package net.coagulate.SL.Pages.HTML;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Iain Price
 */
public class Table extends Container {
	@Nullable
	protected TRGenerator trgen = null;
	// a table is ...
	@Nonnull
	final List<List<Element>> table;
	final List<Element> headers = new ArrayList<>();
	@Nullable
	List<Element> row = null;

	public Table() {
		table = new ArrayList<>();
	}

	@Nonnull
	public Table openRow() {
		row = new ArrayList<>();
		table.add(row);
		return this;
	}

	@Nonnull
	public Table checkRow() {
		if (row == null) { openRow(); }
		return this;
	}

	@Nonnull
	public Table add(Element e) {
		checkRow();
		row.add(e);
		return this;
	}

	@Nonnull
	public Table add(String s) {
		checkRow();
		row.add(new Raw(s));
		return this;
	}

	@Nonnull
	public Table header(Element e) {
		headers.add(e);
		return this;
	}

	@Nonnull
	public Table header(String s) {
		headers.add(new Raw(s));
		return this;
	}

	public void load(Map<String,String> map) {
		for (List<Element> list : table) {
			for (Element ele : list) {
				ele.load(map);
			}
		}
	}

	@Nonnull
	public String toHtml(State st) {
		return
				"<table>" +
						headerRow(st) +
						contentRows(st) +
						"</table>";
	}

	@Nonnull
	protected String headerRow(State st) {
		if (headers.isEmpty()) { return ""; }
		StringBuilder r = new StringBuilder("<tr>");
		for (Element e : headers) {
			r.append("<th>").append(e.toHtml(st)).append("</th>");
		}
		r.append("</tr>");
		return r.toString();
	}

	@Nonnull
	protected String contentRows(State st) {
		StringBuilder r = new StringBuilder();
		for (List<Element> row : table) {
			Map<String, String> stringrow = new HashMap<>();
			for (Element cell : row) {
				int pos = row.indexOf(cell);
				if (pos < headers.size()) {
					stringrow.put(headers.get(pos).toString(st), cell.toString(st));
				}
			}
			r.append(openRow(st, stringrow));
			for (Element cell : row) {
				r.append("<td>");
				r.append(cell.toHtml(st));
				r.append("</td>");
			}
			r.append("</tr>");
		}
		return r.toString();
	}

	@Nonnull
	protected String openRow(State st, Map<String, String> row) {
		if (trgen == null) { return "<tr>"; }
		return trgen.render(st, row);
	}

	@Nonnull
	public Table rowGenerator(TRGenerator generator) {
		trgen = generator;
		return this;
	}

}
