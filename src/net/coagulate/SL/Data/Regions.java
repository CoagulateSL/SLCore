package net.coagulate.SL.Data;

import net.coagulate.Core.Database.LockException;
import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.SL.SL;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Iain Price
 */
public class Regions extends LockableTable {

	public Regions(int id) { super(id); }

	@Nonnull
	public static Regions getByName(String name) {
		try {
			Integer id = SL.getDB().dqi("select id from regions where region like ?", name);
			return new Regions(id);
		} catch (NoDataException e) {
			SL.getDB().d("insert into regions(region) values(?)", name);
			try {
				Integer id = SL.getDB().dqi("select id from regions where region like ?", name);
				return new Regions(id);
			}
			catch (NoDataException f) { throw new SystemException("Failed to find inserted region in regions table",f); }
		}
	}

	@Nonnull
	@Override
	public String getTableName() { return "regions"; }

	@Nullable
	public String getStatus() { return getString("status"); }

	public int getLastUpdate() {
		Integer lu = getIntNullable("lastperf");
		if (lu == null) { return 0; }
		return lu;
	}

	public void setLastUpdate() {
		try {
			set("lastupdate", UnixTime.getUnixTime());
		} catch (LockException e) {} // not that important here, something else is updating it...
	}

	public void setNewStatus(String status) {
		int time = UnixTime.getUnixTime();
		d("update regions set status=?, since=?, lastupdate=? where id=?", status, time, time, getId());
	}

	public int getSince() {
		Integer since = getIntNullable("since");
		if (since == null) { return 0; }
		return since;
	}

	@Nullable
	public String getName() {
		return getString("region");
	}
}
