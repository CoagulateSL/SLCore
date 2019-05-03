package net.coagulate.SL.Data;

import net.coagulate.Core.Database.*;
import net.coagulate.Core.Tools.*;
import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.Data.TableRow;
import net.coagulate.GPHUD.Interfaces.Outputs.Link;
import net.coagulate.SL.Config;
import net.coagulate.SL.Pricing;
import net.coagulate.SL.SL;

import java.util.*;

import static java.util.logging.Level.SEVERE;
import static net.coagulate.Core.Tools.UnixTime.getUnixTime;

/**
 * @author Iain Price
 */
public class User extends LockableTable {

	private static final Map<Integer, User> users = new HashMap<>();
	String username;

	private User(int id, String username) {
		super(id);
		this.username = username;
	}

	/**
	 * Obtain a reference to the SYSTEM avatar, for auditing system functions.
	 *
	 * @return Reference to the SYSTEM avatar
	 */
	public static User getSystem() {
		return findOrCreateAvatar("SYSTEM", "DEADBEEF");
	}

	public static String getGPHUDLink(String name, int id) {
		return new Link(name, "/GPHUD/avatars/view/" + id).asHtml(null, true);
	}

	public static String formatUsername(String username) {
		String original = username;
		// possible formats
		// user.resident
		// user resident
		// user
		// and also we want to formalise case
		username = username.replaceAll("\\.", " "); // merge "user.resident" and "user resident"
		// for simplicity, force to "double barrel"
		if (!username.contains(" ")) { username = username + " Resident"; } // merge "user" and "user resident"
		String[] parts = username.split(" ");
		if (parts.length != 2) {
			throw new SystemException("Formatting username '" + original + "' gave '" + username + "' which has " + parts.length + " parts, which is not 2...");
		}
		// convert to "uppercase-first"
		String firstname = parts[0].toLowerCase();
		String lastname = parts[1].toLowerCase();
		firstname = firstname.substring(0, 1).toUpperCase() + firstname.substring(1);
		lastname = lastname.substring(0, 1).toUpperCase() + lastname.substring(1);
		// only append the surname if /not/ resident
		String output = firstname;
		if (!"Resident".equalsIgnoreCase(lastname)) { output = output + " " + lastname; } // redundant ignores case
		return output;
	}

	private static User factory(int id, String username) {
		synchronized (users) {
			if (users.containsKey(id)) { return users.get(id); }
			User u = new User(id, username);
			users.put(id, u);
			return u;
		}
	}

	public static User get(String username, boolean createifnecessary) {
		username = formatUsername(username);
		boolean mandatory = true;
		if (createifnecessary) { mandatory = false; }
		Integer id = SL.getDB().dqi(mandatory, "select id from users where username=?", username);
		if (id != null) { return factory(id, username); }
		SL.getDB().d("insert into users(username) values(?)", username);
		id = SL.getDB().dqi(false, "select id from users where username=?", username);
		if (id != null) { return factory(id, username); }
		throw new SystemException("Created user for " + username + " and then couldn't find its id");
	}

	public static User get(int id) {
		String username = SL.getDB().dqs(true, "select username from users where id=?", id);
		return factory(id, username);
	}

	public static User get(String username) { return get(username, false); }

	public static User resolveDeveloperKey(String key) {
		if (key == null || "".equals(key)) {
			return null;
		}
		Integer userid = SL.getDB().dqi(false, "select id from users where developerkey=?", key);
		if (userid == null) { return null; }
		return get(userid);
	}

	public static User getSSO(String token) {
		// purge old tokens
		SL.getDB().d("update users set ssotoken=null,ssoexpires=null where ssoexpires<?", UnixTime.getUnixTime());
		Integer match = SL.getDB().dqi(false, "select id from users where ssotoken=?", token);
		if (match == null) { return null; }
		SL.getDB().d("update users set ssotoken=null,ssoexpires=null where id=?", match);
		return get(match);
	}

	public static User findOrCreateAvatar(String name, String key) throws SystemException {
		if (name == null || "".equals(name)) { name = ""; }
		Integer userid = SL.getDB().dqi(false, "select id from users where (username=? or avatarkey=?)", name, key);
		if (userid == null) {
			if (name.isEmpty()) { throw new SystemException("Empty avatar name blocks creation"); }
			if (key.isEmpty()) { throw new SystemException("Empty avatar key blocks creation"); }
			if (name.contains("Loading...")) {
				throw new SystemException("No avatar name was sent with the key, can not create new avatar record");
			}
			try {
				// special key used by the SYSTEM avatar
				if (!"DEADBEEF".equals(key)) {
					SL.getLogger("User").info("Creating new avatar entry for '" + name + "'");
				}
				SL.getDB().d("insert into users(username,lastactive,avatarkey) values(?,?,?)", name, getUnixTime(), key);
			} catch (DBException ex) {
				SL.getLogger("User").log(SEVERE, "Exception creating avatar " + name, ex);
				throw ex;
			}
			userid = SL.getDB().dqi(false, "select id from users where avatarkey=?", key);
		}
		if (userid == null) {
			SL.getLogger("User").severe("Failed to find avatar '" + name + "' after creating it");
			throw new NoDataException("Failed to find avatar object for name '" + name + "' after we created it!");
		}
		return get(userid);
	}

	// GPHUD legacy
	public static Map<Integer, String> loadMap() {
		Map<Integer, String> results = new TreeMap<>();
		Results rows = SL.getDB().dq("select id,username from users");
		for (ResultsRow r : rows) {
			results.put(r.getInt("id"), TableRow.getLink(r.getString("username"), "avatars", r.getInt("id")));
		}
		return results;
	}

	public static User findMandatory(String nameorkey) {
		User user = findOptional(nameorkey);
		if (user == null) {
			throw new UserException("Failed to find avatar object for name or key '" + nameorkey + "'");
		}
		return user;
	}

	/**
	 * Find avatar in database, by name or key.
	 *
	 * @return Avatar object
	 */
	public static User findOptional(String nameorkey) {
		if (nameorkey == null || "".equals(nameorkey)) { throw new UserException("Avatar name/key not supplied"); }
		Integer userid = SL.getDB().dqi(false, "select id from users where username=? or avatarkey=?", nameorkey, nameorkey);
		if (userid == null) { return null; }
		return get(userid);
	}

	public String getGPHUDLink() { return getGPHUDLink(getUsername(), getId()); }

	public String getUsername() { return username; }

	public String getName() { return getUsername(); }

	@Override
	public String getTableName() { return "users"; }

	@Override
	public String toString() { return getUsername() + "#" + getId(); }

	public boolean superuser() { return getId() == 1 && "Iain Maltz".equalsIgnoreCase(username); }

	public boolean hasDeveloperKey() {
		try {
			String s = getDeveloperKey();
			if (s == null || s.isEmpty()) { return false; }
			return true;
		} catch (NoDataException e) { return false; }
	}

	public String getDeveloperKey() {
		return dqs(true, "select developerkey from users where id=?", getId());
	}

	public boolean isSuperAdmin() {
		Integer isadmin = dqi(true, "select superadmin from users where id=?", getId());
		return isadmin == 1;
	}

	public String generateSSO() {
		String token = Tokens.generateToken();
		int expires = UnixTime.getUnixTime() + Config.SSOWINDOWSECONDS;
		d("update users set ssotoken=?,ssoexpires=? where " + getIdColumn() + "=?", token, expires, getId());
		return token;
	}

	public void setPassword(String password, String clientip) throws UserException {
		if (password.length() < 6) { throw new UserException("Password not long enough"); }
		d("update users set password=? where id=?", Passwords.createHash(password), getId());
		SL.getLogger().info("User " + this.getUsername() + " has set password from " + clientip);
	}

	public boolean checkPassword(String password) {
		String hash = dqs(true, "select password from users where id=?", getId());
		if (hash == null || hash.isEmpty()) {
			SL.getLogger().warning("Attempt to log in with a null or empty password hash for user " + getUsername());
			return false;
		}
		return Passwords.verifyPassword(password, hash);
	}

	public int balance() {
		Integer balance = dqi(false, "select sum(ammount) from journal where userid=?", getId());
		if (balance == null) { return 0; }
		return balance;
	}

	public void bill(int ammount, String description) throws UserException {
		int serial;
		try {serial = lock();} catch (LockException e) {
			throw new UserException("Your balance is currently being updated elsewhere, please retry in a moment");
		}
		try {
			int balance = balance();
			if (balance < ammount) {
				throw new UserException("Insufficient balance (L$" + balance + ") to pay charge L$" + ammount);
			}
			d("insert into journal(tds,userid,ammount,description) values(?,?,?,?)", UnixTime.getUnixTime(), getId(), -ammount, description);
		} finally { unlock(serial); }
	}

	public Set<Subscription> getSubscriptions(Pricing.SERVICE service, boolean activeonly, boolean paidonly) {
		Results res;
		int paiduntilfilter = UnixTime.getUnixTime();
		if (paidonly == false) { paiduntilfilter = 0; }
		String activeonlysql;
		if (activeonly) { activeonlysql = " and active=1"; } else { activeonlysql = ""; }
		if (service != null) {
			res = dq("select id from subscriptions where ownerid=? and servicetype=? and paiduntil>?" + activeonlysql, getId(), service.getValue(), paiduntilfilter);
		} else {
			res = dq("select id from subscriptions where ownerid=? and paiduntil>?" + activeonlysql, getId(), paiduntilfilter);
		}
		Set<Subscription> subs = new HashSet<>();
		for (ResultsRow r : res) {
			subs.add(new Subscription(r.getInt("id")));
		}
		return subs;
	}

	public String getEmail() { return getString("email"); }

	public String getNewEmail() { return getString("newemail"); }

	/**
	 * Sets the new email address, returning the token that must be used to validate it.
	 *
	 * @return String token used to validate the email address
	 */
	public String setNewEmail(String newemail) {
		int expires = UnixTime.getUnixTime() + Config.NEWEMAIL_TOKEN_LIFESPAN;
		String token = Tokens.generateToken();
		d("update users set newemail=?,newemailtoken=?,newemailexpires=? where id=?", newemail, token, expires, getId());
		return token;
	}

	public void confirmNewEmail(String token) {
		ResultsRow r = dqone(true, "select newemail,newemailtoken,newemailexpires from users where id=?", getId());
		String newemail = r.getString("newemail");
		String newtoken = r.getString("newemailtoken");
		int expires = r.getInt("newemailexpires");
		if (token == null || token.isEmpty()) { throw new UserException("No token passed"); }
		if (!token.equals(newtoken)) { throw new UserException("Email token does not match"); }
		if (expires < UnixTime.getUnixTime()) {
			throw new UserException("Email token has expired, please register new email address again");
		}
		// token matches, not expired, promote the address
		d("update users set newemail=null, newemailtoken=null, newemailexpires=0, email=? where id=?", newemail, getId());

	}

	/**
	 * Return the avatar's UUID.
	 *
	 * @return UUID (Avatar Key)
	 */
	public String getUUID() {
		return getString("avatarkey");
	}


	public String getTimeZone() {
		String s = getString("timezone");
		if (s == null) { return "America/Los_Angeles"; }
		if ("SLT".equals(s)) { return "America/Los_Angeles"; }
		return s;
	}

	public void setTimeZone(String timezone) { set("timezone", timezone); }

	/**
	 * Sets the last used instance for the avatar - used for logging in to the web portal.
	 *
	 * @param i Instance to set to
	 */
	public void setLastInstance(Instance i) {
		d("update users set lastgphudinstance=? where id=?", i.getId(), getId());
	}

	/**
	 * Gets the avatar's last active timestamp.
	 *
	 * @return The last active time for an avatar, possibly null.
	 */
	public Integer getLastActive() {
		return dqi(true, "select lastactive from users where id=?", getId());
	}


}
