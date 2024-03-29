package net.coagulate.SL;

import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.HTML.Container;
import net.coagulate.Core.HTML.Page;
import net.coagulate.Core.Tools.DumpableState;
import net.coagulate.SL.Data.Session;
import net.coagulate.SL.Data.User;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * General purpose state storage
 *
 * @author Iain Price
 */

public class State extends DumpableState {
	private static final Map<Thread,State> stateMap=new ConcurrentHashMap<>();
	private final Map<String,String> parameters=new TreeMap<>();
	// adhoc storage for one http entity.  used for outputting weird stuff like PNGs :P
	HttpEntity entity;
	private HttpRequest request;
	private HttpContext context;
	private Map<String,String> cookies;
	private Session session;
	private User user;
	private String clientIp="UNSET";
	private JSONObject in;
	private JSONObject                    out;
	private SystemImplementationException outset;
	private Page page;
	
	// We love HTTP :P
	private State() {
	}
	
	public static void maintenance() {
		try {
			for (final Thread entry: stateMap.keySet()) {
				if (!entry.isAlive()) {
					stateMap.remove(entry);
				}
			}
		} catch (final ConcurrentModificationException ignored) {
		}
	}
	
	public static synchronized State get() {
		final Thread thread=Thread.currentThread();
		if (!stateMap.containsKey(thread)) {
			stateMap.put(thread,new State());
		}
		return stateMap.get(thread);
	}
	
	public static void cleanup() {
		Page.cleanup();
		stateMap.remove(Thread.currentThread());
	}
	
	@Nonnull
	@Override
	protected String dumpAdditionalStateToHtml() {
		return "";
	}
	
	public void setupHTTP(final HttpRequest request,final HttpContext context) {
		if (this.request!=null) {
			throw new SystemImplementationException("HTTP request is already set up");
		}
		if (this.context!=null) {
			throw new SystemImplementationException("HTTP context is already set up");
		}
		this.request=request;
		this.context=context;
		setClientIP(SL.getClientIP(request,context));
	}
	
	@Nonnull
	public HttpContext context() {
		if (context==null) {
			throw new SystemImplementationException("No HTTP Context set up");
		}
		return context;
	}
	
	@Nonnull
	public String getUri() {
		return request().getRequestLine().getUri();
	}
	
	@Nonnull
	public HttpRequest request() {
		if (request==null) {
			throw new SystemImplementationException("No HTTP Request set up");
		}
		return request;
	}
	
	public void cookies(final Map<String,String> cookieMap) {
		if (cookies!=null) {
			throw new SystemImplementationException("Overwriting existing cookie map");
		}
		cookies=cookieMap;
	}
	
	public Map<String,String> cookies() {
		if (cookies==null) {
			throw new SystemImplementationException("Cookies map not initialised");
		}
		return cookies;
	}
	
	public void parameters(final Map<String,String> parametersMap) {
		parameters.putAll(parametersMap);
	}
	
	public Map<String,String> parameters() {
		return parameters;
	}
	
	public void logout() {
		if (session!=null) {
			session.logout();
		}
		session=null;
		user=null;
	}
	
	public void loadSession(final String sessionId) {
		session=null;
		if (sessionId==null||sessionId.isEmpty()||"none".equalsIgnoreCase(sessionId)) {
			return;
		}
		session=Session.get(sessionId);
		if (session!=null) {
			user=session.user();
		}
	}
	
	@Nullable
	public String sessionId() {
		if (session==null) {
			return null;
		}
		return session.token();
	}
	
	@Nullable
	public User userNullable() {
		return user;
	}
	
	@Nonnull
	public User user() {
		if (user==null) {
			throw new SystemConsistencyException("There is no user in the SL HTML State");
		}
		return user;
	}
	
	public void user(@Nonnull final User user) {
		// got a session?
		if (session==null) {
			session=Session.create(user);
		} else {
			session.setUser(user);
		}
		this.user=user;
	}
	
	public String getClientIP() {
		return clientIp;
	}

	private void setClientIP(final String s) {
		this.clientIp=s;
	}
	
	public void jsonIn(final JSONObject json) {
		if (in!=null) {
			throw new SystemImplementationException("JSON IN already set");
		}
		in=json;
	}
	
	public JSONObject jsonIn() {
		if (in==null) {
			throw new SystemImplementationException("JSON IN is null");
		}
		return in;
	}
	
	public void jsonOut(final JSONObject json) {
		if (out!=null) {
			throw new SystemImplementationException("JSON OUT already set",outset);
		}
		out=json;
		outset=new SystemImplementationException("Set here");
	}
	
	public JSONObject jsonOut() {
		if (out==null) {
			throw new SystemImplementationException("JSON OUT is null");
		}
		return out;
	}
	
	public void page(final Page page) {
		if (this.page!=null) {
			throw new SystemImplementationException("Page already set");
		}
		this.page=page;
	}
	
	@Nonnull
	public Container root() {
		return page().root();
	}
	
	@Nonnull
	public Page page() {
		if (page==null) {
			throw new SystemImplementationException("Page is not set");
		}
		return page;
	}
	
	public State add(final Container container) {
		page().add(container);
		return this;
	}
	
	public String parameter(final String name) {
		return parameters.get(name);
	}
	
	public void parameter(final String name,final String value) {
		parameters.put(name,value);
	}
	
	public void entity(final HttpEntity setTo) {
		if (entity!=null) {
			throw new SystemImplementationException("Http Entity is already set!");
		}
		entity=setTo;
	}
	
	public HttpEntity entity() {
		if (entity==null) {
			throw new SystemImplementationException("Http Entity is null");
		}
		return entity;
	}
	
	public JSONObject jsonOutNullable() {
		return out;
	}
	
}
