package net.coagulate.SL.SecondLifeAPI;

import net.coagulate.Core.Exceptions.System.SystemRemoteFailureException;
import net.coagulate.Core.Tools.ByteTools;
import net.coagulate.Core.Tools.Crypto;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.Pages.HTML.State;
import net.coagulate.SL.SL;
import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

/**
 * @author Iain Price
 */
public abstract class SLAPI implements HttpRequestHandler {

	public Logger getLogger() {
		final String classname=getClass().getSimpleName();
		return SL.getLogger("SLAPI."+classname);
	}

	@Override
	public void handle(final HttpRequest req,
	                   @Nonnull final HttpResponse resp,
	                   final HttpContext hc) {
		final boolean debug=false;
		try {
			JSONObject content=new JSONObject();
			if (req instanceof HttpEntityEnclosingRequest) {
				final HttpEntityEnclosingRequest r=(HttpEntityEnclosingRequest) req;
				content=new JSONObject(ByteTools.convertStreamToString(r.getEntity().getContent()));
			}
			final State st=new State(req,resp,hc);
			final String shard=requireHeader(req,"X-SecondLife-Shard",st);
			st.put("slapi_shard",shard);
			st.put("slapi_region",requireHeader(req,"X-SecondLife-Region",st));
			st.put("slapi_ownername",optionalHeader(req,"X-SecondLife-Owner-Name",st));
			st.put("slapi_ownerkey",requireHeader(req,"X-SecondLife-Owner-Key",st));
			if (st.get("slapi_ownername")==null || st.get("slapi_ownername").isEmpty()) {
				final User userlookup=User.findOptional(st.get("slapi_ownerkey"));
				if (userlookup!=null) {
					final String username=userlookup.getName();
					if (username!=null) { st.put("slapi_ownername",username); }
				}
			}
			st.put("slapi_objectname",requireHeader(req,"X-SecondLife-Object-Name",st));
			final String objectkey=requireHeader(req,"X-SecondLife-Object-Key",st);
			st.put("slapi_objectkey",objectkey);
			st.put("slapi_objectvelocity",requireHeader(req,"X-SecondLife-Local-Velocity",st));
			st.put("slapi_objectrotation",requireHeader(req,"X-SecondLife-Local-Rotation",st));
			st.put("slapi_objectposition",requireHeader(req,"X-SecondLife-Local-Position",st));
			if (!"Production".equalsIgnoreCase(shard)) {
				SL.getLogger(getClass().getSimpleName()).severe("INCORRECT SHARD : "+objectDump(st));
				resp.setStatusCode(HttpStatus.SC_FORBIDDEN);
				return;
			}
			if (needsDigest()) {
				final String digest=content.optString("digest");
				if (objectkey==null) {
					SL.getLogger().log(SEVERE,"No object owner key provided to Second Life API");
					resp.setStatusCode(HttpStatus.SC_FORBIDDEN);
					return;
				}
				if (digest==null) {
					SL.getLogger().log(SEVERE,"No digest provided to Second Life API");
					resp.setStatusCode(HttpStatus.SC_FORBIDDEN);
					return;
				}
				final String timestamp=content.optString("timestamp");
				if (timestamp==null) {
					SL.getLogger().log(SEVERE,"No timestamp provided to Second Life API");
					resp.setStatusCode(HttpStatus.SC_FORBIDDEN);
					return;
				}
				// not replay attack proof :(  still, the wires are /reasonably/ secure.  maybe later.  hmm
				int timestampoffset=UnixTime.getUnixTime()-Integer.parseInt(timestamp);
				if (timestampoffset<0) { timestampoffset=-timestampoffset; }
				if (timestampoffset>300) {
					SL.getLogger().log(SEVERE,"Timestamp deviates by more than 300 seconds");
					resp.setStatusCode(HttpStatus.SC_FORBIDDEN);
					return;
				}
				final String theirobjectkey=content.optString("objectkey");
				if (theirobjectkey!=null && !theirobjectkey.isEmpty()) {
					if (!theirobjectkey.equals(objectkey)) {
						SL.getLogger().log(SEVERE,"Object key mismatch - headers generated "+objectkey+" but they think it's "+theirobjectkey);
					}
				}
				final String targetdigest=Crypto.SHA1(objectkey+timestamp+"***REMOVED***");
				if (!targetdigest.equalsIgnoreCase(digest)) {
					SL.getLogger().log(SEVERE,"Incorrect digest provided to Second Life API");
					resp.setStatusCode(HttpStatus.SC_FORBIDDEN);
					return;
				}
			}
			final JSONObject response=handleJSON(content,st);
			resp.setEntity(new StringEntity(response.toString(),ContentType.APPLICATION_JSON));
			resp.setStatusCode(HttpStatus.SC_OK);
		}
		catch (@Nonnull final Exception ex) {
			SL.getLogger().log(WARNING,"PageHandler",ex);
			resp.setStatusCode(HttpStatus.SC_OK);
			final JSONObject object=new JSONObject();
			object.put("error","Internal error during SL API parser");
			resp.setEntity(new StringEntity(object.toString(),ContentType.APPLICATION_JSON));
		}
	}

	protected void checkVersion(@Nonnull final JSONObject object,
	                            @Nonnull final String match,
	                            @Nonnull final State st) {
		String version="NULL";
		if (object.has("version")) {
			version=object.getString("version");
			if (match.equals(version)) { return; }
		}
		SL.getLogger(getClass().getSimpleName()).fine("Version mismatch : "+match+">"+version+" : "+objectDump(st));
	}

	private String requireHeader(final HttpRequest req,
	                             final String header,
	                             final State st) {
		return getHeader(true,req,header,st);
	}
	private String optionalHeader(final HttpRequest req,
	                             final String header,
	                             final State st) {
		return getHeader(false,req,header,st);
	}
	private String getHeader(final boolean mandatory,
	                         final HttpRequest req,
	                         final String header,
	                         final State st) {
		final Header[] headerset=req.getHeaders(header);
		if (headerset.length==0) {
			if (!mandatory) { return null; }
			final SystemRemoteFailureException e=new SystemRemoteFailureException("Mandatory data was not supplied to SL API processor");
			SL.report("Missing mandatory header "+header,e,st);
			throw e;
		}
		if (headerset.length>1) {
			final SystemRemoteFailureException e=new SystemRemoteFailureException("Too much mandatory data was supplied to SL API processor");
			SL.report("Excessive mandatory header "+header,e,st);
			throw e;
		}
		return headerset[0].getValue();
	}

	@Nonnull
	protected abstract JSONObject handleJSON(JSONObject object,
	                                         State st);

	protected boolean needsDigest() { return true; }

	@Nonnull
	String objectDump(@Nonnull final State st) {
		return "'"+st.get("slapi_objectname")+"' ["+st.get("slapi_objectkey")+"] owned by "+st.get("slapi_ownername")+" ["+st.get("slapi_ownerkey")+"] at "+st.get(
				"slapi_region")+" "+st.get("slapi_objectposition");
	}
}
