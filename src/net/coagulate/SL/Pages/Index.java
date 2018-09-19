package net.coagulate.SL.Pages;

import net.coagulate.SL.HTTPPipelines.AuthenticatedStringHandler;

/**
 *
 * @author Iain Price
 */
public class Index extends AuthenticatedStringHandler {

    @Override
    public String handleAuthenticated() {
        return "<p>Welcome to Coagulate SL.  Nothing to see here.</p>";
    }
    
}
