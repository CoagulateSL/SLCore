package net.coagulate.SL;

import net.coagulate.Core.Tools.UnixTime;

import javax.annotation.Nonnull;
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
    public String getVersion() {
        try {
            final Properties properties = new Properties();
            properties.load(this.getClass().getClassLoader().getResourceAsStream(getName()+".properties"));
            return properties.getProperty("version");
        } catch(Throwable error) { error.printStackTrace(); }
        return "0";
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
}
