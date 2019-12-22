package net.coagulate.SL.HTTPPipelines;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.JSLBot.Packets.Types.LLUUID;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.Pages.HTML.Raw;
import net.coagulate.SL.Pages.HTML.State;
import net.coagulate.SL.SL;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import javax.annotation.Nonnull;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

/**
 * @author Iain Price
 */
public abstract class AuthenticatedStringHandler extends Handler {

	private static final String loginpage1="<form method=post><p align=center>"+"<table align=center><tr><td colspan=2>"+"<h1>Welcome to Coagulate.SL</h1><br>"+"Coagulate"+
			".SL"+" is an umbrella service that contains a set of different services used in Second Life.<br>"+"Currently this consists of the following:<ul>"+"<li"+">RPHUD "
			+"- "+"Obsolete RP HUD (<a href=\"https://www.coagulate.net/wiki/index.php/RPHUD\">Limited Documentation Here</a>)</li>"+"<li>GPHUD - Next Generation RP HUD "+
			"(<a "+"href=\"https://coagulate.sl/Docs/GPHUD/\">Documentation</a>)</li>"+"<li>Quiet Life Rentals - Land rental company in Second Life (<a "+"href=\"https" +
			"://coagulate"+".sl/Docs/QLR/\">Documentation)</a></li>"+"</ul>"+"These services are run by <a href=\"secondlife:///app/agent/8dc52677-bea8-4fc3-b69b"+
			"-21c5e2224306/about\">Iain "+"Maltz</a>"+"</p></td></tr>"+"<tr><td colspan=2>&nbsp;</td></tr>"+"<tr><td colspan=2><hr></td></tr>"+"<tr><td colspan=2>&nbsp;"+
			"</td></tr>"+"<tr><td colspan=2 "+"align=center><font size=5><u>Login</u></font></td></tr>"+"<tr><td>&nbsp;</td></tr>"+"<tr><th align=right>Avatar "+"Name:</th" +
			"><td><input autofocus type=text "+"size=20"+" name=login_username></td></tr>";
	private static final String loginpageprebot=
			""+"<tr><th align=right>Coagulate SL Password:</th><td><input type=password size=20 name=login_password></td></tr>"+"<tr><td"+" colspan=2 align=center><i>If you "
					+"do not enter a password, your avatar will be IMed a login in Second Life</i></td></tr>"+"<tr><td colspan=2>&nbsp;</td></tr>"+"<tr><td colspan=2 "+
					"align=center><button type=submit name=Login value=Login>Login</button></td></tr>"+"</table>";
	private static final String loginpagepostbot="</p></form></table>";

	@Nonnull
	@Override
	public StringEntity handleContent(@Nonnull final State state) {
		try {
			String content;
			final String username=state.get("login_username");
			final String password=state.get("login_password");
			if (!checkAuth(state)) {
				if (username==null || username.isEmpty()) {
					return new StringEntity(new Page().add(new Raw(loginPage())).toHtml(state),ContentType.TEXT_HTML);
				}
				else {
					if ("Login".equals(state.get("Login")) && password.isEmpty()) {
						final User target=User.findOptional(username);
						if (target!=null) {
							final String token=target.generateSSO();
							final String message;
							if (SL.DEV) {
								message="\n\n===== DEVELOPMENT SSO ENTRY POINT =====\n\n\n[https://dev.coagulate.sl/SSO/"+token+" Log in to Coagulate SL DEVELOPMENT "+
										"ENVIRONMENT]\n\n";
							}
							else {
								message=
										"\n\nPlease click the link below to log in to Coagulate SL Services\n\nThis link will be valid for 5 minutes only, and one use.\nIf "+"you wish to log in through the web page rather than via the bot, please 'Set Password' under 'Account' on the top right of the web "+"page after following the link below.\n\n[https://coagulate.sl/SSO/"+token+" Log in to Coagulate SL]\n\n";
							}

							SL.bot().im(new LLUUID(target.getUUID()),message);
							return new StringEntity(new Page().add(new Raw(ssoSentPage())).toHtml(state),ContentType.TEXT_HTML);
						}
					}
					return new StringEntity(new Page().add(new Raw(failPage())).toHtml(state),ContentType.TEXT_HTML);
				}
			}
			try { content=handleString(state); }
			catch (@Nonnull final UserException ue) {
				SL.report("AuthenticatedStringHandler caught exception",ue,state);
				SL.getLogger().log(WARNING,"User exception propagated to handler",ue);
				content="<p>Exception: "+ue.getLocalizedMessage()+"</p>";
			}
			return new StringEntity(content,ContentType.TEXT_HTML);
		}
		catch (@Nonnull final Exception ex) {
			SL.getLogger().log(SEVERE,"PageHandler",ex);
			state.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			return new StringEntity("<html><body><pre><b>500 - Internal Server Error</b></pre><p>Internal Exception, see debug logs</p></body></html>",ContentType.TEXT_HTML);
		}
	}

	protected boolean checkAuth(@Nonnull final State state) {
		if (state.userNullable()!=null) { return true; }
		// not (yet?) logged in
		final String username=state.get("login_username");
		final String password=state.get("login_password");
		state.put("parameters","login_username","OBSCURED FROM DEEPER CODE");
		state.put("parameters","login_password","OBSCURED FROM DEEPER CODE");
		if ("Login".equals(state.get("Login")) && !username.isEmpty() && !password.isEmpty()) {
			User u=null;
			try { u=User.get(username,false); } catch (@Nonnull final NoDataException ignore) {}
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

	@Nonnull
	private String failPage() { return loginpage1+"<tr><td colspan=2><font color=red><b>Invalid Login</b></font></td></tr>"+loginpageprebot+botLine()+loginpagepostbot; }

	@Nonnull
	private String ssoSentPage() { return loginpage1+"<tr><td colspan=2><font color=blue><b>Login sent via IM in Second Life</b></font></td></tr>"+loginpageprebot+botLine()+loginpagepostbot; }

	@Nonnull
	private String loginPage() { return loginpage1+loginpageprebot+botLine()+loginpagepostbot; }

	@Nonnull
	public abstract String handleString(State state);

	@Nonnull
	private String botLine() { return ""; }
	//return "===> Click <a href=\"secondlife:///app/agent/"+SL.bot.getUUID().toUUIDString()+"/im\">to instant message the bot "+SL.bot.getUsername()+"</a><br>";

}
