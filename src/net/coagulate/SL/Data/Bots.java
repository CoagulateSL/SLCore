package net.coagulate.SL.Data;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Iain Price
 */
public class Bots extends LockableTable {

	public Bots(final int id) {
		super(id);
	}

	@Nonnull
	@Override
	public String getTableName() {
		return "bots";
	}

	@Nullable
	public String getFirstName() { return getStringNullable("firstname"); }

	@Nullable
	public String getLastName() { return getStringNullable("lastname"); }

	@Nullable
	public String getPassword() { return getStringNullable("password"); }

	@Nonnull
	public User getOwner() { return User.get(getInt("ownerid")); }

}
