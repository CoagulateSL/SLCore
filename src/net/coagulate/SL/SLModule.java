package net.coagulate.SL;

import net.coagulate.Core.Exceptions.System.SystemInitialisationException;
import net.coagulate.Core.Tools.UnixTime;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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
        final Properties properties = new Properties();
        try { properties.load(this.getClass().getClassLoader().getResourceAsStream(getName() + ".properties")); } catch (IOException e) {
            throw new SystemInitialisationException("Unable to load properties for "+getName(),e);
        }

        version = properties.getProperty("version");
        try {
            String parts[]=version.split("\\.");
            if (parts.length!=3) {
                majorversion=0;
                minorversion=0;
                bugfixversion=99;
            } else {
                majorversion=Integer.parseInt(parts[0]);
                minorversion=Integer.parseInt(parts[1]);
                bugfixversion=Integer.parseInt(parts[2]);
            }
        } catch (NumberFormatException e) {
            majorversion=0; minorversion=0; bugfixversion=98;
        }


        String abuilddate = properties.getProperty("build");
        System.out.println("Parsing:"+abuilddate);
        abuilddate=abuilddate.replaceAll("T"," ");
        try {
            abuilddate=abuilddate.replaceAll("Z"," ");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            builddate=sdf.parse(abuilddate);
            System.out.println("First pass ("+abuilddate+")");
        } catch (ParseException e) {}
        if (builddate==null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmm");
                sdf.setTimeZone(TimeZone.getTimeZone("Europe/London"));
                builddate = sdf.parse(abuilddate);
                System.out.println("Second pass");
            } catch (ParseException e) {
            }
        }
        if (builddate==null) { builddate=new Date(0); }
        System.out.println("Parsed:"+builddate);
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
