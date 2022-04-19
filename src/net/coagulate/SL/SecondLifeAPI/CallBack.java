package net.coagulate.SL.SecondLifeAPI;

import net.coagulate.SL.Config;
import net.coagulate.SL.HTTPPipelines.PageType;
import net.coagulate.SL.HTTPPipelines.Url;
import net.coagulate.SL.SL;
import net.coagulate.SL.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class CallBack {
	@Url(url="/SecondLifeAPI/CallBack",pageType = PageType.SLAPI)
	public static void callBack(@Nonnull final State state) {
		final String url = state.parameters().get("url");
        final JSONObject object = new JSONObject();
		object.put("hostname",Config.getHostName()); // saturn mars neptune
		SL.log("CallBack").fine("CallBack received for URL "+url);
		//noinspection CallToThreadRun
		new Transmit(object,url).run();
		state.jsonOut(object);
	}

}
