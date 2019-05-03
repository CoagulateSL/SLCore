package net.coagulate.SL.Pages.HTML;

import net.coagulate.Core.Tools.DumpableState;
import net.coagulate.SL.Data.Session;
import net.coagulate.SL.Data.User;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpContext;

import java.util.HashMap;
import java.util.Map;

/**
 * General purpose state storage
 *
 * @author Iain Price
 */
public class State extends DumpableState {
	private final HttpRequest request;
	private final HttpResponse response;
	private final HttpContext httpcontext;
	// We love general purpose KV maps.  more than one of them.
	private final Map<String, Map<String, String>> maps = new HashMap<>();
	private int returnstatus = HttpStatus.SC_OK;
	private String sessionid = null;
	// hmm, getting a bit complex here
	private User user = null;
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

	public String sessionId() { return sessionid; }

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

	public User user() { return user; }

	public void user(User user) {
		// got a session?
		if (session == null) {
			session = Session.create(user);
			sessionid = session.token();
		} else { session.setUser(user); }
		this.user = user;
	}

	public String getClientIP() {
		//try {
		//HttpInetConnection connection = (HttpInetConnection) httpcontext.getAttribute(ExecutionContext.HTTP_CONNECTION);
		//InetAddress ia = connection.getRemoteAddress();
		//return ia.getCanonicalHostName()+" / "+ia.getHostAddress();
		//} catch (Exception e) { SL.getLogger().log(WARNING,"Exception getting client address",e); }
		Header[] headers = request.getHeaders("X-Forwarded-For");
		if (headers.length == 0) { return "UNKNOWN"; }
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
}
