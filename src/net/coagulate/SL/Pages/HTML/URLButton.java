package net.coagulate.SL.Pages.HTML;

import net.coagulate.SL.State;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * @author Iain Price
 */
public class URLButton implements Element {
	
	private final String label;
	private final String url;
	
	public URLButton(final String label,final String url) {
		this.label=label;
		this.url=url;
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String toHtml(final State st) {
		return "<a href=\""+url+"\"><button type=submit>"+label+"</button></a>";
	}
	
	@Override
	public String toString(final State st) {
		return label;
	}
	
	@Override
	public void load(final Map<String,String> map) {
	}
	
}
