package net.coagulate.SL.Pages.HTML;

/**
 * @author Iain Price
 */
public class Paragraph extends Container {
	private ALIGNMENT alignment = ALIGNMENT.NONE;

	public Paragraph() {}

	public Paragraph(String s) { add(new Raw(s)); }

	public Paragraph add(Element e) {
		super.add(e);
		return this;
	}

	public Paragraph add(String s) { return add(new Raw(s)); }

	public String toHtml(State st) {
		return
				"<p" + renderAlignment() + ">" +
						super.toHtml(st) +
						"</p>";
	}

	private String renderAlignment() {
		if (alignment == ALIGNMENT.CENTER) { return " align=\"center\""; }
		return "";
	}

	;

	public Paragraph align(ALIGNMENT alignment) {
		this.alignment = alignment;
		return this;
	}

	public enum ALIGNMENT {NONE, CENTER}
}
