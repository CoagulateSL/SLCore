package net.coagulate.SL.Pages.HTML;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * @author Iain Price
 */
public abstract class TRGenerator implements Element {

	@Nonnull
	@Override
	public String toHtml(State st) { return "<tr>"; }

	@Nonnull
	@Override
	public String toString(State st) { return "<tr>"; }

	@Override
	public void load(Map<String, String> map) {}

	public abstract String render(State st, Map<String, String> row);
}
