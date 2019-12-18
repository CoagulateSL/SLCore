package net.coagulate.SL.SecondLifeAPI;

import net.coagulate.SL.Config;
import net.coagulate.SL.HTTPPipelines.PageMapper.Url;
import net.coagulate.SL.Pages.HTML.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class CallBack extends SLAPI {

	@Url("/SecondLifeAPI/CallBack")
	public CallBack() {super();}

	@Nonnull
	@Override
	public String toString() { return "CallBack"; }

	@Nonnull
	@Override
	protected JSONObject handleJSON(@Nonnull final JSONObject object,
	                                final State st)
	{
		final String url=object.getString("url");
		object.put("nodename",Config.getNodeName()); // sl1 sl2 sl3
		object.put("node",Config.getNode()); // 0 1 2
		object.put("hostname",Config.getHostName()); // saturn mars neptune
		object.remove("developerkey");
		getLogger().fine("CallBack received for URL "+url);
		//noinspection CallToThreadRun
		new Transmit(object,url).run();
		return object;
	}

}
