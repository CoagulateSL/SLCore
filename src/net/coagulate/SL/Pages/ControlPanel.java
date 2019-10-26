package net.coagulate.SL.Pages;

import net.coagulate.Core.Tools.ExceptionTools;
import net.coagulate.Core.Tools.MailTools;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.SL.Config;
import net.coagulate.SL.HTTPPipelines.AuthenticatedContainerHandler;
import net.coagulate.SL.HTTPPipelines.Page;
import net.coagulate.SL.HTTPPipelines.PageMapper.Url;
import net.coagulate.SL.Maintenance;
import net.coagulate.SL.Pages.HTML.Raw;
import net.coagulate.SL.Pages.HTML.State;
import net.coagulate.SL.Pages.HTML.Table;
import net.coagulate.SL.SL;

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

	@Override
	protected void run(State state, Page page) {
		if (!state.user().superuser()) {
			throw new SystemException("Unauthorised access to Control Panel from " + state.user());
		}
		page.layout(Page.PAGELAYOUT.CENTERCOLUMN);
		page.header("Control Panel");
		if ("Test Mail".equals(state.get("Test Mail"))) {
			page.paragraph("Sending mail");
			try {
				MailTools.mail("CL Cluster " + Config.getHostName(), "sl-cluster-alerts@predestined.net", "SL Mail Tester", "sl-cluster-alerts@predestined.net", "SL Cluster mail test", "Test OK");
			} catch (MessagingException ex) {
				Logger.getLogger(ControlPanel.class.getName()).log(Level.SEVERE, null, ex);
				page.add(new Raw(ExceptionTools.toHTML(ex)));
			}
			page.paragraph("Sent mail");
		}
		if ("Thread Info".equals(state.get("Thread Info"))) {
			Table t=new Table();
			page.add(t);
			t.header("Name").header("Daemon").header("Stacktrace");
			Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
			for (Thread thread:threads.keySet()) {
				t.openRow();
				t.add(thread.getName());
				t.add(thread.isDaemon()+"");
				StackTraceElement[] stack=threads.get(thread);
				String stacktrace="";
				for (StackTraceElement element:stack) {
					if (!stacktrace.isEmpty()) { stacktrace+="<br>"; }
					String classname=element.getClassName();
					if (classname.startsWith("net.coagulate.")) {
						stacktrace+=classname+"/"+element.getMethodName()+":"+element.getLineNumber();
					}
				}
				t.add(stacktrace);
			}
		}
		if ("UserException".equals(state.get("UserException"))) {
			throw new UserException("Manually triggered user exception");
		}
		if ("SystemException".equals(state.get("SystemException"))) {
			throw new SystemException("Manually triggered system exception");
		}
		if ("Region Stats Archival".equals(state.get("Region Stats Archival"))) {
			page.paragraph("Running Region State");
			Maintenance.regionStatsArchival();
		}
		if ("Shutdown".equals(state.get("Shutdown"))) {
			SL.shutdown();
		}
		page.form().
				submit("Thread Info").
				submit("Test Mail").
				submit("Region Stats Archival").
				submit("UserException").
				submit("SystemException").
				submit("Shutdown");
	}


}
