package net.coagulate.SL.HTTPPipelines;

/**
 *
 * @author Iain Price
 */
public class AuthenticatedStringHandler extends StringHandler {

    @Override
    protected String handleString(State state) {
        return loginpage;
    }
    
    private static final String loginpage="<p align=center><table><tr><td colspan=2>&nbsp;</td></tr><tr><td></td><td colspan=2 align=center><font size=5><u>Login</u></font></td></tr>"
            + "<tr><th>Username:</th><td><input type=text size=20 name=username></td></tr>"
            + "<tr><th>Password:</th><td><input type=password size=20 name=password></td></tr>"
            + "<tr><td colspan=2>&nbsp;</td></tr>"
            + "<tr><td></td><td><button type=submit name=Login value=Login style='width:100%;'>Login</button></td></tr>"
            + "</table>"
            + "<br><br><br><br><br>"
            + "<table border=1 width=50%>"
            + "<tr><td>Data</td><tr>"
            + "</table></p>";
            
    
}
