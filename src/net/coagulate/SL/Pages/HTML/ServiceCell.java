package net.coagulate.SL.Pages.HTML;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class ServiceCell extends Container {

	private final String target;
	private final String title;

	public ServiceCell(final String title, final String targeturl) {
		target = targeturl;
		this.title = title;
	}

	@Nonnull
	@Override
	public String toHtml(final State st) {
		return
				"<a href=\"" + target + "\">" +
						"<li style=\"vertical-align: top; white-space: normal; color:black; border-style: solid; border-width: 5px; height: 250px; width: 200px; text-align: center; margin: 5px; padding: 5px; list-style-type: none; display: inline-block;\">" +
						"<h3 align=center>" + title + "</h3>" +
						super.toHtml(st) +
						"</li></a>";
	}
}
