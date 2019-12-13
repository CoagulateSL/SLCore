package net.coagulate.SL.Data;

import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.Tokens;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.SL.Config;
import net.coagulate.SL.SL;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Iain Price
 */
public class Session extends Table {

	private final String id;
	private final User user;

	private Session(String sessionid, User user) {
		this.id = sessionid;
		this.user = user;
	}

	@Nullable
	public static Session get(String sessionid) {
		ResultsRow user = SL.getDB().dqone(false, "select userid,expires from sessions where cookie=? and expires>?", sessionid, UnixTime.getUnixTime());
		if (user == null) {
			//Log.note("Session","Invalid session id presented");
			return null;
		}
		int userid = user.getInt("userid");
		int expires = user.getInt("expires");
		int expiresin = expires - UnixTime.getUnixTime();
		if (expiresin < (Config.SESSIONLIFESPANSECONDS / 2)) {
			// refresh
			SL.getDB().d("update sessions set expires=? where cookie=?", UnixTime.getUnixTime() + Config.SESSIONLIFESPANSECONDS, sessionid);
		}
		return new Session(sessionid, User.get(userid));
	}

	@Nonnull
	public static Session create(@Nonnull User user) {
		// we abuse this "pipeline" to purge old sessions
		try { SL.getDB().d("delete from sessions where expires<?", UnixTime.getUnixTime()); } catch (Exception e) {}
		int userid = user.getId();
		String sessionid = Tokens.generateToken();
		int expires = UnixTime.getUnixTime() + Config.SESSIONLIFESPANSECONDS;
		SL.getDB().d("insert into sessions(cookie,userid,expires) values(?,?,?)", sessionid, userid, expires);
		return new Session(sessionid, user);
	}

	public String token() { return id; }

	public User user() { return user; }

	public void setUser(@Nonnull User user) {
		getDatabase().d("update sessions set userid=? where cookie=?", user.getId(), token());
	}

	public void logout() {
		getDatabase().d("delete from sessions where cookie=?", token());
	}

	@Nonnull
	@Override
	public String getTableName() { return "sessions"; }
}
