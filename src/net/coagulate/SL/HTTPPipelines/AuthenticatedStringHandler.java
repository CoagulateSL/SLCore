package net.coagulate.SL.HTTPPipelines;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.Pages.HTML.Raw;
import net.coagulate.SL.Pages.HTML.State;
import net.coagulate.SL.SL;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

/**
 *
 * @author Iain Price
 */
public abstract class AuthenticatedStringHandler extends Handler {

   @Override
    public StringEntity handleContent(State state) {
        try {
            String content="<p><b>WEIRD INTERNAL LOGIC ERROR</b></p>";
            String username=state.get("login_username");
            if (!checkAuth(state)) {
                if (username==null || username.isEmpty()) {
                    return new StringEntity(new Page().add(new Raw(loginPage())).toHtml(state),ContentType.TEXT_HTML);
                } else {
                    return new StringEntity(new Page().add(new Raw(failPage())).toHtml(state),ContentType.TEXT_HTML);
                }
            }
            try { content=handleString(state); }
            catch (UserException ue) {
                SL.getLogger().log(WARNING,"User exception propagated to handler",ue);
                content="<p>Exception: "+ue.getLocalizedMessage()+"</p>";
            }
            return new StringEntity(content,ContentType.TEXT_HTML);
        }
        catch (Exception ex) {
            SL.getLogger().log(SEVERE,"Unexpected exception thrown in page handler",ex);
            state.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            return new StringEntity("<html><body><pre><b>500 - Internal Server Error</b></pre><p>Internal Exception, see debug logs</p></body></html>",ContentType.TEXT_HTML);
        }
    }        
    
    protected boolean checkAuth(State state) {
        if (state.user()!=null) { return true; }
        // not (yet?) logged in
        String username=state.get("login_username");
        String password=state.get("login_password");
        state.put("parameters","login_username","OBSCURED FROM DEEPER CODE");
        state.put("parameters","login_password","OBSCURED FROM DEEPER CODE");
        if (state.get("Login").equals("Login") && !username.isEmpty() && !password.isEmpty()) {
            User u=null;
            try { u=User.get(username, false); } catch (NoDataException ignore) {}
            if (u==null) {
                SL.getLogger().warning("Attempt to authenticate as invalid user '"+username+"' from "+state.getClientIP());
                return false;
            }
            if (u.checkPassword(password)) {
                state.user(u);
                return true;
            }
            SL.getLogger().warning("Attempt to authenticate with incorrect password as '"+username+"' from "+state.getClientIP());
        }
        return false;
    }
    
    private static final String loginpage1="<form method=post><p align=center><table><tr><td colspan=2>&nbsp;</td></tr><tr><td></td><td colspan=2 align=center><font size=5><u>Login</u></font></td></tr><tr><th>Username:</th><td><input autofocus type=text size=20 name=login_username></td></tr>";
    private static final String loginpageprebot=""
            + "<tr><th>Password:</th><td><input type=password size=20 name=login_password></td></tr>"
            + "<tr><th></th><td><i><b>NOT</b> your SL password</i></td></tr>"
            + "<tr><td colspan=2>&nbsp;</td></tr>"
            + "<tr><td></td><td><button type=submit name=Login value=Login style='width:100%;'>Login</button></td></tr>"
            + "</table>"
            + "<br><br><br><br><br>"
            + "<table border=1 width=\"600px\">"
            + "<tr><td align=center>Registering</td></tr>"
            + "<tr><td><p>If you do not have a password, you must log in through Second Life<br>";
    private static final String loginpagepostbot=""
            + "Send the message 'login', and the bot will reply with a URL that will log you in.</p>"
            + "<p>If you wish to avoid the Second Life step in future, and use a password, follow the above to get logged in, and then click 'Account' on the top right of the web pages</p></td></tr>"
            + "</table></p></form>";
            
    private String failPage() { return loginpage1+"<tr><td colspan=2><font color=red><b>Invalid Login</b></font></td></tr>"+loginpageprebot+botLine()+loginpagepostbot; }
    private String loginPage() { return loginpage1+loginpageprebot+botLine()+loginpagepostbot; }
    public abstract String handleString(State state);
    private String botLine() {
        return "===> Click <a href=\"secondlife:///app/agent/"+SL.bot.getUUID().toUUIDString()+"/im\">to instant message the bot "+SL.bot.getUsername()+"</a><br>";
    }

}
