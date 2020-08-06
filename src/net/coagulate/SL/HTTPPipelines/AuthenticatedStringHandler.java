package net.coagulate.SL.HTTPPipelines;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.SL.Config;
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

	private static String loginpage1() {
		String r="<form method=post><p align=center>"+
				"<table align=center><tr><td colspan=2>"+
				"<h1>Welcome to "+SL.brandNameUniversal()+"</h1><br>";
		if (Config.isOfficial()) {
			r += "SL.Coagulate.net is an umbrella service that contains a set of different services used in Second Life.<br>" +
					"Currently this consists of the following:<ul>" +
					"<li>RPHUD -  Obsolete RP HUD (<a href=\"https://www.coagulate.net/wiki/index.php/RPHUD\">Limited Documentation Here</a>)</li>" +
					"<li>GPHUD - Next Generation RP HUD (<a href=\"https://sl.coagulate.net/Docs/GPHUD/\">Documentation</a>)</li>" +
					"<li>Quiet Life Rentals - Land rental company in Second Life (<a href=\"https://coagulate.sl/Docs/QLR/\">Documentation)</a></li></ul>" +
					"These services are run by <a href=\"secondlife:///app/agent/8dc52677-bea8-4fc3-b69b-21c5e2224306/about\">Iain Maltz</a></p></td></tr>";
		} else {
			r+= "<a href=\"https://sl.coagulate.net/landingpage\">(C) Iain Maltz / Iain Price, Coagulate</a><br><br>This service is operated by "+Config.getBrandingOwnerHumanReadable();
		}
		r+="<tr><td colspan=2>&nbsp;</td></tr>"+
				"<tr><td colspan=2><hr></td></tr>"+
				"<tr><td colspan=2>&nbsp;</td></tr>"+
				"<tr><td colspan=2 align=center><font size=5><u>Login</u></font></td></tr>"+
				"<tr><td>&nbsp;</td></tr>"+
				"<tr><th align=right>Avatar Name:</th><td><input autofocus type=text size=20 name=login_username></td></tr>";
				return r;
	}
	private static String loginpageprebot() {
		return "<tr><th align=right>"+SL.brandNameUniversal()+" Password:</th><td><input type=password size=20 name=login_password></td></tr>"+
				"<tr><td colspan=2 align=center><i>If you do not enter a password, your avatar will be IMed a login in "+Config.getGridName()+"</i></td></tr>"+
				"<tr><td colspan=2>&nbsp;</td></tr>"+
				"<tr><td colspan=2 align=center><button type=submit name=Login value=Login>Login</button></td></tr></table>";
	}
	private static String loginpageprebotnoim() {
		return "<tr><th align=right>\"+SL.brandNameUniversal()+\" Password:</th><td><input type=password size=20 name=login_password></td></tr>"+
				"<tr><td colspan=2>&nbsp;</td></tr>"+
				"<tr><td colspan=2 align=center><button type=submit name=Login value=Login>Login</button></td></tr>"+
				"</table>";
	}
	private static String loginpagepostbot() {
			return "</p></form></table>";
	}

	// ---------- INSTANCE ----------
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
					if ("Login".equals(state.get("Login")) && password.isEmpty() && SL.canIM()) {
						final User target=User.findUsernameNullable(username,false);
						if (target!=null) {
							final String token=target.generateSSO();
							final String message;
							if (Config.getDevelopment()) {
								message="\n\n===== DEVELOPMENT SSO ENTRY POINT =====\n\n\n[https://dev.sl.coagulate.net/SSO/"+token+" Log in to Coagulate SL DEVELOPMENT "+"ENVIRONMENT]\n\n";
							}
							else {
								message="\n\nPlease click the link below to log in to "+ SL.brandNameUniversal()+"\n\nThis link will be valid for 5 minutes only, and one use.\n\n[https://"+ Config.getURLHost()+"/SSO/"+token+" Log in to "+ SL.brandNameUniversal()+"]\n\n";
							}

							SL.im(target.getUUID(),message);
							return new StringEntity(new Page().add(new Raw(ssoSentPage())).toHtml(state),ContentType.TEXT_HTML);
						}
					}
					return new StringEntity(new Page().add(new Raw(failPage())).toHtml(state),ContentType.TEXT_HTML);
				}
			}
			try { content=handleString(state); }
			catch (@Nonnull final UserException ue) {
				SL.report("AuthenticatedStringHandler caught exception",ue,state);
				SL.log().log(WARNING,"User exception propagated to handler",ue);
				content="<p>Exception: "+ue.getLocalizedMessage()+"</p>";
			}
			return new StringEntity(content,ContentType.TEXT_HTML);
		}
		catch (@Nonnull final Exception ex) {
			SL.log().log(SEVERE,"PageHandler",ex);
			state.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			return new StringEntity("<html><body><pre><b>500 - Internal Server Error</b></pre><p>Internal Exception, see debug logs</p></body></html>",ContentType.TEXT_HTML);
		}
	}

	@Nonnull
	public abstract String handleString(State state);

	// ----- Internal Instance -----
	protected boolean checkAuth(@Nonnull final State state) {
		if (state.userNullable()!=null) { return true; }
		// not (yet?) logged in
		final String username=state.get("login_username");
		final String password=state.get("login_password");
		state.put("parameters","login_username","OBSCURED FROM DEEPER CODE");
		state.put("parameters","login_password","OBSCURED FROM DEEPER CODE");
		if ("Login".equals(state.get("Login")) && !username.isEmpty() && !password.isEmpty()) {
			User u=null;
			try { u=User.findUsernameNullable(username,false); } catch (@Nonnull final NoDataException ignore) {}
			if (u==null) {
				SL.log().warning("Attempt to authenticate as invalid user '"+username+"' from "+state.getClientIP());
				return false;
			}
			if (u.checkPassword(password)) {
				state.user(u);
				return true;
			}
			SL.log().warning("Attempt to authenticate with incorrect password as '"+username+"' from "+state.getClientIP());
		}
		return false;
	}

	@Nonnull
	private String failPage() { return loginpage1()+"<tr><td colspan=2><font color=red><b>Invalid Login</b></font></td></tr>"+(SL.canIM()?loginpageprebot():loginpageprebotnoim())+botLine()+loginpagepostbot(); }

	@Nonnull
	private String ssoSentPage() { return loginpage1()+"<tr><td colspan=2><font color=blue><b>Login sent via IM in "+Config.getGridName()+"</b></font></td></tr>"+(SL.canIM()?loginpageprebot():loginpageprebotnoim())+botLine()+loginpagepostbot(); }

	@Nonnull
	private String loginPage() { return loginpage1()+(SL.canIM()?loginpageprebot():loginpageprebotnoim())+botLine()+loginpagepostbot(); }

	@Nonnull
	private String botLine() { return ""; }
	//return "===> Click <a href=\"secondlife:///app/agent/"+SL.bot.getUUID().toUUIDString()+"/im\">to instant message the bot "+SL.bot.getUsername()+"</a><br>";

}
