package net.coagulate.SL.HTTPPipelines;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.coagulate.Core.Tools.ClassTools;
import net.coagulate.SL.Pages.FourZeroFour;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerMapper;

/**
 *
 * @author Iain Price
 */
public class PageMapper implements HttpRequestHandlerMapper {
    private static final boolean DEBUG=false;
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Target(ElementType.CONSTRUCTOR)
    public @interface Url {
        public String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Target(ElementType.CONSTRUCTOR)
    public @interface Prefix {
        public String value();
    }
    
    
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
        // SL pages
        for (Constructor c:ClassTools.getAnnotatedConstructors(Url.class)) {
            String url=((Url)(c.getAnnotation(Url.class))).value();
            try {
                exact(url, (HttpRequestHandler) c.newInstance());
            } catch (InstantiationException|IllegalAccessException|IllegalArgumentException|InvocationTargetException ex) {
                logger.log(Level.SEVERE, "URL Annotated constructor in class "+c.getDeclaringClass().getCanonicalName()+" failed instansiation:"+ex.getLocalizedMessage(), ex);
            }
        }
        for (Constructor c:ClassTools.getAnnotatedConstructors(Prefix.class)) {
            String url=((Prefix)(c.getAnnotation(Prefix.class))).value();
            try {
                prefix(url, (HttpRequestHandler) c.newInstance());
            } catch (InstantiationException|IllegalAccessException|IllegalArgumentException|InvocationTargetException ex) {
                logger.log(Level.SEVERE, "Prefix URL Annotated constructor in class "+c.getDeclaringClass().getCanonicalName()+" failed instansiation:"+ex.getLocalizedMessage(), ex);
            }
        }
        
    }
    
    
    @Override
    public HttpRequestHandler lookup(HttpRequest req) {
        if (DEBUG) { System.out.println("REQUEST URI:"+req.getRequestLine().getUri()); }
        String line=req.getRequestLine().getUri().toLowerCase();
        if (exact.containsKey(line)) {
            if (DEBUG) { System.out.println("Exact match "+exact.get(line).getClass().getCanonicalName()); }
            return exact.get(line);
        }
        String matchedprefix="";
        HttpRequestHandler matchedhandler=null;
        for (String prefix:prefixes.keySet()) {
            if (line.startsWith(prefix)) {
                if (prefix.length()>matchedprefix.length()) {
                    matchedprefix=prefix;
                    matchedhandler=prefixes.get(prefix);
                }
            }
        }
        if (DEBUG) {
            if (matchedhandler!=null) {System.out.println("Prefix match "+matchedhandler.getClass().getCanonicalName());}
            else
            {System.out.println("Prefix match returned null match, this is now a 404");}
        }
        if (matchedhandler==null) {
            logger.fine("Requested URI '"+req.getRequestLine().getUri()+"' was not mapped to a page - returning 404.");
            return new FourZeroFour();
        }
        return matchedhandler;
    }

}
