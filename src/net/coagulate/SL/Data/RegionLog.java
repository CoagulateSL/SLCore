package net.coagulate.SL.Data;

import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.SL.SL;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Iain Price
 */
public class RegionLog extends Table {

	@Nullable
	public static Results getLast(@Nonnull Regions region) {
		return SL.getDB().dq("select tds,changetype,oldvalue,newvalue from regionlog where regionid=? order by tds desc limit 0,100", region.getId());
	}

	public static void log(int region, String changetype, String oldvalue, String newvalue, int tds) {
		SL.getDB().d("insert into regionlog(regionid,changetype,oldvalue,newvalue,tds) values(?,?,?,?,?)", region, changetype, oldvalue, newvalue, tds);
	}

	public static void log(@Nonnull Regions region, String changetype, String oldvalue, String newvalue, int tds) {
		log(region.getId(), changetype, oldvalue, newvalue, tds);
	}

	public static void log(int region, String changetype, String oldvalue, String newvalue) {
		log(region, changetype, oldvalue, newvalue, UnixTime.getUnixTime());
	}

	public static void log(@Nonnull Regions region, String changetype, String oldvalue, String newvalue) {
		log(region.getId(), changetype, oldvalue, newvalue, UnixTime.getUnixTime());
	}

	@Nonnull
	@Override
	public String getTableName() { return "regionlog"; }

}
