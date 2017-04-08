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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.nexustools.njs.Utilities.FilePosition;

/**
 *
 * @author Katelyn Slater <kate@nexustools.com>
 */
public class JavaTranspiler extends RegexCompiler {

    public static final boolean DEBUG = RegexCompiler.DEBUG;
    public static final boolean DUMP_SOURCE = DEBUG;

    private final Map<java.lang.String, AtomicInteger> GEN_PACKAGE_USED_NAMES = new HashMap();

    private void scanArgument(Parsed argument, ScopeOptimizer variableScope) {
        while (argument instanceof OpenBracket) {
            argument = ((OpenBracket) argument).contents;
        }

        if (argument.isNumberOrBool() || argument instanceof OpenGroup || argument instanceof OpenArray) {
            return;
        }

        if (argument.isString()) {
            variableScope.markUseSyntheticStack();
        }
        if (argument instanceof BaseReferency) {
            if (argument instanceof Reference) {
                variableScope.assertTyped(((Reference) argument).ref, "number", "boolean", "object", "array", "arguments");
            } else if (argument instanceof Call) {
                scanParsedSource(argument, variableScope);
            } else if (argument instanceof RhLh) {
                if (argument instanceof Set) {
                    scanArgument(((RhLh) argument).lhs, variableScope);
                    scanParsedSource(argument, variableScope);
                } else if (argument instanceof OrOr || argument instanceof AndAnd) {
                    scanArgument(((RhLh) argument).lhs, variableScope);
                    scanArgument(((RhLh) argument).rhs, variableScope);
                } else {
                    scanParsedSource(argument, variableScope);
                }
            } else if (argument instanceof AndAnd || argument instanceof OrOr) {
                scanArgument(((RhLh) argument).lhs, variableScope);
                scanArgument(((RhLh) argument).rhs, variableScope);
            } else if (argument instanceof Rh) {
                scanParsedSource(argument, variableScope);
            } else if (argument instanceof Number || argument instanceof Long || argument instanceof Null || argument instanceof Undefined) {
                // IGNORED
            } else if (argument instanceof RightReference || argument instanceof ReferenceChain) {
                variableScope.markUseSyntheticStack();
                scanParsedSource(argument, variableScope);
            } else if(argument instanceof New) {
                for(Parsed arg : ((New)argument).arguments)
                    scanParsedSource(arg, variableScope);
            } else if(argument instanceof String) {
                variableScope.markUseSyntheticStack();
            } else {
                throw new CannotOptimizeUnimplemented("Unhandled argument: " + describe(argument));
            }
        } else if (argument instanceof RhLh) {
            scanParsedSource(((RhLh) argument).lhs, variableScope);
            scanParsedSource(((RhLh) argument).rhs, variableScope);
        } else if (argument instanceof Rh) {
            scanParsedSource(((Rh) argument).rhs, variableScope);
        } else if (argument instanceof Function) {
            scanParsedSource(argument, variableScope);
        } else {
            throw new CannotOptimizeUnimplemented("Unhandled argument: " + describe(argument));
        }
    }

    private static abstract class ClassNameScopeChain {

        java.lang.String toClassName(java.lang.String name) {
            java.lang.String output = name.replaceAll("[^a-zA-Z_]", "_");
            if (!output.equals(name)) {
                output += Math.abs(name.hashCode());
            }
            return toClassName(output, true);
        }

        protected abstract java.lang.String toClassName(java.lang.String name, boolean create);

        public final ClassNameScopeChain extend() {
            return new ExtendedClassNameScopeChain(this);
        }
    }

    private static class ExtendedClassNameScopeChain extends ClassNameScopeChain {

        public final ClassNameScopeChain parent;
        private final Map<java.lang.String, AtomicInteger> used_names = new HashMap();

        public ExtendedClassNameScopeChain(ClassNameScopeChain parent) {
            this.parent = parent;
        }

        @Override
        public java.lang.String toClassName(java.lang.String name, boolean create) {
            java.lang.String output = parent.toClassName(name, false);
            if (output == null) {
                AtomicInteger atomicInteger = used_names.get(name);
                if (atomicInteger == null) {
                    if (!create) {
                        return null;
                    }
                    used_names.put(name, atomicInteger = new AtomicInteger(1));
                }
                int num = atomicInteger.getAndIncrement();
                if (num > 1) {
                    return toClassName(name + "_" + num, true);
                }
                return name;
            } else {
                return output;
            }

        }
    }
    private ClassNameScopeChain BASE_CLASS_NAME_SCOPE_CHAIN = new ClassNameScopeChain() {
        @Override
        public java.lang.String toClassName(java.lang.String name, boolean create) {
            AtomicInteger atomicInteger;
            synchronized (GEN_PACKAGE_USED_NAMES) {
                atomicInteger = GEN_PACKAGE_USED_NAMES.get(name);
                if (atomicInteger == null) {
                    if (!create) {
                        return null;
                    }
                    GEN_PACKAGE_USED_NAMES.put(name, atomicInteger = new AtomicInteger(1));
                }
            }
            int num = atomicInteger.getAndIncrement();
            if (num > 1) {
                return toClassName(name + "_" + num, true);
            }
            return name;
        }
    };

    private static interface StackOptimizations {

        public java.lang.String get(java.lang.String key);

        public ScopeOptimizer.StackType stackType();

        public boolean usesArguments();

        public java.util.Set<java.lang.String> keys();

        public java.lang.String getReference(java.lang.String ref);
    }

    private static class MapStackOptimizations implements StackOptimizations {

        private final boolean usesArguments;
        private final ScopeOptimizer.StackType stackType;
        private final Map<java.lang.String, java.lang.String> map;

        public MapStackOptimizations(Map<java.lang.String, java.lang.String> map, ScopeOptimizer.StackType stackType, boolean usesArguments) {
            this.usesArguments = usesArguments;
            this.stackType = stackType;
            this.map = map;
        }

        @Override
        public boolean usesArguments() {
            return usesArguments;
        }

        @Override
        public java.lang.String get(java.lang.String key) {
            return map.get(key);
        }

        @Override
        public ScopeOptimizer.StackType stackType() {
            return stackType;
        }

        @Override
        public java.util.Set<java.lang.String> keys() {
            return map.keySet();
        }

        @Override
        public java.lang.String getReference(java.lang.String ref) {
            switch (stackType) {
                case SyntheticScopeable:
                case TypedClass:
                    return "localStack." + ref;
                default:
                    return ref;
            }
        }
    }

    private static class ExtendedStackOptimizations extends MapStackOptimizations {

        private final StackOptimizations parent;

        public ExtendedStackOptimizations(StackOptimizations parent, Map<java.lang.String, java.lang.String> map, ScopeOptimizer.StackType stackType, boolean usesArguments) {
            super(map, stackType, usesArguments);
            this.parent = parent;
        }

        @Override
        public java.lang.String get(java.lang.String key) {
            java.lang.String type = super.get(key);
            if (type == null) {
                type = parent.get(key);
                if (type != null && type.startsWith("parent")) {
                    return "parent" + (java.lang.Integer.valueOf(type.substring(6)) + 1);
                }
                return type == null ? null : "parent1";
            }
            return type;
        }

    }

    private java.lang.String extendMethodChain(java.lang.String methodPrefix, java.lang.String methodName) {
        boolean methodPrefixIsntNull = false, methodNameIsNull = methodName == null;
        if (methodPrefix != null) {
            if (methodPrefix.equals("<anonymous>")) {
                if (methodNameIsNull) {
                    return methodPrefix;
                }
                methodPrefix = null;
            } else if (methodPrefix.endsWith(".<anonymous>")) {
                if (methodNameIsNull) {
                    return methodPrefix;
                }
                methodPrefix = methodPrefix.substring(0, methodPrefix.length() - 12);
                methodPrefixIsntNull = true;
            } else if (methodPrefix.isEmpty()) {
                methodPrefix = null;
            } else {
                methodPrefixIsntNull = true;
            }
        }

        if (!methodNameIsNull && methodPrefixIsntNull) {
            return methodPrefix + '.' + methodName;
        } else if (methodPrefixIsntNull) {
            return methodPrefix + '.' + "<anonymous>";
        } else if (!methodNameIsNull) {
            return methodName;
        } else {
            return "<anonymous>";
        }
    }

    private void generateStringSource(SourceBuilder sourceBuilder, Parsed part, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, StackOptimizations expectedStack, Map<java.lang.String, Function> functionMap, ClassNameScopeChain scopeChain, Map<java.lang.Integer, FilePosition> sourceMap) {
        if (part instanceof String) {
            sourceBuilder.append("\"");
            sourceBuilder.append(convertStringSource(((String) part).string));
            sourceBuilder.append("\"");
        } else if (part instanceof TemplateLiteral) {
            sourceBuilder.append("(");
            boolean first = true;
            for(java.lang.Object _part : ((TemplateLiteral)part).parts) {
                if(first)
                    first = false;
                else
                    sourceBuilder.append(" + ");
                if(_part instanceof Parsed)
                    generateStringSource(sourceBuilder, (Parsed)_part, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                else {
                    sourceBuilder.append("\"");
                    sourceBuilder.append(convertStringSource(_part.toString()));
                    sourceBuilder.append("\"");
                }
            }
            sourceBuilder.append(")");
        } else if (part instanceof Plus && ((Plus) part).isStringReferenceChain()) {
            generateStringSource(sourceBuilder, ((Plus) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(" + ");
            generateStringSource(sourceBuilder, ((Plus) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
        } else {
            if (localStack != null) {
                if (part instanceof Reference) {
                    java.lang.String type = localStack.get(((Reference) part).ref);
                    if (type != null) {
                        if (type.equals("string")) {
                            sourceBuilder.append(expectedStack.getReference(((Reference) part).ref));
                            return;
                        } else if (type.equals("number")) {
                            sourceBuilder.append("net.nexustools.njs.Number.toString(");
                            sourceBuilder.append(expectedStack.getReference(((Reference) part).ref));
                            sourceBuilder.append(")");
                            return;
                        } else if (type.equals("boolean")) {
                            sourceBuilder.append("(");
                            sourceBuilder.append(expectedStack.getReference(((Reference) part).ref));
                            sourceBuilder.append(" ? \"true\" : \"false\")");
                            return;
                        }
                    }
                }
            }
            transpileParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(".toString()");
        }
    }

    private void generateNumberSource(SourceBuilder sourceBuilder, Parsed part, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, StackOptimizations expectedStack, Map<java.lang.String, Function> functionMap, ClassNameScopeChain scopeChain, Map<java.lang.Integer, FilePosition> sourceMap) {
        if (part instanceof Number) {
            if(Double.isNaN(((Number) part).value))
                sourceBuilder.append("Double.NaN");
            else if(((Number) part).value == Double.POSITIVE_INFINITY)
                sourceBuilder.append("Double.POSITIVE_INFINITY");
            else if(((Number) part).value == Double.NEGATIVE_INFINITY)
                sourceBuilder.append("Double.NEGATIVE_INFINITY");
            else {
                sourceBuilder.append(java.lang.String.valueOf(((Number) part).value));
                sourceBuilder.append("D");
            }
        } else if (part instanceof Long) {
            sourceBuilder.append(java.lang.String.valueOf(((Long) part).value));
            sourceBuilder.append("D");
        } else if (part instanceof String) {
            try {
                sourceBuilder.append(java.lang.String.valueOf(java.lang.Double.valueOf(((String) part).string)));
            } catch (NumberFormatException ex) {
                sourceBuilder.append("Double.NaN");
            }
        } else if (part instanceof DoubleShiftRight) {
            Parsed lhs = ((DoubleShiftRight) part).lhs;
            Parsed rhs = ((DoubleShiftRight) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, ">>>", methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, MathWrap.Double);
        } else if (part instanceof ShiftLeft) {
            Parsed lhs = ((ShiftLeft) part).lhs;
            Parsed rhs = ((ShiftLeft) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, "<<", methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, MathWrap.Double);
        } else if (part instanceof ShiftRight) {
            Parsed lhs = ((ShiftRight) part).lhs;
            Parsed rhs = ((ShiftRight) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, ">>", methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, MathWrap.Double);
        } else if (part instanceof Plus) {
            Parsed lhs = ((Plus) part).lhs;
            Parsed rhs = ((Plus) part).rhs;
            if(lhs == null)
                generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            else
                generateMath(sourceBuilder, lhs, rhs, '+', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, MathWrap.Double);
        } else if (part instanceof Multiply) {
            Parsed lhs = ((Multiply) part).lhs;
            Parsed rhs = ((Multiply) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '*', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, MathWrap.Double);
        } else if (part instanceof Divide) {
            Parsed lhs = ((Divide) part).lhs;
            Parsed rhs = ((Divide) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '/', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, MathWrap.Double);
        } else if (part instanceof And) {
            Parsed lhs = ((And) part).lhs;
            Parsed rhs = ((And) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '&', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, MathWrap.Double);
        } else if (part instanceof Or) {
            Parsed lhs = ((Or) part).lhs;
            Parsed rhs = ((Or) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '|', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, MathWrap.Double);
        } else if (part instanceof Percent) {
            Parsed lhs = ((Percent) part).lhs;
            Parsed rhs = ((Percent) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '%', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, MathWrap.Double);
        } else if (part instanceof Minus) {
            Parsed lhs = ((Minus) part).lhs;
            Parsed rhs = ((Minus) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '-', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, MathWrap.Double);
        } else if (part instanceof OpenBracket) {
            generateNumberSource(sourceBuilder, ((OpenBracket) part).contents, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
        } else {
            if (localStack != null) {
                if (part instanceof Reference) {
                    java.lang.String type = localStack.get(((Reference) part).ref);
                    if (type != null) {
                        if (type.equals("number")) {
                            sourceBuilder.append(expectedStack.getReference(((Reference) part).ref));
                            return;
                        }
                        if (type.equals("string")) {
                            try {
                                Double.valueOf(((Reference) part).ref);
                                sourceBuilder.append(expectedStack.getReference(((Reference) part).ref));
                                if (!((Reference) part).ref.contains(".")) {
                                    sourceBuilder.append(".0");
                                }
                            } catch (NumberFormatException ex) {
                                sourceBuilder.append("Double.NaN");
                            }
                            return;
                        }
                    }
                } else if (part instanceof ReferenceChain) {
                    List<java.lang.String> chain = ((ReferenceChain) part).chain;
                    java.lang.String type = localStack.get(chain.get(0));
                    if (type != null) {
                        if (type.equals("array") && chain.size() == 2 && chain.get(1).equals("length")) {
                            sourceBuilder.append(chain.get(0));
                            sourceBuilder.append(".length()");
                            return;
                        }
                    }
                }
            }
            sourceBuilder.append("global.Number.fromValueOf(");
            transpileParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(").value");
        }
    }

    private void generateLongSource(SourceBuilder sourceBuilder, Parsed part, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, StackOptimizations expectedStack, Map<java.lang.String, Function> functionMap, ClassNameScopeChain scopeChain, Map<java.lang.Integer, FilePosition> sourceMap) {
        if (part instanceof Number) {
            sourceBuilder.append(java.lang.String.valueOf((long)((Number) part).value));
            sourceBuilder.append("L");
        } else if (part instanceof Long) {
            sourceBuilder.append(java.lang.String.valueOf(((Long) part).value));
            sourceBuilder.append("L");
        } else if (part instanceof String) {
            try {
                java.lang.Double.valueOf(((String) part).string);
                int pos = ((String) part).string.indexOf('.');
                if (pos > -1) {
                    sourceBuilder.append(((String) part).string.substring(0, pos));
                } else {
                    sourceBuilder.append(((String) part).string);
                }
            } catch (NumberFormatException ex) {
                sourceBuilder.append("0");
            }
            sourceBuilder.append("L");
        } else if (part instanceof Plus) {
            Parsed lhs = ((Plus) part).lhs;
            Parsed rhs = ((Plus) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '+', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, MathWrap.Long);
        } else if (part instanceof Multiply) {
            Parsed lhs = ((Multiply) part).lhs;
            Parsed rhs = ((Multiply) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '*', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, MathWrap.Long);
        } else if (part instanceof Divide) {
            Parsed lhs = ((Divide) part).lhs;
            Parsed rhs = ((Divide) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '/', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, MathWrap.Long);
        } else if (part instanceof And) {
            Parsed lhs = ((And) part).lhs;
            Parsed rhs = ((And) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '&', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, MathWrap.Long);
        } else if (part instanceof Or) {
            Parsed lhs = ((Or) part).lhs;
            Parsed rhs = ((Or) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '|', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, MathWrap.Long);
        } else if (part instanceof Percent) {
            Parsed lhs = ((Percent) part).lhs;
            Parsed rhs = ((Percent) part).rhs;
            sourceBuilder.append("(long)");
            generateMath(sourceBuilder, lhs, rhs, '%', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, MathWrap.Long);
        } else if (part instanceof Minus) {
            Parsed lhs = ((Minus) part).lhs;
            Parsed rhs = ((Minus) part).rhs;
            sourceBuilder.append("(long)");
            generateMath(sourceBuilder, lhs, rhs, '-', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, MathWrap.Long);
        } else {
            if (localStack != null) {
                if (part instanceof BaseReferency) {
                    if (part instanceof Reference) {
                        java.lang.String type = localStack.get(((Reference) part).ref);
                        if (type != null && type.equals("number")) {
                            sourceBuilder.append("(long)");
                            sourceBuilder.append(expectedStack.getReference(((Reference) part).ref));
                            return;
                        }
                    } else {
                        throw new UnsupportedOperationException("Cannot compile optimized long : " + describe(part));
                    }
                }
            }

            sourceBuilder.append("(long)");
            sourceBuilder.append("global.Number.fromValueOf(");
            transpileParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(").value");
        }
    }

    private static enum MathWrap {
        BaseObject,
        Long,
        Double,
        String,
        Void
    }

    private void generateMath(SourceBuilder sourceBuilder, Parsed lhs, Parsed rhs, char op, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, StackOptimizations expectedStack, Map<java.lang.String, Function> functionMap, ClassNameScopeChain scopeChain, Map<java.lang.Integer, FilePosition> sourceMap, MathWrap wrap) {
        generateMath(sourceBuilder, lhs, rhs, "" + op, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, wrap);
    }

    private void generateMath(SourceBuilder sourceBuilder, Parsed lhs, Parsed rhs, java.lang.String op, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, StackOptimizations expectedStack, Map<java.lang.String, Function> functionMap, ClassNameScopeChain scopeChain, Map<java.lang.Integer, FilePosition> sourceMap, MathWrap wrap) {
        boolean wrapAsBaseObject = wrap == MathWrap.BaseObject;
        if (op.equals("+") && (wrapAsBaseObject || wrap == MathWrap.String)) {
            if ((lhs instanceof StringReferency && !isNumber(lhs, localStack)) || (rhs instanceof StringReferency && !isNumber(rhs, localStack))) {
                if (lhs instanceof String && ((String) lhs).string.isEmpty()) {
                    if (wrapAsBaseObject) {
                        transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                        sourceBuilder.append("._toString()");
                    } else {
                        generateStringSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    }
                    return;
                }

                if (wrapAsBaseObject) {
                    sourceBuilder.append("global.wrap(");
                } else {
                    sourceBuilder.append("(");
                }
                generateStringSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.append(" + ");
                generateStringSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.append(")");
                return;
            } else if (((lhs instanceof Plus && ((Plus) lhs).isStringReferenceChain()) || (rhs instanceof Plus && ((Plus) rhs).isStringReferenceChain()))) {
                if (wrapAsBaseObject) {
                    sourceBuilder.append("global.wrap(");
                }
                generateStringSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.append(" + ");
                generateStringSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                if (wrapAsBaseObject) {
                    sourceBuilder.append(")");
                }
                return;
            } else if (!(lhs instanceof NumberReferency) && !(rhs instanceof NumberReferency)) {
                if (!wrapAsBaseObject) {
                    sourceBuilder.append("global.Number.fromValueOf(");
                }
                sourceBuilder.append("plus(global, ");
                transpileParsedSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.append(", ");
                transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.append(")");
                if (!wrapAsBaseObject) {
                    sourceBuilder.append(").value");
                }
                return;
            }
        }

        boolean andOrOr = op.equals("|") || op.equals("&") || op.startsWith("<") || op.startsWith(">");
        if (wrapAsBaseObject) {
            sourceBuilder.append("global.wrap(");
        } else {
            switch (wrap) {
                case Long:
                    if (!andOrOr) {
                        sourceBuilder.append("(long)");
                    }
                    break;
                case Double:
                    if (andOrOr) {
                        sourceBuilder.append("(double)");
                    }
                    break;
            }
            sourceBuilder.append("(");
        }
        if (andOrOr) {
            generateLongSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
        } else {
            generateNumberSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
        }
        sourceBuilder.append(" ");
        sourceBuilder.append(op);
        sourceBuilder.append(" ");
        if (andOrOr) {
            generateLongSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
        } else {
            generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
        }
        sourceBuilder.append(")");
    }

    private boolean generateIfBlockSource(SourceBuilder sourceBuilder, Else els, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, StackOptimizations expectedStack, Map<java.lang.String, Function> functionMap, ClassNameScopeChain scopeChain, Map<java.lang.Integer, FilePosition> sourceMap) {
        boolean hasReturn = true, hasElse = false;
        while (els != null) {
            if (els.simpleimpl != null) {
                if (els instanceof ElseIf) {
                    sourceBuilder.append(" else if(");
                    generateBooleanSource(sourceBuilder, els.condition, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    sourceBuilder.appendln(") {");
                } else {
                    hasElse = true;
                    sourceBuilder.appendln(" else {");
                }
                sourceBuilder.indent();
                hasReturn = transpileParsedSource(sourceBuilder, els.simpleimpl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, true) && hasReturn;
                if(!(els.simpleimpl instanceof Block))
                    sourceBuilder.appendln(";");
                else
                    sourceBuilder.appendln();
                sourceBuilder.unindent();
                sourceBuilder.append("}");
            } else {
                if (els instanceof ElseIf) {
                    sourceBuilder.append(" else if(");
                    generateBooleanSource(sourceBuilder, els.condition, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    sourceBuilder.appendln(") {");
                } else {
                    hasElse = true;
                    sourceBuilder.appendln(" else {");
                }
                sourceBuilder.indent();
                hasReturn = generateBlockSource(sourceBuilder, els.impl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap) && hasReturn;
                sourceBuilder.unindent();
                sourceBuilder.append("}");
            }

            if (els instanceof ElseIf) {
                els = ((ElseIf) els).el;
            } else {
                break;
            }
        }
        return hasReturn && hasElse;
    }

    private void generateBooleanSource(SourceBuilder sourceBuilder, Parsed part, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, StackOptimizations expectedStack, Map<java.lang.String, Function> functionMap, ClassNameScopeChain scopeChain, Map<java.lang.Integer, FilePosition> sourceMap) {
        while (part instanceof OpenBracket) {
            part = ((OpenBracket) part).contents;
        }

        if (part instanceof In) {
            transpileParsedSource(sourceBuilder, ((In)part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(".in(");
            generateStringSource(sourceBuilder, ((In)part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(")");
        } else if (part instanceof Boolean) {
            if (((Boolean) part).value) {
                sourceBuilder.append("true");
            } else {
                sourceBuilder.append("false");
            }
        } else if (part instanceof InstanceOf) {
            transpileParsedSource(sourceBuilder, ((InstanceOf) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(".instanceOf((BaseFunction)");
            transpileParsedSource(sourceBuilder, ((InstanceOf) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(")");
        } else if (part instanceof Not) {
            sourceBuilder.append("!");
            generateBooleanSource(sourceBuilder, ((Not) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
        } else if (part instanceof OrOr) {
            generateBooleanSource(sourceBuilder, ((OrOr) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(" || ");
            generateBooleanSource(sourceBuilder, ((OrOr) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
        } else if (part instanceof AndAnd) {
            generateBooleanSource(sourceBuilder, ((AndAnd) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(" && ");
            generateBooleanSource(sourceBuilder, ((AndAnd) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
        } else if (part instanceof Equals) {
            java.lang.String ltype = ((Equals) part).lhs.primaryType();
            java.lang.String rtype = ((Equals) part).rhs.primaryType();
            if (ltype.equals(rtype) && generateCommonComparison(sourceBuilder, ltype, false, ((Equals) part).lhs, ((Equals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap)) {
                return;
            }

            transpileParsedSource(sourceBuilder, ((Equals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(".equals(");
            transpileParsedSource(sourceBuilder, ((Equals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(")");
        } else if (part instanceof NotEquals) {
            java.lang.String ltype = ((NotEquals) part).lhs.primaryType();
            java.lang.String rtype = ((NotEquals) part).rhs.primaryType();
            if (ltype.equals(rtype) && generateCommonComparison(sourceBuilder, ltype, true, ((NotEquals) part).lhs, ((NotEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap)) {
                return;
            }

            sourceBuilder.append("!");
            transpileParsedSource(sourceBuilder, ((NotEquals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(".equals(");
            transpileParsedSource(sourceBuilder, ((NotEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(")");
        } else if (part instanceof StrictEquals) {
            java.lang.String ltype = ((StrictEquals) part).lhs.primaryType();
            java.lang.String rtype = ((StrictEquals) part).rhs.primaryType();
            if (ltype.equals(rtype) && generateCommonComparison(sourceBuilder, ltype, false, ((StrictEquals) part).lhs, ((StrictEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap)) {
                return;
            }

            transpileParsedSource(sourceBuilder, ((StrictEquals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(".strictEquals(");
            transpileParsedSource(sourceBuilder, ((StrictEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(")");
        } else if (part instanceof StrictNotEquals) {
            java.lang.String ltype = ((StrictNotEquals) part).lhs.primaryType();
            java.lang.String rtype = ((StrictNotEquals) part).rhs.primaryType();
            if (ltype.equals(rtype) && generateCommonComparison(sourceBuilder, ltype, true, ((StrictNotEquals) part).lhs, ((StrictNotEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap)) {
                return;
            }

            sourceBuilder.append("!");
            transpileParsedSource(sourceBuilder, ((StrictNotEquals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(".strictEquals(");
            transpileParsedSource(sourceBuilder, ((StrictNotEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(")");
        } else if (part instanceof MoreThan) {
            Parsed lhs = ((MoreThan) part).lhs;
            Parsed rhs = ((MoreThan) part).rhs;

            if (isNumber(lhs, localStack) || isNumber(rhs, localStack)) {
                generateNumberSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.append(" > ");
                generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                return;
            }

            sourceBuilder.append("lessThan(");
            transpileParsedSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(", ");
            transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(")");
        } else if (part instanceof LessThan) {
            Parsed lhs = ((LessThan) part).lhs;
            Parsed rhs = ((LessThan) part).rhs;

            if (isNumber(lhs, localStack) || isNumber(rhs, localStack)) {
                generateNumberSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.append(" < ");
                generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                return;
            }

            sourceBuilder.append("lessThan(");
            transpileParsedSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(", ");
            transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(")");
        } else if (part instanceof MoreEqual) {
            Parsed lhs = ((MoreEqual) part).lhs;
            Parsed rhs = ((MoreEqual) part).rhs;

            if (isNumber(lhs, localStack) || isNumber(rhs, localStack)) {
                generateNumberSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.append(" >= ");
                generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                return;
            }

            sourceBuilder.append("moreEqual(");
            transpileParsedSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(", ");
            transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(")");
        } else if (part instanceof LessEqual) {
            Parsed lhs = ((LessEqual) part).lhs;
            Parsed rhs = ((LessEqual) part).rhs;

            if (isNumber(lhs, localStack) || isNumber(rhs, localStack)) {
                generateNumberSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.append(" <= ");
                generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                return;
            }

            sourceBuilder.append("lessEqual(");
            transpileParsedSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(", ");
            transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(")");
        } else if (part instanceof Delete) {
            Parsed rhs = ((Delete) part).rhs;
            if (rhs instanceof Reference) {
                sourceBuilder.append(baseScope);
                sourceBuilder.append(".delete(\"");
                sourceBuilder.append(convertStringSource(((Reference) rhs).ref));
                sourceBuilder.append("\")");
                return;
            } else if (rhs instanceof ReferenceChain) {
                java.lang.String first = ((ReferenceChain) rhs).chain.remove(0);
                java.lang.String last = ((ReferenceChain) rhs).chain.remove(((ReferenceChain) rhs).chain.size() - 1);

                sourceBuilder.append(baseScope);
                sourceBuilder.append(".get(\"");
                sourceBuilder.append(convertStringSource(first));
                sourceBuilder.append("\")");
                for (java.lang.String key : ((ReferenceChain) rhs).chain) {
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
            if (localStack != null) {
                if (part instanceof Reference) {
                    java.lang.String type = localStack.get(((Reference) part).ref);
                    if (type != null) {
                        if (type.equals("boolean")) {
                            sourceBuilder.append(expectedStack.getReference(((Reference) part).ref));
                            return;
                        } else if (type.equals("string")) {
                            sourceBuilder.append("!");
                            sourceBuilder.append(expectedStack.getReference(((Reference) part).ref));
                            sourceBuilder.append(".isEmpty()");
                            return;
                        } else if (type.equals("number")) {
                            sourceBuilder.append("(");
                            sourceBuilder.append(expectedStack.getReference(((Reference) part).ref));
                            sourceBuilder.append(" != 0)");
                            return;
                        }
                    }
                }
            }
            transpileParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(".toBool()");
        }
    }

    private void generateStringNumberIndex(SourceBuilder sourceBuilder, java.lang.String ref) {
        try {
            if (ref.endsWith(".0")) {
                ref = ref.substring(0, ref.length() - 2);
            }
            if (java.lang.Integer.valueOf(ref) < 0) {
                throw new NumberFormatException();
            }
            sourceBuilder.append(ref);
        } catch (NumberFormatException ex) {
            sourceBuilder.append("\"");
            sourceBuilder.append(convertStringSource(ref));
            sourceBuilder.append("\"");
        }
    }

    static {
        if (System.getProperties().containsKey("NJSNOCOMPILING")) {
            throw new RuntimeException("NJSNOCOMPILING");
        }
        //assert ((new JavaTranspiler().compile("(function munchkin(){\n\tfunction yellow(){\n\t\treturn 55;\n\t}\n\treturn yellow()\n\t})()", "JavaCompilerStaticTest", false)).exec(new Global(), null).toString().equals("55"));
    }

    private boolean generateCommonComparison(SourceBuilder sourceBuilder, java.lang.String ltype, boolean not, Parsed lhs, Parsed rhs, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, StackOptimizations expectedStack, Map<java.lang.String, Function> functionMap, ClassNameScopeChain scopeChain, Map<java.lang.Integer, FilePosition> sourceMap) {
        if (ltype.equals("string")) {
            if (not) {
                sourceBuilder.append("!");
            }
            generateStringSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(".equals(");
            generateStringSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(")");
            return true;
        }
        if (ltype.equals("number")) {
            generateNumberSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(not ? " != " : " == ");
            generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            return true;
        }
        if (ltype.equals("boolean")) {
            generateBooleanSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(not ? " != " : " == ");
            generateBooleanSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            return true;
        }
        return false;
    }

    private void generateBaseScopeAccess(SourceBuilder sourceBuilder, java.lang.String ref, java.lang.String baseScope, StackOptimizations opts, LocalStack localStack) {
        java.lang.String type;
        if (localStack != null && (type = localStack.get(ref)) != null) {
            if (ref.equals("this")) {
                sourceBuilder.append("_this");
                return;
            }

            if (opts.stackType() == ScopeOptimizer.StackType.SyntheticScopeable) {
                type = "unknown";
            }
            if (type.equals("number")) {
                sourceBuilder.append("global.Number.wrap(");
                sourceBuilder.append(opts.getReference(ref));
                sourceBuilder.append(")");
            } else if (type.equals("string")) {
                sourceBuilder.append("global.String.wrap(");
                sourceBuilder.append(opts.getReference(ref));
                sourceBuilder.append(")");
            } else if (type.equals("boolean")) {
                sourceBuilder.append("(");
                sourceBuilder.append(opts.getReference(ref));
                sourceBuilder.append(" ? global.Boolean.TRUE : global.Boolean.FALSE)");
            } else {
                sourceBuilder.append(opts.getReference(ref));
            }
        } else {
            sourceBuilder.append(baseScope);
            sourceBuilder.append(".get(\"");
            sourceBuilder.append(convertStringSource(ref));
            sourceBuilder.append("\")");
        }
    }

    private void addSourceMapEntry(SourceBuilder sourceBuilder, Map<java.lang.Integer, FilePosition> sourceMap, Parsed part) {
        int row = sourceBuilder.row;
        if (sourceMap.containsKey(row)) {
            return;
        }

        sourceMap.put(row, new FilePosition(part.rows, part.columns));
    }

    private boolean isNumber(Parsed part, LocalStack localStack) {
        if (part.isNumber()) {
            return true;
        }

        if (localStack != null && part instanceof Reference) {
            java.lang.String type = localStack.get(((Reference) part).ref);
            return type != null && type.equals("number");
        }
        return false;
    }

    private static final Pattern RESTRICTED_SCOPE_NAMES = Pattern.compile("^(_this|__this|function|global|(localStack|(block|catch|base)Scope)\\d*)$");

    public static class ScopeOptimizer {

        public static enum StackType {
            TypedLocal,
            TypedClass,
            SyntheticScopeable
        }

        public StackType stackType = StackType.TypedLocal;
        public final ArrayList<java.lang.String> volati = new ArrayList();
        public final HashMap<java.lang.String, java.lang.String> scope = new HashMap();

        public void var(java.lang.String key, java.lang.String type) {
            if (RESTRICTED_SCOPE_NAMES.matcher(key).matches()) {
                markUseTypedClassStack();
            }
            java.lang.String current = scope.get(key);
            if (current == null) {
                scope.put(key, type);
            } else {
                update(key, type);
            }
        }

        public void let(java.lang.String key, java.lang.String type) {
            var(key, type);
        }

        public void markVolatile(java.lang.String key) {
            reference(key);
            if (scope.containsKey(key) && !volati.contains(key)) {
                volati.add(key);
            }
        }

        public void update(java.lang.String key, java.lang.String newType) {
            java.lang.String current = scope.get(key);
            if (DEBUG) {
                System.out.println(key + " : " + current + " : " + newType);
            }
            reference(key);
            if (current == null) {
                return;
            }
            if (current.equals("any")) {
                scope.put(key, newType);
            } else if (!current.equals(newType)) {
                scope.put(key, "unknown");
                if (volati.contains(key)) {
                    markUseSyntheticStack();
                }
            }
        }

        public boolean reference(java.lang.String key) {
            return scope.containsKey(key);
        }

        public boolean isTyped(java.lang.String key, java.lang.String type) {
            reference(key);
            return type.equals(scope.get(key));
        }

        public void assertTyped(java.lang.String key, java.lang.String... _types) {
            java.lang.String current = scope.get(key);
            if (current == null || !Arrays.asList(_types).contains(current)) {
                markUseSyntheticStack();
                return;
            }
            markVolatile(key);
        }

        public java.lang.String lookup(java.lang.String key) {
            reference(key);
            java.lang.String current = scope.get(key);
            if (current == null) {
                return "unknown";
            } else {
                return current;
            }
        }

        public boolean markUseSyntheticStack() {
            if(stackType == StackType.SyntheticScopeable)
                return false;
            stackType = StackType.SyntheticScopeable;
            return true;
        }

        public void markUseTypedClassStack() {
            if (stackType == StackType.TypedLocal) {
                stackType = StackType.TypedClass;
            }
        }

        public void markUsesArguments() {
        }
    }

    public static class FunctionScopeOptimizer extends ScopeOptimizer {

        boolean usesArguments;
        final ScopeOptimizer parent;

        FunctionScopeOptimizer(ScopeOptimizer parent) {
            this.parent = parent;
        }

        @Override
        public boolean reference(java.lang.String key) {
            usesArguments = usesArguments || key.equals("arguments");
            if(!scope.containsKey(key)) {
                if(parent.reference(key))
                    parent.markUseSyntheticStack();
                return false;
            } else
                return true;
        }

        @Override
        public void markVolatile(java.lang.String key) {
            if (scope.containsKey(key)) {
                reference(key);
                if (!volati.contains(key)) {
                    volati.add(key);
                }
            } else {
                parent.markVolatile(key);
            }
        }

        @Override
        public void update(java.lang.String key, java.lang.String newType) {
            java.lang.String current = scope.get(key);
            if (DEBUG) {
                System.out.println(key + " : " + current + " : " + newType);
            }
            if (current == null) {
                reference(key);
                return;
            }
            if (current.equals("any")) {
                scope.put(key, newType);
            } else if (!current.equals(newType)) {
                scope.put(key, "unknown");
                if (volati.contains(key)) {
                    markUseSyntheticStack();
                }
            } else {
                parent.update(key, newType);
                return;
            }
            reference(key);
        }

        @Override
        public boolean isTyped(java.lang.String key, java.lang.String type) {
            java.lang.String typedAs = scope.get(key);
            if (typedAs != null) {
                return type.equals(typedAs);
            }
            return parent.isTyped(key, type);
        }

        @Override
        public void assertTyped(java.lang.String key, java.lang.String... _types) {
            java.lang.String current = scope.get(key);
            if (current == null) {
                parent.assertTyped(key, _types);
                return;
            }

            List<java.lang.String> types = Arrays.asList(_types);
            if (!types.contains(current)) {
                markUseSyntheticStack();
                return;
            }
            markVolatile(key);
        }

        @Override
        public java.lang.String lookup(java.lang.String key) {
            java.lang.String current = scope.get(key);
            if (current == null) {
                return parent.lookup(key);
            } else {
                return current;
            }
        }

        @Override
        public void markUsesArguments() {
            usesArguments = true;
        }
    }

    public static class BlockScopeOptimizer extends ScopeOptimizer {

        final ScopeOptimizer parent;

        BlockScopeOptimizer(ScopeOptimizer parent) {
            this.parent = parent;
        }

        @Override
        public boolean reference(java.lang.String key) {
            if(!scope.containsKey(key)) {
                if(parent.reference(key))
                    parent.markUseSyntheticStack();
                return false;
            } else
                return true;
        }

        @Override
        public void var(java.lang.String key, java.lang.String type) {
            parent.var(key, type);
        }

        @Override
        public void markVolatile(java.lang.String key) {
            if (scope.containsKey(key)) {
                reference(key);
                if (!volati.contains(key)) {
                    volati.add(key);
                }
            } else {
                parent.markVolatile(key);
            }
        }

        @Override
        public void update(java.lang.String key, java.lang.String newType) {
            java.lang.String current = scope.get(key);
            if (DEBUG) {
                System.out.println(key + " : " + current + " : " + newType);
            }
            if (current == null) {
                parent.update(key, newType);
                return;
            }
            reference(key);
            if (current.equals("any")) {
                scope.put(key, newType);
            } else if (!current.equals(newType)) {
                scope.put(key, "unknown");
                if (volati.contains(key)) {
                    markUseSyntheticStack();
                }
            }
        }

        @Override
        public boolean isTyped(java.lang.String key, java.lang.String type) {
            java.lang.String typedAs = scope.get(key);
            if (typedAs != null) {
                return type.equals(typedAs);
            }
            return parent.isTyped(key, type);
        }

        @Override
        public void assertTyped(java.lang.String key, java.lang.String... _types) {
            java.lang.String current = scope.get(key);
            if (current == null) {
                parent.assertTyped(key, _types);
                return;
            }

            List<java.lang.String> types = Arrays.asList(_types);
            if (!types.contains(current)) {
                markUseSyntheticStack();
                return;
            }
            markVolatile(key);
        }

        @Override
        public java.lang.String lookup(java.lang.String key) {
            java.lang.String current = scope.get(key);
            if (current == null) {
                return parent.lookup(key);
            } else {
                return current;
            }
        }

        @Override
        public boolean markUseSyntheticStack() {
            return super.markUseSyntheticStack() ||
                    parent.markUseSyntheticStack();
        }

        @Override
        public void markUseTypedClassStack() {
            super.markUseTypedClassStack();
            parent.markUseTypedClassStack();
        }

        @Override
        public void markUsesArguments() {
            parent.markUsesArguments();
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
        if (parsed instanceof Let) {
            for (Var.Set set : ((Var) parsed).sets) {
                if(set.lhs instanceof Reference) {
                    if (set.rhs != null) {
                        variableScope.let(((Reference)set.lhs).ref, set.rhs.primaryType());
                    } else {
                        variableScope.let(((Reference)set.lhs).ref, "any");
                    }
                } else {
                    variableScope.markUseSyntheticStack();
                    for(java.lang.String name : ((NameSet)set.lhs).names.values()) {
                        variableScope.let(name, "any");
                    }
                }
            }
        } else if (parsed instanceof Var) {
            for (Var.Set set : ((Var) parsed).sets) {
                if(set.lhs instanceof Reference) {
                    if (set.rhs != null) {
                        variableScope.var(((Reference)set.lhs).ref, set.rhs.primaryType());
                    } else {
                        variableScope.var(((Reference)set.lhs).ref, "any");
                    }
                } else {
                    variableScope.markUseSyntheticStack();
                    for(java.lang.String name : ((NameSet)set.lhs).names.values()) {
                        variableScope.var(name, "any");
                    }
                }
            }
        } else if (parsed instanceof Call) {
            scanParsedSource(((Call)parsed).reference, variableScope);
            for (Parsed argument : ((Call) parsed).arguments) {
                scanArgument(argument, variableScope);
            }
        } else if (parsed instanceof If) {
            scanParsedSource(((If) parsed).condition, variableScope);
            if (((If) parsed).simpleimpl != null) {
                scanParsedSource(((If) parsed).simpleimpl, variableScope);
            } else {
                scanScriptSource(((If) parsed).impl, new BlockScopeOptimizer(variableScope));
            }

            Else el = ((If) parsed).el;
            while (el != null) {
                if (el instanceof ElseIf) {
                    scanParsedSource(((ElseIf) el).condition, variableScope);
                }
                if (el.simpleimpl != null) {
                    scanParsedSource(el.simpleimpl, variableScope);
                } else {
                    scanScriptSource(el.impl, new BlockScopeOptimizer(variableScope));
                }
                if (el instanceof ElseIf) {
                    el = ((ElseIf) el).el;
                } else {
                    break;
                }
            }
        } else if (parsed instanceof For) {
            BlockScopeOptimizer forScope = new BlockScopeOptimizer(variableScope);
            scanParsedSource(((For) parsed).init, forScope);
            scanParsedSource(((For) parsed).loop, forScope);
            if (((For) parsed).type == For.ForType.Standard) {
                scanParsedSource(((For) parsed).condition, forScope);
            }
            if (((For) parsed).simpleimpl != null) {
                scanParsedSource(((For) parsed).simpleimpl, forScope);
            } else {
                scanScriptSource(((For) parsed).impl, forScope);
            }
        } else if (parsed instanceof While) {
            scanParsedSource(((While) parsed).condition, variableScope);
            if (((While) parsed).simpleimpl != null) {
                scanParsedSource(((While) parsed).simpleimpl, variableScope);
            } else {
                scanScriptSource(((While) parsed).impl, new BlockScopeOptimizer(variableScope));
            }
        } else if (parsed instanceof Try) {
            scanScriptSource(((Try) parsed).impl, variableScope);
            if (((Try) parsed).c != null) {
                scanScriptSource(((Try) parsed).c.impl, variableScope);
            }
            if (((Try) parsed).f != null) {
                scanScriptSource(((Try) parsed).f.impl, variableScope);
            }
        } else if (parsed instanceof OpenBracket) {
            scanParsedSource(((OpenBracket) parsed).contents, variableScope);
        } else if (parsed instanceof VariableReference) {
            scanParsedSource(((VariableReference) parsed).ref, variableScope);
            scanParsedSource(((VariableReference) parsed).lhs, variableScope);
        } else if (parsed instanceof New) {
            scanParsedSource(((New) parsed).reference, variableScope);
            if (((New) parsed).arguments != null) {
                for (Parsed entry : ((New) parsed).arguments) {
                    scanParsedSource(entry, variableScope);
                }
            }
        } else if (parsed instanceof OpenArray) {
            for (Parsed entry : ((OpenArray) parsed).entries) {
                scanParsedSource(entry, variableScope);
            }
        } else if (parsed instanceof RightReference) {
            scanParsedSource(((RightReference) parsed).ref, variableScope);
        } else if (parsed instanceof PlusPlus) {
            Parsed ref = ((PlusPlus) parsed).ref;
            if (ref instanceof Reference) {
                variableScope.update(((Reference) ref).ref, "number");
            } else {
                throw new CannotOptimizeUnimplemented("No implementation for optimizing " + describe(ref));
            }
        } else if (parsed instanceof PlusEq) {
            Parsed ref = ((PlusEq) parsed).lhs;
            Parsed rhs = ((PlusEq) parsed).rhs;
            if ((rhs.isNumber() || (rhs instanceof Reference && variableScope.isTyped(((Reference) rhs).ref, "number")))) {
                if (ref instanceof Reference) {
                    variableScope.update(((Reference) ref).ref, "number");
                } else {
                    throw new CannotOptimizeUnimplemented("No implementation for optimizing " + describe(ref));
                }
            }
        } else if (parsed instanceof OrEq) {
            Parsed ref = ((OrEq) parsed).lhs;
            if (ref instanceof Reference) {
                variableScope.update(((Reference) ref).ref, "number");
            } else {
                throw new CannotOptimizeUnimplemented("No implementation for optimizing " + describe(ref));
            }
        } else if (parsed instanceof AndEq) {
            Parsed ref = ((AndEq) parsed).lhs;
            if (ref instanceof Reference) {
                variableScope.update(((Reference) ref).ref, "number");
            } else {
                throw new CannotOptimizeUnimplemented("No implementation for optimizing " + describe(ref));
            }
        } else if (parsed instanceof MultiplyEq) {
            Parsed ref = ((MultiplyEq) parsed).lhs;
            if (ref instanceof Reference) {
                variableScope.update(((Reference) ref).ref, "number");
            } else {
                throw new CannotOptimizeUnimplemented("No implementation for optimizing " + describe(ref));
            }
        } else if (parsed instanceof DivideEq) {
            Parsed ref = ((DivideEq) parsed).lhs;
            if (ref instanceof Reference) {
                variableScope.update(((Reference) ref).ref, "number");
            } else {
                throw new CannotOptimizeUnimplemented("No implementation for optimizing " + describe(ref));
            }
        } else if (parsed instanceof MinusMinus) {
            Parsed ref = ((MinusMinus) parsed).ref;
            if (ref instanceof Reference) {
                variableScope.update(((Reference) ref).ref, "number");
            } else {
                throw new CannotOptimizeUnimplemented("No implementation for optimizing " + describe(ref));
            }
        } else if (parsed instanceof Set) {
            Parsed rhs = ((Set) parsed).rhs;
            Parsed lhs = ((Set) parsed).lhs;

            if (lhs instanceof Reference) {
                if (rhs instanceof BaseReferency) {
                    if (rhs instanceof Reference) {
                        variableScope.update(((Reference) lhs).ref, variableScope.lookup(((Reference) rhs).ref));
                    } else {
                        variableScope.update(((Reference) lhs).ref, rhs.primaryType());
                    }
                    if(DEBUG)
                        System.out.println("Updated " + ((Reference) lhs).ref + " to be " + variableScope.lookup(((Reference) lhs).ref) + " based on " + rhs);
                } else {
                    variableScope.reference(((Reference) lhs).ref);
                    scanParsedSource(rhs, variableScope);
                }
            } else if (lhs instanceof ReferenceChain || lhs instanceof VariableReference || lhs instanceof IntegerReference) {
                scanParsedSource(lhs, variableScope);
                scanParsedSource(rhs, variableScope);
            } else {
                throw new CannotOptimizeUnimplemented("No implementation for optimizing set " + describe(lhs));
            }
        } else if (parsed instanceof OpenGroup) {
            for (Map.Entry<?, Parsed> entry : ((OpenGroup) parsed).entries.entrySet()) {
                scanParsedSource(entry.getValue(), variableScope);
            }
        } else if (parsed instanceof Reference) {
            variableScope.reference(((Reference) parsed).ref);
        } else if (parsed instanceof ReferenceChain) {
            variableScope.reference(((ReferenceChain) parsed).chain.get(0));
        } else if (parsed instanceof Long || parsed instanceof Boolean
                || parsed instanceof Number || parsed instanceof String || parsed instanceof Null || parsed instanceof Undefined) {
            // IGNORED
        } else if (parsed instanceof RhLh) {
            scanParsedSource(((RhLh) parsed).lhs, variableScope);
            scanParsedSource(((RhLh) parsed).rhs, variableScope);
        } else if (parsed instanceof Rh) {
            scanParsedSource(((Rh) parsed).rhs, variableScope);
        } else if (parsed instanceof Function) {
            FunctionScopeOptimizer scopeOptimizer = new FunctionScopeOptimizer(variableScope);
            scopeOptimizer.scope.put("arguments", "arguments");
            for (java.lang.String arg : ((Function) parsed).arguments) {
                if (RESTRICTED_SCOPE_NAMES.matcher(arg).matches()) {
                    scopeOptimizer.markUseTypedClassStack();
                    return;
                }
                scopeOptimizer.scope.put(arg, "argument");
            }
            java.lang.String arg = ((Function)parsed).vararg;
            if(arg != null) {
                if (RESTRICTED_SCOPE_NAMES.matcher(arg).matches()) {
                    scopeOptimizer.markUseTypedClassStack();
                    return;
                }
                scopeOptimizer.scope.put(arg, "argument");
            }
            
            try {
                scanScriptSource(((Function) parsed).impl, scopeOptimizer);
                ((Function) parsed).impl.optimizations = new MapStackOptimizations(scopeOptimizer.scope, scopeOptimizer.stackType, scopeOptimizer.usesArguments);
            } catch (CannotOptimize ex) {
                if (DEBUG || ex instanceof CannotOptimizeUnimplemented) {
                    ex.printStackTrace(System.out);
                }
                variableScope.markUseSyntheticStack();
            }
        } else if(parsed instanceof IntegerReference) {
            scanParsedSource(((IntegerReference) parsed).lhs, variableScope);
        } else if(parsed instanceof Fork) {
            scanArgument(((Fork)parsed).condition, variableScope);
            scanParsedSource(((Fork)parsed).success, variableScope);
            scanParsedSource(((Fork)parsed).failure, variableScope);
        } else if(parsed instanceof MultiBracket) {
            for(Parsed part : ((MultiBracket)parsed).parts)
                scanParsedSource(part, variableScope);
        } else {
            throw new CannotOptimizeUnimplemented("Unhandled " + describe(parsed));
        }
    }

    private void scanParsedSource(Parsed[] impl, ScopeOptimizer variableScope) {
        for (Parsed parsed : impl) {
            while (parsed instanceof OpenBracket) {
                parsed = ((OpenBracket) parsed).contents;
            }

            scanParsedSource(parsed, variableScope);
        }
    }

    private void scanScriptSource(ScriptData script, ScopeOptimizer variableScope) {
        scanParsedSource(script.impl, variableScope);

        if (!script.functions.isEmpty()) {
            variableScope.markUseTypedClassStack();
            for (Function function : script.functions.values()) {
                scanParsedSource(function, variableScope);
            }
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
        
        for (java.lang.String name : new java.lang.String[]{
            "abstract", "assert", "boolean", "break", "byte",
            "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else",
            "enum", "extends", "false", "final", "finally",
            "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long",
            "native", "new", "null", "package", "private",
            "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "true", "try",
            "void", "volatile", "while", "BaseObject", "BaseFunction",
            "AbstractFunction", "GenericObject", "GenericArray", "CompiledScript", "Null", "Undefined",
            "CompiledFunction", "Utilities", "Iterator", "String", "SyntheticScope"}) {
            GEN_PACKAGE_USED_NAMES.put(name, new AtomicInteger(2));
        }
    }

    protected static class LocalStack {

        public final boolean has_this;
        public LocalStack(boolean has_this) {
            this.has_this = has_this;
        }
        private final HashMap<java.lang.String, java.lang.String> stack = new HashMap();

        public void put(java.lang.String key, java.lang.String val) {
            stack.put(key, val);
        }

        public java.lang.String get(java.lang.String key) {
            return stack.get(key);
        }

        public boolean isEmpty() {
            return stack.isEmpty();
        }

        private java.util.Set<java.lang.String> keys() {
            return stack.keySet();
        }
    }

    protected boolean generateBlockSource(SourceBuilder sourceBuilder, ScriptData blockDat, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, StackOptimizations expectedStack, Map<java.lang.String, Function> functionMap, ClassNameScopeChain scopeChain, Map<java.lang.Integer, FilePosition> sourceMap) {
        boolean hasReturn = false;
        for (Parsed part : blockDat.impl) {
            if (addDebugging) {
                addSourceMapEntry(sourceBuilder, sourceMap, part);
            }
            hasReturn = transpileParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, true) || hasReturn;
            if(!(part instanceof Block))
                sourceBuilder.appendln(";");
            else
                sourceBuilder.appendln();
        }
        return hasReturn;
    }

    protected boolean transpileParsedSource(SourceBuilder sourceBuilder, Parsed part, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, StackOptimizations expectedStack, Map<java.lang.String, Function> functionMap, ClassNameScopeChain scopeChain, Map<java.lang.Integer, FilePosition> sourceMap) {
        return transpileParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, false);
    }

    protected boolean transpileParsedSource(SourceBuilder sourceBuilder, Parsed part, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, StackOptimizations expectedStack, Map<java.lang.String, Function> functionMap, ClassNameScopeChain scopeChain, Map<java.lang.Integer, FilePosition> sourceMap, boolean atTop) {
        while (part instanceof OpenBracket) {
            part = ((OpenBracket) part).contents;
        }

        if (part instanceof Return) {
            sourceBuilder.append("return ");
            if(((Return) part).rhs == null)
                sourceBuilder.append("Undefined.INSTANCE");
            else
                transpileParsedSource(sourceBuilder, ((Return) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            return true;
        } else if (part instanceof TypeOf) {
            Parsed rhs = ((TypeOf) part).rhs;
            sourceBuilder.append("global.wrap(");
            transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(".typeOf())");
            return false;
        } else if (part instanceof Call) {
            Parsed reference = ((Call) part).reference;
            while (reference instanceof OpenBracket) {// unwrap
                reference = ((OpenBracket) reference).contents;
            }

            if (reference instanceof BaseReferency && !(reference instanceof Reference || reference instanceof Call)) {
                final java.lang.String source = reference.toSimpleSource();
                if (reference instanceof RightReference) {
                    final java.lang.String key = ((RightReference) reference).chain.remove(((RightReference) reference).chain.size() - 1);
                    if (atTop) {
                        sourceBuilder.append("__this = ");
                        transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                        sourceBuilder.appendln(";");
                        if (addDebugging) {
                            sourceBuilder.appendln("try {");
                            sourceBuilder.indent();
                        }
                        sourceBuilder.append("function = (BaseFunction)__this.get(\"");
                        sourceBuilder.append(convertStringSource(key));
                        sourceBuilder.appendln("\");");
                        if (addDebugging) {
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
                        transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    }
                    for (Parsed arg : ((Call) part).arguments) {
                        sourceBuilder.append(", ");
                        transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    }
                    sourceBuilder.append(")");
                    return false;
                } else if (reference instanceof IntegerReference) {
                    final int key = ((IntegerReference) reference).ref;
                    if (atTop) {
                        sourceBuilder.append("__this = ");
                        transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                        sourceBuilder.appendln(";");
                        if (addDebugging) {
                            sourceBuilder.appendln("try {");
                            sourceBuilder.indent();
                        }
                        sourceBuilder.append("function = (BaseFunction)__this.get(");
                        sourceBuilder.append(java.lang.String.valueOf(key));
                        sourceBuilder.appendln(");");
                        if (addDebugging) {
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
                        transpileParsedSource(sourceBuilder, ((IntegerReference) reference).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    }
                    for (Parsed arg : ((Call) part).arguments) {
                        sourceBuilder.append(", ");
                        transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    }
                    sourceBuilder.append(")");
                    return false;
                } else if (reference instanceof ReferenceChain) {
                    final java.lang.String key = ((ReferenceChain) reference).chain.remove(((ReferenceChain) reference).chain.size() - 1);
                    if (atTop) {
                        sourceBuilder.append("__this = ");
                        transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                        sourceBuilder.appendln(";");
                        if (addDebugging) {
                            sourceBuilder.appendln("try {");
                            sourceBuilder.indent();
                        }
                        sourceBuilder.append("function = (BaseFunction)__this.get(\"");
                        sourceBuilder.append(convertStringSource(key));
                        sourceBuilder.appendln("\");");
                        if (addDebugging) {
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
                        transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    }
                    for (Parsed arg : ((Call) part).arguments) {
                        sourceBuilder.append(", ");
                        transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    }
                    sourceBuilder.append(")");
                    return false;
                } else if (reference instanceof New) {
                    sourceBuilder.append("callNew(");
                    if (addDebugging) {
                        sourceBuilder.append("\"");
                        sourceBuilder.append(convertStringSource(source));
                        sourceBuilder.append("\", ");
                    }
                    transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    for (Parsed arg : ((Call) part).arguments) {
                        sourceBuilder.append(", ");
                        transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    }
                    sourceBuilder.append(")");
                    return false;
                } else if(reference instanceof VariableReference) {
                    sourceBuilder.append("Utilities.get(");
                    transpileParsedSource(sourceBuilder, ((VariableReference)reference).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    sourceBuilder.append(", ");
                    transpileParsedSource(sourceBuilder, ((VariableReference)reference).ref, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    sourceBuilder.append(")");
                    return false;
                }

                throw new UnsupportedOperationException("Cannot compile call: " + describe(reference));
            }

            if (addDebugging) {
                java.lang.String source = convertStringSource(reference.toSimpleSource());
                if (atTop) {
                    sourceBuilder.appendln("try {");
                    sourceBuilder.indent();
                    sourceBuilder.append("function = (BaseFunction)");
                    transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    sourceBuilder.appendln(";");
                    sourceBuilder.unindent();
                    sourceBuilder.appendln("} catch(ClassCastException ex) {");
                    sourceBuilder.append("\tthrow new net.nexustools.njs.Error.JavaException(\"ReferenceError\", \"");
                    sourceBuilder.append(source);
                    sourceBuilder.appendln(" is not a function\");");
                    sourceBuilder.appendln("}");
                    sourceBuilder.append("function.call(_this");
                } else {
                    sourceBuilder.append("callTop(");
                    sourceBuilder.append("\"");
                    sourceBuilder.append(source);
                    sourceBuilder.append("\", ");
                    transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    sourceBuilder.append(", ");
                    sourceBuilder.append("_this");
                }
                for (Parsed arg : ((Call) part).arguments) {
                    sourceBuilder.append(", ");
                    transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                }
                sourceBuilder.append(")");
            } else {
                sourceBuilder.append("((BaseFunction)");
                transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.append(").call(");
                sourceBuilder.append("_this");
                for (Parsed arg : ((Call) part).arguments) {
                    sourceBuilder.append(", ");
                    transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                }
                sourceBuilder.append(")");
            }
            return false;
        } else if (part instanceof InstanceOf) {
            sourceBuilder.append("(");
            transpileParsedSource(sourceBuilder, ((InstanceOf) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(".instanceOf((BaseFunction)");
            transpileParsedSource(sourceBuilder, ((InstanceOf) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
            return false;
        } else if (part instanceof Number) {
            double value = ((Number) part).value;
            if (value == 0) {
                sourceBuilder.append("global.Zero");
            } else if (value == 1) {
                sourceBuilder.append("global.PositiveOne");
            } else if (value == -1) {
                sourceBuilder.append("global.NegativeOne");
            } else {
                if(Double.isNaN(value))
                    sourceBuilder.append("global.NaN");
                else if(Double.POSITIVE_INFINITY == value)
                    sourceBuilder.append("global.PositiveInfinity");
                else if(Double.NEGATIVE_INFINITY == value)
                    sourceBuilder.append("global.NegativeInfinity");
                else {
                    sourceBuilder.append("global.wrap(");
                    sourceBuilder.append(java.lang.String.valueOf(value));
                    sourceBuilder.append(")");
                }
            }
            return false;
        } else if (part instanceof Long) {
            long value = ((Long) part).value;
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
            return false;
        } else if (part instanceof String) {
            sourceBuilder.append("global.wrap(\"");
            sourceBuilder.append(convertStringSource(java.lang.String.valueOf(((String) part).string)));
            sourceBuilder.append("\")");
            return false;
        } else if (part instanceof Reference) {
            generateBaseScopeAccess(sourceBuilder, ((Reference) part).ref, baseScope, expectedStack, localStack);
            return false;
        } else if (part instanceof DoubleShiftRight) {
            Parsed lhs = ((DoubleShiftRight) part).lhs;
            Parsed rhs = ((DoubleShiftRight) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, ">>>", methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, atTop ? MathWrap.Void : MathWrap.BaseObject);
            return false;
        } else if (part instanceof ShiftLeft) {
            Parsed lhs = ((ShiftLeft) part).lhs;
            Parsed rhs = ((ShiftLeft) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, "<<", methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, atTop ? MathWrap.Void : MathWrap.BaseObject);
            return false;
        } else if (part instanceof ShiftRight) {
            Parsed lhs = ((ShiftRight) part).lhs;
            Parsed rhs = ((ShiftRight) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, ">>", methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, atTop ? MathWrap.Void : MathWrap.BaseObject);
            return false;
        } else if (part instanceof Plus) {
            Parsed lhs = ((Plus) part).lhs;
            Parsed rhs = ((Plus) part).rhs;
            if(lhs == null) {
                sourceBuilder.append("global.Number.fromValueOf(");
                transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.append(")");
            } else
                generateMath(sourceBuilder, lhs, rhs, '+', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, atTop ? MathWrap.Void : MathWrap.BaseObject);
            return false;
        } else if (part instanceof Multiply) {
            Parsed lhs = ((Multiply) part).lhs;
            Parsed rhs = ((Multiply) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '*', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, atTop ? MathWrap.Void : MathWrap.BaseObject);
            return false;
        } else if (part instanceof Divide) {
            Parsed lhs = ((Divide) part).lhs;
            Parsed rhs = ((Divide) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '/', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, atTop ? MathWrap.Void : MathWrap.BaseObject);
            return false;
        } else if (part instanceof And) {
            Parsed lhs = ((And) part).lhs;
            Parsed rhs = ((And) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '&', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, atTop ? MathWrap.Void : MathWrap.BaseObject);
            return false;
        } else if (part instanceof Or) {
            Parsed lhs = ((Or) part).lhs;
            Parsed rhs = ((Or) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '|', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, atTop ? MathWrap.Void : MathWrap.BaseObject);
            return false;
        } else if (part instanceof Percent) {
            Parsed lhs = ((Percent) part).lhs;
            Parsed rhs = ((Percent) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '%', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, atTop ? MathWrap.Void : MathWrap.BaseObject);
            return false;
        } else if (part instanceof Minus) {
            Parsed lhs = ((Minus) part).lhs;
            Parsed rhs = ((Minus) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '-', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, atTop ? MathWrap.Void : MathWrap.BaseObject);
            return false;
        } else if (part instanceof New) {
            boolean addComma;
            if (addDebugging) {
                addComma = true;
                sourceBuilder.append("constructTop(\"");
                sourceBuilder.append(convertStringSource(((New) part).reference.toSimpleSource()));
                sourceBuilder.append("\", ");
                transpileParsedSource(sourceBuilder, ((New) part).reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            } else {
                addComma = false;
                sourceBuilder.append("((BaseFunction)");
                transpileParsedSource(sourceBuilder, ((New) part).reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.append(").construct(");
            }
            if (((New) part).arguments != null) {
                for (Parsed arg : ((New) part).arguments) {
                    if (addComma) {
                        sourceBuilder.append(", ");
                    } else {
                        addComma = true;
                    }
                    transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                }
            }
            sourceBuilder.append(")");
            return false;
        } else if (part instanceof RightReference) {
            transpileParsedSource(sourceBuilder, ((RightReference) part).ref, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            for (java.lang.String key : ((RightReference) part).chain) {
                sourceBuilder.append(".get(");
                generateStringNumberIndex(sourceBuilder, key);
                sourceBuilder.append(")");
            }
            return false;
        } else if (part instanceof Throw) {
            sourceBuilder.append("throw new net.nexustools.njs.Error.Thrown(");
            transpileParsedSource(sourceBuilder, ((Throw) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(")");
            return true;
        } else if (part instanceof Function) {
            java.lang.String name = ((Function) part).name;
            if (name == null || name.isEmpty()) {
                name = "<anonymous>";
            }

            name = scopeChain.toClassName(name);
            if (functionMap.containsKey(name)) {
                int index = 2;
                java.lang.String base = name + "_";
                do {
                    name = base + index++;
                } while (functionMap.containsKey(name));
            }
            ((Function) part).uname = name;
            functionMap.put(name, (Function) part);

            sourceBuilder.append("new ");
            sourceBuilder.append(name);
            sourceBuilder.append("(");
            if(((Function)part).isLambda())
                sourceBuilder.append("_this, ");
            sourceBuilder.append("global, ");
            sourceBuilder.append(baseScope);
            sourceBuilder.append(")");
            return false;
        } else if (part instanceof Var) {
            java.lang.String _type = part instanceof Let ? "let" : "var";
            List<Var.Set> sets = ((Var) part).sets;
            java.lang.Boolean first = true;
            for (Var.Set set : sets) {
                if (first) {
                    first = false;
                } else {
                    sourceBuilder.appendln(";");
                }
                if(set.lhs instanceof Reference) {
                    java.lang.String ref = ((Reference)set.lhs).ref;
                    java.lang.String type = expectedStack == null ? null : expectedStack.get(ref);
                    if (type != null) {
                        if (type.startsWith("parent")) {
                            throw new RuntimeException("Should not be declaring a parent's property...");
                        }
                        if (expectedStack.stackType() == ScopeOptimizer.StackType.SyntheticScopeable) {
                            type = "unknown";
                        }
                        localStack.put(ref, type);
                        if (set.rhs == null) {
                            continue;
                        }

                        sourceBuilder.append(expectedStack.getReference(ref));
                        sourceBuilder.append(" = ");
                        if (type.equals("string")) {
                            if (set.rhs == null) {
                                sourceBuilder.append("\"undefined\"");
                            } else {
                                generateStringSource(sourceBuilder, set.rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                            }
                        } else if (type.equals("boolean")) {
                            if (set.rhs == null) {
                                sourceBuilder.append("false");
                            } else {
                                generateBooleanSource(sourceBuilder, set.rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                            }
                        } else if (type.equals("number")) {
                            if (set.rhs == null) {
                                sourceBuilder.append("0");
                            } else {
                                generateNumberSource(sourceBuilder, set.rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                            }
                        } else if (set.rhs == null) {
                            sourceBuilder.append("Undefined.INSTANCE");
                        } else {
                            transpileParsedSource(sourceBuilder, set.rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                        }
                    } else {
                        sourceBuilder.append(baseScope);
                        sourceBuilder.append("." + _type + "(\"");
                        sourceBuilder.append(convertStringSource(ref));
                        sourceBuilder.append("\", ");
                        if (set.rhs == null) {
                            sourceBuilder.append("Undefined.INSTANCE");
                        } else {
                            transpileParsedSource(sourceBuilder, set.rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                        }
                        sourceBuilder.append(")");
                    }
                } else {
                    sourceBuilder.append(baseScope);
                    sourceBuilder.append(".multi" + _type + "(");
                    transpileParsedSource(sourceBuilder, set.rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    for(Map.Entry<java.lang.String, java.lang.String> name : ((NameSet)set.lhs).names.entrySet()) {
                        sourceBuilder.append(", \"");
                        sourceBuilder.append(convertStringSource(name.getValue()));
                        sourceBuilder.append("\"");
                        sourceBuilder.append(", \"");
                        sourceBuilder.append(convertStringSource(name.getKey()));
                        sourceBuilder.append("\"");
                    }
                    sourceBuilder.append(")");
                }
            }
            return false;
        } else if (part instanceof OrOr) {
            sourceBuilder.append("orOr(");
            transpileParsedSource(sourceBuilder, ((OrOr) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(", ");
            transpileParsedSource(sourceBuilder, ((OrOr) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(")");
            return false;
        } else if (part instanceof AndAnd) {
            sourceBuilder.append("andAnd(");
            transpileParsedSource(sourceBuilder, ((AndAnd) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(", ");
            transpileParsedSource(sourceBuilder, ((AndAnd) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(")");
            return false;
        } else if (part instanceof Equals) {
            sourceBuilder.append("(");
            java.lang.String ltype = ((Equals) part).lhs.primaryType();
            java.lang.String rtype = ((Equals) part).rhs.primaryType();
            if (ltype.equals(rtype)) {
                if (generateCommonComparison(sourceBuilder, ltype, false, ((Equals) part).lhs, ((Equals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap)) {
                    sourceBuilder.append(" ? global.Boolean.TRUE : global.Boolean.FALSE)");
                    return false;
                }
            }

            transpileParsedSource(sourceBuilder, ((Equals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(".equals(");
            transpileParsedSource(sourceBuilder, ((Equals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
            return false;
        } else if (part instanceof NotEquals) {
            sourceBuilder.append("(");
            java.lang.String ltype = ((NotEquals) part).lhs.primaryType();
            java.lang.String rtype = ((NotEquals) part).rhs.primaryType();
            if (ltype.equals(rtype)) {
                if (generateCommonComparison(sourceBuilder, ltype, true, ((NotEquals) part).lhs, ((NotEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap)) {
                    sourceBuilder.append(" ? global.Boolean.TRUE : global.Boolean.FALSE)");
                    return false;
                }
            }

            transpileParsedSource(sourceBuilder, ((NotEquals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(".equals(");
            transpileParsedSource(sourceBuilder, ((NotEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(") ? global.Boolean.FALSE : global.Boolean.TRUE)");
            return false;
        } else if (part instanceof StrictEquals) {
            sourceBuilder.append("(");
            java.lang.String ltype = ((StrictEquals) part).lhs.primaryType();
            java.lang.String rtype = ((StrictEquals) part).rhs.primaryType();
            if (ltype.equals(rtype)) {
                if (generateCommonComparison(sourceBuilder, ltype, false, ((StrictEquals) part).lhs, ((StrictEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap)) {
                    sourceBuilder.append(" ? global.Boolean.TRUE : global.Boolean.FALSE)");
                    return false;
                }
            }

            transpileParsedSource(sourceBuilder, ((StrictEquals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(".strictEquals(");
            transpileParsedSource(sourceBuilder, ((StrictEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
            return false;
        } else if (part instanceof StrictNotEquals) {
            sourceBuilder.append("(");
            java.lang.String ltype = ((StrictNotEquals) part).lhs.primaryType();
            java.lang.String rtype = ((StrictNotEquals) part).rhs.primaryType();
            if (ltype.equals(rtype)) {
                if (generateCommonComparison(sourceBuilder, ltype, true, ((StrictNotEquals) part).lhs, ((StrictNotEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap)) {
                    sourceBuilder.append(" ? global.Boolean.TRUE : global.Boolean.FALSE)");
                    return false;
                }
            }

            transpileParsedSource(sourceBuilder, ((StrictNotEquals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(".strictEquals(");
            transpileParsedSource(sourceBuilder, ((StrictNotEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(") ? global.Boolean.FALSE : global.Boolean.TRUE)");
            return false;
        } else if (part instanceof ReferenceChain) {
            generateBaseScopeAccess(sourceBuilder, ((ReferenceChain) part).chain.remove(0), baseScope, expectedStack, localStack);
            for (java.lang.String ref : ((ReferenceChain) part).chain) {
                sourceBuilder.append(".get(");
                generateStringNumberIndex(sourceBuilder, ref);
                sourceBuilder.append(")");
            }
            return false;
        } else if (part instanceof IntegerReference) {
            int key = ((IntegerReference) part).ref;
            if (((IntegerReference) part).lhs == null) {
                sourceBuilder.append("new GenericArray(global, new BaseObject[]{");
                switch (key) {
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
                return false;
            }

            transpileParsedSource(sourceBuilder, ((IntegerReference) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(".get(");
            sourceBuilder.append(java.lang.String.valueOf(key));
            sourceBuilder.append(")");
            return false;
        } else if (part instanceof Not) {
            sourceBuilder.append("(");
            generateBooleanSource(sourceBuilder, ((Not) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append("? global.Boolean.FALSE : global.Boolean.TRUE)");
            return false;
        } else if (part instanceof OpenArray) {
            boolean first = true;
            sourceBuilder.append("new GenericArray(global");
            if (!((OpenArray) part).entries.isEmpty()) {
                sourceBuilder.append(", new BaseObject[]{");
                for (Parsed subpart : ((OpenArray) part).entries) {
                    if (first) {
                        first = false;
                    } else {
                        sourceBuilder.append(", ");
                    }
                    transpileParsedSource(sourceBuilder, subpart, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                }
                sourceBuilder.append("}");
            }
            sourceBuilder.append(")");
            return false;
        } else if (part instanceof Set) {
            Parsed lhs = ((Set) part).lhs;
            Parsed rhs = ((Set) part).rhs;

            if (lhs instanceof IntegerReference) {
                if (atTop) {
                    transpileParsedSource(sourceBuilder, ((IntegerReference) lhs).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    sourceBuilder.append(".set(");
                    sourceBuilder.append(java.lang.String.valueOf(((IntegerReference) lhs).ref));
                    sourceBuilder.append(", ");
                    transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    sourceBuilder.append(")");
                    return false;
                }

                sourceBuilder.append("callSet(");
                transpileParsedSource(sourceBuilder, ((IntegerReference) lhs).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.append(", ");
                sourceBuilder.append(java.lang.String.valueOf(((IntegerReference) lhs).ref));
                sourceBuilder.append(", ");
                transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.append(")");
                return false;
            } else if (lhs instanceof VariableReference) {
                if (isNumber(((VariableReference) lhs).ref, localStack)) {
                    sourceBuilder.append("Utilities.set(");
                    transpileParsedSource(sourceBuilder, ((VariableReference) lhs).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    sourceBuilder.append(", ");
                    generateNumberSource(sourceBuilder, ((VariableReference) lhs).ref, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    sourceBuilder.append(", ");
                    transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    sourceBuilder.append(")");
                } else {
                    sourceBuilder.append("Utilities.set(");
                    transpileParsedSource(sourceBuilder, ((VariableReference) lhs).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    sourceBuilder.append(", ");
                    transpileParsedSource(sourceBuilder, ((VariableReference) lhs).ref, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    sourceBuilder.append(", ");
                    transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    sourceBuilder.append(")");
                }
                return false;
            } else if (lhs instanceof Reference) {
                if (localStack != null) {
                    java.lang.String type = localStack.get(((Reference) lhs).ref);
                    if (type != null) {
                        if (expectedStack.stackType() == ScopeOptimizer.StackType.SyntheticScopeable) {
                            type = "unknown";
                        }

                        sourceBuilder.append(expectedStack.getReference(((Reference) lhs).ref));
                        sourceBuilder.append(" = ");
                        if (type.equals("string")) {
                            generateStringSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                        } else if (type.equals("number")) {
                            generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                        } else if (type.equals("boolean")) {
                            generateBooleanSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                        } else {
                            transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                        }
                        return false;
                    }
                }

                if (atTop) {
                    sourceBuilder.append(baseScope);
                    sourceBuilder.append(".set(\"");
                    sourceBuilder.append(convertStringSource(((Reference) lhs).ref));
                    sourceBuilder.append("\", ");
                    transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    sourceBuilder.append(")");
                    return false;
                }

                sourceBuilder.append("callSet(");
                sourceBuilder.append(baseScope);
                sourceBuilder.append(", \"");
                sourceBuilder.append(convertStringSource(((Reference) lhs).ref));
                sourceBuilder.append("\", ");
                transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.append(")");
                return false;
            } else if (lhs instanceof ReferenceChain) {
                List<java.lang.String> chain = ((ReferenceChain) lhs).chain;
                if (chain.size() > 1) {
                    java.lang.String key = chain.remove(chain.size() - 1);
                    if (atTop) {
                        generateBaseScopeAccess(sourceBuilder, chain.remove(0), baseScope, expectedStack, localStack);
                        for (java.lang.String k : chain) {
                            sourceBuilder.append(".get(");
                            generateStringNumberIndex(sourceBuilder, k);
                            sourceBuilder.append(")");
                        }
                        sourceBuilder.append(".set(\"");
                        sourceBuilder.append(convertStringSource(key));
                        sourceBuilder.append("\", ");
                        transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                        sourceBuilder.append(")");
                        return false;
                    }

                    sourceBuilder.append("callSet(");
                    generateBaseScopeAccess(sourceBuilder, chain.remove(0), baseScope, expectedStack, localStack);
                    for (java.lang.String k : chain) {
                        sourceBuilder.append(".get(");
                        generateStringNumberIndex(sourceBuilder, k);
                        sourceBuilder.append(")");
                    }
                    sourceBuilder.append(", \"");
                    sourceBuilder.append(convertStringSource(key));
                    sourceBuilder.append("\", ");
                    transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    sourceBuilder.append(")");
                    return false;
                }
            } else if(lhs instanceof Set) {
                transpileParsedSource(sourceBuilder, new Set(((Set)lhs).lhs, new Set(((Set)lhs).rhs, rhs)), methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                return false;
            }

            throw new UnsupportedOperationException("Cannot compile set: " + describe(lhs));
        } else if (part instanceof Try) {
            boolean hasReturn;
            Catch c = ((Try) part).c;
            Finally f = ((Try) part).f;
            if (c != null && f != null) {
                sourceBuilder.appendln("try {");
                sourceBuilder.indent();
                hasReturn = generateBlockSource(sourceBuilder, ((Try) part).impl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
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
                sourceBuilder.append(convertStringSource(((Reference) c.condition).ref));
                sourceBuilder.appendln("\", global.wrap(t));");
                hasReturn = generateBlockSource(sourceBuilder, c.impl, methodPrefix, newScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap) && hasReturn;
                sourceBuilder.unindent();
                sourceBuilder.appendln("} finally {");
                sourceBuilder.append("\t");
                sourceBuilder.append(newScope);
                sourceBuilder.appendln(".exit();");
                sourceBuilder.appendln("}");
                sourceBuilder.unindent();
                sourceBuilder.appendln("} finally {");
                sourceBuilder.indent();
                hasReturn = generateBlockSource(sourceBuilder, f.impl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap) || hasReturn;
                sourceBuilder.unindent();
                sourceBuilder.appendln("}");
                return hasReturn;
            } else if (c != null) {
                sourceBuilder.appendln("try {");
                sourceBuilder.indent();
                hasReturn = generateBlockSource(sourceBuilder, ((Try) part).impl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.unindent();
                sourceBuilder.appendln("} catch(net.nexustools.njs.Error.InvisibleException ex) {");
                sourceBuilder.appendln("\tthrow ex;");
                sourceBuilder.appendln("} catch(Throwable t) {");
                sourceBuilder.indent();
                java.lang.String newScope;
                if (baseScope.equals("catchScope")) {
                    int count;
                    if (baseScope.length() > 10) {
                        count = java.lang.Integer.valueOf(baseScope.substring(10));
                    } else {
                        count = 0;
                    }
                    newScope = "catchScope" + (count + 1);
                } else {
                    newScope = "catchScope";
                }
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
                sourceBuilder.append(convertStringSource(((Reference) c.condition).ref));
                sourceBuilder.appendln("\", global.wrap(t));");
                hasReturn = generateBlockSource(sourceBuilder, c.impl, methodPrefix, newScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap) && hasReturn;
                sourceBuilder.unindent();
                sourceBuilder.appendln("} finally {");
                sourceBuilder.append("\t");
                sourceBuilder.append(newScope);
                sourceBuilder.appendln(".exit();");
                sourceBuilder.appendln("}");
                sourceBuilder.unindent();
                sourceBuilder.appendln("}");
                return hasReturn;
            }

            sourceBuilder.appendln("try {");
            sourceBuilder.indent();
            hasReturn = generateBlockSource(sourceBuilder, ((Try) part).impl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.unindent();
            sourceBuilder.appendln("} finally {");
            sourceBuilder.indent();
            hasReturn = generateBlockSource(sourceBuilder, f.impl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap) || hasReturn;
            sourceBuilder.unindent();
            sourceBuilder.appendln("}");
            return hasReturn;
        } else if (part instanceof If) {
            if (((If) part).simpleimpl != null) {
                sourceBuilder.append("if(");
                generateBooleanSource(sourceBuilder, ((If) part).condition, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.appendln(") {");
                sourceBuilder.indent();
                transpileParsedSource(sourceBuilder, ((If) part).simpleimpl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, true);
                if(!(((If) part).simpleimpl instanceof Block))
                    sourceBuilder.appendln(";");
                else
                    sourceBuilder.appendln();
                sourceBuilder.unindent();
                sourceBuilder.append("}");

                return generateIfBlockSource(sourceBuilder, ((If) part).el, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            }

            boolean hasReturn;
            sourceBuilder.append("if(");
            generateBooleanSource(sourceBuilder, ((If) part).condition, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.appendln(") {");
            sourceBuilder.indent();
            hasReturn = generateBlockSource(sourceBuilder, ((If) part).impl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.unindent();
            sourceBuilder.append("}");

            return generateIfBlockSource(sourceBuilder, ((If) part).el, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap) && hasReturn;
        } else if (part instanceof While) {
            boolean hasReturn = ((While) part).condition.isTrue();
            if (((While) part).simpleimpl != null) {
                sourceBuilder.append("while(");
                generateBooleanSource(sourceBuilder, ((While) part).condition, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.appendln(") {");
                sourceBuilder.indent();
                hasReturn = transpileParsedSource(sourceBuilder, ((While) part).simpleimpl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, true) || hasReturn;
                if(!(((While) part).simpleimpl instanceof Block))
                    sourceBuilder.appendln(";");
                else
                    sourceBuilder.appendln();
                sourceBuilder.append("}");
                return hasReturn;
            }

            sourceBuilder.append("while(");
            generateBooleanSource(sourceBuilder, ((While) part).condition, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.appendln(") {");
            sourceBuilder.indent();
            hasReturn = generateBlockSource(sourceBuilder, ((While) part).impl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap) || hasReturn;
            sourceBuilder.unindent();
            sourceBuilder.append("}");
            return hasReturn;
        } else if (part instanceof For) {
            boolean hasReturn = ((For)part).condition == null || ((For)part).condition.isTrue();
            if (((For) part).simpleimpl != null) {
                switch (((For) part).type) {
                    case InLoop:
                        sourceBuilder.appendln("{");
                        sourceBuilder.indent();
                        sourceBuilder.append("Iterator<java.lang.String> it = ");
                        transpileParsedSource(sourceBuilder, ((For) part).loop, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                        sourceBuilder.appendln(".deepPropertyNameIterator();");
                        sourceBuilder.appendln("while(it.hasNext()) {");
                        sourceBuilder.indent();
                        sourceBuilder.append(baseScope);
                        sourceBuilder.append(".var(\"");
                        sourceBuilder.append(((Reference)((Var) ((For) part).init).sets.get(0).lhs).ref);
                        sourceBuilder.appendln("\", global.String.wrap(it.next()));");
                        hasReturn = transpileParsedSource(sourceBuilder, ((For) part).simpleimpl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap) || hasReturn;
                        if(!(((For) part).simpleimpl instanceof Block))
                            sourceBuilder.appendln(";");
                        else
                            sourceBuilder.appendln();
                        sourceBuilder.unindent();
                        sourceBuilder.appendln("}");
                        sourceBuilder.unindent();
                        sourceBuilder.append("}");
                        break;

                    case OfLoop:
                        sourceBuilder.append("for(BaseObject forObject : ");
                        transpileParsedSource(sourceBuilder, ((For) part).loop, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                        sourceBuilder.appendln(") {");
                        sourceBuilder.indent();
                        sourceBuilder.append(baseScope);
                        sourceBuilder.append(".var(\"");
                        sourceBuilder.append(((Reference)((Var) ((For) part).init).sets.get(0).lhs).ref);
                        sourceBuilder.appendln("\", forObject);");
                        hasReturn = transpileParsedSource(sourceBuilder, ((For) part).simpleimpl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap) || hasReturn;
                        if(!(((For) part).simpleimpl instanceof Block))
                            sourceBuilder.appendln(";");
                        else
                            sourceBuilder.appendln();
                        sourceBuilder.unindent();
                        sourceBuilder.append("}");
                        break;

                    case Standard:
                        if (((For) part).init != null) {
                            transpileParsedSource(sourceBuilder, ((For) part).init, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                            sourceBuilder.appendln(";");
                        }
                        sourceBuilder.append("for(; ");
                        generateBooleanSource(sourceBuilder, ((For) part).condition, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                        sourceBuilder.append("; ");
                        if (((For) part).loop != null)
                            transpileParsedSource(sourceBuilder, ((For) part).loop, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, true);
                        sourceBuilder.appendln(") {");
                        sourceBuilder.indent();
                        hasReturn = transpileParsedSource(sourceBuilder, ((For) part).simpleimpl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, true) || hasReturn;
                        if(!(((For) part).simpleimpl instanceof Block))
                            sourceBuilder.appendln(";");
                        else
                            sourceBuilder.appendln();
                        sourceBuilder.unindent();
                        sourceBuilder.append("}");
                }

                return hasReturn;
            }

            switch (((For) part).type) {
                case InLoop: {
                    sourceBuilder.appendln("{");
                    sourceBuilder.indent();
                    java.lang.String scope;
                    boolean let = ((For) part).init instanceof Let;
                    if (let) {
                        scope = extendScope(baseScope, "letScope");
                        sourceBuilder.append("final Scope ");
                        sourceBuilder.append(scope);
                        sourceBuilder.append(" = ");
                        sourceBuilder.append(baseScope);
                        sourceBuilder.appendln(".beginBlock();");
                    } else {
                        scope = baseScope;
                    }
                    sourceBuilder.append("Iterator<java.lang.String> it = ");
                    transpileParsedSource(sourceBuilder, ((For) part).loop, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    sourceBuilder.appendln(".deepPropertyNameIterator();");
                    sourceBuilder.appendln("while(it.hasNext()) {");
                    sourceBuilder.indent();
                    sourceBuilder.append(scope);
                    if (let) {
                        sourceBuilder.append(".let(\"");
                    } else {
                        sourceBuilder.append(".var(\"");
                    }
                    sourceBuilder.append(((Reference)((Var) ((For) part).init).sets.get(0).lhs).ref);
                    sourceBuilder.appendln("\", global.String.wrap(it.next()));");
                    hasReturn = generateBlockSource(sourceBuilder, ((For) part).impl, methodPrefix, scope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap) || hasReturn;
                    sourceBuilder.unindent();
                    sourceBuilder.appendln("}");
                    sourceBuilder.unindent();
                    sourceBuilder.append("}");
                    break;
                }

                case OfLoop: {
                    java.lang.String scope;
                    boolean let = ((For) part).init instanceof Let;
                    if (let) {
                        sourceBuilder.appendln("{");
                        sourceBuilder.indent();
                        scope = extendScope(baseScope, "letScope");
                        sourceBuilder.append("final Scope ");
                        sourceBuilder.append(scope);
                        sourceBuilder.append(" = ");
                        sourceBuilder.append(baseScope);
                        sourceBuilder.appendln(".beginBlock();");
                    } else {
                        scope = baseScope;
                    }
                    sourceBuilder.append("for(BaseObject forObject : ");
                    transpileParsedSource(sourceBuilder, ((For) part).loop, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    sourceBuilder.appendln(") {");
                    sourceBuilder.indent();
                    Parsed lhs = ((Var) ((For) part).init).sets.get(0).lhs;
                    if(lhs instanceof Reference) {
                        sourceBuilder.append(scope);
                        if (let) {
                            sourceBuilder.append(".let(\"");
                        } else {
                            sourceBuilder.append(".var(\"");
                        }
                        sourceBuilder.append(convertStringSource(((Reference)lhs).ref));
                        sourceBuilder.appendln("\", forObject);");
                    } else if(((NameSet)lhs).array) {
                        int index = 0;
                        for(java.lang.String name : ((NameSet)lhs).names.keySet()) {
                            sourceBuilder.append(scope);
                            if (let) {
                                sourceBuilder.append(".let(\"");
                            } else {
                                sourceBuilder.append(".var(\"");
                            }
                            sourceBuilder.append(convertStringSource(name));
                            sourceBuilder.append("\", forObject.get(");
                            sourceBuilder.append(java.lang.String.valueOf(index++));
                            sourceBuilder.appendln("));");
                        }
                    } else {
                        for(Map.Entry<java.lang.String, java.lang.String> name : ((NameSet)lhs).names.entrySet()) {
                            sourceBuilder.append(scope);
                            if (let) {
                                sourceBuilder.append(".let(\"");
                            } else {
                                sourceBuilder.append(".var(\"");
                            }
                            sourceBuilder.append(convertStringSource(name.getValue()));
                            sourceBuilder.append("\", forObject.get(\"");
                            sourceBuilder.append(convertStringSource(name.getKey()));
                            sourceBuilder.appendln("\"));");
                        }
                    }
                    hasReturn = generateBlockSource(sourceBuilder, ((For) part).impl, methodPrefix, scope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap) || hasReturn;
                    sourceBuilder.unindent();
                    sourceBuilder.append("}");
                    if (let) {
                        sourceBuilder.appendln();
                        sourceBuilder.unindent();
                        sourceBuilder.append("}");
                    }
                    break;
                }

                case Standard:
                    if (((For) part).init != null) {
                        transpileParsedSource(sourceBuilder, ((For) part).init, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                        sourceBuilder.appendln(";");
                    }
                    sourceBuilder.append("for(; ");
                    if(((For) part).condition != null)
                        generateBooleanSource(sourceBuilder, ((For) part).condition, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    sourceBuilder.append("; ");
                    if (((For) part).loop != null)
                        transpileParsedSource(sourceBuilder, ((For) part).loop, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap, true);
                    sourceBuilder.appendln(") {");
                    sourceBuilder.indent();
                    hasReturn = generateBlockSource(sourceBuilder, ((For) part).impl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap) || hasReturn;
                    sourceBuilder.unindent();
                    sourceBuilder.append("}");
            }

            return hasReturn;
        } else if (part instanceof PlusPlus) {
            Parsed ref = ((PlusPlus) part).ref;

            if (localStack != null) {
                if (ref instanceof Reference) {
                    if (expectedStack.stackType() == ScopeOptimizer.StackType.SyntheticScopeable) {
                        if (atTop) {
                            sourceBuilder.append("plusPlusTop(global, ");
                        } else if (((PlusPlus) part).right) {
                            sourceBuilder.append("plusPlusRight(global, ");
                        } else {
                            sourceBuilder.append("plusPlusLeft(global, ");
                        }
                        if (ref instanceof Reference) {
                            sourceBuilder.append("\"");
                            sourceBuilder.append(convertStringSource(((Reference) ref).ref));
                            sourceBuilder.append("\", localStack");
                        } else {
                            throw new UnsupportedOperationException("Cannot compile optimized ++: " + describe(ref));
                        }
                        sourceBuilder.append(")");
                        return false;
                    }

                    if (!atTop) {
                        sourceBuilder.append("global.wrap(");
                    }
                    if (!((PlusPlus) part).right) {
                        sourceBuilder.append("++");
                    }
                    sourceBuilder.append(expectedStack.getReference(((Reference) ref).ref));
                    if (((PlusPlus) part).right) {
                        sourceBuilder.append("++");
                    }
                    if (!atTop) {
                        sourceBuilder.append(")");
                    }
                } else {
                    throw new UnsupportedOperationException("Cannot compile x++: " + describe(ref));
                }
                return false;
            }

            if (atTop) {
                sourceBuilder.append("plusPlusTop(global, ");
            } else if (((PlusPlus) part).right) {
                sourceBuilder.append("plusPlusRight(global, ");
            } else {
                sourceBuilder.append("plusPlusLeft(global, ");
            }
            if (ref instanceof Reference) {
                sourceBuilder.append("\"");
                sourceBuilder.append(convertStringSource(((Reference) ref).ref));
                sourceBuilder.append("\", ");
                sourceBuilder.append(baseScope);
            } else {
                throw new UnsupportedOperationException("Cannot compile ++: " + describe(ref));
            }
            sourceBuilder.append(")");
            return false;
        } else if (part instanceof PlusEq) {
            Parsed ref = ((PlusEq) part).lhs;
            Parsed rhs = ((PlusEq) part).rhs;

            if (localStack != null) {
                // TODO: Fix logic
                if (ref instanceof Reference) {
                    if (!atTop) {
                        sourceBuilder.append("global.wrap(");
                    }

                    sourceBuilder.append(expectedStack.getReference(((Reference) ref).ref));
                    sourceBuilder.append(" += ");
                    generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    if (!atTop) {
                        sourceBuilder.append(")");
                    }
                } else {
                    throw new UnsupportedOperationException("Cannot compile x++: " + describe(ref));
                }
                return false;
            }

            sourceBuilder.append("plusEqual(global, ");
            if (ref instanceof Reference) {
                sourceBuilder.append("\"");
                sourceBuilder.append(convertStringSource(((Reference) ref).ref));
                sourceBuilder.append("\", ");
                transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.append(", ");
                sourceBuilder.append(baseScope);
            } else {
                throw new UnsupportedOperationException("Cannot compile x+=: " + describe(ref));
            }
            sourceBuilder.append(")");
            return false;
        } else if (part instanceof OrEq) {
            Parsed ref = ((OrEq) part).lhs;
            Parsed rhs = ((OrEq) part).rhs;

            if (localStack != null) {
                // TODO: Fix logic
                if (ref instanceof Reference) {
                    if (!atTop) {
                        sourceBuilder.append("global.wrap(");
                    }

                    sourceBuilder.append(expectedStack.getReference(((Reference) ref).ref));
                    sourceBuilder.append(" |= ");
                    generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    if (!atTop) {
                        sourceBuilder.append(")");
                    }
                } else {
                    throw new UnsupportedOperationException("Cannot compile x|=: " + describe(ref));
                }
                return false;
            }

            sourceBuilder.append("orEqual(global, ");
            if (ref instanceof Reference) {
                sourceBuilder.append("\"");
                sourceBuilder.append(convertStringSource(((Reference) ref).ref));
                sourceBuilder.append("\", ");
                generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.append(", ");
                sourceBuilder.append(baseScope);
            } else {
                throw new UnsupportedOperationException("Cannot compile x|=: " + describe(ref));
            }
            sourceBuilder.append(")");
            return false;
        } else if (part instanceof AndEq) {
            Parsed ref = ((AndEq) part).lhs;
            Parsed rhs = ((AndEq) part).rhs;

            if (localStack != null) {
                // TODO: Fix logic
                if (ref instanceof Reference) {
                    if (!atTop) {
                        sourceBuilder.append("global.wrap(");
                    }

                    sourceBuilder.append(expectedStack.getReference(((Reference) ref).ref));
                    sourceBuilder.append(" &= ");
                    generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    if (!atTop) {
                        sourceBuilder.append(")");
                    }
                } else {
                    throw new UnsupportedOperationException("Cannot compile x|=: " + describe(ref));
                }
                return false;
            }

            sourceBuilder.append("andEqual(global, ");
            if (ref instanceof Reference) {
                sourceBuilder.append("\"");
                sourceBuilder.append(convertStringSource(((Reference) ref).ref));
                sourceBuilder.append("\", ");
                generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.append(", ");
                sourceBuilder.append(baseScope);
            } else {
                throw new UnsupportedOperationException("Cannot compile x&=: " + describe(ref));
            }
            sourceBuilder.append(")");
            return false;
        } else if (part instanceof MultiplyEq) {
            Parsed ref = ((MultiplyEq) part).lhs;
            Parsed rhs = ((MultiplyEq) part).rhs;

            sourceBuilder.append("multiplyEqual(global, ");
            if (ref instanceof Reference) {
                sourceBuilder.append("\"");
                sourceBuilder.append(convertStringSource(((Reference) ref).ref));
                sourceBuilder.append("\", ");
                generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.append(", ");
                sourceBuilder.append(baseScope);
            } else {
                throw new UnsupportedOperationException("Cannot compile x*=: " + describe(ref));
            }
            sourceBuilder.append(")");
            return false;
        } else if (part instanceof DivideEq) {
            Parsed ref = ((DivideEq) part).lhs;
            Parsed rhs = ((DivideEq) part).rhs;

            sourceBuilder.append("divideEqual(global, ");
            if (ref instanceof Reference) {
                sourceBuilder.append("\"");
                sourceBuilder.append(convertStringSource(((Reference) ref).ref));
                sourceBuilder.append("\", ");
                generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.append(", ");
                sourceBuilder.append(baseScope);
            } else {
                throw new UnsupportedOperationException("Cannot compile x-=: " + describe(ref));
            }
            sourceBuilder.append(")");
            return false;
        } else if (part instanceof MinusEq) {
            Parsed ref = ((MinusEq) part).lhs;
            Parsed rhs = ((MinusEq) part).rhs;

            sourceBuilder.append("minusEqual(global, ");
            if (ref instanceof Reference) {
                sourceBuilder.append("\"");
                sourceBuilder.append(convertStringSource(((Reference) ref).ref));
                sourceBuilder.append("\", ");
                generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.append(", ");
                sourceBuilder.append(baseScope);
            } else {
                throw new UnsupportedOperationException("Cannot compile x-=: " + describe(ref));
            }
            sourceBuilder.append(")");
            return false;
        } else if (part instanceof DoubleShiftRightEq) {
            Parsed ref = ((DoubleShiftRightEq) part).lhs;
            Parsed rhs = ((DoubleShiftRightEq) part).rhs;

            sourceBuilder.append("dblShiftRightEqual(global, ");
            if (ref instanceof Reference) {
                sourceBuilder.append("\"");
                sourceBuilder.append(convertStringSource(((Reference) ref).ref));
                sourceBuilder.append("\", ");
                generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                sourceBuilder.append(", ");
                sourceBuilder.append(baseScope);
            } else {
                throw new UnsupportedOperationException("Cannot compile x>>>=: " + describe(ref));
            }
            sourceBuilder.append(")");
            return false;
        } else if (part instanceof MinusMinus) {
            Parsed ref = ((MinusMinus) part).ref;

            if (localStack != null) {
                if (ref instanceof Reference) {
                    if (!atTop) {
                        sourceBuilder.append("global.wrap(");
                    }
                    if (!((MinusMinus) part).right) {
                        sourceBuilder.append("--");
                    }
                    sourceBuilder.append(expectedStack.getReference(((Reference) ref).ref));
                    if (((MinusMinus) part).right) {
                        sourceBuilder.append("--");
                    }
                    if (!atTop) {
                        sourceBuilder.append(")");
                    }
                } else {
                    throw new UnsupportedOperationException("Cannot compile x--: " + describe(ref));
                }
                return false;
            }

            if (((MinusMinus) part).right) {
                sourceBuilder.append("minusMinusRight(global, ");
                if (ref instanceof Reference) {
                    sourceBuilder.append("\"");
                    sourceBuilder.append(convertStringSource(((Reference) ref).ref));
                    sourceBuilder.append("\", ");
                    sourceBuilder.append(baseScope);
                } else {
                    throw new UnsupportedOperationException("Cannot compile x++: " + describe(ref));
                }
                sourceBuilder.append(")");
                return false;
            }

            sourceBuilder.append("minusMinusLeft(global, ");
            if (ref instanceof Reference) {
                sourceBuilder.append("\"");
                sourceBuilder.append(convertStringSource(((Reference) ref).ref));
                sourceBuilder.append("\", ");
                sourceBuilder.append(baseScope);
            } else {
                throw new UnsupportedOperationException("Cannot compile ++x: " + describe(ref));
            }
            sourceBuilder.append(")");
            return false;
        } else if (part instanceof OpenGroup) {
            sourceBuilder.append("new GenericObject(global)");
            if (!((OpenGroup) part).entries.isEmpty()) {
                sourceBuilder.appendln("{");
                sourceBuilder.indent();
                sourceBuilder.appendln("{");
                sourceBuilder.indent();
                for (Map.Entry<java.lang.String, Parsed> entry : ((OpenGroup) part).entries.entrySet()) {
                    sourceBuilder.append("setDirectly(\"");
                    sourceBuilder.append(convertStringSource(entry.getKey()));
                    sourceBuilder.append("\", ");
                    transpileParsedSource(sourceBuilder, entry.getValue(), methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                    sourceBuilder.appendln(");");
                }
                sourceBuilder.unindent();
                sourceBuilder.appendln("}");
                sourceBuilder.unindent();
                sourceBuilder.append("}");
            }
            return false;
        } else if (part instanceof Delete) {
            if (atTop) {
                generateBooleanSource(sourceBuilder, part, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                return false;
            }

            Parsed rhs = ((Delete) part).rhs;
            if (rhs instanceof Reference) {
                if (localStack.get(((Reference) rhs).ref) != null) {
                    sourceBuilder.append("global.Boolean.FALSE");
                } else {
                    sourceBuilder.append("(");
                    sourceBuilder.append(baseScope);
                    sourceBuilder.append(".delete(\"");
                    sourceBuilder.append(convertStringSource(((Reference) rhs).ref));
                    sourceBuilder.append("\") ? global.Boolean.TRUE : global.Boolean.FALSE)");
                }
                return false;
            } else if (rhs instanceof ReferenceChain) {
                java.lang.String first = ((ReferenceChain) rhs).chain.remove(0);
                java.lang.String last = ((ReferenceChain) rhs).chain.remove(((ReferenceChain) rhs).chain.size() - 1);

                sourceBuilder.append("(");
                generateBaseScopeAccess(sourceBuilder, first, baseScope, expectedStack, localStack);
                for (java.lang.String key : ((ReferenceChain) rhs).chain) {
                    sourceBuilder.append(".get(\"");
                    sourceBuilder.append(convertStringSource(key));
                    sourceBuilder.append("\")");
                }
                sourceBuilder.append(".delete(\"");
                sourceBuilder.append(convertStringSource(last));
                sourceBuilder.append("\") ? global.Boolean.TRUE : global.Boolean.FALSE)");
                return false;
            }

            throw new UnsupportedOperationException("Cannot compile delete : " + describe(rhs));
        } else if (part instanceof MoreThan) {
            sourceBuilder.append("(moreThan(");
            transpileParsedSource(sourceBuilder, ((MoreThan) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(", ");
            transpileParsedSource(sourceBuilder, ((MoreThan) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
            return false;
        } else if (part instanceof LessThan) {
            sourceBuilder.append("(lessThan(");
            transpileParsedSource(sourceBuilder, ((LessThan) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(", ");
            transpileParsedSource(sourceBuilder, ((LessThan) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
            return false;
        } else if (part instanceof MoreEqual) {
            sourceBuilder.append("(moreEqual(");
            transpileParsedSource(sourceBuilder, ((MoreEqual) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(", ");
            transpileParsedSource(sourceBuilder, ((MoreEqual) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
            return false;
        } else if (part instanceof LessEqual) {
            sourceBuilder.append("(lessEqual(");
            transpileParsedSource(sourceBuilder, ((LessEqual) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(", ");
            transpileParsedSource(sourceBuilder, ((LessEqual) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
            return false;
        } else if (part instanceof VariableReference) {
            Parsed ref = ((VariableReference) part).ref;
            if (localStack != null) {
                if (ref instanceof BaseReferency) {
                    if (ref instanceof Reference) {
                        java.lang.String type = localStack.get(((Reference) ref).ref);
                        if (type != null) {
                            if (type.equals("number")) {
                                sourceBuilder.append("Utilities.get(");
                                transpileParsedSource(sourceBuilder, ((VariableReference) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                                sourceBuilder.append(", ");
                                generateNumberSource(sourceBuilder, ((VariableReference) part).ref, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                                sourceBuilder.append(")");
                                return false;
                            } else if (type.equals("string")) {
                                transpileParsedSource(sourceBuilder, ((VariableReference) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                                sourceBuilder.append(".get(");
                                sourceBuilder.append(expectedStack.getReference(((Reference) ref).ref));
                                sourceBuilder.append(")");
                                return false;
                            }
                        }
                    } else {
                        throw new UnsupportedOperationException("Cannot compile optimized: " + describe(ref));
                    }
                }
            }
            sourceBuilder.append("Utilities.get(");
            transpileParsedSource(sourceBuilder, ((VariableReference) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(", ");
            transpileParsedSource(sourceBuilder, ((VariableReference) part).ref, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(")");
            return false;
        } else if (part instanceof Fork) {
            sourceBuilder.append("(");
            generateBooleanSource(sourceBuilder, ((Fork) part).condition, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(" ? (");
            transpileParsedSource(sourceBuilder, ((Fork) part).success, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(") : (");
            transpileParsedSource(sourceBuilder, ((Fork) part).failure, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append("))");
            return false;
        } else if(part instanceof RegEx) {
            sourceBuilder.append("global.RegEx.create(\"");
            sourceBuilder.append(convertStringSource(((RegEx)part).pattern));
            sourceBuilder.append("\", \"");
            sourceBuilder.append(convertStringSource(((RegEx)part).flags));
            sourceBuilder.append("\")");
            return false;
        } else if(part instanceof Switch) {
            sourceBuilder.append("/* TODO: SWITCHES */");
            return false;
        } else if (part instanceof MultiBracket) {
            boolean first = true;
            sourceBuilder.append("CompiledScript.last(");
            for(Parsed _part : ((MultiBracket)part).parts) {
                if(first)
                    first = false;
                else
                    sourceBuilder.append(", ");
                transpileParsedSource(sourceBuilder, _part, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            }
            sourceBuilder.append(")");
            return false;
        } else if (part instanceof Boolean) {
            sourceBuilder.append("global.Boolean.");
            if (((Boolean) part).value) {
                sourceBuilder.append("TRUE");
            } else {
                sourceBuilder.append("FALSE");
            }
            return false;
        } else if (part instanceof Null) {
            sourceBuilder.append("Null.INSTANCE");
            return false;
        } else if (part instanceof Undefined) {
            sourceBuilder.append("Undefined.INSTANCE");
            return false;
        } else if (part instanceof TemplateLiteral) {
            sourceBuilder.append("global.wrap(");
            boolean first = true;
            for(java.lang.Object _part : ((TemplateLiteral)part).parts) {
                if(first)
                    first = false;
                else
                    sourceBuilder.append(" + ");
                if(_part instanceof Parsed)
                    generateStringSource(sourceBuilder, (Parsed)_part, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
                else {
                    sourceBuilder.append("\"");
                    sourceBuilder.append(convertStringSource(_part.toString()));
                    sourceBuilder.append("\"");
                }
            }
            sourceBuilder.append(")");
            return false;
        } else if (part instanceof In) {
            sourceBuilder.append("(");
            transpileParsedSource(sourceBuilder, ((In)part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(".in(");
            generateStringSource(sourceBuilder, ((In)part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, scopeChain, sourceMap);
            sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
            return false;
        }

        throw new UnsupportedOperationException("Cannot compile: " + describe(part));
    }

    private static enum SourceScope {
        LambdaFunction,
        GlobalFunction,
        GlobalScript,
        SubFunction,
        Function;

        private boolean isNonGlobalFunction() {
            return this == SubFunction || this == Function;
        }

        private boolean isFunction() {
            return this == GlobalFunction || this == SubFunction || this == Function || this == LambdaFunction;
        }

        private boolean isLambda() {
            return this == LambdaFunction;
        }

        private boolean isGlobal() {
            return this == GlobalFunction || this == GlobalScript;
        }
    }

    protected void transpileScriptSource(SourceBuilder sourceBuilder, LocalStack localStack, ScriptData script, java.lang.String methodPrefix, java.lang.String fileName, ClassNameScopeChain scopeChain, SourceScope scope) {
        if (addDebugging || !scope.isFunction()) {
            sourceBuilder.appendln("@Override");
            sourceBuilder.appendln("public String source() {");
            sourceBuilder.append("\treturn \"");
            if (addDebugging) {
                sourceBuilder.append(convertStringSource(script.source));
            } else {
                sourceBuilder.append("[java_code]");
            }
            sourceBuilder.appendln("\";");
            sourceBuilder.appendln("}");
        }
        StackOptimizations opt = (StackOptimizations) script.optimizations;
        boolean usesStackClass = opt != null && opt.stackType() != ScopeOptimizer.StackType.TypedLocal;
        java.lang.String stackName = usesStackClass ? scopeChain.toClassName("Stack") : null;
        if (!scope.isGlobal()) {
            sourceBuilder.appendln("@Override");
            sourceBuilder.appendln("public String name() {");
            sourceBuilder.append("\treturn \"");
            boolean isLambda = script.callee != null && script.callee.isLambda();
            if (script.callee != null && script.callee.name != null) {
                sourceBuilder.append(isLambda ? "<lambda>" : "<anonymous>");
            } else {
                sourceBuilder.append(script.methodName != null ? convertStringSource(script.methodName) : "<anonymous>");
            }
            sourceBuilder.appendln("\";");
            sourceBuilder.appendln("}");

            sourceBuilder.appendln("@Override");
            sourceBuilder.append("public BaseObject call(BaseObject ");
            if(isLambda)
                sourceBuilder.append("__lambda_ignored__");
            else
                sourceBuilder.append("_this");
            sourceBuilder.appendln(", BaseObject... params) {");
            if (opt == null) {
                sourceBuilder.append("\tfinal Scope baseScope = extendScope(");
                if(!isLambda)
                    sourceBuilder.append("_this");
                sourceBuilder.appendln(");");
            }
            if (script.callee != null) {
                methodPrefix = extendMethodChain(methodPrefix, script.callee.name);
                List<java.lang.String> arguments = script.callee.arguments;
                java.lang.String vararg = script.callee.vararg;
                if (opt == null) {
                    sourceBuilder.appendln("\tbaseScope.var(\"arguments\", new Arguments(global, this, params));");
                } else {
                    localStack.put("this", "this");
                    if (usesStackClass) {
                        for (int i = 0; i < arguments.size(); i++) {
                            localStack.put(arguments.get(i), "argument");
                        }
                        if(vararg != null)
                            localStack.put(vararg, "argument");
                        if (localStack.isEmpty()) {
                            sourceBuilder.appendln("\tfinal Scope baseScope = extendScope(_this);");
                        } else {
                            sourceBuilder.append("\tfinal ");
                            sourceBuilder.append(stackName);
                            sourceBuilder.append(" localStack = new ");
                            sourceBuilder.append(stackName);
                            sourceBuilder.appendln("();");
                            if (opt.stackType() == ScopeOptimizer.StackType.TypedClass) {
                                sourceBuilder.appendln("\tfinal Scope baseScope = extendScope(_this);");
                            } else {
                                sourceBuilder.appendln("\tfinal Scope baseScope = extendScope(_this, localStack);");
                            }
                        }
                        if (!arguments.contains("arguments") && opt.usesArguments()) {
                            sourceBuilder.appendln("\tlocalStack.arguments = new Arguments(global, this, params);");
                            localStack.put("arguments", "arguments");
                        }
                    } else {
                        if (!arguments.contains("arguments") && opt.usesArguments()) {
                            sourceBuilder.appendln("\tBaseObject arguments = new Arguments(global, this, params);");
                            localStack.put("arguments", "arguments");
                        }
                        for (int i = 0; i < arguments.size(); i++) {
                            sourceBuilder.append("\tBaseObject ");
                            sourceBuilder.append(arguments.get(i));
                            sourceBuilder.appendln(";");
                            localStack.put(arguments.get(i), "argument");
                        }
                        if(vararg != null) {
                            sourceBuilder.append("\tBaseObject ");
                            sourceBuilder.append(vararg);
                            sourceBuilder.appendln(";");
                            localStack.put(vararg, "argument");
                        }
                        for (java.lang.String key : opt.keys()) {
                            if (key.equals("arguments")) {
                                continue;
                            }
                            java.lang.String type = opt.get(key);
                            if (type.equals("argument")) {
                                continue;
                            }
                            sourceBuilder.append("\t");
                            if (type.equals("string")) {
                                sourceBuilder.append("String");
                            } else if (type.equals("boolean")) {
                                sourceBuilder.append("boolean");
                            } else if (type.equals("number")) {
                                sourceBuilder.append("double");
                            } else if (type.equals("array")) {
                                sourceBuilder.append("GenericArray");
                            } else if (type.equals("object")) {
                                sourceBuilder.append("GenericObject");
                            } else {
                                sourceBuilder.append("BaseObject");
                            }
                            sourceBuilder.append(" ");
                            sourceBuilder.append(key);
                            sourceBuilder.appendln(";");
                        }
                    }
                }
                if (!arguments.isEmpty()) {
                    sourceBuilder.appendln("\tswitch(params.length) {");
                    int argsize = arguments.size();
                    for (int i = 0; i <= argsize; i++) {
                        int a = 0;
                        sourceBuilder.append("\t\t");
                        boolean _default = i == argsize;
                        if (_default) {
                            sourceBuilder.append("default");
                        } else {
                            sourceBuilder.append("case ");
                            sourceBuilder.append(java.lang.String.valueOf(i));
                        }
                        sourceBuilder.appendln(":");
                        for (; a < i; a++) {
                            if (script.optimizations == null) {
                                sourceBuilder.append("\t\t\tbaseScope.var(\"");
                                sourceBuilder.append(convertStringSource(arguments.get(a)));
                                sourceBuilder.append("\", params[");
                                sourceBuilder.append(java.lang.String.valueOf(a));
                                sourceBuilder.appendln("]);");
                            } else {
                                sourceBuilder.append("\t\t\t");
                                if (usesStackClass) {
                                    sourceBuilder.append("localStack.");
                                }
                                sourceBuilder.append(arguments.get(a));
                                sourceBuilder.append(" = params[");
                                sourceBuilder.append(java.lang.String.valueOf(a));
                                sourceBuilder.appendln("];");
                            }
                        }
                        for (; a < argsize; a++) {
                            if (script.optimizations == null) {
                                sourceBuilder.append("\t\t\tbaseScope.var(\"");
                                sourceBuilder.append(convertStringSource(arguments.get(a)));
                                sourceBuilder.appendln("\", Undefined.INSTANCE);");
                            } else {
                                sourceBuilder.append("\t\t\t");
                                if (usesStackClass) {
                                    sourceBuilder.append("localStack.");
                                }
                                sourceBuilder.append(arguments.get(a));
                                sourceBuilder.appendln(" = Undefined.INSTANCE;");
                            }
                        }
                        if(vararg != null) {
                            if(_default) {
                                if (script.optimizations == null) {
                                    sourceBuilder.append("\t\t\tbaseScope.var(\"");
                                    sourceBuilder.append(convertStringSource(vararg));
                                    sourceBuilder.append("\", new GenericArray(global, params, ");
                                    sourceBuilder.append(java.lang.String.valueOf(argsize));
                                    sourceBuilder.appendln("));");
                                } else {
                                    sourceBuilder.append("\t\t\t");
                                    if (usesStackClass) {
                                        sourceBuilder.append("localStack.");
                                    }
                                    sourceBuilder.append(vararg);
                                    sourceBuilder.append(" = new GenericArray(global, params, ");
                                    sourceBuilder.append(java.lang.String.valueOf(argsize));
                                    sourceBuilder.appendln("));");
                                }
                            } else {
                                if (script.optimizations == null) {
                                    sourceBuilder.append("\t\t\tbaseScope.var(\"");
                                    sourceBuilder.append(convertStringSource(vararg));
                                    sourceBuilder.appendln("\", new GenericArray(global));");
                                } else {
                                    sourceBuilder.append("\t\t\t");
                                    if (usesStackClass) {
                                        sourceBuilder.append("localStack.");
                                    }
                                    sourceBuilder.append(vararg);
                                    sourceBuilder.appendln(" = new GenericArray(global);");
                                }
                            }
                        }
                        sourceBuilder.appendln("\t\t\tbreak;");
                    }
                    sourceBuilder.appendln("\t}");
                } else if(vararg != null) {
                    if (script.optimizations == null) {
                        sourceBuilder.append("\tbaseScope.var(\"");
                        sourceBuilder.append(convertStringSource(vararg));
                        sourceBuilder.append("\", new GenericArray(global, params));");
                    } else {
                        sourceBuilder.append("\t");
                        if (usesStackClass) {
                            sourceBuilder.append("localStack.");
                        }
                        sourceBuilder.append(vararg);
                        sourceBuilder.append(" = new GenericArray(global, params);");
                    }
                }
            } else {
                methodPrefix = extendMethodChain(methodPrefix, script.methodName);
            }
        } else {
            sourceBuilder.appendln("@Override");
            if (scope.isFunction()) {
                sourceBuilder.appendln("public BaseObject exec(Global global, final Scope scope) {");
                sourceBuilder.appendln("\tfinal Scope baseScope = scope == null ? Scope.current().beginBlock() : scope;");
                sourceBuilder.appendln("\tfinal BaseObject _this = baseScope._this;");
            } else {
                sourceBuilder.appendln("public BaseObject exec(Global global, Scope scope) {");
                sourceBuilder.appendln("\tfinal Scope baseScope = scope == null ? new Scope(global) : scope;");
                sourceBuilder.appendln("\tfinal BaseObject _this = global;");
            }
        }
        sourceBuilder.indent();
        if (addDebugging) {
            sourceBuilder.append("Utilities.mapCall(\"");
            if (methodPrefix != null) {
                sourceBuilder.append(methodPrefix);
            }
            sourceBuilder.append("\", \"");
            sourceBuilder.append(convertStringSource(fileName));
            sourceBuilder.appendln("\", SOURCE_MAP);");
        }
        if (opt != null) {
            for (Function function : script.functions.values()) {
                if (function.uname == null) {
                    function.uname = scopeChain.toClassName(function.name);
                }
                if (usesStackClass) {
                    sourceBuilder.append("localStack.");
                    sourceBuilder.append(function.uname);
                } else {
                    sourceBuilder.append(function.uname);
                    sourceBuilder.append(" ");
                    sourceBuilder.append(function.name);
                }
                sourceBuilder.append(" = new ");
                sourceBuilder.append(function.uname);
                sourceBuilder.appendln("(");
                if(function.isLambda())
                    sourceBuilder.append("_this, ");
                sourceBuilder.append("global, baseScope);");
                localStack.put(function.name, "function");
            }
        } else {
            for (Function function : script.functions.values()) {
                sourceBuilder.append("baseScope.var(\"");
                sourceBuilder.append(convertStringSource(function.name));
                sourceBuilder.append("\", new ");
                sourceBuilder.append(function.uname = scopeChain.toClassName(function.name));
                sourceBuilder.appendln("(global, baseScope));");
            }
        }
        sourceBuilder.appendln("BaseFunction function;");
        sourceBuilder.appendln("BaseObject __this;");
        if (opt == null || usesStackClass) {
            sourceBuilder.appendln("baseScope.enter();");
            sourceBuilder.appendln("try {");
            sourceBuilder.indent();
        }
        Map<java.lang.Integer, FilePosition> sourceMap = new LinkedHashMap();
        StackOptimizations expectedStack = (StackOptimizations) script.optimizations;
        if (scope.isFunction()) {
            boolean hasReturn = false;
            for (Parsed part : script.impl) {
                if (addDebugging) {
                    addSourceMapEntry(sourceBuilder, sourceMap, part);
                }
                hasReturn = transpileParsedSource(sourceBuilder, part, methodPrefix, "baseScope", fileName, localStack, expectedStack, script.functions, scopeChain, sourceMap, true) || hasReturn;
                if(!(part instanceof Block))
                    sourceBuilder.appendln(";");
                else
                    sourceBuilder.appendln();
            }

            if (!hasReturn) {
                sourceBuilder.appendln("return Undefined.INSTANCE;");
            }
        } else if (script.impl.length > 0) {
            boolean hasReturn = false, atTop = true;
            for (int i = 0; i < script.impl.length; i++) {
                Parsed part = script.impl[i];
                if (addDebugging) {
                    addSourceMapEntry(sourceBuilder, sourceMap, part);
                }
                hasReturn = transpileParsedSource(sourceBuilder, part, methodPrefix, "baseScope", fileName, localStack, expectedStack, script.functions, scopeChain, sourceMap, atTop) || hasReturn;
                if(!(part instanceof Block))
                    sourceBuilder.appendln(";");
                else
                    sourceBuilder.appendln();
            }
            if (!hasReturn) {
                sourceBuilder.appendln("return Undefined.INSTANCE;");
            }
        } else {
            sourceBuilder.appendln("return Undefined.INSTANCE;");
        }
        if (opt == null || usesStackClass) {
            sourceBuilder.unindent();
            sourceBuilder.appendln("} finally {");
            sourceBuilder.appendln("\tbaseScope.exit();");
            sourceBuilder.appendln("}");
        }
        sourceBuilder.unindent();
        sourceBuilder.appendln("}");

        if (usesStackClass && !localStack.isEmpty()) {
            sourceBuilder.append("private ");
            //if(scope.isGlobal())
            sourceBuilder.append("static ");
            sourceBuilder.append("final class ");
            sourceBuilder.append(stackName);
            if (opt.stackType() == ScopeOptimizer.StackType.SyntheticScopeable) {
                sourceBuilder.append(" extends SyntheticScopable");
            }
            sourceBuilder.appendln(" {");
            sourceBuilder.indent();
            for (java.lang.String key : localStack.keys()) {
                if (key.equals("this")) {
                    continue;
                }

                if (opt.stackType() == ScopeOptimizer.StackType.TypedClass) {
                    java.lang.String type = localStack.get(key);
                    if (type.equals("string")) {
                        sourceBuilder.append("String");
                    } else if (type.equals("boolean")) {
                        sourceBuilder.append("boolean");
                    } else if (type.equals("number")) {
                        sourceBuilder.append("double");
                    } else if (type.equals("array")) {
                        sourceBuilder.append("GenericArray");
                    } else if (type.equals("object")) {
                        sourceBuilder.append("GenericObject");
                    } else {
                        sourceBuilder.append("BaseObject");
                    }
                } else {
                    sourceBuilder.append("BaseObject");
                }
                sourceBuilder.append(" ");
                sourceBuilder.append(key);
                sourceBuilder.appendln(";");
            }

            if (opt.stackType() == ScopeOptimizer.StackType.SyntheticScopeable) {
                sourceBuilder.appendln("@Override");
                sourceBuilder.appendln("public BaseObject get(java.lang.String key, Or<BaseObject> or) {");
                sourceBuilder.indent();
                boolean _else = false;
                for (java.lang.String key : localStack.keys()) {
                    if (key.equals("this")) {
                        continue;
                    }
                    if (_else) {
                        sourceBuilder.append("else ");
                    } else {
                        _else = true;
                    }
                    sourceBuilder.append("if(key.equals(\"");
                    sourceBuilder.append(convertStringSource(key));
                    sourceBuilder.appendln("\"))");
                    sourceBuilder.append("\treturn this.");
                    sourceBuilder.append(key);
                    sourceBuilder.appendln(";");
                }
                sourceBuilder.appendln("return or.or(key);");
                sourceBuilder.unindent();
                sourceBuilder.appendln("}");

                sourceBuilder.appendln("@Override");
                sourceBuilder.appendln("public void set(java.lang.String key, BaseObject val, Or<Void> or) {");
                sourceBuilder.indent();
                _else = false;
                for (java.lang.String key : localStack.keys()) {
                    if (key.equals("this")) {
                        continue;
                    }
                    if (_else) {
                        sourceBuilder.append(" else ");
                    } else {
                        _else = true;
                    }
                    sourceBuilder.append("if(key.equals(\"");
                    sourceBuilder.append(convertStringSource(key));
                    sourceBuilder.appendln("\")) {");
                    sourceBuilder.append("\tthis.");
                    sourceBuilder.append(key);
                    sourceBuilder.appendln(" = val;");
                    sourceBuilder.appendln("\treturn;");
                    sourceBuilder.append("}");
                }
                sourceBuilder.appendln();
                sourceBuilder.appendln("or.or(key);");
                sourceBuilder.unindent();
                sourceBuilder.appendln("}");

                sourceBuilder.appendln("@Override");
                sourceBuilder.appendln("public boolean delete(java.lang.String key, Or<Boolean> or) {");
                sourceBuilder.indent();
                _else = false;
                for (java.lang.String key : localStack.keys()) {
                    if (key.equals("this")) {
                        continue;
                    }
                    if (_else) {
                        sourceBuilder.append("else ");
                    } else {
                        _else = true;
                    }
                    sourceBuilder.append("if(key.equals(\"");
                    sourceBuilder.append(convertStringSource(key));
                    sourceBuilder.appendln("\"))");
                    sourceBuilder.appendln("\treturn false;");
                }
                sourceBuilder.appendln("return or.or(key);");
                sourceBuilder.unindent();
                sourceBuilder.appendln("}");
            }

            sourceBuilder.unindent();
            sourceBuilder.appendln("}");
        }

        for (Function function : script.functions.values()) {
            java.lang.String functionName;
            //if(scope.isGlobal())
            sourceBuilder.append("private static final ");
            sourceBuilder.append("class ");
            if (function.uname == null) {
                function.uname = scopeChain.toClassName(function.name);
            }
            sourceBuilder.append(functionName = function.uname);
            sourceBuilder.append(" extends Compiled");
            sourceBuilder.append(function.isLambda() ? "Lambda" : "Function");
            sourceBuilder.appendln(" {");
            sourceBuilder.indent();
            sourceBuilder.appendln("private final Scope baseScope;");
            StackOptimizations funcopt = function.impl == null ? null : (StackOptimizations)function.impl.optimizations;
            
            sourceBuilder.append("private ");
            sourceBuilder.append(functionName);
            sourceBuilder.append("(");
            if(function.isLambda())
                sourceBuilder.append("BaseObject _this, ");
            sourceBuilder.appendln("Global global, Scope scope) {");
            sourceBuilder.append("\tsuper(");
            if(function.isLambda())
                sourceBuilder.append("_this, ");
            sourceBuilder.appendln("global);");
            sourceBuilder.appendln("\tbaseScope = scope;");
            sourceBuilder.appendln("}");

            LocalStack funcLocalStack = function.impl.optimizations == null ? null : new LocalStack(true);
            transpileScriptSource(sourceBuilder, funcLocalStack, function.impl, methodPrefix, fileName, scopeChain.extend(), scope.isNonGlobalFunction() ? SourceScope.SubFunction : SourceScope.Function);

            if (funcopt == null || (funcopt.stackType() != ScopeOptimizer.StackType.TypedLocal && (funcopt.stackType() == ScopeOptimizer.StackType.TypedClass || funcLocalStack.isEmpty()))) {
                sourceBuilder.append("Scope extendScope(");
                if(!function.isLambda())
                    sourceBuilder.append("BaseObject _this");
                sourceBuilder.appendln(") {");
                sourceBuilder.appendln("\treturn baseScope.extend(_this);");
                sourceBuilder.appendln("}");
            } else if (funcopt.stackType() != ScopeOptimizer.StackType.TypedLocal) {
                sourceBuilder.appendln("Scope extendScope(");
                if(!function.isLambda())
                    sourceBuilder.append("BaseObject _this, ");
                sourceBuilder.appendln("Scopable stack) {");
                sourceBuilder.appendln("\treturn baseScope.extend(_this, stack);");
                sourceBuilder.appendln("}");
            }

            sourceBuilder.unindent();
            sourceBuilder.appendln("}");
        }

        if (addDebugging) {
            sourceBuilder.append("public");
            //if(scope != SourceScope.SubFunction)
            sourceBuilder.append(" static");
            sourceBuilder.append(" final Map<Integer, Utilities.FilePosition> SOURCE_MAP = Collections.unmodifiableMap(new LinkedHashMap<Integer, Utilities.FilePosition>()");
            if (!sourceMap.isEmpty()) {
                sourceBuilder.appendln(" {");
                sourceBuilder.appendln("\t{");
                for (Map.Entry<java.lang.Integer, FilePosition> entry : sourceMap.entrySet()) {
                    FilePosition fpos = entry.getValue();
                    if(fpos.row == 0 || fpos.column == 0)
                        continue;
                    
                    sourceBuilder.append("\t\tput(");
                    sourceBuilder.append("" + entry.getKey());
                    sourceBuilder.append(", new Utilities.FilePosition(");
                    sourceBuilder.append("" + entry.getValue().row);
                    sourceBuilder.append(", ");
                    sourceBuilder.append("" + entry.getValue().column);
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
        sourceBuilder.appendLicense();
        if (pkg != null && !pkg.isEmpty()) {
            sourceBuilder.appendln("package " + pkg + ";");
            sourceBuilder.appendln();
        }
        sourceBuilder.appendln("import net.nexustools.njs.compiler.CompiledScript;");
        sourceBuilder.appendln("import net.nexustools.njs.compiler.CompiledLambda;");
        sourceBuilder.appendln("import net.nexustools.njs.compiler.CompiledFunction;");
        sourceBuilder.appendln("import net.nexustools.njs.compiler.SyntheticScopable;");
        sourceBuilder.appendln();
        sourceBuilder.appendln("import net.nexustools.njs.BaseObject;");
        sourceBuilder.appendln("import net.nexustools.njs.GenericObject;");
        sourceBuilder.appendln("import net.nexustools.njs.GenericArray;");
        sourceBuilder.appendln("import net.nexustools.njs.BaseFunction;");
        sourceBuilder.appendln("import net.nexustools.njs.Undefined;");
        sourceBuilder.appendln("import net.nexustools.njs.Utilities;");
        sourceBuilder.appendln("import net.nexustools.njs.Arguments;");
        sourceBuilder.appendln("import net.nexustools.njs.Scopable;");
        sourceBuilder.appendln("import net.nexustools.njs.Global;");
        sourceBuilder.appendln("import net.nexustools.njs.Scope;");
        sourceBuilder.appendln("import net.nexustools.njs.Null;");
        sourceBuilder.appendln();
        sourceBuilder.appendln("import java.util.Iterator;");
        if (addDebugging) {
            sourceBuilder.appendln("import java.util.Collections;");
            sourceBuilder.appendln("import java.util.LinkedHashMap;");
            sourceBuilder.appendln("import java.util.Map;");
        }
        sourceBuilder.appendln();
        sourceBuilder.appendln();

        sourceBuilder.append("public final class ");
        BASE_CLASS_NAME_SCOPE_CHAIN.toClassName(className, true);
        sourceBuilder.append(className);
        sourceBuilder.append(" extends CompiledScript.");
        sourceBuilder.append(addDebugging ? "Debuggable" : "Optimized");
        sourceBuilder.appendln("{");
        sourceBuilder.indent();
        
        sourceBuilder.append("public ");
        sourceBuilder.append(className);
        sourceBuilder.appendln("(Global global) {");
        sourceBuilder.appendln("\tsuper(global);");
        sourceBuilder.appendln("}");
        
        sourceBuilder.append("public ");
        sourceBuilder.append(className);
        sourceBuilder.appendln("() {}");
        

        try {
            if (inFunction) {
                ScopeOptimizer variableScope = new ScopeOptimizer();
                try {
                    scanScriptSource(script, variableScope);
                    script.optimizations = new MapStackOptimizations(variableScope.scope, variableScope.stackType, variableScope instanceof FunctionScopeOptimizer ? ((FunctionScopeOptimizer) variableScope).usesArguments : false);
                } catch (CannotOptimize ex) {
                    if (DEBUG || ex instanceof CannotOptimizeUnimplemented) {
                        ex.printStackTrace(System.out);
                    }
                }
            }
            transpileScriptSource(sourceBuilder, script.optimizations == null ? null : new LocalStack(!inFunction), script, script.methodName, fileName, BASE_CLASS_NAME_SCOPE_CHAIN.extend(), inFunction ? SourceScope.GlobalFunction : SourceScope.GlobalScript);
        } catch (RuntimeException t) {
            System.err.println(sourceBuilder.toString());
            throw t;
        }

        if (generateMain) {
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
        if (baseScope.equals(newScope)) {
            int count;
            if (baseScope.length() > 10) {
                count = java.lang.Integer.valueOf(baseScope.substring(10));
            } else {
                count = 0;
            }
            return newScope + (count + 1);
        } else {
            return newScope;
        }
    }

    @Override
    protected Script compileScript(ScriptData script, java.lang.String fileName, boolean inFunction) {
        final java.lang.String className;
        if (fileName.endsWith(".js")) {
            className = BASE_CLASS_NAME_SCOPE_CHAIN.toClassName(fileName.substring(0, fileName.length() - 3));
        } else {
            className = BASE_CLASS_NAME_SCOPE_CHAIN.toClassName(fileName);
        }
        final java.lang.String source = transpileJavaClassSource(script, className, fileName, "net.nexustools.njs.gen", inFunction, false);
        final java.lang.String classPath = "net.nexustools.njs.gen." + className;

        if (DUMP_SOURCE) {
            System.out.println(source);
        }

        try {
            return (Script) JavaSourceCompiler.compileToClass(classPath.replace(".", "/") + ".java", "net.nexustools.njs.gen." + className, source).newInstance();
        } catch (ClassNotFoundException ex) {
            System.err.println(source);
            throw new net.nexustools.njs.Error.JavaException("EvalError", ex.toString(), ex);
        } catch (InstantiationException ex) {
            System.err.println(source);
            throw new net.nexustools.njs.Error.JavaException("EvalError", ex.toString(), ex);
        } catch (IllegalAccessException ex) {
            System.err.println(source);
            throw new net.nexustools.njs.Error.JavaException("EvalError", ex.toString(), ex);
        } catch (RuntimeException ex) {
            System.out.println(source);
            throw new net.nexustools.njs.Error.JavaException("EvalError", ex.toString(), ex);
        }
    }

    public java.lang.String transpile(java.lang.String input, java.lang.String className, java.lang.String pkg, boolean generateMain) throws FileNotFoundException {
        if (input.equals("-")) {
            return transpile(new InputStreamReader(System.in), "System.in", className, pkg, generateMain);
        }
        return transpile(new FileReader(input), input, className, pkg, generateMain);
    }

    public java.lang.String transpile(InputStreamReader reader, java.lang.String input, java.lang.String className, java.lang.String pkg, boolean generateMain) {
        return transpileJavaClassSource(parse(reader, className, false), className, input, pkg, false, generateMain);
    }

    public static void main(java.lang.String... args) {
        boolean addDebugging = true, generateMain = true;
        java.lang.String input = null, output = null, pkg = null, rclassname = null;
        File root = null;
        for (int i = 0; i < args.length; i++) {
            java.lang.String arg = args[i];
            if (arg.startsWith("-") && arg.length() > 1) {
                if (arg.equals("-s") || arg.equals("-strip") || arg.equals("--strip")) {
                    addDebugging = false;
                } else if (arg.equals("-r") || arg.equals("-root") || arg.equals("--root")) {
                    root = new File(args[++i]);
                } else if (arg.equals("-m") || arg.equals("-nomain") || arg.equals("--nomain") || arg.equals("--no-main")) {
                    generateMain = false;
                } else if (arg.equals("-p") || arg.equals("-package") || arg.equals("--package")) {
                    pkg = args[++i];
                } else if (arg.equals("-c") || arg.equals("-class") || arg.equals("--class")) {
                    rclassname = args[++i];
                } else if (arg.equals("-h") || arg.equals("-help") || arg.equals("--help")) {
                    throw new UnsupportedOperationException();
                } else {
                    System.err.println("No such commandline argument " + arg);
                    System.exit(1);
                    return;
                }
            } else if (input == null) {
                input = arg;
            } else if (output == null) {
                output = arg;
            } else {
                System.err.println("Too many arguments");
                System.exit(1);
                return;
            }
        }
        
        System.out.println(input + " -> " + output);
        if (input == null) {
            System.err.println("No input specified");
            System.exit(1);
            return;
        }

        JavaTranspiler compiler = new JavaTranspiler(addDebugging);

        final java.lang.String className;
        final java.lang.String baseName;
        if (output == null) {
            int pos = input.lastIndexOf(".");
            if (pos > -1) {
                output = input.substring(0, pos) + ".java";
            } else {
                output = input + ".java";
            }
        }
        int pos = output.lastIndexOf(File.separatorChar);
        if (pos > -1) {
            baseName = output.substring(pos + 1);
        } else {
            baseName = output;
        }
        if(rclassname != null) {
            className = rclassname;
        } else if (baseName.endsWith(".java")) {
            className = compiler.BASE_CLASS_NAME_SCOPE_CHAIN.toClassName(baseName.substring(0, baseName.length() - 5));
        } else {
            className = compiler.BASE_CLASS_NAME_SCOPE_CHAIN.toClassName(baseName);
        }
        if (pkg == null) {
            if (pos > -1) {
                pkg = output.substring(0, pos).replaceAll(File.separator, ".");
                if (pkg.startsWith("src.")) {
                    pkg = pkg.substring(4);
                } else if (pkg.startsWith("source.")) {
                    pkg = pkg.substring(7);
                }
            }
        }

        java.lang.String source;
        try {
            if(root == null)
                source = compiler.transpile(input, className, pkg, generateMain);
            else {
                java.lang.String rootPath = root.getAbsolutePath();
                if(!rootPath.endsWith(File.separator) && !rootPath.endsWith("/"))
                    rootPath += File.separator;
                if(input.startsWith(rootPath)) {
                    java.lang.String name = input.substring(rootPath.length());
                    source = compiler.transpile(new FileReader(input), name, className, pkg, generateMain);
                } else
                    source = compiler.transpile(input, className, pkg, generateMain);
                
            }
        } catch (FileNotFoundException ex) {
            System.err.println("Input does not exist...");
            ex.printStackTrace();
            System.exit(1);
            return;
        }

        OutputStream out;
        if (output.equals("-")) {
            out = System.out;
        } else {
            File dir;
            if (pos > -1) {
                dir = new File(output.substring(0, pos));
                if (!dir.exists() && !dir.mkdirs()) {
                    System.err.println("Output directory could not be created...");
                    System.err.println(output.substring(0, pos));
                    System.exit(1);
                }
            } else {
                dir = new File(".");
            }
            if (!dir.isDirectory()) {
                System.err.println("Output parent is not a directory...");
                System.err.println(dir.toString());
                System.exit(1);
            }
            try {
                File outputFile = new File(dir, output);
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
