package net.coagulate.SL;

import net.coagulate.Core.Exceptions.System.SystemBadValueException;
import net.coagulate.Core.Exceptions.User.UserInputValidationParseException;
import net.coagulate.Core.Tools.ValueMapper;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class Config {
    private static final Properties config=new Properties();

    static void load(@Nonnull final String file) {
        System.out.println("Coagulate SL: Loading configuration file from "+file);
        try (FileInputStream fis= new FileInputStream(file)){
            config.load(new InputStreamReader(fis));
        } catch (IOException e) {
            System.err.println("Failed to read config file : "+e.getLocalizedMessage());
            System.exit(1);
        }
    }

    public static boolean getBoolean(@Nonnull final String key, final boolean defaultvalue) {
        if (config.containsKey(key)) { return getBoolean(key); }
        return defaultvalue;
    }

    public static boolean getBoolean(@Nonnull final String key) {
        try {
            return ValueMapper.toBoolean(config.getProperty(key));
        } catch (UserInputValidationParseException e) {
            throw new SystemBadValueException("Config error in '"+key+"' - "+e.getLocalizedMessage(),e);
        }
    }

    @Nonnull public static String getString(@Nonnull final String key) {
        try {
            return config.getProperty(key);
        } catch (UserInputValidationParseException e) {
            throw new SystemBadValueException("Config error in '"+key+"' - "+e.getLocalizedMessage(),e);
        }
    }

    @Nonnull public static String getString(@Nonnull final String key, @Nonnull final String defaultvalue) {
        if (config.containsKey(key)) { return getString(key); }
        return defaultvalue;
    }
    public static int getInt(@Nonnull final String key) {
        try {
            return Integer.parseInt(config.getProperty(key));
        } catch (NumberFormatException|UserInputValidationParseException e) {
            throw new SystemBadValueException("Config error in '"+key+"' - "+e.getLocalizedMessage(),e);
        }
    }
    public static int getInt(@Nonnull final String key,int defaultvalue) {
        if (config.containsKey(key)) {
             return getInt(key);
        }
        return defaultvalue;
    }




    public static String getHostName() {
        return getString("hostname");
    }

    public static boolean getDevelopment() { return getBoolean("DEVELOPMENT", false); }

    public static String getBotFirstName() { return getString("jslbotbridge-firstname"); }

    public static String getBotLastName() {
        return getString("jslbotbridge-lastname");
    }

    public static String getBotPassword() {
        return getString("jslbotbridge-password");
    }

    public static String getBotOwnerUUID() {
        return getString("jslbotbridge-owneruuid");
    }

    public static String getBotOwnerUsername() {
        return getString("jslbotbridge-ownerusername");
    }

    public static String getBotHomeRegion() {
        return getString("jslbotbridge-bothome");
    }

    public static String getDeveloperEmail() {
        return getString("developermail");
    }

    public static String getMailServer() {
        return getString("mailserver","127.0.0.1");
    }

    public static String getJdbc() {
        return getString("jdbc");
    }

    public static String getLSLRJdbc() {
        return getString("lslr-jdbc");
    }

    public static String getQLRFastBotFirstName() { return getString("lslr-fast-firstname"); }

    public static String getQLRFastBotLastName() { return getString("lslr-fast-lastname"); }

    public static String getQLRFastBotPassword() { return getString("lslr-fast-password"); }

    public static String getQLRSlowBotFirstName() { return getString("lslr-slow-firstname"); }

    public static String getQLRSlowBotLastName() { return getString("lslr-slow-lastname"); }

    public static String getQLRSlowBotPassword() { return getString("lslr-slow-password"); }

    public static String getLSLRBotHome() { return getString("lslr-bots-home"); }

    public static String getGPHUDJdbc() { return getString("gphud-jdbc"); }

    public static int getPort() { return getInt("port"); }

    public static int getSSOWindow() { return getInt("ssowindowseconds",5*60); }

    public static int emailTokenLifespan() { return getInt("emailtokenlifespanseconds",60*60); }

    public static int getSessionLifespan() { return getInt("sessionlifespan",60*60*6); }

    public static boolean enableZabbix() { return getBoolean("zabbix",false); }

    public static String getZabbixServer() { return getString("zabbixserver","127.0.0.1"); }

    public static String getDigestSalt() { return getString("digestsalt"); }

    public static String getDistributionRegion() { return getString("gphud-distributionregion",""); }

    public static String getURLHost() { return getString("urlhostname",getHostName()); }

    public static boolean getDatabasePathTracing() { return getBoolean("db-entry-point-checking",false); }

    // don't cheese this.  it currently does the following things
    // 1) Triggers a group invite to instance owners to join the "GPHUD Instance Owners" group via a bot.
    public static boolean isOfficial() { return getBoolean("official-install",false); }

    public static boolean skipShardCheck() { return getBoolean("skipshardcheck",false); }

    public static String getCoagulateSquareLogo() {
        if (getGrid()==GRID.OSGRID) { return "18d20253-3eea-4180-8d95-3f28a212b0ce"; }
        return "8dcd4a48-2d37-4909-9f78-f7a9eb4ef903";
    }
    public static String getCreatorUUID() {
        if (getGrid()==GRID.OSGRID) { return "cb191fe5-0941-46e6-81b9-c57431fd0ee4"; }
        return "8dc52677-bea8-4fc3-b69b-21c5e2224306";
    }
    public static String getCoagulateLogo() {
        if (getGrid()==GRID.OSGRID) { return "d9208f65-9922-42ef-a385-4f1f38255ef1"; }
        return "c792716b-13a3-06c9-6e7c-33c4e9d5a48f";
    }
    public static String getGPHUDLogo() {
        if (getGrid()==GRID.OSGRID) { return "9676cf69-b36c-4edc-b470-57f2ef4a9505"; }
        return "36c48d34-3d84-7b9a-9979-cda80cf1d96f";
    }
    public static String getCoagulateDevLogo() {
        if (getGrid()==GRID.OSGRID) { return "e3b30b2e-dddd-43cb-a8f1-16ebe5f1b33c"; }
        return "891e1d92-9fc6-b256-a423-b5c037e70e28";
    }

    public static GRID getGrid() {
        String gridname=getString("grid","SecondLife").toLowerCase();
        if (gridname.equals("osgrid")) { return GRID.OSGRID; }
        return GRID.SECONDLIFE;
    }

    public static String getWebLogo() { return getString("branding-web-logo",""); }

    public static String getBrandingOwnerHumanReadable() { return getString("branding-owner-name","Someone Unknown"); }

    public enum GRID {SECONDLIFE,OSGRID}

    public static String getBrandingName() { return getString("branding-name",""); }

    public static String getBrandingOwnerUUID() { return getString("branding-owner","00000000-0000-0000-00000000"); }
    public static String getBrandingOwnerSLURL() { return "secondlife:///app/agent/"+getBrandingOwnerUUID()+"/about"; }
}
