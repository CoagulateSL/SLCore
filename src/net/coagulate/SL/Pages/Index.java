package net.coagulate.SL.Pages;

import net.coagulate.SL.HTTPPipelines.Page;

/**
 *
 * @author Iain Price
 */
public class Index extends Page {

    @Override
    public void content() {
        centralisePage();
        para("Welcome to Coagulate Second Life services, select a service for more information.");
        raw("<table style=\"max-width: 900px;\" align=center>");
        raw("<tr width=100%><td width=100%><ul style=\"white-space: nowrap;\">");
        openServiceCell("Region Monitoring");
        p("Region Monitoring monitors the status of specified regions, polling every minute and logging the data.");
        p("This data can be reviewed on the website, or linked to in-world objects to provide region status updates.");
        closeServiceCell();
        openServiceCell("Region Performance");
        p("Region Statistics monitors various aspects of region performance such as FPS, script info, etc");
        p("This information is presented through the website and is graphable");
        closeServiceCell();
        openServiceCell("Bot Agents");
        p("This allows you to operate an automated agent in Second Life (a 'bot').");
        p("This can be used to automate group invites, group ejects, and other features");
        closeServiceCell();
        raw("</ul></td></tr>");
        raw("</table>");
    }

    private void openServiceCell(String title) {
        raw("<li style=\"vertical-align: top; white-space: normal; color:black; border-style: solid; border-width: 5px; width: 200px; text-align: center; margin: 0px; list-style-type: none; display: inline-block;\">");
        raw("<h3 align=center>"+title+"</h3>");        
    }
    private void closeServiceCell() { raw("</li>"); }
}
