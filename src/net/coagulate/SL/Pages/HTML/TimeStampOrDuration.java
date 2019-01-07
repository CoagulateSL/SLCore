package net.coagulate.SL.Pages.HTML;

import net.coagulate.Core.Tools.UnixTime;

/**
 *
 * @author Iain Price
 */
public class TimeStampOrDuration extends TimeStamp {
    public String render(State st,String strvalue) {
        if (strvalue==null || strvalue.isEmpty() || Integer.parseInt(strvalue)==0) { return "-"; }
        int value=Integer.parseInt(strvalue);
        int now=UnixTime.getUnixTime();
        int diff=now-value;
        boolean historic=false; if (now>value) { historic=true; }
        if (diff>(-4*UnixTime.WEEK) && diff<(4*UnixTime.WEEK)) {
            return (historic?"":"in ")+UnixTime.durationRelativeToNow(value,false)+(historic?" ago":"");
        }
        return super.render(st,strvalue);
    }
}
