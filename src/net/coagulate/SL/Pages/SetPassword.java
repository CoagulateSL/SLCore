package net.coagulate.SL.Pages;

import net.coagulate.SL.HTTPPipelines.Page;
import net.coagulate.SL.HTTPPipelines.State;

/**
 *
 * @author Iain Price
 */
public class SetPassword extends Page {

    @Override
    public void content() {
        pageHeader("Set Password");
        dumpParameters();
        centralisePage();
        State state=State.get();
        if (state.get("Set Password").equals("Set Password"))
        {
            String password=state.get("password");
            boolean ok=true;
            if (password.length()<6) { error("Use a longer password"); }
            else {
                para("Setting your password");            
                state.user().setPassword(password);
                para("Password is now set!");
                return;
            }
        }
        para("Here you may set a password which you may use to log directly in to this application<br>without having to log in to Second Life and message a bot.");
        para("As a matter of general security you should use a unique password,<br>and definately <b>NOT</b> the same password you use for Second Life");
        linebreak();
        startForm();
        label("New Password:").passwordInput("password");
        raw(" ");
        submit("Set Password");
        endForm();
    }

    
}
