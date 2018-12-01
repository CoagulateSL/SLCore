package net.coagulate.SL.Data;

import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.SL.SL;

/**
 *
 * @author Iain Price
 */
public class RegionLog extends Table {

    public static Results getLast(Regions region) {
        return SL.getDB().dq("select tds,changetype,oldvalue,newvalue from regionlog where regionid=?",region.getId());
    }

    @Override
    public String getTableName() { return "regionlog"; }
    
    public static void log(int region,String changetype,String oldvalue,String newvalue,int tds) {
        SL.getDB().d("insert into regionlog(regionid,changetype,oldvalue,newvalue,tds) values(?,?,?,?,?)",region,changetype,oldvalue,newvalue,tds);
    }
    public static void log(Regions region,String changetype,String oldvalue,String newvalue,int tds) {
        log(region.getId(),changetype,oldvalue,newvalue,tds);
    }

    public static void log(int region,String changetype,String oldvalue,String newvalue) {
        log(region,changetype,oldvalue,newvalue,UnixTime.getUnixTime());
    }
    public static void log(Regions region,String changetype,String oldvalue,String newvalue) {
        log(region.getId(),changetype,oldvalue,newvalue,UnixTime.getUnixTime());
    }
    
}
