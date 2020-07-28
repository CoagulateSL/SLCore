package net.coagulate.SL;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Tools.UnixTime;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.logging.Logger;

public abstract class SLModule {

    private final Map<String,Integer> nextruns=new HashMap<>();
    private final Logger logger;

    protected final boolean nextRun(String name,int interval) {
        if (!nextruns.containsKey(name)) { nextruns.put(name,UnixTime.getUnixTime()); } // why am i doing unixtime stuff here...
        if (UnixTime.getUnixTime()<nextruns.get(name)) { return false; }
        nextruns.put(name,nextruns.get(name)+interval);
        return true;
    }
    public SLModule() {logger=SL.log(getClass().getSimpleName());}

    @Nonnull public abstract String getName();
    @Nonnull public abstract String getDescription();
    public abstract void shutdown();
    public abstract void startup();
    public abstract void initialise();
    public abstract void maintenance();
    public abstract int majorVersion();
    public abstract int minorVersion();
    public abstract int bugFixVersion();
    public abstract String commitId();

    // this is a lame mechanism.  It allows a module to be invoked even if it might not be present
    // becakse weakInvoke is part of the CoagulateSL module everything knows about this
    // currently only used to send IM's and group invites via JSLBotBridge
    // the idea is that JSLBot and the bridge are optional modules, and if this doesn't exist then
    // the relevant functionality will be unavailable (primarily SSO login via URL being IMed to avatar)
    // Rather than require the JSLBotBridge and thus JSLBot to be available at compile time we can
    // dynamically look up the module, error if it doesn't exist, and call weakInvoke with some very generic arguments
    // This shouldn't be used for many things otherwise it needs a strong redesign...  but for 2-3 bot related method calls
    // it's a bodge/hack.
    public Object weakInvoke(String command,Object... arguments){return null;}

    public final String getVersion() { return majorVersion()+"."+ minorVersion()+"."+ bugFixVersion(); }

    public void schemaCheck(DBConnection db, String schemaname, int requiredversion) {
        try {
            int currentversion = getSchemaVersion(db,schemaname);
            while (currentversion != requiredversion) {
                logger.config("Schema " + currentversion + " is not of required version " + requiredversion + ", calling schemaUpgrade");
                int newversion = schemaUpgrade(db,schemaname, currentversion);
                if (newversion == currentversion) {
                    throw new SystemImplementationException("Schema upgrade failed ; requested upgrade from " + currentversion + " and remained at " + newversion + ", the target is version " + requiredversion);
                }
                logger.config("Upgraded schema from " + currentversion + " to " + newversion + " (target is " + requiredversion + ")");
                db.d("update schemaversions set version=? where name like ?", newversion, schemaname);
                currentversion = newversion;
            }
            logger.config("DB Schema '" + schemaname + "' is at required version " + currentversion);
        } catch (Throwable t) {
            logger.config("Schema upgrade FAILED.  Terminating as a safety measure.  It is advised you keep these logs to diagnose and resolve the failed upgrade.  You will probably need to manually resolve this condition.");
            if (Error.class.isAssignableFrom(t.getClass())) { throw t; }
            System.exit(1);
        }
    }

    /** Issued when a schema upgrade is required.
     * The database version of the schema is not the same as the requirement in the software.
     * This method should update the 'current version' and return the new version of the schema.  Caller will update database.
     * @param schemaname Name of schema to update
     * @param currentversion Current version to update from
     * @return New version of schema
     */
    protected abstract int schemaUpgrade(DBConnection db,String schemaname, int currentversion);

    public int getSchemaVersion(DBConnection db,String schemaname) {
        return db.dqinn("select max(version) from schemaversions where name like ?",schemaname);
    }
}
