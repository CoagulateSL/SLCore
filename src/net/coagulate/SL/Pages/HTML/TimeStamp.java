package net.coagulate.SL.Pages.HTML;

import net.coagulate.Core.Tools.UnixTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Iain Price
 */
public class TimeStamp extends Renderer {

	@Nullable
	@Override
	public String render(@Nonnull State st, @Nullable String value) {
		if (value == null || value.isEmpty() || Integer.parseInt(value) == 0) { return "-"; }
		String timezone = "America/Los_Angeles";
		if (st.user() != null) {
			timezone = st.user().getTimeZone();
		}
		return UnixTime.fromUnixTime(Integer.parseInt(value), timezone);
	}

	@Nullable
	public String toString(State st) {
		return value;
	}
}
