package net.coagulate.SL;

import net.coagulate.Core.Database.DB;
import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Tools.ByteTools;
import net.coagulate.Core.Tools.Cache;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static java.util.logging.Level.*;
import static net.coagulate.SL.SL.DEV;

public class SLCore extends SLModule {
    public static final int MAJORVERSION=1;
    public static final int MINORVERSION=0;
    public static final int BUGFIXVERSION=8;
    public static final String COMMITID ="MANUAL";
    public static final Date BUILDDATE=new Date(0L);
    public final int majorVersion() { return MAJORVERSION; }
    public final int minorVersion() { return MINORVERSION; }
    public final int bugFixVersion() { return BUGFIXVERSION; }
    public final String commitId() { return COMMITID; }
    public static String getBuildDate() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(BUILDDATE);
    }

    @Nonnull
    @Override
    public String getName() { return "SLCore"; }

    @Nonnull
    @Override
    public String getDescription() { return "Provides core services"; }

    @Override
    public void shutdown() {}

    @Override
    public void startup() {
    }

    @Override
    public void initialise() {
        schemaCheck(SL.getDB(),"slcore",2);
    }

    @Override
    public void maintenance() {
        if (nextRun("SLCore-Cache-Clear",60)) {
            Cache.maintenance();
        }
        if (!Config.enableZabbix()) { return; }
        if (nextRun("SLCore-DBStats-maintenance",60)) {
            reportDBStats();
        }
    }

    @Override
    protected int schemaUpgrade(DBConnection db, String schemaname, int currentversion)
    {
        if (currentversion==1) {
            currentversion=2;
            SL.log("SLCore").log(CONFIG,"Upgrading schema from 1 to 2");
            SL.log("SLCore").log(CONFIG,"Schema: Change lastrun in masternode to default 0 and not null");
            db.d("alter table masternode modify column lastrun int default 0 not null");
        }
        return currentversion;
    }

    private void reportDBStats() {
        if (DEV) {return;} // only log for production.
        int queries=0;
        int updates=0;
        long querytime=0;
        long updatetime=0;
        long querymax=0;
        long updatemax=0;
        for (final DBConnection db: DB.get()) {
            final DBConnection.DBStats stats=db.getStats();
            queries=queries+stats.queries;
            updates=updates+stats.updates;
            querytime=querytime+stats.querytotal;
            updatetime=updatetime+stats.updatetotal;
            if (stats.querymax>querymax) { querymax=stats.querymax; }
            if (stats.updatemax>updatemax) { updatemax=stats.updatemax; }
        }
        float queryavg=0;
        float updateavg=0;
        if (queries>0) { queryavg=((float) querytime)/((float) queries); }
        if (updates>0) { updateavg=((float) updatetime)/((float) updates); }
        //getLogger().fine("Stats: "+queries+"q, avg "+queryavg+" worst "+querymax+".  "+updates+"u, avg "+updateavg+" worst "+updatemax);
        String results="";
        results+=Config.getHostName().toLowerCase()+" mariadb.cluster.queries "+queries+"\n";
        results+=Config.getHostName().toLowerCase()+" mariadb.cluster.queryavg "+queryavg+"\n";
        results+=Config.getHostName().toLowerCase()+" mariadb.cluster.querymax "+querymax+"\n";
        results+=Config.getHostName().toLowerCase()+" mariadb.cluster.updates "+updates+"\n";
        results+=Config.getHostName().toLowerCase()+" mariadb.cluster.updateavg "+updateavg+"\n";
        results+=Config.getHostName().toLowerCase()+" mariadb.cluster.updatemax "+updatemax+"\n";
        try {
            final Process zabbix=Runtime.getRuntime().exec(new String[]{"/usr/bin/zabbix_sender","-z",Config.getZabbixServer(),"-i-"});
            zabbix.getOutputStream().write(results.getBytes(StandardCharsets.UTF_8));
            zabbix.getOutputStream().close();
            //final String output=
            ByteTools.convertStreamToString(zabbix.getInputStream());
            //final String error
            ByteTools.convertStreamToString(zabbix.getErrorStream());
            //getLogger().fine("Zabbix output:"+output);
            //getLogger().fine("Zabbix stderr:"+error);
            zabbix.getErrorStream().close();
            zabbix.getOutputStream().close();
            if (!zabbix.waitFor(5, TimeUnit.SECONDS)) {
                log(Level.WARNING,"Zabbix task did not exit promptly");
                zabbix.destroy();
                if (!zabbix.waitFor(5,TimeUnit.SECONDS)) {
                    log(WARNING,"Zabbix task did not destroy either...");
                    zabbix.destroyForcibly();
                    if (!zabbix.waitFor(5,TimeUnit.SECONDS)) {
                        log(SEVERE,"Zabbix task did not forcibly destroy either...");
                    }
                }
            }
        }
        catch (@Nonnull final IOException e) {
            log(Level.WARNING,"Error while passing stats to zabbix",e);
        }
        catch (@Nonnull final InterruptedException e) {
            log(Level.WARNING,"Interrupted while passing stats to zabbix",e);
        }
    }

    private void log(Level warning, String s, Exception e) { SL.log("DBStats").log(warning,s,e); }
    private void log(Level warning, String s) { SL.log("DBStats").log(warning,s); }
}


