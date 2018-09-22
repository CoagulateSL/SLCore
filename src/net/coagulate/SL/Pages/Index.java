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
        raw("<td width=33%><h3>Tab1</h3></td>");
        raw("<td width=33%><h3>Tab2</h3></td>");
        raw("<td width=33%><h3>Tab3</h3></td>");
        raw("</tr>");
        raw("</table>");
    }
    
}
