package net.coagulate.SL.Pages.HTML;

import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.SL.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Iain Price
 */
public class TimeStampOrDuration extends TimeStamp {
	// ---------- INSTANCE ----------
	public String render(@Nonnull final State st,
	                     @Nullable final String strvalue) {
		if (strvalue==null || strvalue.isEmpty() || Integer.parseInt(strvalue)==0) { return "-"; }
		final int value=Integer.parseInt(strvalue);
		final int now=UnixTime.getUnixTime();
		final int diff=now-value;
		boolean historic=false;
		if (now>value) { historic=true; }
		if (diff>(-4*UnixTime.WEEK) && diff<(4*UnixTime.WEEK)) {
			return (historic?"":"in ")+UnixTime.durationRelativeToNow(value,false)+(historic?" ago":"");
		}
		return super.render(st,strvalue);
	}
}
