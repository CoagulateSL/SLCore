package net.coagulate.SL.HTTPPipelines;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Tools.ClassTools;
import net.coagulate.SL.Pages.FourZeroFour;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
	private static final boolean DEBUG=false;
	public final Logger logger;
	final Map<String,HttpRequestHandler> prefixes=new HashMap<>();
	final Map<String,HttpRequestHandler> exact=new HashMap<>();

	public PageMapper() {
		logger=Logger.getLogger(PageMapper.class.getCanonicalName());

		// GPHUD mappings
		net.coagulate.GPHUD.Interface.base="GPHUD";
		exact("/GPHUD/system",new net.coagulate.GPHUD.Interfaces.System.Interface());
		prefix("/GPHUD/",new net.coagulate.GPHUD.Interfaces.User.Interface());
		prefix("/Rental",new net.coagulate.LSLR.HttpReceiver());
		prefix("/rentals-scijp2",new net.coagulate.LSLR.HttpReceiver());
		prefix("/rentalavailability-scijp2",new net.coagulate.LSLR.HttpReceiver());
		// SL pages
		int count=0;
		for (final Constructor<?> c: ClassTools.getAnnotatedConstructors(Url.class)) {
			final String url=c.getAnnotation(Url.class).value();
			count++;
			try {
				exact(url,(HttpRequestHandler) c.newInstance());
			}
			catch (@Nonnull final InstantiationException|IllegalAccessException|IllegalArgumentException|InvocationTargetException ex) {
				logger.log(Level.SEVERE,"URL Annotated constructor in class "+c.getDeclaringClass().getCanonicalName()+" failed instansiation:"+ex.getLocalizedMessage(),ex);
			}
		}
		logger.log(Level.FINE,"Loaded "+count+" exact URI handlers");
		count=0;
		for (final Constructor<?> c: ClassTools.getAnnotatedConstructors(Prefix.class)) {
			final String url=c.getAnnotation(Prefix.class).value();
			count++;
			try {
				prefix(url,(HttpRequestHandler) c.newInstance());
			}
			catch (@Nonnull final InstantiationException|IllegalAccessException|IllegalArgumentException|InvocationTargetException ex) {
				logger.log(Level.SEVERE,
				           "Prefix URL Annotated constructor in class "+c.getDeclaringClass().getCanonicalName()+" failed instansiation:"+ex.getLocalizedMessage(),
				           ex
				          );
			}
		}
		logger.log(Level.FINE,"Loaded "+count+" prefix URI handlers");
	}

	// ---------- INSTANCE ----------
	public void exact(@Nonnull final String url,
	                  final HttpRequestHandler handler) {
		if (DEBUG) { System.out.println("Registering exact '"+url+"'"); }
		if (exact.containsKey(url.toLowerCase())) {
			throw new SystemImplementationException("Duplicate EXACT URL registration for '"+url+"'");
		}
		exact.put(url.toLowerCase(),handler);
	}

	public void prefix(@Nonnull final String url,
	                   final HttpRequestHandler handler) {
		if (DEBUG) { System.out.println("Registering prefix '"+url+"'"); }
		if (prefixes.containsKey(url.toLowerCase())) {
			throw new SystemImplementationException("Duplicate PREFIX URL registration for '"+url+"'");
		}
		prefixes.put(url.toLowerCase(),handler);
	}

	@Nullable
	@Override
	public HttpRequestHandler lookup(@Nonnull final HttpRequest req) {
		if (DEBUG) { System.out.println("REQUEST URI:"+req.getRequestLine().getUri()); }
		final String line=req.getRequestLine().getUri().toLowerCase();
		if (exact.containsKey(line)) {
			if (DEBUG) { System.out.println("Exact match "+exact.get(line).getClass().getCanonicalName()); }
			return exact.get(line);
		}
		else {
			if (DEBUG) { System.out.println("Exact match against "+exact.size()+" elements returned nothing"); }
		}
		for (final String s: exact.keySet()) {
			if (DEBUG) { System.out.println(s); }
		}
		String matchedprefix="";
		HttpRequestHandler matchedhandler=null;
		for (final String prefix: prefixes.keySet()) {
			if (line.startsWith(prefix)) {
				if (prefix.length()>matchedprefix.length()) {
					matchedprefix=prefix;
					matchedhandler=prefixes.get(prefix);
				}
			}
		}
		if (DEBUG) {
			if (matchedhandler!=null) {
				System.out.println("Prefix match "+matchedhandler.getClass().getCanonicalName());
			}
			else {System.out.println("Prefix match returned null match, this is now a 404");}
		}
		if (matchedhandler==null) {
			logger.log(Level.FINE,"Requested URI '{0}' was not mapped to a page - returning 404.",line);
			return new FourZeroFour(line);
		}
		return matchedhandler;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.CONSTRUCTOR)
	public @interface Url {
		// ---------- INSTANCE ----------
		@Nonnull String value();
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.CONSTRUCTOR)
	public @interface Prefix {
		// ---------- INSTANCE ----------
		@Nonnull String value();
	}

}
