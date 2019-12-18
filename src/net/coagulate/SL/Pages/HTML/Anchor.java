package net.coagulate.SL.Pages.HTML;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class Anchor extends Container {
	final String target;

	public Anchor(final String target) { this.target=target; }

	public Anchor(final String target,
	              final Element content)
	{
		this.target=target;
		add(content);
	}

	public Anchor(final String target,
	              final String content)
	{
		this.target=target;
		add(new Raw(content));
	}

	@Nonnull
	public String toHtml(final State st) {
		return "<a href=\"#"+target+"\">"+super.toHtml(st)+"</a>";
	}
}
