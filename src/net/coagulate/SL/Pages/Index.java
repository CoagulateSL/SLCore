package net.coagulate.SL.Pages;

import net.coagulate.SL.HTTPPipelines.AuthenticatedStringHandler;
import net.coagulate.SL.HTTPPipelines.State;

/**
 *
 * @author Iain Price
 */
public class Index extends AuthenticatedStringHandler {

    @Override
    public String handleAuthenticated(State state) {
        return "<p>Welcome to Coagulate SL.  Nothing to see here.</p>";
    }
    
}
