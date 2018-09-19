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
        startForm();
        label("New Password:");
        passwordInput("password");
        linebreak();
        submit("Set Password");
    }

    
}
