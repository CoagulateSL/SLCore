package net.coagulate.SL;

import net.coagulate.Core.Database.DB;
import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.MariaDBConnection;
import net.coagulate.Core.Exceptions.System.SystemBadValueException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.System.SystemInitialisationException;
import net.coagulate.Core.Exceptions.System.SystemLookupFailureException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.HTTP.HTTPListener;
import net.coagulate.Core.Tools.*;
import net.coagulate.SL.HTTPPipelines.PageMapper;
import org.apache.http.Header;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;

/**
 * Bootstrap class.
 *
 * @author Iain Price
 */
public class SL extends Thread {
    public static final String VERSION = "v0.03.00";
    private static final long laststats = new Date().getTime();
    private static final long nextarchival = new Date().getTime() + ((int) ((Math.random() * 60.0 * 45.0 * 1000.0)));
    public static boolean DEV;
    @Nullable
    private static Logger log;
    private static boolean shutdown;
    private static boolean errored;
    @Nullable
    private static DBConnection db;
    @Nullable
    private static HTTPListener listener;
    private static int watchdogcycle;
    private static int gphudoffset;
    private static final Map<String,SLModule> modules=new TreeMap<>();

    private SL() {}

    @Nonnull
    public static Logger log(final String subspace) {
        return Logger.getLogger(log.getName()+"."+subspace);
    }

    public static void shutdown() {
        shutdown = true;
    }

    // Where it all begins
    public static void main(@Nonnull final String[] args) {
        if (args.length != 1) {
            System.err.println("Supply a configuration file as the only parameter");
            System.exit(1);
        }
        Config.load(args[0]);

        DEV = Config.getDevelopment();
        loggingInitialise();
        log.config("Logging services initialised");

        try {
            startup();
            Runtime.getRuntime().addShutdownHook(new SL());
            while (!shutdown) {
                try { Thread.sleep(1000); } catch (final InterruptedException ignored) {}
                if (!shutdown) {
                    try { runMaintenance(); } catch (final Throwable t) {
                        System.out.println("Uhoh, maintenance crashed, even though it's crash proofed (primaryNode()?)");
                        t.printStackTrace();
                    }
                }
            }
        } catch (@Nonnull final Throwable t) {
            System.out.println("Main loop crashed: " + t);
            t.printStackTrace();
        }
        try {
            _shutdown();
        } catch (@Nonnull final Throwable t) {
            System.out.println("Shutdown crashed: " + t);
            t.printStackTrace();
        }
        System.exit(0);
    }

    private static final Map<String,Integer> maintenancefails=new HashMap<>();
    private static void runMaintenance() {
        if (!primaryNode()) { return; }
        for (SLModule module:modules.values()) {
            if (!maintenancefails.containsKey(module.getName())) { maintenancefails.put(module.getName(),0); }
            int failcount=maintenancefails.get(module.getName());
            if (failcount<5) {
                try {
                    module.maintenance();
                } catch (Throwable t) {
                    SL.report("Maintenance Exceptioned in "+module.getName(),t,null);
                    failcount++;
                    maintenancefails.put(module.getName(), failcount);
                    if (failcount >= 5) {
                        SL.reportString("Maintenance DISABLED for " + module.getName(), null, "Module exceeded 5 fail counts");
                    }
                }
            }
        }
    }

    @Nonnull
    public static DBConnection getDB() {
        if (db == null) {
            throw new SystemInitialisationException("DB access before DB is initialised");
        }
        return db;
    }

    @Nonnull
    public static String getBannerURL() {
        return "/resources/banner-coagulate" + (DEV ? "-dev" : "") + ".png";
    }

    @Nonnull
    public static String getBannerHREF() {
        return "<img src=\"" + getBannerURL() + "\">";
    }

    public static void report(final String header,
                              @Nonnull final Throwable t,
                              @Nullable final DumpableState state) {
        reportString(header, t, (state != null ? state.toHTML() : "No state supplied"));
    }

    public static void reportString(final String header,
                                    @Nonnull final Throwable t,
                                    @Nullable final String additional) {
        String output = ExceptionTools.dumpException(t) + "<br><hr><br>";
        if (additional != null) {
            output += additional;
        }

        if (UserException.class.isAssignableFrom(t.getClass())) {
            if (((UserException) t).suppressed()) {
                return;
            }
        }
        if (SystemException.class.isAssignableFrom(t.getClass())) {
            if (((SystemException) t).suppressed()) {
                return;
            }
        }
        LogHandler.alreadyMailed(t);
        try {
            if (LogHandler.suppress(t)) {
                System.out.println("Exception Report Suppressed " + LogHandler.getCount(t) + "x" + LogHandler.getSignature(t));
            } else {
                MailTools.mail((DEV ? "Dev" : "PROD") + " EX : " + header + " - " + t.getLocalizedMessage(), output);
            }
        } catch (@Nonnull final MessagingException e) {
            log().log(SEVERE, "Exception mailing out about exception", e);
        }
    }

    @Nonnull
    public static Logger log() {
        if (log == null) {
            throw new SystemInitialisationException("Logger is null");
        }
        return log;
    }

    public static String textureURL(final String textureuuid) {
        return "https://picture-service.secondlife.com/" + textureuuid + "/320x240.jpg";
    }

    public static String getClientIP(final HttpRequest req,
                                     final HttpContext context) {

        final Header[] headers = req.getHeaders("X-Forwarded-For");
        try {
            @SuppressWarnings("deprecation") final HttpInetConnection connection = (HttpInetConnection) context.getAttribute(org.apache.http.protocol.ExecutionContext.HTTP_CONNECTION);
            final InetAddress ia = connection.getRemoteAddress();
            final StringBuilder ret = new StringBuilder();
            if (!ia.isLoopbackAddress()) {
                ret.append("[").append(ia).append("]");
            }
            for (final Header header : headers) {
                final String value = header.getValue();
                if (!(value.equals("127.0.0.1"))) {
                    if (ret.length() > 0) {
                        ret.append(", ");
                    }
                    ret.append(value);
                }
            }
            return ret.toString();
        } catch (@Nonnull final Exception e) {
            log().log(Level.WARNING, "Exception getting client address", e);
            return "UNKNOWN";
        }

    }

    private static boolean wasmasternode=false;
    public static boolean primaryNode() {
        final String name = getDB().dqs("select name from masternode");
        if (!Config.getHostName().equalsIgnoreCase(name)) {
            if (wasmasternode) {
                log("Maintenance").config("We are no longer the master node!");
                wasmasternode=false;
            }
            return false;
        } // not the master node
        // if we are the master node, shall we update our last run so that things know things are working ... thing.
        if (!wasmasternode) {
            log("Maintenance").config("We are now the master node!");
            wasmasternode=true;
        }
        final int lastrun = getDB().dqinn("select lastrun from masternode");
        if (UnixTime.getUnixTime() > (lastrun + 60)) {
            getDB().d("update masternode set lastrun=?", UnixTime.getUnixTime());
        }
        return true;
    }

    // ----- Internal Statics -----

    private static void _shutdown() {
        log().config("SL Services shutting down");
        for (SLModule module:modules.values()) {
            log().config("Shutting down module "+module.getName());
            module.shutdown();
        }
        if (listener != null) {
            listener.blockingShutdown();
        }
        DB.shutdown();
        log().config("SL Services shutdown is complete, exiting.");
        if (errored) {
            System.exit(1);
        }
        System.exit(0);
    }



    private static void loggingInitialise() {
        LogHandler.initialise();
        log = Logger.getLogger("net.coagulate.SL");
        LogHandler.mailprefix = "E:" + (DEV ? "(DEV) " : " ");
    }

    private static void configureMailTarget() {
        MailTools.defaultfromaddress = Config.getDeveloperEmail();
        MailTools.defaulttoaddress = Config.getDeveloperEmail();
        MailTools.defaulttoname = "SL Developers";
        MailTools.defaultfromname = (DEV ? "Dev " : "") + Config.getHostName();
        MailTools.defaultserver = Config.getMailServer();
    }

    private static void startup() {
        try {
            ClassTools.getClasses();
            log.config("Configuring default developer mail target");
            configureMailTarget(); // mails are gonna be messed up coming from logging init
            log.config("Scanning for modules");
            findModules();
            log.config("Initialising PageMapper");
            PageMapper.initialise();
            if (!DEV) {
                log().config("SL Services starting up on " + Config.getHostName());
            } else {
                log().config("SL DEVELOPMENT Services starting up on " + Config.getHostName());
            }
            db = new MariaDBConnection("SL" + (DEV ? "DEV" : ""), Config.getJdbc());
            for (SLModule module : modules.values()) {
                log().config("Initialising module - " + module.getName());
                module.initialise();
            }
            for (SLModule module : modules.values()) {
                log().config("Starting module - " + module.getName());
                module.startup();
            }
            // something about mails may break later on so we send a test mail here...
            MailTools.mail("CoagulateSL "+(DEV?"DEVELOPMENT ":"")+"startup on "+Config.getHostName()+" (v"+getStackVersion()+" "+getStackBuildDate()+")", "");
            // TODO Pricing.initialise();
            listener = new HTTPListener(Config.getPort(), PageMapper.getPageMapper());
            log().info("Startup complete.");
            log().info("========================================================================================================================");
            log().info(outerPad("=====[ Coagulate " + (DEV ? "DEVELOPMENT " : "") + "Second Life Services ]======"));
            log().info("========================================================================================================================");
            for (SLModule module : modules.values()) {
                log().info(spacePad(spacePrePad(module.getVersion())+" - "+module.getBuildDate()+" - " +module.getName() + " - "+ module.getDescription()));
            }
            log().info("------------------------------------------------------------------------------------------------------------------------");
            log().info(spacePad(spacePrePad(getStackVersion())+" - "+getStackBuildDate()+" - CoagulateSL Stack Version"));
            log().info("========================================================================================================================");
        }
        // print stack trace is discouraged, but the log handler may not be ready yet.
        catch (@Nonnull final Throwable e) {
            errored = true;
            e.printStackTrace();
            log().log(SEVERE, "Startup failed: " + e.getLocalizedMessage(), e);
            shutdown = true;
        }
    }

    private static String spacePad(String s) {
        while (s.length()<120) { s=s+" "; }
        return s;
    }
    private static String spacePrePad(String s) {
        while (s.length()<8) { s=" "+s; }
        return s;
    }

    private static String outerPad(String s) {
        while (s.length()<120) {
            s = "=" + s;
            if (s.length() == 120) {
                return s;
            }
            s = s + "=";
        }
        return s;
    }

    private static void findModules() {
        Set<Class<? extends SLModule>> modulelist = ClassTools.getSubclasses(SLModule.class);
        for (Class<? extends SLModule> module:modulelist) {
            if (modules.containsKey(module.getName())) { throw new SystemBadValueException("Conflict for module name "+module.getName()+" between "+module.getClass().getSimpleName()+" and "+modules.get(module.getName()).getClass().getSimpleName()); }
            try {
                SLModule instance=module.getDeclaredConstructor().newInstance();
                modules.put(instance.getName(),instance);
            } catch (InstantiationException|IllegalAccessException|InvocationTargetException|NoSuchMethodException e) {
                throw new SystemImplementationException("Error instantiating module "+module.getSimpleName()+" - "+e.getLocalizedMessage(),e);
            }
        }
    }

    public static void im(String uuid, String message) {
        weakInvoke("JSLBotBridge","im",uuid,message);
    }

    public static void groupInvite(String uuid, String groupuuid, String roleuuid) {
        weakInvoke("JSLBotBridge","groupinvite",uuid,groupuuid,roleuuid);
    }

    public static Collection<SLModule> modules() { return modules.values(); }

    @Override
    public void run() {
        if (!SL.shutdown) {
            log().severe("JVM Shutdown Hook invoked");
        }
        SL.shutdown = true;
    }

    public static void watchdog() {
        try {
            Thread.sleep(1000);
        } catch (@Nonnull final InterruptedException e) {
        }
        if (shutdown) {
            return;
        }
        if (!DB.test()) {
            log().log(SEVERE, "Database failed connectivity test, shutting down.");
            shutdown = true;
            errored = true;
        }
        // hmm //if (!listener.isAlive()) { log.log(SEVERE,"Primary listener thread is not alive"); shutdown=true; errored=true; return; }
    }

    public static boolean hasModule(String module) {
        if (modules.containsKey(module)) { return true; }
        return false;
    }
    @Nonnull
    public static SLModule getModule(String module) {
        if (!hasModule(module)) { throw new SystemLookupFailureException("There is no module called "+module); }
        return modules.get(module);
    }
    public static Object weakInvoke(String module,String command,Object... arguments) {
        return getModule(module).weakInvoke(command,arguments);
    }

    public static String getStackVersion() {
        int maj=0; int min=0; int bug=0;
        for (SLModule module:modules.values()) {
            maj+=module.getMajorVersion();
            min+=module.getMinorVersion();
            bug+=module.getBugfixversion();
        }
        return maj+"."+min+"."+bug;
    }

    public static String getStackBuildDate() {
        String bd="1";
        for (SLModule module:modules.values()) {
            if (module.getBuildDate().compareTo(bd)>0) { bd=module.getBuildDate(); }
        }
        return bd;
    }
}
