package net.coagulate.SL.Pages.HTML;

import net.coagulate.SL.State;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class Paragraph extends Container {
	private ALIGNMENT alignment=ALIGNMENT.NONE;

	public Paragraph() {}

	public Paragraph(final String s) { add(new Raw(s)); }

	// ---------- INSTANCE ----------
	@Nonnull
	public Paragraph add(final String s) { return add(new Raw(s)); }

	@Nonnull
	public String toHtml(final State st) {
		return "<p"+renderAlignment()+">"+super.toHtml(st)+"</p>";
	}

	@Nonnull
    public Paragraph add(final Element element) {
        super.add(element);
        return this;
    }

	@Nonnull
	public Paragraph align(final ALIGNMENT alignment) {
		this.alignment=alignment;
		return this;
	}

	// ----- Internal Instance -----
	@Nonnull
	private String renderAlignment() {
		if (alignment==ALIGNMENT.CENTER) { return " align=\"center\""; }
		return "";
	}

	public enum ALIGNMENT {
		NONE,
		CENTER
	}
}
