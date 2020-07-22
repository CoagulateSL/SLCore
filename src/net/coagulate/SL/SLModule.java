package net.coagulate.SL;

import net.coagulate.Core.Tools.UnixTime;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public abstract class SLModule {

    private Map<String,Integer> nextruns=new HashMap<>();
    protected boolean nextRun(String name,int interval) {
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
}
