package net.coagulate.SL.Pages;

import net.coagulate.Core.Exceptions.System.SystemBadValueException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserAccessDeniedException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.Core.HTML.Container;
import net.coagulate.Core.HTML.Elements.Paragraph;
import net.coagulate.Core.HTML.Elements.Preformatted;
import net.coagulate.Core.HTML.Elements.Raw;
import net.coagulate.Core.HTML.Elements.Table;
import net.coagulate.Core.HTML.Page;
import net.coagulate.Core.Tools.*;
import net.coagulate.GPHUD.Modules.Instance.Distribution;
import net.coagulate.SL.*;
import net.coagulate.SL.Data.EventQueue;
import net.coagulate.SL.Data.RegionStats;
import net.coagulate.SL.Data.SystemManagement;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.HTTPPipelines.SLPageTemplate;
import net.coagulate.SL.HTTPPipelines.Url;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.mail.MessagingException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Iain Price
 */
public class ControlPanel {
	@Url(url="/ControlPanel")
	public static void controlPanel(@Nonnull final State state) {
		if (!state.user().isSuperAdmin()) {
			throw new UserAccessDeniedException("Unauthorised access to Control Panel from "+state.userNullable());
		}
		Page.page().template(new SLPageTemplate(SLPageTemplate.PAGELAYOUT.NONE));
		final Container page = state.page().root();
		page.header1("Control Panel");
		if ("NameAPI".equals(state.parameter("NameAPI"))) {
			try {
				final String ret= GetAgentID.getAgentID(state.parameter("input"));
				page.p(new Preformatted(state.parameter("input")+" resolved to "+ret));
			}
			catch (final Throwable t) { page.add(new Paragraph(new Preformatted(t.getLocalizedMessage()))); }
		}
		if ("FindUserName".equals(state.parameter("FindUserName"))) {
			try {
                final User user = User.findUsername(state.parameter("input"), false);
				page.p(new Preformatted(state.parameter("input")+" username resolved to "+user));
			}
			catch (final Throwable t) { page.add(new Paragraph(new Preformatted(t.getLocalizedMessage()))); }
		}
		if ("FindUserKey".equals(state.parameter("FindUserKey"))) {
			try {
                final User user = User.findUserKeyNullable(state.parameter("input"));
				page.p(new Preformatted(state.parameter("input")+" username resolved to "+user));
			}
			catch (final Throwable t) { page.add(new Paragraph(new Preformatted(t.getLocalizedMessage()))); }
		}
		if ("ReFormatUsernames".equals(state.parameter("ReFormatUsernames"))) {
			try {
				final String ret= User.reformatUsernames();
				page.p(new Preformatted("Output: "+ret));
			}
			catch (final Throwable t) { page.add(new Paragraph(new Preformatted(t.getLocalizedMessage()))); }
		}
		if ("FormatUsername".equals(state.parameter("FormatUsername"))) {
			try {
				final String ret= User.formatUsername(state.parameter("input"));
				page.p(new Preformatted(state.parameter("input")+" resolved to "+ret));
			}
			catch (final Throwable t) { page.add(new Paragraph(new Preformatted(t.getLocalizedMessage()))); }
		}
		if ("Test Mail".equals(state.parameter("Test Mail"))) {
			page.p("Sending mail");
			try {
				MailTools.mail("SL Stack "+ Config.getHostName(),
				               "sl-cluster-alerts@predestined.net",
				               "SL Mail Tester",
				               "sl-cluster-alerts@predestined.net",
				               "SL Cluster mail test",
				               "Test OK"
				              );
			}
			catch (@Nonnull final MessagingException ex) {
				Logger.getLogger(ControlPanel.class.getName()).log(Level.SEVERE,null,ex);
				//todo
				//noinspection deprecation
				page.add(new Raw(ExceptionTools.toHTML(ex)));
			}
			page.p("Sent mail");
		}
		if("ForceMaintenance".equals(state.parameter("ForceMaintenance"))) {
            for (final SLModule module : SL.modules()) {
                SL.log("ControlPanel").warning("Forcing maintenance run on " + module.getName());
                module.maintenance();
            }
		}
		if ("Thread Profile Info".equals(state.parameter("Thread Profile Info"))) {
			page.add(StackTraceProfiler.htmlDump());
		}
		if ("Thread Info".equals(state.parameter("Thread Info"))) {
			final Table t=new Table();
			t.border(); t.collapsedBorder();
			page.add(t);
			t.row().header("Name").header("Daemon").header("Stacktrace");
			final Map<Thread,StackTraceElement[]> threads=Thread.getAllStackTraces();
			for (final Map.Entry<Thread,StackTraceElement[]> entry: threads.entrySet()) {
				final Thread thread=entry.getKey();
				final StackTraceElement[] stack=entry.getValue();
				final StringBuilder stacktrace=new StringBuilder();
				for (final StackTraceElement element: stack) {
					final String className=element.getClassName();
					if (className.startsWith("net.coagulate.")) {
                        if (!stacktrace.isEmpty()) {
							stacktrace.append("<br>");
						}
						stacktrace.append(className).append("/").append(element.getMethodName()).append(":").append(element.getLineNumber());
					}
				}
                t.row().
						add(thread.getName()).
						add(String.valueOf(thread.isDaemon())).
						add(stacktrace.toString());
			}
		}
		if ("CacheOff".equals(state.parameter("CacheOff"))) {
			SystemManagement.restrictCaches();
			page.form().add("Caching limited");
		}
		if ("CacheOn".equals(state.parameter("CacheOn"))) {
			SystemManagement.unrestrictCaches();
			page.form().add("Caching restored to default behaviour");
		}
		if ("Recalc Names".equals(state.parameter("Recalc Names"))) {
			EventQueue.queue("JSLBotBridge","recalcnames",60,new JSONObject());
			page.form().add("Added recalcnames to event queue");
		}
		if ("Out of permit SQL".equals(state.parameter("Out of permit SQL"))) {
			SL.getDB().d("select count(*) from users");
			page.form().add("Did a database thing naughtily!");
		}
		if ("UserException".equals(state.parameter("UserException"))) {
			throw new UserInputStateException("Manually triggered user exception");
		}
		if ("LoggedOnlyException".equals(state.parameter("LoggedOnlyException"))) {
			SL.log().log(Level.INFO,"Manually generated logged only exception",new SystemBadValueException("Manually generated log event"));
			//todo
			//noinspection deprecation
			page.form().add(new Raw("<p>Sent event</P>"));
		}
		if ("CacheStats".equals(state.parameter("CacheStats"))) {
            final List<Cache.CacheStats> stats = Cache.getSummarisedStats();
            final StringBuilder output = new StringBuilder();
            output.append("<table><tr><th>Cache Name</th><th>Elements</th><th>Cache Hits</th><th>Cache Misses</th></tr>");
            for (final Cache.CacheStats stat : stats) {
                output.append("<tr><td>");
                output.append(stat.cacheName);
                output.append("</td><td>");
                output.append(stat.size);
                output.append("</td><td>");
                output.append(stat.cacheHit);
                output.append("</td><td>");
                output.append(stat.cacheMiss);
                output.append("</td><td>");
                if (stat.cacheHit + stat.cacheMiss > 0) {
					output.append((stat.cacheHit * 100) / (stat.cacheMiss + stat.cacheHit)).append("%");
				}
				output.append("</td></tr>");
			}
			output.append("</table>");
			page.form().add(new Raw(output.toString()));
		}
		if ("SystemException".equals(state.parameter("SystemException"))) {
			throw new SystemImplementationException("Manually triggered system exception");
		}
		if ("RegionStatsArchival".equals(state.parameter("RegionStatsArchival"))) {
			RegionStats.archiveOld();
		}
		if ("Shutdown".equals(state.parameter("Shutdown"))) {
			SL.shutdown();
		}
		if ("ChangeLog".equals(state.parameter("ChangeLog"))) {
			page.form().add(new Raw(ChangeLogging.asHtml()));
		}
		if ("GPHUD Permit".equals(state.parameter("GPHUD Permit"))) {
			final User user=User.findUsername(state.parameter("input"),false);
			user.setPreference("gphud","instancepermit",Integer.toString(UnixTime.getUnixTime()+(60*60*24*7)));
			final net.coagulate.GPHUD.State gpstate=new net.coagulate.GPHUD.State();
			gpstate.setAvatar(user);
			SL.im(user.getUUID(),"""
					===== GPHUD Information =====
					You have been issued a permit to create an installation of GPHUD.
					This permit expires in one week.
					
					You should have been given an item named GPHUD Region Server.
					Rez this on one of your regions and say into local chat
					**createinstance <shortname>
					Replacing <shortname> with a reasonable name for your installation.
					
					Once complete, allow 60 seconds for the device to start up and then
					click the server to be given a HUD.  From here the bottom right quick button
					will take you to the website for further configuration.""");
			Distribution.getServer(gpstate);
		}
		
		//todo
		//noinspection deprecation
		page.form().add(new Raw("<input type=text name=input>")).
				submit("GS Test").
				    submit("Thread Info").
					submit("Thread Profile Info").
				    submit("Test Mail").
				    submit("UserException").
				    submit("SystemException").
					submit("LoggedOnlyException").
				    submit("Shutdown").
		            submit("CacheOff").
				    submit("CacheOn").
					submit("RegionStatsArchival").
					submit("ForceMaintenance").
					submit("FormatUsername").
					submit("ReFormatUsernames").
					submit("FindUserKey").submit("FindUserName").
				    submit("NameAPI").submit("Out of permit SQL").submit("Recalc Names").submit("CacheStats").submit("ChangeLog").submit("GPHUD Permit");

        for (final String traceProfile : TraceProfiler.profiles()) {
            page.header1(traceProfile);
            //todo
            //noinspection deprecation
            page.add(new Raw(TraceProfiler.reportProfile(traceProfile)));
        }
		page.add(EventQueue.tabulate());
	}

}
