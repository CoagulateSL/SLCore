package net.coagulate.SL.Pages;

import net.coagulate.SL.Config;
import net.coagulate.SL.HTTPPipelines.Handler;
import net.coagulate.SL.HTTPPipelines.PageMapper.Url;
import net.coagulate.SL.Pages.HTML.State;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class Ping extends Handler {

	@Url("/Ping")
	public Ping() {super();}

	@Nonnull
	@Override
	protected HttpEntity handleContent(final State state) {
		final String response="nodename:"+Config.getNodeName()+"\nnode:"+Config.getNode()+"\nhostname:"+Config.getHostName();
		return new StringEntity(response,ContentType.TEXT_PLAIN);
	}

}
