package net.coagulate.SL;

import net.coagulate.Core.BuildInfo.JavaCoreBuildInfo;
import net.coagulate.Core.Database.DBConnection;

import javax.annotation.Nonnull;
import java.util.Date;

public class JavaCore extends SLModule {
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
    public int majorVersion() { return JavaCoreBuildInfo.MAJORVERSION; }

    @Override
    public int minorVersion() { return JavaCoreBuildInfo.MINORVERSION; }

    @Override
    public int bugFixVersion() { return JavaCoreBuildInfo.BUGFIXVERSION; }

    @Override
    public String commitId() { return JavaCoreBuildInfo.COMMITID; }

    @Override
    public Date getBuildDate() { return JavaCoreBuildInfo.BUILDDATE; }

    @Override
    protected int schemaUpgrade(DBConnection db, String schemaname, int currentversion) { return currentversion; }
}
