package net.coagulate.SL;

import net.coagulate.Core.Database.DB;
import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.MariaDBConnection;
import net.coagulate.Core.Exceptions.System.SystemInitialisationException;
import net.coagulate.Core.Exceptions.System.SystemRemoteFailureException;
import net.coagulate.Core.HTTP.HTTPListener;
import net.coagulate.Core.Tools.*;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.JSLBot.JSLBot;
import net.coagulate.JSLBot.LLCATruster;
import net.coagulate.LSLR.LSLR;
import net.coagulate.SL.Data.LockTest;
import net.coagulate.SL.HTTPPipelines.PageMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;
import static net.coagulate.SL.Config.LOCK_NUMBER_GPHUD_MAINTENANCE;

/**
 * Bootstrap class.
 *
 * @author Iain Price
 */
public class SL extends Thread {
	public static final String VERSION="v0.03.00";
	public static boolean DEV;
	@Nullable
	private static JSLBot bot;
	@Nullable
	private static Logger log;
	private static boolean shutdown;
	private static boolean errored;
	@Nullable
	private static DBConnection db;
	@Nullable
	private static HTTPListener listener;
	private static int watchdogcycle;
	private static long laststats=new Date().getTime();
	private static long nextarchival=new Date().getTime()+((int) ((Math.random()*60.0*45.0*1000.0)));
	private static int gphudoffset;

	private SL() {}

	@Nonnull
	public static JSLBot bot() {
		if (bot==null) { throw new SystemInitialisationException("Access to bot before it is set up"); }
		return bot;
	}

	@Nonnull
	public static Logger getLogger(final String subspace) { return Logger.getLogger("SL."+subspace); }

	@Nonnull
	public static Logger getLogger() {
		//if (log() ==null) { throw new SystemInitialisationException("Logger not yet initialised"); }
		return log();
	}

	public static void shutdown() { shutdown=true; }

	public static void main(@Nonnull final String[] args) {
		if (args.length>0 && "DEV".equalsIgnoreCase(args[0])) { DEV=true; }
		try {
			try { startup(); }
			// print stack trace is discouraged, but the log handler may not be ready yet.
			catch (@Nonnull final Throwable e) {
				errored=true;
				e.printStackTrace();
				log().log(SEVERE,"Startup failed: "+e.getLocalizedMessage(),e);
				shutdown=true;
			}
			Runtime.getRuntime().addShutdownHook(new SL());
			while (!shutdown) {
				watchdog();
				if (!shutdown) { Maintenance.maintenance(); }
			}
		} catch (@Nonnull final Throwable t) {
			System.out.println("Main loop crashed: "+t);
			t.printStackTrace();
		}
		try { _shutdown(); } catch (@Nonnull final Throwable t) {
			System.out.println("Shutdown crashed: "+t);
			t.printStackTrace();
		}
		System.exit(0);
	}

	private static void startup() {
		loggingInitialise();
		configureMailTarget(); // mails are gonna be messed up coming from logging init
		if (!DEV) {
			log().config("SL Services starting up on "+Config.getNodeName()+" (#"+Config.getNode()+")");
		} else {
			log().config("SL DEVELOPMENT Services starting up on "+Config.getNodeName()+" (#"+Config.getNode()+")");
		}
		//startGPHUD(); if (1==1) { System.exit(0); }
		LLCATruster.doNotUse(); // as in we use our own truster later on
		ClassTools.getClasses();
		db=new MariaDBConnection("SL"+(DEV?"DEV":""),Config.getJdbc());
		CATruster.initialise();
		startBot();
		Pricing.initialise();
		IPC.test();
		startGPHUD();
		if (!DEV) { startLSLR(); } // never in dev
		if (!DEV) { waitBot(); } // makes dev restart faster to ignore this
		listener=new HTTPListener(Config.getPort(),Config.getKeyMaterialFile(),new PageMapper());
		log().info("=====[ Coagulate "+(DEV?"DEVELOPMENT ":"")+"Second Life Services {JavaCore, JSLBot, GPHUD, LSLR} version "+VERSION+", startup is fully complete ]=====");
	}

	private static void _shutdown() {
		log().config("SL Services shutting down");
		if (listener!=null) { listener.blockingShutdown(); }
		if (bot!=null) { bot.shutdown("SL System is shutting down"); }
		DB.shutdown();
		log().config("SL Services shutdown is complete, exiting.");
		if (errored) { System.exit(1); }
		System.exit(0);
	}

	private static void startBot() {
		bot=new JSLBot(Config.getBotConfig());
		bot.registershutdownhook=false;
		bot.ALWAYS_RECONNECT=true;
		bot.start();
	}

	private static void startLSLR() {
		if (!DEV) {
			log().config("Starting LSLR submodule for Quiet Life Rentals services");
			try { LSLR.initialise(); } catch (@Nonnull final SQLException e) {
				throw new SystemInitialisationException("LSLR startup failed",e);
			}
			log().config("Started LSLR submodule");
		}
	}

	private static void waitBot() {
		try { bot().waitConnection(30000); } catch (@Nonnull final IllegalStateException e) {}
		if (!bot().connected()) {
			bot().shutdown("Failed to connect");
			shutdown=true;
			errored=true;
			throw new SystemRemoteFailureException("Unable to connect to Second Life");
		}
		getLogger().config("Primary Second Life automated agent has started");
	}

	public static void startGPHUD() {
		GPHUD.initialiseAsModule(SL.DEV,Config.getGPHUDJdbc(),Config.getHostName(),Config.getNode()+1);
		// make sure the lock is ok
		new LockTest(LOCK_NUMBER_GPHUD_MAINTENANCE);
	}

	public static void watchdog() {
		try { Thread.sleep(1000); } catch (@Nonnull final InterruptedException e) {}
		if (shutdown) return;
		if (!DB.test()) {
			log().log(SEVERE,"Database failed connectivity test, shutting down.");
			shutdown=true;
			errored=true;
		}
		// hmm //if (!listener.isAlive()) { log.log(SEVERE,"Primary listener thread is not alive"); shutdown=true; errored=true; return; }
	}

	private static void loggingInitialise() {
		LogHandler.initialise();
		log=Logger.getLogger("net.coagulate.SL");
		LogHandler.mailprefix="E:"+(DEV?"(DEV) ":" ");
	}

	private static void configureMailTarget() {
		MailTools.defaultfromaddress="sl-cluster-alerts@predestined.net";
		MailTools.defaulttoaddress=MailTools.defaultfromaddress;
		MailTools.defaulttoname="SL Developers";
		MailTools.defaultfromname=(DEV?"Dev ":"")+Config.getHostName();
		MailTools.defaultserver="127.0.0.1";
	}

	@Nonnull
	public static DBConnection getDB() {
		if (db==null) { throw new SystemInitialisationException("DB access before DB is initialised"); }
		return db;
	}

	@Nonnull
	public static String getBannerURL() {
		return "/resources/banner-coagulate"+(DEV?"-dev":"")+".png";
	}

	@Nonnull
	public static String getBannerHREF() {
		return "<img src=\""+getBannerURL()+"\">";
	}

	public static void report(final String header,
	                          @Nonnull final Throwable t,
	                          @Nonnull final DumpableState state)
	{
		final String output=ExceptionTools.dumpException(t)+"<br><hr><br>"+state.toHTML();
		LogHandler.alreadyMailed(t);
		try {
			if (LogHandler.suppress(t)) {
				System.out.println("Exception Report Suppressed "+LogHandler.getCount(t)+"x"+LogHandler.getSignature(t));
			}
			else
			{ MailTools.mail((DEV?"Dev":"PROD")+" EX : "+header+" - "+t.getLocalizedMessage(),output); }
		} catch (@Nonnull final MessagingException e) {
			getLogger().log(SEVERE,"Exception mailing out about exception",e);
		}
	}

	@Nonnull
	public static Logger log() {
		if (log==null) { throw new SystemInitialisationException("Logger is null"); }
		return log;
	}

	@Override
	public void run() {
		if (!SL.shutdown) { log().severe("JVM Shutdown Hook invoked"); }
		SL.shutdown=true;
	}

}
