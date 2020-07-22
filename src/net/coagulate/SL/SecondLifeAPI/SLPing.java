package net.coagulate.SL.SecondLifeAPI;

import net.coagulate.SL.Config;
import net.coagulate.SL.HTTPPipelines.PageMapper.Url;
import net.coagulate.SL.Pages.HTML.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class SLPing extends SLAPI {

	@Url("/SecondLifeAPI/Ping")
	public SLPing() {super();}

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String toString() { return "SLPing"; }

	// ----- Internal Instance -----
	@Nonnull
	@Override
	protected JSONObject handleJSON(@Nonnull final JSONObject object,
	                                final State st) {
		object.put("hostname",Config.getHostName()); // saturn mars neptune
		return object;
	}

	@Override
	protected boolean needsDigest() { return false; }
}
