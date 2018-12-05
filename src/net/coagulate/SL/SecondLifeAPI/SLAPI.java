package net.coagulate.SL.SecondLifeAPI;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import net.coagulate.Core.Tools.ByteTools;
import net.coagulate.Core.Tools.Crypto;
import net.coagulate.Core.Tools.UnixTime;
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

    String objectkey=null;
    String shard=null;
    String region=null;
    String ownername=null;
    String ownerkey=null;
    String objectname=null;
    String objectvelocity=null;
    String objectrotation=null;
    String objectposition=null;
    public Logger getLogger() {
        String classname=this.getClass().getSimpleName();
        return SL.getLogger("SLAPI."+classname);
    }
   @Override
    public void handle(HttpRequest req, HttpResponse resp, HttpContext hc) {
        try {
            JSONObject content=new JSONObject();
            if (req instanceof HttpEntityEnclosingRequest) {
                HttpEntityEnclosingRequest r=(HttpEntityEnclosingRequest) req;
                content=new JSONObject(ByteTools.convertStreamToString(r.getEntity().getContent()));
            }
            shard=req.getHeaders("X-SecondLife-Shard")[0].getValue();
            region=req.getHeaders("X-SecondLife-Region")[0].getValue();
            ownername=req.getHeaders("X-SecondLife-Owner-Name")[0].getValue();
            ownerkey=req.getHeaders("X-SecondLife-Owner-Key")[0].getValue();
            objectname=req.getHeaders("X-SecondLife-Object-Name")[0].getValue();
            objectkey=req.getHeaders("X-SecondLife-Object-Key")[0].getValue();
            objectvelocity=req.getHeaders("X-SecondLife-Local-Velocity")[0].getValue();
            objectrotation=req.getHeaders("X-SecondLife-Local-Rotation")[0].getValue();
            objectposition=req.getHeaders("X-SecondLife-Local-Position")[0].getValue();
            if (!shard.equalsIgnoreCase("Production")) { 
                SL.getLogger(this.getClass().getSimpleName()).severe("INCORRECT SHARD : "+objectDump());
                resp.setStatusCode(HttpStatus.SC_FORBIDDEN); return;
            }
            if (needsDigest()) {
                String digest=content.optString("digest");
                if (objectkey==null) {
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
                // not replay attack proof :(  still, the wires are /reasonably/ secure.  maybe later.  hmm
                int timestampoffset=UnixTime.getUnixTime()-Integer.parseInt(timestamp);
                if (timestampoffset<0) { timestampoffset=-timestampoffset; }
                if (timestampoffset>300) { SL.getLogger().log(SEVERE,"Timestamp deviates by more than 300 seconds"); resp.setStatusCode(HttpStatus.SC_FORBIDDEN); return; }
                String targetdigest=Crypto.SHA1(objectkey+timestamp+"***REMOVED***");
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

    protected void checkVersion(JSONObject object, String match) {
        if (object.has("version")) {
            if (match.equals(object.getString("version"))) { return; }
        }
        SL.getLogger(this.getClass().getSimpleName()).fine("Version mismatch : "+objectDump());
    }
    

    protected abstract JSONObject handleJSON(JSONObject object);
    protected boolean needsDigest() { return true; }

    String objectDump() {
        return "'"+objectname+"' ["+objectkey+"] owned by "+ownername+" ["+ownerkey+"] at "+region+" "+objectposition;
    }
}
