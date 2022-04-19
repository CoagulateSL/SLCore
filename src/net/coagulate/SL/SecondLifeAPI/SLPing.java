package net.coagulate.SL.SecondLifeAPI;

import net.coagulate.SL.Config;
import net.coagulate.SL.HTTPPipelines.PageType;
import net.coagulate.SL.HTTPPipelines.Url;
import net.coagulate.SL.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class SLPing {

	@Url(url="/SecondLifeAPI/Ping",pageType = PageType.SLAPI,digest=false)
	public static void ping(@Nonnull final State state) {
		final JSONObject object = new JSONObject();
		object.put("hostname",Config.getHostName()); // saturn mars neptune
		state.jsonOut(object);
	}
}
