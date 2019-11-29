package net.coagulate.SL.HTTPPipelines;

import net.coagulate.SL.Pages.HTML.State;
import net.coagulate.SL.SL;
import org.apache.http.*;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

/**
 * Process a page into a String input/output
 *
 * @author Iain Price
 */
public abstract class Handler implements HttpRequestHandler {
	private static final boolean DEBUG_PARAMS = false;

	@Override
	public void handle(HttpRequest req, HttpResponse resp, HttpContext hc) {
		try {
			Map<String, String> parameters = new HashMap<>();
			List<NameValuePair> uriparams = URLEncodedUtils.parse(new URI(req.getRequestLine().getUri()), StandardCharsets.UTF_8);
			for (NameValuePair up : uriparams) {
				parameters.put(up.getName(), up.getValue());
				if (DEBUG_PARAMS) {
					System.out.println("Imported URI parameter '" + up.getName() + "'='" + up.getValue() + "'");
				}
			}


			if (req instanceof HttpEntityEnclosingRequest) {
				HttpEntityEnclosingRequest r = (HttpEntityEnclosingRequest) req;
				List<NameValuePair> map = URLEncodedUtils.parse(r.getEntity());
				for (NameValuePair kv : map) {
					parameters.put(kv.getName(), kv.getValue());
					if (DEBUG_PARAMS) {
						System.out.println("Imported POST parameter '" + kv.getName() + "'='" + kv.getValue() + "'");
					}
				}
			}
			State state = new State(req, resp, hc);
			state.putMap("parameters", parameters);
			Map<String, String> cookiemap = new HashMap<>();
			for (Header header : req.getHeaders("Cookie")) {
				for (String component : header.getValue().split(";")) {
					String[] kv = component.split("=");
					if (kv.length != 2) {
						SL.getLogger().log(Level.WARNING, "Unusual cookie element to parse in line " + header.getValue() + " piece " + component);
					} else {
						//System.out.println(kv[0]+"="+kv[1]);
						cookiemap.put(kv[0].trim(), kv[1].trim());
					}
				}
			}
			state.putMap("cookies", cookiemap);
			state.sessionId(cookiemap.get("coagulateslsessionid"));

			resp.setEntity(handleContent(state));

			if (state.sessionId() != null && !state.sessionId().isEmpty() && !"none".equalsIgnoreCase(state.sessionId())) {
				if (!state.sessionId().equals(cookiemap.get("coagulateslsessionid"))) {
					resp.addHeader("Set-Cookie", "coagulateslsessionid=" + state.sessionId() + "; HttpOnly; Path=/; Secure;");
				}
			} else {
				resp.addHeader("Set-Cookie", "coagulateslsessionid=none; HttpOnly; Path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT; Secure;");
			}
			resp.setStatusCode(state.status());

			return;
		} catch (URISyntaxException use) { // dont log the exception because we don't want the mail.  this is probably a script kiddie hack attempt that doesn't work
			SL.getLogger().log(WARNING, "Unexpected exception thrown in page handler");
			resp.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			resp.setEntity(new StringEntity("<html><body><pre><b>500 - Internal Server Error</b></pre><p>Whatever you're trying to do is illegal.</p></body></html>", ContentType.TEXT_HTML));
			return;
		} catch (Exception ex) {
			SL.getLogger().log(SEVERE, "Unexpected exception thrown in page handler", ex);
			resp.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			resp.setEntity(new StringEntity("<html><body><pre><b>500 - Internal Server Error</b></pre><p>Internal Exception, see debug logs</p></body></html>", ContentType.TEXT_HTML));
			return;
		}
	}

	protected abstract HttpEntity handleContent(State state);
}
