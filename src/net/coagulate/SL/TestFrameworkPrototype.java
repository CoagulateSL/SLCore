package net.coagulate.SL;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
public abstract class TestFrameworkPrototype {
	protected final SelfTest.PassFailRecord results=new SelfTest.PassFailRecord();
	public          Logger                  logger;
	
	protected TestFrameworkPrototype() {
		logger=
				SL.log("UNSPECIFIED-MODULE"); // override this in the module specific constructor.  this is here because dumb.
	}
	
	protected boolean runTest(final String testName,final Supplier<TestOutput> test) {
		try {
			final TestOutput result=test.get();
			result(result.pass(),testName,result.message());
			return result.pass();
		} catch (final Exception e) {
			logger.log(Level.SEVERE,"FAIL : "+testName+" : FAILED WITH EXCEPTION : "+e,e);
			result(false,testName,"Exception:"+e);
			return false;
		}
	}
	
	public void result(final boolean passed,final String testName,final String message) {
		if (passed) {
			logger.log(Level.INFO," ok  : "+testName+" : "+message);
			results.record(new TestResult(true,testName,message));
		} else {
			logger.log(Level.SEVERE,"FAIL : "+testName+" : "+message);
			results.record(new TestResult(false,testName,message));
		}
	}
	
	public void error(final String message,final Throwable exception) {
		logger.log(Level.SEVERE,message,exception);
	}
	
	public void info(final String message) {
		logger.info(message);
	}
	
	public record TestResult(boolean pass,String name,String message) {
	}
	
	public record TestOutput(boolean pass,String message) {
	}
}