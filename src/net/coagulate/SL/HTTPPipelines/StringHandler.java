package net.coagulate.SL.HTTPPipelines;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.SL.Config;
import net.coagulate.SL.SL;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;

/** Process a page into a String input/output
 *
 * @author Iain Price
 */
public abstract class StringHandler extends Handler {
    private static final boolean DEBUG_PARAMS=false;
   @Override
    public void handleContent(HttpRequest req, HttpResponse resp, HttpContext hc,State state) {
        try {
            String content="<p><b>WEIRD INTERNAL LOGIC ERROR</b></p>";
            try { content=handleString(); }
            catch (UserException ue) {
                SL.getLogger().log(WARNING,"User exception propagated to handler",ue);
                content="<p>Exception: "+ue.getLocalizedMessage()+"</p>";
            }
            resp.setEntity(new StringEntity(pageHeader()+content+pageFooter(),ContentType.TEXT_HTML));
        }
        catch (Exception ex) {
            SL.getLogger().log(SEVERE,"Unexpected exception thrown in page handler",ex);
            resp.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            resp.setEntity(new StringEntity("<html><body><pre><b>500 - Internal Server Error</b></pre><p>Internal Exception, see debug logs</p></body></html>",ContentType.TEXT_HTML));
        }
    }    
    
    public int getReturnStatus() {
        return HttpStatus.SC_OK;
    }

    protected abstract String handleString();
    
    protected String pageHeader() {
        State state=State.get();
        String r="<html><head><title>Coagulate SL Services</title>"
                + "<link rel=\"shortcut icon\" href=\"/resources/icon-cluster.png\">"
                + "</head><body>"
                + "<h1 align=center>Coagulate SL Services</h1><p><hr>";
        r+="<table width=100%><tr width=100%><td align=left width=400px>"
                + "Greetings";
        if (state.user()!=null) { r+=",&nbsp;"+state.user().getUsername().replaceAll(" ", "&nbsp;"); }        
        r+="</td><td align=center>";
        r+= "<a href=\"/\">[&nbsp;Home&nbsp;]</a>";
        r+="</td><td align=right width=400px>";
        r+="<a href=\"/Info\">[Info]</a>"+"&nbsp;&nbsp;&nbsp;";
        if (state.user()!=null) {
            r+="<a href=\"/Billing\">[&nbsp;Billing&nbsp;(L$"+state.user().balance()+")&nbsp;]</a>"
                    + "&nbsp;&nbsp;&nbsp;"
                    + "<a href=\"/Account\">[&nbsp;Account&nbsp;]</a>"
                    + "&nbsp;&nbsp;&nbsp;"
                    + "<a href=\"/Logout\">[&nbsp;Logout&nbsp;]</a>"
                    + "&nbsp;&nbsp;&nbsp;"
                    + "</span>";
        }
        r+="</td></tr></table>";
        r+= "<hr></p>";
        return r;
    }
    protected String pageFooter() {
        String ret="<div style='position:absolute;bottom:5;right:5;left:5;'><hr>";
        ret+=(SL.DEV?"DEVELOPMENT":"Production");
        ret+=" // "+Config.getHostName();
        ret+="<span style='display:block;float:right;'>(C) Iain Maltz @ Second Life</span></div></body></html>";
        return ret;
    }
}
