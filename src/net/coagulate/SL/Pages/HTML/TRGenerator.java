package net.coagulate.SL.Pages.HTML;

import net.coagulate.SL.State;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * @author Iain Price
 */
public abstract class TRGenerator implements Element {
	
	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String toHtml(final State st) {
		return "<tr>";
	}
	
	@Nonnull
	@Override
	public String toString(final State st) {
		return "<tr>";
	}
	
	@Override
	public void load(final Map<String,String> map) {
	}
	
	@Nonnull
	public abstract String render(State st,Map<String,String> row);
}
