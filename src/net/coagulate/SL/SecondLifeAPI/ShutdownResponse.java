package net.coagulate.SL.SecondLifeAPI;

import org.json.JSONObject;

/**
 * @author Iain Price
 */
public class ShutdownResponse extends JSONObject {
	
	public ShutdownResponse(final String reason) {
		put("reason",reason);
		put("command","SHUTDOWN");
	}
}
