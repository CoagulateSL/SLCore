package net.coagulate.SL.SecondLifeAPI;

import org.json.JSONObject;

/**
 *
 * @author Iain Price
 */
public class SLPing extends SLAPI {

    @Override
    public String toString() { return "SLPing"; }
    @Override
    protected JSONObject handleJSON(JSONObject object) {
        return object;
    }
    
}
