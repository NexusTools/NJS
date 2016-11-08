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
 */
package net.nexustools.njs.compiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.nio.CharBuffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.net.URI;

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
 * @author Katelyn Slater <kate@nexustools.com>
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
		return new ByteMapClassLoader(compileJavaSource(fileName, source));
	}
	
	public static Map<java.lang.String, byte[]> compileJavaSource(java.lang.String fileName, java.lang.String source) {
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		ByteMapJavaFileManager fileManager = new ByteMapJavaFileManager();
		List<JavaFileObject> compUnits = new ArrayList<JavaFileObject>(1);
		compUnits.add(new StringJavaFileObject(fileName, source));

		javax.tools.JavaCompiler.CompilationTask task = JAVA_COMPILER.getTask(null, fileManager, diagnostics, new ArrayList(), null, compUnits);

		if (task.call() == false) {
			for (Diagnostic diagnostic : diagnostics.getDiagnostics())
				System.out.println(diagnostic);
			throw new RuntimeException("Failed to compile java source");
		}

		Map<java.lang.String, byte[]> classBytes = fileManager.classBytes;
		try {
			fileManager.close();
		} catch (IOException ex) {}

		return classBytes;
	}
	
	private static class StringJavaFileObject extends SimpleJavaFileObject {

		final java.lang.String source;
		StringJavaFileObject(java.lang.String fileName, java.lang.String code) {
			super(ByteMapJavaFileManager.toURI(fileName), JavaFileObject.Kind.SOURCE);
			this.source = code;
		}

		@Override
		public CharBuffer getCharContent(boolean ignoreEncodingErrors) {
			return CharBuffer.wrap(source);
		}
	}

	private static class ByteMapJavaFileManager extends ForwardingJavaFileManager {

		private Map<java.lang.String, byte[]> classBytes = new HashMap();

		public ByteMapJavaFileManager() {
			super(STANDARD_FILE_MANAGER);
		}

		@Override
		public void close() throws IOException {
			classBytes = null;
		}

		@Override
		public void flush() throws IOException {
		}

		private class ClassOutputBuffer extends SimpleJavaFileObject {

			private final java.lang.String name;

			ClassOutputBuffer(java.lang.String name) {
				super(toURI(name), JavaFileObject.Kind.CLASS);
				this.name = name;
			}

			@Override
			public OutputStream openOutputStream() {
				return new FilterOutputStream(new ByteArrayOutputStream()) {
					@Override
					public void close() throws IOException {
						out.close();
						ByteArrayOutputStream bos = (ByteArrayOutputStream) out;
						classBytes.put(name, bos.toByteArray());
					}
				};
			}
		}

		@Override
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

		static URI toURI(java.lang.String name) {
			File file = new File(name);
			if (file.exists()) {
				return file.toURI();
			} else {
				try {
					final StringBuilder newUri = new StringBuilder();
					newUri.append("mfm:///");
					newUri.append(name.replace('.', '/'));
					if (name.endsWith(".java")) {
						newUri.replace(newUri.length() - ".java".length(), newUri.length(), ".java");
					}
					return URI.create(newUri.toString());
				} catch (Exception exp) {
					return URI.create("mfm:///com/sun/script/java/java_source");
				}
			}
		}
	}

	private static class ByteMapClassLoader extends ClassLoader {

		private final Map<java.lang.String, byte[]> classBytes;
		public ByteMapClassLoader(Map<java.lang.String, byte[]> classBytes) {
			this.classBytes = classBytes;
		}

		@Override
		protected Class findClass(java.lang.String className) throws ClassNotFoundException {
			byte[] buf = classBytes.get(className);
			if (buf != null) {
				classBytes.put(className, null);
				return defineClass(className, buf, 0, buf.length);
			} else {
				return super.findClass(className);
			}
		}
	}
}
