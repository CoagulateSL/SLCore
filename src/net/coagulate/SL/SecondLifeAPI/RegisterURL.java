package net.coagulate.SL.SecondLifeAPI;

import net.coagulate.SL.HTTPPipelines.PageMapper.Url;
import net.coagulate.SL.Pages.HTML.State;
import org.json.JSONObject;

/**
 *
 * @author Iain Price
 */
public class RegisterURL extends SLAPI {

    @Url("/SecondLifeAPI/RegisterURL")
    public RegisterURL(){super();}
    @Override
    protected JSONObject handleJSON(JSONObject object,State st) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
