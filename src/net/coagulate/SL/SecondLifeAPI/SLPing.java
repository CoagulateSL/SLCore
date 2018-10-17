package net.coagulate.SL.SecondLifeAPI;

import net.coagulate.SL.HTTPPipelines.PageMapper.Url;
import org.json.JSONObject;

/**
 *
 * @author Iain Price
 */
public class SLPing extends SLAPI {

    @Url("/SecondLifeAPI/Ping")
    public SLPing(){super();}
    @Override
    public String toString() { return "SLPing"; }
    @Override
    protected JSONObject handleJSON(JSONObject object) {
        return object;
    }
    
}
