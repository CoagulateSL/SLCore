package net.coagulate.SL.Pages.HTML;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class Paragraph extends Container {
	private ALIGNMENT alignment = ALIGNMENT.NONE;

	public Paragraph() {}

	public Paragraph(String s) { add(new Raw(s)); }

	@Nonnull
	public Paragraph add(Element e) {
		super.add(e);
		return this;
	}

	@Nonnull
	public Paragraph add(String s) { return add(new Raw(s)); }

	@Nonnull
	public String toHtml(State st) {
		return
				"<p" + renderAlignment() + ">" +
						super.toHtml(st) +
						"</p>";
	}

	@Nonnull
	private String renderAlignment() {
		if (alignment == ALIGNMENT.CENTER) { return " align=\"center\""; }
		return "";
	}

	@Nonnull
	public Paragraph align(ALIGNMENT alignment) {
		this.alignment = alignment;
		return this;
	}

	public enum ALIGNMENT {NONE, CENTER}
}
