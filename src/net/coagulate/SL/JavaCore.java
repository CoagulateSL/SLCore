package net.coagulate.SL;

import net.coagulate.Core.BuildInfo.JavaCoreBuildInfo;
import net.coagulate.Core.Database.DBConnection;
import net.coagulate.SL.HTML.ServiceTile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.Map;

public class JavaCore extends SLModule {
    @Nullable
    @Override
    public Map<ServiceTile, Integer> getServices() {
        return null;
    }

    @Nonnull
    @Override
    public String getName() { return "JavaCore"; }

    @Nonnull
    @Override
    public String getDescription() { return "Core Java utilities and libraries"; }

    @Override
    public void shutdown() {}

    @Override
    public void startup() {}

    @Override
    public void initialise() {}

    @Override
    public void maintenance() {}

    @Override
    public void maintenanceInternal() {}

    @Override
    public String commitId() { return JavaCoreBuildInfo.COMMITID; }

    @Override
    public Date getBuildDate() { return JavaCoreBuildInfo.BUILDDATE; }

    @Override
    protected int schemaUpgrade(DBConnection db, String schemaName, int currentVersion) { return currentVersion; }
}
