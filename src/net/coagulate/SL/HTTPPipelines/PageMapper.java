package net.coagulate.SL.HTTPPipelines;

import net.coagulate.Core.Tools.ClassTools;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.SL.Pages.FourZeroFour;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerMapper;

import java.lang.annotation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Iain Price
 */
public final class PageMapper implements HttpRequestHandlerMapper {
	private static final boolean DEBUG = false;
	public Logger logger;
	Map<String, HttpRequestHandler> prefixes = new HashMap<>();
	Map<String, HttpRequestHandler> exact = new HashMap<>();

	public PageMapper() {
		logger = Logger.getLogger(PageMapper.class.getCanonicalName());

		// GPHUD mappings
		net.coagulate.GPHUD.Interface.base = "GPHUD";
		exact("/GPHUD/system", new net.coagulate.GPHUD.Interfaces.System.Interface());
		prefix("/GPHUD/", new net.coagulate.GPHUD.Interfaces.User.Interface());
		prefix("/GPHUD/hud/", new net.coagulate.GPHUD.Interfaces.HUD.Interface());
		prefix("/Rental", new net.coagulate.LSLR.HttpReceiver());
		prefix("/rentals-scijp2", new net.coagulate.LSLR.HttpReceiver());
		prefix("/rentalavailability-scijp2", new net.coagulate.LSLR.HttpReceiver());
		// SL pages
		int count = 0;
		for (Constructor c : ClassTools.getAnnotatedConstructors(Url.class)) {
			String url = ((Url) (c.getAnnotation(Url.class))).value();
			count++;
			try {
				exact(url, (HttpRequestHandler) c.newInstance());
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
				logger.log(Level.SEVERE, "URL Annotated constructor in class " + c.getDeclaringClass().getCanonicalName() + " failed instansiation:" + ex.getLocalizedMessage(), ex);
			}
		}
		logger.log(Level.FINE, "Loaded " + count + " exact URI handlers");
		count = 0;
		for (Constructor c : ClassTools.getAnnotatedConstructors(Prefix.class)) {
			String url = ((Prefix) (c.getAnnotation(Prefix.class))).value();
			count++;
			try {
				prefix(url, (HttpRequestHandler) c.newInstance());
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
				logger.log(Level.SEVERE, "Prefix URL Annotated constructor in class " + c.getDeclaringClass().getCanonicalName() + " failed instansiation:" + ex.getLocalizedMessage(), ex);
			}
		}
		logger.log(Level.FINE, "Loaded " + count + " prefix URI handlers");
	}

	public void exact(String url, HttpRequestHandler handler) {
		if (DEBUG) { System.out.println("Registering exact '" + url + "'"); }
		if (exact.containsKey(url.toLowerCase())) {
			throw new SystemException("Duplicate EXACT URL registration for '" + url + "'");
		}
		exact.put(url.toLowerCase(), handler);
	}

	public void prefix(String url, HttpRequestHandler handler) {
		if (DEBUG) { System.out.println("Registering prefix '" + url + "'"); }
		if (prefixes.containsKey(url.toLowerCase())) {
			throw new SystemException("Duplicate PREFIX URL registration for '" + url + "'");
		}
		prefixes.put(url.toLowerCase(), handler);
	}

	@Override
	public HttpRequestHandler lookup(HttpRequest req) {
		if (DEBUG) { System.out.println("REQUEST URI:" + req.getRequestLine().getUri()); }
		String line = req.getRequestLine().getUri().toLowerCase();
		if (exact.containsKey(line)) {
			if (DEBUG) { System.out.println("Exact match " + exact.get(line).getClass().getCanonicalName()); }
			return exact.get(line);
		} else {
			if (DEBUG) { System.out.println("Exact match against " + exact.size() + " elements returned nothing"); }
		}
		for (String s : exact.keySet()) {
			if (DEBUG) { System.out.println(s); }
		}
		String matchedprefix = "";
		HttpRequestHandler matchedhandler = null;
		for (String prefix : prefixes.keySet()) {
			if (line.startsWith(prefix)) {
				if (prefix.length() > matchedprefix.length()) {
					matchedprefix = prefix;
					matchedhandler = prefixes.get(prefix);
				}
			}
		}
		if (DEBUG) {
			if (matchedhandler != null) {
				System.out.println("Prefix match " + matchedhandler.getClass().getCanonicalName());
			} else {System.out.println("Prefix match returned null match, this is now a 404");}
		}
		if (matchedhandler == null) {
			logger.log(Level.FINE, "Requested URI '{0}' was not mapped to a page - returning 404.", line);
			return new FourZeroFour(line);
		}
		return matchedhandler;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.CONSTRUCTOR)
	public @interface Url {
		public String value();
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.CONSTRUCTOR)
	public @interface Prefix {
		public String value();
	}

}
