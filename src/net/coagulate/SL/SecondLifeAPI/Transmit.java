package net.coagulate.SL.SecondLifeAPI;

import net.coagulate.Core.Tools.ByteTools;
import net.coagulate.SL.Config;
import net.coagulate.SL.SL;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
	public static   boolean    debugSpawn;
	final           String     url;
	@Nullable final JSONObject json;
	@Nullable JSONObject jsonResponse;
	int delay;
	
	public Transmit(@Nullable final JSONObject json,final String url) {
		this.url=url;
		this.json=json;
	}
	
	public Transmit(@Nullable final JSONObject json,final String url,final int delay) {
		this.url=url;
		this.json=json;
		this.delay=delay;
	}
	
	// ---------- INSTANCE ----------
	@Nullable
	public JSONObject getResponse() {
		return jsonResponse;
	}
	
	// can call .start() to background run this, or .run() to async run inline/inthread
	@Override
	public void run() {
		if (delay>0) {
			try {
				Thread.sleep(delay*1000L);
			} catch (@Nonnull final InterruptedException ignored) {
			}
		}
		int retries=5;
		String response=null;
		if (url==null||json==null||url.isEmpty()) {
			return;
		}
		while (response==null&&retries>0) {
			try {
				response=sendAttempt();
			} catch (@Nonnull final FileNotFoundException e) {
				getLogger().log(FINE,"404 on url, revoked connection while sending "+json);
				return;
			} catch (@Nonnull final MalformedURLException ex) {
				getLogger().log(WARNING,"MALFORMED URL: "+url+", revoked connection while sending "+json);
				return;
			} catch (@Nonnull final IOException e) {
				retries--;
				getLogger().log(INFO,"IOException "+e.getMessage()+" retries="+retries+" left");
				try {
					Thread.sleep(5*1000);
				} catch (@Nonnull final InterruptedException ignored) {
				}
			}
		}
		if (response==null) {
			getLogger().log(WARNING,"Failed all retransmission attempts for "+json);
		}
		if (response!=null&&!response.isEmpty()) {
			try {
				jsonResponse=new JSONObject(response);
				// process response?
			} catch (@Nonnull final Exception e) {
				getLogger().log(WARNING,"Exception in response parser",e);
			}
		}
	}
	
	@Nonnull
	private String sendAttempt() throws IOException {
		final URLConnection transmission=new URL(url).openConnection();
		transmission.setDoOutput(true);
		transmission.setAllowUserInteraction(false);
		transmission.setDoInput(true);
		transmission.setConnectTimeout(5000);
		transmission.setReadTimeout(35000);
		transmission.connect();
		
		final OutputStreamWriter out=new OutputStreamWriter(transmission.getOutputStream());
		out.write(json+"\n");
		out.flush();
		out.close();
		final String response=ByteTools.convertStreamToString(transmission.getInputStream());
		if (Config.logRequests()) {
			System.out.println("ReqLog:'SLAPI/OUTBOUND','"+Thread.currentThread().getName()+"',"+response.length()+","+
			                   (json==null?"-":json.length())+",-1");
		}
		return response;
	}
	
	// ----- Internal Instance -----
	Logger getLogger() {
		return SL.log("SLAPI.Transmit");
	}
}
