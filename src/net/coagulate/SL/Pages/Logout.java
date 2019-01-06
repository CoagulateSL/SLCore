package net.coagulate.SL.Pages;

import net.coagulate.SL.HTTPPipelines.PageMapper.Url;
import net.coagulate.SL.HTTPPipelines.State;
import net.coagulate.SL.HTTPPipelines.StringHandler;

/**
 *
 * @author Iain Price
 */
public class Logout extends StringHandler {

    @Url("/Logout")
    public Logout(){super();}
    @Override
    protected String handleString(State state) {
        state.logout();
        return "<br><br><h3 align=center>Your session has been ended</h3><br><br><br><p align=center><a href=\"/\">Click here to return to the login page</a></p>";
    }
    
}
