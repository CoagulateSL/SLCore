package net.coagulate.SL.Pages.HTML;

/**
 * @author Iain Price
 */
public class SignificantFigures extends Renderer {

	int sf = 0;

	public SignificantFigures(int sf) { this.sf = sf; }

	@Override
	public String render(State st, String value) {
		if (value == null || value.isEmpty()) { return ""; }
		if (value.length() <= (sf)) { return value; }
		if (value.indexOf(".") == -1) { return value; }
		return value.substring(0, sf + 1);
	}

}
