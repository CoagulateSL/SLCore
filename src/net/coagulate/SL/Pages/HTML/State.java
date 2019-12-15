package net.coagulate.SL.Pages.HTML;

import net.coagulate.Core.Tools.DumpableState;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.SL.Data.Session;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.SL;
import org.apache.http.*;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * General purpose state storage
 *
 * @author Iain Price
 */

@SuppressWarnings("deprecation")
public class State extends DumpableState {
	private final HttpRequest request;
	private final HttpResponse response;
	private final HttpContext httpcontext;
	// We love general purpose KV maps.  more than one of them.
	private final Map<String, Map<String, String>> maps = new HashMap<>();
	private int returnstatus = HttpStatus.SC_OK;
	@Nullable
	private String sessionid = null;
	// hmm, getting a bit complex here
	@Nullable
	private User user = null;
	@Nullable
	private Session session = null;

	// We love HTTP :P
	public State(HttpRequest request, HttpResponse response, HttpContext httpcontext) {
		this.request = request;
		this.response = response;
		this.httpcontext = httpcontext;
	}

	public HttpRequest request() { return request; }

	public HttpResponse response() { return response; }

	public HttpContext context() { return httpcontext; }

	public void status(int httpstatus) { returnstatus = httpstatus; }

	public int status() { return returnstatus; }

	public void sessionId(String set) {
		sessionid = set;
		loadSession();
	}

	@Nonnull
	public String sessionId() {
		if (sessionid==null) { throw new SystemException("Session ID is null"); }
		return sessionid;
	}

	private Map<String, String> getMap(String mapname) {
		if (!maps.containsKey(mapname)) {
			maps.put(mapname, new HashMap<>());
		}
		return maps.get(mapname);
	}

	public String get(String mapname, String key, String defaultvalue) {
		if (!getMap(mapname).containsKey(key)) { return defaultvalue; }
		return getMap(mapname).get(key);
	}

	public String get(String mapname, String key) { return get(mapname, key, null); }

	public void delete(String mapname, String key) { getMap(mapname).remove(key); }

	public void put(String mapname, String key, String value) { getMap(mapname).put(key, value); }

	public void putMap(String mapname, Map<String, String> map) { maps.put(mapname, map); }

	// WELL KNOWN MAPS
	// parameters - get/post data
	// cookies - header cookie data
	// parameters is assumed to be default :P legacy behaviour, kinda
	public String get(String key) { return get("parameters", key, ""); }

	public void put(String key, String value) { put("parameters", key, value); }

	@Nullable
	public User userNullable() { return user; }

	@Nonnull
	public User user() {
		if (user==null) { throw new SystemException("There is no user in the SL HTML State"); }
		return user;
	}

	public void user(@Nonnull User user) {
		// got a session?
		if (session == null) {
			session = Session.create(user);
			sessionid = session.token();
		} else { session.setUser(user); }
		this.user = user;
	}
	@SuppressWarnings("deprecation")
	public String getClientIP() {
		//try {
		//HttpInetConnection connection = (HttpInetConnection) httpcontext.getAttribute(ExecutionContext.HTTP_CONNECTION);
		//InetAddress ia = connection.getRemoteAddress();
		//return ia.getCanonicalHostName()+" / "+ia.getHostAddress();
		//} catch (Exception e) { SL.getLogger().log(WARNING,"Exception getting client address",e); }
		Header[] headers = request.getHeaders("X-Forwarded-For");
		if (headers.length == 0) {
			try {
				HttpInetConnection connection = (HttpInetConnection) httpcontext.getAttribute(ExecutionContext.HTTP_CONNECTION);
				InetAddress ia = connection.getRemoteAddress();
				return "DIRECT:" + ia.getHostAddress();
			} catch (Exception e) { SL.getLogger().log(Level.WARNING,"Exception getting client address",e); return "UNKNOWN"; }
		}
		if (headers.length > 1) { return "MULTIPLE?:" + headers[0].getValue(); }
		return headers[0].getValue();
	}

	public void logout() {
		if (sessionid != null) {
			Session.get(sessionid).logout();
		}
		sessionid = null;
		user = null;
	}

	void loadSession() {
		session = null;
		if (sessionid == null || sessionid.isEmpty() || "none".equalsIgnoreCase(sessionid)) { return; }
		session = Session.get(sessionid);
		//System.out.println("Loaded session id "+sessionid+" and got "+session);
		if (session != null) { user = session.user(); } else { sessionid = null; }
	}

	@Nonnull
	@Override
	protected String dumpAdditionalStateToHtml() {
		return "";
	}
}
