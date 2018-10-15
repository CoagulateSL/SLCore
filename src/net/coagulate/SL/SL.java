package net.coagulate.SL;

import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;
import net.coagulate.Core.Database.DB;
import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.LockException;
import net.coagulate.Core.Database.MariaDBConnection;
import net.coagulate.Core.Tools.ClassTools;
import net.coagulate.Core.Tools.LogHandler;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Maintenance;
import static net.coagulate.GPHUD.Maintenance.UPDATEINTERVAL;
import static net.coagulate.GPHUD.Maintenance.cycle;
import net.coagulate.GPHUD.Modules.Experience.VisitXP;
import net.coagulate.JSLBot.JSLBot;
import net.coagulate.JSLBot.LLCATruster;
import static net.coagulate.SL.Config.LOCK_NUMBER_GPHUD_MAINTENANCE;
import net.coagulate.SL.Data.LockTest;

/** Bootstrap class.
 *
 * @author Iain Price
 */
public class SL extends Thread {
    public static final String VERSION="v0.00.00";
    private static Logger log;
    public static final Logger getLogger(String subspace) { return Logger.getLogger(log.getName()+"."+subspace); }
    public static final Logger getLogger() { return log; }
    
    public static JSLBot bot;
    private static boolean shutdown=false;
    private static boolean errored=false;
    private static DBConnection db;
    
    public static void shutdown() { shutdown=true; }
    
    public static void main(String[] args) {
        try {
            try { startup(); }
            catch (Throwable e) { errored=true; log.log(SEVERE,"Startup failed: "+e.getLocalizedMessage(),e); shutdown=true; }
            Runtime.getRuntime().addShutdownHook(new SL());
            while (!shutdown) { watchdog(); }
        }
        catch (Throwable t) { System.out.println("Main loop crashed: "+t); }
        try { _shutdown(); }
        catch (Throwable t) { System.out.println("Shutdown crashed: "+t); }
    }

    private static void startup() {
        loggingInitialise();
        log.config("SL Services starting up on "+Config.getNodeName()+" (#"+Config.getNode()+")");
        LLCATruster.doNotUse();
        ClassTools.getClasses();
        db=new MariaDBConnection("SL",Config.getJdbc());
        CATruster.initialise();
        startBot(); 
        Pricing.initialise();
        IPC.test();
        startGPHUD();
        waitBot();
        HTTPSListener.initialise();
        log.info("=====[ Coagulate Second Life Services {JavaCore, JSLBot, GPHUD} version "+VERSION+", startup is fully complete ]=====");
    }

    private static void _shutdown() {
        log.config("SL Services shutting down");
        HTTPSListener.blockingShutdown();
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
    private static void waitBot() {
        try { bot.waitConnection(30000); }  catch (IllegalStateException e) {}
        if (!bot.connected()) { bot.shutdown("Failed to connect"); shutdown=true; errored=true; throw new SystemException("Unable to connect to Second Life"); }
        getLogger().config("Primary Second Life automated agent has started");
    }
    
    public static void startGPHUD() {
        GPHUD.initialiseAsModule(false,Config.getGPHUDJdbc());
        // make sure the lock is ok
        new LockTest(LOCK_NUMBER_GPHUD_MAINTENANCE);
    }
    
    private static int watchdogcycle=0;
    public static void watchdog() {
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        if (shutdown) return;
        if (!DB.test()) {
            log.log(SEVERE,"Database failed connectivity test, shutting down."); shutdown=true; errored=true;
        }
        if (shutdown) return;
        if (!bot.connected()) {
            log.log(SEVERE,"Main bot has become disconnected"); shutdown=true; errored=true;
        }
        watchdogcycle++;
        if ((watchdogcycle % 10)==0) { net.coagulate.SL.HTTPPipelines.State.cleanup(); }

        if ((watchdogcycle % 60)==0) { gphudMaintenance(); }
    }
    
    private static void gphudMaintenance() {
        try { Maintenance.refreshCharacterURLs(); }
        catch (Exception e) { GPHUD.getLogger().log(SEVERE,"Maintenance refresh character URLs caught an exception",e); }
        try { Maintenance.refreshRegionURLs(); }
        catch (Exception e) { GPHUD.getLogger().log(SEVERE,"Maintenance refresh region URLs caught an exception",e); }

        // this stuff all must run 'exclusively' across the cluster...
        LockTest lock=new LockTest(LOCK_NUMBER_GPHUD_MAINTENANCE);
        int lockserial;
        try { lockserial=lock.lock(60); }
        catch (LockException e) { GPHUD.getLogger().finer("Maintenance didn't aquire lock: "+e.getLocalizedMessage()); return; } // maintenance session already locked            
            
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
    
    private static void loggingInitialise() {
        LogHandler.initialise();
        log=Logger.getLogger("net.coagulate.SL");        
    }

    public static DBConnection getDB() { return db; }

    private SL() {}
    @Override
    public void run() { if (!SL.shutdown) { log.severe("JVM Shutdown Hook invoked"); } SL.shutdown=true; }
    
}
