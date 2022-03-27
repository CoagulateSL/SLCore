package net.coagulate.SL;

import net.coagulate.Core.BuildInfo.SLCoreBuildInfo;
import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.SL.Data.EventQueue;
import net.coagulate.SL.Data.SystemManagement;
import net.coagulate.SL.HTML.ServiceTile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public abstract class SLModule {

    private final Map<String,Integer> nextRuns =new HashMap<>();
    private final Logger logger;

    protected final boolean nextRun(String name,int interval,int variance) {
        if (!nextRuns.containsKey(name)) { nextRuns.put(name,UnixTime.getUnixTime()); } // why am i doing unix time stuff here...
        if (UnixTime.getUnixTime()< nextRuns.get(name)) { return false; }
        int nextRun= nextRuns.get(name)+interval;
        if (variance>0) { nextRun+=ThreadLocalRandom.current().nextInt(variance*2)-variance; }
        nextRuns.put(name,nextRun);
        return true;
    }
    public SLModule() {logger=SL.log(getClass().getSimpleName());}

    @Nullable
    public abstract Map<ServiceTile,Integer> getServices();

    @Nonnull public abstract String getName();
    @Nonnull public abstract String getDescription();
    public abstract void shutdown();
    public abstract void startup();
    public abstract void initialise();
    public abstract void maintenance(); // called only if we're the master node
    public abstract void maintenanceInternal(); // called regardless
    public abstract String commitId();
    public String getBuildDateString() { return convertDate(getBuildDate()); }
    public abstract Date getBuildDate();
    public static String convertDate(Date date) { return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date); }

    // this is a lame mechanism.  It allows a module to be invoked even if it might not be present
    // because weakInvoke is part of the CoagulateSL module everything knows about this
    // currently only used to send IMs and group invites via JSLBotBridge
    // the idea is that JSLBot and the bridge are optional modules, and if this doesn't exist then
    // the relevant functionality will be unavailable (primarily SSO login via URL being IMed to avatar)
    // Rather than require the JSLBotBridge and thus JSLBot to be available at compile time we can
    // dynamically look up the module, error if it doesn't exist, and call weakInvoke with some very generic arguments
    // This shouldn't be used for many things otherwise it needs a strong redesign...  but for 2-3 bot related method calls
    // it's a bodge/hack.
    public Object weakInvoke(String command,Object... arguments){return null;}

    public void schemaCheck(DBConnection db, String schemaName, int requiredVersion) {
        try {
            int currentVersion = getSchemaVersion(db,schemaName);
            if (currentVersion>requiredVersion) {
                // if the schema is "too new"
                if (Config.getDevelopment()) {
                    logger.warning("Schema for " + schemaName + " is too new " + currentVersion + " > " + requiredVersion + " (current>required), continuing as in DEVELOPMENT mode.  No schema upgrade is executed.");
                    return;
                } else {
                    logger.config("Schema for " + schemaName + " is too new " + currentVersion + " > " + requiredVersion + " (current>required)");
                    throw new SystemImplementationException("Schema version too new on "+schemaName+" - current #"+currentVersion+" > required #"+requiredVersion+", terminating as a safety measure");
                }
            }
            while (currentVersion != requiredVersion) {
                logger.config("Schema " + currentVersion + " is not of required version " + requiredVersion + ", calling schemaUpgrade");
                int newVersion = schemaUpgrade(db,schemaName, currentVersion);
                if (newVersion == currentVersion) {
                    throw new SystemImplementationException("Schema upgrade failed ; requested upgrade from " + currentVersion + " and remained at " + newVersion + ", the target is version " + requiredVersion);
                }
                logger.config("Upgraded schema from " + currentVersion + " to " + newVersion + " (target is " + requiredVersion + ")");
                db.d("update schemaversions set version=? where name like ?", newVersion, schemaName);
                currentVersion = newVersion;
            }
            logger.config("DB Schema '" + schemaName + "' is at required version " + currentVersion);
        } catch (Throwable t) {
            t.printStackTrace();
            logger.config("Schema upgrade FAILED for "+schemaName+" version "+requiredVersion+".  Terminating as a safety measure.  It is advised you keep these logs to diagnose and resolve the failed upgrade.  You will probably need to manually resolve this condition.");
            if (Error.class.isAssignableFrom(t.getClass())) { throw t; }
            System.exit(1);
        }
    }

    /** Issued when a schema upgrade is required.
     * The database version of the schema is not the same as the requirement in the software.
     * This method should update the 'current version' and return the new version of the schema.  Caller will update database.
     * @param schemaName Name of schema to update
     * @param currentVersion Current version to update from
     * @return New version of schema
     */
    protected abstract int schemaUpgrade(DBConnection db,String schemaName, int currentVersion);

    public int getSchemaVersion(DBConnection db,String schemaName) {
        try { return SystemManagement.get(db,schemaName); }
        catch (Throwable t) {
            System.err.println("Exception thrown during Schema Version query on schema "+schemaName);
            throw t;
        }
    }

    /** Called when this node is becomming the primary node */
    public void promote() {}
    /** Called to demote this node from being a primary node */
    public void demote() {}
    /** Called to process an EventQueue object */
    public void processEvent(EventQueue event) {}
}
