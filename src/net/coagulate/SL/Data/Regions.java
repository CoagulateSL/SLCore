package net.coagulate.SL.Data;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.SL.SL;

/**
 *
 * @author Iain Price
 */
public class Regions extends LockableTable {

    public Regions(int id) { super(id); }

    @Override
    public String getTableName() { return "regions"; }

    public static Regions getByName(String name) {
        Integer id=SL.getDB().dqi(false,"select id from regions where region like ?",name);
        if (id!=null) { return new Regions(id); }
        SL.getDB().d("insert into regions(region) values(?)",name);
        id=SL.getDB().dqi(false,"select id from regions where region like ?",name);
        if (id!=null) { return new Regions(id); }
        throw new SystemException("Failed to find inserted region in regions table");
    }

    public String getStatus() { return getString("status"); }
    
    public int getLastUpdate() {
        Integer lu=getInt("lastupdate");
        if (lu==null) { return 0; }
        return lu;
    }
}
