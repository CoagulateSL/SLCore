package net.coagulate.SL.Data;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Tools.Cache;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.SL.Config;
import net.coagulate.SL.SL;
import net.coagulate.SL.SelfTest;

public class SystemManagement {
	private static boolean wasMasterNode;
	private static boolean ranSelfTest=false;
	
	public static int get(final DBConnection database,final String schemaName) {
		return database.dqiNotNull("select max(version) from schemaversions where name like ?",schemaName);
	}
	
	public static boolean primaryNode() {
		// default schema has this being empty :P
		final int rowCount=SL.getDB().dqiNotNull("select count(*) from masternode");
		if (rowCount==0) {
			SL.getDB().d("insert into masternode(name) values(?)",Config.getHostName());
			SL.log("Maintenance").config("Claimed the master node role as it was unset");
		}
		final String name=SL.getDB().dqs("select name from masternode");
		if (!Config.getHostName().equalsIgnoreCase(name)) {
			if (wasMasterNode) {
				SL.log("Maintenance").config("We are no longer the master node!");
				wasMasterNode=false;
				SL.log("Maintenance").config("Disabling caching due to demotion from primary/maintenance node");
				SystemManagement.restrictCaches();
			}
			return false;
		} // not the master node
		// if we are the master node, shall we update our last run so that things know things are working ... thing.
		if (!wasMasterNode) {
			SL.log("Maintenance").config("We are now the master node!");
			SL.log("Maintenance").config("Enabling caching due to transition to primary/maintenance node");
			SystemManagement.unrestrictCaches();
			wasMasterNode=true;
			if (Config.getDevelopment()&&Config.runSelfTests()&!ranSelfTest) {
				ranSelfTest=true;
				new SelfTest.SelfTestRunner().start();
			}
		}
		final int lastRun=SL.getDB().dqiNotNull("select lastrun from masternode");
		if (UnixTime.getUnixTime()>(lastRun+60)) {
			SL.getDB().d("update masternode set lastrun=?",UnixTime.getUnixTime());
		}
		return true;
	}
	
	/**
	 * Limit caching, e.g. during server transition
	 */
	public static void restrictCaches() {
		Cache.disableCache();
		SL.log("SysOps").config("Caching has been set to RESTRICTED mode");
	}
	
	/**
	 * Restore normal caching operations
	 */
	public static void unrestrictCaches() {
		Cache.enableCache();
		SL.log("SysOps").config("Caches have been dropped");
		SL.log("SysOps").config("Normal caching behaviour has been resumed");
		SL.log("SysOps").config("Preloading caches");
		SL.preLoadCaches();
		SL.log("SysOps").config("Cache prepopulation is complete");
	}
	
	/**
	 * @return The current status of all caching modules.
	 */
	public static String cacheStatus() {
		return "Cache module eager flushing is "+Cache.eagerCacheFlush+".";
	}
}

