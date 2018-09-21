package net.coagulate.SL.SecondLifeAPI;

import net.coagulate.SL.Tools;
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
            if (req instanceof HttpEntityEnclosingRequest) {
                HttpEntityEnclosingRequest r=(HttpEntityEnclosingRequest) req;
                content=new JSONObject(Tools.convertStreamToString(r.getEntity().getContent()));
            }
            JSONObject response=handleJSON(content);
            resp.setEntity(new StringEntity(response.toString(),ContentType.APPLICATION_JSON));
            resp.setStatusCode(HttpStatus.SC_OK);
            return;
        } catch (Exception ex) {
            net.coagulate.SL.Log.warn("StringHandler","Unexpected exception thrown in page handler",ex);
            resp.setStatusCode(HttpStatus.SC_OK);
            JSONObject object=new JSONObject();
            object.put("error","Internal error: "+ex.toString());
            resp.setEntity(new StringEntity(object.toString(),ContentType.APPLICATION_JSON));
            return;
        }
    }     

    protected abstract JSONObject handleJSON(JSONObject object);
}
