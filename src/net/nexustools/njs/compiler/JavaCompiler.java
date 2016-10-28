/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import net.nexustools.njs.Global;

/**
 *
 * @author kate
 */
public class JavaCompiler extends AbstractCompiler {
	private static final List<java.lang.String> RESTRICTED_NAMES = Arrays.asList(new java.lang.String[]{"Arguments", "CompiledScript", "Debuggable", "Optimized", "String", "Number", "RegEx", "Global", "Scope", "exec", "call", "multiply", "plus", "divide", "or", "BaseObject", "BaseFunction", "AbstractFunction", "GenericObject", "GenericArray", "ConstructableFunction", "JSHelper"});

	private static java.lang.String toSource(java.lang.String string) {
		return string.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\"", "\\\"");
	}

	private static final Map<java.lang.String, AtomicInteger> USED_NAMES = new HashMap();
	private java.lang.String toClassName(java.lang.String name, boolean topLevel) {
		java.lang.String output = name.replaceAll("[^_a-zA-Z0-9\\xA0-\\uFFFF]", "_");
		if(!output.equals(name) || RESTRICTED_NAMES.indexOf(name) > -1)
			output += Math.abs(name.hashCode());
		
		if(topLevel) {
			AtomicInteger atomicInteger;
			synchronized(USED_NAMES) {
				atomicInteger = USED_NAMES.get(output);
				if(atomicInteger == null)
					USED_NAMES.put(output, atomicInteger = new AtomicInteger());
			}
			int num = atomicInteger.getAndIncrement();
			if(num > 0)
				return toClassName(output + "_" + num, true);
		}
		return output;
	}

	private java.lang.String extendMethodChain(java.lang.String methodPrefix, java.lang.String methodName) {
		boolean methodPrefixIsntNull = false, methodNameIsNull = methodName == null;
		if(methodPrefix != null) {
			if(methodPrefix.equals("<anonymous>")) {
				if(methodNameIsNull)
					return methodPrefix;
				methodPrefix = null;
			} else if(methodPrefix.endsWith(".<anonymous>")) {
				if(methodNameIsNull)
					return methodPrefix;
				methodPrefix = methodPrefix.substring(0, methodPrefix.length()-12);
				methodPrefixIsntNull = true;
			} else if(methodPrefix.isEmpty())
				methodPrefix = null;
			else
				methodPrefixIsntNull = true;
		}
		
		if(!methodNameIsNull && methodPrefixIsntNull)
			return methodPrefix + '.' + methodName;
		else if(methodPrefixIsntNull)
			return methodPrefix + '.' + "<anonymous>";
		else if(!methodNameIsNull)
			return methodName;
		else
			return "<anonymous>";
	}

	private static class SourceBuilder {

		java.lang.String indent = "";
		StringBuilder builder = new StringBuilder();

		public void append(java.lang.String source) {
			assert (source.indexOf('\n') == -1);
			builder.append(source);
		}

		public void appendln(java.lang.String source) {
			append(source);
			builder.append('\n');
			builder.append(indent);
		}

		public void appendln() {
			builder.append('\n');
			builder.append(indent);
		}

		public void indent() {
			indent += '\t';
			builder.append('\t');
		}

		public void unindent() {
			indent = indent.substring(0, indent.length() - 1);
			builder.deleteCharAt(builder.length() - 1);
		}

		@Override
		public java.lang.String toString() {
			return builder.toString(); //To change body of generated methods, choose Tools | Templates.
		}
	}

	private static final javax.tools.JavaCompiler JAVA_COMPILER;
	private static final StandardJavaFileManager STANDARD_FILE_MANAGER;

	static {
		JAVA_COMPILER = ToolProvider.getSystemJavaCompiler();
		if (JAVA_COMPILER == null)
			throw new RuntimeException("Could not get Java compiler. Please, ensure that JDK is used instead of JRE.");
		STANDARD_FILE_MANAGER = JAVA_COMPILER.getStandardFileManager(null, null, null);
		assert ((new JavaCompiler().eval("(function munchkin(){\n\tfunction yellow(){\n\t\treturn 55;\n\t}\n\treturn yellow()\n\t})()", "JavaCompilerStaticTest", false)).exec(new Global(), null).toString().equals("55"));
	}

	private static enum SourceState {
		GlobalScript,
		FunctionScript,
		Function
	}
	public final boolean addDebugging;
	public JavaCompiler() {
		this(true);
	}
	public JavaCompiler(boolean addDebugging) {
		this.addDebugging = addDebugging;
	}
	
	protected void generateBlockSource(SourceBuilder sourceBuilder, ScriptData blockDat, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, boolean addDebugging) {
		for(Parsed part : blockDat.impl) {
			if (addDebugging && (part.rows > 1 || part.columns > 1)) {
				sourceBuilder.append("stackElement.rows = ");
				sourceBuilder.append(java.lang.String.valueOf(part.rows));
				sourceBuilder.appendln(";");
				sourceBuilder.append("stackElement.columns = ");
				sourceBuilder.append(java.lang.String.valueOf(part.columns));
				sourceBuilder.appendln(";");
			}
			generateParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName, addDebugging);
			sourceBuilder.appendln(";");
		}
	}
		
	protected void generateParsedSource(SourceBuilder sourceBuilder, Parsed part, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, boolean addDebugging) {
		if (part instanceof Return) {
			sourceBuilder.append("return ");
			generateParsedSource(sourceBuilder, ((Return) part).rhs, methodPrefix, baseScope, fileName, addDebugging);
			return;
		} else if (part instanceof Call) {
			Parsed reference = ((Call)part).reference;
			while(reference instanceof OpenBracket) // unwrap
				reference = ((OpenBracket)reference).contents;
			
			if(reference instanceof Referency && !(reference instanceof Reference || reference instanceof Call)) {
				final java.lang.String source = reference.toString();
				if(reference instanceof RightReference) {
					final java.lang.String key = ((RightReference)reference).chain.remove(((RightReference)reference).chain.size()-1);
					sourceBuilder.append("callTop(");
					if (addDebugging) {
						sourceBuilder.append("\"");
						sourceBuilder.append(toSource(source));
						sourceBuilder.append("\", ");
					}
					sourceBuilder.append("\"");
					sourceBuilder.append(toSource(key));
					sourceBuilder.append("\", ");
					generateParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, addDebugging);
					for (Parsed arg : ((Call) part).arguments) {
						sourceBuilder.append(", ");
						generateParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, addDebugging);
					}
					sourceBuilder.append(")");
					return;
				} else if(reference instanceof IntegerReference) {
					final int key = ((IntegerReference)reference).ref;
					sourceBuilder.append("callTop(");
					if (addDebugging) {
						sourceBuilder.append("\"");
						sourceBuilder.append(toSource(source));
						sourceBuilder.append("\", ");
					}
					sourceBuilder.append(java.lang.String.valueOf(key));
					sourceBuilder.append(", ");
					generateParsedSource(sourceBuilder, ((IntegerReference)reference).lhs, methodPrefix, baseScope, fileName, addDebugging);
					for (Parsed arg : ((Call) part).arguments) {
						sourceBuilder.append(", ");
						generateParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, addDebugging);
					}
					sourceBuilder.append(")");
					return;
				} else if(reference instanceof ReferenceChain) {
					final java.lang.String key = ((ReferenceChain)reference).chain.remove(((ReferenceChain)reference).chain.size()-1);
					sourceBuilder.append("callTop(");
					if (addDebugging) {
						sourceBuilder.append("\"");
						sourceBuilder.append(toSource(source));
						sourceBuilder.append("\", ");
					}
					sourceBuilder.append("\"");
					sourceBuilder.append(toSource(key));
					sourceBuilder.append("\", ");
					generateParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, addDebugging);
					for (Parsed arg : ((Call) part).arguments) {
						sourceBuilder.append(", ");
						generateParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, addDebugging);
					}
					sourceBuilder.append(")");
					return;
				} else if(reference instanceof New) {
					sourceBuilder.append("callNew(");
					if (addDebugging) {
						sourceBuilder.append("\"");
						sourceBuilder.append(toSource(source));
						sourceBuilder.append("\", ");
					}
					generateParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, addDebugging);
					for (Parsed arg : ((Call) part).arguments) {
						sourceBuilder.append(", ");
						generateParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, addDebugging);
					}
					sourceBuilder.append(")");
					return;
				}
				
				throw new UnsupportedOperationException("Cannot compile call: " + describe(reference));
			}
			
			if(addDebugging) {
				sourceBuilder.append("callTop(");
				sourceBuilder.append("\"");
				sourceBuilder.append(toSource(reference.toString()));
				sourceBuilder.append("\", ");
				generateParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, addDebugging);
				sourceBuilder.append(", ");
				sourceBuilder.append("Undefined.INSTANCE");
				for (Parsed arg : ((Call) part).arguments) {
					sourceBuilder.append(", ");
					generateParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, addDebugging);
				}
				sourceBuilder.append(")");
			} else {
				sourceBuilder.append("((BaseFunction)");
				generateParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, addDebugging);
				sourceBuilder.append(").call(");
				sourceBuilder.append("Undefined.INSTANCE");
				for (Parsed arg : ((Call) part).arguments) {
					sourceBuilder.append(", ");
					generateParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, addDebugging);
				}
				sourceBuilder.append(")");
			}
			return;
			
		} else if (part instanceof Number) {
			sourceBuilder.append("global.wrap(");
			sourceBuilder.append(java.lang.String.valueOf(((Number) part).value));
			sourceBuilder.append(")");
			return;
		} else if (part instanceof Integer) {
			sourceBuilder.append("global.wrap(");
			sourceBuilder.append(java.lang.String.valueOf(((Integer) part).value));
			sourceBuilder.append(")");
			return;
		} else if (part instanceof String) {
			sourceBuilder.append("global.wrap(\"");
			sourceBuilder.append(toSource(java.lang.String.valueOf(((String) part).string)));
			sourceBuilder.append("\")");
			return;
		} else if (part instanceof Reference) {
			sourceBuilder.append(baseScope);
			sourceBuilder.append(".get(\"");
			sourceBuilder.append(toSource(((Reference) part).ref));
			sourceBuilder.append("\")");
			return;
		} else if (part instanceof Plus) {
			sourceBuilder.append("plus(global, ");
			generateParsedSource(sourceBuilder, ((Plus) part).lhs, methodPrefix, baseScope, fileName, addDebugging);
			sourceBuilder.append(", ");
			generateParsedSource(sourceBuilder, ((Plus) part).rhs, methodPrefix, baseScope, fileName, addDebugging);
			sourceBuilder.append(")");
			return;
		} else if (part instanceof Multiply) {
			sourceBuilder.append("multiply(global, ");
			generateParsedSource(sourceBuilder, ((Multiply) part).lhs, methodPrefix, baseScope, fileName, addDebugging);
			sourceBuilder.append(", ");
			generateParsedSource(sourceBuilder, ((Multiply) part).rhs, methodPrefix, baseScope, fileName, addDebugging);
			sourceBuilder.append(")");
			return;
		} else if (part instanceof New) {
			boolean addComma;
			if(addDebugging) {
				addComma = true;
				sourceBuilder.append("constructTop(\"");
				sourceBuilder.append(toSource(((New)part).reference.toString()));
				sourceBuilder.append("\", ");
				generateParsedSource(sourceBuilder, ((New) part).reference, methodPrefix, baseScope, fileName, addDebugging);
			} else {
				addComma = false;
				sourceBuilder.append("((BaseFunction)");
				generateParsedSource(sourceBuilder, ((New) part).reference, methodPrefix, baseScope, fileName, addDebugging);
				sourceBuilder.append(").construct(");
			}
			if (((New) part).arguments != null) {
				for (Parsed arg : ((New) part).arguments) {
					if (addComma)
						sourceBuilder.append(", ");
					else
						addComma = true;
					generateParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, addDebugging);
				}
			}
			sourceBuilder.append(")");
			return;
		} else if(part instanceof RightReference) {
			generateParsedSource(sourceBuilder, ((RightReference) part).ref, methodPrefix, baseScope, fileName, addDebugging);
			for(java.lang.String key : ((RightReference)part).chain) {
				sourceBuilder.append(".get(\"");
				sourceBuilder.append(toSource(key));
				sourceBuilder.append("\")");
			}
			return;
		} else if(part instanceof OpenBracket) {
			sourceBuilder.append("(");
			generateParsedSource(sourceBuilder, ((OpenBracket) part).contents, methodPrefix, baseScope, fileName, addDebugging);
			sourceBuilder.append(")");
			return;
		} else if(part instanceof Throw) {
			sourceBuilder.append("throw new net.nexustools.njs.Error.Thrown(");
			generateParsedSource(sourceBuilder, ((Throw) part).rhs, methodPrefix, baseScope, fileName, addDebugging);
			sourceBuilder.append(")");
			return;
		} else if(part instanceof Function) {
			sourceBuilder.appendln("new ConstructableFunction(global) {");
			sourceBuilder.indent();
			sourceBuilder.appendln("Scope extendScope(BaseObject _this) {");
			sourceBuilder.append("\treturn ");
			sourceBuilder.append(baseScope);
			sourceBuilder.appendln(".extend(_this);");
			sourceBuilder.appendln("}");
			((Function)part).impl.callee = ((Function)part);
			generateScriptSource(sourceBuilder, ((Function)part).impl, methodPrefix, fileName, SourceScope.Function, addDebugging);
			sourceBuilder.unindent();
			sourceBuilder.append("}");
			return;
		} else if(part instanceof Var) {
			List<Var.Set> sets = ((Var)part).sets;
			if(sets.size() > 1) {
			} else {
				Var.Set set = sets.get(0);
				sourceBuilder.append(baseScope);
				sourceBuilder.append(".var(\"");
				sourceBuilder.append(toSource(set.lhs));
				sourceBuilder.append("\", ");
				generateParsedSource(sourceBuilder, set.rhs, methodPrefix, baseScope, fileName, addDebugging);
				sourceBuilder.append(")");
				return;
			}

			throw new UnsupportedOperationException("Cannot compile var: " + describe(part));
		} else if(part instanceof Or) {
			sourceBuilder.append("or(");
			generateParsedSource(sourceBuilder, ((Or) part).lhs, methodPrefix, baseScope, fileName, addDebugging);
			sourceBuilder.append(", ");
			generateParsedSource(sourceBuilder, ((Or) part).rhs, methodPrefix, baseScope, fileName, addDebugging);
			sourceBuilder.append(")");
			return;
		} else if(part instanceof ReferenceChain) {
			java.lang.String first = ((ReferenceChain)part).chain.remove(0);
			sourceBuilder.append(baseScope);
			sourceBuilder.append(".get(\"");
			sourceBuilder.append(toSource(first));
			sourceBuilder.append("\")");
			for(java.lang.String ref : ((ReferenceChain)part).chain) {
				sourceBuilder.append(".get(\"");
				sourceBuilder.append(toSource(ref));
				sourceBuilder.append("\")");
			}
			return;
		} else if(part instanceof IntegerReference) {
			int key = ((IntegerReference)part).ref;
			generateParsedSource(sourceBuilder, ((IntegerReference) part).lhs, methodPrefix, baseScope, fileName, addDebugging);
			sourceBuilder.append(".get(");
			sourceBuilder.append(java.lang.String.valueOf(key));
			sourceBuilder.append(")");
			return;
		} else if(part instanceof OpenArray) {
			boolean first = true;
			sourceBuilder.append("new GenericArray(global, new BaseObject[]{");
			for(Parsed subpart : ((OpenArray)part).entries) {
				if(first)
					first = false;
				else
					sourceBuilder.append(", ");
				generateParsedSource(sourceBuilder, subpart, methodPrefix, baseScope, fileName, addDebugging);
			}
			sourceBuilder.append("})");
			return;
		} else if(part instanceof Set) {
			Parsed lhs = ((Set)part).lhs;
			Parsed rhs = ((Set)part).rhs;
			if(lhs instanceof IntegerReference) {
				sourceBuilder.append("callSet(");
				generateParsedSource(sourceBuilder, ((IntegerReference)lhs).lhs, methodPrefix, baseScope, fileName, addDebugging);
				sourceBuilder.append(", ");
				sourceBuilder.append(java.lang.String.valueOf(((IntegerReference)lhs).ref));
				sourceBuilder.append(", ");
				generateParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, addDebugging);
				sourceBuilder.append(")");
				return;
			} else if(lhs instanceof ReferenceChain) {
				List<java.lang.String> chain = ((ReferenceChain)lhs).chain;
				if(chain.size() > 1) {
					java.lang.String key = chain.remove(chain.size()-1);
					java.lang.String first = chain.remove(0);
					sourceBuilder.append("callSet(");
					sourceBuilder.append(baseScope);
					sourceBuilder.append(".get(\"");
					sourceBuilder.append(toSource(first));
					sourceBuilder.append("\")");
					for(java.lang.String k : chain) {
						sourceBuilder.append(".get(\"");
						sourceBuilder.append(toSource(k));
						sourceBuilder.append("\")");
					}
					sourceBuilder.append(", \"");
					sourceBuilder.append(toSource(key));
					sourceBuilder.append("\", ");
					generateParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, addDebugging);
					sourceBuilder.append(")");
					return;
				} else {
					
				}
			}

			throw new UnsupportedOperationException("Cannot compile set: " + describe(lhs));
		} else if(part instanceof Try) {
			Catch c = ((Try)part).c;
			Finally f = ((Try)part).f;
			if(c != null && f != null) {
				
			} else if(c != null) {
				sourceBuilder.appendln("try {");
				sourceBuilder.indent();
				generateBlockSource(sourceBuilder, ((Try)part).impl, methodPrefix, baseScope, fileName, addDebugging);
				sourceBuilder.unindent();
				sourceBuilder.appendln("} catch(net.nexustools.njs.Error.InvisibleException ex) {");
				sourceBuilder.appendln("\tthrow ex;");
				sourceBuilder.appendln("} catch(Throwable t) {");
				sourceBuilder.indent();
				java.lang.String newScope;
				if(baseScope.equals("catchScope")) {
					int count;
					if(baseScope.length() > 10)
						count = java.lang.Integer.valueOf(baseScope.substring(10));
					else
						count = 0;
					newScope = "catchScope" + (count+1);
				} else
					newScope = "catchScope";
				sourceBuilder.append("final Scope ");
				sourceBuilder.append(newScope);
				sourceBuilder.append(" = ");
				sourceBuilder.append(baseScope);
				sourceBuilder.appendln(".beginBlock();");
				sourceBuilder.append(newScope);
				sourceBuilder.appendln(".enter();");
				sourceBuilder.appendln("try {");
				sourceBuilder.indent();
				sourceBuilder.append(newScope);
				sourceBuilder.append(".param(\"");
				sourceBuilder.append(toSource(((Reference)c.condition).ref));
				sourceBuilder.appendln("\", global.wrap(t));");
				generateBlockSource(sourceBuilder, c.impl, methodPrefix, newScope, fileName, addDebugging);
				sourceBuilder.unindent();
				sourceBuilder.appendln("} finally {");
				sourceBuilder.append("\t");
				sourceBuilder.append(newScope);
				sourceBuilder.appendln(".exit();");
				sourceBuilder.appendln("}");
				sourceBuilder.unindent();
				sourceBuilder.appendln("}");
				return;
			}

			throw new UnsupportedOperationException("Cannot compile try: " + describe(c) + describe(f));
		} else if(part instanceof If) {
			if(((If)part).simpleimpl != null) {
				sourceBuilder.appendln("if(JSHelper.isTrue(");
				generateParsedSource(sourceBuilder, ((If)part).condition, methodPrefix, baseScope, fileName, addDebugging);
				sourceBuilder.appendln("))");
				sourceBuilder.append("\t");
				generateParsedSource(sourceBuilder, ((If)part).simpleimpl, methodPrefix, baseScope, fileName, addDebugging);
				return;
			}

			throw new UnsupportedOperationException("Cannot compile if: " + describe(part));
		} else if(part instanceof Boolean) {
			sourceBuilder.append("global.Boolean.");
			if(((Boolean)part).value)
				sourceBuilder.append("TRUE");
			else
				sourceBuilder.append("FALSE");
			return;
		} else if(part instanceof Null) {
			sourceBuilder.append("Null.INSTANCE");
			return;
		} else if(part instanceof Undefined) {
			sourceBuilder.append("Undefined.INSTANCE");
			return;
		}

		throw new UnsupportedOperationException("Cannot compile: " + describe(part));
	}

	private static enum SourceScope {
		GlobalFunction,
		GlobalScript,
		Function;

		private boolean isFunction() {
			return this == GlobalFunction || this == Function;
		}
	}
	protected void generateScriptSource(SourceBuilder sourceBuilder, ScriptData script, java.lang.String methodPrefix, java.lang.String fileName, SourceScope scope, boolean addDebugging) {
		sourceBuilder.appendln("@Override");
		sourceBuilder.appendln("public String source() {");
		sourceBuilder.append("\treturn \"");
		if (addDebugging)
			sourceBuilder.append(toSource(script.source));
		else
			sourceBuilder.append("[java_code]");
		sourceBuilder.appendln("\";");
		sourceBuilder.appendln("}");
		if (scope.isFunction()) {
			sourceBuilder.appendln("@Override");
			sourceBuilder.appendln("public String name() {");
			sourceBuilder.append("\treturn \"");
			if(script.callee != null)
				sourceBuilder.append(script.callee.name != null ? toSource(script.callee.name) : "<anonymous>");
			else
				sourceBuilder.append(script.methodName != null ? toSource(script.methodName) : "<anonymous>");
			sourceBuilder.appendln("\";");
			sourceBuilder.appendln("}");
			
			sourceBuilder.appendln("@Override");
			sourceBuilder.appendln("public BaseObject call(BaseObject _this, BaseObject... params) {");
			sourceBuilder.appendln("\tfinal Scope baseScope = extendScope(_this);");
			if(script.callee != null) {
				methodPrefix = extendMethodChain(methodPrefix, script.callee.name);
				List<java.lang.String> arguments = script.callee.arguments;
				sourceBuilder.appendln("\tbaseScope.var(\"arguments\", new Arguments(global, this, params));");
				if(!arguments.isEmpty()) {
					sourceBuilder.appendln("\tswitch(params.length) {");
					for(int i=0; i<=arguments.size(); i++) {
						int a=0;
						sourceBuilder.append("\t\tcase ");
						sourceBuilder.append(java.lang.String.valueOf(i));
						sourceBuilder.appendln(":");
						for(; a < i; a++) {
							sourceBuilder.append("\t\t\tbaseScope.var(\"");
							sourceBuilder.append(toSource(arguments.get(a)));
							sourceBuilder.append("\", params[");
							sourceBuilder.append(java.lang.String.valueOf(a));
							sourceBuilder.appendln("]);");
						}
						for(; a < arguments.size(); a++) {
							sourceBuilder.append("\t\t\tbaseScope.var(\"");
							sourceBuilder.append(toSource(arguments.get(a)));
							sourceBuilder.appendln("\", Undefined.INSTANCE);");
						}
						sourceBuilder.appendln("\t\t\tbreak;");
					}
					sourceBuilder.appendln("\t}");
				}
			} else
				methodPrefix = extendMethodChain(methodPrefix, script.methodName);
		} else {
			sourceBuilder.appendln("@Override");
			sourceBuilder.appendln("public BaseObject exec(Global global, Scope scope) {");
			sourceBuilder.appendln("\tif(scope == null)");
			sourceBuilder.appendln("\t\tscope = new Scope(global);");
			sourceBuilder.appendln("\tfinal Scope baseScope = scope;");
		}
		sourceBuilder.indent();
		for (Function function : script.functions) {
			sourceBuilder.append("baseScope.var(\"");
			sourceBuilder.append(toSource(function.name));
			sourceBuilder.append("\", new ");
			sourceBuilder.append(function.uname = toClassName(function.name, false));
			sourceBuilder.appendln("(global, baseScope));");
		}
		if (addDebugging) {
			sourceBuilder.append("final JSHelper.ReplacementStackTraceElement stackElement = JSHelper.renameCall(\"");
			if (methodPrefix != null)
				sourceBuilder.append(methodPrefix);
			sourceBuilder.append("\", \"");
			sourceBuilder.append(fileName);
			sourceBuilder.append("\", ");
			if (script.impl.length > 0) {
				sourceBuilder.append(java.lang.String.valueOf(script.impl[0].rows));
				sourceBuilder.append(", ");
				sourceBuilder.append(java.lang.String.valueOf(script.impl[0].columns));
			} else {
				sourceBuilder.append(java.lang.String.valueOf(script.rows));
				sourceBuilder.append(", ");
				sourceBuilder.append(java.lang.String.valueOf(script.columns));
			}
			sourceBuilder.appendln(");");
		}
		sourceBuilder.appendln("baseScope.enter();");
		sourceBuilder.appendln("try {");
		sourceBuilder.indent();
		if (scope.isFunction()) {
			boolean hasReturn = false, first = true;
			for (Parsed part : script.impl) {
				if (first) {
					first = false;
				} else if (addDebugging && (part.rows > 1 || part.columns > 1)) {
					sourceBuilder.append("stackElement.rows = ");
					sourceBuilder.append(java.lang.String.valueOf(part.rows));
					sourceBuilder.appendln(";");
					sourceBuilder.append("stackElement.columns = ");
					sourceBuilder.append(java.lang.String.valueOf(part.columns));
					sourceBuilder.appendln(";");
				}
				hasReturn = hasReturn || part instanceof Return || part instanceof Throw;
				generateParsedSource(sourceBuilder, part, methodPrefix, "baseScope", fileName, addDebugging);
				sourceBuilder.appendln(";");
			}

			if (!hasReturn) {
				sourceBuilder.appendln("return Undefined.INSTANCE;");
			}
		} else if (script.impl.length > 0) {
			boolean needReturn = false;
			for (int i = 0; i < script.impl.length; i++) {
				Parsed part = script.impl[i];
				if (addDebugging && i > 0 && (part.rows > 1 || part.columns > 1)) {
					sourceBuilder.append("stackElement.rows = ");
					sourceBuilder.append(java.lang.String.valueOf(part.rows));
					sourceBuilder.appendln(";");
					sourceBuilder.append("stackElement.columns = ");
					sourceBuilder.append(java.lang.String.valueOf(part.columns));
					sourceBuilder.appendln(";");
				}
				if (i == script.impl.length - 1 && !(part instanceof Throw)) {
					if(!(part instanceof Var))
						sourceBuilder.append("return ");
					else
						needReturn = true;
				}
				generateParsedSource(sourceBuilder, part, methodPrefix, "baseScope", fileName, addDebugging);
				sourceBuilder.appendln(";");
			}
			if(needReturn)
				sourceBuilder.appendln("return Undefined.INSTANCE;");
		} else {
			sourceBuilder.appendln("return Undefined.INSTANCE;");
		}
		sourceBuilder.unindent();
		sourceBuilder.appendln("} finally {");
		sourceBuilder.appendln("\tbaseScope.exit();");
		if (addDebugging)
			sourceBuilder.appendln("\tstackElement.finishCall();");
		sourceBuilder.appendln("}");
		sourceBuilder.unindent();
		sourceBuilder.appendln("}");
		
		for (Function function : script.functions) {
			java.lang.String functionName;
			if(scope != SourceScope.Function)
				sourceBuilder.append("private static final ");
			sourceBuilder.append("class ");
			if(function.uname == null)
				function.uname = toClassName(function.name, false);
			sourceBuilder.append(functionName = function.uname);
			sourceBuilder.appendln(" extends ConstructableFunction {");
			sourceBuilder.indent();
			if(scope != SourceScope.Function)
				sourceBuilder.appendln("private final Global global;");
			sourceBuilder.appendln("private final Scope baseScope;");
			sourceBuilder.appendln("Scope extendScope(BaseObject _this) {");
			sourceBuilder.appendln("\treturn baseScope.extend(_this);");
			sourceBuilder.appendln("}");
			sourceBuilder.append("private ");
			sourceBuilder.append(functionName);
			sourceBuilder.appendln("(Global global, Scope scope) {");
			sourceBuilder.appendln("\tsuper(global);");
			if(scope != SourceScope.Function)
				sourceBuilder.appendln("\tthis.global = global;");
			sourceBuilder.appendln("\tbaseScope = scope;");
			sourceBuilder.appendln("}");

			generateScriptSource(sourceBuilder, function.impl, methodPrefix, fileName, scope == SourceScope.Function ? SourceScope.Function : SourceScope.GlobalFunction, addDebugging);

			sourceBuilder.unindent();
			sourceBuilder.appendln("}");
		}
	}

	protected java.lang.String generateClassSource(ScriptData script, java.lang.String className, java.lang.String fileName, java.lang.String pkg, boolean inFunction, boolean addDebugging) {
		SourceBuilder sourceBuilder = new SourceBuilder();
		sourceBuilder.appendln("package " + pkg + ";");
		sourceBuilder.appendln();
		sourceBuilder.appendln("import net.nexustools.njs.compiler.CompiledScript;");
		sourceBuilder.appendln("import net.nexustools.njs.BaseObject;");
		sourceBuilder.appendln("import net.nexustools.njs.GenericObject;");
		sourceBuilder.appendln("import net.nexustools.njs.GenericArray;");
		sourceBuilder.appendln("import net.nexustools.njs.Arguments;");
		sourceBuilder.appendln("import net.nexustools.njs.BaseFunction;");
		sourceBuilder.appendln("import net.nexustools.njs.ConstructableFunction;");
		sourceBuilder.appendln("import net.nexustools.njs.Undefined;");
		sourceBuilder.appendln("import net.nexustools.njs.JSHelper;");
		sourceBuilder.appendln("import net.nexustools.njs.Global;");
		sourceBuilder.appendln("import net.nexustools.njs.Scope;");
		sourceBuilder.appendln("import net.nexustools.njs.Null;");
		sourceBuilder.appendln();
		sourceBuilder.append("public final class ");
		sourceBuilder.append(className);
		sourceBuilder.append(" extends CompiledScript.");
		sourceBuilder.append(addDebugging ? "Debuggable" : "Optimized");
		sourceBuilder.appendln("{");
		sourceBuilder.indent();

		try {
			generateScriptSource(sourceBuilder, script, script.methodName, fileName, SourceScope.GlobalScript, addDebugging);
		} catch(RuntimeException t) {
			System.err.println(sourceBuilder.toString());
			throw t;
		}

		sourceBuilder.unindent();
		sourceBuilder.appendln("}");

		return sourceBuilder.toString();
	}

	@Override
	protected Script compileScript(ScriptData script, java.lang.String fileName, boolean inFunction) {
		final java.lang.String className;
		if(fileName.endsWith(".js"))
			className = toClassName(fileName.substring(0, fileName.length()-3), true);
		else
			className = toClassName(fileName, true);
		final java.lang.String source = generateClassSource(script, className, fileName, "net.nexustools.njs.gen", inFunction, addDebugging);
		final java.lang.String classPath = "net.nexustools.njs.gen." + className;

		if(DEBUG)
			System.out.println(source);
		
		MemoryClassLoader memoryClassLoader;
		try {
			memoryClassLoader = new MemoryClassLoader(compile(classPath.replace(".", "/") + ".java", source));
		} catch(RuntimeException t) {
			System.err.println(source);
			throw t;
		}
		try {
			return (Script) memoryClassLoader.loadClass("net.nexustools.njs.gen." + className).newInstance();
		} catch (ClassNotFoundException ex) {
			throw new net.nexustools.njs.Error.JavaException("EvalError", ex.toString(), ex);
		} catch (InstantiationException ex) {
			throw new net.nexustools.njs.Error.JavaException("EvalError", ex.toString(), ex);
		} catch (IllegalAccessException ex) {
			throw new net.nexustools.njs.Error.JavaException("EvalError", ex.toString(), ex);
		}
	}

	public Map<java.lang.String, byte[]> compile(java.lang.String fileName, java.lang.String source) {
		return compile(fileName, source, new PrintWriter(System.err), null, null);
	}

	/**
	 * compile given String source and return bytecodes as a Map.
	 *
	 * @param fileName source fileName to be used for error messages etc.
	 * @param source Java source as String
	 * @param err error writer where diagnostic messages are written
	 * @param sourcePath location of additional .java source files
	 * @param classPath location of additional .class files
	 */
	private Map<java.lang.String, byte[]> compile(java.lang.String fileName, java.lang.String source,
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

	private Map<java.lang.String, byte[]> compile(final List<JavaFileObject> compUnits,
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

	public static void main(String... args) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
				super(toURI(fileName), Kind.SOURCE);
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
				super(toURI(name), Kind.CLASS);
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
			Kind kind,
			FileObject sibling) throws IOException {
			if (kind == Kind.CLASS) {
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
