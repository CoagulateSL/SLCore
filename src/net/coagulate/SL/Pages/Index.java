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
    }
    
}
