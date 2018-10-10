package net.coagulate.SL;

import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;
import net.coagulate.Core.ClassTools;
import net.coagulate.Core.Database.DB;
import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.MariaDBConnection;
import net.coagulate.Core.LogHandler;
import net.coagulate.Core.SystemException;
import net.coagulate.JSLBot.JSLBot;
import net.coagulate.JSLBot.LLCATruster;

/** Bootstrap class.
 *
 * @author Iain Price
 */
public class SL extends Thread {
    private static Logger log;
    public static final Logger getLogger(String subspace) { return Logger.getLogger(log.getName()+"."+subspace); }
    public static final Logger getLogger() { return log; }
    
    public static JSLBot bot;
    private static boolean shutdown=false;
    private static boolean errored=false;
    private static DBConnection db;
    
    public static void shutdown() { shutdown=true; }
    
    public static void main(String[] args) {
        try { startup(); }
        catch (SystemException e) { log.log(SEVERE,"Startup failed: "+e.getLocalizedMessage(),e); shutdown=true; }
        Runtime.getRuntime().addShutdownHook(new SL());
        while (!shutdown) { watchdog(); }
        _shutdown();
    }

    private static void startup() {
        loggingInitialise();
        log.config("SL Services starting up on "+Config.getNodeName()+" (#"+Config.getNode()+")");
        ClassTools.getClasses();
        Pricing.initialise();
        LLCATruster.doNotUse();
        CATruster.initialise();
        db=new MariaDBConnection("SL",Config.getJdbc());
        IPC.test();
        startBot(); 
        HTTPSListener.initialise();
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
        try { bot.waitConnection(30000); }  catch (IllegalStateException e) {}
        if (!bot.connected()) { bot.shutdown("Failed to connect"); shutdown=true; errored=true; throw new SystemException("Unable to connect to Second Life"); }
        getLogger().config("Primary Second Life automated agent has started");
    }
    
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
        net.coagulate.SL.HTTPPipelines.State.cleanup();
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
