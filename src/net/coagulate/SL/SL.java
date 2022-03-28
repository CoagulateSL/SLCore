package net.coagulate.SL;

import net.coagulate.Core.Database.DB;
import net.coagulate.Core.Database.DBConnection;

import net.coagulate.Core.Database.MySqlDBConnection;
import net.coagulate.Core.Exceptions.System.SystemBadValueException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.System.SystemInitialisationException;
import net.coagulate.Core.Exceptions.System.SystemLookupFailureException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.HTML.Container;
import net.coagulate.Core.HTML.Elements.Img;
import net.coagulate.Core.HTML.Elements.Paragraph;
import net.coagulate.Core.HTML.Elements.Preformatted;
import net.coagulate.Core.HTML.Elements.Table;
import net.coagulate.Core.HTTP.HTTPListener;
import net.coagulate.Core.HTTP.URLDistribution;
import net.coagulate.Core.Tools.*;
import net.coagulate.SL.Data.EventQueue;
import net.coagulate.SL.Data.SystemManagement;
import net.coagulate.SL.HTML.ServiceTile;
import org.apache.http.Header;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;

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
    @Nullable
    private static Logger log;
    private static boolean shutdown;
    private static boolean errored;
    @Nullable
    private static DBConnection db;
    @Nullable
    private static HTTPListener listener;
    private static final Map<String,SLModule> modules=new TreeMap<>();
    private static boolean imWarned =false;

    private SL() {}

    public static List<ServiceTile> getServiceTiles() {
        Map<ServiceTile,Integer> tiles=new HashMap<>();
        for (SLModule module:SL.modules()) {
            Map<ServiceTile, Integer> moduleServices = module.getServices();
            if (moduleServices!=null) { tiles.putAll(moduleServices); }
        }
        List<ServiceTile> services=new ArrayList<>();
        while (!tiles.isEmpty()) {
            int min=999999999;
            for (Map.Entry<ServiceTile,Integer> tile:tiles.entrySet()) {
                if (tile.getValue()<min) { min=tile.getValue(); }
            }
            Set<ServiceTile> addedList=new HashSet<>();
            for (Map.Entry<ServiceTile,Integer> tile:tiles.entrySet()) {
                if (tile.getValue()==min) {
                    services.add(tile.getKey());
                    addedList.add(tile.getKey());
                }
            }
            for (ServiceTile processed:addedList) { tiles.remove(processed); }
        }
        return services;
    }


    @Nonnull
    public static Logger log(final String subspace) {
        if (log==null) { throw new SystemInitialisationException("Logger is not initialised by the time it is used"); }
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

        loggingInitialise();
        log().config("Logging services initialised");

        try {
            startup();
            Runtime.getRuntime().addShutdownHook(new SL());
            new StackTraceProfiler().start();
            while (!shutdown) {
                try { //noinspection BusyWait
                    Thread.sleep(1000); } catch (final InterruptedException ignored) {}
                if (!shutdown) {
                    try { runMaintenance(); } catch (final Throwable t) {
                        System.out.println("Uh oh, maintenance crashed, even though it's crash proofed (primaryNode()?)");
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

    private static final Map<String,Integer> maintenanceFails =new HashMap<>();
    private static void runMaintenance() {
        boolean activeNode=SystemManagement.primaryNode();
        for (SLModule module:modules.values()) {
            if (!maintenanceFails.containsKey(module.getName())) { maintenanceFails.put(module.getName(),0); }
            int failCount= maintenanceFails.get(module.getName());
            if (failCount<5) {
                try {
                    module.maintenanceInternal();
                    if (activeNode) { module.maintenance(); }
                } catch (Throwable t) {
                    SL.report("Maintenance Exception in "+module.getName(),t,null);
                    failCount++;
                    maintenanceFails.put(module.getName(), failCount);
                    if (failCount >= 5) {
                        SL.reportString("Maintenance DISABLED for " + module.getName(),null,"Module exceeded 5 fail counts");
                    }
                }
            }
        }
        if (activeNode) {
            for (EventQueue event : EventQueue.getOutstandingEvents()) {
                if (SL.hasModule(event.getModuleName())) {
                    SL.getModule(event.getModuleName()).processEvent(event);
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
        if (Config.getWebLogo().isBlank()) { return "/resources/banner-coagulate" + (Config.getDevelopment() ? "-dev" : "") + ".png"; }
        return "/resources/"+Config.getWebLogo();
    }

    @Nonnull
    public static Container getBannerHREF() {
        return new Img(getBannerURL());
    }

    public static void report(final String header,
                              @Nullable final Throwable t,
                              @Nullable final DumpableState state) {

        log().warning(ExceptionTools.getPertinent(t));
        reportString(header, t, (state != null ? state.toHTML() : "No state supplied"));
    }

    public static void reportString(final String header,
                                    @Nullable final Throwable t,
                                    @Nullable final String additional) {
        String output = "";
        if (t!=null) { output+=ExceptionTools.dumpException(t) + "<br><hr><br>"; }
        if (additional != null) {
            output += additional;
        }
        try {
            if (t!=null) {
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
                if (LogHandler.suppress(t)) {
                    System.out.println("Exception Report Suppressed " + LogHandler.getCount(t) + "x" + LogHandler.getSignature(t));
                    return;
                }
                MailTools.mail((Config.getDevelopment() ? "Dev" : "PROD") + " EX : " + header + " - " + t.getLocalizedMessage(), output);
                return;
            }
            MailTools.mail((Config.getDevelopment() ? "Dev" : "PROD") + " EX : " + header, output);
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

    public static String textureURL(final String textureUUID) {
        return "https://picture-service.secondlife.com/" + textureUUID + "/320x240.jpg";
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
        LogHandler.mailprefix = "E:" + (Config.getDevelopment() ? "(DEV) " : " ");
    }

    private static void configureMailTarget() {
        MailTools.defaultfromaddress = Config.getDeveloperEmail();
        MailTools.defaulttoaddress = Config.getDeveloperEmail();
        MailTools.defaulttoname = "SL Developers";
        MailTools.defaultfromname = (Config.getDevelopment() ? "Dev " : "") + Config.getHostName();
        MailTools.defaultserver = Config.getMailServer();
    }

    private static void startup() {
        try {
            ClassTools.getClasses();
            log().config("Configuring default developer mail target");
            configureMailTarget(); // mails are gonna be messed up coming from logging init
            log().config("Scanning for modules");
            findModules();
            log().config("Initialising Core URL DistributorPageMapper");
            if (!Config.getDevelopment()) {
                log().config("SL Services starting up on " + Config.getHostName());
            } else {
                log().config("SL DEVELOPMENT Services starting up on " + Config.getHostName());
            }
            db = new MySqlDBConnection("SL" + (Config.getDevelopment() ? "DEV" : ""), Config.getJdbc());
            for (SLModule module : modules.values()) {
                log().config("Initialising module - " + module.getName());
                module.initialise();
            }
            // turn on path tracing AFTER initialisation as initialisation may update the schema from the module directly
            if (Config.getDatabasePathTracing()) {
                log().config("Database calling path verification is enabled for SLCore and primary SL services");
                db.permit("net.coagulate.SL.Data");
            }
            for (SLModule module : modules.values()) {
                log().config("Starting module - " + module.getName());
                module.startup();
                module.registerChanges();
            }
            // something about mails may break later on so we send a test mail here...
            MailTools.mail("CoagulateSL "+(Config.getDevelopment()?"DEVELOPMENT ":"")+"startup on "+Config.getHostName()+" ("+SL.getStackBuildDate()+")", htmlVersionDump().toString());
            // TODO Pricing.initialise();
            listener = new HTTPListener(Config.getPort(), URLDistribution.getPageMapper());
            log().info("Startup complete.");
            log().info("========================================================================================================================");
            log().info(outerPad("=====[ Coagulate " + (Config.getDevelopment() ? "DEVELOPMENT " : "") + "Second Life Services ]======"));
            log().info("========================================================================================================================");
            for (SLModule module : modules.values()) {
                log().info(spacePad(module.getBuildDateString()+" - "+module.commitId()+" - " +module.getName() + " - "+ module.getDescription()));
            }
            log().info("------------------------------------------------------------------------------------------------------------------------");
            log().info(spacePad(SL.getStackBuildDate()+" - CoagulateSL Stack"));
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

    public static Container htmlVersionDump() {
        Paragraph p=new Paragraph();
        p.alignment("center");
        Table t=new Table();
        p.add(t);
        t.collapsedBorder();
        t.row().header("Name").
                header("Commit Date").
                header("Commit Hash").
                header("Description");
        for (SLModule module:modules()) {
            t.row().data(module.getName()).
                    data(module.getBuildDateString()).
                    data(module.commitId()).
                    data(module.getDescription());
        }
        t.row().header("CoagulateSL").alignCell("left").
                header(SL.getStackBuildDate()).alignCell("left").
                header("Coagulate SL Stack Build Information").alignCell("left").spanCell(2);
        t.styleCascade("padding: 2px");
        return new Preformatted().add(p);
    }

    private static String spacePad(String s) {
        StringBuilder sBuilder = new StringBuilder(s);
        while (sBuilder.length()<120) { sBuilder.append(" "); }
        return sBuilder.toString();
    }
    private static String spacePrePad(String s) {
        StringBuilder sBuilder = new StringBuilder(s);
        while (sBuilder.length()<8) { sBuilder.insert(0, " "); }
        return sBuilder.toString();
    }

    private static String outerPad(String s) {
        StringBuilder sBuilder = new StringBuilder(s);
        while (sBuilder.length()<120) {
            sBuilder.insert(0, "=");
            if (sBuilder.length() == 120) {
                return sBuilder.toString();
            }
            sBuilder.append("=");
        }
        return sBuilder.toString();
    }

    private static void findModules() {
        Set<Class<? extends SLModule>> moduleList = ClassTools.getSubclasses(SLModule.class);
        for (Class<? extends SLModule> module:moduleList) {
            if (modules.containsKey(module.getName())) { throw new SystemBadValueException("Conflict for module name "+module.getName()+" between "+modules.get(module.getName()).getClass().getSimpleName()+" and "+modules.get(module.getName()).getClass().getSimpleName()); }
            try {
                SLModule instance=module.getDeclaredConstructor().newInstance();
                modules.put(instance.getName(),instance);
            } catch (InstantiationException|IllegalAccessException|InvocationTargetException|NoSuchMethodException e) {
                throw new SystemImplementationException("Error instantiating module "+module.getSimpleName()+" - "+e.getLocalizedMessage(),e);
            }
        }
    }

    public static boolean canIM() { return hasModule("JSLBotBridge") || hasModule("GPHUD"); }
    public static void im(String uuid, String message) {
        if (hasModule("JSLBotBridge")) {
            JSONObject json=new JSONObject();
            json.put("uuid",uuid);
            json.put("message",message);
            EventQueue.queue("JSLBotBridge","im",1,json);
            return;
        }
        if (hasModule("GPHUD")) {
            weakInvoke("GPHUD","im",uuid,message);
            return;
        }
        if (!imWarned) { log().warning("There is no supplier configured for delivering instant messages"); imWarned =true; }
    }

    public static void groupInvite(String uuid, String groupUUID, String roleUUID) {
        weakInvoke("JSLBotBridge","groupinvite",uuid,groupUUID,roleUUID);
    }

    public static Collection<SLModule> modules() { return modules.values(); }

    /** a branding name regardless of who owns it (i.e. can return Coagulate SL)
     *
     * @return a branding name regardless of who owns it (i.e. can return Coagulate SL)
     */
    public static String brandNameUniversal() {
        if (Config.isOfficial()) { return "Coagulate SL"; }
        if (!Config.getBrandingName().isBlank()) { return Config.getBrandingName(); }
        return "Unknown";
    }

    public static String getStackBuildDate() {
        Date bd=new Date(0L);
        String bds="UNKNOWN";
        for (SLModule module:modules.values()) {
            if (module.getBuildDate().compareTo(bd)>0) { bd=module.getBuildDate(); bds=module.getBuildDateString(); }
        }
        return bds;
    }

    @Override
    public void run() {
        if (!SL.shutdown) {
            log().severe("JVM Shutdown Hook invoked");
        }
        SL.shutdown = true;
    }

    public static boolean hasModule(String module) {
        return modules.containsKey(module);
    }
    @Nonnull
    public static SLModule getModule(String module) {
        if (!hasModule(module)) { throw new SystemLookupFailureException("There is no module called "+module); }
        return modules.get(module);
    }
    @SuppressWarnings("UnusedReturnValue")
    public static Object weakInvoke(String module, String command, Object... arguments) {
        return getModule(module).weakInvoke(command,arguments);
    }
}
