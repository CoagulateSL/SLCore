package net.coagulate.SL;

import net.coagulate.Core.Exceptions.System.SystemRemoteFailureException;
import net.coagulate.Core.Exceptions.User.UserConfigurationException;
import net.coagulate.Core.Exceptions.User.UserInputValidationFilterException;
import net.coagulate.Core.Exceptions.User.UserRemoteFailureException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class GetAgentID {

	private static final String SERVICE_URL="https://api.secondlife.com/get_agent_id";
	private static Map<String,String> alreadyResolved=new HashMap<>();
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
	public static String getAgentID(@Nonnull String name) {
		final String rawname=name;
		if (alreadyResolved.containsKey(rawname)) {
			SL.log("GetAgentID").info("NOT Performing DUPLICATE SL getAgentID lookup for "+rawname);
			return alreadyResolved.get(rawname);
		}
		if (Config.getGrid()!= Config.GRID.SECONDLIFE) { throw new UserConfigurationException("Unable to try resolve this name on this grid type"); }
		if (Config.getSecondLifeAPIKey().isEmpty()) { throw new UserConfigurationException("Unable to try resolve this name due to lack of Name Service API Key"); }
		// validate the name a bit and we need to break it down into a firstname (called username in modern LL nomenclature, apparently)
		// and a last name
		// "Usernames can contain only letters and numbers" (and when concatenated a single space or dot separator)
		SL.log("GetAgentID").info("Performing SL getAgentID lookup for "+name);
		name=name.trim();
		if (Pattern.compile(".*[^A-Za-z0-9. ].*").matcher(name).matches()) {
			throw new UserInputValidationFilterException("Name '"+name+"' contains invalid characters");
		}
		if (Pattern.compile(".+[ .].+[ .].+").matcher(name).matches()) {
			throw new UserInputValidationFilterException("Name '"+name+"' contains too many separators");
		}

		String firstname=name.replaceAll("\\."," ");
		String lastname=null;
		if (firstname.contains(" ")) {
			final String[] parts=firstname.split(" ");
			firstname=parts[0].trim();
			if (parts.length>1) { lastname=parts[1].trim(); }
		}

		try {
			final HttpURLConnection transmission=((HttpURLConnection) new URL(SERVICE_URL).openConnection());
			transmission.setDoOutput(true);
			transmission.setAllowUserInteraction(false);
			transmission.setDoInput(true);
			transmission.setConnectTimeout(2000);
			transmission.setReadTimeout(2000);
			transmission.setRequestProperty("api-key",Config.getSecondLifeAPIKey());
			transmission.connect();

			final JSONObject request=new JSONObject();
			request.put("username",firstname);
			if (lastname!=null) { request.put("lastname",lastname); }

			final OutputStreamWriter out=new OutputStreamWriter(transmission.getOutputStream());
			out.write(request+"\n");
			out.flush();
			out.close();

			final int responsecode;
			responsecode=transmission.getResponseCode();

			switch (responsecode) {
				case 403:
					SystemRemoteFailureException urfe=new SystemRemoteFailureException("SL Name API Rate Limited");
					SL.report("Name API IO Error",urfe,null);
					throw urfe;
				case 405:
					SystemRemoteFailureException srfe=new SystemRemoteFailureException("SL Name API said Malformed Request");
					SL.report("Name API IO Error",srfe,null);
					throw srfe;
				case 500:
					UserRemoteFailureException error=new UserRemoteFailureException("SL Name API service errored");
					SL.report("Name API IO Error",error,null);
					throw error;
			}

			final BufferedReader rd;
			if (responsecode==200) { rd=new BufferedReader(new InputStreamReader(transmission.getInputStream())); }
			else { rd=new BufferedReader(new InputStreamReader(transmission.getErrorStream())); }
			String line;
			final StringBuilder responsebuilder=new StringBuilder();
			//noinspection NestedAssignment
			while ((line=rd.readLine())!=null) {
				responsebuilder.append(line).append("\n");
			}
			final String response=responsebuilder.toString();
			if (response.isEmpty()) {
				throw new UserRemoteFailureException("SL Name API gave empty response");
			}

			final JSONObject json=new JSONObject(response);
			if (responsecode==404) {
				throw new UserRemoteFailureException("SL Name API - "+json.optString("error","???")+" - "+json.optString("message","NoErrorMessage"));
			}
			alreadyResolved.put(rawname,json.getString("agent_id"));
			return json.getString("agent_id");
		}
		catch (final IOException ex) {
			SL.report("Name API IO Error",ex,null);
			throw new UserRemoteFailureException("SL Name API IO Error: "+ex.getLocalizedMessage(),ex);
		}
	}

}


