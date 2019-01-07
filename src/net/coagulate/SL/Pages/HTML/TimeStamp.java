package net.coagulate.SL.Pages.HTML;

import net.coagulate.Core.Tools.UnixTime;

/**
 *
 * @author Iain Price
 */
public class TimeStamp extends Renderer {

    @Override
    public String render(State st, String value) {
        if (value==null || value.isEmpty() || Integer.parseInt(value)==0) { return "-"; }
        String timezone="America/Los_Angeles";
        if (st.user()!=null) {
            timezone=st.user().getTimeZone();
        }
        return UnixTime.fromUnixTime(Integer.parseInt(value), timezone);
    }
    
}
