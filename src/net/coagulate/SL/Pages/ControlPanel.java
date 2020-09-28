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
import net.coagulate.Core.Tools.ExceptionTools;
import net.coagulate.Core.Tools.MailTools;
import net.coagulate.Core.Tools.TraceProfiler;
import net.coagulate.SL.*;
import net.coagulate.SL.Data.EventQueue;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.HTTPPipelines.SLPageTemplate;
import net.coagulate.SL.HTTPPipelines.Url;

import javax.annotation.Nonnull;
import javax.mail.MessagingException;
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
		Container page=state.page().root();
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
				User user=User.findUsername(state.parameter("input"),false);
				page.p(new Preformatted(state.parameter("input")+" username resolved to "+user));
			}
			catch (final Throwable t) { page.add(new Paragraph(new Preformatted(t.getLocalizedMessage()))); }
		}
		if ("FindUserKey".equals(state.parameter("FindUserKey"))) {
			try {
				User user=User.findUserKeyNullable(state.parameter("input"));
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
			for (SLModule module:SL.modules()) {
				SL.log("ControlPanel").warning("Forcing maintenance run on "+module.getName());
				module.maintenance();
			}
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
                        if (stacktrace.length()>0) { stacktrace.append("<br>"); }
						stacktrace.append(className).append("/").append(element.getMethodName()).append(":").append(element.getLineNumber());
					}
				}
                t.row().
                    add(thread.getName()).
                    add(thread.isDaemon()+"").
				    add(stacktrace.toString());
			}
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
		if ("SystemException".equals(state.parameter("SystemException"))) {
			throw new SystemImplementationException("Manually triggered system exception");
		}
		if ("Shutdown".equals(state.parameter("Shutdown"))) {
			SL.shutdown();
		}
		//todo
		//noinspection deprecation
		page.form().add(new Raw("<input type=text name=input>")).
				submit("GS Test").
				    submit("Thread Info").
				    submit("Test Mail").
				    submit("UserException").
				    submit("SystemException").
					submit("LoggedOnlyException").
				    submit("Shutdown").
					submit("ForceMaintenance").
					submit("FormatUsername").
					submit("ReFormatUsernames").
					submit("FindUserKey").submit("FindUserName").
				    submit("NameAPI").submit("Out of permit SQL");

		for (String traceProfile: TraceProfiler.profiles()) {
			page.header1(traceProfile);
			//todo
			//noinspection deprecation
			page.add(new Raw(TraceProfiler.reportProfile(traceProfile)));
		}
		page.add(EventQueue.tabulate());
	}

}
