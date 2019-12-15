package net.coagulate.SL.SecondLifeAPI;

import net.coagulate.SL.HTTPPipelines.PageMapper.Url;
import net.coagulate.SL.Pages.HTML.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class RegisterURL extends SLAPI {

	@Url("/SecondLifeAPI/RegisterURL")
	public RegisterURL() {super();}

	@Nonnull
	@Override
	protected JSONObject handleJSON(final JSONObject object, final State st) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

}
