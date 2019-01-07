package net.coagulate.SL.HTTPPipelines;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.SL.Pages.HTML.Raw;
import net.coagulate.SL.Pages.HTML.State;
import net.coagulate.SL.SL;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

/** Process a page into a String input/output
 *
 * @author Iain Price
 */
public abstract class StringHandler extends Handler {
    private static final boolean DEBUG_PARAMS=false;
   @Override
    public StringEntity handleContent(State state) {
        try {
            String content="<p><b>WEIRD INTERNAL LOGIC ERROR</b></p>";
            try { content=handleString(state); }
            catch (UserException ue) {
                SL.getLogger().log(WARNING,"User exception propagated to handler",ue);
                content="<p>Exception: "+ue.getLocalizedMessage()+"</p>";
            }
            return new StringEntity(new Page().add(new Raw(content)).toHtml(state),ContentType.TEXT_HTML);
        }
        catch (Exception ex) {
            SL.getLogger().log(SEVERE,"Unexpected exception thrown in page handler",ex);
            state.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            return new StringEntity("<html><body><pre><b>500 - Internal Server Error</b></pre><p>Internal Exception, see debug logs</p></body></html>",ContentType.TEXT_HTML);
        }
    }    
    protected abstract String handleString(State state);
    

}
