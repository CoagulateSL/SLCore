package net.coagulate.SL.Data;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class Bots extends LockableTable {

	public Bots(int id) {
		super(id);
	}

	@Nonnull
	@Override
	public String getTableName() {
		return "bots";
	}

	public String getFirstName() { return getString("firstname"); }

	public String getLastName() { return getString("lastname"); }

	public String getPassword() { return getString("password"); }

	public User getOwner() { return User.get(getInt("ownerid")); }

}
