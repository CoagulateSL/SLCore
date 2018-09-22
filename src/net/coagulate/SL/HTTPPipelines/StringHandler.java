package net.coagulate.SL.HTTPPipelines;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.coagulate.SL.Log;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

/** Process a page into a String input/output
 *
 * @author Iain Price
 */
public abstract class StringHandler implements HttpRequestHandler {
   @Override
    public void handle(HttpRequest req, HttpResponse resp, HttpContext hc) {
        try {
            Map<String,String> parameters=new HashMap<>();
            if (req instanceof HttpEntityEnclosingRequest) {
                HttpEntityEnclosingRequest r=(HttpEntityEnclosingRequest) req;
                List<NameValuePair> map = URLEncodedUtils.parse(r.getEntity());
                for (NameValuePair kv:map) {
                    parameters.put(kv.getName(),kv.getValue());
                }
            }
            State state=State.create();
            state.request=req;
            state.response=resp;
            state.httpcontext=hc;
            state.parameters=parameters;
            Map<String,String> cookiemap=new HashMap<>();
            Header cookies = req.getFirstHeader("Cookie");
            if (cookies!=null) {
                for (HeaderElement cookieelement:cookies.getElements()) {
                    cookiemap.put(cookieelement.getName(), cookieelement.getValue());
                }
            }
            state.cookies=cookiemap;
            state.setSessionId(cookiemap.get("coagulateslsessionid"));
            String content=handleString();
            resp.setEntity(new StringEntity(pageHeader()+content+pageFooter(),ContentType.TEXT_HTML));
            if (state.sessionid!=null) {
                if (!state.sessionid.equals(cookiemap.get("coagulateslsessionid"))) {
                    resp.addHeader("Set-Cookie","coagulateslsessionid="+state.sessionid+"; HttpOnly; Path=/; Domain=coagulate.net;");
                }
            } else { resp.addHeader("Set-Cookie","coagulateslsessionid=; HttpOnly; Path=/; Domain=coagulate.net; expires=Thu, 01 Jan 1970 00:00:00 GMT"); }
            resp.setStatusCode(HttpStatus.SC_OK);
            return;
        } catch (Exception ex) {
            Log.warn("StringHandler","Unexpected exception thrown in page handler",ex);
            resp.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            resp.setEntity(new StringEntity("<html><body><pre><b>500 - Internal Server Error</b></pre><p>Internal Exception, see debug logs</p></body></html>",ContentType.TEXT_HTML));
            return;
        }
    }    

    protected abstract String handleString();
    
    protected String pageHeader() {
        State state=State.get();
        String r="<html><head><title>Coagulate SL Services</title></head><body>"
                + "<h1 align=center>Coagulate SL Services</h1><p><hr>";
        r+="<table width=100%><tr width=100%><td align=left width=300px>"
                + "Greetings";
        if (state.user()!=null) { r+=", "+state.user().getUsername(); }        
        r+="</td><td align=center>";
        r+= "<a href=\"/App1\">[ App1 ]</a>"
                + "&nbsp;&nbsp;&nbsp;"
                + "<a href=\"\">[ App2 ]</a>"
                + "</span>";        
        r+="</td><td align=right width=300px>";
        if (state.user()!=null) {
            r+="<a href=\"/SetPassword\">[ Set Password ]</a>"
                    + "&nbsp;&nbsp;&nbsp;"
                    + "<a href=\"\">[ Billing ]</a>"
                    + "&nbsp;&nbsp;&nbsp;"
                    + "<a href=\"/Logout\">[ Logout ]</a>"
                    + "&nbsp;&nbsp;&nbsp;"
                    + "</span>";
        }
        r+="</td></tr></table>";
        r+= "<hr></p>";
        return r;
    }
    protected String pageFooter() {
        return "<div style='position:absolute;bottom:5;right:5;left:5;'><hr><span style='display:block;float:right;'>(C) Iain Maltz @ Second Life</span></div></body></html>";
    }
}
