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
        raw("</tr>");
        raw("</table>");
    }

    private void openServiceCell(String title) {
        raw("<td style=\"padding: 10px; vertical-align: top; border-width: 1px; border-style: solid;\" width=33%>");
        raw("<h3 align=center>"+title+"</h3>");        
    }
    private void closeServiceCell() { raw("</td>"); }
}
