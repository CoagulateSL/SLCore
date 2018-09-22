package net.coagulate.SL.Pages;

import net.coagulate.SL.HTTPPipelines.Page;

/**
 *
 * @author Iain Price
 */
public class Index extends Page {

    @Override
    public void content() {
        linebreak();linebreak();
        centralisePage();
        pageHeader("Welcome");
        para("Welcome to Coagulate Second Life services");
        raw("<table style=\"max-width: 700px;\" align=center>");
        raw("<tr width=100%>");
        raw("<td style=\"border-width: 1px; border-style: solid;\" width=33%>");
        raw("<h3>Tab1</h3>");
        p("Region Monitoring monitors the status of specified regions, polling every minute and logging the data.");
        p("This data can be reviewed on the website, or linked to in-world objects to provide region status updates.");
        raw("</td>");
        raw("<td style=\"border-width: 1px; border-style: solid;\" width=33%>");
        raw("<h3>Tab2</h3>");
        p("Region Statistics monitors various aspects of region performance such as FPS, script info, etc");
        p("This information is presented through the website and is graphable");
        raw("</td>");
        raw("<td style=\"border-width: 1px; border-style: solid;\" width=33%>");
        raw("<h3>Tab3</h3>");
        raw("</td>");
        raw("</tr>");
        raw("</table>");
    }
    
}
