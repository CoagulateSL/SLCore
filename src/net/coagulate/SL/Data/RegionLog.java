package net.coagulate.SL.Data;

import net.coagulate.SL.SL;

/**
 *
 * @author Iain Price
 */
public class RegionLog extends Table {

    @Override
    public String getTableName() { return "regionlog"; }
    
    public static void log(int region,String status,int from,int until) {
        SL.getDB().d("insert int regionlog(regionid,status,from,until) values(?,?,?,?)",region,status,from,until);
    }
    
}
