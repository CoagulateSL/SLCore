package net.coagulate.SL.HTTPPipelines;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerMapper;

/**
 *
 * @author Iain Price
 */
public class PageMapper implements HttpRequestHandlerMapper {

    public Logger logger;
    
    Map<String,HttpRequestHandler> prefixes=new HashMap<>();
    Map<String,HttpRequestHandler> exact=new HashMap<>();
            
    public void exact(String url,HttpRequestHandler handler) { exact.put(url.toLowerCase(),handler); }
    public void prefix(String url,HttpRequestHandler handler) { prefixes.put(url.toLowerCase(),handler); }
            
    public PageMapper() {
        logger=Logger.getLogger(PageMapper.class.getCanonicalName());

        // GPHUD mappings
        net.coagulate.GPHUD.Interface.base="GPHUD";
        exact("/GPHUD/system",new net.coagulate.GPHUD.Interfaces.System.Interface());
        prefix("/GPHUD/",new net.coagulate.GPHUD.Interfaces.User.Interface());
        prefix("/GPHUD/hud/",new net.coagulate.GPHUD.Interfaces.HUD.Interface());
        
        
    }
    
    
    @Override
    public HttpRequestHandler lookup(HttpRequest req) {
        System.out.println("REQUEST:"+req.getRequestLine().getUri());
        String line=req.getRequestLine().getUri().toLowerCase();
        if (exact.containsKey(line)) { return exact.get(line); }
        String matchedprefix="";
        HttpRequestHandler matchedhandler=null;
        for (String prefix:prefixes.keySet()) {
            if (prefix.startsWith(line)) {
                if (prefix.length()>matchedprefix.length()) {
                    matchedprefix=prefix;
                    matchedhandler=prefixes.get(prefix);
                }
            }
        }
        return matchedhandler;
    }
    
}
