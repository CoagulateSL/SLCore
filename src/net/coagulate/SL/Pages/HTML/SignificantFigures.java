package net.coagulate.SL.Pages.HTML;

import javax.annotation.Nullable;

/**
 * @author Iain Price
 */
public class SignificantFigures extends Renderer {

	final int sf;

	public SignificantFigures(final int sf) { this.sf=sf; }

	// ---------- INSTANCE ----------
	@Nullable
	@Override
	public String render(final State st,
	                     @Nullable final String value) {
		if (value==null || value.isEmpty()) { return ""; }
		if (value.length()<=(sf)) { return value; }
		if (!value.contains(".")) { return value; }
		return value.substring(0,sf+1);
	}

}
