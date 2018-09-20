package net.coagulate.SL.HTTPPipelines;

import net.coagulate.JSLBot.Log;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.Database.NoDataException;
import net.coagulate.SL.Launch;

/**
 *
 * @author Iain Price
 */
public abstract class AuthenticatedStringHandler extends StringHandler {

    @Override
    protected String handleString() {
        State state=State.get();
        if (state.user()!=null) { return handleAuthenticated(); }
        // not (yet?) logged in
        String username=state.get("login_username");
        String password=state.get("login_password");
        if (state.get("Login").equals("Login") && !username.isEmpty() && !password.isEmpty()) {
            User u=null;
            try { u=User.get(username, false); } catch (NoDataException ignore) {}
            if (u==null) {
                Log.note("Authentication", "Attempt to authenticate as invalid user '"+username+"' from "+state.getClientIP());
                return failPage();
            }
            if (u.checkPassword(password)) {
                state.user(u);
                return handleAuthenticated();
            }
            Log.note("Authentication", "Attempt to authenticate with incorrect password as '"+username+"' from "+state.getClientIP());
            return failPage();
        }
        return loginpage1+loginpage2;
    }
    
    private static final String loginpage1="<form method=post><p align=center><table><tr><td colspan=2>&nbsp;</td></tr><tr><td></td><td colspan=2 align=center><font size=5><u>Login</u></font></td></tr><tr><th>Username:</th><td><input type=text size=20 name=login_username></td></tr>";
    private static final String loginpage2=""
            + "<tr><th>Password:</th><td><input type=password size=20 name=login_password></td></tr>"
            + "<tr><th></th><td><i><b>NOT</b> your SL password</i></td></tr>"
            + "<tr><td colspan=2>&nbsp;</td></tr>"
            + "<tr><td></td><td><button type=submit name=Login value=Login style='width:100%;'>Login</button></td></tr>"
            + "</table>"
            + "<br><br><br><br><br>"
            + "<table border=1 width=\"600px\">"
            + "<tr><td align=center>Registering</td></tr>"
            + "<tr><td><p>If you do not have a password, you must log in through Second Life<br>"
            + "===> Click <a href=\"secondlife:///app/agent/"+Launch.bot.getUUID().toUUIDString()+"/im\">to instant message the bot "+Launch.bot.getUsername()+"</a><br>"
            + "Send the message 'login', and the bot will reply with a URL that will log you in.</p>"
            + "<p>If you wish to avoid the Second Life step in future, and use a password, follow the above to get logged in, and then click 'Set Password' on the top right of the web pages</p></td></tr>"
            + "</table></p></form>";
            
    private String failPage() { return loginpage1+"<tr><td colspan=2><font color=red><b>Invalid Login</b></font></td></tr>"+loginpage2; }
    public abstract String handleAuthenticated();
}
