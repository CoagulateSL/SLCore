package net.coagulate.SL;

import net.coagulate.Core.Exceptions.User.UserInputValidationFilterException;
import net.coagulate.Core.Tools.MailTools;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Cookie;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.GPHUD;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.mail.MessagingException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

public class GetAgentID {

	private static final String SERVICE_URL="https://api.secondlife.com/get_agent_id";
	private static final String API_KEY="***REMOVED***";

// ---------- STATICS ----------
	/**
	 * Resolve a name to UUID.
	 * Exceptions.  A LOT.  Exceptions should be taken as advisory not fatal.
	 *
	 * @param name Avatar name to look up, firstname.lastname, firstname lastname, or firstname (implies 'Resident')
	 *
	 * @return The UUID, or an exception if one wasn't found.
	 */
	@Nonnull
	public static final String getAgentID(
			@Nonnull
			final String name) throws Exception {

		// validate the name a bit and we need to break it down into a firstname (called username in modern LL nomenclature, apparently)
		// and a last name
		// "Usernames can contain only letters and numbers" (and when concatenated a single space or dot separator)
		if (Pattern.compile("[^A-Za-z0-9\\. ]?").matcher(name).matches()) {
			throw new UserInputValidationFilterException("Name '"+name+"' contains invalid characters");
		}
		if (Pattern.compile("([ \\.])").matcher(name).groupCount()>1) {
			throw new UserInputValidationFilterException("Name '"+name+"' contains too many separators");
		}


		final URLConnection transmission=new URL(SERVICE_URL).openConnection();
		transmission.setDoOutput(true);
		transmission.setAllowUserInteraction(false);
		transmission.setDoInput(true);
		transmission.setConnectTimeout(5000);
		transmission.setReadTimeout(35000);
		transmission.setRequestProperty("api-key", API_KEY);
		transmission.connect();

		JSONObject request=new JSONObject();
		request.put("username",firstname);
		request.put("lastname",lastname);

		final OutputStreamWriter out=new OutputStreamWriter(transmission.getOutputStream());
		out.write(request+"\n");
		out.flush();
		out.close();
		final BufferedReader rd=new BufferedReader(new InputStreamReader(transmission.getInputStream()));
		String line;
		final StringBuilder response=new StringBuilder();
		//noinspection NestedAssignment
		while ((line=rd.readLine())!=null) {
			response.append(line).append("\n");
		}
		if (response==null) {
			GPHUD.getLogger().log(WARNING,"Failed all retransmission attempts for "+json);
			return;
		}
		if (!response.isEmpty()) {
			try {
				final JSONObject j=new JSONObject(response);
				jsonresponse=j;
				final String incommand=j.optString("incommand","");
				if ("pong".equals(incommand)) {
					if (j.has("callback")) { Char.refreshURL(j.getString("callback")); }
					if (j.has("callback")) { Region.refreshURL(j.getString("callback")); }
					if (j.has("cookie")) { Cookie.refreshCookie(j.getString("cookie")); }
				}
			}
			catch (
					@Nonnull
					final Exception e) {
				GPHUD.getLogger().log(WARNING,"Exception in response parser",e);
				final StringBuilder body=new StringBuilder(url+"\n<br>\n");
				body.append("Character:").append(character==null?"null":character.getNameSafe()).append("\n<br>\n");
				if (caller!=null) {
					for (final StackTraceElement ele: caller) {
						body.append("Caller: ").append(ele.getClassName()).append("/").append(ele.getMethodName()).append(":").append(ele.getLineNumber()).append("\n<br>\n");
					}
				}
				body.append(response);
				try {
					MailTools.mail("Failed response",body.toString());
				}
				catch (
						@Nonnull
						final MessagingException ee) {
					GPHUD.getLogger().log(SEVERE,"Mail exception in response parser exception handler",ee);
				}
			}
		}
		succeeded=true;
	}

}


