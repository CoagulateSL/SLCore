package net.coagulate.SL.Data;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.SL.SL;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author Iain Price
 */
public class RegionStats extends Table {
	public static final int HOUR = 60 * 60;
	public static final int DAY = 24 * HOUR;

	public static Float getAverage(@Nonnull Regions reg, String stattype, int time) {
		return SL.getDB().dqf(false, "select avg(statavg) from regionstats where regionid=? and timestamp>? and stattype=?", reg.getId(), UnixTime.getUnixTime() - time, stattype);
	}

	public static Float getMin(@Nonnull Regions reg, String stattype, int time) {
		return SL.getDB().dqf(false, "select min(statavg) from regionstats where regionid=? and timestamp>? and stattype=?", reg.getId(), UnixTime.getUnixTime() - time, stattype);
	}

	public static Float getMax(@Nonnull Regions reg, String stattype, int time) {
		return SL.getDB().dqf(false, "select max(statavg) from regionstats where regionid=? and timestamp>? and stattype=?", reg.getId(), UnixTime.getUnixTime() - time, stattype);
	}

	@Nonnull
	public static Iterable<String> getStatTypes(@Nonnull Regions region, int time) {
		Set<String> stattypes = new HashSet<>();
		for (ResultsRow row : SL.getDB().dq("select distinct stattype from regionstats where regionid=? and timestamp>?", region.getId(), UnixTime.getUnixTime() - time)) {
			stattypes.add(row.getString());
		}
		return stattypes;
	}

	public static void log(int region, int timestamp, String statstype, float min, float max, float avg, float sd) {
		//SL.getLogger("RegionStas").fine("Region "+region+" has timestamp "+timestamp+" with stats "+statstype+"/"+min+"/"+max+"/"+avg+"/"+sd);
		SL.getDB().d("insert into regionstats(regionid,timestamp,stattype,statmin,statmax,statavg,statsd,samplesize) values(?,?,?,?,?,?,?,?)", region, timestamp, statstype, min, max, avg, sd, "SINGLE");
	}

	public static void log(@Nonnull Regions region, int timestamp, String statstype, float min, float max, float avg, float sd) {
		log(region.getId(), timestamp, statstype, min, max, avg, sd);
	}

	public static Results graphableData(@Nonnull Regions r, String stattype, int from, int to, int x) {
		DBConnection d = SL.getDB();
		// 'x' defines how many 'slots' we have for data (horizontal pixels).  eventually borders and stuff so
		int timerange = to - from;
		if (timerange < 0) {
			int swap = from;
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
		DBConnection d = SL.getDB();
		Logger log = SL.getLogger("RegionPerformance.RegionStats");
		int start = UnixTime.getUnixTime();
		int rollups = 0;
		for (ResultsRow r : d.dq("select floor(timestamp/(60*60)) as basetime,regionid,stattype,min(statmin) as newmin,max(statmax) as newmax,avg(statavg) as newavg,avg(statsd) as newsd from regionstats where timestamp<? and samplesize='SINGLE' group by basetime,regionid,stattype", start - (60 * 60 * 24 * 3))) {
			if ((UnixTime.getUnixTime() - start) > 30) {
				log.fine("Stopping incomplete archival run due to runtime>30 seconds");
				return;
			}
			int regionid = r.getInt("regionid");
			int basetime = r.getInt("basetime");
			String stattype = r.getString("stattype");
			float min = r.getFloat("newmin");
			float max = r.getFloat("newmax");
			float avg = r.getFloat("newavg");
			float sd = r.getFloat("newsd");
			basetime = basetime * 60 * 60; // we divided above, to split into hourly blocks, but we need a full time reference
			basetime += (30 * 60); // and push half an hour into the time period.
			d.d("insert into regionstats(regionid,timestamp,stattype,statmin,statmax,statavg,statsd,samplesize) values(?,?,?,?,?,?,?,?)", regionid, basetime, stattype, min, max, avg, sd, "HOURLY");
			rollups++;
			//log.finer("Rolling "+d.dqi(true,"select count(*) from regionstats where regionid=? and timestamp>=? and timestamp<? and stattype=? and samplesize='SINGLE'",regionid,basetime-(30*60),basetime+(30*60),stattype)+" records into one HOURLY record");
			d.d("delete from regionstats where regionid=? and timestamp>=? and timestamp<? and stattype=? and samplesize='SINGLE'", regionid, basetime - (30 * 60), basetime + (30 * 60), stattype);

		}
		if (rollups > 0) { log.fine("Completed region stats archiving, batched into " + rollups + " HOURLY samples"); }
		rollups = 0;
		for (ResultsRow r : d.dq("select floor(timestamp/(60*60*24)) as basetime,regionid,stattype,min(statmin) as newmin,max(statmax) as newmax,avg(statavg) as newavg,avg(statsd) as newsd from regionstats where timestamp<? and samplesize='HOURLY' group by basetime,regionid,stattype", start - (60 * 60 * 24 * 7 * 2))) {
			if ((UnixTime.getUnixTime() - start) > 30) {
				log.fine("Stopping incomplete archival run due to runtime>30 seconds");
				return;
			}
			int regionid = r.getInt("regionid");
			int basetime = r.getInt("basetime");
			String stattype = r.getString("stattype");
			float min = r.getFloat("newmin");
			float max = r.getFloat("newmax");
			float avg = r.getFloat("newavg");
			float sd = r.getFloat("newsd");
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
