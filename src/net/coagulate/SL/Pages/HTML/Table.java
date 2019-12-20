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
	// a table is ...
	@Nonnull
	final List<List<Element>> table;
	final List<Element> headers=new ArrayList<>();
	@Nullable
	protected TRGenerator trgen;
	@Nullable
	List<Element> row;

	public Table() {
		table=new ArrayList<>();
	}

	@Nonnull
	public Table openRow() {
		row=new ArrayList<>();
		table.add(row);
		return this;
	}

	@Nonnull
	public Table checkRow() {
		if (row==null) { openRow(); }
		return this;
	}

	@Nonnull
	public Table add(final Element e) {
		checkRow();
		if (row==null) { openRow(); }
		row.add(e);
		return this;
	}

	@Nonnull
	public Table add(final String s) {
		checkRow();
		if (row==null) { openRow(); }
		row.add(new Raw(s));
		return this;
	}

	@Nonnull
	public Table header(final Element e) {
		headers.add(e);
		return this;
	}

	@Nonnull
	public Table header(final String s) {
		headers.add(new Raw(s));
		return this;
	}

	public void load(final Map<String,String> map) {
		for (final List<Element> list: table) {
			for (final Element ele: list) {
				ele.load(map);
			}
		}
	}

	@Nonnull
	public String toHtml(final State st) {
		return "<table>"+headerRow(st)+contentRows(st)+"</table>";
	}

	@Nonnull
	protected String headerRow(final State st) {
		if (headers.isEmpty()) { return ""; }
		final StringBuilder r=new StringBuilder("<tr>");
		for (final Element e: headers) {
			r.append("<th>").append(e.toHtml(st)).append("</th>");
		}
		r.append("</tr>");
		return r.toString();
	}

	@Nonnull
	protected String contentRows(final State st) {
		final StringBuilder r=new StringBuilder();
		for (final List<Element> row: table) {
			final Map<String,String> stringrow=new HashMap<>();
			for (final Element cell: row) {
				final int pos=row.indexOf(cell);
				if (pos<headers.size()) {
					stringrow.put(headers.get(pos).toString(st),cell.toString(st));
				}
			}
			r.append(openRow(st,stringrow));
			for (final Element cell: row) {
				r.append("<td>");
				r.append(cell.toHtml(st));
				r.append("</td>");
			}
			r.append("</tr>");
		}
		return r.toString();
	}

	@Nonnull
	protected String openRow(final State st,
	                         final Map<String,String> row) {
		if (trgen==null) { return "<tr>"; }
		return trgen.render(st,row);
	}

	@Nonnull
	public Table rowGenerator(final TRGenerator generator) {
		trgen=generator;
		return this;
	}

}
