package net.coagulate.SL.SecondLifeAPI;

import net.coagulate.Core.Tools.ByteTools;
import net.coagulate.Core.Tools.Crypto;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.SL.Pages.HTML.State;
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

import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

/**
 * @author Iain Price
 */
public abstract class SLAPI implements HttpRequestHandler {

	public Logger getLogger() {
		String classname = this.getClass().getSimpleName();
		return SL.getLogger("SLAPI." + classname);
	}

	@Override
	public void handle(HttpRequest req, HttpResponse resp, HttpContext hc) {
		try {
			JSONObject content = new JSONObject();
			if (req instanceof HttpEntityEnclosingRequest) {
				HttpEntityEnclosingRequest r = (HttpEntityEnclosingRequest) req;
				content = new JSONObject(ByteTools.convertStreamToString(r.getEntity().getContent()));
			}
			State st = new State(req, resp, hc);
			String shard = req.getHeaders("X-SecondLife-Shard")[0].getValue();
			st.put("slapi_shard", shard);
			st.put("slapi_region", req.getHeaders("X-SecondLife-Region")[0].getValue());
			st.put("slapi_ownername", req.getHeaders("X-SecondLife-Owner-Name")[0].getValue());
			st.put("slapi_ownerkey", req.getHeaders("X-SecondLife-Owner-Key")[0].getValue());
			st.put("slapi_objectname", req.getHeaders("X-SecondLife-Object-Name")[0].getValue());
			String objectkey = req.getHeaders("X-SecondLife-Object-Key")[0].getValue();
			st.put("slapi_objectkey", objectkey);
			st.put("slapi_objectvelocity", req.getHeaders("X-SecondLife-Local-Velocity")[0].getValue());
			st.put("slapi_objectrotation", req.getHeaders("X-SecondLife-Local-Rotation")[0].getValue());
			st.put("slapi_objectposition", req.getHeaders("X-SecondLife-Local-Position")[0].getValue());
			if (!shard.equalsIgnoreCase("Production")) {
				SL.getLogger(this.getClass().getSimpleName()).severe("INCORRECT SHARD : " + objectDump(st));
				resp.setStatusCode(HttpStatus.SC_FORBIDDEN);
				return;
			}
			if (needsDigest()) {
				String digest = content.optString("digest");
				if (objectkey == null) {
					SL.getLogger().log(SEVERE, "No object owner key provided to Second Life API");
					resp.setStatusCode(HttpStatus.SC_FORBIDDEN);
					return;
				}
				if (digest == null) {
					SL.getLogger().log(SEVERE, "No digest provided to Second Life API");
					resp.setStatusCode(HttpStatus.SC_FORBIDDEN);
					return;
				}
				String timestamp = content.optString("timestamp");
				if (timestamp == null) {
					SL.getLogger().log(SEVERE, "No timestamp provided to Second Life API");
					resp.setStatusCode(HttpStatus.SC_FORBIDDEN);
					return;
				}
				// not replay attack proof :(  still, the wires are /reasonably/ secure.  maybe later.  hmm
				int timestampoffset = UnixTime.getUnixTime() - Integer.parseInt(timestamp);
				if (timestampoffset < 0) { timestampoffset = -timestampoffset; }
				if (timestampoffset > 300) {
					SL.getLogger().log(SEVERE, "Timestamp deviates by more than 300 seconds");
					resp.setStatusCode(HttpStatus.SC_FORBIDDEN);
					return;
				}
				String targetdigest = Crypto.SHA1(objectkey + timestamp + "***REMOVED***");
				if (!targetdigest.equalsIgnoreCase(digest)) {
					SL.getLogger().log(SEVERE, "Incorrect digest provided to Second Life API");
					resp.setStatusCode(HttpStatus.SC_FORBIDDEN);
					return;
				}
			}
			JSONObject response = handleJSON(content, st);
			resp.setEntity(new StringEntity(response.toString(), ContentType.APPLICATION_JSON));
			resp.setStatusCode(HttpStatus.SC_OK);
			return;
		} catch (Exception ex) {
			SL.getLogger().log(WARNING, "Unexpected exception thrown in page handler", ex);
			resp.setStatusCode(HttpStatus.SC_OK);
			JSONObject object = new JSONObject();
			object.put("error", "Internal error: " + ex.toString());
			resp.setEntity(new StringEntity(object.toString(), ContentType.APPLICATION_JSON));
			return;
		}
	}

	protected void checkVersion(JSONObject object, String match, State st) {
		String version = "NULL";
		if (object.has("version")) {
			version = object.getString("version");
			if (match.equals(version)) { return; }
		}
		SL.getLogger(this.getClass().getSimpleName()).fine("Version mismatch : " + match + ">" + version + " : " + objectDump(st));
	}


	protected abstract JSONObject handleJSON(JSONObject object, State st);

	protected boolean needsDigest() { return true; }

	String objectDump(State st) {
		return "'" + st.get("slapi_objectname") + "' [" + st.get("slapi_objectkey") + "] owned by " + st.get("slapi_ownername") + " [" + st.get("slapi_ownerkey") + "] at " + st.get("slapi_region") + " " + st.get("slapi_objectposition");
	}
}
