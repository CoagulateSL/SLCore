package net.coagulate.SL.Data;

import net.coagulate.Core.Database.LockException;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UnixTime;
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
        Integer lu=getInt("lastperf");
        if (lu==null) { return 0; }
        return lu;
    }

    public void setLastUpdate() {
        try {
            set("lastupdate",UnixTime.getUnixTime());
        } catch (LockException e) {} // not that important here, something else is updating it...
    }

    public void setNewStatus(String status) {
        int time=UnixTime.getUnixTime();
        d("update regions set status=?, since=?, lastupdate=? where id=?",status,time,time,getId());
    }

    public int getSince() {
        Integer since=getInt("since");
        if (since==null) { return 0; }
        return since;
    }

    public String getName() {
        return getString("region");
    }
}
