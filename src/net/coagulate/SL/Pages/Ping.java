package net.coagulate.SL.Pages;

import net.coagulate.SL.Config;
import net.coagulate.SL.HTTPPipelines.Handler;
import net.coagulate.SL.HTTPPipelines.PageMapper.Url;
import net.coagulate.SL.HTTPPipelines.State;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;

/**
 *
 * @author Iain Price
 */
public class Ping extends Handler {

    @Url("/Ping")
    public Ping(){super();}
    @Override
    protected void handleContent(HttpRequest req, HttpResponse resp, HttpContext hc,State state) {
        String response="nodename:"+Config.getNodeName()+"\nnode:"+Config.getNode()+"\nhostname:"+Config.getHostName();
        resp.setEntity(new StringEntity(response, ContentType.TEXT_PLAIN));
    }
    
}
