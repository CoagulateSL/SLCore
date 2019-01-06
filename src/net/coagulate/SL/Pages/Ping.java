package net.coagulate.SL.Pages;

import net.coagulate.SL.Config;
import net.coagulate.SL.HTTPPipelines.Handler;
import net.coagulate.SL.HTTPPipelines.PageMapper.Url;
import net.coagulate.SL.HTTPPipelines.State;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

/**
 *
 * @author Iain Price
 */
public class Ping extends Handler {

    @Url("/Ping")
    public Ping(){super();}
    @Override
    protected HttpEntity handleContent(State state) {
        String response="nodename:"+Config.getNodeName()+"\nnode:"+Config.getNode()+"\nhostname:"+Config.getHostName();
        return new StringEntity(response, ContentType.TEXT_PLAIN);
    }
   
}
