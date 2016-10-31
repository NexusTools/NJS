/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs.compiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class JavaTranspiler extends AbstractCompiler {
	private static final List<java.lang.String> RESTRICTED_NAMES = Arrays.asList(new java.lang.String[]{"new", "public", "private", "static", "protected", "trasient", "try", "catch", "finally", "if", "do", "while", "for", "switch", "case", "default", "enum", "Arguments", "CompiledScript", "Debuggable", "Optimized", "String", "Number", "RegEx", "Global", "Scope", "exec", "call", "multiply", "plus", "divide", "or", "BaseObject", "BaseFunction", "AbstractFunction", "GenericObject", "GenericArray", "CompiledFunction", "JSHelper"});

	private static final Map<java.lang.String, AtomicInteger> GEN_PACKAGE_USED_NAMES = new HashMap();
	private java.lang.String toClassName(java.lang.String name, boolean genPackage) {
		java.lang.String output = name.replaceAll("[^_a-zA-Z0-9\\xA0-\\uFFFF]", "_");
		if(!output.equals(name) || RESTRICTED_NAMES.indexOf(name) > -1)
			output += Math.abs(name.hashCode());
		
		if(genPackage) {
			AtomicInteger atomicInteger;
			synchronized(GEN_PACKAGE_USED_NAMES) {
				atomicInteger = GEN_PACKAGE_USED_NAMES.get(output);
				if(atomicInteger == null)
					GEN_PACKAGE_USED_NAMES.put(output, atomicInteger = new AtomicInteger());
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
	
	private void generateStringSource(SourceBuilder sourceBuilder, Parsed part, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName) {
		if(part instanceof String) {
			sourceBuilder.append("\"");
			sourceBuilder.append(convertStringSource(((String)part).string));
			sourceBuilder.append("\"");
		} else if(part instanceof Plus && ((Plus)part).isStringReferenceChain()) {
			generateStringSource(sourceBuilder, ((Plus)part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(" + ");
			generateStringSource(sourceBuilder, ((Plus)part).rhs, methodPrefix, baseScope, fileName);
		} else
			transpileParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName);
	}

	private void generateMath(SourceBuilder sourceBuilder, Parsed lhs, Parsed rhs, java.lang.String op, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName) {
		if((lhs instanceof StringReferency && !isNumber(lhs)) || (rhs instanceof StringReferency && !isNumber(rhs)) && op.equals("plus")) {
			sourceBuilder.append("global.wrap(");
			generateStringSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(" + ");
			generateStringSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(")");
			return;
		} else if(((lhs instanceof Plus && ((Plus)lhs).isStringReferenceChain()) || (rhs instanceof Plus && ((Plus)rhs).isStringReferenceChain())) && op.equals("plus")) {
			sourceBuilder.append("global.wrap(");
			generateStringSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(" + ");
			generateStringSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(")");
			return;
		} else if(lhs instanceof NumberReferency) {
			transpileParsedSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(".");
			sourceBuilder.append(op);
			sourceBuilder.append("(");

			if(rhs instanceof NumberReferency)
				transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName);
			else {
				sourceBuilder.append("global.Number.fromValueOf(");
				transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName);
				sourceBuilder.append(")");
			}

			sourceBuilder.append(")");
			return;
		}
		
		sourceBuilder.append(op);
		sourceBuilder.append("(global, ");
		transpileParsedSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName);
		sourceBuilder.append(", ");
		transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName);
		sourceBuilder.append(")");
	}

	private void generateIfBlockSource(SourceBuilder sourceBuilder, Else els, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName) {
		while(els != null) {
			if(els.simpleimpl != null) {
				if(els instanceof ElseIf) {
					sourceBuilder.append("else if(");
					generateBooleanSource(sourceBuilder, els.condition, methodPrefix, baseScope, fileName);
					sourceBuilder.appendln(")");
				} else
					sourceBuilder.appendln("else");
				sourceBuilder.indent();
				transpileParsedSource(sourceBuilder, els.simpleimpl, methodPrefix, baseScope, fileName, true);
				sourceBuilder.unindent();
			} else{
				if(els instanceof ElseIf) {
					sourceBuilder.append("else if(");
					generateBooleanSource(sourceBuilder, els.condition, methodPrefix, baseScope, fileName);
					sourceBuilder.appendln(") {");
				} else
					sourceBuilder.appendln("else {");
				sourceBuilder.indent();
				generateBlockSource(sourceBuilder, els.impl, methodPrefix, baseScope, fileName);
				sourceBuilder.unindent();
				sourceBuilder.appendln("}");
			}

			if(els instanceof ElseIf)
				els = ((ElseIf)els).el;
			else
				break;
		}
	}

	private void generateBooleanSource(SourceBuilder sourceBuilder, Parsed part, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName) {
		while(part instanceof OpenBracket)
			part = ((OpenBracket)part).contents;
		
		if(part instanceof Boolean) {
			if(((Boolean)part).value)
				sourceBuilder.append("true");
			else
				sourceBuilder.append("false");
		} else if(part instanceof InstanceOf) {
			transpileParsedSource(sourceBuilder, ((InstanceOf)part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(".instanceOf((BaseFunction)");
			transpileParsedSource(sourceBuilder, ((InstanceOf)part).rhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(")");
		} else if(part instanceof Not) {
			sourceBuilder.append("!");
			generateBooleanSource(sourceBuilder, ((Not) part).rhs, methodPrefix, baseScope, fileName);
		} else if(part instanceof Equals) {
			transpileParsedSource(sourceBuilder, ((Equals) part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(".equals(");
			transpileParsedSource(sourceBuilder, ((Equals) part).rhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(")");
		} else if(part instanceof OrOr) {
			transpileParsedSource(sourceBuilder, ((OrOr) part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(" || ");
			generateBooleanSource(sourceBuilder, ((OrOr) part).rhs, methodPrefix, baseScope, fileName);
		} else if(part instanceof AndAnd) {
			generateBooleanSource(sourceBuilder, ((AndAnd) part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(" && ");
			generateBooleanSource(sourceBuilder, ((AndAnd) part).rhs, methodPrefix, baseScope, fileName);
		} else if(part instanceof NotEquals) {
			sourceBuilder.append("(!");
			transpileParsedSource(sourceBuilder, ((NotEquals) part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(".equals(");
			transpileParsedSource(sourceBuilder, ((NotEquals) part).rhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append("))");
		} else if(part instanceof StrictEquals) {
			sourceBuilder.append("(BaseObject)");
			transpileParsedSource(sourceBuilder, ((StrictEquals) part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(" == (BaseObject)");
			transpileParsedSource(sourceBuilder, ((StrictEquals) part).rhs, methodPrefix, baseScope, fileName);
		} else if(part instanceof StrictNotEquals) {
			transpileParsedSource(sourceBuilder, ((StrictNotEquals) part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(" != ");
			transpileParsedSource(sourceBuilder, ((StrictNotEquals) part).rhs, methodPrefix, baseScope, fileName);
		} else if(part instanceof MoreThan) {
			sourceBuilder.append("moreThan(global, ");
			transpileParsedSource(sourceBuilder, ((MoreThan) part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(", ");
			transpileParsedSource(sourceBuilder, ((MoreThan) part).rhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(")");
		} else if(part instanceof LessThan) {
			sourceBuilder.append("lessThan(global, ");
			transpileParsedSource(sourceBuilder, ((LessThan) part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(", ");
			transpileParsedSource(sourceBuilder, ((LessThan) part).rhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(")");
		} else if(part instanceof MoreEqual) {
			sourceBuilder.append("moreEqual(global, ");
			transpileParsedSource(sourceBuilder, ((MoreEqual) part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(", ");
			transpileParsedSource(sourceBuilder, ((MoreEqual) part).rhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(")");
		} else if(part instanceof LessEqual) {
			sourceBuilder.append("lessEqual(global, ");
			transpileParsedSource(sourceBuilder, ((LessEqual) part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(", ");
			transpileParsedSource(sourceBuilder, ((LessEqual) part).rhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(")");
		} else if(part instanceof Delete) {
			Parsed rhs = ((Delete)part).rhs;
			if(rhs instanceof Reference) {
				sourceBuilder.append(baseScope);
				sourceBuilder.append(".delete(\"");
				sourceBuilder.append(convertStringSource(((Reference)rhs).ref));
				sourceBuilder.append("\")");
				return;
			} else if(rhs instanceof ReferenceChain) {
				java.lang.String first = ((ReferenceChain)rhs).chain.remove(0);
				java.lang.String last = ((ReferenceChain)rhs).chain.remove(((ReferenceChain)rhs).chain.size()-1);
				
				sourceBuilder.append(baseScope);
				sourceBuilder.append(".get(\"");
				sourceBuilder.append(convertStringSource(first));
				sourceBuilder.append("\")");
				for(java.lang.String key : ((ReferenceChain)rhs).chain) {
					sourceBuilder.append(".get(\"");
					sourceBuilder.append(convertStringSource(key));
					sourceBuilder.append("\")");
				}
				sourceBuilder.append(".delete(\"");
				sourceBuilder.append(convertStringSource(last));
				sourceBuilder.append("\")");
				return;
			}

			throw new UnsupportedOperationException("Cannot compile delete : " + describe(rhs));
		} else {
			sourceBuilder.append("JSHelper.isTrue(");
			transpileParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName);
			sourceBuilder.append(")");
		}
	}

	private void generateStringNumberIndex(SourceBuilder sourceBuilder, java.lang.String ref) {
		try {
			if(ref.endsWith(".0"))
				ref = ref.substring(0, ref.length()-2);
			if(java.lang.Integer.valueOf(ref) < 0)
				throw new NumberFormatException();
			sourceBuilder.append(ref);
		} catch(NumberFormatException ex) {
			sourceBuilder.append("\"");
			sourceBuilder.append(convertStringSource(ref));
			sourceBuilder.append("\"");
		}
	}

	private boolean isNumber(Parsed lhs) {
		if(lhs instanceof String)
			try {
				Double.valueOf(((String)lhs).string);
				return true;
			} catch(NumberFormatException ex) {}
		return lhs instanceof Number || lhs instanceof Integer;
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
			if(Character.isWhitespace(builder.charAt(builder.length() - 1)))
				builder.append('\t');
		}

		public void unindent() {
			int pos = builder.length() - 1;
			indent = indent.substring(0, indent.length() - 1);
			if(Character.isWhitespace(builder.charAt(pos)))
				builder.deleteCharAt(pos);
		}

		@Override
		public java.lang.String toString() {
			return builder.toString(); //To change body of generated methods, choose Tools | Templates.
		}
	}

	private static final javax.tools.JavaCompiler JAVA_COMPILER;
	private static final StandardJavaFileManager STANDARD_FILE_MANAGER;

	static {
		if(System.getProperties().containsKey("NJSNOCOMPILING"))
			throw new RuntimeException("NJSNOCOMPILING");
		JAVA_COMPILER = ToolProvider.getSystemJavaCompiler();
		if (JAVA_COMPILER == null)
			throw new RuntimeException("Could not get Java compiler. Please, ensure that JDK is used instead of JRE.");
		STANDARD_FILE_MANAGER = JAVA_COMPILER.getStandardFileManager(null, null, null);
		assert ((new JavaTranspiler().compile("(function munchkin(){\n\tfunction yellow(){\n\t\treturn 55;\n\t}\n\treturn yellow()\n\t})()", "JavaCompilerStaticTest", false)).exec(new Global(), null).toString().equals("55"));
	}

	private static enum SourceState {
		GlobalScript,
		FunctionScript,
		Function
	}
	public final boolean addDebugging;
	public JavaTranspiler() {
		this(true);
	}
	public JavaTranspiler(boolean addDebugging) {
		this.addDebugging = addDebugging;
	}
	
	protected void generateBlockSource(SourceBuilder sourceBuilder, ScriptData blockDat, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName) {
		for(Parsed part : blockDat.impl) {
			if (addDebugging && (part.rows > 1 || part.columns > 1)) {
				sourceBuilder.append("stackElement.rows = ");
				sourceBuilder.append(java.lang.String.valueOf(part.rows));
				sourceBuilder.appendln(";");
				sourceBuilder.append("stackElement.columns = ");
				sourceBuilder.append(java.lang.String.valueOf(part.columns));
				sourceBuilder.appendln(";");
			}
			transpileParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName, true);
			sourceBuilder.appendln(";");
		}
	}
		
	protected void transpileParsedSource(SourceBuilder sourceBuilder, Parsed part, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName) {
		transpileParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName, false);
	}
	protected void transpileParsedSource(SourceBuilder sourceBuilder, Parsed part, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, boolean atTop) {
		while(part instanceof OpenBracket)
			part = ((OpenBracket)part).contents;
		
		if (part instanceof Return) {
			sourceBuilder.append("return ");
			transpileParsedSource(sourceBuilder, ((Return) part).rhs, methodPrefix, baseScope, fileName);
			return;
		} else if(part instanceof TypeOf) {
			Parsed rhs = ((TypeOf)part).rhs;
			sourceBuilder.append("global.wrap(");
			transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(".typeOf())");
			return;
		} else if (part instanceof Call) {
			Parsed reference = ((Call)part).reference;
			while(reference instanceof OpenBracket) // unwrap
				reference = ((OpenBracket)reference).contents;
			
			if(reference instanceof Referency && !(reference instanceof Reference || reference instanceof Call)) {
				final java.lang.String source = reference.toSimpleSource();
				if(reference instanceof RightReference) {
					final java.lang.String key = ((RightReference)reference).chain.remove(((RightReference)reference).chain.size()-1);
					sourceBuilder.append("callTop(");
					if (addDebugging) {
						sourceBuilder.append("\"");
						sourceBuilder.append(convertStringSource(source));
						sourceBuilder.append("\", ");
					}
					sourceBuilder.append("\"");
					sourceBuilder.append(convertStringSource(key));
					sourceBuilder.append("\", ");
					transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName);
					for (Parsed arg : ((Call) part).arguments) {
						sourceBuilder.append(", ");
						transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName);
					}
					sourceBuilder.append(")");
					return;
				} else if(reference instanceof IntegerReference) {
					final int key = ((IntegerReference)reference).ref;
					sourceBuilder.append("callTop(");
					if (addDebugging) {
						sourceBuilder.append("\"");
						sourceBuilder.append(convertStringSource(source));
						sourceBuilder.append("\", ");
					}
					sourceBuilder.append(java.lang.String.valueOf(key));
					sourceBuilder.append(", ");
					transpileParsedSource(sourceBuilder, ((IntegerReference)reference).lhs, methodPrefix, baseScope, fileName);
					for (Parsed arg : ((Call) part).arguments) {
						sourceBuilder.append(", ");
						transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName);
					}
					sourceBuilder.append(")");
					return;
				} else if(reference instanceof ReferenceChain) {
					final java.lang.String key = ((ReferenceChain)reference).chain.remove(((ReferenceChain)reference).chain.size()-1);
					sourceBuilder.append("callTop(");
					if (addDebugging) {
						sourceBuilder.append("\"");
						sourceBuilder.append(convertStringSource(source));
						sourceBuilder.append("\", ");
					}
					sourceBuilder.append("\"");
					sourceBuilder.append(convertStringSource(key));
					sourceBuilder.append("\", ");
					transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName);
					for (Parsed arg : ((Call) part).arguments) {
						sourceBuilder.append(", ");
						transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName);
					}
					sourceBuilder.append(")");
					return;
				} else if(reference instanceof New) {
					sourceBuilder.append("callNew(");
					if (addDebugging) {
						sourceBuilder.append("\"");
						sourceBuilder.append(convertStringSource(source));
						sourceBuilder.append("\", ");
					}
					transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName);
					for (Parsed arg : ((Call) part).arguments) {
						sourceBuilder.append(", ");
						transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName);
					}
					sourceBuilder.append(")");
					return;
				}
				
				throw new UnsupportedOperationException("Cannot compile call: " + describe(reference));
			}
			
			if(addDebugging) {
				sourceBuilder.append("callTop(");
				sourceBuilder.append("\"");
				sourceBuilder.append(convertStringSource(reference.toSimpleSource()));
				sourceBuilder.append("\", ");
				transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName);
				sourceBuilder.append(", ");
				sourceBuilder.append("Undefined.INSTANCE");
				for (Parsed arg : ((Call) part).arguments) {
					sourceBuilder.append(", ");
					transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName);
				}
				sourceBuilder.append(")");
			} else {
				sourceBuilder.append("((BaseFunction)");
				transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName);
				sourceBuilder.append(").call(");
				sourceBuilder.append("Undefined.INSTANCE");
				for (Parsed arg : ((Call) part).arguments) {
					sourceBuilder.append(", ");
					transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName);
				}
				sourceBuilder.append(")");
			}
			return;
		} else if(part instanceof InstanceOf) {
			sourceBuilder.append("(");
			transpileParsedSource(sourceBuilder, ((InstanceOf)part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(".instanceOf((BaseFunction)");
			transpileParsedSource(sourceBuilder, ((InstanceOf)part).rhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
			return;
		} else if (part instanceof Number) {
			double value = ((Number) part).value;
			if(value == 0)
				sourceBuilder.append("global.Zero");
			else if(value == 1)
				sourceBuilder.append("global.PositiveOne");
			else if(value == -1)
				sourceBuilder.append("global.NegativeOne");
			else {
				sourceBuilder.append("global.wrap(");
				sourceBuilder.append(java.lang.String.valueOf(value));
				sourceBuilder.append(")");
			}
			return;
		} else if (part instanceof Integer) {
			int value = ((Integer) part).value;
			switch(value) {
				case 0:
					sourceBuilder.append("global.Zero");
					break;
				case 1:
					sourceBuilder.append("global.PositiveOne");
					break;
				case -1:
					sourceBuilder.append("global.NegativeOne");
					break;
				default:
					sourceBuilder.append("global.wrap(");
					sourceBuilder.append(java.lang.String.valueOf(value));
					sourceBuilder.append(")");
			}
			return;
		} else if (part instanceof String) {
			sourceBuilder.append("global.wrap(\"");
			sourceBuilder.append(convertStringSource(java.lang.String.valueOf(((String) part).string)));
			sourceBuilder.append("\")");
			return;
		} else if (part instanceof Reference) {
			sourceBuilder.append(baseScope);
			sourceBuilder.append(".get(\"");
			sourceBuilder.append(convertStringSource(((Reference) part).ref));
			sourceBuilder.append("\")");
			return;
		} else if (part instanceof Plus) {
			Parsed lhs = ((Plus)part).lhs;
			Parsed rhs = ((Plus)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, "plus", methodPrefix, baseScope, fileName);
			return;
		} else if (part instanceof Multiply) {
			Parsed lhs = ((Multiply)part).lhs;
			Parsed rhs = ((Multiply)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, "multiply", methodPrefix, baseScope, fileName);
			return;
		} else if (part instanceof Divide) {
			Parsed lhs = ((Divide)part).lhs;
			Parsed rhs = ((Divide)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, "divide", methodPrefix, baseScope, fileName);
			return;
		} else if (part instanceof And) {
			Parsed lhs = ((And)part).lhs;
			Parsed rhs = ((And)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, "and", methodPrefix, baseScope, fileName);
			return;
		} else if (part instanceof Or) {
			Parsed lhs = ((Or)part).lhs;
			Parsed rhs = ((Or)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, "or", methodPrefix, baseScope, fileName);
			return;
		} else if (part instanceof Percent) {
			Parsed lhs = ((Percent)part).lhs;
			Parsed rhs = ((Percent)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, "percent", methodPrefix, baseScope, fileName);
			return;
		} else if (part instanceof Minus) {
			Parsed lhs = ((Minus)part).lhs;
			Parsed rhs = ((Minus)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, "minus", methodPrefix, baseScope, fileName);
			return;
		} else if (part instanceof New) {
			boolean addComma;
			if(addDebugging) {
				addComma = true;
				sourceBuilder.append("constructTop(\"");
				sourceBuilder.append(convertStringSource(((New)part).reference.toSimpleSource()));
				sourceBuilder.append("\", ");
				transpileParsedSource(sourceBuilder, ((New) part).reference, methodPrefix, baseScope, fileName);
			} else {
				addComma = false;
				sourceBuilder.append("((BaseFunction)");
				transpileParsedSource(sourceBuilder, ((New) part).reference, methodPrefix, baseScope, fileName);
				sourceBuilder.append(").construct(");
			}
			if (((New) part).arguments != null) {
				for (Parsed arg : ((New) part).arguments) {
					if (addComma)
						sourceBuilder.append(", ");
					else
						addComma = true;
					transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName);
				}
			}
			sourceBuilder.append(")");
			return;
		} else if(part instanceof RightReference) {
			transpileParsedSource(sourceBuilder, ((RightReference) part).ref, methodPrefix, baseScope, fileName);
			for(java.lang.String key : ((RightReference)part).chain) {
				sourceBuilder.append(".get(");
				generateStringNumberIndex(sourceBuilder, key);
				sourceBuilder.append(")");
			}
			return;
		} else if(part instanceof Throw) {
			sourceBuilder.append("throw new net.nexustools.njs.Error.Thrown(");
			transpileParsedSource(sourceBuilder, ((Throw) part).rhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(")");
			return;
		} else if(part instanceof Function) {
			sourceBuilder.appendln("new CompiledFunction(global) {");
			sourceBuilder.indent();
			sourceBuilder.appendln("Scope extendScope(BaseObject _this) {");
			sourceBuilder.append("\treturn ");
			sourceBuilder.append(baseScope);
			sourceBuilder.appendln(".extend(_this);");
			sourceBuilder.appendln("}");
			((Function)part).impl.callee = ((Function)part);
			generateScriptSource(sourceBuilder, ((Function)part).impl, methodPrefix, fileName, SourceScope.Function);
			sourceBuilder.unindent();
			sourceBuilder.append("}");
			return;
		} else if(part instanceof Var) {
			List<Var.Set> sets = ((Var)part).sets;
			if(sets.size() > 1) {
				java.lang.Boolean first = true;
				for(Var.Set set : sets) {
					if(first)
						first = false;
					else
						sourceBuilder.appendln(";");
					sourceBuilder.append(baseScope);
					sourceBuilder.append(".var(\"");
					sourceBuilder.append(convertStringSource(set.lhs));
					sourceBuilder.append("\", ");
					if(set.rhs == null)
						sourceBuilder.append("Undefined.INSTANCE");
					else
						transpileParsedSource(sourceBuilder, set.rhs, methodPrefix, baseScope, fileName);
					sourceBuilder.append(")");
				}
				return;
			}
			Var.Set set = sets.get(0);
			sourceBuilder.append(baseScope);
			sourceBuilder.append(".var(\"");
			sourceBuilder.append(convertStringSource(set.lhs));
			sourceBuilder.append("\", ");
			if(set.rhs == null)
				sourceBuilder.append("Undefined.INSTANCE");
			else
				transpileParsedSource(sourceBuilder, set.rhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(")");
			return;
		} else if(part instanceof OrOr) {
			sourceBuilder.append("orOr(");
			transpileParsedSource(sourceBuilder, ((OrOr) part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(", ");
			transpileParsedSource(sourceBuilder, ((OrOr) part).rhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(")");
			return;
		} else if(part instanceof AndAnd) {
			sourceBuilder.append("(andAnd(");
			transpileParsedSource(sourceBuilder, ((AndAnd) part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(", ");
			transpileParsedSource(sourceBuilder, ((AndAnd) part).rhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
			return;
		} else if(part instanceof Equals) {
			sourceBuilder.append("(");
			transpileParsedSource(sourceBuilder, ((Equals) part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(".equals(");
			transpileParsedSource(sourceBuilder, ((Equals) part).rhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
			return;
		} else if(part instanceof NotEquals) {
			sourceBuilder.append("(");
			transpileParsedSource(sourceBuilder, ((NotEquals) part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(".equals(");
			transpileParsedSource(sourceBuilder, ((NotEquals) part).rhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(") ? global.Boolean.FALSE : global.Boolean.TRUE)");
			return;
		} else if(part instanceof StrictEquals) {
			sourceBuilder.append("(((BaseObject)");
			transpileParsedSource(sourceBuilder, ((StrictEquals) part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(" == (BaseObject)");
			transpileParsedSource(sourceBuilder, ((StrictEquals) part).rhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
			return;
		} else if(part instanceof StrictNotEquals) {
			sourceBuilder.append("(((BaseObject)");
			transpileParsedSource(sourceBuilder, ((StrictNotEquals) part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(" == (BaseObject)");
			transpileParsedSource(sourceBuilder, ((StrictNotEquals) part).rhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(") ? global.Boolean.FALSE : global.Boolean.TRUE)");
			return;
		} else if(part instanceof ReferenceChain) {
			java.lang.String first = ((ReferenceChain)part).chain.remove(0);
			sourceBuilder.append(baseScope);
			sourceBuilder.append(".get(\"");
			sourceBuilder.append(convertStringSource(first));
			sourceBuilder.append("\")");
			for(java.lang.String ref : ((ReferenceChain)part).chain) {
				sourceBuilder.append(".get(");
				generateStringNumberIndex(sourceBuilder, ref);
				sourceBuilder.append(")");
			}
			return;
		} else if(part instanceof IntegerReference) {
			int key = ((IntegerReference)part).ref;
			if(((IntegerReference) part).lhs == null) {
				sourceBuilder.append("new GenericArray(global, new BaseObject[]{");
				switch(key){
					case 0:
						sourceBuilder.append("global.Zero");
						break;
					case 1:
						sourceBuilder.append("global.One");
						break;
					default:
						sourceBuilder.append("global.wrap(");
						sourceBuilder.append(java.lang.String.valueOf(key));
						sourceBuilder.append(")");
				}
				sourceBuilder.append("})");
				return;
			}
			
			transpileParsedSource(sourceBuilder, ((IntegerReference) part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(".get(");
			sourceBuilder.append(java.lang.String.valueOf(key));
			sourceBuilder.append(")");
			return;
		} else if(part instanceof Not) {
			sourceBuilder.append("global.wrap(!");
			generateBooleanSource(sourceBuilder, ((Not) part).rhs, methodPrefix, baseScope, fileName);
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
				transpileParsedSource(sourceBuilder, subpart, methodPrefix, baseScope, fileName);
			}
			sourceBuilder.append("})");
			return;
		} else if(part instanceof Set) {
			Parsed lhs = ((Set)part).lhs;
			Parsed rhs = ((Set)part).rhs;
			if(lhs instanceof IntegerReference) {
				sourceBuilder.append("callSet(");
				transpileParsedSource(sourceBuilder, ((IntegerReference)lhs).lhs, methodPrefix, baseScope, fileName);
				sourceBuilder.append(", ");
				sourceBuilder.append(java.lang.String.valueOf(((IntegerReference)lhs).ref));
				sourceBuilder.append(", ");
				transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName);
				sourceBuilder.append(")");
				return;
			} else if(lhs instanceof VariableReference) {
				sourceBuilder.append("JSHelper.set(");
				transpileParsedSource(sourceBuilder, ((VariableReference)lhs).lhs, methodPrefix, baseScope, fileName);
				sourceBuilder.append(", ");
				transpileParsedSource(sourceBuilder, ((VariableReference)lhs).ref, methodPrefix, baseScope, fileName);
				sourceBuilder.append(", ");
				transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName);
				sourceBuilder.append(")");
				return;
			} else if(lhs instanceof Reference) {
				sourceBuilder.append("callSet(");
				sourceBuilder.append(baseScope);
				sourceBuilder.append(", \"");
				sourceBuilder.append(convertStringSource(((Reference)lhs).ref));
				sourceBuilder.append("\", ");
				transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName);
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
					sourceBuilder.append(convertStringSource(first));
					sourceBuilder.append("\")");
					for(java.lang.String k : chain) {
						sourceBuilder.append(".get(");
						generateStringNumberIndex(sourceBuilder, k);
						sourceBuilder.append(")");
					}
					sourceBuilder.append(", \"");
					sourceBuilder.append(convertStringSource(key));
					sourceBuilder.append("\", ");
					transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName);
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
				sourceBuilder.appendln("try {");
				sourceBuilder.indent();
				generateBlockSource(sourceBuilder, ((Try)part).impl, methodPrefix, baseScope, fileName);
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
				sourceBuilder.append(convertStringSource(((Reference)c.condition).ref));
				sourceBuilder.appendln("\", global.wrap(t));");
				generateBlockSource(sourceBuilder, c.impl, methodPrefix, newScope, fileName);
				sourceBuilder.unindent();
				sourceBuilder.appendln("} finally {");
				sourceBuilder.append("\t");
				sourceBuilder.append(newScope);
				sourceBuilder.appendln(".exit();");
				sourceBuilder.appendln("}");
				sourceBuilder.unindent();
				sourceBuilder.appendln("} finally {");
				sourceBuilder.indent();
				generateBlockSource(sourceBuilder, f.impl, methodPrefix, baseScope, fileName);
				sourceBuilder.unindent();
				sourceBuilder.appendln("}");
				return;
			} else if(c != null) {
				sourceBuilder.appendln("try {");
				sourceBuilder.indent();
				generateBlockSource(sourceBuilder, ((Try)part).impl, methodPrefix, baseScope, fileName);
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
				sourceBuilder.append(convertStringSource(((Reference)c.condition).ref));
				sourceBuilder.appendln("\", global.wrap(t));");
				generateBlockSource(sourceBuilder, c.impl, methodPrefix, newScope, fileName);
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
			
			sourceBuilder.appendln("try {");
			sourceBuilder.indent();
			generateBlockSource(sourceBuilder, ((Try)part).impl, methodPrefix, baseScope, fileName);
			sourceBuilder.unindent();
			sourceBuilder.appendln("} finally {");
			sourceBuilder.indent();
			generateBlockSource(sourceBuilder, f.impl, methodPrefix, baseScope, fileName);
			sourceBuilder.unindent();
			sourceBuilder.appendln("}");
			return;
		} else if(part instanceof If) {
			if(((If)part).simpleimpl != null) {
				sourceBuilder.append("if(");
				generateBooleanSource(sourceBuilder, ((If)part).condition, methodPrefix, baseScope, fileName);
				sourceBuilder.appendln(")");
				sourceBuilder.indent();
				transpileParsedSource(sourceBuilder, ((If)part).simpleimpl, methodPrefix, baseScope, fileName, true);
				if(((If)part).el != null)
					sourceBuilder.appendln(";");
				else
					sourceBuilder.appendln();
				sourceBuilder.unindent();
				
				generateIfBlockSource(sourceBuilder, ((If)part).el, methodPrefix, baseScope, fileName);
				return;
			}

			sourceBuilder.append("if(");
			generateBooleanSource(sourceBuilder, ((If)part).condition, methodPrefix, baseScope, fileName);
			sourceBuilder.appendln(") {");
			sourceBuilder.indent();
			generateBlockSource(sourceBuilder, ((If)part).impl, methodPrefix, baseScope, fileName);
			sourceBuilder.unindent();
			sourceBuilder.appendln("}");

			generateIfBlockSource(sourceBuilder, ((If)part).el, methodPrefix, baseScope, fileName);
			return;
		} else if(part instanceof While) {
			if(((While)part).simpleimpl != null) {
				sourceBuilder.append("while(");
				generateBooleanSource(sourceBuilder, ((While)part).condition, methodPrefix, baseScope, fileName);
				sourceBuilder.appendln(")");
				sourceBuilder.append("\t");
				transpileParsedSource(sourceBuilder, ((While)part).simpleimpl, methodPrefix, baseScope, fileName, true);
				return;
			}

			
			sourceBuilder.append("while(");
			generateBooleanSource(sourceBuilder, ((While)part).condition, methodPrefix, baseScope, fileName);
			sourceBuilder.appendln(") {");
			sourceBuilder.indent();
			generateBlockSource(sourceBuilder, ((While)part).impl, methodPrefix, baseScope, fileName);
			sourceBuilder.unindent();
			sourceBuilder.appendln("}");
			return;
		} else if(part instanceof For) {
			if(((For)part).simpleimpl != null) {
				if(((For)part).init != null) {
					transpileParsedSource(sourceBuilder, ((For)part).init, methodPrefix, baseScope, fileName);
					sourceBuilder.appendln(";");
				}
				sourceBuilder.append("for(; ");
				generateBooleanSource(sourceBuilder, ((For)part).condition, methodPrefix, baseScope, fileName);
				sourceBuilder.append("; ");
				if(((For)part).loop != null)
					transpileParsedSource(sourceBuilder, ((For)part).loop, methodPrefix, baseScope, fileName);
				sourceBuilder.appendln(")");
				sourceBuilder.append("\t");
				transpileParsedSource(sourceBuilder, ((For)part).simpleimpl, methodPrefix, baseScope, fileName, true);
				return;
			}

			if(((For)part).init != null) {
				transpileParsedSource(sourceBuilder, ((For)part).init, methodPrefix, baseScope, fileName);
				sourceBuilder.appendln(";");
			}
			sourceBuilder.append("for(; ");
			generateBooleanSource(sourceBuilder, ((For)part).condition, methodPrefix, baseScope, fileName);
			sourceBuilder.append("; ");
			if(((For)part).loop != null)
				transpileParsedSource(sourceBuilder, ((For)part).loop, methodPrefix, baseScope, fileName);
			sourceBuilder.appendln(") {");
			sourceBuilder.indent();
			generateBlockSource(sourceBuilder, ((For)part).impl, methodPrefix, baseScope, fileName);
			sourceBuilder.unindent();
			sourceBuilder.appendln("}");
			return;
		} else if(part instanceof PlusPlus) {
			Parsed ref = ((PlusPlus)part).ref;
			if(((PlusPlus)part).right) {
				sourceBuilder.append("plusPlusRight(global, ");
				if(ref instanceof Reference) {
					sourceBuilder.append("\"");
					sourceBuilder.append(convertStringSource(((Reference) ref).ref));
					sourceBuilder.append("\", ");
					sourceBuilder.append(baseScope);
				} else
					throw new UnsupportedOperationException("Cannot compile x++: " + describe(ref));
				sourceBuilder.append(")");
				return;
			}
			
			sourceBuilder.append("plusPlusLeft(global, ");
			if(ref instanceof Reference) {
				sourceBuilder.append("\"");
				sourceBuilder.append(convertStringSource(((Reference) ref).ref));
				sourceBuilder.append("\", ");
				sourceBuilder.append(baseScope);
			} else
				throw new UnsupportedOperationException("Cannot compile ++x: " + describe(ref));
			sourceBuilder.append(")");
			return;
		} else if(part instanceof MinusMinus) {
			Parsed ref = ((MinusMinus)part).ref;
			if(((MinusMinus)part).right) {
				sourceBuilder.append("minusMinusRight(global, ");
				if(ref instanceof Reference) {
					sourceBuilder.append("\"");
					sourceBuilder.append(convertStringSource(((Reference) ref).ref));
					sourceBuilder.append("\", ");
					sourceBuilder.append(baseScope);
				} else
					throw new UnsupportedOperationException("Cannot compile x++: " + describe(ref));
				sourceBuilder.append(")");
				return;
			}
			
			sourceBuilder.append("minusMinusLeft(global, ");
			if(ref instanceof Reference) {
				sourceBuilder.append("\"");
				sourceBuilder.append(convertStringSource(((Reference) ref).ref));
				sourceBuilder.append("\", ");
				sourceBuilder.append(baseScope);
			} else
				throw new UnsupportedOperationException("Cannot compile ++x: " + describe(ref));
			sourceBuilder.append(")");
			return;
		} else if(part instanceof OpenGroup) {
			sourceBuilder.appendln("new GenericObject(global) {");
			sourceBuilder.indent();
			sourceBuilder.appendln("{");
			sourceBuilder.indent();
			for(Map.Entry<java.lang.String, Parsed> entry : ((OpenGroup)part).entries.entrySet()) {
				sourceBuilder.append("set(\"");
				sourceBuilder.append(convertStringSource(entry.getKey()));
				sourceBuilder.append("\", ");
				transpileParsedSource(sourceBuilder, entry.getValue(), methodPrefix, baseScope, fileName);
				sourceBuilder.appendln(");");
			}
			sourceBuilder.unindent();
			sourceBuilder.appendln("}");
			sourceBuilder.unindent();
			sourceBuilder.appendln("}");
			return;
		} else if(part instanceof Delete) {
			if(atTop) {
				generateBooleanSource(sourceBuilder, part, methodPrefix, baseScope, fileName);
				return;
			}
			
			Parsed rhs = ((Delete)part).rhs;
			if(rhs instanceof Reference) {
				sourceBuilder.append("(");
				sourceBuilder.append(baseScope);
				sourceBuilder.append(".delete(\"");
				sourceBuilder.append(convertStringSource(((Reference)rhs).ref));
				sourceBuilder.append("\") ? global.Boolean.TRUE : global.Boolean.FALSE)");
				return;
			} else if(rhs instanceof ReferenceChain) {
				java.lang.String first = ((ReferenceChain)rhs).chain.remove(0);
				java.lang.String last = ((ReferenceChain)rhs).chain.remove(((ReferenceChain)rhs).chain.size()-1);
				
				sourceBuilder.append("(");
				sourceBuilder.append(baseScope);
				sourceBuilder.append(".get(\"");
				sourceBuilder.append(convertStringSource(first));
				sourceBuilder.append("\")");
				for(java.lang.String key : ((ReferenceChain)rhs).chain) {
					sourceBuilder.append(".get(\"");
					sourceBuilder.append(convertStringSource(key));
					sourceBuilder.append("\")");
				}
				sourceBuilder.append(".delete(\"");
				sourceBuilder.append(convertStringSource(last));
				sourceBuilder.append("\") ? global.Boolean.TRUE : global.Boolean.FALSE)");
				return;
			}

			throw new UnsupportedOperationException("Cannot compile delete : " + describe(rhs));
		} else if(part instanceof MoreThan) {
			sourceBuilder.append("(moreThan(global, ");
			transpileParsedSource(sourceBuilder, ((MoreThan) part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(", ");
			transpileParsedSource(sourceBuilder, ((MoreThan) part).rhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
			return;
		} else if(part instanceof LessThan) {
			sourceBuilder.append("(lessThan(global, ");
			transpileParsedSource(sourceBuilder, ((LessThan) part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(", ");
			transpileParsedSource(sourceBuilder, ((LessThan) part).rhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
			return;
		} else if(part instanceof MoreEqual) {
			sourceBuilder.append("(moreEqual(global, ");
			transpileParsedSource(sourceBuilder, ((MoreEqual) part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(", ");
			transpileParsedSource(sourceBuilder, ((MoreEqual) part).rhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
			return;
		} else if(part instanceof LessEqual) {
			sourceBuilder.append("(lessEqual(global, ");
			transpileParsedSource(sourceBuilder, ((LessEqual) part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(", ");
			transpileParsedSource(sourceBuilder, ((LessEqual) part).rhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
			return;
		} else if(part instanceof VariableReference) {
			sourceBuilder.append("JSHelper.get(");
			transpileParsedSource(sourceBuilder, ((VariableReference)part).lhs, methodPrefix, baseScope, fileName);
			sourceBuilder.append(", ");
			transpileParsedSource(sourceBuilder, ((VariableReference)part).ref, methodPrefix, baseScope, fileName);
			sourceBuilder.append(")");
			return;
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
	protected void generateScriptSource(SourceBuilder sourceBuilder, ScriptData script, java.lang.String methodPrefix, java.lang.String fileName, SourceScope scope) {
		if(addDebugging || !scope.isFunction()) {
			sourceBuilder.appendln("@Override");
			sourceBuilder.appendln("public String source() {");
			sourceBuilder.append("\treturn \"");
			if (addDebugging)
				sourceBuilder.append(convertStringSource(script.source));
			else
				sourceBuilder.append("[java_code]");
			sourceBuilder.appendln("\";");
			sourceBuilder.appendln("}");
		}
		if (scope == SourceScope.Function) {
			sourceBuilder.appendln("@Override");
			sourceBuilder.appendln("public String name() {");
			sourceBuilder.append("\treturn \"");
			if(script.callee != null)
				sourceBuilder.append(script.callee.name != null ? convertStringSource(script.callee.name) : "<anonymous>");
			else
				sourceBuilder.append(script.methodName != null ? convertStringSource(script.methodName) : "<anonymous>");
			sourceBuilder.appendln("\";");
			sourceBuilder.appendln("}");
			
			sourceBuilder.appendln("@Override");
			sourceBuilder.appendln("@SuppressWarnings(\"all\")");
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
						sourceBuilder.append("\t\t");
						if(i == arguments.size())
							sourceBuilder.append("default");
						else {
							sourceBuilder.append("case ");
							sourceBuilder.append(java.lang.String.valueOf(i));
						}
						sourceBuilder.appendln(":");
						for(; a < i; a++) {
							sourceBuilder.append("\t\t\tbaseScope.var(\"");
							sourceBuilder.append(convertStringSource(arguments.get(a)));
							sourceBuilder.append("\", params[");
							sourceBuilder.append(java.lang.String.valueOf(a));
							sourceBuilder.appendln("]);");
						}
						for(; a < arguments.size(); a++) {
							sourceBuilder.append("\t\t\tbaseScope.var(\"");
							sourceBuilder.append(convertStringSource(arguments.get(a)));
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
			sourceBuilder.appendln("@SuppressWarnings(\"all\")");
			sourceBuilder.appendln("public BaseObject exec(Global global, Scope scope) {");
			sourceBuilder.appendln("\tif(scope == null)");
			sourceBuilder.appendln("\t\tscope = new Scope(global);");
			sourceBuilder.appendln("\tfinal Scope baseScope = scope;");
		}
		sourceBuilder.indent();
		for (Function function : script.functions) {
			sourceBuilder.append("baseScope.var(\"");
			sourceBuilder.append(convertStringSource(function.name));
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
				transpileParsedSource(sourceBuilder, part, methodPrefix, "baseScope", fileName, true);
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
					if(!(part instanceof Delete) && !(part instanceof Var) && !(part instanceof Try) && !(part instanceof If) && !(part instanceof While) && !(part instanceof For) && !(part instanceof Switch))
						sourceBuilder.append("return ");
					else
						needReturn = true;
				}
				transpileParsedSource(sourceBuilder, part, methodPrefix, "baseScope", fileName, true);
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
			sourceBuilder.appendln(" extends CompiledFunction {");
			sourceBuilder.indent();
			sourceBuilder.appendln("private final Scope baseScope;");
			sourceBuilder.appendln("Scope extendScope(BaseObject _this) {");
			sourceBuilder.appendln("\treturn baseScope.extend(_this);");
			sourceBuilder.appendln("}");
			sourceBuilder.append("private ");
			sourceBuilder.append(functionName);
			sourceBuilder.appendln("(Global global, Scope scope) {");
			sourceBuilder.appendln("\tsuper(global);");
			sourceBuilder.appendln("\tbaseScope = scope;");
			sourceBuilder.appendln("}");

			generateScriptSource(sourceBuilder, function.impl, methodPrefix, fileName, SourceScope.Function);

			sourceBuilder.unindent();
			sourceBuilder.appendln("}");
		}
	}

	protected java.lang.String transpileJavaClassSource(ScriptData script, java.lang.String className, java.lang.String fileName, java.lang.String pkg, boolean inFunction, boolean generateMain) {
		SourceBuilder sourceBuilder = new SourceBuilder();
		if(pkg != null && !pkg.isEmpty()) {
			sourceBuilder.appendln("package " + pkg + ";");
			sourceBuilder.appendln();
		}
		sourceBuilder.appendln("import net.nexustools.njs.compiler.CompiledScript;");
		sourceBuilder.appendln("import net.nexustools.njs.compiler.CompiledFunction;");
		sourceBuilder.appendln("import net.nexustools.njs.BaseObject;");
		sourceBuilder.appendln("import net.nexustools.njs.GenericObject;");
		sourceBuilder.appendln("import net.nexustools.njs.GenericArray;");
		sourceBuilder.appendln("import net.nexustools.njs.Arguments;");
		sourceBuilder.appendln("import net.nexustools.njs.BaseFunction;");
		sourceBuilder.appendln("import net.nexustools.njs.Undefined;");
		sourceBuilder.appendln("import net.nexustools.njs.JSHelper;");
		sourceBuilder.appendln("import net.nexustools.njs.Global;");
		sourceBuilder.appendln("import net.nexustools.njs.Scope;");
		sourceBuilder.appendln("import net.nexustools.njs.Null;");
		sourceBuilder.appendln();
		sourceBuilder.appendln("@SuppressWarnings(\"all\")");
		sourceBuilder.append("public final class ");
		sourceBuilder.append(className);
		sourceBuilder.append(" extends CompiledScript.");
		sourceBuilder.append(addDebugging ? "Debuggable" : "Optimized");
		sourceBuilder.appendln("{");
		sourceBuilder.indent();

		try {
			generateScriptSource(sourceBuilder, script, script.methodName, fileName, inFunction ? SourceScope.GlobalFunction : SourceScope.GlobalScript);
		} catch(RuntimeException t) {
			System.err.println(sourceBuilder.toString());
			throw t;
		}
		
		if(generateMain) {
			sourceBuilder.appendln("public static void main(String[] args) {");
			sourceBuilder.appendln("\tGlobal global = JSHelper.createExtendedGlobal();");
			sourceBuilder.appendln("\tScope scope = new Scope(global);");
			sourceBuilder.appendln("\tscope.var(\"arguments\", JSHelper.convertArguments(global, args));");
			sourceBuilder.append("\tnew ");
			sourceBuilder.append(className);
			sourceBuilder.appendln("().exec(global, scope);");
			sourceBuilder.appendln("}");
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
		final java.lang.String source = transpileJavaClassSource(script, className, fileName, "net.nexustools.njs.gen", inFunction, false);
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
	
	public java.lang.String transpile(java.lang.String input, java.lang.String className, java.lang.String pkg, boolean generateMain) throws FileNotFoundException {
		if(input.equals("-"))
			return transpile(new InputStreamReader(System.in), "System.in", className, pkg, generateMain);
		return transpile(new FileReader(input), input, className, pkg, generateMain);
	}
	
	public java.lang.String transpile(InputStreamReader reader, java.lang.String input, java.lang.String className, java.lang.String pkg, boolean generateMain) {
		return transpileJavaClassSource(parse(reader, className, false), className, input, pkg, false, generateMain);
	}
	
	public static void main(java.lang.String... args) {
		boolean addDebugging = true, generateMain = true;
		java.lang.String input = null, output = null, pkg = null;
		for(int i=0; i<args.length; i++) {
			java.lang.String arg = args[i];
			if(arg.startsWith("-")) {
				if(arg.equals("-s") || arg.equals("-strip") || arg.equals("--strip"))
					addDebugging = false;
				else if(arg.equals("-m") || arg.equals("-nomain") || arg.equals("--nomain") || arg.equals("--no-main"))
					generateMain = false;
				else if(arg.equals("-p") || arg.equals("-package") || arg.equals("--package")) {
					
				} else if(arg.equals("-h") || arg.equals("-help") || arg.equals("--help"))
					throw new UnsupportedOperationException();
				else {
					System.err.println("No such commandline argument " + arg);
					System.exit(1);
					return;
				}
			} else if(input == null)
				input = arg;
			else if(output == null)
				output = arg;
			else {
				System.err.println("Too many arguments");
				System.exit(1);
				return;
			}
		}
		if(input == null) {
			System.err.println("No input specified");
			System.exit(1);
			return;
		}
		
		JavaTranspiler compiler = new JavaTranspiler(addDebugging);
		
		final java.lang.String className;
		final java.lang.String baseName;
		if(output == null) {
			int pos = input.lastIndexOf(".");
			if(pos > -1)
				output = input.substring(0, pos) + ".java";
			else
				output = input + ".java";
		}
		int pos = output.lastIndexOf(File.separatorChar);
		if(pos > -1)
			baseName = output.substring(pos+1);
		else
			baseName = output;
		if(baseName.endsWith(".java"))
			className = compiler.toClassName(baseName.substring(0, baseName.length()-5), false);
		else
			className = compiler.toClassName(baseName, false);
		if(pkg == null) {
			if(pos > -1) {
				pkg = output.substring(0, pos).replaceAll(File.separator, ".");
				if(pkg.startsWith("src."))
					pkg = pkg.substring(4);
				else if(pkg.startsWith("source."))
					pkg = pkg.substring(7);
			}
		}
		
		java.lang.String source;
		try {
			source = compiler.transpile(input, className, pkg, generateMain);
		} catch (FileNotFoundException ex) {
			System.err.println("Input does not exist...");
			ex.printStackTrace();
			System.exit(1);
			return;
		}
		
		OutputStream out;
		if(output.equals("-"))
			out = System.out;
		else {
			File dir;
			if(pos > -1) {
				dir = new File(output.substring(0, pos));
				if(!dir.exists() && !dir.mkdirs()) {
					System.err.println("Output directory could not be created...");
					System.err.println(output.substring(0, pos));
					System.exit(1);
				}
			} else
				dir = new File(".");
			if(!dir.isDirectory()) {
				System.err.println("Output parent is not a directory...");
				System.err.println(dir.toString());
				System.exit(1);
			}
			try {
				File outputFile = new File(dir, className + ".java");
				out = new FileOutputStream(outputFile);
				System.out.println("Writing to " + outputFile);
			} catch (FileNotFoundException ex) {
				System.err.println("Output could not be created...");
				ex.printStackTrace();
				System.exit(1);
				return;
			}
		}
		
		try {
			out.write(source.getBytes());
		} catch (IOException ex) {
			System.err.println("Output could not be written...");
			ex.printStackTrace();
			System.exit(1);
		}
	}
	
}