package net.coagulate.SL.SecondLifeAPI;

import net.coagulate.SL.Config;
import net.coagulate.SL.HTTPPipelines.PageMapper.Url;
import net.coagulate.SL.Pages.HTML.State;
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
    protected JSONObject handleJSON(JSONObject object,State st) {
        object.put("nodename",Config.getNodeName()); // sl1 sl2 sl3
        object.put("node",Config.getNode()); // 0 1 2
        object.put("hostname",Config.getHostName()); // saturn mars neptune
        return object;
    }
    
    @Override
    protected boolean needsDigest() { return false; }
}
