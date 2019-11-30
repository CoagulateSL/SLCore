package net.coagulate.SL.SecondLifeAPI;

import net.coagulate.SL.SL;
import org.json.JSONObject;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

import static java.util.logging.Level.*;

/**
 * Implements a callback transmission.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Transmit extends Thread {
	public static boolean debugspawn = false;
	final String url;
	JSONObject json = null;
	JSONObject jsonresponse = null;
	int delay = 0;
	public Transmit(JSONObject json, String url) {
		this.url = url;
		this.json = json;
	}

	public Transmit(JSONObject json, String url, int delay) {
		this.url = url;
		this.json = json;
		this.delay = delay;
	}

	Logger getLogger() { return SL.getLogger("SLAPI.Transmit"); }

	public JSONObject getResponse() { return jsonresponse; }

	// can call .start() to background run this, or .run() to async run inline/inthread
	@Override
	public void run() {
		boolean debug = false;
		if (delay > 0) {
			try { Thread.sleep(delay * 1000); } catch (InterruptedException e) {}
		}
		int retries = 5;
		String response = null;
		if (url == null || json == null || url.isEmpty()) { return; }
		while (response == null && retries > 0) {
			try {
				response = sendAttempt();
			} catch (FileNotFoundException e) {
				getLogger().log(FINE, "404 on url, revoked connection while sending " + json.toString());
				return;
			} catch (MalformedURLException ex) {
				getLogger().log(WARNING, "MALFORMED URL: " + url + ", revoked connection while sending " + json.toString());
				return;
			} catch (IOException e) {
				retries--;
				getLogger().log(INFO, "IOException " + e.getMessage() + " retries=" + retries + " left");
				try { Thread.sleep(5 * 1000); } catch (InterruptedException ee) {}
			}
		}
		if (response == null) { getLogger().log(WARNING, "Failed all retransmission attempts for " + json.toString()); }
		if (response != null && !response.isEmpty()) {
			try {
				jsonresponse = new JSONObject(response);
				// process response?
			} catch (Exception e) {
				getLogger().log(WARNING, "Exception in response parser", e);
			}
		}
	}

	private String sendAttempt() throws IOException {
		boolean debug = false;
		URLConnection transmission = new URL(url).openConnection();
		transmission.setDoOutput(true);
		transmission.setAllowUserInteraction(false);
		transmission.setDoInput(true);
		transmission.setConnectTimeout(5000);
		transmission.setReadTimeout(35000);
		transmission.connect();

		OutputStreamWriter out = new OutputStreamWriter(transmission.getOutputStream());
		out.write(json.toString() + "\n");
		out.flush();
		out.close();
		BufferedReader rd = new BufferedReader(new InputStreamReader(transmission.getInputStream()));
		String line;
		StringBuilder response = new StringBuilder();
		while ((line = rd.readLine()) != null) {
			response.append(line).append("\n");
		}
		return response.toString();
	}
}
