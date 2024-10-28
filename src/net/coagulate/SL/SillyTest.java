package net.coagulate.SL;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSInternalError;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.*;

public class SillyTest {
	
	/** Smiley face
	 *
	 * Oct 28 13:45:13 sol rundev.sh[3542418]: COMPILED net.coagulate.SL.SillyTestCaller
	 * Oct 28 13:45:13 sol rundev.sh[3542418]: Trying the invoke
	 * Oct 28 13:45:13 sol rundev.sh[3542418]: TARGET METHOD WAS CALLED!!!
	 *
	 */
	private static final Map<String,SourceCode> sourceCodes=new HashMap<String,SourceCode>();
	
	static void execute() throws Exception {
		final JavaCompiler javac=ToolProvider.getSystemJavaCompiler();
		final DynamicClassLoader classLoader=new DynamicClassLoader(ClassLoader.getSystemClassLoader());
		final String sourceCode="""
				package net.coagulate.SL;
				public class SillyTestCaller {
					public static void callMe() {
						SillyTest.target();
					}
				}
				""";
		addSource("net.coagulate.SL.SillyTestCaller",sourceCode);
		final Collection<SourceCode> compilationUnits=sourceCodes.values();
		final CompiledCode[] code;
		
		code=new CompiledCode[1];
		final Iterator<SourceCode> iter=compilationUnits.iterator();
		for (int i=0;i<code.length;i++) {
			code[i]=new CompiledCode(iter.next().getClassName());
		}
		final DiagnosticCollector<JavaFileObject> collector=new DiagnosticCollector<>();
		final ExtendedStandardJavaFileManager fileManager=
				new ExtendedStandardJavaFileManager(javac.getStandardFileManager(null,null,null),classLoader);
		final JavaCompiler.CompilationTask task=javac.getTask(null,fileManager,collector,null,null,compilationUnits);
		final boolean result=task.call();
		if (!result||collector.getDiagnostics().size()>0) {
			final StringBuffer exceptionMsg=new StringBuffer();
			exceptionMsg.append("Unable to compile the source");
			boolean hasWarnings=false;
			boolean hasErrors=false;
			for (final Diagnostic<? extends JavaFileObject> d: collector.getDiagnostics()) {
				switch (d.getKind()) {
					case NOTE:
					case MANDATORY_WARNING:
					case WARNING:
						hasWarnings=true;
						break;
					case OTHER:
					case ERROR:
					default:
						hasErrors=true;
						break;
				}
				exceptionMsg.append("\n").append("[kind=").append(d.getKind());
				exceptionMsg.append(", ").append("line=").append(d.getLineNumber());
				exceptionMsg.append(", ").append("message=").append(d.getMessage(Locale.US)).append("]");
			}
			if (hasErrors) {
				throw new GSInternalError(exceptionMsg.toString());
			}
		}
		
		final Map<String,Class<?>> classes=new HashMap<String,Class<?>>();
		for (final String className: sourceCodes.keySet()) {
			classes.put(className,classLoader.loadClass(className));
		}
		for (final String classname:classes.keySet()) {
			System.out.println("COMPILED "+classname);
			System.out.println("Trying the invoke");
			classes.get(classname).getMethod("callMe",null).invoke(null);
		}
	}
	
	public static void addSource(final String className,final String sourceCode) throws Exception {
		sourceCodes.put(className,new SourceCode(className,sourceCode));
	}
	
	public static void target() {
		System.out.println("TARGET METHOD WAS CALLED!!!");
	}
	
	
	public static class DynamicClassLoader extends ClassLoader {
		
		private final Map<String,CompiledCode> customCompiledCode=new HashMap<>();
		
		public DynamicClassLoader(final ClassLoader parent) {
			super(parent);
		}
		
		public void addCode(final CompiledCode cc) {
			customCompiledCode.put(cc.getName(),cc);
		}
		
		@Override
		protected Class<?> findClass(final String name) throws ClassNotFoundException {
			final CompiledCode cc=customCompiledCode.get(name);
			if (cc==null) {
				return super.findClass(name);
			}
			final byte[] byteCode=cc.getByteCode();
			return defineClass(name,byteCode,0,byteCode.length);
		}
	}
	
	public static class CompiledCode extends SimpleJavaFileObject {
		private final ByteArrayOutputStream baos=new ByteArrayOutputStream();
		private final String                className;
		
		public CompiledCode(final String className) throws Exception {
			super(new URI(className),Kind.CLASS);
			this.className=className;
		}
		
		public String getClassName() {
			return className;
		}
		
		@Override
		public OutputStream openOutputStream() throws IOException {
			return baos;
		}
		
		public byte[] getByteCode() {
			return baos.toByteArray();
		}
	}
	
	public static class SourceCode extends SimpleJavaFileObject {
		private       String contents=null;
		private final String className;
		
		public SourceCode(final String className,final String contents) throws Exception {
			super(URI.create("string:///"+className.replace('.','/')+Kind.SOURCE.extension),Kind.SOURCE);
			this.contents=contents;
			this.className=className;
		}
		
		public String getClassName() {
			return className;
		}
		
		public CharSequence getCharContent(final boolean ignoreEncodingErrors) throws IOException {
			return contents;
		}
	}
	
	public static class ExtendedStandardJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
		
		private final List<CompiledCode> compiledCode=new ArrayList<CompiledCode>();
		private final DynamicClassLoader cl;
		
		/**
		 * Creates a new instance of ForwardingJavaFileManager.
		 *
		 * @param fileManager delegate to this file manager
		 * @param cl
		 */
		protected ExtendedStandardJavaFileManager(final JavaFileManager fileManager,final DynamicClassLoader cl) {
			super(fileManager);
			this.cl=cl;
		}
		
		@Override
		public ClassLoader getClassLoader(final JavaFileManager.Location location) {
			return cl;
		}
		
		@Override
		public JavaFileObject getJavaFileForOutput(final JavaFileManager.Location location,
		                                           final String className,
		                                           final JavaFileObject.Kind kind,
		                                           final FileObject sibling) throws IOException {
			
			try {
				final CompiledCode innerClass=new CompiledCode(className);
				compiledCode.add(innerClass);
				cl.addCode(innerClass);
				return innerClass;
			} catch (final Exception e) {
				throw new RuntimeException("Error while creating in-memory output file for "+className,e);
			}
		}
	}
}
