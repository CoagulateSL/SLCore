package net.coagulate.SL.Data;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.SL.Config;
import net.coagulate.SL.SL;

public class SystemManagement {
    private static boolean wasMasterNode =false;

    public static int get(DBConnection database, String schemaName) {
        return database.dqiNotNull("select max(version) from schemaversions where name like ?",schemaName);
    }

    public static boolean primaryNode() {
        // default schema has this being empty :P
        int rowCount= SL.getDB().dqiNotNull("select count(*) from masternode");
        if (rowCount==0) {
            SL.getDB().d("insert into masternode(name) values(?)", Config.getHostName());
            SL.log("Maintenance").config("Claimed the master node role as it was unset");
        }
        final String name = SL.getDB().dqs("select name from masternode");
        if (!Config.getHostName().equalsIgnoreCase(name)) {
            if (wasMasterNode) {
                SL.log("Maintenance").config("We are no longer the master node!");
                wasMasterNode =false;
            }
            return false;
        } // not the master node
        // if we are the master node, shall we update our last run so that things know things are working ... thing.
        if (!wasMasterNode) {
            SL.log("Maintenance").config("We are now the master node!");
            wasMasterNode =true;
        }
        final int lastRun = SL.getDB().dqiNotNull("select lastrun from masternode");
        if (UnixTime.getUnixTime() > (lastRun + 60)) {
            SL.getDB().d("update masternode set lastrun=?", UnixTime.getUnixTime());
        }
        return true;
    }
}

