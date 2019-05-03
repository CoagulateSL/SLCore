package net.coagulate.SL.SecondLifeAPI;

import org.json.JSONObject;

/**
 * @author Iain Price
 */
public class ShutdownResponse extends JSONObject {

	public ShutdownResponse(String reason) {
		super();
		this.put("reason", reason);
		this.put("command", "SHUTDOWN");
	}
}
