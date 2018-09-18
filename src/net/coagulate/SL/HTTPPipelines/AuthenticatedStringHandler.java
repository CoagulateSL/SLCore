package net.coagulate.SL.HTTPPipelines;

import net.coagulate.SL.Launch;

/**
 *
 * @author Iain Price
 */
public abstract class AuthenticatedStringHandler extends StringHandler {

    @Override
    protected String handleString(State state) {
        if (state.user==null) { return loginpage; }
        return handleAuthenticated(state);
    }
    
    private static final String loginpage="<p align=center><table><tr><td colspan=2>&nbsp;</td></tr><tr><td></td><td colspan=2 align=center><font size=5><u>Login</u></font></td></tr>"
            + "<tr><th>Username:</th><td><input type=text size=20 name=username></td></tr>"
            + "<tr><th>Password:</th><td><input type=password size=20 name=password></td></tr>"
            + "<tr><th></th><td><i><b>NOT</b> your SL password</i></td></tr>"
            + "<tr><td colspan=2>&nbsp;</td></tr>"
            + "<tr><td></td><td><button type=submit name=Login value=Login style='width:100%;'>Login</button></td></tr>"
            + "</table>"
            + "<br><br><br><br><br>"
            + "<table border=1 width=\"600px\">"
            + "<tr><td align=center>Registering</td></tr>"
            + "<tr><td></td></tr>"
            + "<tr><td><p>If you do not have a password, you must log in through Second Life<br>"
            + "===> Click <a href=\"secondlife:///app/agent/"+Launch.bot.getUUID().toUUIDString()+"/im\">to instant message the bot "+Launch.bot.getUsername()+"</a><br>"
            + "Send the message 'login', and the bot will reply with a URL that will log you in.</p>"
            + "<p>If you wish to avoid the Second Life step in future, and use a password, follow the above to get logged in, and then click 'Set Password' on the top right of the web pages</p></td></tr>"
            + "</table></p>";
            
    
    public abstract String handleAuthenticated(State state);
}
