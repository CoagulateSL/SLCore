package net.coagulate.SL;

import net.coagulate.Core.BuildInfo.SLCoreBuildInfo;
import net.coagulate.Core.Database.DB;
import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.HTML.Page;
import net.coagulate.Core.HTTP.URLDistribution;
import net.coagulate.Core.Tools.ByteTools;
import net.coagulate.Core.Tools.Cache;
import net.coagulate.Core.Tools.ClassTools;
import net.coagulate.SL.Data.EventQueue;
import net.coagulate.SL.HTML.ServiceTile;
import net.coagulate.SL.HTTPPipelines.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.*;

public class SLCore extends SLModule {
    public static final boolean DEBUG_URLS=false;
    public final String commitId() { return SLCoreBuildInfo.COMMITID; }
    public Date getBuildDate() { return SLCoreBuildInfo.BUILDDATE; }

    @Nullable
    @Override
    public Map<ServiceTile, Integer> getServices() {
        return null;
        /*
        Map<ServiceTile, Integer> ret=new HashMap<>();
        ret.put(new ServiceTile("SLCore","Provides core services, page management, authentication, and similar",null,null,getVersion(),getBuildDateString(),commitId()),999);
        return ret;
         */
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
        final Logger logger = Logger.getLogger(getClass().getCanonicalName());
        schemaCheck(SL.getDB(), "slcore", SLCORE_DATABASE_SCHEMA_VERSION);
        URLDistribution.register("", HTMLMapper.get());
        for (final Method method : ClassTools.getAnnotatedMethods(Url.class)) {
            final Url annotation = method.getAnnotation(Url.class);
            checkMethod(method);
            if (annotation.pageType() == PageType.HTML) {
                URLDistribution.register(annotation.url(), HTMLMapper.get());
                HTMLMapper.get().exact(annotation.url(), method);
                if (DEBUG_URLS) {
                    logger.fine("HTTPMapper exact URL " + annotation.url() + " to " + method);
                }
            }
            if (annotation.pageType() == PageType.SLAPI) {
                URLDistribution.register(annotation.url(), SLAPIMapper.get());
                SLAPIMapper.get().exact(annotation.url(), method);
                if (DEBUG_URLS) {
                    logger.fine("SLAPIMapper exact URL " + annotation.url() + " to " + method);
                }
            }
            if (annotation.pageType() == PageType.PLAINTEXT) {
                URLDistribution.register(annotation.url(), PlainTextMapper.get());
                PlainTextMapper.get().exact(annotation.url(), method);
                if (DEBUG_URLS) {
                    logger.fine("PlainTextMapper exact URL " + annotation.url() + " to " + method);
                }
            }
        }
        for (final Method method : ClassTools.getAnnotatedMethods(UrlPrefix.class)) {
            final UrlPrefix annotation = method.getAnnotation(UrlPrefix.class);
            checkMethod(method);
            if (annotation.pageType() == PageType.HTML) {
                URLDistribution.register(annotation.url(), HTMLMapper.get());
                HTMLMapper.get().prefix(annotation.url(), method);
                if (DEBUG_URLS) {
                    logger.fine("HTTPMapper prefix URL " + annotation.url() + " to " + method);
                }
            }
            if (annotation.pageType() == PageType.SLAPI) {
                URLDistribution.register(annotation.url(), SLAPIMapper.get());
                SLAPIMapper.get().prefix(annotation.url(), method);
                if (DEBUG_URLS) {
                    logger.fine("SLAPIMapper prefix URL " + annotation.url() + " to " + method);
                }
            }
            if (annotation.pageType() == PageType.PLAINTEXT) {
                URLDistribution.register(annotation.url(), PlainTextMapper.get());
                PlainTextMapper.get().exact(annotation.url(), method);
                if (DEBUG_URLS) {
                    logger.fine("PlainTextMapper exact URL " + annotation.url() + " to " + method);
                }
            }
        }
    }

    private void checkMethod(final Method m) {
        final String qualifiedMethodName = m.getDeclaringClass().getCanonicalName() + "." + m.getName();
        try {
            if (!m.canAccess(null)) {
                throw new SystemImplementationException("No public access on " + qualifiedMethodName + " during URL setup");
            }
        } catch (final IllegalArgumentException e) {
            throw new SystemImplementationException("Not a static method? on URL setup for " + qualifiedMethodName, e);
        }
        if (m.getParameterCount() != 1) {
            throw new SystemImplementationException("Incorrect parameters on " + qualifiedMethodName + " during URL setup (Should be singular state)");
        }
        if (!m.getParameters()[0].getType().equals(State.class)) {
            throw new SystemImplementationException("Parameter on " + qualifiedMethodName + " is not of correct type during URL setup");
        }
        if (!m.getReturnType().equals(void.class)) {
            throw new SystemImplementationException("Wrong return type on " + qualifiedMethodName + " during URL setup");
        }
    }

    @Override
    public void maintenanceInternal() {
        if (nextRun("SLCore-Cache-Clear", 60, 30)) {
            Cache.maintenance();
        }
        if (Config.enableZabbix() && nextRun("SLCore-DBStats-maintenance", 60, 0)) {
            reportDBStats();
        }
        if (nextRun("SLCore-Page-Thread-Cleaner", 60, 30)) {
            Page.maintenance();
        }
        if (nextRun("SLCore-State-Cleaner", 60, 30)) {
            State.maintenance();
        }
    }
    @Override
    public void maintenance() {
        for (final EventQueue event : EventQueue.getOutstandingEvents()) {
            final String module = event.getModuleName();
            if (SL.hasModule(module)) {
                SL.getModule(module).processEvent(event);
            }
        }
    }


    @Override
    protected int schemaUpgrade(final DBConnection db, final String schemaName, int currentVersion) {
        if (currentVersion == 1) {
            currentVersion = 2;
            SL.log("SLCore").log(CONFIG, "Upgrading schema from 1 to 2");
            SL.log("SLCore").log(CONFIG, "Schema: Change lastrun in masternode to default 0 and not null");
            db.d("alter table masternode modify column lastrun int default 0 not null");
        }
        if (currentVersion == 2) {
            currentVersion = 3;
            SL.log("SLCore").log(CONFIG, "Upgrading schema from 2 to 3");
            SL.log("SLCore").log(CONFIG, "Schema: Introduce the eventqueue table");
            db.d("""
                    CREATE TABLE `eventqueue` (
                      `eventid` INT NOT NULL AUTO_INCREMENT,
                      `modulename` VARCHAR(64) NOT NULL,
                      `commandname` VARCHAR(64) NOT NULL,
                      `queued` INT NOT NULL,
                      `expires` INT NOT NULL,
                      `claimed` INT NULL DEFAULT NULL,
                      `completed` INT NULL DEFAULT NULL,
                      `status` VARCHAR(64) NOT NULL DEFAULT '',
                      `structureddata` VARCHAR(4096) NOT NULL DEFAULT '{}',
                      PRIMARY KEY (`eventid`),
                      UNIQUE INDEX `eventid_UNIQUE` (`eventid` ASC),
                      INDEX `eventqueue_queued` (`queued` ASC),
                      INDEX `eventqueue_expires` (`expires` ASC),
                      INDEX `eventqueue_claimed` (`claimed` ASC));""");
        }
        if (currentVersion==3) {
            currentVersion = 4;
            SL.log("SLCore").log(CONFIG, "Upgrading schema from 3 to 4");
            SL.log("SLCore").log(CONFIG, "Schema: Rename ID column to id in eventqueue");
            db.d("ALTER TABLE `eventqueue` CHANGE COLUMN `eventid` `id` INT(11) NOT NULL AUTO_INCREMENT");
        }
        if (currentVersion==4) {
            currentVersion = 5;
            SL.log("SLCore").log(CONFIG, "Upgrading schema from 4 to 5");
            SL.log("SLCore").log(CONFIG, "Schema: Add column to eventqueue to log server that executed event");
            db.d("ALTER TABLE `eventqueue` ADD COLUMN `executor` VARCHAR(64) NULL DEFAULT NULL AFTER `structureddata`");
        }
        if (currentVersion==5) {
            currentVersion = 6;
            SL.log("SLCore").log(CONFIG, "Upgrading schema from 5 to 6");
            SL.log("SLCore").log(CONFIG, "Schema: Add column suspended to users table");
            db.d("ALTER TABLE `users` ADD COLUMN `suspended` tinyint(4) NOT NULL DEFAULT 0");
        }
        if (currentVersion==6) {
            currentVersion = 7;
            SL.log("SLCore").log(CONFIG, "Upgrading schema from 6 to 7");
            SL.log("SLCore").log(CONFIG, "Schema: Added user preferences table");
            db.d("""
                    CREATE TABLE `userpreferences` (
                      `id` INT NOT NULL AUTO_INCREMENT,
                      `userid` INT NOT NULL,
                      `application` VARCHAR(64) NOT NULL,
                      `preferencename` VARCHAR(64) NOT NULL,
                      `preferencevalue` VARCHAR(4096) NOT NULL,
                      PRIMARY KEY (`id`),
                      INDEX `userpreferences_users_userid_idx` (`userid` ASC),
                      INDEX `userpreferences_userid_index` (`userid` ASC),
                      INDEX `userpreferences_userapplication` (`userid` ASC, `application` ASC),
                      CONSTRAINT `userpreferences_users_userid`
                        FOREIGN KEY (`userid`)
                        REFERENCES `users` (`id`)
                        ON DELETE CASCADE
                        ON UPDATE RESTRICT);
                    """);
        }
        // don't forget to update  SLCORE_DATABASE_SCHEMA_VERSION
        return currentVersion;
    }
    public static final int SLCORE_DATABASE_SCHEMA_VERSION=7;

    private void reportDBStats() {
        if (Config.getDevelopment()) {return;} // only log for production.
        int queries=0;
        int updates=0;
        long queryTime=0;
        long updateTime=0;
        long queryMax=0;
        long updateMax=0;
        for (final DBConnection db: DB.get()) {
            final DBConnection.DBStats stats=db.getStats();
            queries=queries+stats.queries;
            updates=updates+stats.updates;
            queryTime=queryTime+stats.queryTotal;
            updateTime=updateTime+stats.updateTotal;
            if (stats.queryMax >queryMax) { queryMax=stats.queryMax; }
            if (stats.updateMax >updateMax) { updateMax=stats.updateMax; }
        }
        float queryAverage=0;
        float updateAverage=0;
        if (queries>0) { queryAverage=((float) queryTime)/ queries; }
        if (updates>0) { updateAverage=((float) updateTime)/ updates; }
        String results="";
        results+=Config.getHostName().toLowerCase()+" mariadb.cluster.queries "+queries+"\n";
        results+=Config.getHostName().toLowerCase()+" mariadb.cluster.queryavg "+queryAverage+"\n";
        results+=Config.getHostName().toLowerCase()+" mariadb.cluster.querymax "+queryMax+"\n";
        results+=Config.getHostName().toLowerCase()+" mariadb.cluster.updates "+updates+"\n";
        results+=Config.getHostName().toLowerCase()+" mariadb.cluster.updateavg "+updateAverage+"\n";
        results+=Config.getHostName().toLowerCase()+" mariadb.cluster.updatemax "+updateMax+"\n";
        try {
            @SuppressWarnings("CallToRuntimeExec") final Process zabbix=Runtime.getRuntime().exec(new String[]{"/usr/bin/zabbix_sender","-z",Config.getZabbixServer(),"-i-"});
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
                log(WARNING, "Zabbix task did not exit promptly");
                zabbix.destroy();
                if (!zabbix.waitFor(5,TimeUnit.SECONDS)) {
                    log(WARNING,"Zabbix task did not destroy either...");
                    zabbix.destroyForcibly();
                    if (!zabbix.waitFor(5,TimeUnit.SECONDS)) {
                        log(SEVERE,"Zabbix task did not forcibly destroy either...");
                    }
                }
            }
        } catch (@Nonnull final IOException e) {
            log(WARNING, "Error while passing stats to zabbix", e);
        } catch (@Nonnull final InterruptedException e) {
            log(WARNING, "Interrupted while passing stats to zabbix", e);
        }
    }

    private void log(final Level warning, final String s, final Exception e) {
        SL.log("DBStats").log(warning, s, e);
    }

    private void log(final Level warning, final String s) {
        SL.log("DBStats").log(warning, s);
    }
}


