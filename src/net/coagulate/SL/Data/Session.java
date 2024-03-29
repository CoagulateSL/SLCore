package net.coagulate.SL.Data;

import net.coagulate.Core.Database.NoDataException;
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
	private final User   user;
	
	private Session(final String sessionid,final User user) {
		id=sessionid;
		this.user=user;
	}
	
	// ---------- STATICS ----------
	@Nullable
	public static Session get(final String sessionid) {
		try {
			final ResultsRow user=SL.getDB()
			                        .dqOne("select userid,expires from sessions where cookie=? and expires>?",
			                               sessionid,
			                               UnixTime.getUnixTime());
			final int userid=user.getInt("userid");
			final int expires=user.getInt("expires");
			final int expiresin=expires-UnixTime.getUnixTime();
			if (expiresin<(Config.getSessionLifespan()/2)) {
				// refresh
				SL.getDB()
				  .d("update sessions set expires=? where cookie=?",
				     UnixTime.getUnixTime()+Config.getSessionLifespan(),
				     sessionid);
			}
			return new Session(sessionid,User.get(userid));
		} catch (@Nonnull final NoDataException e) {
			return null;
		}
	}
	
	@Nonnull
	public static Session create(@Nonnull final User user) {
		// we abuse this "pipeline" to purge old sessions
		try {
			SL.getDB().d("delete from sessions where expires<?",UnixTime.getUnixTime());
		} catch (@Nonnull final Exception ignored) {
		}
		final int userid=user.getId();
		final String sessionid=Tokens.generateToken();
		final int expires=UnixTime.getUnixTime()+Config.getSessionLifespan();
		SL.getDB().d("insert into sessions(cookie,userid,expires) values(?,?,?)",sessionid,userid,expires);
		return new Session(sessionid,user);
	}
	
	// ---------- INSTANCE ----------
	public String toString() {
		return "SLSession["+user+"]";
	}
	
	public String token() {
		return id;
	}
	
	public User user() {
		return user;
	}
	
	public void setUser(@Nonnull final User user) {
		getDatabase().d("update sessions set userid=? where cookie=?",user.getId(),token());
	}
	
	public void logout() {
		getDatabase().d("delete from sessions where cookie=?",token());
	}
	
	@Nonnull
	@Override
	public String getTableName() {
		return "sessions";
	}
}
