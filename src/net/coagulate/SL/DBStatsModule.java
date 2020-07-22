package net.coagulate.SL;

import net.coagulate.Core.Database.DB;
import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Tools.ByteTools;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static net.coagulate.SL.SL.DEV;

public class DBStatsModule extends SLModule {
    @Nonnull
    @Override
    public String getName() { return "DBStatsModule"; }

    @Nonnull
    @Override
    public String getDescription() { return "Drives the reporting of database statistics"; }

    @Override
    public void shutdown() {}

    @Override
    public void startup() {}

    @Override
    public void initialise() {}

    @Override
    public void maintenance() {
        if (!Config.enableZabbix()) { return; }
        if (nextRun("DBStatsModule-maintenance",60)) {
            reportDBStats();
        }
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
            final String output= ByteTools.convertStreamToString(zabbix.getInputStream());
            final String error=ByteTools.convertStreamToString(zabbix.getErrorStream());
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


