package net.coagulate.SL.Pages;

import net.coagulate.SL.HTTPPipelines.Page;
import net.coagulate.SL.HTTPPipelines.PageMapper.Prefix;
import net.coagulate.SL.HTTPPipelines.PageMapper.Url;

/**
 *
 * @author Iain Price
 */
public class Index extends Page {

    @Url("/")
    @Prefix("/asda")
    public Index(){super();}
    @Override
    public void content() {
        centralisePage();
        para("Welcome to Coagulate Second Life services, select a service for more information.");
        raw("<table style=\"max-width: 900px;\" align=center>");
        raw("<tr width=100%><td width=100%><ul style=\"white-space: nowrap;\">");
        openServiceCell("Region Monitoring","/RegionMonitor");
        p("Region Monitoring monitors the status of specified regions, polling every minute and logging the data.");
        p("Can also use scripted or bot services to collect performance metrics.");
        closeServiceCell();
        openServiceCell("Web Chat","/WebChat");
        p("[Placeholder note to self] Web Chat provides a simple interface to Second Life chat over a web page.");
        p("Perfect for use on a simple browser or low power consumption device.  Can use hosted or home-run bot connections.");
        closeServiceCell();
        openServiceCell("Bot Agents","/Bot");
        p("This allows you to operate an automated agent in Second Life (a 'bot').");
        p("This can be used to automate group invites, group ejects, and other features");
        closeServiceCell();
        raw("</ul></td></tr>");
        raw("<tr width=100%><td width=100%><ul style=\"white-space: nowrap;\">");
        openServiceCell("GPHUD","/GPHUD/");
        p("GPHUD is the 2nd generation role-play HUD.");
        p("This is used to implement various game modes at sims.");
        raw("</table>");
    }

    private void openServiceCell(String title,String target) {
        raw("<a href=\""+target+"\">");
        raw("<li style=\"vertical-align: top; white-space: normal; color:black; border-style: solid; border-width: 5px; height: 250px; width: 200px; text-align: center; margin: 0px; list-style-type: none; display: inline-block;\">");
        raw("<h3 align=center>"+title+"</h3>");        
    }
    private void closeServiceCell() { raw("</li></a>"); }
}
