package net.coagulate.SL;

import net.coagulate.Core.Tools.UnixTime;

import javax.annotation.Nonnull;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public abstract class SLModule {

    private final Map<String,Integer> nextruns=new HashMap<>();
    protected final boolean nextRun(String name,int interval) {
        if (!nextruns.containsKey(name)) { nextruns.put(name,UnixTime.getUnixTime()); } // why am i doing unixtime stuff here...
        if (UnixTime.getUnixTime()<nextruns.get(name)) { return false; }
        nextruns.put(name,nextruns.get(name)+interval);
        return true;
    }

    @Nonnull public abstract String getName();
    @Nonnull public abstract String getDescription();
    public abstract void shutdown();
    public abstract void startup();
    public abstract void initialise();
    public abstract void maintenance();
    private String version;
    private Date builddate;
    private int majorversion;
    private int minorversion;
    private int bugfixversion;
    public SLModule() {
        try {
            final Properties properties = new Properties();
            properties.load(this.getClass().getClassLoader().getResourceAsStream(getName() + ".properties"));
            version = properties.getProperty("version");
            String abuilddate = properties.getProperty("build");
            System.out.println("Parsing:"+abuilddate);
            abuilddate=abuilddate.replaceAll("T"," ");
            try {
                builddate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ").parse(abuilddate);
                System.out.println("First pass");
            } catch (ParseException e) {}
            if (builddate==null) {
                try {
                    builddate = new SimpleDateFormat("yyyyMMdd-HHmm").parse(abuilddate);
                    System.out.println("Second pass");
                } catch (ParseException e) {
                }
            }
            if (builddate==null) { builddate=new Date(0); }
            System.out.println("Parsed:"+builddate);
        } catch (Throwable error) {
            error.printStackTrace();
            version = "0.0.0"; majorversion=0; minorversion=0; bugfixversion=0;
        }
    }

    public String getBuildDate() {
        return new SimpleDateFormat("YYYY-MM-dd HH:mm").format(builddate);
    }

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

    public final int getMajorVersion() { return majorversion; }
    public final int getMinorVersion() { return minorversion; }
    public final int getBugfixversion() { return bugfixversion; }
    public final String getVersion() { return getMajorVersion()+"."+getMinorVersion()+"."+getBugfixversion(); }
}
