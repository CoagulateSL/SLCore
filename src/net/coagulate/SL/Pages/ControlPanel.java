package net.coagulate.SL.Pages;

import net.coagulate.Core.Exceptions.System.SystemBadValueException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserAccessDeniedException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.Core.Tools.ExceptionTools;
import net.coagulate.Core.Tools.MailTools;
import net.coagulate.SL.Config;
import net.coagulate.SL.GetAgentID;
import net.coagulate.SL.HTTPPipelines.AuthenticatedContainerHandler;
import net.coagulate.SL.HTTPPipelines.Page;
import net.coagulate.SL.HTTPPipelines.PageMapper.Url;
import net.coagulate.SL.Pages.HTML.Raw;
import net.coagulate.SL.Pages.HTML.State;
import net.coagulate.SL.Pages.HTML.Table;
import net.coagulate.SL.SL;

import javax.annotation.Nonnull;
import javax.mail.MessagingException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Iain Price
 */
public class ControlPanel extends AuthenticatedContainerHandler {

	@Url("/ControlPanel")
	public ControlPanel() {super();}

	// ----- Internal Instance -----
	@Override
	protected void run(@Nonnull final State state,
	                   @Nonnull final Page page) {
		if (!state.user().superuser()) {
			throw new UserAccessDeniedException("Unauthorised access to Control Panel from "+state.userNullable());
		}
		page.layout(Page.PAGELAYOUT.CENTERCOLUMN);
		page.header("Control Panel");
		if ("NameAPI".equals(state.get("NameAPI"))) {
			try {
				final String ret=GetAgentID.getAgentID(state.get("input"));
				page.add(new Raw("<pre>"+state.get("input")+" resolved to "+ret+"</pre><br>"));
			}
			catch (final Throwable t) { page.add(new Raw("<pre>"+t.getLocalizedMessage()+"</pre><br>")); }
		}
		if ("Test Mail".equals(state.get("Test Mail"))) {
			page.paragraph("Sending mail");
			try {
				MailTools.mail("CL Cluster "+Config.getHostName(),
				               "sl-cluster-alerts@predestined.net",
				               "SL Mail Tester",
				               "sl-cluster-alerts@predestined.net",
				               "SL Cluster mail test",
				               "Test OK"
				              );
			}
			catch (@Nonnull final MessagingException ex) {
				Logger.getLogger(ControlPanel.class.getName()).log(Level.SEVERE,null,ex);
				page.add(new Raw(ExceptionTools.toHTML(ex)));
			}
			page.paragraph("Sent mail");
		}
		if ("Thread Info".equals(state.get("Thread Info"))) {
			final Table t=new Table();
			page.add(t);
			t.header("Name").header("Daemon").header("Stacktrace");
			final Map<Thread,StackTraceElement[]> threads=Thread.getAllStackTraces();
			for (final Map.Entry<Thread,StackTraceElement[]> entry: threads.entrySet()) {
				final Thread thread=entry.getKey();
				t.openRow();
				t.add(thread.getName());
				t.add(thread.isDaemon()+"");
				final StackTraceElement[] stack=entry.getValue();
				final StringBuilder stacktrace=new StringBuilder();
				for (final StackTraceElement element: stack) {
					if (stacktrace.length()>0) { stacktrace.append("<br>"); }
					final String classname=element.getClassName();
					if (classname.startsWith("net.coagulate.")) {
						stacktrace.append(classname).append("/").append(element.getMethodName()).append(":").append(element.getLineNumber());
					}
				}
				t.add(stacktrace.toString());
			}
		}
		if ("UserException".equals(state.get("UserException"))) {
			throw new UserInputStateException("Manually triggered user exception");
		}
		if ("LoggedOnlyException".equals(state.get("LoggedOnlyException"))) {
			SL.log().log(Level.INFO,"Manually generated logged only exception",new SystemBadValueException("Manually generated log event"));
			page.form().add(new Raw("<p>Sent event</P>"));
		}
		if ("SystemException".equals(state.get("SystemException"))) {
			throw new SystemImplementationException("Manually triggered system exception");
		}
		if ("Shutdown".equals(state.get("Shutdown"))) {
			SL.shutdown();
		}
		page.form().add(new Raw("<input type=text name=input>")).
				submit("GS Test").
				    submit("Thread Info").
				    submit("Test Mail").
				    submit("UserException").
				    submit("SystemException").
					submit("LoggedOnlyException").
				    submit("Shutdown").
				    submit("NameAPI");
	}


}
