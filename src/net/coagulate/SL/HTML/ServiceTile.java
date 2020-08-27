package net.coagulate.SL.HTML;

import net.coagulate.Core.HTML.Container;

public class ServiceTile extends Container {


    private final String commitId;
    private final String buildDate;
    private final String version;
    private final String description;
    private final String moduleName;
    private final String url;
    private final String imageUrl;

    public ServiceTile(String moduleName, String description, String url, String imageUrl, String version, String buildDate, String commitId) {
        super();
        this.moduleName=moduleName;
        this.description=description;
        this.version=version;
        this.buildDate=buildDate;
        this.commitId=commitId;
        this.url=url;
        this.imageUrl=imageUrl;
    }

    @Override
    public String toString() {
        StringBuilder s=new StringBuilder();
        if (url!=null) { s.append("<a href=\"").append(url).append("\">"); }
        s.append("<li style=\"vertical-align: top; white-space: normal; color:black; border-style: solid; border-width: 5px; height: 250px; width: 200px; text-align: center; margin: 5px; padding: 5px; list-style-type: none; display: inline-block;\">\n");
        if (imageUrl==null) { s.append("<h3 align=\"center\">").append(moduleName); }
        else { s.append("<h3 align=\"center\"><img src=\"").append(imageUrl).append("\">"); }
        s.append("</h3>");
        s.append("<p>");
        s.append(description);
        s.append("</p>");
        s.append("</li>");
        if (url!=null) { s.append("</a>"); }
        return s.toString();
    }
}
