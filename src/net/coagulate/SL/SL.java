package net.coagulate.SL;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;
import net.coagulate.Core.Database.DB;
import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.LockException;
import net.coagulate.Core.Database.MariaDBConnection;
import net.coagulate.Core.HTTP.HTTPListener;
import net.coagulate.Core.Tools.ByteTools;
import net.coagulate.Core.Tools.ClassTools;
import net.coagulate.Core.Tools.LogHandler;
import net.coagulate.Core.Tools.MailTools;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Maintenance;
import static net.coagulate.GPHUD.Maintenance.UPDATEINTERVAL;
import static net.coagulate.GPHUD.Maintenance.cycle;
import net.coagulate.GPHUD.Modules.Experience.VisitXP;
import net.coagulate.JSLBot.JSLBot;
import net.coagulate.JSLBot.LLCATruster;
import net.coagulate.LSLR.LSLR;
import static net.coagulate.SL.Config.LOCK_NUMBER_GPHUD_MAINTENANCE;
import static net.coagulate.SL.Config.LOCK_NUMBER_REGIONSTATS_ARCHIVAL;
import net.coagulate.SL.Data.LockTest;
import net.coagulate.SL.Data.RegionStats;
import net.coagulate.SL.HTTPPipelines.PageMapper;

/** Bootstrap class.
 *
 * @author Iain Price
 */
public class SL extends Thread {
    public static boolean DEV=false;
    public static final String VERSION="v0.02.00";
    private static Logger log;
    public static final Logger getLogger(String subspace) { return Logger.getLogger(log.getName()+"."+subspace); }
    public static final Logger getLogger() { return log; }
    
    public static JSLBot bot;
    private static boolean shutdown=false;
    private static boolean errored=false;
    private static DBConnection db;
    private static HTTPListener listener;
    
    public static void shutdown() { shutdown=true; }
    
    public static void main(String[] args) {
        if (args.length>0 && args[0].equalsIgnoreCase("DEV")) { DEV=true; }
        try {
            try { startup(); }
            catch (Throwable e) { errored=true; log.log(SEVERE,"Startup failed: "+e.getLocalizedMessage(),e); shutdown=true; }
            Runtime.getRuntime().addShutdownHook(new SL());
            while (!shutdown) { watchdog(); }
        }
        catch (Throwable t) { System.out.println("Main loop crashed: "+t); t.printStackTrace(); }
        try { _shutdown(); }
        catch (Throwable t) { System.out.println("Shutdown crashed: "+t); t.printStackTrace(); }
        System.exit(0);
    }

    private static void startup() {
        configureMailTarget();
        loggingInitialise();
        if (!DEV) {
            log.config("SL Services starting up on "+Config.getNodeName()+" (#"+Config.getNode()+")");
        } else {
            log.config("SL DEVELOPMENT Services starting up on "+Config.getNodeName()+" (#"+Config.getNode()+")");
        }
        LLCATruster.doNotUse(); // as in we use our own truster later on
        ClassTools.getClasses();
        db=new MariaDBConnection("SL"+(DEV?"DEV":""),Config.getJdbc());
        CATruster.initialise();
        startBot(); 
        Pricing.initialise();
        IPC.test();
        startGPHUD();
        if (!DEV) { startLSLR(); }
        waitBot();
        listener=new HTTPListener(Config.getPort(),Config.getKeyMaterialFile(),new PageMapper());
        log.info("=====[ Coagulate "+(DEV?"DEVELOPMENT ":"")+"Second Life Services {JavaCore, JSLBot, GPHUD, LSLR} version "+VERSION+", startup is fully complete ]=====");
    }

    private static void _shutdown() {
        log.config("SL Services shutting down");
        if (listener!=null) { listener.blockingShutdown(); }
        if (bot!=null) { bot.shutdown("SL System is shutting down"); }
        DB.shutdown();
        log.config("SL Services shutdown is complete, exiting.");
        if (errored) { System.exit(1); }
        System.exit(0);
    }
    
    private static void startBot() {
        bot=new JSLBot(Config.getBotConfig());
        bot.registershutdownhook=false;
        bot.start();
    }
    private static void startLSLR() {
        if (!DEV) { 
            log.config("Starting LSLR submodule for Quiet Life Rentals services");
            try { LSLR.initialise(); }
            catch (SQLException e) { throw new SystemException("LSLR startup failed",e); }
            log.config("Started LSLR submodule");
        }
    }
    private static void waitBot() {
        try { bot.waitConnection(30000); }  catch (IllegalStateException e) {}
        if (!bot.connected()) { bot.shutdown("Failed to connect"); shutdown=true; errored=true; throw new SystemException("Unable to connect to Second Life"); }
        getLogger().config("Primary Second Life automated agent has started");
    }
    
    public static void startGPHUD() {
        GPHUD.initialiseAsModule(SL.DEV,Config.getGPHUDJdbc(),Config.getHostName(),Config.getNode()+1);
        // make sure the lock is ok
        new LockTest(LOCK_NUMBER_GPHUD_MAINTENANCE);
    }
    
    private static int watchdogcycle=0;
    private static long laststats=new Date().getTime();
    private static long nextarchival=new Date().getTime()+((int)((Math.random()*60.0*45.0*1000.0)));
    public static void watchdog() {
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        if (shutdown) return;
        if (!DB.test()) {
            log.log(SEVERE,"Database failed connectivity test, shutting down."); shutdown=true; errored=true; return;
        }
        if (!bot.connected()) {
            log.log(SEVERE,"Main bot has become disconnected"); shutdown=true; errored=true; return;
        }
        // hmm //if (!listener.isAlive()) { log.log(SEVERE,"Primary listener thread is not alive"); shutdown=true; errored=true; return; }
        watchdogcycle++;
        if ((watchdogcycle % 10)==0) { net.coagulate.SL.HTTPPipelines.State.cleanup(); }

        if (((watchdogcycle+gphudoffset) % 60)==0) { gphudMaintenance(); }
        
        if ((laststats+60000)<new Date().getTime()) {
            dbStats();
            laststats=new Date().getTime();
        }
        if (nextarchival<new Date().getTime()) {
            regionStatsArchival();
            nextarchival+=(1000*60*60);
        }
    }
    private static int gphudoffset=0;
    private static void gphudMaintenance() {
        try { Maintenance.refreshCharacterURLs(); }
        catch (Exception e) { GPHUD.getLogger().log(SEVERE,"Maintenance refresh character URLs caught an exception",e); }
        try { Maintenance.refreshRegionURLs(); }
        catch (Exception e) { GPHUD.getLogger().log(SEVERE,"Maintenance refresh region URLs caught an exception",e); }

        // this stuff all must run 'exclusively' across the cluster...
        LockTest lock=new LockTest(LOCK_NUMBER_GPHUD_MAINTENANCE);
        int lockserial;
        try { lockserial=lock.lock(60); }
        catch (LockException e) { gphudoffset=gphudoffset+((int)Math.random()*10); GPHUD.getLogger().finer("Maintenance didn't aquire lock: "+e.getLocalizedMessage()); return; } // maintenance session already locked            
            
        try { Maintenance.startEvents(); }
        catch (Exception e) { GPHUD.getLogger().log(SEVERE,"Maintenance start events caught an exception",e); }            
        
        lock.extendLock(lockserial, 60);
        try { Maintenance.purgeOldCookies(); }
        catch (Exception e) { GPHUD.getLogger().log(SEVERE,"Maintenance run purge cookies caught an exception",e); }
        
        lock.extendLock(lockserial, 60);
        try { new VisitXP(-1).runAwards(); }
        catch (Exception e) { GPHUD.getLogger().log(SEVERE,"Maintenance run awards run caught an exception",e); }
        
        lock.extendLock(lockserial, 60);
        try { if ((cycle % UPDATEINTERVAL)==0) { Maintenance.updateInstances(); } }
        catch (Exception e) { GPHUD.getLogger().log(SEVERE,"Maintenance update Instances caught an exception",e); }
        
        lock.unlock(lockserial);
    }
    
    private static void dbStats() {
        if (DEV) {return;} // only log for production.
        int queries=0; int updates=0;
        long querytime=0; long updatetime=0;
        long querymax=0; long updatemax=0;
        for (DBConnection db:DB.get()) {
            DBConnection.DBStats stats = db.getStats();
            queries=queries+stats.queries;
            updates=updates+stats.updates;
            querytime=querytime+stats.querytotal;
            updatetime=updatetime+stats.updatetotal;
            if (stats.querymax>querymax) { querymax=stats.querymax; }
            if (stats.updatemax>updatemax) { updatemax=stats.updatemax; }
        }
        float queryavg=0;
        float updateavg=0;
        if (queries>0) { queryavg=querytime/queries; }
        if (updates>0) { updateavg=updatetime/updates; }
        //getLogger().fine("Stats: "+queries+"q, avg "+queryavg+" worst "+querymax+".  "+updates+"u, avg "+updateavg+" worst "+updatemax);
        String results="";
        results+=Config.getHostName().toLowerCase()+" mariadb.cluster.queries "+queries+"\n";
        results+=Config.getHostName().toLowerCase()+" mariadb.cluster.queryavg "+queryavg+"\n";
        results+=Config.getHostName().toLowerCase()+" mariadb.cluster.querymax "+querymax+"\n";
        results+=Config.getHostName().toLowerCase()+" mariadb.cluster.updates "+updates+"\n";
        results+=Config.getHostName().toLowerCase()+" mariadb.cluster.updateavg "+updateavg+"\n";
        results+=Config.getHostName().toLowerCase()+" mariadb.cluster.updatemax "+updatemax+"\n";
        try {
            Process zabbix=Runtime.getRuntime().exec(new String[]{"/usr/bin/zabbix_sender","-z","10.0.0.1","-i-"});
            zabbix.getOutputStream().write(results.getBytes(StandardCharsets.UTF_8));
            zabbix.getOutputStream().close();
            String output=ByteTools.convertStreamToString(zabbix.getInputStream());
            String error=ByteTools.convertStreamToString(zabbix.getErrorStream());
            //getLogger().fine("Zabbix output:"+output);
            //getLogger().fine("Zabbix stderr:"+error);
            zabbix.getErrorStream().close();
            zabbix.getOutputStream().close();
        } catch (IOException e) {
            getLogger().log(Level.WARNING,"Error while passing stats to zabbix",e);
        }
    }
    
    private static void loggingInitialise() {
        LogHandler.mailprefix="EXCEPTION: "+(DEV?"Dev/":"PRODUCTION/")+Config.getHostName();
        LogHandler.initialise();
        log=Logger.getLogger("net.coagulate.SL");        
    }
    
    private static void configureMailTarget() {
        MailTools.defaultfromaddress="sl-cluster-alerts@predestined.net";
        MailTools.defaulttoaddress=MailTools.defaultfromaddress;
        MailTools.defaulttoname="SL Developers";
        MailTools.defaultfromname=(DEV?"Dev ":"")+Config.getHostName();
        MailTools.defaultserver="127.0.0.1";
    }
    
    public static void regionStatsArchival() {
        LockTest lock=new LockTest(LOCK_NUMBER_REGIONSTATS_ARCHIVAL);
        int serial=0;
        try { serial=lock.lock(300); }
        catch (LockException e) { log.fine("Failed to get lock to run region stats archiver."); return; }
        long start=new Date().getTime();
        RegionStats.archiveOld();       
        long end=new Date().getTime();
        log.fine("Region stats archiver ran for "+((float)(end-start))/1000.0+" ms");
        lock.unlock(serial);
    }

    public static DBConnection getDB() { return db; }

    private SL() {}
    @Override
    public void run() { if (!SL.shutdown) { log.severe("JVM Shutdown Hook invoked"); } SL.shutdown=true; }
    
}
