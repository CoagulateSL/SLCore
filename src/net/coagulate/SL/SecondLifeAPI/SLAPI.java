package net.coagulate.SL.SecondLifeAPI;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import net.coagulate.Core.Tools.ByteTools;
import net.coagulate.Core.Tools.Crypto;
import net.coagulate.SL.SL;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.json.JSONObject;

/**
 *
 * @author Iain Price
 */
public abstract class SLAPI implements HttpRequestHandler {

   @Override
    public void handle(HttpRequest req, HttpResponse resp, HttpContext hc) {
        try {
            JSONObject content=new JSONObject();
            String key=req.getHeaders("X-SecondLife-Object-Key")[0].getValue();
            if (req instanceof HttpEntityEnclosingRequest) {
                HttpEntityEnclosingRequest r=(HttpEntityEnclosingRequest) req;
                content=new JSONObject(ByteTools.convertStreamToString(r.getEntity().getContent()));
            }
            if (needsDigest()) {
                String digest=content.optString("digest");
                if (key==null) {
                    SL.getLogger().log(SEVERE,"No object owner key provided to Second Life API");
                    resp.setStatusCode(HttpStatus.SC_FORBIDDEN); return;
                }
                if (digest==null) {
                    SL.getLogger().log(SEVERE,"No digest provided to Second Life API");
                    resp.setStatusCode(HttpStatus.SC_FORBIDDEN); return;
                }
                String timestamp=content.optString("timestamp");
                if (timestamp==null) {
                    SL.getLogger().log(SEVERE,"No timestamp provided to Second Life API");
                    resp.setStatusCode(HttpStatus.SC_FORBIDDEN); return;
                }
                String targetdigest=Crypto.SHA1(key+timestamp+"***REMOVED***");
                if (!targetdigest.equalsIgnoreCase(digest)) {
                    SL.getLogger().log(SEVERE,"Incorrect digest provided to Second Life API");
                    resp.setStatusCode(HttpStatus.SC_FORBIDDEN); return;                
                }
            }   
            JSONObject response=handleJSON(content);
            resp.setEntity(new StringEntity(response.toString(),ContentType.APPLICATION_JSON));
            resp.setStatusCode(HttpStatus.SC_OK);
            return;
        } catch (Exception ex) {
            SL.getLogger().log(WARNING,"Unexpected exception thrown in page handler",ex);
            resp.setStatusCode(HttpStatus.SC_OK);
            JSONObject object=new JSONObject();
            object.put("error","Internal error: "+ex.toString());
            resp.setEntity(new StringEntity(object.toString(),ContentType.APPLICATION_JSON));
            return;
        }
    }     

    protected abstract JSONObject handleJSON(JSONObject object);
    protected boolean needsDigest() { return true; }
}
