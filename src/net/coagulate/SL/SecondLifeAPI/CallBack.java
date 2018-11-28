package net.coagulate.SL.SecondLifeAPI;

import net.coagulate.SL.Config;
import net.coagulate.SL.HTTPPipelines.PageMapper.Url;
import org.json.JSONObject;

/**
 *
 * @author Iain Price
 */
public class CallBack extends SLAPI {

    @Url("/SecondLifeAPI/CallBack")
    public CallBack(){super();}
    @Override
    public String toString() { return "CallBack"; }
    @Override
    protected JSONObject handleJSON(JSONObject object) {
        String url=object.getString("url");
        object.put("nodename",Config.getNodeName()); // sl1 sl2 sl3
        object.put("node",Config.getNode()); // 0 1 2
        object.put("hostname",Config.getHostName()); // saturn mars neptune
        getLogger().fine("CallBack received for URL "+url);
        return object;
    }
    
}
