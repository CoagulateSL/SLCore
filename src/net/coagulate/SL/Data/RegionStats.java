package net.coagulate.SL.Data;

import net.coagulate.SL.SL;

/**
 *
 * @author Iain Price
 */
public class RegionStats extends Table {

    @Override
    public String getTableName() { return "regionstats"; }
    
    public static void log(int region,int timestamp,String statstype,float min,float max,float avg,float sd) {
        SL.getLogger("RegionStas").fine("Region "+region+" has timestamp "+timestamp+" with stats "+statstype+"/"+min+"/"+max+"/"+avg+"/"+sd);
        SL.getDB().d("insert into regionstats(regionid,timestamp,stattype,statmin,statmax,statavg,statsd) values(?,?,?,?,?,?,?)",region,timestamp,statstype,min,max,avg,sd);
    }
    public static void log(Regions region,int timestamp,String statstype,float min,float max,float avg,float sd) {
        log(region.getId(),timestamp,statstype,min,max,avg,sd);
    }
    
}
