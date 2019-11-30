package net.coagulate.SL.Pages.HTML;

import javax.annotation.Nullable;

/**
 * @author Iain Price
 */
public class SignificantFigures extends Renderer {

	int sf = 0;

	public SignificantFigures(int sf) { this.sf = sf; }

	@Nullable
	@Override
	public String render(State st, @Nullable String value) {
		if (value == null || value.isEmpty()) { return ""; }
		if (value.length() <= (sf)) { return value; }
		if (!value.contains(".")) { return value; }
		return value.substring(0, sf + 1);
	}

}
