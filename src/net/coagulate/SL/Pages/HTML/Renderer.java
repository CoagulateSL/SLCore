package net.coagulate.SL.Pages.HTML;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * @author Iain Price
 */
public abstract class Renderer implements Element {
	// note no valued constructor as many places (table...) run in-line as render() instead
	@Nullable
	protected String value;

	public void set(final String value) { this.value = value; }

	@Nullable
	public String get() { return value; }

	@Nullable
	@Override
	public String toHtml(final State st) { return render(st, value); }

	@Nullable
	public String toString(final State st) { return value; }

	@Override
	public void load(final Map<String, String> map) {}

	@Nullable
	public abstract String render(State st, String value);
}
