package net.coagulate.SL.Data;

import net.coagulate.Core.Database.LockException;
import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.SL.SL;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Iain Price
 */
public class Regions extends LockableTable {

	public Regions(final int id) { super(id); }

	@Nonnull
	public static Regions getByName(final String name) {
		try {
			final int id=SL.getDB().dqinn("select id from regions where region like ?",name);
			return new Regions(id);
		} catch (@Nonnull final NoDataException e) {
			SL.getDB().d("insert into regions(region) values(?)",name);
			try {
				final int id=SL.getDB().dqinn("select id from regions where region like ?",name);
				return new Regions(id);
			} catch (@Nonnull final NoDataException f) {
				throw new SystemConsistencyException("Failed to find inserted region in regions table",f);
			}
		}
	}

	@Nonnull
	@Override
	public String getTableName() { return "regions"; }

	@Nullable
	public String getStatus() { return getStringNullable("status"); }

	public int getLastUpdate() {
		final Integer lu=getIntNullable("lastperf");
		if (lu==null) { return 0; }
		return lu;
	}

	public void setLastUpdate() {
		try {
			set("lastupdate",UnixTime.getUnixTime());
		} catch (@Nonnull final LockException e) {} // not that important here, something else is updating it...
	}

	public void setNewStatus(final String status) {
		final int time=UnixTime.getUnixTime();
		d("update regions set status=?, since=?, lastupdate=? where id=?",status,time,time,getId());
	}

	public int getSince() {
		final Integer since=getIntNullable("since");
		if (since==null) { return 0; }
		return since;
	}

	@Nullable
	public String getName() {
		return getStringNullable("region");
	}
}
