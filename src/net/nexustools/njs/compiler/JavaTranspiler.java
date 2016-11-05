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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.nexustools.njs.Global;
import net.nexustools.njs.Utilities.FilePosition;
import static net.nexustools.njs.compiler.RegexCompiler.DEBUG;

/**
 *
 * @author Katelyn Slater <ktaeyln@gmail.com>
 */
public class JavaTranspiler extends RegexCompiler {
	private static final List<java.lang.String> RESTRICTED_NAMES = Arrays.asList(new java.lang.String[]{
		"abstract",     "assert",        "boolean",      "break",           "byte",
		"case",         "catch",         "char",         "class",           "const",
		"continue",     "default",       "do",           "double",          "else",
		"enum",         "extends",       "false",        "final",           "finally",
		"float",        "for",           "goto",         "if",              "implements",
		"import",       "instanceof",    "int",          "interface",       "long",
		"native",       "new",           "null",         "package",         "private",
		"protected",    "public",        "return",       "short",           "static",
		"strictfp",     "super",         "switch",       "synchronized",    "this",
		"throw",        "throws",        "transient",    "true",            "try",
		"void",         "volatile",      "while",		 "BaseObject",		"BaseFunction",
		"AbstractFunction", "GenericObject", "GenericArray", "CompiledFunction", "Utilities", "Iterator"});
	
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
	
	private void generateStringSource(SourceBuilder sourceBuilder, Parsed part, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, HashMap<java.lang.String, java.lang.String> expectedStack, Map<java.lang.Integer, FilePosition> sourceMap) {
		if(part instanceof String) {
			sourceBuilder.append("\"");
			sourceBuilder.append(convertStringSource(((String)part).string));
			sourceBuilder.append("\"");
		} else if(part instanceof Plus && ((Plus)part).isStringReferenceChain()) {
			generateStringSource(sourceBuilder, ((Plus)part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(" + ");
			generateStringSource(sourceBuilder, ((Plus)part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
		} else {
			if(localStack != null) {
				if(part instanceof Reference) {
					java.lang.String type = localStack.get(((Reference)part).ref);
					if(type != null) {
						if(type.equals("string")) {
							sourceBuilder.append(((Reference)part).ref);
							return;
						} else if(type.equals("number")) {
							sourceBuilder.append("net.nexustools.njs.Number.toString(");
							sourceBuilder.append(((Reference)part).ref);
							sourceBuilder.append(")");
							return;
						} else if(type.equals("boolean")) {
							sourceBuilder.append("(");
							sourceBuilder.append(((Reference)part).ref);
							sourceBuilder.append(" ? \"true\" : \"false\")");
							return;
						}
					}
				}
			}
			transpileParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
		}
	}
	
	private void generateNumberSource(SourceBuilder sourceBuilder, Parsed part, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, HashMap<java.lang.String, java.lang.String> expectedStack, Map<java.lang.Integer, FilePosition> sourceMap) {
		if(part instanceof Number) {
			sourceBuilder.append(java.lang.String.valueOf(((Number)part).value));
		} else if(part instanceof Integer) {
			sourceBuilder.append(java.lang.String.valueOf(((Integer)part).value));
			sourceBuilder.append(".0");
		} else if(part instanceof String) {
			try {
				java.lang.Double.valueOf(((String)part).string);
				sourceBuilder.append(java.lang.String.valueOf(((String)part).string));
			} catch(NumberFormatException ex) {
				sourceBuilder.append("Double.NaN");
			}
		} else if (part instanceof DoubleShiftLeft) {
			Parsed lhs = ((DoubleShiftLeft)part).lhs;
			Parsed rhs = ((DoubleShiftLeft)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, "<<<", methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, false);
		} else if (part instanceof DoubleShiftRight) {
			Parsed lhs = ((DoubleShiftRight)part).lhs;
			Parsed rhs = ((DoubleShiftRight)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, "<<<", methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, false);
		} else if (part instanceof ShiftLeft) {
			Parsed lhs = ((ShiftLeft)part).lhs;
			Parsed rhs = ((ShiftLeft)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, "<<", methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, false);
		} else if (part instanceof ShiftRight) {
			Parsed lhs = ((ShiftRight)part).lhs;
			Parsed rhs = ((ShiftRight)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, "<<", methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, false);
		} else if (part instanceof Plus) {
			Parsed lhs = ((Plus)part).lhs;
			Parsed rhs = ((Plus)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, '+', methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, false);
		} else if (part instanceof Multiply) {
			Parsed lhs = ((Multiply)part).lhs;
			Parsed rhs = ((Multiply)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, '*', methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, false);
		} else if (part instanceof Divide) {
			Parsed lhs = ((Divide)part).lhs;
			Parsed rhs = ((Divide)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, '/', methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, false);
		} else if (part instanceof And) {
			Parsed lhs = ((And)part).lhs;
			Parsed rhs = ((And)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, '&', methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, false);
		} else if (part instanceof Or) {
			Parsed lhs = ((Or)part).lhs;
			Parsed rhs = ((Or)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, '|', methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, false);
		} else if (part instanceof Percent) {
			Parsed lhs = ((Percent)part).lhs;
			Parsed rhs = ((Percent)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, '%', methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, false);
		} else if (part instanceof Minus) {
			Parsed lhs = ((Minus)part).lhs;
			Parsed rhs = ((Minus)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, '-', methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, false);
		} else if(part instanceof OpenBracket) {
			generateNumberSource(sourceBuilder, ((OpenBracket)part).contents, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
		} else if(part.isNumber()) {
			transpileParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(".value");
		} else {
			if(localStack != null) {
				if(part instanceof Reference) {
					java.lang.String type = localStack.get(((Reference)part).ref);
					if(type != null) {
						if(type.equals("number")) {
							sourceBuilder.append(((Reference)part).ref);
							return;
						}
						if(type.equals("string")) {
							try {
								Double.valueOf(((Reference)part).ref);
								sourceBuilder.append(((Reference)part).ref);
								if(!((Reference)part).ref.contains("."))
									sourceBuilder.append(".0");
							} catch(NumberFormatException ex) {
								sourceBuilder.append("Double.NaN");
							}
							return;
						}
					}
				} else if(part instanceof ReferenceChain) {
					List<java.lang.String> chain = ((ReferenceChain)part).chain;
					java.lang.String type = localStack.get(chain.get(0));
					if(type != null) {
						if(type.equals("array") && chain.size() == 2 && chain.get(1).equals("length")) {
							sourceBuilder.append(chain.get(0));
							sourceBuilder.append(".length()");
							return;
						}
					}
				}
			}
			sourceBuilder.append("global.Number.fromValueOf(");
			transpileParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(").value");
		}
	}
	
	private void generateIntegerSource(SourceBuilder sourceBuilder, Parsed part, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, HashMap<java.lang.String, java.lang.String> expectedStack, Map<java.lang.Integer, FilePosition> sourceMap) {
		if(part instanceof Number) {
			sourceBuilder.append(java.lang.String.valueOf((int)((Number)part).value));
		} else if(part instanceof Integer) {
			sourceBuilder.append(java.lang.String.valueOf(((Integer)part).value));
		} else if(part instanceof String) {
			try {
				java.lang.Double.valueOf(((String)part).string);
				int pos = ((String)part).string.indexOf('.');
				if(pos > -1)
					sourceBuilder.append(((String)part).string.substring(0, pos));
				else
					sourceBuilder.append(((String)part).string);
			} catch(NumberFormatException ex) {
				sourceBuilder.append("0");
			}
		} else if (part instanceof Plus) {
			Parsed lhs = ((Plus)part).lhs;
			Parsed rhs = ((Plus)part).rhs;
			sourceBuilder.append("(int)");
			generateMath(sourceBuilder, lhs, rhs, '+', methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, false);
		} else if (part instanceof Multiply) {
			Parsed lhs = ((Multiply)part).lhs;
			Parsed rhs = ((Multiply)part).rhs;
			sourceBuilder.append("(int)");
			generateMath(sourceBuilder, lhs, rhs, '*', methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, false);
		} else if (part instanceof Divide) {
			Parsed lhs = ((Divide)part).lhs;
			Parsed rhs = ((Divide)part).rhs;
			sourceBuilder.append("(int)");
			generateMath(sourceBuilder, lhs, rhs, '/', methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, false);
		} else if (part instanceof And) {
			Parsed lhs = ((And)part).lhs;
			Parsed rhs = ((And)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, '&', methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, false);
		} else if (part instanceof Or) {
			Parsed lhs = ((Or)part).lhs;
			Parsed rhs = ((Or)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, '|', methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, false);
		} else if (part instanceof Percent) {
			Parsed lhs = ((Percent)part).lhs;
			Parsed rhs = ((Percent)part).rhs;
			sourceBuilder.append("(int)");
			generateMath(sourceBuilder, lhs, rhs, '%', methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, false);
		} else if (part instanceof Minus) {
			Parsed lhs = ((Minus)part).lhs;
			Parsed rhs = ((Minus)part).rhs;
			sourceBuilder.append("(int)");
			generateMath(sourceBuilder, lhs, rhs, '-', methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, false);
		} else {
			if(localStack != null) {
				if(part instanceof BaseReferency) {
					if(part instanceof Reference) {
						java.lang.String type = localStack.get(((Reference)part).ref);
						if(type != null && type.equals("number")) {
							sourceBuilder.append("(int)");
							sourceBuilder.append(((Reference)part).ref);
							return;
						}
					} else
						throw new UnsupportedOperationException("Cannot compile optimized long : " + describe(part));
				}
			}
			
			sourceBuilder.append("(int)");
			sourceBuilder.append("global.Number.fromValueOf(");
			transpileParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(").value");
		}
	}

	
	private void generateMath(SourceBuilder sourceBuilder, Parsed lhs, Parsed rhs, char op, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, HashMap<java.lang.String, java.lang.String> expectedStack, Map<java.lang.Integer, FilePosition> sourceMap) {
		generateMath(sourceBuilder, lhs, rhs, op, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, true);
	}
	
	private void generateMath(SourceBuilder sourceBuilder, Parsed lhs, Parsed rhs, char op, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, HashMap<java.lang.String, java.lang.String> expectedStack, Map<java.lang.Integer, FilePosition> sourceMap, boolean wrapAsBaseObject) {
		generateMath(sourceBuilder, lhs, rhs, "" + op, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, wrapAsBaseObject);
	}
	private void generateMath(SourceBuilder sourceBuilder, Parsed lhs, Parsed rhs, java.lang.String op, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, HashMap<java.lang.String, java.lang.String> expectedStack, Map<java.lang.Integer, FilePosition> sourceMap, boolean wrapAsBaseObject) {
		if(op.equals("+") && wrapAsBaseObject) {
			if((lhs instanceof StringReferency && !isNumber(lhs, localStack)) || (rhs instanceof StringReferency && !isNumber(rhs, localStack))) {
				if(lhs instanceof String && ((String)lhs).string.isEmpty()) {
					if(wrapAsBaseObject) {
						generateStringSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
						sourceBuilder.append("._toString()");
					} else
						generateStringSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					return;
				}
				
				if(wrapAsBaseObject)
					sourceBuilder.append("global.wrap(");
				else
					sourceBuilder.append("(");
				generateStringSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				sourceBuilder.append(" + ");
				generateStringSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				sourceBuilder.append(")");
				return;
			} else if(((lhs instanceof Plus && ((Plus)lhs).isStringReferenceChain()) || (rhs instanceof Plus && ((Plus)rhs).isStringReferenceChain()))) {
				if(wrapAsBaseObject) 
					sourceBuilder.append("global.wrap(");
				else
					sourceBuilder.append("(");
				generateStringSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				sourceBuilder.append(" + ");
				generateStringSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				sourceBuilder.append(")");
				return;
			} else if((!(lhs instanceof NumberReferency) && !(rhs instanceof NumberReferency))) {
				if(!wrapAsBaseObject)
					sourceBuilder.append("global.Number.fromValueOf(");
				sourceBuilder.append("plus(global, ");
				transpileParsedSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				sourceBuilder.append(", ");
				transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				sourceBuilder.append(")");
				if(!wrapAsBaseObject)
					sourceBuilder.append(").value");
				return;
			}
		}
		
		if(wrapAsBaseObject)
			sourceBuilder.append("global.wrap(");
		else
			sourceBuilder.append("(");
		boolean andOrOr = op.equals("|") || op.equals("&") || op.startsWith("<") || op.startsWith(">");
		if(andOrOr)
			generateIntegerSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
		else
			generateNumberSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
		sourceBuilder.append(" " + op + " ");
		if(andOrOr)
			generateIntegerSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
		else
			generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
		sourceBuilder.append(")");
	}

	private void generateIfBlockSource(SourceBuilder sourceBuilder, Else els, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, HashMap<java.lang.String, java.lang.String> expectedStack, Map<java.lang.Integer, FilePosition> sourceMap) {
		while(els != null) {
			if(els.simpleimpl != null) {
				if(els instanceof ElseIf) {
					sourceBuilder.append(" else if(");
					generateBooleanSource(sourceBuilder, els.condition, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					sourceBuilder.appendln(") {");
				} else
					sourceBuilder.appendln(" else {");
				sourceBuilder.indent();
				transpileParsedSource(sourceBuilder, els.simpleimpl, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, true);
				sourceBuilder.appendln(";");
				sourceBuilder.unindent();
				sourceBuilder.append("}");
			} else{
				if(els instanceof ElseIf) {
					sourceBuilder.append(" else if(");
					generateBooleanSource(sourceBuilder, els.condition, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					sourceBuilder.appendln(") {");
				} else
					sourceBuilder.appendln(" else {");
				sourceBuilder.indent();
				generateBlockSource(sourceBuilder, els.impl, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				sourceBuilder.unindent();
				sourceBuilder.append("}");
			}

			if(els instanceof ElseIf)
				els = ((ElseIf)els).el;
			else
				break;
		}
	}

	private void generateBooleanSource(SourceBuilder sourceBuilder, Parsed part, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, HashMap<java.lang.String, java.lang.String> expectedStack, Map<java.lang.Integer, FilePosition> sourceMap) {
		while(part instanceof OpenBracket)
			part = ((OpenBracket)part).contents;
		
		if(part instanceof Boolean) {
			if(((Boolean)part).value)
				sourceBuilder.append("true");
			else
				sourceBuilder.append("false");
		} else if(part instanceof InstanceOf) {
			transpileParsedSource(sourceBuilder, ((InstanceOf)part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(".instanceOf((BaseFunction)");
			transpileParsedSource(sourceBuilder, ((InstanceOf)part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(")");
		} else if(part instanceof Not) {
			sourceBuilder.append("!");
			generateBooleanSource(sourceBuilder, ((Not) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
		} else if(part instanceof OrOr) {
			transpileParsedSource(sourceBuilder, ((OrOr) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(" || ");
			generateBooleanSource(sourceBuilder, ((OrOr) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
		} else if(part instanceof AndAnd) {
			generateBooleanSource(sourceBuilder, ((AndAnd) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(" && ");
			generateBooleanSource(sourceBuilder, ((AndAnd) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
		} else if(part instanceof Equals) {
			java.lang.String ltype = ((Equals)part).lhs.primaryType();
			java.lang.String rtype = ((Equals)part).rhs.primaryType();
			if(ltype.equals(rtype) && generateCommonComparison(sourceBuilder, ltype, false, ((Equals)part).lhs, ((Equals)part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap))
				return;
			
			transpileParsedSource(sourceBuilder, ((Equals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(".equals(");
			transpileParsedSource(sourceBuilder, ((Equals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(")");
		} else if(part instanceof NotEquals) {
			java.lang.String ltype = ((NotEquals)part).lhs.primaryType();
			java.lang.String rtype = ((NotEquals)part).rhs.primaryType();
			if(ltype.equals(rtype) && generateCommonComparison(sourceBuilder, ltype, true, ((NotEquals)part).lhs, ((NotEquals)part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap))
				return;
			
			sourceBuilder.append("!");
			transpileParsedSource(sourceBuilder, ((NotEquals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(".equals(");
			transpileParsedSource(sourceBuilder, ((NotEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(")");
		} else if(part instanceof StrictEquals) {
			java.lang.String ltype = ((StrictEquals)part).lhs.primaryType();
			java.lang.String rtype = ((StrictEquals)part).rhs.primaryType();
			if(ltype.equals(rtype) && generateCommonComparison(sourceBuilder, ltype, false, ((StrictEquals)part).lhs, ((StrictEquals)part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap))
				return;
			
			sourceBuilder.append("(BaseObject)");
			transpileParsedSource(sourceBuilder, ((StrictEquals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(" == (BaseObject)");
			transpileParsedSource(sourceBuilder, ((StrictEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
		} else if(part instanceof StrictNotEquals) {
			java.lang.String ltype = ((StrictNotEquals)part).lhs.primaryType();
			java.lang.String rtype = ((StrictNotEquals)part).rhs.primaryType();
			if(ltype.equals(rtype) && generateCommonComparison(sourceBuilder, ltype, true, ((StrictNotEquals)part).lhs, ((StrictNotEquals)part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap))
				return;
			
			sourceBuilder.append("(BaseObject)");
			transpileParsedSource(sourceBuilder, ((StrictNotEquals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(" != (BaseObject)");
			transpileParsedSource(sourceBuilder, ((StrictNotEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
		} else if(part instanceof MoreThan) {
			Parsed lhs = ((MoreThan)part).lhs;
			Parsed rhs = ((MoreThan)part).rhs;
			
			if(isNumber(lhs, localStack) || isNumber(rhs, localStack)) {
				generateNumberSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				sourceBuilder.append(" > ");
				generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				return;
			}
			
			sourceBuilder.append("lessThan(");
			transpileParsedSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(", ");
			transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(")");
		} else if(part instanceof LessThan) {
			Parsed lhs = ((LessThan)part).lhs;
			Parsed rhs = ((LessThan)part).rhs;
			
			if(isNumber(lhs, localStack) || isNumber(rhs, localStack)) {
				generateNumberSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				sourceBuilder.append(" < ");
				generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				return;
			}
			
			sourceBuilder.append("lessThan(");
			transpileParsedSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(", ");
			transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(")");
		} else if(part instanceof MoreEqual) {
			Parsed lhs = ((MoreEqual)part).lhs;
			Parsed rhs = ((MoreEqual)part).rhs;
			
			if(isNumber(lhs, localStack) || isNumber(rhs, localStack)) {
				generateNumberSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				sourceBuilder.append(" >= ");
				generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				return;
			}
			
			sourceBuilder.append("moreEqual(");
			transpileParsedSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(", ");
			transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(")");
		} else if(part instanceof LessEqual) {
			Parsed lhs = ((LessEqual)part).lhs;
			Parsed rhs = ((LessEqual)part).rhs;
			
			if(isNumber(lhs, localStack) || isNumber(rhs, localStack)) {
				generateNumberSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				sourceBuilder.append(" <= ");
				generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				return;
			}
			
			sourceBuilder.append("lessEqual(");
			transpileParsedSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(", ");
			transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
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
			if(localStack != null) {
				if(part instanceof Reference) {
					java.lang.String type = localStack.get(((Reference)part).ref);
					if(type != null) {
						if(type.equals("boolean")) {
							sourceBuilder.append(((Reference)part).ref);
							return;
						} else if(type.equals("string")) {
							sourceBuilder.append("!");
							sourceBuilder.append(((Reference)part).ref);
							sourceBuilder.append(".isEmpty()");
							return;
						} else if(type.equals("number")) {
							sourceBuilder.append("(");
							sourceBuilder.append(((Reference)part).ref);
							sourceBuilder.append(" != 0)");
							return;
						}
					}
				}
			}
			transpileParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(".toBool()");
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

	static {
		if(System.getProperties().containsKey("NJSNOCOMPILING"))
			throw new RuntimeException("NJSNOCOMPILING");
		//assert ((new JavaTranspiler().compile("(function munchkin(){\n\tfunction yellow(){\n\t\treturn 55;\n\t}\n\treturn yellow()\n\t})()", "JavaCompilerStaticTest", false)).exec(new Global(), null).toString().equals("55"));
	}

	private boolean generateCommonComparison(SourceBuilder sourceBuilder, java.lang.String ltype, boolean not, Parsed lhs, Parsed rhs, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, HashMap<java.lang.String, java.lang.String> expectedStack, Map<java.lang.Integer, FilePosition> sourceMap) {
		if(ltype.equals("string")) {
			if(not)
				sourceBuilder.append("!");
			generateStringSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(".equals(");
			generateStringSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(")");
			return true;
		}
		if(ltype.equals("number")) {
			generateNumberSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(not ? " != " : " == ");
			generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			return true;
		}
		if(ltype.equals("boolean")) {
			generateBooleanSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(not ? " != " : " == ");
			generateBooleanSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			return true;
		}
		return false;
	}

	private void generateLocalStackAccess(SourceBuilder sourceBuilder, java.lang.String ref, java.lang.String baseScope, LocalStack localStack) {
		java.lang.String type;
		if(localStack != null && (type = localStack.get(ref)) != null) {
			if(ref.equals("this"))
				ref = "_this";
			if(type.equals("number")) {
				sourceBuilder.append("global.Number.wrap(");
				sourceBuilder.append(ref);
				sourceBuilder.append(")");
			} else if(type.equals("string")) {
				sourceBuilder.append("global.String.wrap(");
				sourceBuilder.append(ref);
				sourceBuilder.append(")");
			} else if(type.equals("boolean")) {
				sourceBuilder.append("(");
				sourceBuilder.append(ref);
				sourceBuilder.append(" ? global.Boolean.TRUE : global.Boolean.FALSE)");
			} else
				sourceBuilder.append(ref);
		} else {
			sourceBuilder.append(baseScope);
			sourceBuilder.append(".get(\"");
			sourceBuilder.append(convertStringSource(ref));
			sourceBuilder.append("\")");
		}
	}

	private void addSourceMapEntry(SourceBuilder sourceBuilder, Map<java.lang.Integer, FilePosition> sourceMap, Parsed part) {
		int row = sourceBuilder.row;
		if(sourceMap.containsKey(row))
			return;
		
		sourceMap.put(row, new FilePosition(part.rows, part.columns));
	}

	private boolean isNumber(Parsed part, LocalStack localStack) {
		if(part.isNumber())
			return true;
		
		if(localStack != null && part instanceof Reference) {
			java.lang.String type = localStack.get(((Reference)part).ref);
			return type != null && type.equals("number");
		}
		return false;
	}

	private static final Pattern RESTRICTED_PATTERN = Pattern.compile("^(_this|__this|function|global|(block|catch|base)Scope\\d*)$");
	public static class ScopeOptimizer {
		public final ArrayList<java.lang.String> volati = new ArrayList();
		public final HashMap<java.lang.String, java.lang.String> scope = new HashMap();
		public void var(java.lang.String key, java.lang.String type) {
			if(RESTRICTED_PATTERN.matcher(key).matches())
				throw new CannotOptimize("Encountered " + key);
			java.lang.String current = scope.get(key);
			if(current == null || current.equals("any"))
				scope.put(key, type);
			else if(!current.equals(type))
				scope.put(key, "unknown");
			else
				scope.put(key, type);
		}
		public void let(java.lang.String key, java.lang.String type) {
			var(key, type);
		}
		public void markVolatile(java.lang.String key) {
			if(scope.containsKey(key) && !volati.contains(key))
				volati.add(key);
		}
		public void update(java.lang.String key, java.lang.String newType) {
			java.lang.String current = scope.get(key);
			if(DEBUG) System.out.println(key + " : " + current + " : " + newType);
			if(current == null)
				return;
			if(current.equals("any"))
				scope.put(key, newType);
			else if(!current.equals(newType) && volati.contains(key))
				throw new CannotOptimize("Volatile key " + key + " was changed to " + newType + " from " + current);
		}

		public boolean isTyped(java.lang.String key, java.lang.String type) {
			return type.equals(scope.get(key));
		}

		public void assertTyped(java.lang.String key, java.lang.String... _types) {
			java.lang.String current = scope.get(key);
			List<java.lang.String> types = Arrays.asList(_types);
			if(current == null)
				throw new CannotOptimize("Unknown variable " + key + " must be typed as one of " + types + " : " + this);
			else if(!types.contains(current))
				throw new CannotOptimize(current + " typed variable " + key + " must be typed as one of " + types + " : " + this);
			markVolatile(key);
		}

		public java.lang.String lookup(java.lang.String key) {
			java.lang.String current = scope.get(key);
			if(current == null)
				return "unknown";
			else
				return current;
		}
	}
	public static class FunctionScopeOptimizer extends ScopeOptimizer {
		final ScopeOptimizer parent;
		FunctionScopeOptimizer(ScopeOptimizer parent) {
			this.parent = parent;
		}
		
		@Override
		public void markVolatile(java.lang.String key) {
			if(scope.containsKey(key)) {
				if(!volati.contains(key))
					volati.add(key);
			} else
				parent.markVolatile(key);
		}
		
		@Override
		public void update(java.lang.String key, java.lang.String newType) {
			java.lang.String current = scope.get(key);
			if(DEBUG) System.out.println(key + " : " + current + " : " + newType);
			if(current == null)
				return;
			if(current.equals("any"))
				scope.put(key, newType);
			else if(!current.equals(newType)) {
				if(volati.contains(key))
					throw new CannotOptimize("Volatile key " + key + " was changed to " + newType + " from " + current);
				scope.put(key, "unknown");
			} else
				parent.update(key, newType);
		}

		@Override
		public boolean isTyped(java.lang.String key, java.lang.String type) {
			java.lang.String typedAs = scope.get(key);
			if(typedAs != null)
				return type.equals(typedAs);
			return parent.isTyped(key, type);
		}

		@Override
		public void assertTyped(java.lang.String key, java.lang.String... _types) {
			java.lang.String current = scope.get(key);
			if(current == null) {
				parent.assertTyped(key, _types);
				return;
			}
			
			List<java.lang.String> types = Arrays.asList(_types);
			if(!types.contains(current))
				throw new CannotOptimize(current + " typed variable " + key + " must be typed as one of " + types);
		}

		@Override
		public java.lang.String lookup(java.lang.String key) {
			java.lang.String current = scope.get(key);
			if(current == null)
				return parent.lookup(key);
			else
				return current;
		}
	}
	public static class BlockScopeOptimizer extends ScopeOptimizer {
		final ScopeOptimizer parent;
		BlockScopeOptimizer(ScopeOptimizer parent) {
			this.parent = parent;
		}

		@Override
		public void var(java.lang.String key, java.lang.String type) {
			parent.var(key, type);
		}
		
		@Override
		public void markVolatile(java.lang.String key) {
			if(scope.containsKey(key)) {
				if(!volati.contains(key))
					volati.add(key);
			} else
				parent.markVolatile(key);
		}
		
		@Override
		public void update(java.lang.String key, java.lang.String newType) {
			java.lang.String current = scope.get(key);
			if(DEBUG) System.out.println(key + " : " + current + " : " + newType);
			if(current == null)
				return;
			if(current.equals("any"))
				scope.put(key, newType);
			else if(!current.equals(newType)) {
				if(volati.contains(key))
					throw new CannotOptimize("Volatile key " + key + " was changed to " + newType + " from " + current);
				scope.put(key, "unknown");
			} else
				parent.update(key, newType);
		}

		@Override
		public boolean isTyped(java.lang.String key, java.lang.String type) {
			java.lang.String typedAs = scope.get(key);
			if(typedAs != null)
				return type.equals(typedAs);
			return parent.isTyped(key, type);
		}

		@Override
		public void assertTyped(java.lang.String key, java.lang.String... _types) {
			java.lang.String current = scope.get(key);
			if(current == null) {
				parent.assertTyped(key, _types);
				return;
			}
			
			List<java.lang.String> types = Arrays.asList(_types);
			if(!types.contains(current))
				throw new CannotOptimize(current + " typed variable " + key + " must be typed as one of " + types);
		}

		@Override
		public java.lang.String lookup(java.lang.String key) {
			java.lang.String current = scope.get(key);
			if(current == null)
				return parent.lookup(key);
			else
				return current;
		}
	}
	private static class CannotOptimize extends RuntimeException {
		public CannotOptimize(java.lang.String reason) {
			super(reason);
		}
	}
	private static final class CannotOptimizeUnimplemented extends CannotOptimize {
		public CannotOptimizeUnimplemented(java.lang.String reason) {
			super(reason);
		}
	}
	private void scanParsedSource(Parsed parsed, ScopeOptimizer variableScope) {
		if(parsed instanceof Let) {
			for(Var.Set set : ((Var)parsed).sets) {
				if(set.rhs != null)
					variableScope.let(set.lhs, set.rhs.primaryType());
				else
					variableScope.let(set.lhs, "any");
			}
		} else if(parsed instanceof Var) {
			for(Var.Set set : ((Var)parsed).sets) {
				if(set.rhs != null)
					variableScope.var(set.lhs, set.rhs.primaryType());
				else
					variableScope.var(set.lhs, "any");
			}
		} else if(parsed instanceof Call) {
			for(Parsed argument : ((Call)parsed).arguments) {
				if(argument.isNumberOrBool())
					continue;
				
				if(argument instanceof BaseReferency) {
					if(argument instanceof Reference)
						variableScope.assertTyped(((Reference)argument).ref, "number", "boolean");
					else if(argument instanceof Call)
						scanParsedSource(argument, variableScope);
					else if(argument instanceof RhLh) {
						scanParsedSource(((RhLh)argument).lhs, variableScope);
						scanParsedSource(((RhLh)argument).rhs, variableScope);
					} else if(argument instanceof Rh)
						scanParsedSource(((Rh)argument).rhs, variableScope);
					else if(argument instanceof String || argument instanceof Number || argument instanceof Integer || argument instanceof Null || argument instanceof Undefined) {
						// IGNORED
					} else if(argument instanceof ReferenceChain)
						throw new CannotOptimize("Encountered " + argument.getClass().getSimpleName());
					else
						throw new CannotOptimizeUnimplemented("Unhandled argument: " + describe(argument));
				} else if(argument instanceof Equals || argument instanceof NotEquals || argument instanceof InstanceOf ||
						argument instanceof StrictEquals || argument instanceof StrictNotEquals) {
					// IGNORED
				}else if(argument instanceof RhLh) {
						scanParsedSource(((RhLh)argument).lhs, variableScope);
						scanParsedSource(((RhLh)argument).rhs, variableScope);
				} else if(argument instanceof Rh)
					scanParsedSource(((Rh)argument).rhs, variableScope);
				else if(argument instanceof Function)
					scanScriptSource(((Function)argument).impl);
				else
					throw new CannotOptimizeUnimplemented("Unhandled argument: " + describe(argument));
			}
		} else if(parsed instanceof If) {
			scanParsedSource(((If)parsed).condition, variableScope);
			if(((If)parsed).simpleimpl != null)
				scanParsedSource(((If)parsed).simpleimpl, variableScope);
			else
				scanScriptSource(((If)parsed).impl, new BlockScopeOptimizer(variableScope));
			
			Else el = ((If)parsed).el;
			while(el != null) {
				if(el instanceof ElseIf)
					scanParsedSource(((ElseIf)el).condition, variableScope);
				if(el.simpleimpl != null)
					scanParsedSource(el.simpleimpl, variableScope);
				else
					scanScriptSource(el.impl, new BlockScopeOptimizer(variableScope));
				if(el instanceof ElseIf)
					el = ((ElseIf)el).el;
				else
					break;
			}
		} else if(parsed instanceof For) {
			BlockScopeOptimizer forScope = new BlockScopeOptimizer(variableScope);
			scanParsedSource(((For)parsed).init, forScope);
			scanParsedSource(((For)parsed).loop, forScope);
			if(((For)parsed).type == For.ForType.Standard)
				scanParsedSource(((For)parsed).condition, forScope);
			if(((For)parsed).simpleimpl != null)
				scanParsedSource(((For)parsed).simpleimpl, forScope);
			else
				scanScriptSource(((For)parsed).impl, forScope);
		} else if(parsed instanceof While) {
			scanParsedSource(((While)parsed).condition, variableScope);
			if(((While)parsed).simpleimpl != null)
				scanParsedSource(((While)parsed).simpleimpl, variableScope);
			else
				scanScriptSource(((While)parsed).impl, new BlockScopeOptimizer(variableScope));
		} else if(parsed instanceof Try) {
			scanScriptSource(((Try)parsed).impl, variableScope);
			if(((Try)parsed).c != null)
				scanScriptSource(((Try)parsed).c.impl, variableScope);
			if(((Try)parsed).f != null)
				scanScriptSource(((Try)parsed).f.impl, variableScope);
		} else if(parsed instanceof BaseReferency) {
			if(parsed instanceof PlusPlus) {
				Parsed ref = ((PlusPlus)parsed).ref;
				if(ref instanceof Reference) {
					variableScope.update(((Reference)ref).ref, "number");
				} else
					throw new CannotOptimizeUnimplemented("No implementation for optimizing " + describe(ref));
			} else if(parsed instanceof PlusEq) {
				Parsed ref = ((PlusEq)parsed).lhs;
				Parsed rhs = ((PlusEq)parsed).rhs;
				if(rhs.isNumber() || (rhs instanceof Reference && variableScope.isTyped(((Reference)rhs).ref, "number"))) {
					if(ref instanceof Reference) {
						variableScope.update(((Reference)ref).ref, "number");
					} else
						throw new CannotOptimizeUnimplemented("No implementation for optimizing " + describe(ref));
				}
			} else if(parsed instanceof MultiplyEq) {
				Parsed ref = ((MultiplyEq)parsed).lhs;
				if(ref instanceof Reference) {
					variableScope.update(((Reference)ref).ref, "number");
				} else
					throw new CannotOptimizeUnimplemented("No implementation for optimizing " + describe(ref));
			} else if(parsed instanceof MinusMinus) {
				Parsed ref = ((MinusMinus)parsed).ref;
				if(ref instanceof Reference) {
					variableScope.update(((Reference)ref).ref, "number");
				} else
					throw new CannotOptimizeUnimplemented("No implementation for optimizing " + describe(ref));
				
				//variableScope.update(, NUMBER_REG);
			} else if(parsed instanceof Set) {
				Parsed lhs = ((Set)parsed).lhs;
				
				if(lhs instanceof Reference) {
					Parsed rhs = ((Set)parsed).rhs;
					if(rhs instanceof BaseReferency) {
						if(rhs instanceof Reference)
							variableScope.update(((Reference)lhs).ref, variableScope.lookup(((Reference)rhs).ref));
						else
							variableScope.update(((Reference)lhs).ref, rhs.primaryType());
					} else if(rhs instanceof Function) {
						for(java.lang.String arg : ((Function)rhs).arguments)
							if(RESTRICTED_PATTERN.matcher(arg).matches())
								throw new CannotOptimize("Encountered: " + arg);
						scanScriptSource(((Function)rhs).impl, new FunctionScopeOptimizer(variableScope));
					} else
						throw new CannotOptimizeUnimplemented("No implementation for optimizing set " + describe(rhs));
				} else if(lhs instanceof ReferenceChain || lhs instanceof VariableReference || lhs instanceof IntegerReference) {
					// IGNORED
				} else
					throw new CannotOptimizeUnimplemented("No implementation for optimizing set " + describe(lhs));
			} else if(parsed instanceof New || parsed instanceof Boolean || parsed instanceof String || parsed instanceof Integer || parsed instanceof ShiftLeft || parsed instanceof ShiftRight ||
					parsed instanceof ReferenceChain || parsed instanceof Reference || parsed instanceof Number || parsed instanceof DoubleShiftLeft || parsed instanceof DoubleShiftRight) {
				// IGNORED
			} else
				throw new CannotOptimizeUnimplemented("No implementation for optimizing " + describe(parsed));
		} else if(parsed instanceof AndAnd || parsed instanceof OrOr || parsed instanceof Equals || parsed instanceof NotEquals || parsed instanceof StrictEquals || parsed instanceof StrictNotEquals || parsed instanceof Throw ||
				parsed instanceof New || parsed instanceof Boolean || parsed instanceof String || parsed instanceof Integer || parsed instanceof LessThan || parsed instanceof MoreThan || parsed instanceof Return ||
					parsed instanceof ReferenceChain || parsed instanceof Reference || parsed instanceof Number || parsed instanceof LessEqual || parsed instanceof MoreEqual || parsed instanceof Not) {
			// IGNORED
		} else
			throw new CannotOptimizeUnimplemented("Unhandled " + describe(parsed));
	}
	private void scanParsedSource(Parsed[] impl, ScopeOptimizer variableScope) {
		for(Parsed parsed : impl) {
			while(parsed instanceof OpenBracket)
				parsed = ((OpenBracket)parsed).contents;
			
			scanParsedSource(parsed, variableScope);
		}
	}
	private void scanScriptSource(ScriptData script, ScopeOptimizer variableScope) {
		if(!script.functions.isEmpty())
			throw new CannotOptimize("Contains primary functions...");
		
		scanParsedSource(script.impl, variableScope);
		
	}
	private void scanScriptSource(ScriptData script) {
		ScopeOptimizer variableScope = new ScopeOptimizer();
		try {
			scanScriptSource(script, variableScope);
			script.optimizations = variableScope.scope;
		} catch(CannotOptimize ex) {
			if(DEBUG || ex instanceof CannotOptimizeUnimplemented)
				ex.printStackTrace(System.out);
		}
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
	
	protected static class LocalStack {
		private static final HashMap<java.lang.String, java.lang.String> stack = new HashMap();
		public void put(java.lang.String key, java.lang.String val) {
			stack.put(key, val);
		}
		public java.lang.String get(java.lang.String key) {
			return stack.get(key);
		}
	}
	
	protected void generateBlockSource(SourceBuilder sourceBuilder, ScriptData blockDat, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, HashMap<java.lang.String, java.lang.String> expectedStack, Map<java.lang.Integer, FilePosition> sourceMap) {
		for(Parsed part : blockDat.impl) {
			if (addDebugging && (part.rows > 1 || part.columns > 1))
				addSourceMapEntry(sourceBuilder, sourceMap, part);
			transpileParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, true);
			sourceBuilder.appendln(";");
		}
	}
		
	protected void transpileParsedSource(SourceBuilder sourceBuilder, Parsed part, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, HashMap<java.lang.String, java.lang.String> expectedStack, Map<java.lang.Integer, FilePosition> sourceMap) {
		transpileParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, false);
	}
	protected void transpileParsedSource(SourceBuilder sourceBuilder, Parsed part, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, HashMap<java.lang.String, java.lang.String> expectedStack, Map<java.lang.Integer, FilePosition> sourceMap, boolean atTop) {
		while(part instanceof OpenBracket)
			part = ((OpenBracket)part).contents;
		
		if (part instanceof Return) {
			sourceBuilder.append("return ");
			transpileParsedSource(sourceBuilder, ((Return) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			return;
		} else if(part instanceof TypeOf) {
			Parsed rhs = ((TypeOf)part).rhs;
			sourceBuilder.append("global.wrap(");
			transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(".typeOf())");
			return;
		} else if (part instanceof Call) {
			Parsed reference = ((Call)part).reference;
			while(reference instanceof OpenBracket) // unwrap
				reference = ((OpenBracket)reference).contents;
			
			if(reference instanceof BaseReferency && !(reference instanceof Reference || reference instanceof Call)) {
				final java.lang.String source = reference.toSimpleSource();
				if(reference instanceof RightReference) {
					final java.lang.String key = ((RightReference)reference).chain.remove(((RightReference)reference).chain.size()-1);
					if(atTop) {
						sourceBuilder.append("__this = ");
						transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
						sourceBuilder.appendln(";");
						if(addDebugging) {
							sourceBuilder.appendln("try {");
							sourceBuilder.indent();
						}
						sourceBuilder.append("function = (BaseFunction)__this.get(\"");
						sourceBuilder.append(convertStringSource(key));
						sourceBuilder.appendln("\");");
						if(addDebugging) {
							sourceBuilder.unindent();
							sourceBuilder.appendln("} catch(ClassCastException ex) {");
							sourceBuilder.append("\tthrow new net.nexustools.njs.Error.JavaException(\"ReferenceError\", \"");
							sourceBuilder.append(convertStringSource(source));
							sourceBuilder.appendln(" is not a function\");");
							sourceBuilder.appendln("}");
						}
						sourceBuilder.append("function.call(__this");
					} else {
						sourceBuilder.append("callTop(");
						if (addDebugging) {
							sourceBuilder.append("\"");
							sourceBuilder.append(convertStringSource(source));
							sourceBuilder.append("\", ");
						}
						sourceBuilder.append("\"");
						sourceBuilder.append(convertStringSource(key));
						sourceBuilder.append("\", ");
						transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					}
					for (Parsed arg : ((Call) part).arguments) {
						sourceBuilder.append(", ");
						transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					}
					sourceBuilder.append(")");
					return;
				} else if(reference instanceof IntegerReference) {
					final int key = ((IntegerReference)reference).ref;
					if(atTop) {
						sourceBuilder.append("__this = ");
						transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
						sourceBuilder.appendln(";");
						if(addDebugging) {
							sourceBuilder.appendln("try {");
							sourceBuilder.indent();
						}
						sourceBuilder.append("function = (BaseFunction)__this.get(");
						sourceBuilder.append(java.lang.String.valueOf(key));
						sourceBuilder.appendln(");");
						if(addDebugging) {
							sourceBuilder.unindent();
							sourceBuilder.appendln("} catch(ClassCastException ex) {");
							sourceBuilder.append("\tthrow new net.nexustools.njs.Error.JavaException(\"ReferenceError\", \"");
							sourceBuilder.append(convertStringSource(source));
							sourceBuilder.appendln(" is not a function\");");
							sourceBuilder.appendln("}");
						}
						sourceBuilder.append("function.call(__this");
					} else {
						sourceBuilder.append("callTop(");
						if (addDebugging) {
							sourceBuilder.append("\"");
							sourceBuilder.append(convertStringSource(source));
							sourceBuilder.append("\", ");
						}
						sourceBuilder.append(java.lang.String.valueOf(key));
						sourceBuilder.append(", ");
						transpileParsedSource(sourceBuilder, ((IntegerReference)reference).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					}
					for (Parsed arg : ((Call) part).arguments) {
						sourceBuilder.append(", ");
						transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					}
					sourceBuilder.append(")");
					return;
				} else if(reference instanceof ReferenceChain) {
					final java.lang.String key = ((ReferenceChain)reference).chain.remove(((ReferenceChain)reference).chain.size()-1);
					if(atTop) {
						sourceBuilder.append("__this = ");
						transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
						sourceBuilder.appendln(";");
						if(addDebugging) {
							sourceBuilder.appendln("try {");
							sourceBuilder.indent();
						}
						sourceBuilder.append("function = (BaseFunction)__this.get(\"");
						sourceBuilder.append(convertStringSource(key));
						sourceBuilder.appendln("\");");
						if(addDebugging) {
							sourceBuilder.unindent();
							sourceBuilder.appendln("} catch(ClassCastException ex) {");
							sourceBuilder.append("\tthrow new net.nexustools.njs.Error.JavaException(\"ReferenceError\", \"");
							sourceBuilder.append(convertStringSource(source));
							sourceBuilder.appendln(" is not a function\");");
							sourceBuilder.appendln("}");
						}
						sourceBuilder.append("function.call(__this");
					} else {
						sourceBuilder.append("callTop(");
						if (addDebugging) {
							sourceBuilder.append("\"");
							sourceBuilder.append(convertStringSource(source));
							sourceBuilder.append("\", ");
						}
						sourceBuilder.append("\"");
						sourceBuilder.append(convertStringSource(key));
						sourceBuilder.append("\", ");
						transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					}
					for (Parsed arg : ((Call) part).arguments) {
						sourceBuilder.append(", ");
						transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
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
					transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					for (Parsed arg : ((Call) part).arguments) {
						sourceBuilder.append(", ");
						transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					}
					sourceBuilder.append(")");
					return;
				}
				
				throw new UnsupportedOperationException("Cannot compile call: " + describe(reference));
			}
			
			if(addDebugging) {
				java.lang.String source = convertStringSource(reference.toSimpleSource());
				if(atTop) {
					sourceBuilder.appendln("try {");
					sourceBuilder.indent();
					sourceBuilder.append("function = (BaseFunction)");
					transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					sourceBuilder.appendln(";");
					sourceBuilder.unindent();
					sourceBuilder.appendln("} catch(ClassCastException ex) {");
					sourceBuilder.append("\tthrow new net.nexustools.njs.Error.JavaException(\"ReferenceError\", \"");
					sourceBuilder.append(source);
					sourceBuilder.appendln(" is not a function\");");
					sourceBuilder.appendln("}");
					sourceBuilder.append("function.call(Undefined.INSTANCE");
				} else {
					sourceBuilder.append("callTop(");
					sourceBuilder.append("\"");
					sourceBuilder.append(source);
					sourceBuilder.append("\", ");
					transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					sourceBuilder.append(", ");
					sourceBuilder.append("Undefined.INSTANCE");
				}
				for (Parsed arg : ((Call) part).arguments) {
					sourceBuilder.append(", ");
					transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				}
				sourceBuilder.append(")");
			} else {
				sourceBuilder.append("((BaseFunction)");
				transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				sourceBuilder.append(").call(");
				sourceBuilder.append("Undefined.INSTANCE");
				for (Parsed arg : ((Call) part).arguments) {
					sourceBuilder.append(", ");
					transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				}
				sourceBuilder.append(")");
			}
			return;
		} else if(part instanceof InstanceOf) {
			sourceBuilder.append("(");
			transpileParsedSource(sourceBuilder, ((InstanceOf)part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(".instanceOf((BaseFunction)");
			transpileParsedSource(sourceBuilder, ((InstanceOf)part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
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
			generateLocalStackAccess(sourceBuilder, ((Reference) part).ref, baseScope, localStack);
			return;
		} else if (part instanceof DoubleShiftLeft) {
			Parsed lhs = ((DoubleShiftLeft)part).lhs;
			Parsed rhs = ((DoubleShiftLeft)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, "<<<", methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, !atTop);
			return;
		} else if (part instanceof DoubleShiftRight) {
			Parsed lhs = ((DoubleShiftRight)part).lhs;
			Parsed rhs = ((DoubleShiftRight)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, ">>>", methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, !atTop);
			return;
		} else if (part instanceof ShiftLeft) {
			Parsed lhs = ((ShiftLeft)part).lhs;
			Parsed rhs = ((ShiftLeft)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, "<<", methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, !atTop);
			return;
		} else if (part instanceof ShiftRight) {
			Parsed lhs = ((ShiftRight)part).lhs;
			Parsed rhs = ((ShiftRight)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, ">>", methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, !atTop);
			return;
		} else if (part instanceof Plus) {
			Parsed lhs = ((Plus)part).lhs;
			Parsed rhs = ((Plus)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, '+', methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, !atTop);
			return;
		} else if (part instanceof Multiply) {
			Parsed lhs = ((Multiply)part).lhs;
			Parsed rhs = ((Multiply)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, '*', methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, !atTop);
			return;
		} else if (part instanceof Divide) {
			Parsed lhs = ((Divide)part).lhs;
			Parsed rhs = ((Divide)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, '/', methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, !atTop);
			return;
		} else if (part instanceof And) {
			Parsed lhs = ((And)part).lhs;
			Parsed rhs = ((And)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, '&', methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, !atTop);
			return;
		} else if (part instanceof Or) {
			Parsed lhs = ((Or)part).lhs;
			Parsed rhs = ((Or)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, '|', methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, !atTop);
			return;
		} else if (part instanceof Percent) {
			Parsed lhs = ((Percent)part).lhs;
			Parsed rhs = ((Percent)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, '%', methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, !atTop);
			return;
		} else if (part instanceof Minus) {
			Parsed lhs = ((Minus)part).lhs;
			Parsed rhs = ((Minus)part).rhs;
			generateMath(sourceBuilder, lhs, rhs, '-', methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, !atTop);
			return;
		} else if (part instanceof New) {
			boolean addComma;
			if(addDebugging) {
				addComma = true;
				sourceBuilder.append("constructTop(\"");
				sourceBuilder.append(convertStringSource(((New)part).reference.toSimpleSource()));
				sourceBuilder.append("\", ");
				transpileParsedSource(sourceBuilder, ((New) part).reference, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			} else {
				addComma = false;
				sourceBuilder.append("((BaseFunction)");
				transpileParsedSource(sourceBuilder, ((New) part).reference, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				sourceBuilder.append(").construct(");
			}
			if (((New) part).arguments != null) {
				for (Parsed arg : ((New) part).arguments) {
					if (addComma)
						sourceBuilder.append(", ");
					else
						addComma = true;
					transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				}
			}
			sourceBuilder.append(")");
			return;
		} else if(part instanceof RightReference) {
			transpileParsedSource(sourceBuilder, ((RightReference) part).ref, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			for(java.lang.String key : ((RightReference)part).chain) {
				sourceBuilder.append(".get(");
				generateStringNumberIndex(sourceBuilder, key);
				sourceBuilder.append(")");
			}
			return;
		} else if(part instanceof Throw) {
			sourceBuilder.append("throw new net.nexustools.njs.Error.Thrown(");
			transpileParsedSource(sourceBuilder, ((Throw) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(")");
			return;
		} else if(part instanceof Function) {
			sourceBuilder.appendln("new CompiledFunction(global) {");
			sourceBuilder.indent();
			ScopeOptimizer variableScope = new ScopeOptimizer();
			try {
				for(java.lang.String arg : ((Function)part).arguments)
					if(RESTRICTED_PATTERN.matcher(arg).matches())
						throw new CannotOptimize("Encountered: " + arg);
				scanScriptSource(((Function)part).impl, variableScope);
				((Function)part).impl.optimizations = variableScope.scope;
			} catch(CannotOptimize ex) {
				if(DEBUG || ex instanceof CannotOptimizeUnimplemented)
					ex.printStackTrace(System.out);
			}
			if(((Function)part).impl.optimizations == null) {
				sourceBuilder.appendln("Scope extendScope(BaseObject _this) {");
				sourceBuilder.append("\treturn ");
				sourceBuilder.append(baseScope);
				sourceBuilder.appendln(".extend(_this);");
				sourceBuilder.appendln("}");
			}
			((Function)part).impl.callee = ((Function)part);
			transpileScriptSource(sourceBuilder, ((Function)part).impl, methodPrefix, fileName, SourceScope.NewFunction);
			sourceBuilder.unindent();
			sourceBuilder.append("}");
			return;
		} else if(part instanceof Var) {
			List<Var.Set> sets = ((Var)part).sets;
			java.lang.Boolean first = true;
			for(Var.Set set : sets) {
				if(first)
					first = false;
				else
					sourceBuilder.appendln(";");
				java.lang.String type = expectedStack == null ? null : expectedStack.get(set.lhs);
				if(type != null) {
					localStack.put(set.lhs, type);
					
					if(type.equals("string"))
						sourceBuilder.append("String");
					else if(type.equals("boolean"))
						sourceBuilder.append("boolean");
					else if(type.equals("number"))
						sourceBuilder.append("double");
					else if(type.equals("array"))
						sourceBuilder.append("GenericArray");
					else if(type.equals("object"))
						sourceBuilder.append("GenericObject");
					else
						sourceBuilder.append("BaseObject");
					sourceBuilder.append(" ");
					sourceBuilder.append(set.lhs);
					sourceBuilder.append(" = ");
					
					if(type.equals("string")) {
						if(set.rhs == null)
							sourceBuilder.append("\"undefined\"");
						else
							generateStringSource(sourceBuilder, set.rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					} else if(type.equals("boolean")) {
						if(set.rhs == null)
							sourceBuilder.append("false");
						else
							generateBooleanSource(sourceBuilder, set.rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					} else if(type.equals("number")) {
						if(set.rhs == null)
							sourceBuilder.append("0");
						else
							generateNumberSource(sourceBuilder, set.rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					} else if(set.rhs == null)
						sourceBuilder.append("Undefined.INSTANCE");
					else
						transpileParsedSource(sourceBuilder, set.rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					
				} else {
					sourceBuilder.append(baseScope);
					sourceBuilder.append(".var(\"");
					sourceBuilder.append(convertStringSource(set.lhs));
					sourceBuilder.append("\", ");
					if(set.rhs == null)
						sourceBuilder.append("Undefined.INSTANCE");
					else
						transpileParsedSource(sourceBuilder, set.rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					sourceBuilder.append(")");
				}
			}
			return;
		} else if(part instanceof OrOr) {
			sourceBuilder.append("orOr(");
			transpileParsedSource(sourceBuilder, ((OrOr) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(", ");
			transpileParsedSource(sourceBuilder, ((OrOr) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(")");
			return;
		} else if(part instanceof AndAnd) {
			sourceBuilder.append("(andAnd(");
			transpileParsedSource(sourceBuilder, ((AndAnd) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(", ");
			transpileParsedSource(sourceBuilder, ((AndAnd) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
			return;
		} else if(part instanceof Equals) {
			sourceBuilder.append("(");
			java.lang.String ltype = ((Equals)part).lhs.primaryType();
			java.lang.String rtype = ((Equals)part).rhs.primaryType();
			if(ltype.equals(rtype)) {
				if(generateCommonComparison(sourceBuilder, ltype, false, ((Equals)part).lhs, ((Equals)part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap)) {
					sourceBuilder.append(" ? global.Boolean.TRUE : global.Boolean.FALSE)");
					return;
				}
			}
			
			transpileParsedSource(sourceBuilder, ((Equals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(".equals(");
			transpileParsedSource(sourceBuilder, ((Equals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
			return;
		} else if(part instanceof NotEquals) {
			sourceBuilder.append("(");
			java.lang.String ltype = ((NotEquals)part).lhs.primaryType();
			java.lang.String rtype = ((NotEquals)part).rhs.primaryType();
			if(ltype.equals(rtype)) {
				if(generateCommonComparison(sourceBuilder, ltype, true, ((NotEquals)part).lhs, ((NotEquals)part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap)) {
					sourceBuilder.append(" ? global.Boolean.TRUE : global.Boolean.FALSE)");
					return;
				}
			}
			
			transpileParsedSource(sourceBuilder, ((NotEquals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(".equals(");
			transpileParsedSource(sourceBuilder, ((NotEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(") ? global.Boolean.FALSE : global.Boolean.TRUE)");
			return;
		} else if(part instanceof StrictEquals) {
			sourceBuilder.append("(");
			java.lang.String ltype = ((StrictEquals)part).lhs.primaryType();
			java.lang.String rtype = ((StrictEquals)part).rhs.primaryType();
			if(ltype.equals(rtype)) {
				if(generateCommonComparison(sourceBuilder, ltype, false, ((StrictEquals)part).lhs, ((StrictEquals)part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap)) {
					sourceBuilder.append(" ? global.Boolean.TRUE : global.Boolean.FALSE)");
					return;
				}
			}
			
			sourceBuilder.append("((BaseObject)");
			transpileParsedSource(sourceBuilder, ((StrictEquals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(" == (BaseObject)");
			transpileParsedSource(sourceBuilder, ((StrictEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
			return;
		} else if(part instanceof StrictNotEquals) {
			sourceBuilder.append("(");
			java.lang.String ltype = ((StrictNotEquals)part).lhs.primaryType();
			java.lang.String rtype = ((StrictNotEquals)part).rhs.primaryType();
			if(ltype.equals(rtype)) {
				if(generateCommonComparison(sourceBuilder, ltype, true, ((StrictNotEquals)part).lhs, ((StrictNotEquals)part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap)) {
					sourceBuilder.append(" ? global.Boolean.TRUE : global.Boolean.FALSE)");
					return;
				}
			}
			
			sourceBuilder.append("((BaseObject)");
			transpileParsedSource(sourceBuilder, ((StrictNotEquals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(" == (BaseObject)");
			transpileParsedSource(sourceBuilder, ((StrictNotEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(") ? global.Boolean.FALSE : global.Boolean.TRUE)");
			return;
		} else if(part instanceof ReferenceChain) {
			generateLocalStackAccess(sourceBuilder, ((ReferenceChain)part).chain.remove(0), baseScope, localStack);
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
			
			transpileParsedSource(sourceBuilder, ((IntegerReference) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(".get(");
			sourceBuilder.append(java.lang.String.valueOf(key));
			sourceBuilder.append(")");
			return;
		} else if(part instanceof Not) {
			sourceBuilder.append("global.wrap(!");
			generateBooleanSource(sourceBuilder, ((Not) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(")");
			return;
		} else if(part instanceof OpenArray) {
			boolean first = true;
			sourceBuilder.append("new GenericArray(global");
			if(!((OpenArray)part).entries.isEmpty()) {
				sourceBuilder.append(", new BaseObject[]{");
				for(Parsed subpart : ((OpenArray)part).entries) {
					if(first)
						first = false;
					else
						sourceBuilder.append(", ");
					transpileParsedSource(sourceBuilder, subpart, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				}
				sourceBuilder.append("}");
			}
			sourceBuilder.append(")");
			return;
		} else if(part instanceof Set) {
			Parsed lhs = ((Set)part).lhs;
			Parsed rhs = ((Set)part).rhs;
			
			if(lhs instanceof IntegerReference) {
				if(atTop) {
					sourceBuilder.append(baseScope);
					sourceBuilder.append(".set(");
					sourceBuilder.append(java.lang.String.valueOf(((IntegerReference)lhs).ref));
					sourceBuilder.append(", ");
					transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					sourceBuilder.append(")");
					return;
				}
				
				sourceBuilder.append("callSet(");
				transpileParsedSource(sourceBuilder, ((IntegerReference)lhs).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				sourceBuilder.append(", ");
				sourceBuilder.append(java.lang.String.valueOf(((IntegerReference)lhs).ref));
				sourceBuilder.append(", ");
				transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				sourceBuilder.append(")");
				return;
			} else if(lhs instanceof VariableReference) {
				if(isNumber(((VariableReference)lhs).ref, localStack)) {
					sourceBuilder.append("Utilities.set(");
					transpileParsedSource(sourceBuilder, ((VariableReference)lhs).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					sourceBuilder.append(", ");
					generateNumberSource(sourceBuilder, ((VariableReference)lhs).ref, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					sourceBuilder.append(", ");
					transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					sourceBuilder.append(")");
				} else {
					sourceBuilder.append("Utilities.set(");
					transpileParsedSource(sourceBuilder, ((VariableReference)lhs).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					sourceBuilder.append(", ");
					transpileParsedSource(sourceBuilder, ((VariableReference)lhs).ref, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					sourceBuilder.append(", ");
					transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					sourceBuilder.append(")");
				}
				return;
			} else if(lhs instanceof Reference) {
				if(localStack != null) {
					java.lang.String type = localStack.get(((Reference)lhs).ref);
					if(type != null) {
						sourceBuilder.append(((Reference)lhs).ref);
						sourceBuilder.append(" = ");
						if(type.equals("string")) {
							generateStringSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
						} else if(type.equals("number")) {
							generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
						} else if(type.equals("boolean")) {
							generateBooleanSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
						} else
							transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
						return;
					}
				}
				
				if(atTop) {
					sourceBuilder.append(baseScope);
					sourceBuilder.append(".set(\"");
					sourceBuilder.append(convertStringSource(((Reference)lhs).ref));
					sourceBuilder.append("\", ");
					transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					sourceBuilder.append(")");
					return;
				}
				
				sourceBuilder.append("callSet(");
				sourceBuilder.append(baseScope);
				sourceBuilder.append(", \"");
				sourceBuilder.append(convertStringSource(((Reference)lhs).ref));
				sourceBuilder.append("\", ");
				transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				sourceBuilder.append(")");
				return;
			} else if(lhs instanceof ReferenceChain) {
				List<java.lang.String> chain = ((ReferenceChain)lhs).chain;
				if(chain.size() > 1) {
					java.lang.String key = chain.remove(chain.size()-1);
					if(atTop) {
						generateLocalStackAccess(sourceBuilder, chain.remove(0), baseScope, localStack);
						for(java.lang.String k : chain) {
							sourceBuilder.append(".get(");
							generateStringNumberIndex(sourceBuilder, k);
							sourceBuilder.append(")");
						}
						sourceBuilder.append(".set(\"");
						sourceBuilder.append(convertStringSource(key));
						sourceBuilder.append("\", ");
						transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
						sourceBuilder.append(")");
						return;
					}
					
					sourceBuilder.append("callSet(");
					generateLocalStackAccess(sourceBuilder, chain.remove(0), baseScope, localStack);
					for(java.lang.String k : chain) {
						sourceBuilder.append(".get(");
						generateStringNumberIndex(sourceBuilder, k);
						sourceBuilder.append(")");
					}
					sourceBuilder.append(", \"");
					sourceBuilder.append(convertStringSource(key));
					sourceBuilder.append("\", ");
					transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					sourceBuilder.append(")");
					return;
				}
			}

			throw new UnsupportedOperationException("Cannot compile set: " + describe(lhs));
		} else if(part instanceof Try) {
			Catch c = ((Try)part).c;
			Finally f = ((Try)part).f;
			if(c != null && f != null) {
				sourceBuilder.appendln("try {");
				sourceBuilder.indent();
				generateBlockSource(sourceBuilder, ((Try)part).impl, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				sourceBuilder.unindent();
				sourceBuilder.appendln("} catch(net.nexustools.njs.Error.InvisibleException ex) {");
				sourceBuilder.appendln("\tthrow ex;");
				sourceBuilder.appendln("} catch(Throwable t) {");
				sourceBuilder.indent();
				java.lang.String newScope = extendScope(baseScope, "catchScope");
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
				sourceBuilder.append(".let(\"");
				sourceBuilder.append(convertStringSource(((Reference)c.condition).ref));
				sourceBuilder.appendln("\", global.wrap(t));");
				generateBlockSource(sourceBuilder, c.impl, methodPrefix, newScope, fileName, localStack, expectedStack, sourceMap);
				sourceBuilder.unindent();
				sourceBuilder.appendln("} finally {");
				sourceBuilder.append("\t");
				sourceBuilder.append(newScope);
				sourceBuilder.appendln(".exit();");
				sourceBuilder.appendln("}");
				sourceBuilder.unindent();
				sourceBuilder.appendln("} finally {");
				sourceBuilder.indent();
				generateBlockSource(sourceBuilder, f.impl, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				sourceBuilder.unindent();
				sourceBuilder.appendln("}");
				return;
			} else if(c != null) {
				sourceBuilder.appendln("try {");
				sourceBuilder.indent();
				generateBlockSource(sourceBuilder, ((Try)part).impl, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
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
				sourceBuilder.append(".let(\"");
				sourceBuilder.append(convertStringSource(((Reference)c.condition).ref));
				sourceBuilder.appendln("\", global.wrap(t));");
				generateBlockSource(sourceBuilder, c.impl, methodPrefix, newScope, fileName, localStack, expectedStack, sourceMap);
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
			generateBlockSource(sourceBuilder, ((Try)part).impl, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.unindent();
			sourceBuilder.appendln("} finally {");
			sourceBuilder.indent();
			generateBlockSource(sourceBuilder, f.impl, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.unindent();
			sourceBuilder.appendln("}");
			return;
		} else if(part instanceof If) {
			if(((If)part).simpleimpl != null) {
				sourceBuilder.append("if(");
				generateBooleanSource(sourceBuilder, ((If)part).condition, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				sourceBuilder.appendln(") {");
				sourceBuilder.indent();
				transpileParsedSource(sourceBuilder, ((If)part).simpleimpl, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, true);
				sourceBuilder.appendln(";");
				sourceBuilder.unindent();
				sourceBuilder.append("}");
				
				generateIfBlockSource(sourceBuilder, ((If)part).el, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				return;
			}

			sourceBuilder.append("if(");
			generateBooleanSource(sourceBuilder, ((If)part).condition, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.appendln(") {");
			sourceBuilder.indent();
			generateBlockSource(sourceBuilder, ((If)part).impl, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.unindent();
			sourceBuilder.append("}");

			generateIfBlockSource(sourceBuilder, ((If)part).el, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			return;
		} else if(part instanceof While) {
			if(((While)part).simpleimpl != null) {
				sourceBuilder.append("while(");
				generateBooleanSource(sourceBuilder, ((While)part).condition, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				sourceBuilder.appendln(") {");
				sourceBuilder.indent();
				transpileParsedSource(sourceBuilder, ((While)part).simpleimpl, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, true);
				sourceBuilder.appendln(";");
				sourceBuilder.append("}");
				return;
			}

			
			sourceBuilder.append("while(");
			generateBooleanSource(sourceBuilder, ((While)part).condition, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.appendln(") {");
			sourceBuilder.indent();
			generateBlockSource(sourceBuilder, ((While)part).impl, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.unindent();
			sourceBuilder.appendln("}");
			return;
		} else if(part instanceof For) {
			if(((For)part).simpleimpl != null) {
				switch(((For)part).type) {
					case InLoop:
						sourceBuilder.appendln("{");
						sourceBuilder.indent();
						sourceBuilder.append("Iterator<java.lang.String> it = ");
						transpileParsedSource(sourceBuilder, ((For)part).loop, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
						sourceBuilder.appendln(".deepPropertyNameIterator();");
						sourceBuilder.appendln("while(it.hasNext()) {");
						sourceBuilder.indent();
						sourceBuilder.append(baseScope);
						sourceBuilder.append(".var(\"");
						sourceBuilder.append(((Var)((For)part).init).sets.get(0).lhs);
						sourceBuilder.appendln("\", global.String.wrap(it.next()));");
						transpileParsedSource(sourceBuilder, ((For)part).simpleimpl, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
						sourceBuilder.appendln(";");
						sourceBuilder.unindent();
						sourceBuilder.appendln("}");
						sourceBuilder.unindent();
						sourceBuilder.append("}");
						break;
						
					case OfLoop:
						sourceBuilder.append("for(BaseObject forObject : ");
						transpileParsedSource(sourceBuilder, ((For)part).loop, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
						sourceBuilder.appendln(") {");
						sourceBuilder.indent();
						sourceBuilder.append(baseScope);
						sourceBuilder.append(".var(\"");
						sourceBuilder.append(((Var)((For)part).init).sets.get(0).lhs);
						sourceBuilder.appendln("\", forObject);");
						transpileParsedSource(sourceBuilder, ((For)part).simpleimpl, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
						sourceBuilder.appendln(";");
						sourceBuilder.unindent();
						sourceBuilder.append("}");
						break;
						
					case Standard:
						if(((For)part).init != null) {
							transpileParsedSource(sourceBuilder, ((For)part).init, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
							sourceBuilder.appendln(";");
						}
						sourceBuilder.append("for(; ");
						generateBooleanSource(sourceBuilder, ((For)part).condition, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
						sourceBuilder.append("; ");
						if(((For)part).loop != null)
							transpileParsedSource(sourceBuilder, ((For)part).loop, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, true);
						sourceBuilder.appendln(") {");
						sourceBuilder.indent();
						transpileParsedSource(sourceBuilder, ((For)part).simpleimpl, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, true);
						sourceBuilder.appendln(";");
						sourceBuilder.unindent();
						sourceBuilder.append("}");
				}
					
				return;
			}
			
			switch(((For)part).type) {
				case InLoop:
				{
					sourceBuilder.appendln("{");
					sourceBuilder.indent();
					java.lang.String scope;
					boolean let = ((For)part).init instanceof Let;
					if(let) {
						scope = extendScope(baseScope, "letScope");
						sourceBuilder.append("final Scope ");
						sourceBuilder.append(scope);
						sourceBuilder.append(" = ");
						sourceBuilder.append(baseScope);
						sourceBuilder.appendln(".beginBlock();");
					} else
						scope = baseScope;
					sourceBuilder.append("Iterator<java.lang.String> it = ");
					transpileParsedSource(sourceBuilder, ((For)part).loop, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					sourceBuilder.appendln(".deepPropertyNameIterator();");
					sourceBuilder.appendln("while(it.hasNext()) {");
					sourceBuilder.indent();
					sourceBuilder.append(scope);
					if(let)
						sourceBuilder.append(".let(\"");
					else
						sourceBuilder.append(".var(\"");
					sourceBuilder.append(((Var)((For)part).init).sets.get(0).lhs);
					sourceBuilder.appendln("\", global.String.wrap(it.next()));");
					generateBlockSource(sourceBuilder, ((For)part).impl, methodPrefix, scope, fileName, localStack, expectedStack, sourceMap);
					sourceBuilder.unindent();
					sourceBuilder.appendln("}");
					sourceBuilder.unindent();
					sourceBuilder.append("}");
					break;
				}

				case OfLoop:
				{
					java.lang.String scope;
					boolean let = ((For)part).init instanceof Let;
					if(let) {
						sourceBuilder.appendln("{");
						sourceBuilder.indent();
						scope = extendScope(baseScope, "letScope");
						sourceBuilder.append("final Scope ");
						sourceBuilder.append(scope);
						sourceBuilder.append(" = ");
						sourceBuilder.append(baseScope);
						sourceBuilder.appendln(".beginBlock();");
					} else
						scope = baseScope;
					sourceBuilder.append("for(BaseObject forObject : ");
					transpileParsedSource(sourceBuilder, ((For)part).loop, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					sourceBuilder.appendln(") {");
					sourceBuilder.indent();
					sourceBuilder.append(scope);
					if(let)
						sourceBuilder.append(".let(\"");
					else
						sourceBuilder.append(".var(\"");
					sourceBuilder.append(((Var)((For)part).init).sets.get(0).lhs);
					sourceBuilder.appendln("\", forObject);");
					generateBlockSource(sourceBuilder, ((For)part).impl, methodPrefix, scope, fileName, localStack, expectedStack, sourceMap);
					sourceBuilder.unindent();
					sourceBuilder.append("}");
					if(let) {
						sourceBuilder.appendln();
						sourceBuilder.unindent();
						sourceBuilder.append("}");
					}
					break;
				}

				case Standard:
					if(((For)part).init != null) {
						transpileParsedSource(sourceBuilder, ((For)part).init, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
						sourceBuilder.appendln(";");
					}
					sourceBuilder.append("for(; ");
					generateBooleanSource(sourceBuilder, ((For)part).condition, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					sourceBuilder.append("; ");
					if(((For)part).loop != null)
						transpileParsedSource(sourceBuilder, ((For)part).loop, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap, true);
					sourceBuilder.appendln(") {");
					sourceBuilder.indent();
					generateBlockSource(sourceBuilder, ((For)part).impl, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					sourceBuilder.unindent();
					sourceBuilder.append("}");
			}
			
			return;
		} else if(part instanceof PlusPlus) {
			Parsed ref = ((PlusPlus)part).ref;
			
			if(localStack != null) {
				if(ref instanceof Reference) {
					if(!atTop)
						sourceBuilder.append("global.wrap(");
					if(!((PlusPlus)part).right)
						sourceBuilder.append("++");
					sourceBuilder.append(((Reference)ref).ref);
					if(((PlusPlus)part).right)
						sourceBuilder.append("++");
					if(!atTop)
						sourceBuilder.append(")");
				} else
					throw new UnsupportedOperationException("Cannot compile x++: " + describe(ref));
				return;
			}
			
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
		} else if(part instanceof PlusEq) {
			Parsed ref = ((PlusEq)part).lhs;
			Parsed rhs = ((PlusEq)part).rhs;
			
			if(localStack != null) {
				// TODO: Fix logic
				if(ref instanceof Reference) {
					if(!atTop)
						sourceBuilder.append("global.wrap(");
					sourceBuilder.append(((Reference)ref).ref);
					sourceBuilder.append(" += ");
					generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
					if(!atTop)
						sourceBuilder.append(")");
				} else
					throw new UnsupportedOperationException("Cannot compile x++: " + describe(ref));
				return;
			}
			
			sourceBuilder.append("plusEqual(global, ");
			if(ref instanceof Reference) {
				sourceBuilder.append("\"");
				sourceBuilder.append(convertStringSource(((Reference) ref).ref));
				sourceBuilder.append("\", ");
				generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				sourceBuilder.append(", ");
				sourceBuilder.append(baseScope);
			} else
				throw new UnsupportedOperationException("Cannot compile x++: " + describe(ref));
			sourceBuilder.append(")");
			return;
		} else if(part instanceof MinusMinus) {
			Parsed ref = ((MinusMinus)part).ref;
			
			if(localStack != null) {
				if(ref instanceof Reference) {
					if(!atTop)
						sourceBuilder.append("global.wrap(");
					if(!((MinusMinus)part).right)
						sourceBuilder.append("--");
					sourceBuilder.append(((Reference)ref).ref);
					if(((MinusMinus)part).right)
						sourceBuilder.append("--");
					if(!atTop)
						sourceBuilder.append(")");
				} else
					throw new UnsupportedOperationException("Cannot compile x--: " + describe(ref));
				return;
			}
			
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
				sourceBuilder.append("setDirectly(\"");
				sourceBuilder.append(convertStringSource(entry.getKey()));
				sourceBuilder.append("\", ");
				transpileParsedSource(sourceBuilder, entry.getValue(), methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				sourceBuilder.appendln(");");
			}
			sourceBuilder.unindent();
			sourceBuilder.appendln("}");
			sourceBuilder.unindent();
			sourceBuilder.appendln("}");
			return;
		} else if(part instanceof Delete) {
			if(atTop) {
				generateBooleanSource(sourceBuilder, part, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
				return;
			}
			
			Parsed rhs = ((Delete)part).rhs;
			if(rhs instanceof Reference) {
				if(localStack.get(((Reference)rhs).ref) != null)
					sourceBuilder.append("global.Boolean.FALSE");
				else {
					sourceBuilder.append("(");
					sourceBuilder.append(baseScope);
					sourceBuilder.append(".delete(\"");
					sourceBuilder.append(convertStringSource(((Reference)rhs).ref));
					sourceBuilder.append("\") ? global.Boolean.TRUE : global.Boolean.FALSE)");
				}
				return;
			} else if(rhs instanceof ReferenceChain) {
				java.lang.String first = ((ReferenceChain)rhs).chain.remove(0);
				java.lang.String last = ((ReferenceChain)rhs).chain.remove(((ReferenceChain)rhs).chain.size()-1);
				
				sourceBuilder.append("(");
				generateLocalStackAccess(sourceBuilder, first, baseScope, localStack);
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
			sourceBuilder.append("(moreThan(");
			transpileParsedSource(sourceBuilder, ((MoreThan) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(", ");
			transpileParsedSource(sourceBuilder, ((MoreThan) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
			return;
		} else if(part instanceof LessThan) {
			sourceBuilder.append("(lessThan(");
			transpileParsedSource(sourceBuilder, ((LessThan) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(", ");
			transpileParsedSource(sourceBuilder, ((LessThan) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
			return;
		} else if(part instanceof MoreEqual) {
			sourceBuilder.append("(moreEqual(");
			transpileParsedSource(sourceBuilder, ((MoreEqual) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(", ");
			transpileParsedSource(sourceBuilder, ((MoreEqual) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
			return;
		} else if(part instanceof LessEqual) {
			sourceBuilder.append("(lessEqual(");
			transpileParsedSource(sourceBuilder, ((LessEqual) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(", ");
			transpileParsedSource(sourceBuilder, ((LessEqual) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
			return;
		} else if(part instanceof VariableReference) {
			Parsed ref = ((VariableReference)part).ref;
			if(localStack != null) {
				if(ref instanceof BaseReferency) {
					if(ref instanceof Reference) {
						java.lang.String type = localStack.get(((Reference)ref).ref);
						if(type != null) {
							if(type.equals("number")) {
								sourceBuilder.append("Utilities.get(");
								transpileParsedSource(sourceBuilder, ((VariableReference)part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
								sourceBuilder.append(", ");
								generateNumberSource(sourceBuilder, ((VariableReference)part).ref, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
								sourceBuilder.append(")");
								return;
							} else if(type.equals("string")) {
								transpileParsedSource(sourceBuilder, ((VariableReference)part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
								sourceBuilder.append(".get(");
								sourceBuilder.append(((Reference)ref).ref);
								sourceBuilder.append(")");
								return;
							}
						}
					} else
						throw new UnsupportedOperationException("Cannot compile optimized: " + describe(ref));
				}
			}
			sourceBuilder.append("Utilities.get(");
			transpileParsedSource(sourceBuilder, ((VariableReference)part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
			sourceBuilder.append(", ");
			transpileParsedSource(sourceBuilder, ((VariableReference)part).ref, methodPrefix, baseScope, fileName, localStack, expectedStack, sourceMap);
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
		NewFunction,
		Function;

		private boolean isFunction() {
			return this == GlobalFunction || this == NewFunction || this == Function;
		}
		private boolean isGlobal() {
			return this == GlobalFunction || this == GlobalScript;
		}
	}
	protected void transpileScriptSource(SourceBuilder sourceBuilder, ScriptData script, java.lang.String methodPrefix, java.lang.String fileName, SourceScope scope) {
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
		LocalStack localStack = script.optimizations == null ? null : new LocalStack();
		if (!scope.isGlobal()) {
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
			if(script.optimizations == null)
				sourceBuilder.appendln("\tfinal Scope baseScope = extendScope(_this);");
			if(script.callee != null) {
				methodPrefix = extendMethodChain(methodPrefix, script.callee.name);
				List<java.lang.String> arguments = script.callee.arguments;
				if(script.optimizations == null)
					sourceBuilder.appendln("\tbaseScope.var(\"arguments\", new Arguments(global, this, params));");
				else {
					localStack.put("this", "unknown");
					if(!arguments.contains("arguments")) {
						sourceBuilder.appendln("\tBaseObject arguments = new Arguments(global, this, params);");
						localStack.put("arguments", "arguments");
					}
					for(int i=0; i<arguments.size(); i++) {
						sourceBuilder.append("\tBaseObject ");
						sourceBuilder.append(arguments.get(i));
						sourceBuilder.appendln(";");
						localStack.put(arguments.get(i), "argument");
					}
				}
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
							if(script.optimizations == null) {
								sourceBuilder.append("\t\t\tbaseScope.var(\"");
								sourceBuilder.append(convertStringSource(arguments.get(a)));
								sourceBuilder.append("\", params[");
								sourceBuilder.append(java.lang.String.valueOf(a));
								sourceBuilder.appendln("]);");
							} else {
								sourceBuilder.append("\t\t\t");
								sourceBuilder.append(arguments.get(a));
								sourceBuilder.append(" = params[");
								sourceBuilder.append(java.lang.String.valueOf(a));
								sourceBuilder.appendln("];");
							}
						}
						for(; a < arguments.size(); a++) {
							if(script.optimizations == null) {
								sourceBuilder.append("\t\t\tbaseScope.var(\"");
								sourceBuilder.append(convertStringSource(arguments.get(a)));
								sourceBuilder.appendln("\", Undefined.INSTANCE);");
							} else {
								sourceBuilder.append("\t\t\t");
								sourceBuilder.append(arguments.get(a));
								sourceBuilder.appendln(" = Undefined.INSTANCE;");
							}
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
		if(script.optimizations != null) {
			for (Function function : script.functions.values()) {
				sourceBuilder.append(function.uname = toClassName(function.name, false));
				sourceBuilder.append(" ");
				sourceBuilder.append(function.name);
				sourceBuilder.append(" = new ");
				sourceBuilder.append(function.uname);
				sourceBuilder.appendln("(global, baseScope);");
			}
		} else
			for (Function function : script.functions.values()) {
				sourceBuilder.append("baseScope.var(\"");
				sourceBuilder.append(convertStringSource(function.name));
				sourceBuilder.append("\", new ");
				sourceBuilder.append(function.uname = toClassName(function.name, false));
				sourceBuilder.appendln("(global, baseScope));");
			}
		if (addDebugging) {
			sourceBuilder.append("Utilities.mapCall(\"");
			if (methodPrefix != null)
				sourceBuilder.append(methodPrefix);
			sourceBuilder.append("\", \"");
			sourceBuilder.append(fileName);
			sourceBuilder.appendln("\", SOURCE_MAP);");
		}
		sourceBuilder.appendln("BaseFunction function;");
		sourceBuilder.appendln("BaseObject __this;");
		if(script.optimizations == null) {
			sourceBuilder.appendln("baseScope.enter();");
			sourceBuilder.appendln("try {");
			sourceBuilder.indent();
		}
		Map<java.lang.Integer, FilePosition> sourceMap = new LinkedHashMap();
		HashMap<java.lang.String, java.lang.String> expectedStack = (HashMap<java.lang.String, java.lang.String>)script.optimizations;
		if (scope.isFunction()) {
			boolean hasReturn = false, first = true;
			for (Parsed part : script.impl) {
				if (first) {
					first = false;
				} else if (addDebugging && (part.rows > 1 || part.columns > 1))
					addSourceMapEntry(sourceBuilder, sourceMap, part);
				hasReturn = hasReturn || part instanceof Return || part instanceof Throw;
				transpileParsedSource(sourceBuilder, part, methodPrefix, "baseScope", fileName, localStack, expectedStack, sourceMap, true);
				sourceBuilder.appendln(";");
			}

			if (!hasReturn) {
				sourceBuilder.appendln("return Undefined.INSTANCE;");
			}
		} else if (script.impl.length > 0) {
			boolean needReturn = false, atTop = true;
			for (int i = 0; i < script.impl.length; i++) {
				Parsed part = script.impl[i];
				if (addDebugging && i > 0 && (part.rows > 1 || part.columns > 1))
					addSourceMapEntry(sourceBuilder, sourceMap, part);
				if (i == script.impl.length - 1 && !(part instanceof Throw)) {
					if(!(part instanceof Delete) && !(part instanceof Var) && !(part instanceof Try) && !(part instanceof If) && !(part instanceof While) && !(part instanceof For) && !(part instanceof Switch)) {
						sourceBuilder.append("return ");
						atTop = false;
					} else
						needReturn = true;
				}
				transpileParsedSource(sourceBuilder, part, methodPrefix, "baseScope", fileName, localStack, expectedStack, sourceMap, atTop);
				sourceBuilder.appendln(";");
			}
			if(needReturn)
				sourceBuilder.appendln("return Undefined.INSTANCE;");
		} else {
			sourceBuilder.appendln("return Undefined.INSTANCE;");
		}
		if(script.optimizations == null) {
			sourceBuilder.unindent();
			sourceBuilder.appendln("} finally {");
			sourceBuilder.appendln("\tbaseScope.exit();");
			sourceBuilder.appendln("}");
		}
		sourceBuilder.unindent();
		sourceBuilder.appendln("}");
		
		for (Function function : script.functions.values()) {
			java.lang.String functionName;
			if(scope != SourceScope.NewFunction)
				sourceBuilder.append("private static final ");
			sourceBuilder.append("class ");
			if(function.uname == null)
				function.uname = toClassName(function.name, false);
			sourceBuilder.append(functionName = function.uname);
			sourceBuilder.appendln(" extends CompiledFunction {");
			sourceBuilder.indent();
			sourceBuilder.appendln("private final Scope baseScope;");
			ScopeOptimizer variableScope = new ScopeOptimizer();
			try {
				for(java.lang.String arg : function.arguments)
					if(RESTRICTED_PATTERN.matcher(arg).matches())
						throw new CannotOptimize("Encountered: " + arg);
				scanScriptSource(function.impl, variableScope);
				function.impl.optimizations = variableScope.scope;
			} catch(CannotOptimize ex) {
				if(DEBUG || ex instanceof CannotOptimizeUnimplemented)
					ex.printStackTrace(System.out);
			}
			if(script.optimizations == null) {
				sourceBuilder.appendln("Scope extendScope(BaseObject _this) {");
				sourceBuilder.appendln("\treturn baseScope.extend(_this);");
				sourceBuilder.appendln("}");
			}
			sourceBuilder.append("private ");
			sourceBuilder.append(functionName);
			sourceBuilder.appendln("(Global global, Scope scope) {");
			sourceBuilder.appendln("\tsuper(global);");
			sourceBuilder.appendln("\tbaseScope = scope;");
			sourceBuilder.appendln("}");

			transpileScriptSource(sourceBuilder, function.impl, methodPrefix, fileName, scope == SourceScope.NewFunction ? SourceScope.NewFunction : SourceScope.Function);

			sourceBuilder.unindent();
			sourceBuilder.appendln("}");
		}
		
		if(addDebugging) {
			sourceBuilder.appendln("@SuppressWarnings(\"all\")");
			sourceBuilder.append("public");
			if(scope != SourceScope.NewFunction)
				sourceBuilder.append(" static");
			sourceBuilder.append(" final Map<Integer, Utilities.FilePosition> SOURCE_MAP = Collections.unmodifiableMap(new LinkedHashMap()");
			if(!sourceMap.isEmpty()) {
				sourceBuilder.appendln(" {");
				sourceBuilder.appendln("\t{");
				for(Map.Entry<java.lang.Integer, FilePosition> entry : sourceMap.entrySet()) {
					sourceBuilder.append("\t\tput(");
					sourceBuilder.append(""+entry.getKey());
					sourceBuilder.append(", new Utilities.FilePosition(");
					sourceBuilder.append(""+entry.getValue().row);
					sourceBuilder.append(", ");
					sourceBuilder.append(""+entry.getValue().column);
					sourceBuilder.appendln("));");
				}
				sourceBuilder.appendln("\t}");
				sourceBuilder.append("}");
			}
			sourceBuilder.appendln(");");
		}
	}

	protected java.lang.String transpileJavaClassSource(ScriptData script, java.lang.String className, java.lang.String fileName, java.lang.String pkg, boolean inFunction, boolean generateMain) {
		SourceBuilder sourceBuilder = new SourceBuilder();
		sourceBuilder.appendln("/*");
		sourceBuilder.appendln(" * Generated by NJS");
		sourceBuilder.appendln(" * https://nexustools.com/projects/njs");
		sourceBuilder.appendln(" * ");
		sourceBuilder.appendln(" * NJS is licensed under the LGPLv3");
		sourceBuilder.appendln(" * You must use a compatible license");
		sourceBuilder.appendln(" */");
		sourceBuilder.appendln();
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
		sourceBuilder.appendln("import net.nexustools.njs.Utilities;");
		sourceBuilder.appendln("import net.nexustools.njs.Global;");
		sourceBuilder.appendln("import net.nexustools.njs.Scope;");
		sourceBuilder.appendln("import net.nexustools.njs.Null;");
		sourceBuilder.appendln();
		sourceBuilder.appendln("import java.util.Iterator;");
		if(addDebugging) {
			sourceBuilder.appendln("import java.util.Collections;");
			sourceBuilder.appendln("import java.util.LinkedHashMap;");
			sourceBuilder.appendln("import java.util.Map;");
		}
		sourceBuilder.appendln();
		sourceBuilder.appendln();
		sourceBuilder.appendln("@SuppressWarnings(\"all\")");
		sourceBuilder.append("public final class ");
		sourceBuilder.append(className);
		sourceBuilder.append(" extends CompiledScript.");
		sourceBuilder.append(addDebugging ? "Debuggable" : "Optimized");
		sourceBuilder.appendln("{");
		sourceBuilder.indent();

		try {
			if(inFunction)
				scanScriptSource(script);
			transpileScriptSource(sourceBuilder, script, script.methodName, fileName, inFunction ? SourceScope.GlobalFunction : SourceScope.GlobalScript);
		} catch(RuntimeException t) {
			System.err.println(sourceBuilder.toString());
			throw t;
		}
		
		if(generateMain) {
			sourceBuilder.appendln("public static void main(String[] args) {");
			sourceBuilder.appendln("\tGlobal global = Utilities.createExtendedGlobal();");
			sourceBuilder.appendln("\tScope scope = new Scope(global);");
			sourceBuilder.appendln("\tscope.var(\"arguments\", Utilities.convertArguments(global, args));");
			sourceBuilder.append("\tnew ");
			sourceBuilder.append(className);
			sourceBuilder.appendln("().exec(global, scope);");
			sourceBuilder.appendln("}");
		}

		sourceBuilder.unindent();
		sourceBuilder.appendln("}");

		return sourceBuilder.toString();
	}
	
	private java.lang.String extendScope(java.lang.String baseScope, java.lang.String newScope) {
		if(baseScope.equals(newScope)) {
			int count;
			if(baseScope.length() > 10)
				count = java.lang.Integer.valueOf(baseScope.substring(10));
			else
				count = 0;
			return newScope + (count+1);
		} else
			return newScope;
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
		
		try {
			return (Script) JavaSourceCompiler.compileToClass(classPath.replace(".", "/") + ".java", "net.nexustools.njs.gen." + className, source).newInstance();
		} catch (ClassNotFoundException ex) {
			throw new net.nexustools.njs.Error.JavaException("EvalError", ex.toString(), ex);
		} catch (InstantiationException ex) {
			throw new net.nexustools.njs.Error.JavaException("EvalError", ex.toString(), ex);
		} catch (IllegalAccessException ex) {
			throw new net.nexustools.njs.Error.JavaException("EvalError", ex.toString(), ex);
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
			if(arg.startsWith("-") && arg.length() > 1) {
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
