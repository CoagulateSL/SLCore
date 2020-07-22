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

    private static final boolean getBoolean(@Nonnull final String key,final boolean defaultvalue) {
        if (config.containsKey(key)) { return getBoolean(key); }
        return defaultvalue;
    }

    private static final boolean getBoolean(@Nonnull final String key) {
        try {
            return ValueMapper.toBoolean(config.getProperty(key));
        } catch (UserInputValidationParseException e) {
            throw new SystemBadValueException("Config error in '"+key+"' - "+e.getLocalizedMessage(),e);
        }
    }

    @Nonnull private static final String getString(@Nonnull final String key) {
        try {
            return config.getProperty(key);
        } catch (UserInputValidationParseException e) {
            throw new SystemBadValueException("Config error in '"+key+"' - "+e.getLocalizedMessage(),e);
        }
    }

    @Nonnull private static final String getString(@Nonnull final String key,@Nonnull final String defaultvalue) {
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

    public static String getQLRSLowBotFirstName() { return getString("lslr-slow-lastname"); }

    public static String getQLRSlowBotPassword() { return getString("lslr-slow-password"); }

    public static String getLSLRBotHome() { return getString("lslr-bots-home"); }

    public static String getGPHUDJdbc() { return getString("gphud-jdbc"); }

    public static int getPort() { return getInt("port"); }

    public static int getSSOWindow() { return getInt("ssowindowseconds",5*60); }

    public static int emailTokenLifespan() { return getInt("emailtokenlifespanseconds",60*60); }

    public static int getSessionLifespan() { return getInt("sessionlifespan",60*60*6); }

    public static boolean enableZabbix() { return getBoolean("zabbix",false); }

    public static String getZabbixServer() { return getString("zabbixserver","127.0.0.1"); }
}
