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
	public String render(@Nonnull final State st,@Nullable final String value) {
		if (value==null||value.isEmpty()||Integer.parseInt(value)==0) {
			return "-";
		}
		final int intValue=Integer.parseInt(value);
		final int now=UnixTime.getUnixTime();
		final int diff=now-intValue;
		final boolean historic=now>intValue;
		if (diff>(-4*UnixTime.WEEK)&&diff<(4*UnixTime.WEEK)) {
			return (historic?"":"in ")+UnixTime.durationRelativeToNow(intValue,false)+(historic?" ago":"");
		}
		return super.render(st,value);
	}
}
