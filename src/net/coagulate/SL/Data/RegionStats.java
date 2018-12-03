package net.coagulate.SL.Data;

import java.util.logging.Logger;
import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.SL.SL;

/**
 *
 * @author Iain Price
 */
public class RegionStats extends Table {
    public static final int HOUR=60*60;
    public static final int DAY=24*HOUR;

    public static Float getAverage(Regions reg, String stattype, int time) {
        return SL.getDB().dqf(false, "select avg(statavg) from regionstats where regionid=? and timestamp>? and stattype=?",reg.getId(),UnixTime.getUnixTime()-time,stattype);
    }
    public static Float getMin(Regions reg, String stattype, int time) {
        return SL.getDB().dqf(false, "select min(statavg) from regionstats where regionid=? and timestamp>? and stattype=?",reg.getId(),UnixTime.getUnixTime()-time,stattype);
    }
    public static Float getMax(Regions reg, String stattype, int time) {
        return SL.getDB().dqf(false, "select max(statavg) from regionstats where regionid=? and timestamp>? and stattype=?",reg.getId(),UnixTime.getUnixTime()-time,stattype);
    }

    @Override
    public String getTableName() { return "regionstats"; }
    
    public static void log(int region,int timestamp,String statstype,float min,float max,float avg,float sd) {
        //SL.getLogger("RegionStas").fine("Region "+region+" has timestamp "+timestamp+" with stats "+statstype+"/"+min+"/"+max+"/"+avg+"/"+sd);
        SL.getDB().d("insert into regionstats(regionid,timestamp,stattype,statmin,statmax,statavg,statsd,samplesize) values(?,?,?,?,?,?,?,?)",region,timestamp,statstype,min,max,avg,sd,"SINGLE");
    }
    public static void log(Regions region,int timestamp,String statstype,float min,float max,float avg,float sd) {
        log(region.getId(),timestamp,statstype,min,max,avg,sd);
    }
    
    public static Results graphableData(Regions r,String stattype,int from, int to, int x) {
        DBConnection d = SL.getDB();
        // 'x' defines how many 'slots' we have for data (horizontal pixels).  eventually borders and stuff so
        int xsize=x;
        int timerange=to-from;
        if (timerange<0) { 
            int swap=from; from=to; to=swap; timerange=to-from;
        }
        if (timerange==0) { throw new UserException("No time range covered?"); }
        
        
        // range is "from" to "to", subtract the from, divide by the total range, scale to size
        return d.dq("select "
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
                + "order by timestamp asc",xsize,from,timerange,from,to,stattype,r.getId());
    }
    
    
    public static void archiveOld() {
        DBConnection d = SL.getDB();
        Logger log = SL.getLogger("RegionPerformance.RegionStats");
        int start=UnixTime.getUnixTime();
        
        for (ResultsRow r:d.dq("select floor(timestamp/(60*60)) as basetime,regionid,stattype,min(statmin) as newmin,max(statmax) as newmax,avg(statavg) as newavg,avg(statsd) as newsd from regionstats where timestamp<? and samplesize='SINGLE' group by basetime,regionid,stattype", start-(60*60*24*7))) {
            if ((UnixTime.getUnixTime()-start)>30) {
                log.fine("Stopping incomplete archival run due to runtime>30 seconds");
                return;
            }
            int regionid=r.getInt("regionid");
            int basetime=r.getInt("basetime");
            String stattype=r.getString("stattype");
            float min=r.getFloat("newmin");
            float max=r.getFloat("newmax");
            float avg=r.getFloat("newavg");
            float sd=r.getFloat("newsd");
            basetime=basetime*60*60; // we divided above, to split into hourly blocks, but we need a full time reference
            basetime+=(30*60); // and push half an hour into the time period.
            d.d("insert into regionstats(regionid,timestamp,stattype,statmin,statmax,statavg,statsd,samplesize) values(?,?,?,?,?,?,?,?)",regionid,basetime,stattype,min,max,avg,sd,"HOURLY");
            log.finer("Rolling "+d.dqi(true,"select count(*) from regionstats where regionid=? and timestamp>=? and timestamp<? and stattype=? and samplesize='SINGLE'",regionid,basetime-(30*60),basetime+(30*60),stattype)+" records into one HOURLY record");
            d.d("delete from regionstats where regionid=? and timestamp>=? and timestamp<? and stattype=? and samplesize='SINGLE'",regionid,basetime-(30*60),basetime+(30*60),stattype);
            
        }
    }
}
