package net.coagulate.SL.Pages;

import net.coagulate.Core.Tools.ExceptionTools;
import net.coagulate.Core.Tools.MailTools;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.ByteCode;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.ByteCodeDataType;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSCompiler;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.GSParser;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.GSStart;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.ParseException;
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
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
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
			for (Map.Entry<Thread, StackTraceElement[]> entry : threads.entrySet()) {
				Thread thread = entry.getKey();
				t.openRow();
				t.add(thread.getName());
				t.add(thread.isDaemon()+"");
				StackTraceElement[] stack= entry.getValue();
				StringBuilder stacktrace= new StringBuilder();
				for (StackTraceElement element:stack) {
					if (stacktrace.length() > 0) { stacktrace.append("<br>"); }
					String classname=element.getClassName();
					if (classname.startsWith("net.coagulate.")) {
						stacktrace.append(classname).append("/").append(element.getMethodName()).append(":").append(element.getLineNumber());
					}
				}
				t.add(stacktrace.toString());
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
		if ("GS Test".equals(state.get("GS Test"))) {
			String script=state.get("script");
			ByteArrayInputStream bais=new ByteArrayInputStream(script.getBytes());
			GSParser parser = new GSParser(bais);
			parser.enable_tracing();
			GSStart gsscript=null;
			try {
				gsscript=parser.Start();
				page.paragraph("Parser completed");
				page.add(new Raw(gsscript.toHtml()));
				page.paragraph("<pre>"+"</pre>");
			}
			catch (Throwable e) { // catch throwable bad, but "lexical error" is an ERROR type... which we're not meant to catch.   but have to.  great.
				if (e instanceof ParseException) {
					ParseException pe=(ParseException)e;
					parser.enable_tracing();
					String tokenimage="";
					tokenimage="Last token: "+pe.currentToken.image+"<br>";
					page.paragraph("Parse failed: "+e.toString()+"<br>"+tokenimage);
				}
				page.paragraph("Parse failed: "+e.toString());
				e.printStackTrace();
			}
			if (gsscript!=null) {
				try {
					GSCompiler compiler=new GSCompiler(gsscript);
					List<ByteCode> bytecode=compiler.compile();
					page.paragraph("Compilation completed!");
					StringBuilder code= new StringBuilder("<pre><table border=0>");
					for(ByteCode bc:bytecode) {
						code.append("<tr><td>").append(bc.explain().replaceFirst(" \\(", "</td><td><i>(")).append("</i></td><td>");
						ArrayList<Byte> bcl=new ArrayList<>();
						bc.toByteCode(bcl);
						for (Byte b:bcl) {
							code.append(b).append(" ");
						}
						code.append("</td></tr>");
					}
					code.append("</table></pre>");
					page.paragraph(code.toString());
					page.paragraph("<b>Byte code</b>");
					Byte[] rawcode=compiler.toByteCode();
					StringBuilder bcstring= new StringBuilder("<pre><table border=0><tr><th>00</th>");
					for (int i=0;i<rawcode.length;i++) {
						if ((i%10)==0) { bcstring.append("</tr><tr><th>").append(i).append("</th>"); }
						bcstring.append("<td>").append(rawcode[i]).append("</td>");
					}
					bcstring.append("</tr></table></pre>");
					page.paragraph(bcstring.toString());
					page.paragraph("<b>Byte code decode</b>");
					GSVM gsvm=new GSVM(rawcode);
					page.paragraph(gsvm.toHtml());
					page.paragraph("<b>Simulation run</b>");
					List<GSVM.ExecutionStep> steps = gsvm.simulate(null);
					StringBuilder output= new StringBuilder("<table border=1><th>PC</th><th>OpCode</th><th>OpArgs</th><th>Stack</th><th>Variables</th></tr>");
					for (GSVM.ExecutionStep step:steps) {
						output.append("<tr><th>").append(step.programcounter).append("</th><td>").append(step.decode).append("</td><td><table>");
						for (int i=0;i<step.resultingstack.size();i++) {
							output.append("<tr><th>").append(i).append("</th><td>").append(step.resultingstack.get(i).htmlDecode()).append("</td></tr>");
						}
						output.append("</table></td><td><table>");
						for (Map.Entry<String, ByteCodeDataType> entry : step.resultingvariables.entrySet()) {
							String decode="???";
							if (entry.getValue() !=null) {
								decode = entry.getValue().htmlDecode();
							}
							output.append("<tr><th>").append(entry.getKey()).append("</th><td>").append(decode).append("</td></tr>");
						}
						output.append("</table></td></tr>");
						if (step.t!=null) { output.append("<tr><td colspan=100>").append(ExceptionTools.toHTML(step.t)).append("</td></tr>"); }
					}
					output.append("</table>");
					page.paragraph(output.toString());
				} catch (Throwable e) { page.paragraph("<b>Compilation failed : "+e.toString()+"</b>"); page.paragraph(ExceptionTools.toHTML(e));}
			}
		}
		page.form().
				submit("GS Test").
				submit("Thread Info").
				submit("Test Mail").
				submit("Region Stats Archival").
				submit("UserException").
				submit("SystemException").
				submit("Shutdown").
				add(new Raw("<br><textarea rows=10 cols=80 name=script>"+(state.get("script")!=null?state.get("script"):"")+"</textarea>"));
	}


}
