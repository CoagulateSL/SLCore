package net.coagulate.SL.Data;

import net.coagulate.SL.SL;

/**
 *
 * @author Iain Price
 */
public class RegionLog extends Table {

    @Override
    public String getTableName() { return "regionlog"; }
    
    public static void log(int region,String changetype,String status,int from,int until) {
        SL.getDB().d("insert into regionlog(regionid,changetype,status,begin,end) values(?,?,?,?)",region,changetype,status,from,until);
    }
    
}
