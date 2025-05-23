package net.coagulate.SL.Data;

import net.coagulate.Core.Database.DBException;
import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemBadValueException;
import net.coagulate.Core.Exceptions.System.SystemExecutionException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.*;
import net.coagulate.Core.Tools.Cache;
import net.coagulate.Core.Tools.MailTools;
import net.coagulate.Core.Tools.Passwords;
import net.coagulate.Core.Tools.Tokens;
import net.coagulate.SL.CacheConfig;
import net.coagulate.SL.Config;
import net.coagulate.SL.GetAgentID;
import net.coagulate.SL.SL;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.logging.Level.SEVERE;
import static net.coagulate.Core.Tools.UnixTime.getUnixTime;

/**
 * @author Iain Price
 */
public class User extends StandardSLTable implements Comparable<User> {
	
	private static final Map<Integer,User>                          users           =new HashMap<>();
	public static        Cache<User,Map<String,Map<String,String>>> preferencesCache=
			Cache.getCache("slcore/userpferences",60*60);
	
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
	
	private String userNameCache;
	
	@Nullable
	public static User resolveDeveloperKey(@Nullable final String key) {
		if (key==null||key.isEmpty()) {
			return null;
		}
		try {
			final int userid=SL.getDB().dqiNotNull("select id from users where developerkey=?",key);
			return get(userid);
		} catch (@Nonnull final NoDataException e) {
			return null;
		}
	}
	
	// public static User get(final String username) { return get(username,false); }
	
	public static User get(final int id) {
		return factory(id);
	}
	
	// ----- Internal Statics -----
	private static User factory(final int id) {
		synchronized(users) {
			if (users.containsKey(id)) {
				return users.get(id);
			}
			final User u=new User(id);
			users.put(id,u);
			return u;
		}
	}
	
	@Nullable
	public static User getSSO(final String token) {
		// purge old tokens
		SL.getDB().d("update users set ssotoken=null,ssoexpires=null where ssoexpires<?",getUnixTime());
		try {
			final int match=SL.getDB().dqiNotNull("select id from users where ssotoken=?",token);
			SL.getDB().d("update users set ssotoken=null,ssoexpires=null where id=?",match);
			return get(match);
		} catch (@Nonnull final NoDataException e) {
			return null;
		}
	}
	
	public static User findUsernameNullable(@Nonnull final String name,final boolean trustName) {
		try {
			return findUsername(name,trustName);
		} catch (final RuntimeException ignore) {
			return null;
		} // hmm
	}
	
	/**
	 * Find avatar in database, by name or key.
	 *
	 * @return Avatar object
	 */
	@Nonnull
	public static User findUsername(@Nonnull final String name,final boolean trustName) {
		final String finalName=formatUsername(name);
		return userNameResolverCache.get(name,()->{
			Integer id=null;
			try {
				id=SL.getDB().dqi("select id from users where username=?",finalName);
			} catch (final NoDataException ignore) {
			}
			if (id==null) {
				return createByName(finalName,trustName);
			} else {
				return User.get(id);
			}
		});
	}
	
	private static final Cache<String,User> userNameResolverCache=
			Cache.getCache("SL/UserNameResolver",CacheConfig.PERMANENT_CONFIG,true);
	
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
		if (!username.contains(" ")) {
			username=username+" Resident";
		} // merge "user" and "user resident"
		username=username.trim();
		final String[] parts=username.split(" ");
		if (parts.length!=2) {
			throw new SystemImplementationException(
					"Formatting username '"+original+"' gave '"+username+"' which has "+parts.length+
					" parts, which is not 2...");
		}
		// convert to "uppercase-first"
		final String firstName=parts[0];
		final String lastName=parts[1];
		// only append the surname if /not/ resident
		String output=firstName;
		if (!"Resident".equalsIgnoreCase(lastName)) {
			output=output+" "+lastName;
		}
		return output;
	}
	
	@Nonnull
	private static User createByName(@Nonnull String name,final boolean trustName) {
		if (Config.getGrid()!=Config.GRID.SECONDLIFE) {
			throw new SystemExecutionException(
					"It is not possible to createByName on a grid other than Second Life due to the lack of a name resolution service");
		}
		name=formatUsername(name);
		try {
			final String uuid=GetAgentID.getAgentID(name);
			return findOrCreate(name,uuid,trustName);
		} catch (final Throwable t) {
			throw new UserInputLookupFailureException(
					"Failed to find avatar object for name or key '"+name+"' "+t.getLocalizedMessage(),t);
		}
	}
	
	/**
	 * Find or create a user entry in the database.
	 * <p>
	 * Call will filter "usernames" of ??? (???) (Loading...) or Loading.. all of which seem to be garbage SL generates.
	 * Note trustName should be set to false if the username is retrieved from HTTP headers from Objects which seem to update later than other methods.
	 * For usernames retrieved from the new GetAgentID LL API this should be set to TRUE to update the database with the new name.
	 *
	 * @param inName      Optional name of the avatar, creation will not proceed without this value, otherwise it may be null
	 * @param key       Mandatory UUID string of the avatar
	 * @param trustName Trust the username supplied to the point we will update our recorded username if it differs.
	 * @return The User object for this avatar
	 *
	 * @throws SystemBadValueException if it is necessary to create the user and both username and key are not presented
	 */
	public static User findOrCreate(@Nullable final String inName,@Nonnull final String key,final boolean trustName) {
		return uuidLookup.get(key,()->{
			final String name;
			if (inName==null||"???".equals(inName)||"(???)".equals(inName)||"Loading...".equals(inName)||
			    "(Loading...)".equals(inName)) {
				name="";
			} else {
				if (!inName.isEmpty()) {
					name=formatUsername(inName);
				} else {
					name="";
				}
			}
			Integer userid=null;
			try {
				userid=SL.getDB().dqi("select id from users where (avatarkey=?)",key);
			} catch (@Nonnull final NoDataException ignored) {
			}
			if (userid==null) {
				if (key.isEmpty()) {
					throw new SystemBadValueException("Empty avatar key blocks creation");
				}
				if (name.isEmpty()) {
					throw new SystemBadValueException("Empty avatar name blocks creation (for key "+key+")");
				}
				try {
					// special key used by the SYSTEM avatar
					if (!"DEADBEEF".equals(key)) {
						SL.log("User").info("Creating new avatar entry for '"+name+"'");
					}
					SL.getDB()
					  .d("insert into users(username,lastactive,avatarkey) values(?,?,?)",name,getUnixTime(),key);
					uuidLookup.purge(key);
					userNameResolverCache.purge(name);
				} catch (@Nonnull final DBException ex) {
					SL.log("User").log(SEVERE,"Exception creating avatar "+name,ex);
					throw ex;
				}
				try {
					userid=SL.getDB().dqi("select id from users where avatarkey=?",key);
				} catch (@Nonnull final NoDataException ignored) {
				}
			}
			if (userid==null) {
				SL.log("User").severe("Failed to find avatar '"+name+"' after creating it");
				throw new NoDataException("Failed to find avatar object for name '"+name+"' after we created it!");
			}
			final User u=get(userid);
			final String currentUsername=u.getUsername();
			//System.out.println("Find or create for "+key+" -> "+userid+" current "+currentUsername+" supplied "+name);
			if (trustName&&!name.isEmpty()&&!currentUsername.equalsIgnoreCase(name)) {
				u.setUsername(name);
				try {
					SL.report("Name change:"+currentUsername+" -> "+name+" for "+key,
					          new Exception("Here "+currentUsername+" -> "+name),
					          null);
					MailTools.mail("Name change:"+currentUsername+" -> "+name+" for "+key,
					               "Name change:"+currentUsername+" -> "+name+" for "+key);
				} catch (final MessagingException exception) {
					SL.report("Exception during mailer (!)",exception,null);
				}
			}
			if (!name.isBlank()) {
				userNameResolverCache.set(name,u);
			}
			uuidLookup.set(key,u);
			return u;
		});
	}
	
	public String getUsername() {
		if (userNameCache==null) {
			userNameCache=getString("username");
		}
		return userNameCache;
	}
	
	public void setUsername(@Nonnull final String username) {
		d("update users set username=? where id=?",username,getId());
		userNameCache=username;
	}
	
	// GPHUD legacy
	@Nonnull
	public static Map<Integer,String> loadMap() {
		final Map<Integer,String> results=new TreeMap<>();
		final Results rows=SL.getDB().dq("select id,username from users");
		for (final ResultsRow r: rows) {
			results.put(r.getInt("id"),getGPHUDLink(r.getString("username"),r.getInt("id")));
		}
		return results;
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
		final User user=findUserKeyNullable(uuid);
		if (user==null) {
			throw new UserInputLookupFailureException("Found no avatar registered against UUID "+uuid);
		}
		return user;
	}
	
	@Nullable
	public static User findUserKeyNullable(@Nonnull final String uuid) {
		return uuidLookup.get(uuid,()->{
			try {
				final Integer id=SL.getDB().dqi("select id from users where avatarkey like ?",uuid);
				if (id==null) {
					return null;
				}
				return get(id);
			} catch (final NoDataException e) {
				return null;
			}
		});
	}
	
	private static final Cache<String,User> uuidLookup=
			Cache.getCache("SL/UUIDUserMapCache",CacheConfig.PERMANENT_CONFIG,true);
	
	public static Map<Integer,String> getIdToNameMap() {
		final Map<Integer,String> avatarNames=new TreeMap<>();
		for (final ResultsRow r: SL.getDB().dq("select id,username from users")) {
			avatarNames.put(r.getInt("id"),r.getString("username"));
		}
		return avatarNames;
	}
	
	public static String reformatUsernames() {
		final StringBuilder s=new StringBuilder();
		for (final ResultsRow row: SL.getDB().dq("select id,username from users")) {
			final String username=row.getString("username");
			try {
				if (!username.equals(formatUsername(username))) {
					final String newusername=formatUsername(username);
					s.append(username).append(" -> ").append(newusername).append("\n");
					SL.getDB().d("update users set username=? where id=?",newusername,row.getInt("id"));
				}
			} catch (final Exception e) {
				s.append(username).append(" exceptioned ").append(e).append("\n");
			}
		}
		return s.toString();
	}
	
	/** A horrible method that is not to be used much.  Thanks. */
	public static Set<User> getAllUsers() {
		final Set<User> ret=new HashSet<>();
		for (final ResultsRow row: SL.getDB().dq("select id from users")) {
			ret.add(User.get(row.getInt("id")));
		}
		return ret;
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	public String getGPHUDLink() {
		return getGPHUDLink(getUsername(),getId());
	}
	
	@Nonnull
	public static String getGPHUDLink(final String name,final int id) {
		return "<a href=\"/GPHUD/avatars/view/"+id+"\">"+name.replaceAll(" ","&nbsp;")+"</a>";
	}
	
	@Nonnull
	@Override
	public String getTableName() {
		return "users";
	}
	
	@Nullable
	public String getDeveloperKey() {
		return dqs("select developerkey from users where id=?",getId());
	}
	
	/**
	 * Same as getUsername()
	 */
	public String getName() {
		return getUsername();
	}
	
	@Nonnull
	@Override
	public String toString() {
		return getUsername()+"[#"+getId()+"]";
	}
	
	public boolean hasDeveloperKey() {
		try {
			final String s=getDeveloperKey();
			return s!=null&&!s.isEmpty();
		} catch (@Nonnull final NoDataException e) {
			return false;
		}
	}
	
	public boolean isSuperAdmin() {
		final int isAdmin=dqinn("select superadmin from users where id=?",getId());
		return isAdmin==1;
	}
	
	@Nonnull
	public String generateSSO() {
		final String token=Tokens.generateToken();
		final int expires=getUnixTime()+Config.getSSOWindow();
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
		} else {
			SL.getDB().d("update users set developerkey=? where id=?",developerkey,getId());
		}
	}
	
	public void setPassword(@Nonnull final String password,final String clientIp) {
		if (password.length()<6) {
			throw new UserInputTooShortException("Password not long enough");
		}
		d("update users set password=? where id=?",Passwords.createHash(password),getId());
		SL.log().info("User "+getUsername()+" has set password from "+clientIp);
	}
	
	public void bill(final int ammount,final String description) {
		if (!SystemManagement.primaryNode()) {
			throw new UserInputStateException("This node is not the authorised master node, please retry in a minute");
		}
		final int balance=balance();
		if (balance<ammount) {
			throw new UserInsufficientCreditException("Insufficient balance (L$"+balance+") to pay charge L$"+ammount);
		}
		d("insert into journal(tds,userid,ammount,description) values(?,?,?,?)",
		  getUnixTime(),
		  getId(),
		  -ammount,
		  description);
	}

	/*
	@Nonnull
	public Set<Subscription> getSubscriptions(@Nullable final Pricing.SERVICE service,
	                                          final boolean activeOnly,
	                                          final boolean paidOnly) {
		final Results res;
		int paidUntilFilter=UnixTime.getUnixTime();
		if (!paidOnly) { paidUntilFilter=0; }
		final String activeOnlySql;
		if (activeOnly) { activeOnlySql=" and active=1"; }
		else { activeOnlySql=""; }
		if (service!=null) {
			res=dq("select id from subscriptions where ownerid=? and servicetype=? and paiduntil>?"+activeOnlySql,getId(),service.getValue(),paidUntilFilter);
		}
		else {
			res=dq("select id from subscriptions where ownerid=? and paiduntil>?"+activeOnlySql,getId(),paidUntilFilter);
		}
		final Set<Subscription> subs=new HashSet<>();
		for (final ResultsRow r: res) {
			subs.add(new Subscription(r.getInt("id")));
		}
		return subs;
	}
	 */
	
	public int balance() {
		try {
			return dqinn("select sum(ammount) from journal where userid=?",getId());
		} catch (@Nonnull final NoDataException e) {
			return 0;
		}
	}
	
	public boolean checkPassword(@Nonnull final String password) {
		final String hash=dqs("select password from users where id=?",getId());
		if (hash==null||hash.isEmpty()) {
			SL.log().warning("Attempt to log in with a null or empty password hash for user "+getUsername());
			return false;
		}
		return Passwords.verifyPassword(password,hash);
	}
	
	@Nullable
	public String getEmail() {
		return getStringNullable("email");
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
	
	@Nullable
	public String getNewEmail() {
		return getStringNullable("newemail");
	}
	
	/**
	 * Sets the new email address, returning the token that must be used to validate it.
	 *
	 * @return String token used to validate the email address
	 */
	@Nonnull
	public String setNewEmail(final String newemail) {
		final int expires=getUnixTime()+Config.emailTokenLifespan();
		final String token=Tokens.generateToken();
		d("update users set newemail=?,newemailtoken=?,newemailexpires=? where id=?",newemail,token,expires,getId());
		return token;
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
	
	public void confirmNewEmail(@Nullable final String token) {
		final ResultsRow r=dqone("select newemail,newemailtoken,newemailexpires from users where id=?",getId());
		final String newEmail=r.getStringNullable("newemail");
		final String newToken=r.getStringNullable("newemailtoken");
		final int expires=r.getInt("newemailexpires");
		if (token==null||token.isEmpty()) {
			throw new UserInputEmptyException("No token passed");
		}
		if (!token.equals(newToken)) {
			throw new UserInputStateException("Email token does not match");
		}
		if (expires<getUnixTime()) {
			throw new UserInputStateException("Email token has expired, please register new email address again");
		}
		// token matches, not expired, promote the address
		d("update users set newemail=null, newemailtoken=null, newemailexpires=0, email=? where id=?",newEmail,getId());
		
	}
	
	/**
	 * Sets the last used instance for the avatar - used for logging in to the web portal.
	 *
	 * @param i Instance to set to
	 */
	public void setLastInstance(final int i) { //TODO consider purging this method, it can be worked out from characters tables.  probably.
		d("update users set lastgphudinstance=? where id=?",i,getId());
	}
	
	@Override
	public int compareTo(@Nonnull final User o) {
		return getUsername().compareToIgnoreCase(o.getUsername());
	}
	
	@Nonnull
	public String getTimeZone() {
		final String s=getStringNullable("timezone");
		if (s==null) {
			return "America/Los_Angeles";
		}
		if ("SLT".equals(s)) {
			return "America/Los_Angeles";
		}
		return s;
	}
	
	public void setTimeZone(final String timezone) {
		set("timezone",timezone);
	}
	
	public boolean isSuspended() {
		return getBool("suspended");
	}
	
	/**
	 * @param application  Application to get preference for
	 * @param key          Preference
	 * @param defaultValue Default value to return if the user has none set
	 * @return The user's preference value
	 */
	@Nonnull
	public String getPreferenceNotNull(@Nonnull final String application,
	                                   @Nonnull final String key,
	                                   @Nonnull final String defaultValue) {
		final String r=getPreferences(application).getOrDefault(key.toLowerCase(),defaultValue);
		if (r!=null) {
			return r;
		}
		return defaultValue;
	}
	
	@Nonnull
	public Map<String,String> getPreferences(@Nonnull final String application) {
		if (getPreferences().containsKey(application.toLowerCase())) {
			return getPreferences().get(application.toLowerCase());
		}
		return new TreeMap<>();
	}
	
	@Nonnull
	public Map<String,Map<String,String>> getPreferences() {
		return preferencesCache.get(this,()->{
			final Map<String,Map<String,String>> preferences=new TreeMap<>();
			for (final ResultsRow row: dq("select * from userpreferences where userid=?",getId())) {
				final String application=row.getString("application");
				final String pref=row.getString("preferencename");
				final String val=row.getString("preferencevalue");
				if (!preferences.containsKey(application)) {
					preferences.put(application,new TreeMap<String,String>());
				}
				preferences.get(application).put(pref,val);
			}
			return preferences;
		});
	}
	
	@Nullable
	public String getPreference(@Nonnull final String application,
	                            @Nonnull final String key,
	                            @Nullable final String defaultValue) {
		if (getPreferences(application).containsKey(key.toLowerCase())) {
			return getPreferences(application).get(key.toLowerCase());
		} else {
			return defaultValue;
		}
	}
	
	public void setPreference(@Nonnull final String application,
	                          @Nonnull final String key,
	                          @Nullable final String value) {
		if (value==null) {
			d("delete from userpreferences where userid=? and application=? and preferencename=?",
			  getId(),
			  application.toLowerCase(),
			  key.toLowerCase());
		} else {
			d("replace into userpreferences(userid,application,preferencename,preferencevalue) values(?,?,?,?)",
			  getId(),
			  application.toLowerCase(),
			  key.toLowerCase(),
			  value);
		}
		preferencesCache.purge(this);
	}
	
	public static void preLoadCache() {
		final AtomicInteger loaded=new AtomicInteger();
		SL.getDB().dqSlow("select id,username,avatarkey from users").forEach((row)->{
			final User u=User.get(row.getInt("id"));
			userNameResolverCache.set(row.getString("username"),u);
			uuidLookup.set(row.getString("avatarkey"),u);
			loaded.getAndIncrement();
		});
		SL.log("PreLoadCache").config("Loaded "+loaded+" user/avatar records");
	}
}
