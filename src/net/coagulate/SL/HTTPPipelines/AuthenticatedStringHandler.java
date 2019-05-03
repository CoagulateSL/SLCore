package net.coagulate.SL.HTTPPipelines;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.JSLBot.Packets.Types.LLUUID;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.Pages.HTML.Raw;
import net.coagulate.SL.Pages.HTML.State;
import net.coagulate.SL.SL;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

/**
 * @author Iain Price
 */
public abstract class AuthenticatedStringHandler extends Handler {

	private static final String loginpage1 = "<form method=post><p align=center><table><tr><td colspan=2>&nbsp;</td></tr><tr><td></td><td colspan=2 align=center><font size=5><u>Login</u></font></td></tr><tr><th>Avatar Name:</th><td><input autofocus type=text size=20 name=login_username></td></tr>";
	private static final String loginpageprebot = ""
			+ "<tr><td></td><td><i>OPTIONAL: If you do not<br>enter a password,<br>your avatar will be sent a<br>login URL in Second Life</i></td></tr>"
			+ "<tr><th>Coagulate SL Password:</th><td><input type=password size=20 name=login_password></td></tr>"
			+ "<tr><td colspan=2>&nbsp;</td></tr>"
			+ "<tr><td></td><td><button type=submit name=Login value=Login style='width:100%;'>Login</button></td></tr>"
			+ "</table>";
	private static final String loginpagepostbot = "</p></form>";

	@Override
	public StringEntity handleContent(State state) {
		try {
			String content = "<p><b>WEIRD INTERNAL LOGIC ERROR</b></p>";
			String username = state.get("login_username");
			String password = state.get("login_password");
			if (!checkAuth(state)) {
				if (username == null || username.isEmpty()) {
					return new StringEntity(new Page().add(new Raw(loginPage())).toHtml(state), ContentType.TEXT_HTML);
				} else {
					if ("Login".equals(state.get("Login")) && !username.isEmpty() && password.isEmpty()) {
						User target = User.findOptional(username);
						if (target != null) {
							String token = target.generateSSO();
							String message;
							if (SL.DEV) {
								message = "\n\n===== DEVELOPMENT SSO ENTRY POINT =====\n\n\n[https://sldev.coagulate.net/SSO/" + token + " Log in to Coagulate SL DEVELOPMENT ENVIRONMENT]\n\n";
							} else {
								message = "\n\nPlease click the link below to log in to Coagulate SL Services\n\nThis link will be valid for 5 minutes only, and one use.\nIf you wish to log in through the web page rather than via the bot, please 'Set Password' under 'Account' on the top right of the web page after following the link below.\n\n[https://sl.coagulate.net/SSO/" + token + " Log in to Coagulate SL]\n\n";
							}

							SL.bot.im(new LLUUID(target.getUUID()), message);
							return new StringEntity(new Page().add(new Raw(ssoSentPage())).toHtml(state), ContentType.TEXT_HTML);
						}
					}
					return new StringEntity(new Page().add(new Raw(failPage())).toHtml(state), ContentType.TEXT_HTML);
				}
			}
			try { content = handleString(state); } catch (UserException ue) {
				SL.report("AuthenticatedStringHandler caught exception", ue, state);
				SL.getLogger().log(WARNING, "User exception propagated to handler", ue);
				content = "<p>Exception: " + ue.getLocalizedMessage() + "</p>";
			}
			return new StringEntity(content, ContentType.TEXT_HTML);
		} catch (Exception ex) {
			SL.getLogger().log(SEVERE, "Unexpected exception thrown in page handler", ex);
			state.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			return new StringEntity("<html><body><pre><b>500 - Internal Server Error</b></pre><p>Internal Exception, see debug logs</p></body></html>", ContentType.TEXT_HTML);
		}
	}

	protected boolean checkAuth(State state) {
		if (state.user() != null) { return true; }
		// not (yet?) logged in
		String username = state.get("login_username");
		String password = state.get("login_password");
		state.put("parameters", "login_username", "OBSCURED FROM DEEPER CODE");
		state.put("parameters", "login_password", "OBSCURED FROM DEEPER CODE");
		if ("Login".equals(state.get("Login")) && !username.isEmpty() && !password.isEmpty()) {
			User u = null;
			try { u = User.get(username, false); } catch (NoDataException ignore) {}
			if (u == null) {
				SL.getLogger().warning("Attempt to authenticate as invalid user '" + username + "' from " + state.getClientIP());
				return false;
			}
			if (u.checkPassword(password)) {
				state.user(u);
				return true;
			}
			SL.getLogger().warning("Attempt to authenticate with incorrect password as '" + username + "' from " + state.getClientIP());
		}
		return false;
	}

	private String failPage() { return loginpage1 + "<tr><td colspan=2><font color=red><b>Invalid Login</b></font></td></tr>" + loginpageprebot + botLine() + loginpagepostbot; }

	private String ssoSentPage() { return loginpage1 + "<tr><td colspan=2><font color=blue><b>Login sent via IM in Second Life</b></font></td></tr>" + loginpageprebot + botLine() + loginpagepostbot; }

	private String loginPage() { return loginpage1 + loginpageprebot + botLine() + loginpagepostbot; }

	public abstract String handleString(State state);

	private String botLine() { return ""; }
	//return "===> Click <a href=\"secondlife:///app/agent/"+SL.bot.getUUID().toUUIDString()+"/im\">to instant message the bot "+SL.bot.getUsername()+"</a><br>";

}
