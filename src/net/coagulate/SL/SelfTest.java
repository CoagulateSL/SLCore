package net.coagulate.SL;

import net.coagulate.Core.Tools.LogHandler;
import net.coagulate.Core.Tools.MailTools;

import javax.mail.MessagingException;
import java.util.ArrayList;
import java.util.List;

import static java.util.logging.Level.SEVERE;

public class SelfTest {
	public static class PassFailRecord {
		private final List<TestFrameworkPrototype.TestResult> results=new ArrayList<>();
		private       int                                     passes =0;
		private       int                                     fails  =0;
		
		public void record(final TestFrameworkPrototype.TestResult result) {
			if (result.pass()) {
				passes++;
			} else {
				fails++;
			}
			results.add(result);
		}
		
		public void mergeResults(final PassFailRecord merge) {
			passes=passes+merge.passes();
			fails=fails+merge.fails();
			results.addAll(merge.results());
		}
		
		public int passes() {
			return passes;
		}
		
		public int fails() {
			return fails;
		}
		
		public List<TestFrameworkPrototype.TestResult> results() {
			return results;
		}
	}
	
	public static class SelfTestRunner extends Thread {
		public void run() {
			boolean clean=false;
			try {
				this.setName("Self test runner");
				final PassFailRecord total=new PassFailRecord();
				SL.log().info("Beginning system wide self tests");
				for (final SLModule module: SL.modules()) {
					SL.log().info("Beginning self test on module "+module.getName());
					final PassFailRecord results=module.selfTest();
					total.mergeResults(results);
					SL.log()
					  .info("Finished self test on module "+module.getName()+", "+results.passes+" passes, "+results.fails+" fails (Total: "+total.passes+" passed, "+total.fails+" failed)");
				}
				if (total.fails==0) {
					SL.log().info("PASS PASS PASS : All "+total.passes+" self tests successfully passed");
					clean=true;
				} else {
					SL.log().severe("FAIL FAIL FAIL : "+total.fails+" self tests FAILED ("+total.passes+" succeeded)");
					try {
						final StringBuilder body=new StringBuilder("<html><body><table>");
						for (final TestFrameworkPrototype.TestResult r: total.results()) {
							if (r.pass()) {
								body.append("<tr bgcolor=#d0ffd0>");
							} else {
								body.append("<tr bgcolor=#ffd0d0>");
							}
							body.append("<td>").append(r.pass()?"Pass":"FAIL").append("</td>");
							body.append("<td>").append(r.name()).append("</td>");
							body.append("<td>").append(r.message()).append("</td>");
							body.append("</tr>");
						}
						body.append("</table></body></html>");
						if (Config.getDeveloperEmail()!=null && !Config.getDeveloperEmail().isBlank()) {
							MailTools.mail(LogHandler.mailprefix+" SELF TEST FAILED: "+total.fails+"/"+(total.fails+total.passes)+" failed, "+total.passes+" passed.",
							               body.toString());
						}
					} catch (final MessagingException mailfail) {
						SL.log().log(SEVERE,"Failed to mail out about self tests failing : "+mailfail,mailfail);
					}
				}
			}
			catch (final Exception e) {
				SL.log().log(SEVERE,"Exception escaped self testing",e);
			}
			if (Config.getSelfTestOnly()) {
				SL.log().warning("Self test only flag is set, exiting.");
				System.exit(clean?0:1);
			}
			
		}
		
	}
}
