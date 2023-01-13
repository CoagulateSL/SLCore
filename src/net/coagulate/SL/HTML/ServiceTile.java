package net.coagulate.SL.HTML;

import net.coagulate.Core.HTML.Container;

import javax.annotation.Nonnull;

public class ServiceTile extends Container {
	
	
	private final String commitId;
	private final String buildDate;
	private final String description;
	private final String moduleName;
	private final String url;
	private final String imageUrl;
	
	public ServiceTile(final String moduleName,
	                   final String description,
	                   final String url,
	                   final String imageUrl,
	                   final String commitId,
	                   final String buildDate) {
		this.moduleName=moduleName;
		this.description=description;
		this.buildDate=buildDate;
		this.commitId=commitId;
		this.url=url;
		this.imageUrl=imageUrl;
	}
	
	@Override
	public void toString(@Nonnull final StringBuilder s) {
		final String fixedHeight="130";
		if (url!=null) {
			s.append("<a href=\"").append(url).append("\">");
		}
		s.append(
				"<li style=\"vertical-align: top; white-space: normal; color:black; border-style: solid; border-width: 5px; min-height: 250px; width: 200px; text-align: center; margin: 5px; padding: 5px; list-style-type: none; display: inline-block;\">\n");
		if (imageUrl==null) {
			s.append("<h3 align=\"center\">").append(moduleName);
		} else {
			s.append("<h3 align=\"center\"><img src=\"").append(imageUrl).append("\">");
		}
		s.append("</h3>");
		s.append("<p><table height="+fixedHeight+"px valign=top align=center><tr height="+fixedHeight+
		         "px valign=top align=center><td height="+fixedHeight+"px valign=top align=center>");
		s.append("<b>").append(moduleName).append("</b><br>&nbsp;<br>").append(description);
		s.append("</td></tr></table></p>");
		s.append("<p style=\"text-align: left; margin: 0px; font-size: 10px;\">").append(commitId)
		 //.append("<span style=\"float:center;\">").append("hello").append("</span>")
		 .append("<span style=\"float:right;\">").append(buildDate).append("</span>").append("</p>");
		s.append("</li>");
		if (url!=null) {
			s.append("</a>");
		}
	}
}
