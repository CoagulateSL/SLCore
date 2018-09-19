package net.coagulate.SL.Pages;

import net.coagulate.SL.HTTPPipelines.Page;

/**
 *
 * @author Iain Price
 */
public class SetPassword extends Page {

    @Override
    public void content() {
        pageHeader("Set Password");
        
        centralisePage();
        para("Here you may set a password which you may use to log directly in to this application without having to log in to Second Life and message a bot.");
        para("As a matter of general security you should use a unique password, and definately <b>NOT</b> the same password you use for Second Life");
        linebreak();
        startForm();
        label("New Password:").passwordInput("password");
        raw(" ");
        submit("Set Password");
        endForm();
    }

    
}
