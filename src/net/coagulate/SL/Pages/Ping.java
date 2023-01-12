package net.coagulate.SL.Pages;

import net.coagulate.Core.HTML.Elements.PlainText;
import net.coagulate.SL.Config;
import net.coagulate.SL.HTTPPipelines.PageType;
import net.coagulate.SL.HTTPPipelines.Url;
import net.coagulate.SL.State;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class Ping {
	
	@Url(url="/Ping", authenticate=false, pageType=PageType.PLAINTEXT)
	public static void ping(@Nonnull final State state) {
		final String response="hostname:"+Config.getHostName();
		state.add(new PlainText(response));
	}
	
}
