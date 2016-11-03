/* 
 * Copyright (C) 2016 NexusTools.
 *
 * This library is free software: you can redistribute it and/or modify  
 * it under the terms of the GNU Lesser General Public License as   
 * published by the Free Software Foundation, version 3.0.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Katelyn Slater <ktaeyln@gmail.com>
 */
package net.nexustools.njs.compiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 *
 * @author Katelyn Slater <ktaeyln@gmail.com>
 */
public class JavaSourceCompiler {
	private static final javax.tools.JavaCompiler JAVA_COMPILER;
	private static final StandardJavaFileManager STANDARD_FILE_MANAGER;

	static {
		JAVA_COMPILER = ToolProvider.getSystemJavaCompiler();
		if (JAVA_COMPILER == null)
			throw new RuntimeException("Could not get Java compiler. Please, ensure that JDK is used instead of JRE.");
		STANDARD_FILE_MANAGER = JAVA_COMPILER.getStandardFileManager(null, null, null);
	}
	
	public static Class<?> compileToClass(java.lang.String fileName, java.lang.String className, java.lang.String source) throws ClassNotFoundException {
		return compileToClassLoader(fileName, source).loadClass(className);
	}
	
	public static ClassLoader compileToClassLoader(java.lang.String fileName, java.lang.String source) {
		return new MemoryClassLoader(compile(fileName, source));
	}
	
	public static Map<java.lang.String, byte[]> compile(java.lang.String fileName, java.lang.String source) {
		return compile(fileName, source, new PrintWriter(System.err), null, null);
	}

	private static Map<java.lang.String, byte[]> compile(java.lang.String fileName, java.lang.String source,
		Writer err, java.lang.String sourcePath, java.lang.String classPath) {
		// to collect errors, warnings etc.
		DiagnosticCollector<JavaFileObject> diagnostics
			= new DiagnosticCollector<JavaFileObject>();

		// create a new memory JavaFileManager
		MemoryJavaFileManager fileManager = new MemoryJavaFileManager(STANDARD_FILE_MANAGER);

		// prepare the compilation unit
		List<JavaFileObject> compUnits = new ArrayList<JavaFileObject>(1);
		compUnits.add(fileManager.makeStringSource(fileName, source));

		return compile(compUnits, fileManager, err, sourcePath, classPath);
	}

	private static Map<java.lang.String, byte[]> compile(final List<JavaFileObject> compUnits,
		final MemoryJavaFileManager fileManager,
		Writer err, java.lang.String sourcePath, java.lang.String classPath) {
		// to collect errors, warnings etc.
		DiagnosticCollector<JavaFileObject> diagnostics
			= new DiagnosticCollector<JavaFileObject>();

		// javac options
		List<java.lang.String> options = new ArrayList();
		options.add("-Xlint:all");
		//      options.add("-g:none");
		options.add("-deprecation");
		if (sourcePath != null) {
			options.add("-sourcepath");
			options.add(sourcePath);
		}

		if (classPath != null) {
			options.add("-classpath");
			options.add(classPath);
		}

		// create a compilation task
		javax.tools.JavaCompiler.CompilationTask task
			= JAVA_COMPILER.getTask(err, fileManager, diagnostics,
				options, null, compUnits);

		if (task.call() == false) {
			PrintWriter perr = new PrintWriter(err);
			for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
				perr.println(diagnostic);
			}
			perr.flush();
			throw new net.nexustools.njs.Error.JavaException("SyntaxError", "Failed to compile transpiled java source");
		}

		Map<java.lang.String, byte[]> classBytes = fileManager.getClassBytes();
		try {
			fileManager.close();
		} catch (IOException exp) {
		}

		return classBytes;
	}

	private static class MemoryJavaFileManager extends ForwardingJavaFileManager {

		/**
		 * Java source file extension.
		 */
		private final static java.lang.String EXT = ".java";

		private Map<java.lang.String, byte[]> classBytes;

		public MemoryJavaFileManager(JavaFileManager fileManager) {
			super(fileManager);
			classBytes = new HashMap();
		}

		public Map<java.lang.String, byte[]> getClassBytes() {
			return classBytes;
		}

		public void close() throws IOException {
			classBytes = null;
		}

		public void flush() throws IOException {
		}

		/**
		 * A file object used to represent Java source coming from a string.
		 */
		private static class StringInputBuffer extends SimpleJavaFileObject {

			final java.lang.String source;

			StringInputBuffer(java.lang.String fileName, java.lang.String code) {
				super(toURI(fileName), JavaFileObject.Kind.SOURCE);
				this.source = code;
			}

			public CharBuffer getCharContent(boolean ignoreEncodingErrors) {
				return CharBuffer.wrap(source);
			}
		}

		/**
		 * A file object that stores Java bytecode into the classBytes map.
		 */
		private class ClassOutputBuffer extends SimpleJavaFileObject {

			private java.lang.String name;

			ClassOutputBuffer(java.lang.String name) {
				super(toURI(name), JavaFileObject.Kind.CLASS);
				this.name = name;
			}

			public OutputStream openOutputStream() {
				return new FilterOutputStream(new ByteArrayOutputStream()) {
					public void close() throws IOException {
						out.close();
						ByteArrayOutputStream bos = (ByteArrayOutputStream) out;
						classBytes.put(name, bos.toByteArray());
					}
				};
			}
		}

		public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location,
			java.lang.String className,
			JavaFileObject.Kind kind,
			FileObject sibling) throws IOException {
			if (kind == JavaFileObject.Kind.CLASS) {
				return new ClassOutputBuffer(className);
			} else {
				return super.getJavaFileForOutput(location, className, kind, sibling);
			}
		}

		static JavaFileObject makeStringSource(java.lang.String fileName, java.lang.String source) {
			return new StringInputBuffer(fileName, source);
		}

		static URI toURI(java.lang.String name) {
			File file = new File(name);
			if (file.exists()) {
				return file.toURI();
			} else {
				try {
					final StringBuilder newUri = new StringBuilder();
					newUri.append("mfm:///");
					newUri.append(name.replace('.', '/'));
					if (name.endsWith(EXT)) {
						newUri.replace(newUri.length() - EXT.length(), newUri.length(), EXT);
					}
					return URI.create(newUri.toString());
				} catch (Exception exp) {
					return URI.create("mfm:///com/sun/script/java/java_source");
				}
			}
		}
	}

	private static class MemoryClassLoader extends URLClassLoader {

		private Map<java.lang.String, byte[]> classBytes;

		public MemoryClassLoader(Map<java.lang.String, byte[]> classBytes,
			java.lang.String classPath, ClassLoader parent) {
			super(toURLs(classPath), parent);
			this.classBytes = classBytes;
		}

		public MemoryClassLoader(Map<java.lang.String, byte[]> classBytes, java.lang.String classPath) {
			this(classBytes, classPath, ClassLoader.getSystemClassLoader());
		}

		public MemoryClassLoader(Map<java.lang.String, byte[]> classBytes) {
			this(classBytes, null, ClassLoader.getSystemClassLoader());
		}

		public Class load(java.lang.String className) throws ClassNotFoundException {
			return loadClass(className);
		}

		public Iterable<Class> loadAll() throws ClassNotFoundException {
			List<Class> classes = new ArrayList<Class>(classBytes.size());
			for (java.lang.String name : classBytes.keySet()) {
				classes.add(loadClass(name));
			}
			return classes;
		}

		protected Class findClass(java.lang.String className) throws ClassNotFoundException {
			byte[] buf = classBytes.get(className);
			if (buf != null) {
				// clear the bytes in map -- we don't need it anymore
				classBytes.put(className, null);
				return defineClass(className, buf, 0, buf.length);
			} else {
				return super.findClass(className);
			}
		}

		private static URL[] toURLs(java.lang.String classPath) {
			if (classPath == null) {
				return new URL[0];
			}

			List<URL> list = new ArrayList<URL>();
			StringTokenizer st = new StringTokenizer(classPath, File.pathSeparator);
			while (st.hasMoreTokens()) {
				java.lang.String token = st.nextToken();
				File file = new File(token);
				if (file.exists()) {
					try {
						list.add(file.toURI().toURL());
					} catch (MalformedURLException mue) {
					}
				} else {
					try {
						list.add(new URL(token));
					} catch (MalformedURLException mue) {
					}
				}
			}
			URL[] res = new URL[list.size()];
			list.toArray(res);
			return res;
		}
	}
}
