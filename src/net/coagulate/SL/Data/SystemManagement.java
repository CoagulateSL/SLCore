package net.coagulate.SL.Data;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.Cache;
import net.coagulate.SL.Config;
import net.coagulate.SL.SL;
import net.coagulate.SL.SelfTest;

public class SystemManagement {
	private static boolean wasMasterNode;
	private static boolean ranSelfTest=false;
	
	public static int get(final DBConnection database,final String schemaName) {
		return database.dqiNotNull("select max(version) from schemaversions where name like ?",schemaName);
	}
	
	/**
	 * Describes if we are the PRIMARY MAINTENANCE NODE.
	 * Also inherited the responsibility of starting or stopping cache.
	 * This function is called every maint loop ; approx once a second.
	 *
	 * @return True if we should action the maintenance tasks.
	 */
	public static boolean primaryNode() {
		// if clustering is disabled we are always the primary and only node, assuming our sysadmin set it up properly.
		if (!Config.cluster()) {
			if (!ranSelfTest&&Config.getDevelopment()&&Config.runSelfTests()) {
				ranSelfTest=true;
				new SelfTest.SelfTestRunner().start();
			}
			return true;
		}
		// on the off chance this table is empty
		final int rowCount=SL.getDB().dqiNotNull("select count(*) from cluster");
		if (rowCount==0) {
			SL.getDB()
			  .d("INSERT INTO `cluster`(`maintnode`,`mainttransfer`,`cachenode`) values(?,?,?)",
			     Config.getHostName(),
			     null,
			     Config.getHostName());
			SL.log("Maintenance").config("Claimed the maint node role and cache server as it was unset");
		}
		// consider starting or stopping the cache
		final ResultsRow clusterinfo=SL.getDB().dq("select *  from cluster limit 0,1").first();
		final boolean shouldWeCache=Config.getHostName().equalsIgnoreCase(clusterinfo.getString("cachenode"));
		if (shouldWeCache!=cacheEnabled()) {
			// need to start or stop cache
			if (shouldWeCache) {
				SL.log("Maintenance").config("Enabling caching due to cluster config change");
				SystemManagement.unrestrictCaches();
				if (Config.getDevelopment()&&Config.runSelfTests()&&!ranSelfTest) {
					ranSelfTest=true;
					new SelfTest.SelfTestRunner().start();
				}
			} else {
				SL.log("Maintenance").config("Disabling caching due to cluster config change");
				SystemManagement.restrictCaches();
			}
		}
		// caching status updated, what about the maintenance thread, are we elected?
		if (!Config.getHostName().equalsIgnoreCase(clusterinfo.getString("maintnode"))) {
			// we are not the current maint node
			if (wasMasterNode) {
				SL.log("Maintenance").config("Stopped running the maintenance task");
				wasMasterNode=false;
			}
			return false;
		}
		// are we transfering?
		final String transferto=clusterinfo.getStringNullable("mainttransfer");
		if (transferto!=null) {
			// okay then
			SL.log("Maintenance")
			  .config("Transfering maintenance runner role to "+transferto+" due to cluster config change");
			SL.getDB().d("UPDATE `cluster` SET `maintnode`=?, `mainttransfer`=?",transferto,null);
			if (wasMasterNode) {
				SL.log("Maintenance").config("Stopped running the maintenance task");
				wasMasterNode=false;
			}
			return false;
		}
		if (!wasMasterNode) {
			SL.log("Maintenance").config("Started running the maintenance task");
			wasMasterNode=true;
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
	
	/**
	 * Returns the status of wether we're caching or not.
	 *
	 * @return The current cache enablement status
	 */
	public static boolean cacheEnabled() {
		return Cache.cacheEnabled();
	}
}

