package net.coagulate.SL.Data;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.SL.SL;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author Iain Price
 */
public class RegionStats extends Table {
	public static final int HOUR = 60 * 60;
	public static final int DAY = 24 * HOUR;

	@Nullable
	public static Float getAverage(@Nonnull final Regions reg, final String stattype, final int time) {
		try { return SL.getDB().dqf( "select avg(statavg) from regionstats where regionid=? and timestamp>? and stattype=?", reg.getId(), UnixTime.getUnixTime() - time, stattype); }
		catch (final NoDataException e) { return null; }
	}

	@Nullable
	public static Float getMin(@Nonnull final Regions reg, final String stattype, final int time) {
		try { return SL.getDB().dqf( "select min(statavg) from regionstats where regionid=? and timestamp>? and stattype=?", reg.getId(), UnixTime.getUnixTime() - time, stattype); }
		catch (final NoDataException e) { return null; }
	}

	@Nullable
	public static Float getMax(@Nonnull final Regions reg, final String stattype, final int time) {
		try { return SL.getDB().dqf( "select max(statavg) from regionstats where regionid=? and timestamp>? and stattype=?", reg.getId(), UnixTime.getUnixTime() - time, stattype); }
		catch (final NoDataException e) { return null; }
	}

	@Nonnull
	public static Iterable<String> getStatTypes(@Nonnull final Regions region, final int time) {
		final Set<String> stattypes = new HashSet<>();
		for (final ResultsRow row : SL.getDB().dq("select distinct stattype from regionstats where regionid=? and timestamp>?", region.getId(), UnixTime.getUnixTime() - time)) {
			stattypes.add(row.getStringNullable());
		}
		return stattypes;
	}

	public static void log(final int region, final int timestamp, final String statstype, final float min, final float max, final float avg, final float sd) {
		//SL.getLogger("RegionStas").fine("Region "+region+" has timestamp "+timestamp+" with stats "+statstype+"/"+min+"/"+max+"/"+avg+"/"+sd);
		SL.getDB().d("insert into regionstats(regionid,timestamp,stattype,statmin,statmax,statavg,statsd,samplesize) values(?,?,?,?,?,?,?,?)", region, timestamp, statstype, min, max, avg, sd, "SINGLE");
	}

	public static void log(@Nonnull final Regions region, final int timestamp, final String statstype, final float min, final float max, final float avg, final float sd) {
		log(region.getId(), timestamp, statstype, min, max, avg, sd);
	}

	@Nonnull
	public static Results graphableData(@Nonnull final Regions r, final String stattype, int from, int to, final int x) {
		final DBConnection d = SL.getDB();
		// 'x' defines how many 'slots' we have for data (horizontal pixels).  eventually borders and stuff so
		int timerange = to - from;
		if (timerange < 0) {
			final int swap = from;
			from = to;
			to = swap;
			timerange = to - from;
		}
		if (timerange == 0) { throw new UserException("No time range covered?"); }


		// range is "from" to "to", subtract the from, divide by the total range, scale to size
		return d.dqSlow("select "
				+ "round(?*((timestamp-?)/?)) as x,"
				+ "timestamp,"
				+ "min(statmin) as plotmin,"
				+ "max(statmax) as plotmax,"
				+ "avg(statavg) as plotavg,"
				+ "avg(statsd) as plotsd,"
				+ "samplesize "
				+ "from regionstats "
				+ "where timestamp>=? "
				+ "and timestamp<=? "
				+ "and stattype=? "
				+ "and regionid=? "
				+ "group by x "
				+ "order by timestamp asc", x, from, timerange, from, to, stattype, r.getId());
	}

	public static void archiveOld() {
		final DBConnection d = SL.getDB();
		final Logger log = SL.getLogger("RegionPerformance.RegionStats");
		final int start = UnixTime.getUnixTime();
		int rollups = 0;
		for (final ResultsRow r : d.dq("select floor(timestamp/(60*60)) as basetime,regionid,stattype,min(statmin) as newmin,max(statmax) as newmax,avg(statavg) as newavg,avg(statsd) as newsd from regionstats where timestamp<? and samplesize='SINGLE' group by basetime,regionid,stattype", start - (60 * 60 * 24 * 3))) {
			if ((UnixTime.getUnixTime() - start) > 30) {
				log.fine("Stopping incomplete archival run due to runtime>30 seconds");
				return;
			}
			final int regionid = r.getIntNullable("regionid");
			int basetime = r.getIntNullable("basetime");
			final String stattype = r.getStringNullable("stattype");
			final float min = r.getFloat("newmin");
			final float max = r.getFloat("newmax");
			final float avg = r.getFloat("newavg");
			final float sd = r.getFloat("newsd");
			basetime = basetime * 60 * 60; // we divided above, to split into hourly blocks, but we need a full time reference
			basetime += (30 * 60); // and push half an hour into the time period.
			d.d("insert into regionstats(regionid,timestamp,stattype,statmin,statmax,statavg,statsd,samplesize) values(?,?,?,?,?,?,?,?)", regionid, basetime, stattype, min, max, avg, sd, "HOURLY");
			rollups++;
			//log.finer("Rolling "+d.dqi(true,"select count(*) from regionstats where regionid=? and timestamp>=? and timestamp<? and stattype=? and samplesize='SINGLE'",regionid,basetime-(30*60),basetime+(30*60),stattype)+" records into one HOURLY record");
			d.d("delete from regionstats where regionid=? and timestamp>=? and timestamp<? and stattype=? and samplesize='SINGLE'", regionid, basetime - (30 * 60), basetime + (30 * 60), stattype);

		}
		if (rollups > 0) { log.fine("Completed region stats archiving, batched into " + rollups + " HOURLY samples"); }
		rollups = 0;
		for (final ResultsRow r : d.dq("select floor(timestamp/(60*60*24)) as basetime,regionid,stattype,min(statmin) as newmin,max(statmax) as newmax,avg(statavg) as newavg,avg(statsd) as newsd from regionstats where timestamp<? and samplesize='HOURLY' group by basetime,regionid,stattype", start - (60 * 60 * 24 * 7 * 2))) {
			if ((UnixTime.getUnixTime() - start) > 30) {
				log.fine("Stopping incomplete archival run due to runtime>30 seconds");
				return;
			}
			final int regionid = r.getIntNullable("regionid");
			int basetime = r.getIntNullable("basetime");
			final String stattype = r.getStringNullable("stattype");
			final float min = r.getFloat("newmin");
			final float max = r.getFloat("newmax");
			final float avg = r.getFloat("newavg");
			final float sd = r.getFloat("newsd");
			basetime = basetime * 60 * 60 * 24; // we divided above, to split into hourly blocks, but we need a full time reference
			basetime += (30 * 60 * 24); // and push half an hour into the time period.
			d.d("insert into regionstats(regionid,timestamp,stattype,statmin,statmax,statavg,statsd,samplesize) values(?,?,?,?,?,?,?,?)", regionid, basetime, stattype, min, max, avg, sd, "DAILY");
			rollups++;
			//log.finer("Rolling "+d.dqi(true,"select count(*) from regionstats where regionid=? and timestamp>=? and timestamp<? and stattype=? and samplesize='SINGLE'",regionid,basetime-(30*60),basetime+(30*60),stattype)+" records into one HOURLY record");
			d.d("delete from regionstats where regionid=? and timestamp>=? and timestamp<? and stattype=? and samplesize='HOURLY'", regionid, basetime - (30 * 60 * 24), basetime + (30 * 60 * 24), stattype);

		}
		if (rollups > 0) { log.fine("Completed region stats archiving, batched into " + rollups + " DAILY samples"); }
	}

	@Nonnull
	@Override
	public String getTableName() { return "regionstats"; }
}
