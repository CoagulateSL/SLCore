package net.coagulate.SL.Pages.HTML;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Iain Price
 */
public class Table extends Container {
	protected TRGenerator trgen = null;
	// a table is ...
	List<List<Element>> table;
	List<Element> headers = new ArrayList<>();
	List<Element> row = null;

	public Table() {
		table = new ArrayList<>();
	}

	public Table openRow() {
		row = new ArrayList<>();
		table.add(row);
		return this;
	}

	public Table checkRow() {
		if (row == null) { openRow(); }
		return this;
	}

	public Table add(Element e) {
		checkRow();
		row.add(e);
		return this;
	}

	public Table add(String s) {
		checkRow();
		row.add(new Raw(s));
		return this;
	}

	public Table header(Element e) {
		headers.add(e);
		return this;
	}

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

	public String toHtml(State st) {
		return
				"<table>" +
						headerRow(st) +
						contentRows(st) +
						"</table>";
	}

	protected String headerRow(State st) {
		if (headers.isEmpty()) { return ""; }
		String r = "<tr>";
		for (Element e : headers) {
			r = r +
					"<th>" +
					e.toHtml(st) +
					"</th>";
		}
		r += "</tr>";
		return r;
	}

	protected String contentRows(State st) {
		String r = "";
		for (List<Element> row : table) {
			Map<String, String> stringrow = new HashMap<>();
			for (Element cell : row) {
				int pos = row.indexOf(cell);
				if (pos < headers.size()) {
					stringrow.put(headers.get(pos).toString(st), cell.toString(st));
				}
			}
			r += openRow(st, stringrow);
			for (Element cell : row) {
				r += "<td>";
				r += cell.toHtml(st);
				r += "</td>";
			}
			r += "</tr>";
		}
		return r;
	}

	protected String openRow(State st, Map<String, String> row) {
		if (trgen == null) { return "<tr>"; }
		return trgen.render(st, row);
	}

	public Table rowGenerator(TRGenerator generator) {
		trgen = generator;
		return this;
	}

}
