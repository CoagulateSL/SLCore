package net.coagulate.SL.Data;

import net.coagulate.Core.Database.*;
import net.coagulate.Core.Exceptions.System.SystemBadValueException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.*;
import net.coagulate.Core.Tools.MailTools;
import net.coagulate.Core.Tools.Passwords;
import net.coagulate.Core.Tools.Tokens;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.Data.TableRow;
import net.coagulate.GPHUD.Interfaces.Outputs.Link;
import net.coagulate.SL.Config;
import net.coagulate.SL.GetAgentID;
import net.coagulate.SL.Pricing;
import net.coagulate.SL.SL;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import java.util.*;

import static java.util.logging.Level.SEVERE;
import static net.coagulate.Core.Tools.UnixTime.getUnixTime;

/**
 * @author Iain Price
 */
public class User extends LockableTable implements Comparable<User> {

	private static final Map<Integer,User> users=new HashMap<>();
	private String usernamecache;

	private User(final int id) {
		super(id);
	}

	// ---------- STATICS ----------

	/**
	 * Obtain a reference to the SYSTEM avatar, for auditing system functions.
	 *
	 * @return Reference to the SYSTEM avatar
	 */
	public static User getSystem() {
		return findOrCreate("SYSTEM","DEADBEEF",true);
	}

	@Nonnull
	public static String getGPHUDLink(final String name,
	                                  final int id) {
		return new Link(name,"/GPHUD/avatars/view/"+id).asHtml(null,true);
	}

	/*public static User get(String username,
	                       final boolean createifnecessary) {
		username=formatUsername(username);
		Integer id=null;
		try {
			id=SL.getDB().dqi("select id from users where username=?",username);
		}
		catch (@Nonnull final NoDataException e) {}
		if (id!=null) { return factory(id,username); }
		if (!createifnecessary) { throw new NoDataException("Can not find existing user for "+username); }
		SL.getDB().d("insert into users(username) values(?)",username);
		try {
			id=SL.getDB().dqi("select id from users where username=?",username);
		}
		catch (@Nonnull final NoDataException e) {}
		if (id!=null) { return factory(id,username); }
		throw new SystemConsistencyException("Created user for "+username+" and then couldn't find its id");
	}*/

	@Nonnull
	public static String formatUsername(String username) {
		final String original=username;
		// possible formats
		// user.resident
		// user resident
		// user
		// and also we want to formalise case
		username=username.replaceAll("\\."," "); // merge "user.resident" and "user resident"
		// for simplicity, force to "double barrel"
		if (!username.contains(" ")) { username=username+" Resident"; } // merge "user" and "user resident"
		final String[] parts=username.split(" ");
		if (parts.length!=2) {
			throw new SystemImplementationException("Formatting username '"+original+"' gave '"+username+"' which has "+parts.length+" parts, which is not 2...");
		}
		// convert to "uppercase-first"
		String firstname=parts[0].toLowerCase();
		String lastname=parts[1].toLowerCase();
		firstname=firstname.substring(0,1).toUpperCase()+firstname.substring(1);
		lastname=lastname.substring(0,1).toUpperCase()+lastname.substring(1);
		// only append the surname if /not/ resident
		String output=firstname;
		if (!"Resident".equalsIgnoreCase(lastname)) { output=output+" "+lastname; } // redundant ignores case
		return output;
	}

	// public static User get(final String username) { return get(username,false); }

	public static User get(final int id) {
		return factory(id);
	}

	@Nullable
	public static User resolveDeveloperKey(@Nullable final String key) {
		if (key==null || "".equals(key)) {
			return null;
		}
		try {
			final int userid=SL.getDB().dqinn("select id from users where developerkey=?",key);
			return get(userid);
		}
		catch (@Nonnull final NoDataException e) { return null; }
	}

	@Nullable
	public static User getSSO(final String token) {
		// purge old tokens
		SL.getDB().d("update users set ssotoken=null,ssoexpires=null where ssoexpires<?",UnixTime.getUnixTime());
		try {
			final int match=SL.getDB().dqinn("select id from users where ssotoken=?",token);
			SL.getDB().d("update users set ssotoken=null,ssoexpires=null where id=?",match);
			return get(match);
		}
		catch (@Nonnull final NoDataException e) { return null; }
	}

	/**
	 * Find or create a user entry in the database.
	 *
	 * Call will filter "usernames" of ??? (???) (Loading...) or Loading.. all of which seem to be garbage SL generates.
	 * Note trustname should be set to false if the username is retrieved from HTTP headers from Objects which seem to update later than other methods.
	 * For usernames retrieved from the new GetAgentID LL API this should be set to TRUE to update the database with the new name.
	 *
	 * @param name      Optional name of the avatar, creation will not proceed without this value, otherwise it may be null
	 * @param key       Mandatory UUID string of the avatar
	 * @param trustname Trust the username supplied to the point we will update our recorded username if it differs.
	 *
	 * @return The User object for this avatar
	 *
	 * @throws SystemBadValueException if it is necessary to create the user and both username and key are not presented
	 */
	public static User findOrCreate(@Nullable String name,
	                                @Nonnull final String key,
	                                final boolean trustname) {
		if (name==null || "???".equals(name) || "(???)".equals(name) || "Loading...".equals(name) || "(Loading...)".equals(name)) { name=""; }
		if (!name.isEmpty()) { name=formatUsername(name); }
		Integer userid=null;
		try {
			userid=SL.getDB().dqi("select id from users where (avatarkey=?)",key);
		}
		catch (@Nonnull final NoDataException e) {}
		if (userid==null) {
			if (key.isEmpty()) { throw new SystemBadValueException("Empty avatar key blocks creation"); }
			if (name.isEmpty()) { throw new SystemBadValueException("Empty avatar name blocks creation (for key "+key+")"); }
			try {
				// special key used by the SYSTEM avatar
				if (!"DEADBEEF".equals(key)) {
					SL.getLogger("User").info("Creating new avatar entry for '"+name+"'");
				}
				SL.getDB().d("insert into users(username,lastactive,avatarkey) values(?,?,?)",name,getUnixTime(),key);
			}
			catch (@Nonnull final DBException ex) {
				SL.getLogger("User").log(SEVERE,"Exception creating avatar "+name,ex);
				throw ex;
			}
			try {
				userid=SL.getDB().dqi("select id from users where avatarkey=?",key);
			}
			catch (@Nonnull final NoDataException e) {}
		}
		if (userid==null) {
			SL.getLogger("User").severe("Failed to find avatar '"+name+"' after creating it");
			throw new NoDataException("Failed to find avatar object for name '"+name+"' after we created it!");
		}
		final User u=get(userid);
		final String currentusername=u.getUsername();
		//System.out.println("Find or create for "+key+" -> "+userid+" current "+currentusername+" supplied "+name);
		if (trustname && !name.isEmpty() && !currentusername.equalsIgnoreCase(name)) {
			u.setUsername(name);
			try {
				SL.report("Namechange:"+currentusername+" -> "+name+" for "+key,new Exception("Here "+currentusername+" -> "+name),null);
				MailTools.mail("Namechange:"+currentusername+" -> "+name+" for "+key,"Namechange:"+currentusername+" -> "+name+" for "+key);
			}
			catch (final MessagingException exception) {
				SL.report("Exception during mailer (!)",exception,null);
			}
		}
		return u;
	}

	// GPHUD legacy
	@Nonnull
	public static Map<Integer,String> loadMap() {
		final Map<Integer,String> results=new TreeMap<>();
		final Results rows=SL.getDB().dq("select id,username from users");
		for (final ResultsRow r: rows) {
			results.put(r.getInt("id"),TableRow.getLink(r.getString("username"),"avatars",r.getInt("id")));
		}
		return results;
	}

	@Nonnull
	public static User findUsername(@Nonnull String name,
	                                final boolean trustname) {
		name=formatUsername(name);
		try { return User.get(SL.getDB().dqinn("select id from users where username=?",name)); }
		catch (final NoDataException e) {
			return createByName(name,trustname);
		}
	}

	/**
	 * Find avatar in database, by name or key.
	 *
	 * @return Avatar object
	 */
	@Nullable
	public static User findUsernameNullable(@Nonnull final String username,
	                                        final boolean trustname) {
		try {
			return findUsername(username,trustname);
		}
		catch (@Nonnull final Throwable t) {
			return null;
		}
	}

	public static Set<User> getDevelopers() {
		final Set<User> ret=new TreeSet<>();
		for (final ResultsRow result: SL.getDB().dq("select id from users where developerkey is not null")) {
			ret.add(User.get(result.getInt()));
		}
		return ret;
	}

	@Nonnull
	public static User findUserKey(@Nonnull final String uuid) {
		return new User(SL.getDB().dqinn("select id from users where avatarkey like ?",uuid));
	}

	@Nullable
	public static User findUserKeyNullable(@Nonnull final String uuid) {
		try { return findUserKey(uuid); }
		catch (final NoDataException e) { return null; }
	}

	// ----- Internal Statics -----
	private static User factory(final int id) {
		synchronized (users) {
			if (users.containsKey(id)) { return users.get(id); }
			final User u=new User(id);
			users.put(id,u);
			return u;
		}
	}

	@Nonnull
	private static User createByName(@Nonnull String name,
	                                 final boolean trustname) {
		name=formatUsername(name);
		try {
			final String uuid=GetAgentID.getAgentID(name);
			return findOrCreate(name,uuid,trustname);
		}
		catch (final Throwable t) {
			throw new UserInputLookupFailureException("Failed to find avatar object for name or key '"+name+"' "+t.getLocalizedMessage(),t);
		}
	}

	// ---------- INSTANCE ----------
	@Nonnull
	public String getGPHUDLink() { return getGPHUDLink(getUsername(),getId()); }

	public String getUsername() {
		if (usernamecache==null) { usernamecache=getString("username"); }
		return usernamecache;
	}

	@Nonnull
	@Override
	public String getTableName() { return "users"; }

	/**
	 * Same as getUsername()
	 */
	public String getName() { return getUsername(); }

	@Nonnull
	@Override
	public String toString() { return getUsername()+"[#"+getId()+"]"; }

	@Nullable
	public String getDeveloperKey() {
		return dqs("select developerkey from users where id=?",getId());
	}

	public boolean superuser() { return getId()==1 && "Iain Maltz".equalsIgnoreCase(getUsername()); }

	public boolean hasDeveloperKey() {
		try {
			final String s=getDeveloperKey();
			if (s==null || s.isEmpty()) { return false; }
			return true;
		}
		catch (@Nonnull final NoDataException e) { return false; }
	}

	@Nonnull
	public String generateSSO() {
		final String token=Tokens.generateToken();
		final int expires=UnixTime.getUnixTime()+Config.SSOWINDOWSECONDS;
		d("update users set ssotoken=?,ssoexpires=? where "+getIdColumn()+"=?",token,expires,getId());
		return token;
	}

	/**
	 * Set or remove a developer key
	 *
	 * @param developerkey New key, may be null to remove the key
	 */
	public void setDeveloperKey(@Nullable final String developerkey) {
		if (developerkey==null) {
			SL.getDB().d("update users set developerkey=null where id=?",getId());
		}
		else {
			SL.getDB().d("update users set developerkey=? where id=?",developerkey,getId());
		}
	}

	public boolean isSuperAdmin() {
		final int isadmin=dqinn("select superadmin from users where id=?",getId());
		return isadmin==1;
	}

	public void setPassword(@Nonnull final String password,
	                        final String clientip) {
		if (password.length()<6) { throw new UserInputTooShortException("Password not long enough"); }
		d("update users set password=? where id=?",Passwords.createHash(password),getId());
		SL.getLogger().info("User "+getUsername()+" has set password from "+clientip);
	}

	public void bill(final int ammount,
	                 final String description) {
		final int serial;
		try {serial=lock();}
		catch (@Nonnull final LockException e) {
			throw new UserInputStateException("Your balance is currently being updated elsewhere, please retry in a moment");
		}
		try {
			final int balance=balance();
			if (balance<ammount) {
				throw new UserInsufficientCreditException("Insufficient balance (L$"+balance+") to pay charge L$"+ammount);
			}
			d("insert into journal(tds,userid,ammount,description) values(?,?,?,?)",UnixTime.getUnixTime(),getId(),-ammount,description);
		}
		finally { unlock(serial); }
	}

	public boolean checkPassword(@Nonnull final String password) {
		final String hash=dqs("select password from users where id=?",getId());
		if (hash==null || hash.isEmpty()) {
			SL.getLogger().warning("Attempt to log in with a null or empty password hash for user "+getUsername());
			return false;
		}
		return Passwords.verifyPassword(password,hash);
	}

	public int balance() {
		try {
			return dqinn("select sum(ammount) from journal where userid=?",getId());
		}
		catch (@Nonnull final NoDataException e) { return 0; }
	}

	@Nonnull
	public Set<Subscription> getSubscriptions(@Nullable final Pricing.SERVICE service,
	                                          final boolean activeonly,
	                                          final boolean paidonly) {
		final Results res;
		int paiduntilfilter=UnixTime.getUnixTime();
		if (!paidonly) { paiduntilfilter=0; }
		final String activeonlysql;
		if (activeonly) { activeonlysql=" and active=1"; }
		else { activeonlysql=""; }
		if (service!=null) {
			res=dq("select id from subscriptions where ownerid=? and servicetype=? and paiduntil>?"+activeonlysql,getId(),service.getValue(),paiduntilfilter);
		}
		else {
			res=dq("select id from subscriptions where ownerid=? and paiduntil>?"+activeonlysql,getId(),paiduntilfilter);
		}
		final Set<Subscription> subs=new HashSet<>();
		for (final ResultsRow r: res) {
			subs.add(new Subscription(r.getInt("id")));
		}
		return subs;
	}

	@Nullable
	public String getEmail() { return getStringNullable("email"); }

	@Nullable
	public String getNewEmail() { return getStringNullable("newemail"); }

	/**
	 * Sets the new email address, returning the token that must be used to validate it.
	 *
	 * @return String token used to validate the email address
	 */
	@Nonnull
	public String setNewEmail(final String newemail) {
		final int expires=UnixTime.getUnixTime()+Config.NEWEMAIL_TOKEN_LIFESPAN;
		final String token=Tokens.generateToken();
		d("update users set newemail=?,newemailtoken=?,newemailexpires=? where id=?",newemail,token,expires,getId());
		return token;
	}

	/**
	 * Return the avatar's UUID.
	 *
	 * @return UUID (Avatar Key)
	 */
	@Nonnull
	public String getUUID() {
		return getString("avatarkey");
	}

	public void confirmNewEmail(@Nullable final String token) {
		final ResultsRow r=dqone("select newemail,newemailtoken,newemailexpires from users where id=?",getId());
		final String newemail=r.getStringNullable("newemail");
		final String newtoken=r.getStringNullable("newemailtoken");
		final int expires=r.getInt("newemailexpires");
		if (token==null || token.isEmpty()) { throw new UserInputEmptyException("No token passed"); }
		if (!token.equals(newtoken)) { throw new UserInputStateException("Email token does not match"); }
		if (expires<UnixTime.getUnixTime()) {
			throw new UserInputStateException("Email token has expired, please register new email address again");
		}
		// token matches, not expired, promote the address
		d("update users set newemail=null, newemailtoken=null, newemailexpires=0, email=? where id=?",newemail,getId());

	}

	@Nonnull
	public String getTimeZone() {
		final String s=getStringNullable("timezone");
		if (s==null) { return "America/Los_Angeles"; }
		if ("SLT".equals(s)) { return "America/Los_Angeles"; }
		return s;
	}

	/**
	 * Gets the avatar's last active timestamp.
	 *
	 * @return The last active time for an avatar, possibly null.
	 */
	@Nonnull
	public Integer getLastActive() {
		return dqinn("select lastactive from users where id=?",getId());
	}

	public void setTimeZone(final String timezone) { set("timezone",timezone); }

	/**
	 * Sets the last used instance for the avatar - used for logging in to the web portal.
	 *
	 * @param i Instance to set to
	 */
	public void setLastInstance(@Nonnull final Instance i) {
		d("update users set lastgphudinstance=? where id=?",i.getId(),getId());
	}

	@Override
	public int compareTo(@NotNull final User o) {
		return getUsername().compareToIgnoreCase(o.getUsername());
	}

	// ----- Internal Instance -----
	private void setUsername(@Nonnull String username) {
		username=formatUsername(username);
		d("update users set username=? where id=?",username,getId());
		usernamecache=username;
	}
}
