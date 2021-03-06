/* 
 * Copyright (C) 2017 NexusTools.
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
import java.io.Reader;
import java.util.ArrayList;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.nexustools.njs.Utilities.FilePosition;

/**
 *
 * @author Katelyn Slater <kate@nexustools.com>
 */
public class JavaTranspiler extends RegexCompiler {
    
    private static class SuperSourceBuilder extends SourceBuilder {
        boolean first = true;
        boolean _super = true;
        final SourceBuilder parent;
        public SuperSourceBuilder() {
            this(new SourceBuilder());
        }
        public SuperSourceBuilder(SourceBuilder parent) {
            this.parent = parent;
        }

        @Override
        public void appendLicense() {
            parent.appendLicense(); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void append(java.lang.String source) {
            if(first) {
                _super = source.startsWith("_super");
                first = false;
            }
            parent.append(source);
        }

        @Override
        public void appendln(java.lang.String source) {
            parent.appendln(source);
        }

        @Override
        public void appendln() {
            parent.appendln();
        }

        @Override
        public java.lang.String toString() {
            return parent.toString();
        }
        
    }

    private static enum SwitchType {
        Enum,
        Integer
    }
    
    private static class GetterSetter {
        ClassMethod getter;
        ClassMethod setter;
    }
    
    private static class SwitchEnum {
        public final Map<java.lang.String, Parsed> keys = new LinkedHashMap();

        private java.lang.String key(java.lang.String key, Parsed part) {
            int index = 0;
            java.lang.String k = key;
            do {
                try {
                    return tryKey(k, part);
                } catch (Throwable ex) {
                    k = key + (++index);
                }
            } while(true);
        }
        private java.lang.String tryKey(java.lang.String key, Parsed part) throws Throwable {
            for(java.lang.String k : keys.keySet())
                if(k.equals(key))
                    throw new Throwable();
            keys.put(key, part);
            return key;
        }
    }

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

        java.lang.String refName(java.lang.String name) {
            java.lang.String output = name.replaceAll("[^a-zA-Z0-9_]", "_");
            if (!output.equals(name)) {
                output += Math.abs(name.hashCode());
            }
            return refName(output, true);
        }

        protected abstract java.lang.String refName(java.lang.String name, boolean create);

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
        public java.lang.String refName(java.lang.String name, boolean create) {
            java.lang.String output = parent.refName(name, false);
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
                    return refName(name + "_" + num, true);
                }
                return name;
            } else
                return output;
        }
    }
    private ClassNameScopeChain BASE_CLASS_NAME_SCOPE_CHAIN = new ClassNameScopeChain() {
        @Override
        public java.lang.String refName(java.lang.String name, boolean create) {
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
                return refName(name + "_" + num, true);
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

        private final boolean usesArgumentsOrHasLet;
        private final ScopeOptimizer.StackType stackType;
        private final Map<java.lang.String, java.lang.String> map;

        public MapStackOptimizations(Map<java.lang.String, java.lang.String> map, ScopeOptimizer.StackType stackType, boolean usesArguments) {
            this.usesArgumentsOrHasLet = usesArguments;
            this.stackType = stackType;
            this.map = map;
        }

        @Override
        public boolean usesArguments() {
            return usesArgumentsOrHasLet;
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

    private java.lang.String extendMethodChain(java.lang.String methodPrefix, java.lang.String methodName, boolean lambda) {
        boolean methodPrefixIsntNull = false, methodNameIsNull = methodName == null;
        if (methodPrefix != null) {
            if (methodPrefix.equals(lambda ? "<lambda>" : "<anonymous>")) {
                if (methodNameIsNull) {
                    return methodPrefix;
                }
                methodPrefix = null;
            } else if (methodPrefix.endsWith(lambda ? ".<lambda>" : ".<anonymous>")) {
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
            return methodPrefix + '.' + (lambda ? "<lambda>" : "<anonymous>");
        } else if (!methodNameIsNull) {
            return methodName;
        } else {
            return lambda ? "<lambda>" : "<anonymous>";
        }
    }

    private void generateStringSource(SourceBuilder sourceBuilder, Parsed part, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, StackOptimizations expectedStack, Map<java.lang.String, Function> functionMap, Map<java.lang.String, Class> classMap, ClassNameScopeChain scopeChain, Map<java.lang.Integer, FilePosition> sourceMap, List<java.lang.Object> extras, boolean[] hasBreak, TranspileContext context) {
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
                    generateStringSource(sourceBuilder, (Parsed)_part, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                else {
                    sourceBuilder.append("\"");
                    sourceBuilder.append(convertStringSource(_part.toString()));
                    sourceBuilder.append("\"");
                }
            }
            sourceBuilder.append(")");
        } else if (part instanceof Plus && ((Plus) part).isStringReferenceChain()) {
            generateStringSource(sourceBuilder, ((Plus) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(" + ");
            generateStringSource(sourceBuilder, ((Plus) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
            transpileParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(".toString()");
        }
    }

    private void generateNumberSource(SourceBuilder sourceBuilder, Parsed part, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, StackOptimizations expectedStack, Map<java.lang.String, Function> functionMap, Map<java.lang.String, Class> classMap, ClassNameScopeChain scopeChain, Map<java.lang.Integer, FilePosition> sourceMap, List<java.lang.Object> extras, boolean[] hasBreak, TranspileContext context) {
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
            generateMath(sourceBuilder, lhs, rhs, ">>>", methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, MathWrap.Double);
        } else if (part instanceof ShiftLeft) {
            Parsed lhs = ((ShiftLeft) part).lhs;
            Parsed rhs = ((ShiftLeft) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, "<<", methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, MathWrap.Double);
        } else if (part instanceof ShiftRight) {
            Parsed lhs = ((ShiftRight) part).lhs;
            Parsed rhs = ((ShiftRight) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, ">>", methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, MathWrap.Double);
        } else if (part instanceof Plus) {
            Parsed lhs = ((Plus) part).lhs;
            Parsed rhs = ((Plus) part).rhs;
            if(lhs == null)
                generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            else
                generateMath(sourceBuilder, lhs, rhs, '+', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, MathWrap.Double);
        } else if (part instanceof Multiply) {
            Parsed lhs = ((Multiply) part).lhs;
            Parsed rhs = ((Multiply) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '*', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, MathWrap.Double);
        } else if (part instanceof Divide) {
            Parsed lhs = ((Divide) part).lhs;
            Parsed rhs = ((Divide) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '/', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, MathWrap.Double);
        } else if (part instanceof And) {
            Parsed lhs = ((And) part).lhs;
            Parsed rhs = ((And) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '&', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, MathWrap.Double);
        } else if (part instanceof Or) {
            Parsed lhs = ((Or) part).lhs;
            Parsed rhs = ((Or) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '|', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, MathWrap.Double);
        } else if (part instanceof Percent) {
            Parsed lhs = ((Percent) part).lhs;
            Parsed rhs = ((Percent) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '%', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, MathWrap.Double);
        } else if (part instanceof Minus) {
            Parsed lhs = ((Minus) part).lhs;
            Parsed rhs = ((Minus) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '-', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, MathWrap.Double);
        } else if (part instanceof OpenBracket) {
            generateNumberSource(sourceBuilder, ((OpenBracket) part).contents, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
            transpileParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(").value");
        }
    }

    private void generateLongSource(SourceBuilder sourceBuilder, Parsed part, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, StackOptimizations expectedStack, Map<java.lang.String, Function> functionMap, Map<java.lang.String, Class> classMap, ClassNameScopeChain scopeChain, Map<java.lang.Integer, FilePosition> sourceMap, List<java.lang.Object> extras, boolean[] hasBreak, TranspileContext context) {
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
            if(lhs == null)
                generateLongSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            else
                generateMath(sourceBuilder, lhs, rhs, '+', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, MathWrap.Long);
        } else if (part instanceof Multiply) {
            Parsed lhs = ((Multiply) part).lhs;
            Parsed rhs = ((Multiply) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '*', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, MathWrap.Long);
        } else if (part instanceof Divide) {
            Parsed lhs = ((Divide) part).lhs;
            Parsed rhs = ((Divide) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '/', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, MathWrap.Long);
        } else if (part instanceof And) {
            Parsed lhs = ((And) part).lhs;
            Parsed rhs = ((And) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '&', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, MathWrap.Long);
        } else if (part instanceof Or) {
            Parsed lhs = ((Or) part).lhs;
            Parsed rhs = ((Or) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '|', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, MathWrap.Long);
        } else if (part instanceof Percent) {
            Parsed lhs = ((Percent) part).lhs;
            Parsed rhs = ((Percent) part).rhs;
            sourceBuilder.append("(long)");
            generateMath(sourceBuilder, lhs, rhs, '%', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, MathWrap.Long);
        } else if (part instanceof Minus) {
            Parsed lhs = ((Minus) part).lhs;
            Parsed rhs = ((Minus) part).rhs;
            sourceBuilder.append("(long)");
            generateMath(sourceBuilder, lhs, rhs, '-', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, MathWrap.Long);
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
                        throw new CannotOptimizeUnimplemented("Cannot compile optimized long : " + describe(part));
                    }
                }
            }

            sourceBuilder.append("(long)");
            sourceBuilder.append("global.Number.fromValueOf(");
            transpileParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(").value");
        }
    }
    
    private static enum TranspileContext {
        Script,
        Function,
        ClassMethod
    }

    private static enum MathWrap {
        BaseObject,
        Long,
        Double,
        String,
        Void
    }

    private void generateMath(SourceBuilder sourceBuilder, Parsed lhs, Parsed rhs, char op, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, StackOptimizations expectedStack, Map<java.lang.String, Function> functionMap, Map<java.lang.String, Class> classMap, ClassNameScopeChain scopeChain, Map<java.lang.Integer, FilePosition> sourceMap, List<java.lang.Object> extras, boolean[] hasBreak, TranspileContext context, MathWrap wrap) {
        generateMath(sourceBuilder, lhs, rhs, "" + op, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, wrap);
    }

    private void generateMath(SourceBuilder sourceBuilder, Parsed lhs, Parsed rhs, java.lang.String op, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, StackOptimizations expectedStack, Map<java.lang.String, Function> functionMap, Map<java.lang.String, Class> classMap, ClassNameScopeChain scopeChain, Map<java.lang.Integer, FilePosition> sourceMap, List<java.lang.Object> extras, boolean[] hasBreak, TranspileContext context, MathWrap wrap) {
        boolean wrapAsBaseObject = wrap == MathWrap.BaseObject;
        if (op.equals("+") && (wrapAsBaseObject || wrap == MathWrap.String)) {
            if ((lhs instanceof StringReferency && !isNumber(lhs, localStack)) || (rhs instanceof StringReferency && !isNumber(rhs, localStack))) {
                if (lhs instanceof String && ((String) lhs).string.isEmpty()) {
                    if (wrapAsBaseObject) {
                        transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                        sourceBuilder.append("._toString()");
                    } else {
                        generateStringSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    }
                    return;
                }

                if (wrapAsBaseObject) {
                    sourceBuilder.append("global.wrap(");
                } else {
                    sourceBuilder.append("(");
                }
                generateStringSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(" + ");
                generateStringSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(")");
                return;
            } else if (((lhs instanceof Plus && ((Plus) lhs).isStringReferenceChain()) || (rhs instanceof Plus && ((Plus) rhs).isStringReferenceChain()))) {
                if (wrapAsBaseObject) {
                    sourceBuilder.append("global.wrap(");
                }
                generateStringSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(" + ");
                generateStringSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                if (wrapAsBaseObject) {
                    sourceBuilder.append(")");
                }
                return;
            } else if (!(lhs instanceof NumberReferency) && !(rhs instanceof NumberReferency)) {
                if (!wrapAsBaseObject) {
                    sourceBuilder.append("global.Number.fromValueOf(");
                }
                sourceBuilder.append("plus(global, ");
                transpileParsedSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(", ");
                transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
            generateLongSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
        } else {
            generateNumberSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
        }
        sourceBuilder.append(" ");
        sourceBuilder.append(op);
        sourceBuilder.append(" ");
        if (andOrOr) {
            generateLongSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
        } else {
            generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
        }
        sourceBuilder.append(")");
    }

    private boolean generateIfBlockSource(SourceBuilder sourceBuilder, Else els, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, StackOptimizations expectedStack, Map<java.lang.String, Function> functionMap, Map<java.lang.String, Class> classMap, ClassNameScopeChain scopeChain, Map<java.lang.Integer, FilePosition> sourceMap, List<java.lang.Object> extras, boolean[] hasBreak, TranspileContext context) {
        boolean hasReturn = true, hasElse = false;
        while (els != null) {
            if (els.simpleimpl != null) {
                if (els instanceof ElseIf) {
                    sourceBuilder.append(" else if(");
                    generateBooleanSource(sourceBuilder, els.condition, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    sourceBuilder.appendln(") {");
                } else {
                    hasElse = true;
                    sourceBuilder.appendln(" else {");
                }
                sourceBuilder.indent();
                hasReturn = transpileParsedSource(sourceBuilder, els.simpleimpl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, true) && hasReturn;
                if(!(els.simpleimpl instanceof Block))
                    sourceBuilder.appendln(";");
                else
                    sourceBuilder.appendln();
                sourceBuilder.unindent();
                sourceBuilder.append("}");
            } else {
                if (els instanceof ElseIf) {
                    sourceBuilder.append(" else if(");
                    generateBooleanSource(sourceBuilder, els.condition, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    sourceBuilder.appendln(") {");
                } else {
                    hasElse = true;
                    sourceBuilder.appendln(" else {");
                }
                sourceBuilder.indent();
                hasReturn = generateBlockSource(sourceBuilder, els.impl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context) && hasReturn;
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

    private void generateBooleanSource(SourceBuilder sourceBuilder, Parsed part, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, StackOptimizations expectedStack, Map<java.lang.String, Function> functionMap, Map<java.lang.String, Class> classMap, ClassNameScopeChain scopeChain, Map<java.lang.Integer, FilePosition> sourceMap, List<java.lang.Object> extras, boolean[] hasBreak, TranspileContext context) {
        while (part instanceof OpenBracket) {
            part = ((OpenBracket) part).contents;
        }

        if (part instanceof In) {
            transpileParsedSource(sourceBuilder, ((In)part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(".in(");
            generateStringSource(sourceBuilder, ((In)part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(")");
        } else if (part instanceof Boolean) {
            if (((Boolean) part).value) {
                sourceBuilder.append("true");
            } else {
                sourceBuilder.append("false");
            }
        } else if (part instanceof InstanceOf) {
            transpileParsedSource(sourceBuilder, ((InstanceOf) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(".instanceOf((BaseFunction)");
            transpileParsedSource(sourceBuilder, ((InstanceOf) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(")");
        } else if (part instanceof Not) {
            sourceBuilder.append("!");
            generateBooleanSource(sourceBuilder, ((Not) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
        } else if (part instanceof OrOr) {
            generateBooleanSource(sourceBuilder, ((OrOr) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(" || ");
            generateBooleanSource(sourceBuilder, ((OrOr) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
        } else if (part instanceof AndAnd) {
            generateBooleanSource(sourceBuilder, ((AndAnd) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(" && ");
            generateBooleanSource(sourceBuilder, ((AndAnd) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
        } else if (part instanceof Equals) {
            java.lang.String ltype = ((Equals) part).lhs.primaryType();
            java.lang.String rtype = ((Equals) part).rhs.primaryType();
            if (ltype.equals(rtype) && generateCommonComparison(sourceBuilder, ltype, false, ((Equals) part).lhs, ((Equals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context)) {
                return;
            }

            transpileParsedSource(sourceBuilder, ((Equals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(".equals(");
            transpileParsedSource(sourceBuilder, ((Equals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(")");
        } else if (part instanceof NotEquals) {
            java.lang.String ltype = ((NotEquals) part).lhs.primaryType();
            java.lang.String rtype = ((NotEquals) part).rhs.primaryType();
            if (ltype.equals(rtype) && generateCommonComparison(sourceBuilder, ltype, true, ((NotEquals) part).lhs, ((NotEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context)) {
                return;
            }

            sourceBuilder.append("!");
            transpileParsedSource(sourceBuilder, ((NotEquals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(".equals(");
            transpileParsedSource(sourceBuilder, ((NotEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(")");
        } else if (part instanceof StrictEquals) {
            java.lang.String ltype = ((StrictEquals) part).lhs.primaryType();
            java.lang.String rtype = ((StrictEquals) part).rhs.primaryType();
            if (ltype.equals(rtype) && generateCommonComparison(sourceBuilder, ltype, false, ((StrictEquals) part).lhs, ((StrictEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context)) {
                return;
            }

            transpileParsedSource(sourceBuilder, ((StrictEquals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(".strictEquals(");
            transpileParsedSource(sourceBuilder, ((StrictEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(")");
        } else if (part instanceof StrictNotEquals) {
            java.lang.String ltype = ((StrictNotEquals) part).lhs.primaryType();
            java.lang.String rtype = ((StrictNotEquals) part).rhs.primaryType();
            if (ltype.equals(rtype) && generateCommonComparison(sourceBuilder, ltype, true, ((StrictNotEquals) part).lhs, ((StrictNotEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context)) {
                return;
            }

            sourceBuilder.append("!");
            transpileParsedSource(sourceBuilder, ((StrictNotEquals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(".strictEquals(");
            transpileParsedSource(sourceBuilder, ((StrictNotEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(")");
        } else if (part instanceof MoreThan) {
            Parsed lhs = ((MoreThan) part).lhs;
            Parsed rhs = ((MoreThan) part).rhs;

            if (isNumber(lhs, localStack) || isNumber(rhs, localStack)) {
                generateNumberSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(" > ");
                generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                return;
            }

            sourceBuilder.append("lessThan(");
            transpileParsedSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(", ");
            transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(")");
        } else if (part instanceof LessThan) {
            Parsed lhs = ((LessThan) part).lhs;
            Parsed rhs = ((LessThan) part).rhs;

            if (isNumber(lhs, localStack) || isNumber(rhs, localStack)) {
                generateNumberSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(" < ");
                generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                return;
            }

            sourceBuilder.append("lessThan(");
            transpileParsedSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(", ");
            transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(")");
        } else if (part instanceof MoreEqual) {
            Parsed lhs = ((MoreEqual) part).lhs;
            Parsed rhs = ((MoreEqual) part).rhs;

            if (isNumber(lhs, localStack) || isNumber(rhs, localStack)) {
                generateNumberSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(" >= ");
                generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                return;
            }

            sourceBuilder.append("moreEqual(");
            transpileParsedSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(", ");
            transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(")");
        } else if (part instanceof LessEqual) {
            Parsed lhs = ((LessEqual) part).lhs;
            Parsed rhs = ((LessEqual) part).rhs;

            if (isNumber(lhs, localStack) || isNumber(rhs, localStack)) {
                generateNumberSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(" <= ");
                generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                return;
            }

            sourceBuilder.append("lessEqual(");
            transpileParsedSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(", ");
            transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
            } else if (rhs instanceof VariableReference) {
                sourceBuilder.append("Utilities.delete(");
                transpileParsedSource(sourceBuilder, ((VariableReference)rhs).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(", ");
                transpileParsedSource(sourceBuilder, ((VariableReference)rhs).ref, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(")");
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
            transpileParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
        //assert ((new JavaTranspiler().compile("(function munchkin(){\n    function yellow(){\n        return 55;\n    }\n    return yellow()\n    })()", "JavaCompilerStaticTest", false)).exec(new Global(), null).toString().equals("55"));
    }

    private boolean generateCommonComparison(SourceBuilder sourceBuilder, java.lang.String ltype, boolean not, Parsed lhs, Parsed rhs, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, StackOptimizations expectedStack, Map<java.lang.String, Function> functionMap, Map<java.lang.String, Class> classMap, ClassNameScopeChain scopeChain, Map<java.lang.Integer, FilePosition> sourceMap, List<java.lang.Object> extras, boolean[] hasBreak, TranspileContext context) {
        if (ltype.equals("string")) {
            if (not) {
                sourceBuilder.append("!");
            }
            generateStringSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(".equals(");
            generateStringSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(")");
            return true;
        }
        if (ltype.equals("number")) {
            generateNumberSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(not ? " != " : " == ");
            generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            return true;
        }
        if (ltype.equals("boolean")) {
            generateBooleanSource(sourceBuilder, lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(not ? " != " : " == ");
            generateBooleanSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
        boolean hasLet;

        BlockScopeOptimizer(ScopeOptimizer parent) {
            this.parent = parent;
        }

        @Override
        public void let(java.lang.String key, java.lang.String type) {
            hasLet = true;
            super.var(key, type);
            parent.markUseSyntheticStack();
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
                BlockScopeOptimizer blockScopeOptimizer = new BlockScopeOptimizer(variableScope);
                scanScriptSource(((If) parsed).impl, blockScopeOptimizer);
                ((If) parsed).impl.optimizations = new MapStackOptimizations(blockScopeOptimizer.scope, blockScopeOptimizer.stackType, blockScopeOptimizer.hasLet);
            }

            Else el = ((If) parsed).el;
            while (el != null) {
                if (el instanceof ElseIf) {
                    scanParsedSource(((ElseIf) el).condition, variableScope);
                }
                if (el.simpleimpl != null) {
                    scanParsedSource(el.simpleimpl, variableScope);
                } else {
                    BlockScopeOptimizer blockScopeOptimizer = new BlockScopeOptimizer(variableScope);
                    scanScriptSource(el.impl, blockScopeOptimizer);
                    el.impl.optimizations = new MapStackOptimizations(blockScopeOptimizer.scope, blockScopeOptimizer.stackType, blockScopeOptimizer.hasLet);
                }
                if (el instanceof ElseIf) {
                    el = ((ElseIf) el).el;
                } else {
                    break;
                }
            }
        } else if (parsed instanceof For) {
            BlockScopeOptimizer forScope = new BlockScopeOptimizer(variableScope);
            if (((For) parsed).type == For.ForType.Standard) {
                scanParsedSource(((For) parsed).init, forScope);
                for(Parsed part : ((For) parsed).loop)
                    scanParsedSource(part, forScope);
                scanParsedSource(((For) parsed).condition, forScope);
            } else
                scanParsedSource(((For)parsed).init, forScope);
            if (((For) parsed).simpleimpl != null) {
                scanParsedSource(((For) parsed).simpleimpl, forScope);
            } else {
                scanScriptSource(((For) parsed).impl, forScope);
                ((For) parsed).impl.optimizations = new MapStackOptimizations(forScope.scope, forScope.stackType, forScope.hasLet);
            }
        } else if (parsed instanceof While) {
            scanParsedSource(((While) parsed).condition, variableScope);
            if (((While) parsed).simpleimpl != null) {
                scanParsedSource(((While) parsed).simpleimpl, variableScope);
            } else {
                BlockScopeOptimizer blockScope = new BlockScopeOptimizer(variableScope);
                scanScriptSource(((While) parsed).impl, blockScope);
                ((While) parsed).impl.optimizations = new MapStackOptimizations(blockScope.scope, blockScope.stackType, blockScope.hasLet);
            }
        } else if (parsed instanceof Do) {
            scanParsedSource(((Do) parsed).condition, variableScope);
            BlockScopeOptimizer blockScope = new BlockScopeOptimizer(variableScope);
            scanScriptSource(((Do) parsed).impl, blockScope);
            ((Do) parsed).impl.optimizations = new MapStackOptimizations(blockScope.scope, blockScope.stackType, blockScope.hasLet);
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
        } else if (parsed instanceof Class) {
            for(ClassMethod classMethod : ((Class)parsed).methods)
                scanParsedSource(classMethod, variableScope);
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
                if (ex instanceof CannotOptimizeUnimplemented) {
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

    protected boolean generateBlockSource(SourceBuilder sourceBuilder, ScriptData blockDat, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, StackOptimizations expectedStack, Map<java.lang.String, Function> functionMap, Map<java.lang.String, Class> classMap, ClassNameScopeChain scopeChain, Map<java.lang.Integer, FilePosition> sourceMap, List<java.lang.Object> extras, boolean[] hasBreak, TranspileContext context) {
        if(blockDat == null)
            return false;
        
        boolean hasReturn = false;
        for (Parsed part : blockDat.impl) {
            if (addDebugging) {
                addSourceMapEntry(sourceBuilder, sourceMap, part);
            }
            hasReturn = transpileParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, true) || hasReturn;
            if(!(part instanceof Block))
                sourceBuilder.appendln(";");
            else
                sourceBuilder.appendln();
        }
        return hasReturn;
    }

    protected boolean transpileParsedSource(SourceBuilder sourceBuilder, Parsed part, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, StackOptimizations expectedStack, Map<java.lang.String, Function> functionMap, Map<java.lang.String, Class> classMap, ClassNameScopeChain scopeChain, Map<java.lang.Integer, FilePosition> sourceMap, List<java.lang.Object> extras, boolean[] hasBreak, TranspileContext context) {
        return transpileParsedSource(sourceBuilder, part, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, false);
    }

    protected boolean transpileParsedSource(SourceBuilder sourceBuilder, Parsed part, java.lang.String methodPrefix, java.lang.String baseScope, java.lang.String fileName, LocalStack localStack, StackOptimizations expectedStack, Map<java.lang.String, Function> functionMap, Map<java.lang.String, Class> classMap, ClassNameScopeChain scopeChain, Map<java.lang.Integer, FilePosition> sourceMap, List<java.lang.Object> extras, boolean[] hasBreak, TranspileContext context, boolean atTop) {
        while (part instanceof OpenBracket) {
            part = ((OpenBracket) part).contents;
        }

        if (part instanceof Return) {
            sourceBuilder.append("return ");
            if(((Return) part).rhs == null)
                sourceBuilder.append("Undefined.INSTANCE");
            else
                transpileParsedSource(sourceBuilder, ((Return) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            return true;
        } else if (part instanceof TypeOf) {
            Parsed rhs = ((TypeOf) part).rhs;
            sourceBuilder.append("global.wrap(");
            transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(".typeOf())");
            return false;
        } else if (part instanceof Call) {
            Parsed reference = ((Call) part).reference;
            while (reference instanceof OpenBracket) {// unwrap
                reference = ((OpenBracket) reference).contents;
            }

            SuperSourceBuilder superSourceBuilder = context == TranspileContext.ClassMethod ? new SuperSourceBuilder(atTop ? sourceBuilder : new SourceBuilder()) : null;
            if (reference instanceof BaseReferency && !(reference instanceof Reference || reference instanceof Call)) {
                final java.lang.String source = reference.toSimpleSource();
                if (reference instanceof RightReference) {
                    final java.lang.String key = ((RightReference) reference).chain.remove(((RightReference) reference).chain.size() - 1);
                    if (atTop) {
                        sourceBuilder.append("__this = ");
                        transpileParsedSource(superSourceBuilder == null ? sourceBuilder : superSourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
                            sourceBuilder.append("    throw new net.nexustools.njs.Error.JavaException(\"ReferenceError\", \"");
                            sourceBuilder.append(convertStringSource(source));
                            sourceBuilder.appendln(" is not a function\");");
                            sourceBuilder.appendln("}");
                        }
                        sourceBuilder.append("function.call(");
                        if(superSourceBuilder == null || !superSourceBuilder._super)
                            sourceBuilder.append("_");
                        sourceBuilder.append("_this");
                    } else {
                        sourceBuilder.append("callTop(");
                        if (addDebugging) {
                            sourceBuilder.append("\"");
                            sourceBuilder.append(convertStringSource(source));
                            sourceBuilder.append("\", ");
                        }
                        if(superSourceBuilder == null) {
                            sourceBuilder.append("\"");
                            sourceBuilder.append(convertStringSource(key));
                            sourceBuilder.append("\", ");
                            transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                        } else {
                            transpileParsedSource(superSourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                            if(superSourceBuilder._super) {
                                ((RightReference) reference).chain.add(key);
                                transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                                sourceBuilder.append(", _this");
                            } else {
                                sourceBuilder.append("\"");
                                sourceBuilder.append(convertStringSource(key));
                                sourceBuilder.append("\", ");
                                sourceBuilder.append(superSourceBuilder.toString());
                            }
                        }
                    }
                    for (Parsed arg : ((Call) part).arguments) {
                        sourceBuilder.append(", ");
                        transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    }
                    sourceBuilder.append(")");
                    return false;
                } else if (reference instanceof IntegerReference) {
                    final int key = (int)((IntegerReference) reference).ref;
                    if (atTop) {
                        sourceBuilder.append("__this = ");
                        transpileParsedSource(superSourceBuilder == null ? sourceBuilder : superSourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
                            sourceBuilder.append("    throw new net.nexustools.njs.Error.JavaException(\"ReferenceError\", \"");
                            sourceBuilder.append(convertStringSource(source));
                            sourceBuilder.appendln(" is not a function\");");
                            sourceBuilder.appendln("}");
                        }
                        sourceBuilder.append("function.call(");
                        if(superSourceBuilder == null || !superSourceBuilder._super)
                            sourceBuilder.append("_");
                        sourceBuilder.append("_this");
                    } else {
                        sourceBuilder.append("callTop(");
                        if (addDebugging) {
                            sourceBuilder.append("\"");
                            sourceBuilder.append(convertStringSource(source));
                            sourceBuilder.append("\", ");
                        }
                        if(superSourceBuilder == null) {
                            sourceBuilder.append(java.lang.String.valueOf(key));
                            sourceBuilder.append(", ");
                            transpileParsedSource(sourceBuilder, ((IntegerReference) reference).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                        } else {
                            transpileParsedSource(superSourceBuilder, ((IntegerReference) reference).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                            if(superSourceBuilder._super) {
                                transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                                sourceBuilder.append(", _this");
                            } else {
                                sourceBuilder.append(java.lang.String.valueOf(key));
                                sourceBuilder.append(", ");
                                sourceBuilder.append(superSourceBuilder.toString());
                            }
                        }
                    }
                    for (Parsed arg : ((Call) part).arguments) {
                        sourceBuilder.append(", ");
                        transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    }
                    sourceBuilder.append(")");
                    return false;
                } else if (reference instanceof ReferenceChain) {
                    final java.lang.String key = ((ReferenceChain) reference).chain.remove(((ReferenceChain) reference).chain.size() - 1);
                    if (atTop) {
                        sourceBuilder.append("__this = ");
                        transpileParsedSource(superSourceBuilder == null ? sourceBuilder : superSourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
                            sourceBuilder.append("    throw new net.nexustools.njs.Error.JavaException(\"ReferenceError\", \"");
                            sourceBuilder.append(convertStringSource(source));
                            sourceBuilder.appendln(" is not a function\");");
                            sourceBuilder.appendln("}");
                        }
                        sourceBuilder.append("function.call(");
                        if(superSourceBuilder == null || !superSourceBuilder._super)
                            sourceBuilder.append("_");
                        sourceBuilder.append("_this");
                    } else {
                        sourceBuilder.append("callTop(");
                        if (addDebugging) {
                            sourceBuilder.append("\"");
                            sourceBuilder.append(convertStringSource(source));
                            sourceBuilder.append("\", ");
                        }
                        if(superSourceBuilder == null) {
                            sourceBuilder.append("\"");
                            sourceBuilder.append(convertStringSource(key));
                            sourceBuilder.append("\", ");
                            transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                        } else {
                            transpileParsedSource(superSourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                            if(superSourceBuilder._super) {
                                ((ReferenceChain) reference).chain.add(key);
                                transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                                sourceBuilder.append(", _this");
                            } else {
                                sourceBuilder.append("\"");
                                sourceBuilder.append(convertStringSource(key));
                                sourceBuilder.append("\", ");
                                sourceBuilder.append(superSourceBuilder.toString());
                            }
                        }
                    }
                    for (Parsed arg : ((Call) part).arguments) {
                        sourceBuilder.append(", ");
                        transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
                    transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    for (Parsed arg : ((Call) part).arguments) {
                        sourceBuilder.append(", ");
                        transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    }
                    sourceBuilder.append(")");
                    return false;
                } else if(reference instanceof VariableReference) {
                    if (atTop) {
                        sourceBuilder.append("__this = ");
                        transpileParsedSource(superSourceBuilder == null ? sourceBuilder : superSourceBuilder, ((VariableReference)reference).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                        sourceBuilder.appendln(";");
                        if (addDebugging) {
                            sourceBuilder.appendln("try {");
                            sourceBuilder.indent();
                        }
                        sourceBuilder.append("function = (BaseFunction)Utilities.get(__this, ");
                        transpileParsedSource(sourceBuilder, ((VariableReference)reference).ref, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                        sourceBuilder.appendln(");");
                        if (addDebugging) {
                            sourceBuilder.unindent();
                            sourceBuilder.appendln("} catch(ClassCastException ex) {");
                            sourceBuilder.append("    throw new net.nexustools.njs.Error.JavaException(\"ReferenceError\", \"");
                            sourceBuilder.append(convertStringSource(source));
                            sourceBuilder.appendln(" is not a function\");");
                            sourceBuilder.appendln("}");
                        }
                        sourceBuilder.append("function.call(");
                        if(superSourceBuilder == null || !superSourceBuilder._super)
                            sourceBuilder.append("_");
                        sourceBuilder.append("_this");
                    } else {
                        sourceBuilder.append("callTopDynamic(");
                        if (addDebugging) {
                            sourceBuilder.append("\"");
                            sourceBuilder.append(convertStringSource(source));
                            sourceBuilder.append("\", ");
                        }
                        if(superSourceBuilder == null) {
                            transpileParsedSource(sourceBuilder, ((VariableReference)reference).ref, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                            sourceBuilder.append(", ");
                            transpileParsedSource(sourceBuilder, ((VariableReference)reference).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                        } else {
                            transpileParsedSource(superSourceBuilder, ((VariableReference) reference).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                            if(superSourceBuilder._super) {
                                transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                                sourceBuilder.append(", _this");
                            } else {
                                transpileParsedSource(sourceBuilder, ((VariableReference)reference).ref, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                                sourceBuilder.append(", ");
                                sourceBuilder.append(superSourceBuilder.toString());
                            }
                        }
                    }
                    for (Parsed arg : ((Call) part).arguments) {
                        sourceBuilder.append(", ");
                        transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    }
                    sourceBuilder.append(")");
                    return false;
                } else if(reference instanceof Function || reference instanceof Class) {
                    transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    sourceBuilder.append(".call(_this");
                    for (Parsed arg : ((Call) part).arguments) {
                        sourceBuilder.append(", ");
                        transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    }
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
                    transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    sourceBuilder.appendln(";");
                    sourceBuilder.unindent();
                    sourceBuilder.appendln("} catch(ClassCastException ex) {");
                    sourceBuilder.append("    throw new net.nexustools.njs.Error.JavaException(\"ReferenceError\", \"");
                    sourceBuilder.append(source);
                    sourceBuilder.appendln(" is not a function\");");
                    sourceBuilder.appendln("}");
                    sourceBuilder.append("function.call(_this");
                } else {
                    sourceBuilder.append("callTop(");
                    sourceBuilder.append("\"");
                    sourceBuilder.append(source);
                    sourceBuilder.append("\", ");
                    transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    sourceBuilder.append(", ");
                    sourceBuilder.append("_this");
                }
                for (Parsed arg : ((Call) part).arguments) {
                    sourceBuilder.append(", ");
                    transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                }
                sourceBuilder.append(")");
            } else {
                sourceBuilder.append("((BaseFunction)");
                transpileParsedSource(sourceBuilder, reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(").call(");
                sourceBuilder.append("_this");
                for (Parsed arg : ((Call) part).arguments) {
                    sourceBuilder.append(", ");
                    transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                }
                sourceBuilder.append(")");
            }
            return false;
        } else if (part instanceof InstanceOf) {
            sourceBuilder.append("(");
            transpileParsedSource(sourceBuilder, ((InstanceOf) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(".instanceOf((BaseFunction)");
            transpileParsedSource(sourceBuilder, ((InstanceOf) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
                    sourceBuilder.append("D)");
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
                sourceBuilder.append("D)");
            }
            return false;
        } else if (part instanceof String) {
            sourceBuilder.append("global.wrap(\"");
            sourceBuilder.append(convertStringSource(java.lang.String.valueOf(((String) part).string)));
            sourceBuilder.append("\")");
            return false;
        } else if (part instanceof Reference) {
            if(context == TranspileContext.ClassMethod && ((Reference)part).ref.equals("super"))
                sourceBuilder.append("_super");
            else
                generateBaseScopeAccess(sourceBuilder, ((Reference) part).ref, baseScope, expectedStack, localStack);
            return false;
        } else if (part instanceof DoubleShiftRight) {
            Parsed lhs = ((DoubleShiftRight) part).lhs;
            Parsed rhs = ((DoubleShiftRight) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, ">>>", methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, atTop ? MathWrap.Void : MathWrap.BaseObject);
            return false;
        } else if (part instanceof ShiftLeft) {
            Parsed lhs = ((ShiftLeft) part).lhs;
            Parsed rhs = ((ShiftLeft) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, "<<", methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, atTop ? MathWrap.Void : MathWrap.BaseObject);
            return false;
        } else if (part instanceof ShiftRight) {
            Parsed lhs = ((ShiftRight) part).lhs;
            Parsed rhs = ((ShiftRight) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, ">>", methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, atTop ? MathWrap.Void : MathWrap.BaseObject);
            return false;
        } else if (part instanceof Plus) {
            Parsed lhs = ((Plus) part).lhs;
            Parsed rhs = ((Plus) part).rhs;
            if(lhs == null) {
                sourceBuilder.append("global.Number.fromValueOf(");
                transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(")");
            } else
                generateMath(sourceBuilder, lhs, rhs, '+', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, atTop ? MathWrap.Void : MathWrap.BaseObject);
            return false;
        } else if (part instanceof Multiply) {
            Parsed lhs = ((Multiply) part).lhs;
            Parsed rhs = ((Multiply) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '*', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, atTop ? MathWrap.Void : MathWrap.BaseObject);
            return false;
        } else if (part instanceof Divide) {
            Parsed lhs = ((Divide) part).lhs;
            Parsed rhs = ((Divide) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '/', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, atTop ? MathWrap.Void : MathWrap.BaseObject);
            return false;
        } else if (part instanceof And) {
            Parsed lhs = ((And) part).lhs;
            Parsed rhs = ((And) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '&', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, atTop ? MathWrap.Void : MathWrap.BaseObject);
            return false;
        } else if (part instanceof Or) {
            Parsed lhs = ((Or) part).lhs;
            Parsed rhs = ((Or) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '|', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, atTop ? MathWrap.Void : MathWrap.BaseObject);
            return false;
        } else if (part instanceof Percent) {
            Parsed lhs = ((Percent) part).lhs;
            Parsed rhs = ((Percent) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '%', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, atTop ? MathWrap.Void : MathWrap.BaseObject);
            return false;
        } else if (part instanceof Minus) {
            Parsed lhs = ((Minus) part).lhs;
            Parsed rhs = ((Minus) part).rhs;
            generateMath(sourceBuilder, lhs, rhs, '-', methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, atTop ? MathWrap.Void : MathWrap.BaseObject);
            return false;
        } else if (part instanceof New) {
            boolean addComma;
            if (addDebugging) {
                addComma = true;
                sourceBuilder.append("constructTop(\"");
                sourceBuilder.append(convertStringSource(((New) part).reference.toSimpleSource()));
                sourceBuilder.append("\", ");
                transpileParsedSource(sourceBuilder, ((New) part).reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            } else {
                addComma = false;
                sourceBuilder.append("((BaseFunction)");
                transpileParsedSource(sourceBuilder, ((New) part).reference, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(").construct(");
            }
            if (((New) part).arguments != null) {
                for (Parsed arg : ((New) part).arguments) {
                    if (addComma) {
                        sourceBuilder.append(", ");
                    } else {
                        addComma = true;
                    }
                    transpileParsedSource(sourceBuilder, arg, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                }
            }
            sourceBuilder.append(")");
            return false;
        } else if (part instanceof RightReference) {
            transpileParsedSource(sourceBuilder, ((RightReference) part).ref, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            for (java.lang.String key : ((RightReference) part).chain) {
                sourceBuilder.append(".get(");
                generateStringNumberIndex(sourceBuilder, key);
                sourceBuilder.append(")");
            }
            return false;
        } else if (part instanceof Throw) {
            sourceBuilder.append("throw new net.nexustools.njs.Error.Thrown(");
            transpileParsedSource(sourceBuilder, ((Throw) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(")");
            return true;
        } else if (part instanceof Function) {
            java.lang.String name = ((Function) part).name;
            if (name == null || name.isEmpty()) {
                name = "<anonymous>";
            }

            name = scopeChain.refName(name);
            if (functionMap.containsKey(name)) {
                int index = 2;
                java.lang.String base = name + "_";
                do {
                    name = base + index++;
                } while (functionMap.containsKey(name));
                name = scopeChain.refName(name);
            }
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
        } else if(part instanceof Class) {
            java.lang.String name = ((Class) part).name;
            name = scopeChain.refName(name);
            if (functionMap.containsKey(name)) {
                int index = 2;
                java.lang.String base = name + "_";
                do {
                    name = base + index++;
                } while (classMap.containsKey(name));
            }
            classMap.put(name, (Class)part);

            boolean _extends = ((Class)part)._extends != null;
            sourceBuilder.append(baseScope);
            sourceBuilder.append(".let(\"");
            sourceBuilder.append(convertStringSource(((Class)part).name));
            sourceBuilder.append("\", new ");
            sourceBuilder.append(name);
            sourceBuilder.append("(");
            if(_extends) {
                sourceBuilder.append("lookupSuper(\"");
                sourceBuilder.append(convertStringSource(((Class)part)._extends));
                sourceBuilder.append("\", ");
                sourceBuilder.append(baseScope);
                sourceBuilder.append("), ");
            }
            sourceBuilder.append("global, ");
            sourceBuilder.append(baseScope);
            sourceBuilder.append("))");
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
                                generateStringSource(sourceBuilder, set.rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                            }
                        } else if (type.equals("boolean")) {
                            if (set.rhs == null) {
                                sourceBuilder.append("false");
                            } else {
                                generateBooleanSource(sourceBuilder, set.rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                            }
                        } else if (type.equals("number")) {
                            if (set.rhs == null) {
                                sourceBuilder.append("0");
                            } else {
                                generateNumberSource(sourceBuilder, set.rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                            }
                        } else if (set.rhs == null) {
                            sourceBuilder.append("Undefined.INSTANCE");
                        } else {
                            transpileParsedSource(sourceBuilder, set.rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                        }
                    } else {
                        sourceBuilder.append(baseScope);
                        sourceBuilder.append("." + _type + "(\"");
                        sourceBuilder.append(convertStringSource(ref));
                        sourceBuilder.append("\", ");
                        if (set.rhs == null) {
                            sourceBuilder.append("Undefined.INSTANCE");
                        } else {
                            transpileParsedSource(sourceBuilder, set.rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                        }
                        sourceBuilder.append(")");
                    }
                } else {
                    sourceBuilder.append(baseScope);
                    sourceBuilder.append(".multi" + _type + "(");
                    transpileParsedSource(sourceBuilder, set.rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
            transpileParsedSource(sourceBuilder, ((OrOr) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(", ");
            transpileParsedSource(sourceBuilder, ((OrOr) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(")");
            return false;
        } else if (part instanceof AndAnd) {
            sourceBuilder.append("andAnd(");
            transpileParsedSource(sourceBuilder, ((AndAnd) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(", ");
            transpileParsedSource(sourceBuilder, ((AndAnd) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(")");
            return false;
        } else if (part instanceof Equals) {
            sourceBuilder.append("(");
            java.lang.String ltype = ((Equals) part).lhs.primaryType();
            java.lang.String rtype = ((Equals) part).rhs.primaryType();
            if (ltype.equals(rtype)) {
                if (generateCommonComparison(sourceBuilder, ltype, false, ((Equals) part).lhs, ((Equals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context)) {
                    sourceBuilder.append(" ? global.Boolean.TRUE : global.Boolean.FALSE)");
                    return false;
                }
            }

            transpileParsedSource(sourceBuilder, ((Equals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(".equals(");
            transpileParsedSource(sourceBuilder, ((Equals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
            return false;
        } else if (part instanceof NotEquals) {
            sourceBuilder.append("(");
            java.lang.String ltype = ((NotEquals) part).lhs.primaryType();
            java.lang.String rtype = ((NotEquals) part).rhs.primaryType();
            if (ltype.equals(rtype)) {
                if (generateCommonComparison(sourceBuilder, ltype, true, ((NotEquals) part).lhs, ((NotEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context)) {
                    sourceBuilder.append(" ? global.Boolean.TRUE : global.Boolean.FALSE)");
                    return false;
                }
            }

            transpileParsedSource(sourceBuilder, ((NotEquals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(".equals(");
            transpileParsedSource(sourceBuilder, ((NotEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(") ? global.Boolean.FALSE : global.Boolean.TRUE)");
            return false;
        } else if (part instanceof StrictEquals) {
            sourceBuilder.append("(");
            java.lang.String ltype = ((StrictEquals) part).lhs.primaryType();
            java.lang.String rtype = ((StrictEquals) part).rhs.primaryType();
            if (ltype.equals(rtype)) {
                if (generateCommonComparison(sourceBuilder, ltype, false, ((StrictEquals) part).lhs, ((StrictEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context)) {
                    sourceBuilder.append(" ? global.Boolean.TRUE : global.Boolean.FALSE)");
                    return false;
                }
            }

            transpileParsedSource(sourceBuilder, ((StrictEquals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(".strictEquals(");
            transpileParsedSource(sourceBuilder, ((StrictEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
            return false;
        } else if (part instanceof StrictNotEquals) {
            sourceBuilder.append("(");
            java.lang.String ltype = ((StrictNotEquals) part).lhs.primaryType();
            java.lang.String rtype = ((StrictNotEquals) part).rhs.primaryType();
            if (ltype.equals(rtype)) {
                if (generateCommonComparison(sourceBuilder, ltype, true, ((StrictNotEquals) part).lhs, ((StrictNotEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context)) {
                    sourceBuilder.append(" ? global.Boolean.TRUE : global.Boolean.FALSE)");
                    return false;
                }
            }

            transpileParsedSource(sourceBuilder, ((StrictNotEquals) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(".strictEquals(");
            transpileParsedSource(sourceBuilder, ((StrictNotEquals) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(") ? global.Boolean.FALSE : global.Boolean.TRUE)");
            return false;
        } else if (part instanceof ReferenceChain) {
            java.lang.String base = ((ReferenceChain) part).chain.remove(0);
            if(context == TranspileContext.ClassMethod && base.equals("super")) {
                sourceBuilder.append("_super");
                if(!((ReferenceChain)part).chain.isEmpty())
                    sourceBuilder.append(".prototype()");
            } else
                generateBaseScopeAccess(sourceBuilder, base, baseScope, expectedStack, localStack);
            for (java.lang.String ref : ((ReferenceChain) part).chain) {
                sourceBuilder.append(".get(");
                generateStringNumberIndex(sourceBuilder, ref);
                sourceBuilder.append(")");
            }
            ((ReferenceChain) part).chain.add(base);
            return false;
        } else if (part instanceof IntegerReference) {
            int key = (int)((IntegerReference) part).ref;
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

            transpileParsedSource(sourceBuilder, ((IntegerReference) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(".get(");
            sourceBuilder.append(java.lang.String.valueOf(key));
            sourceBuilder.append(")");
            return false;
        } else if (part instanceof Not) {
            sourceBuilder.append("(");
            generateBooleanSource(sourceBuilder, ((Not) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
                    transpileParsedSource(sourceBuilder, subpart, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
                    transpileParsedSource(sourceBuilder, ((IntegerReference) lhs).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    sourceBuilder.append(".set(");
                    sourceBuilder.append(java.lang.String.valueOf(((IntegerReference) lhs).ref));
                    sourceBuilder.append(", ");
                    transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    sourceBuilder.append(")");
                    return false;
                }

                sourceBuilder.append("callSet(");
                transpileParsedSource(sourceBuilder, ((IntegerReference) lhs).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(", ");
                sourceBuilder.append(java.lang.String.valueOf(((IntegerReference) lhs).ref));
                sourceBuilder.append(", ");
                transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(")");
                return false;
            } else if (lhs instanceof VariableReference) {
                if (isNumber(((VariableReference) lhs).ref, localStack)) {
                    sourceBuilder.append("Utilities.set(");
                    transpileParsedSource(sourceBuilder, ((VariableReference) lhs).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    sourceBuilder.append(", ");
                    generateNumberSource(sourceBuilder, ((VariableReference) lhs).ref, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    sourceBuilder.append(", ");
                    transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    sourceBuilder.append(")");
                } else {
                    sourceBuilder.append("Utilities.set(");
                    transpileParsedSource(sourceBuilder, ((VariableReference) lhs).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    sourceBuilder.append(", ");
                    transpileParsedSource(sourceBuilder, ((VariableReference) lhs).ref, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    sourceBuilder.append(", ");
                    transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
                            generateStringSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                        } else if (type.equals("number")) {
                            generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                        } else if (type.equals("boolean")) {
                            generateBooleanSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                        } else {
                            transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                        }
                        return false;
                    }
                }

                if (atTop) {
                    sourceBuilder.append(baseScope);
                    sourceBuilder.append(".set(\"");
                    sourceBuilder.append(convertStringSource(((Reference) lhs).ref));
                    sourceBuilder.append("\", ");
                    transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    sourceBuilder.append(")");
                    return false;
                }

                sourceBuilder.append("callSet(");
                sourceBuilder.append(baseScope);
                sourceBuilder.append(", \"");
                sourceBuilder.append(convertStringSource(((Reference) lhs).ref));
                sourceBuilder.append("\", ");
                transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
                        transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
                    transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    sourceBuilder.append(")");
                    return false;
                }
            } else if(lhs instanceof Set) {
                transpileParsedSource(sourceBuilder, new Set(((Set)lhs).lhs, new Set(((Set)lhs).rhs, rhs)), methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
                hasReturn = generateBlockSource(sourceBuilder, ((Try) part).impl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.unindent();
                sourceBuilder.appendln("} catch(net.nexustools.njs.Error.InvisibleException ex) {");
                sourceBuilder.appendln("    throw ex;");
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
                hasReturn = generateBlockSource(sourceBuilder, c.impl, methodPrefix, newScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context) && hasReturn;
                sourceBuilder.unindent();
                sourceBuilder.appendln("} finally {");
                sourceBuilder.append("    ");
                sourceBuilder.append(newScope);
                sourceBuilder.appendln(".exit();");
                sourceBuilder.appendln("}");
                sourceBuilder.unindent();
                sourceBuilder.appendln("} finally {");
                sourceBuilder.indent();
                hasReturn = generateBlockSource(sourceBuilder, f.impl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context) || hasReturn;
                sourceBuilder.unindent();
                sourceBuilder.appendln("}");
                return hasReturn;
            } else if (c != null) {
                sourceBuilder.appendln("try {");
                sourceBuilder.indent();
                hasReturn = generateBlockSource(sourceBuilder, ((Try) part).impl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.unindent();
                sourceBuilder.appendln("} catch(net.nexustools.njs.Error.InvisibleException ex) {");
                sourceBuilder.appendln("    throw ex;");
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
                hasReturn = generateBlockSource(sourceBuilder, c.impl, methodPrefix, newScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context) && hasReturn;
                sourceBuilder.unindent();
                sourceBuilder.appendln("} finally {");
                sourceBuilder.append("    ");
                sourceBuilder.append(newScope);
                sourceBuilder.appendln(".exit();");
                sourceBuilder.appendln("}");
                sourceBuilder.unindent();
                sourceBuilder.appendln("}");
                return hasReturn;
            }

            sourceBuilder.appendln("try {");
            sourceBuilder.indent();
            hasReturn = generateBlockSource(sourceBuilder, ((Try) part).impl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.unindent();
            sourceBuilder.appendln("} finally {");
            sourceBuilder.indent();
            hasReturn = generateBlockSource(sourceBuilder, f.impl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context) || hasReturn;
            sourceBuilder.unindent();
            sourceBuilder.appendln("}");
            return hasReturn;
        } else if (part instanceof If) {
            boolean hasReturn;
            if (((If) part).simpleimpl != null) {
                sourceBuilder.append("if(");
                generateBooleanSource(sourceBuilder, ((If) part).condition, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.appendln(") {");
                sourceBuilder.indent();
                hasReturn = transpileParsedSource(sourceBuilder, ((If) part).simpleimpl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, true);
                if(!(((If) part).simpleimpl instanceof Block))
                    sourceBuilder.appendln(";");
                else
                    sourceBuilder.appendln();
                sourceBuilder.unindent();
                sourceBuilder.append("}");

                return generateIfBlockSource(sourceBuilder, ((If) part).el, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context) && hasReturn;
            }

            sourceBuilder.append("if(");
            generateBooleanSource(sourceBuilder, ((If) part).condition, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.appendln(") {");
            sourceBuilder.indent();
            hasReturn = generateBlockSource(sourceBuilder, ((If) part).impl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.unindent();
            sourceBuilder.append("}");

            return generateIfBlockSource(sourceBuilder, ((If) part).el, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context) && hasReturn;
        } else if (part instanceof While) {
            boolean hasReturn = ((While) part).condition.isTrue();
            if (((While) part).simpleimpl != null) {
                sourceBuilder.append("while(");
                generateBooleanSource(sourceBuilder, ((While) part).condition, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.appendln(") {");
                sourceBuilder.indent();
                transpileParsedSource(sourceBuilder, ((While) part).simpleimpl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, true);
                if(!(((While) part).simpleimpl instanceof Block))
                    sourceBuilder.appendln(";");
                else
                    sourceBuilder.appendln();
                sourceBuilder.append("}");
                return hasReturn;
            }

            sourceBuilder.append("while(");
            generateBooleanSource(sourceBuilder, ((While) part).condition, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.appendln(") {");
            sourceBuilder.indent();
            hasReturn = !generateBlockSource(sourceBuilder, ((While) part).impl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context) && hasReturn;
            sourceBuilder.unindent();
            sourceBuilder.append("}");
            return hasReturn;
        } else if (part instanceof Do) {
            boolean hasReturn = ((Do) part).condition.isTrue();

            sourceBuilder.append("do {");
            sourceBuilder.indent();
            hasReturn = !generateBlockSource(sourceBuilder, ((Do) part).impl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context) && hasReturn;
            sourceBuilder.unindent();
            sourceBuilder.append("} while(");
            generateBooleanSource(sourceBuilder, ((Do) part).condition, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(");");
            return hasReturn;
        } else if (part instanceof For) {
            boolean hasReturn = ((For)part).condition == null || ((For)part).condition.isTrue();
            if (((For) part).simpleimpl != null) {
                switch (((For) part).type) {
                    case InLoop:
                    {
                        sourceBuilder.appendln("{");
                        sourceBuilder.indent();
                        final Var var = (Var)((For) part).init;
                        sourceBuilder.append("Iterator<java.lang.String> it = ");
                        transpileParsedSource(sourceBuilder, var.sets.get(0).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                        sourceBuilder.appendln(".deepPropertyNameIterator();");
                        sourceBuilder.appendln("while(it.hasNext()) {");
                        sourceBuilder.indent();
                        sourceBuilder.append(baseScope);
                        sourceBuilder.append(".var(\"");
                        sourceBuilder.append(((Reference)var.sets.get(0).lhs).ref);
                        sourceBuilder.appendln("\", global.String.wrap(it.next()));");
                        hasReturn = transpileParsedSource(sourceBuilder, ((For) part).simpleimpl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context) && hasReturn;
                        if(!(((For) part).simpleimpl instanceof Block))
                            sourceBuilder.appendln(";");
                        else
                            sourceBuilder.appendln();
                        sourceBuilder.unindent();
                        sourceBuilder.appendln("}");
                        sourceBuilder.unindent();
                        sourceBuilder.append("}");
                        break;
                    }

                    case OfLoop:
                    {
                        final Var set = (Var)((For) part).init;
                        sourceBuilder.append("for(BaseObject forObject : ");
                        transpileParsedSource(sourceBuilder, set.sets.get(0).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                        sourceBuilder.appendln(") {");
                        sourceBuilder.indent();
                        sourceBuilder.append(baseScope);
                        sourceBuilder.append(".var(\"");
                        sourceBuilder.append(((Reference) set.sets.get(0).lhs).ref);
                        sourceBuilder.appendln("\", forObject);");
                        hasReturn = transpileParsedSource(sourceBuilder, ((For) part).simpleimpl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context) && hasReturn;
                        if(!(((For) part).simpleimpl instanceof Block))
                            sourceBuilder.appendln(";");
                        else
                            sourceBuilder.appendln();
                        sourceBuilder.unindent();
                        sourceBuilder.append("}");
                        break;
                    }

                    case Standard:
                        if(((For)part).init != null) {
                            transpileParsedSource(sourceBuilder, ((For)part).init, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, true);
                            if(!(((For)part).init instanceof Block))
                                sourceBuilder.appendln(";");
                            else
                                sourceBuilder.appendln();
                        }
                        sourceBuilder.append("for(; ");
                        generateBooleanSource(sourceBuilder, ((For) part).condition, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                        sourceBuilder.append("; ");
                        boolean first = true;
                        for(Parsed _part : ((For)part).loop) {
                            if(first)
                                first = false;
                            else
                                sourceBuilder.appendln(", ");
                            transpileParsedSource(sourceBuilder, _part, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                        }
                        sourceBuilder.appendln(") {");
                        sourceBuilder.indent();
                        hasReturn = transpileParsedSource(sourceBuilder, ((For) part).simpleimpl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, true) && hasReturn;
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
                    final Var var = (Var)((For) part).init;
                    boolean let = var instanceof Let;
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
                    transpileParsedSource(sourceBuilder, var.sets.get(0).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    sourceBuilder.appendln(".deepPropertyNameIterator();");
                    sourceBuilder.appendln("while(it.hasNext()) {");
                    sourceBuilder.indent();
                    sourceBuilder.append(scope);
                    if (let) {
                        sourceBuilder.append(".let(\"");
                    } else {
                        sourceBuilder.append(".var(\"");
                    }
                    sourceBuilder.append(((Reference)var.sets.get(0).lhs).ref);
                    sourceBuilder.appendln("\", global.String.wrap(it.next()));");
                    hasReturn = !generateBlockSource(sourceBuilder, ((For) part).impl, methodPrefix, scope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context) && hasReturn;
                    sourceBuilder.unindent();
                    sourceBuilder.appendln("}");
                    sourceBuilder.unindent();
                    sourceBuilder.append("}");
                    break;
                }

                case OfLoop: {
                    java.lang.String scope;
                    final Var var = (Var)((For) part).init;
                    boolean let = var instanceof Let;
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
                    transpileParsedSource(sourceBuilder, var.sets.get(0).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    sourceBuilder.appendln(") {");
                    sourceBuilder.indent();
                    Parsed lhs = var.sets.get(0).lhs;
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
                    hasReturn = !generateBlockSource(sourceBuilder, ((For) part).impl, methodPrefix, scope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context) && hasReturn;
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
                    if(((For)part).init != null) {
                        transpileParsedSource(sourceBuilder, ((For)part).init, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context, true);
                        if(!(((For)part).init instanceof Block))
                            sourceBuilder.appendln(";");
                        else
                            sourceBuilder.appendln();
                    }
                    sourceBuilder.append("for(; ");
                    if(((For) part).condition != null)
                        generateBooleanSource(sourceBuilder, ((For) part).condition, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    sourceBuilder.append("; ");
                    boolean first = true;
                    for(Parsed _part : ((For)part).loop) {
                        if(first)
                            first = false;
                        else
                            sourceBuilder.appendln(", ");
                        transpileParsedSource(sourceBuilder, _part, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                    }
                    sourceBuilder.appendln(") {");
                    sourceBuilder.indent();
                    hasReturn = !generateBlockSource(sourceBuilder, ((For) part).impl, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context) && hasReturn;
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
                            throw new CannotOptimizeUnimplemented("Cannot compile optimized ++: " + describe(ref));
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
            } else if(ref instanceof ReferenceChain) {
                java.lang.String first = ((ReferenceChain) ref).chain.remove(0);
                java.lang.String last = ((ReferenceChain) ref).chain.remove(((ReferenceChain) ref).chain.size() - 1);

                sourceBuilder.append("\"");
                sourceBuilder.append(convertStringSource(last));
                sourceBuilder.append("\", ");
                generateBaseScopeAccess(sourceBuilder, first, baseScope, expectedStack, localStack);
                for (java.lang.String key : ((ReferenceChain) ref).chain) {
                    sourceBuilder.append(".get(\"");
                    sourceBuilder.append(convertStringSource(key));
                    sourceBuilder.append("\")");
                }
            } else if(ref instanceof VariableReference) {
                transpileParsedSource(sourceBuilder, ((VariableReference)ref).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(", ");
                transpileParsedSource(sourceBuilder, ((VariableReference)ref).ref, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
                    generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
                transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(", ");
                sourceBuilder.append(baseScope);
            } else if(ref instanceof ReferenceChain) {
                java.lang.String first = ((ReferenceChain) ref).chain.remove(0);
                java.lang.String last = ((ReferenceChain) ref).chain.remove(((ReferenceChain) ref).chain.size() - 1);

                sourceBuilder.append("\"");
                sourceBuilder.append(convertStringSource(last));
                sourceBuilder.append("\", ");
                transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(", ");
                generateBaseScopeAccess(sourceBuilder, first, baseScope, expectedStack, localStack);
                for (java.lang.String key : ((ReferenceChain) ref).chain) {
                    sourceBuilder.append(".get(\"");
                    sourceBuilder.append(convertStringSource(key));
                    sourceBuilder.append("\")");
                }
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
                    generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
                generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
                    generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
                generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
                generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
                generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
                generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(", ");
                sourceBuilder.append(baseScope);
            } else if(ref instanceof ReferenceChain) {
                java.lang.String first = ((ReferenceChain) ref).chain.remove(0);
                java.lang.String last = ((ReferenceChain) ref).chain.remove(((ReferenceChain) ref).chain.size() - 1);

                sourceBuilder.append("\"");
                sourceBuilder.append(convertStringSource(last));
                sourceBuilder.append("\", ");
                transpileParsedSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(", ");
                generateBaseScopeAccess(sourceBuilder, first, baseScope, expectedStack, localStack);
                for (java.lang.String key : ((ReferenceChain) ref).chain) {
                    sourceBuilder.append(".get(\"");
                    sourceBuilder.append(convertStringSource(key));
                    sourceBuilder.append("\")");
                }
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
                generateNumberSource(sourceBuilder, rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
                } else if(ref instanceof ReferenceChain) {
                    java.lang.String first = ((ReferenceChain) ref).chain.remove(0);
                    java.lang.String last = ((ReferenceChain) ref).chain.remove(((ReferenceChain) ref).chain.size() - 1);

                    sourceBuilder.append("\"");
                    sourceBuilder.append(convertStringSource(last));
                    sourceBuilder.append("\", ");
                    generateBaseScopeAccess(sourceBuilder, first, baseScope, expectedStack, localStack);
                    for (java.lang.String key : ((ReferenceChain) ref).chain) {
                        sourceBuilder.append(".get(\"");
                        sourceBuilder.append(convertStringSource(key));
                        sourceBuilder.append("\")");
                    }
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
                    transpileParsedSource(sourceBuilder, entry.getValue(), methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
                generateBooleanSource(sourceBuilder, part, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
            } else if(rhs instanceof VariableReference) {
                sourceBuilder.append("(Utilities.delete(");
                transpileParsedSource(sourceBuilder, ((VariableReference) rhs).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(", ");
                transpileParsedSource(sourceBuilder, ((VariableReference) rhs).ref, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
                return false;
            }

            throw new UnsupportedOperationException("Cannot compile delete : " + describe(rhs));
        } else if (part instanceof MoreThan) {
            sourceBuilder.append("(moreThan(");
            transpileParsedSource(sourceBuilder, ((MoreThan) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(", ");
            transpileParsedSource(sourceBuilder, ((MoreThan) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
            return false;
        } else if (part instanceof LessThan) {
            sourceBuilder.append("(lessThan(");
            transpileParsedSource(sourceBuilder, ((LessThan) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(", ");
            transpileParsedSource(sourceBuilder, ((LessThan) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
            return false;
        } else if (part instanceof MoreEqual) {
            sourceBuilder.append("(moreEqual(");
            transpileParsedSource(sourceBuilder, ((MoreEqual) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(", ");
            transpileParsedSource(sourceBuilder, ((MoreEqual) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
            return false;
        } else if (part instanceof LessEqual) {
            sourceBuilder.append("(lessEqual(");
            transpileParsedSource(sourceBuilder, ((LessEqual) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(", ");
            transpileParsedSource(sourceBuilder, ((LessEqual) part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
                                transpileParsedSource(sourceBuilder, ((VariableReference) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                                sourceBuilder.append(", ");
                                generateNumberSource(sourceBuilder, ((VariableReference) part).ref, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                                sourceBuilder.append(")");
                                return false;
                            } else if (type.equals("string")) {
                                transpileParsedSource(sourceBuilder, ((VariableReference) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                                sourceBuilder.append(".get(");
                                sourceBuilder.append(expectedStack.getReference(((Reference) ref).ref));
                                sourceBuilder.append(")");
                                return false;
                            }
                        }
                    } else {
                        throw new CannotOptimizeUnimplemented("Cannot compile optimized: " + describe(ref));
                    }
                }
            }
            sourceBuilder.append("Utilities.get(");
            transpileParsedSource(sourceBuilder, ((VariableReference) part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(", ");
            transpileParsedSource(sourceBuilder, ((VariableReference) part).ref, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(")");
            return false;
        } else if (part instanceof Fork) {
            sourceBuilder.append("(");
            generateBooleanSource(sourceBuilder, ((Fork) part).condition, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(" ? (");
            transpileParsedSource(sourceBuilder, ((Fork) part).success, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(") : (");
            transpileParsedSource(sourceBuilder, ((Fork) part).failure, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
            SwitchType type = null;
            for(Parsed parsed : ((Switch) part).impl.impl) {
                if(parsed instanceof Case) {
                    Parsed ref = ((Case) parsed).ref;
                    if(ref instanceof Undefined)
                        type = SwitchType.Enum;
                    else if(ref instanceof String)
                        type = SwitchType.Enum;
                    else if(ref instanceof Long) {
                        if(type == null)
                            type = ((Long) ref).value >= Integer.MIN_VALUE && ((Long) ref).value <= Integer.MAX_VALUE ? SwitchType.Integer : SwitchType.Enum;
                    }
                }
            }
            
            SwitchEnum switchEnum;
            sourceBuilder.append("switch(");
            if(type == SwitchType.Integer) {
                switchEnum = null;
                transpileParsedSource(sourceBuilder, ((Switch) part).condition, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(".toInt()");
            } else {
                switchEnum = new SwitchEnum();
                sourceBuilder.append("__switch_enum");
                sourceBuilder.append(java.lang.String.valueOf(extras.size()));
                sourceBuilder.append("__(");
                transpileParsedSource(sourceBuilder, ((Switch) part).condition, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
                sourceBuilder.append(")");
                extras.add(switchEnum);
            }
            sourceBuilder.appendln(") {");
            sourceBuilder.indent();
            boolean[] myBreak = new boolean[]{false};
            for(Parsed _part : ((Switch) part).impl.impl) {
                if(_part instanceof Case) {
                    sourceBuilder.append("case ");
                    Parsed ref = ((Case)_part).ref;
                    if(switchEnum == null)
                        sourceBuilder.append(java.lang.String.valueOf((int)((Long)ref).value));
                    else if(ref instanceof Long)
                        sourceBuilder.append(switchEnum.key("_" + java.lang.String.valueOf(((Long)ref).value), ref));
                    else if(ref instanceof Number)
                        sourceBuilder.append(switchEnum.key("_" + net.nexustools.njs.Number.toString(((Number)ref).value).replace(".", "_"), ref));
                    else if(ref instanceof String)
                        sourceBuilder.append(switchEnum.key("_" + ((String)ref).string.replaceAll("[^a-zA-Z0-9]", "_") + "_", ref));
                    else if(ref instanceof Undefined)
                        sourceBuilder.append(switchEnum.key("undefined", ref));
                    else if(ref instanceof Null)
                        sourceBuilder.append(switchEnum.key("null", ref));
                    else
                        throw new UnsupportedOperationException("Cannot compile case " + ref + ":");
                    sourceBuilder.appendln(":");
                } else if(_part instanceof Default) {
                    sourceBuilder.appendln("default:");
                } else {
                    transpileParsedSource(sourceBuilder, _part, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, myBreak, context, true);
                    if(!(_part instanceof Block))
                        sourceBuilder.appendln(";");
                    else
                        sourceBuilder.appendln();
                }
            }
            sourceBuilder.unindent();
            sourceBuilder.appendln("}");
            return false;
        } else if (part instanceof MultiBracket) {
            boolean first = true;
            sourceBuilder.append("CompiledScript.last(");
            for(Parsed _part : ((MultiBracket)part).parts) {
                if(first)
                    first = false;
                else
                    sourceBuilder.append(", ");
                transpileParsedSource(sourceBuilder, _part, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
                    generateStringSource(sourceBuilder, (Parsed)_part, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
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
            transpileParsedSource(sourceBuilder, ((In)part).rhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(".in(");
            generateStringSource(sourceBuilder, ((In)part).lhs, methodPrefix, baseScope, fileName, localStack, expectedStack, functionMap, classMap, scopeChain, sourceMap, extras, hasBreak, context);
            sourceBuilder.append(") ? global.Boolean.TRUE : global.Boolean.FALSE)");
            return false;
        } else if(part instanceof Break) {
            sourceBuilder.append("break");
            if(hasBreak != null)
                hasBreak[0] = true;
            return false;
        }

        throw new UnsupportedOperationException("Cannot compile: " + describe(part));
    }

    private static enum SourceScope {
        LambdaFunction,
        GlobalFunction,
        GlobalScript,
        SubFunction,
        ClassMethod,
        Function;

        private boolean isNonGlobalFunction() {
            return this == ClassMethod || this == SubFunction || this == Function;
        }

        private boolean isFunction() {
            return this == ClassMethod || this == GlobalFunction || this == SubFunction || this == Function || this == LambdaFunction;
        }

        private boolean isMethod() {
            return this == ClassMethod;
        }

        private boolean isLambda() {
            return this == LambdaFunction;
        }

        private boolean isGlobal() {
            return this == GlobalFunction || this == GlobalScript;
        }
    }

    protected void transpileScriptSource(SourceBuilder sourceBuilder, LocalStack localStack, ScriptData script, java.lang.String methodPrefix, java.lang.String fileName, ClassNameScopeChain scopeChain, SourceScope scope, List<java.lang.Object> extras, boolean[] hasBreak, TranspileContext context) {
        if (addDebugging || !scope.isFunction()) {
            sourceBuilder.appendln("@Override");
            sourceBuilder.appendln("public String source() {");
            sourceBuilder.append("    return \"");
            if (addDebugging) {
                sourceBuilder.append(convertStringSource(script.source));
            } else {
                sourceBuilder.append("[java_code]");
            }
            sourceBuilder.appendln("\";");
            sourceBuilder.appendln("}");
        }
        
        Map<java.lang.String, Class> classes = new HashMap();
        Map<java.lang.String, Function> functionsMap = new HashMap();
        for(Map.Entry<java.lang.String, Function> entry : script.functions.entrySet()) {
            functionsMap.put(scopeChain.refName(entry.getKey()), entry.getValue());
        }
        
        StackOptimizations opt = (StackOptimizations) script.optimizations;
        boolean usesStackClass = opt != null && opt.stackType() != ScopeOptimizer.StackType.TypedLocal;
        java.lang.String stackName = usesStackClass ? scopeChain.refName("Stack") : null;
        if (!scope.isGlobal()) {
            sourceBuilder.appendln("@Override");
            sourceBuilder.appendln("public String name() {");
            sourceBuilder.append("    return \"");
            boolean isLambda = script.callee != null && script.callee.isLambda();
            if (script.callee != null && script.callee.name != null) {
                sourceBuilder.append(isLambda ? "<lambda>" : "<anonymous>");
            } else {
                sourceBuilder.append(script.methodName != null ? convertStringSource(script.methodName) : (isLambda ? "<lambda>" : "<anonymous>"));
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
            if (opt == null && !scope.isMethod()) {
                sourceBuilder.append("    final Scope baseScope = extendScope(");
                if(!isLambda)
                    sourceBuilder.append("_this");
                sourceBuilder.appendln(");");
            }
            if (script.callee != null) {
                methodPrefix = extendMethodChain(methodPrefix, script.callee.name, isLambda);
                List<java.lang.String> arguments = script.callee.arguments;
                java.lang.String vararg = script.callee.vararg;
                if (opt == null) {
                    sourceBuilder.appendln("    baseScope.var(\"arguments\", new Arguments(global, this, params));");
                } else {
                    localStack.put("this", "this");
                    if (usesStackClass) {
                        for (int i = 0; i < arguments.size(); i++) {
                            localStack.put(arguments.get(i), "argument");
                        }
                        if(vararg != null)
                            localStack.put(vararg, "argument");
                        if (localStack.isEmpty()) {
                            sourceBuilder.appendln("    final Scope baseScope = extendScope(_this);");
                        } else {
                            sourceBuilder.append("    final ");
                            sourceBuilder.append(stackName);
                            sourceBuilder.append(" localStack = new ");
                            sourceBuilder.append(stackName);
                            sourceBuilder.appendln("();");
                            if (opt.stackType() == ScopeOptimizer.StackType.TypedClass) {
                                sourceBuilder.appendln("    final Scope baseScope = extendScope(_this);");
                            } else {
                                sourceBuilder.appendln("    final Scope baseScope = extendScope(_this, localStack);");
                            }
                        }
                        if (!arguments.contains("arguments") && opt.usesArguments()) {
                            sourceBuilder.appendln("    localStack.arguments = new Arguments(global, this, params);");
                            localStack.put("arguments", "arguments");
                        }
                    } else {
                        if (!arguments.contains("arguments") && opt.usesArguments()) {
                            sourceBuilder.appendln("    BaseObject arguments = new Arguments(global, this, params);");
                            localStack.put("arguments", "arguments");
                        }
                        for (int i = 0; i < arguments.size(); i++) {
                            sourceBuilder.append("    BaseObject ");
                            sourceBuilder.append(arguments.get(i));
                            sourceBuilder.appendln(";");
                            localStack.put(arguments.get(i), "argument");
                        }
                        if(vararg != null) {
                            sourceBuilder.append("    BaseObject ");
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
                            sourceBuilder.append("    ");
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
                    sourceBuilder.appendln("    switch(params.length) {");
                    int argsize = arguments.size();
                    for (int i = 0; i <= argsize; i++) {
                        int a = 0;
                        sourceBuilder.append("        ");
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
                                sourceBuilder.append("            baseScope.var(\"");
                                sourceBuilder.append(convertStringSource(arguments.get(a)));
                                sourceBuilder.append("\", params[");
                                sourceBuilder.append(java.lang.String.valueOf(a));
                                sourceBuilder.appendln("]);");
                            } else {
                                sourceBuilder.append("            ");
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
                                sourceBuilder.append("            baseScope.var(\"");
                                sourceBuilder.append(convertStringSource(arguments.get(a)));
                                sourceBuilder.appendln("\", Undefined.INSTANCE);");
                            } else {
                                sourceBuilder.append("            ");
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
                                    sourceBuilder.append("            baseScope.var(\"");
                                    sourceBuilder.append(convertStringSource(vararg));
                                    sourceBuilder.append("\", new GenericArray(global, params, ");
                                    sourceBuilder.append(java.lang.String.valueOf(argsize));
                                    sourceBuilder.appendln("));");
                                } else {
                                    sourceBuilder.append("            ");
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
                                    sourceBuilder.append("            baseScope.var(\"");
                                    sourceBuilder.append(convertStringSource(vararg));
                                    sourceBuilder.appendln("\", new GenericArray(global));");
                                } else {
                                    sourceBuilder.append("            ");
                                    if (usesStackClass) {
                                        sourceBuilder.append("localStack.");
                                    }
                                    sourceBuilder.append(vararg);
                                    sourceBuilder.appendln(" = new GenericArray(global);");
                                }
                            }
                        }
                        sourceBuilder.appendln("            break;");
                    }
                    sourceBuilder.appendln("    }");
                } else if(vararg != null) {
                    if (script.optimizations == null) {
                        sourceBuilder.append("    baseScope.var(\"");
                        sourceBuilder.append(convertStringSource(vararg));
                        sourceBuilder.append("\", new GenericArray(global, params));");
                    } else {
                        sourceBuilder.append("    ");
                        if (usesStackClass) {
                            sourceBuilder.append("localStack.");
                        }
                        sourceBuilder.append(vararg);
                        sourceBuilder.append(" = new GenericArray(global, params);");
                    }
                }
            } else {
                methodPrefix = extendMethodChain(methodPrefix, script.methodName, isLambda);
            }
        } else {
            sourceBuilder.appendln("@Override");
            if (scope.isFunction()) {
                sourceBuilder.appendln("public BaseObject exec(Global global, final Scope scope) {");
                sourceBuilder.appendln("    final Scope baseScope = scope == null ? Scope.current().beginBlock() : scope;");
                sourceBuilder.appendln("    final BaseObject _this = baseScope._this;");
            } else {
                sourceBuilder.appendln("public BaseObject exec(Global global, Scope scope) {");
                sourceBuilder.appendln("    final Scope baseScope = scope == null ? new Scope(global) : scope;");
                sourceBuilder.appendln("    final BaseObject _this = global;");
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
            for (Map.Entry<java.lang.String, Function> entry : functionsMap.entrySet()) {
                Function function = entry.getValue();
                java.lang.String functionName = entry.getKey();
                if (usesStackClass) {
                    sourceBuilder.append("localStack.");
                    sourceBuilder.append(functionName);
                } else {
                    sourceBuilder.append(functionName);
                    sourceBuilder.append(" ");
                    sourceBuilder.append(function.name);
                }
                sourceBuilder.append(" = new ");
                sourceBuilder.append(functionName);
                sourceBuilder.appendln("(");
                if(function.isLambda())
                    sourceBuilder.append("_this, ");
                sourceBuilder.append("global, baseScope);");
                localStack.put(function.name, "function");
            }
        } else {
            for (Map.Entry<java.lang.String, Function> entry : functionsMap.entrySet()) {
                sourceBuilder.append("baseScope.var(\"");
                sourceBuilder.append(convertStringSource(entry.getValue().name));
                sourceBuilder.append("\", new ");
                sourceBuilder.append(entry.getKey());
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
            boolean hasReturn, lastAtTop = true;
            int last = script.impl.length-1;
            if(last > -1 && !(script.impl[last] instanceof Return) && !(script.impl[last] instanceof Block) && !(script.impl[last] instanceof Throw)) {
                script.impl[last] = new Return(script.impl[last]);
                hasReturn = true;
            } else
                hasReturn = false;
            int i=0;
            for (Parsed part : script.impl) {
                if (addDebugging) {
                    addSourceMapEntry(sourceBuilder, sourceMap, part);
                }
                hasReturn = transpileParsedSource(sourceBuilder, part, methodPrefix, "baseScope", fileName, localStack, expectedStack, functionsMap, classes, scopeChain, sourceMap, extras, hasBreak, context, i++ != last || lastAtTop) || hasReturn;
                if(!(part instanceof Block))
                    sourceBuilder.appendln(";");
                else
                    sourceBuilder.appendln();
            }

            if (!hasReturn) {
                sourceBuilder.appendln("return Undefined.INSTANCE;");
            }
        } else if (script.impl.length > 0) {
            boolean hasReturn, lastAtTop = true;
            int last = script.impl.length-1;
            if(last > -1 && !(script.impl[last] instanceof Return) && !(script.impl[last] instanceof Block) && !(script.impl[last] instanceof Throw)) {
                hasReturn = true;
                script.impl[last] = new Return(script.impl[last]);
                lastAtTop = false;
            } else
                hasReturn = false;
            for (int i = 0; i < script.impl.length; i++) {
                Parsed part = script.impl[i];
                if (addDebugging) {
                    addSourceMapEntry(sourceBuilder, sourceMap, part);
                }
                hasReturn = transpileParsedSource(sourceBuilder, part, methodPrefix, "baseScope", fileName, localStack, expectedStack, functionsMap, classes, scopeChain, sourceMap, extras, hasBreak, context, i != last || lastAtTop) || hasReturn;
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
            sourceBuilder.appendln("    baseScope.exit();");
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
                    sourceBuilder.append("    return this.");
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
                    sourceBuilder.append("    this.");
                    sourceBuilder.append(key);
                    sourceBuilder.appendln(" = val;");
                    sourceBuilder.appendln("    return;");
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
                    sourceBuilder.appendln("    return false;");
                }
                sourceBuilder.appendln("return or.or(key);");
                sourceBuilder.unindent();
                sourceBuilder.appendln("}");
            }

            sourceBuilder.unindent();
            sourceBuilder.appendln("}");
        }
        
        for (Map.Entry<java.lang.String, Class> entry : classes.entrySet()) {
            ClassNameScopeChain _scopeChain = scopeChain.extend();
            Map<java.lang.String, Function> functionMap = new HashMap();
            
            Class clazz = entry.getValue();
            java.lang.String className = entry.getKey();
            sourceBuilder.append("private static final class ");
            sourceBuilder.append(className);
            sourceBuilder.appendln(" extends CompiledFunction {");
            sourceBuilder.indent();
            sourceBuilder.appendln("private final Scope baseScope;");
            boolean _extends = clazz._extends != null;
            if(_extends)
                sourceBuilder.appendln("private final BaseFunction _super;");
            sourceBuilder.append("private ");
            sourceBuilder.append(className);
            sourceBuilder.append("(");
            if(_extends)
                sourceBuilder.append("BaseFunction _super, ");
            sourceBuilder.appendln("Global global, Scope scope) {");
            sourceBuilder.indent();
            sourceBuilder.append("super(");
            if(_extends)
                sourceBuilder.append("_super, ");
            sourceBuilder.appendln("global);");
            if(_extends)
                sourceBuilder.appendln("this._super = _super;");
            sourceBuilder.appendln("baseScope = scope;");
            GetterSetter getset;
            ClassMethod constructor = null;
            Map<Parsed, ClassMethod> symbols = new HashMap();
            Map<java.lang.String, GetterSetter> properties = new HashMap();
            for(ClassMethod classMethod : clazz.methods) {
                switch(classMethod.type) {
                    case Constructor:
                        constructor = classMethod;
                        break;
                    case Symbol:
                        symbols.put(classMethod.symbolRef, classMethod);
                        break;
                    case Getter:
                        getset = properties.get(classMethod.name);
                        if(getset == null)
                            properties.put(classMethod.name, getset = new GetterSetter());
                        getset.getter = classMethod;
                        break;
                    case Setter:
                        getset = properties.get(classMethod.name);
                        if(getset == null)
                            properties.put(classMethod.name, getset = new GetterSetter());
                        getset.setter = classMethod;
                        break;
                    case Normal:
                        sourceBuilder.append("((GenericObject)prototype).setHidden(\"");
                        sourceBuilder.append(convertStringSource(classMethod.name));
                        sourceBuilder.append("\", new ");
                        
                        java.lang.String name = classMethod.name;

                        name = _scopeChain.refName(name);
                        if (functionMap.containsKey(name)) {
                            int index = 2;
                            java.lang.String base = name + "_";
                            do {
                                name = base + index++;
                            } while (functionMap.containsKey(name));
                            name = _scopeChain.refName(name);
                        }
                        functionMap.put(name, classMethod);

                        sourceBuilder.append(name);
                        sourceBuilder.append("(");
                        if(_extends)
                            sourceBuilder.append("_super, ");
                        sourceBuilder.appendln("global, scope));");
                        break;
                    default:
                        throw new RuntimeException("Cannot compile `" + classMethod + "`");
                }
            }
            for(Map.Entry<Parsed, ClassMethod> _entry : symbols.entrySet()) {
                
            }
            for(Map.Entry<java.lang.String, GetterSetter> _entry : properties.entrySet()) {
                sourceBuilder.append("((GenericObject)prototype).defineProperty(\"");
                sourceBuilder.append(convertStringSource(_entry.getKey()));
                sourceBuilder.append("\", ");
                GetterSetter _getset = _entry.getValue();
                if(_getset.getter == null)
                    sourceBuilder.append("null");
                else {
                    java.lang.String name = "get_" + _getset.getter.name;

                    name = _scopeChain.refName(name);
                    if (functionMap.containsKey(name)) {
                        int index = 2;
                        java.lang.String base = name + "_";
                        do {
                            name = base + index++;
                        } while (functionMap.containsKey(name));
                        name = _scopeChain.refName(name);
                    }
                    functionMap.put(name, _getset.getter);
                    
                    sourceBuilder.append("new ");
                    sourceBuilder.append(name);
                    sourceBuilder.append("(");
                    if(_extends)
                        sourceBuilder.append("_super, ");
                    sourceBuilder.append("global, scope)");
                }
                sourceBuilder.append(", ");
                if(_getset.setter == null)
                    sourceBuilder.append("null");
                else {
                    java.lang.String name = "set_" + _getset.setter.name;

                    name = _scopeChain.refName(name);
                    if (functionMap.containsKey(name)) {
                        int index = 2;
                        java.lang.String base = name + "_";
                        do {
                            name = base + index++;
                        } while (functionMap.containsKey(name));
                        name = _scopeChain.refName(name);
                    }
                    functionMap.put(name, _getset.setter);
                    
                    sourceBuilder.append("new ");
                    sourceBuilder.append(name);
                    sourceBuilder.append("(");
                    if(_extends)
                        sourceBuilder.append("_super, ");
                    sourceBuilder.append("global, scope)");
                }
                sourceBuilder.appendln(");");
            }
            sourceBuilder.unindent();
            sourceBuilder.appendln("}");
            if(constructor == null) {
                constructor = new ClassMethod(clazz);
                constructor.type = ClassMethod.Type.Constructor;
                constructor.name = "<constructor>";
                Parsed[] impl;
                if(_extends)
                    impl = new Parsed[]{
                        new Call(new Reference("super")),
                        null
                    };
                else
                    impl = new Parsed[1];
                impl[impl.length-1] = new Return(new Reference("this"));
                constructor.impl = new ScriptData(impl, "[java_code]", 0, 0);
            }
            List<java.lang.Object> _extras = new ArrayList();
            LocalStack funcLocalStack = constructor.impl.optimizations == null ? null : new LocalStack(true);
            transpileScriptSource(sourceBuilder, funcLocalStack, constructor.impl, methodPrefix, fileName, _scopeChain, scope.isNonGlobalFunction() ? SourceScope.SubFunction : SourceScope.Function, _extras, null, TranspileContext.ClassMethod);
            generateExtras(sourceBuilder, extras);
            generateFunctions(sourceBuilder, functionMap, methodPrefix, fileName, _scopeChain, SourceScope.SubFunction, _extends, TranspileContext.ClassMethod);
            sourceBuilder.appendln("Scope extendScope(BaseObject _this) {");
            sourceBuilder.appendln("    return baseScope.extend(_this);");
            sourceBuilder.appendln("}");
            sourceBuilder.unindent();
            sourceBuilder.appendln("}");
        }

        generateFunctions(sourceBuilder, functionsMap, methodPrefix, fileName, scopeChain, scope.isNonGlobalFunction() ? SourceScope.SubFunction : SourceScope.Function, false, TranspileContext.Function);

        if (addDebugging) {
            sourceBuilder.append("public");
            //if(scope != SourceScope.SubFunction)
            sourceBuilder.append(" static");
            sourceBuilder.append(" final Map<Integer, Utilities.FilePosition> SOURCE_MAP = Collections.unmodifiableMap(new LinkedHashMap<Integer, Utilities.FilePosition>()");
            if (!sourceMap.isEmpty()) {
                sourceBuilder.appendln(" {");
                sourceBuilder.appendln("    {");
                for (Map.Entry<java.lang.Integer, FilePosition> entry : sourceMap.entrySet()) {
                    FilePosition fpos = entry.getValue();
                    if(fpos.row == 0 || fpos.column == 0)
                        continue;
                    
                    sourceBuilder.append("        put(");
                    sourceBuilder.append("" + entry.getKey());
                    sourceBuilder.append(", new Utilities.FilePosition(");
                    sourceBuilder.append("" + entry.getValue().row);
                    sourceBuilder.append(", ");
                    sourceBuilder.append("" + entry.getValue().column);
                    sourceBuilder.appendln("));");
                }
                sourceBuilder.appendln("    }");
                sourceBuilder.append("}");
            }
            sourceBuilder.appendln(");");
        }
    }
    
    protected void generateFunctions(SourceBuilder sourceBuilder, Map<java.lang.String, Function> functionMap, java.lang.String methodPrefix, java.lang.String fileName, ClassNameScopeChain scopeChain, SourceScope scope, boolean _scopeHasSuper, TranspileContext context) {
        for (Map.Entry<java.lang.String, Function> entry : functionMap.entrySet()) {
            ClassNameScopeChain _scopeChain = scopeChain.extend();
            java.lang.String functionName = entry.getKey();
            Function function = entry.getValue();
            
            boolean scopeHasSuper = _scopeHasSuper && function instanceof ClassMethod;
            //if(scope.isGlobal())
            sourceBuilder.append("private static final class ");
            sourceBuilder.append(functionName);
            sourceBuilder.append(" extends Compiled");
            sourceBuilder.append(function.isLambda() ? "Lambda" : "Function");
            sourceBuilder.appendln(" {");
            sourceBuilder.indent();
            sourceBuilder.appendln("private final Scope baseScope;");
            if(scopeHasSuper)
                sourceBuilder.appendln("private final BaseFunction _super;");
            StackOptimizations funcopt = function.impl == null ? null : (StackOptimizations)function.impl.optimizations;

            sourceBuilder.append("private ");
            sourceBuilder.append(functionName);
            sourceBuilder.append("(");
            if(scopeHasSuper)
                sourceBuilder.append("BaseFunction _super, ");
            if(function.isLambda())
                sourceBuilder.append("BaseObject _this, ");
            sourceBuilder.appendln("Global global, Scope scope) {");
            sourceBuilder.append("    super(");
            if(function.isLambda())
                sourceBuilder.append("_this, ");
            sourceBuilder.appendln("global);");
            sourceBuilder.appendln("    baseScope = scope;");
            if(scopeHasSuper)
                sourceBuilder.appendln("    this._super = _super;");
            sourceBuilder.appendln("}");

            if(function.impl == null) {
                System.out.println("WARNING: function was not finished...");
                function.finish();
            }

            List<java.lang.Object> extras = new ArrayList();
            LocalStack funcLocalStack = function.impl.optimizations == null ? null : new LocalStack(true);
            transpileScriptSource(sourceBuilder, funcLocalStack, function.impl, methodPrefix, fileName, _scopeChain, scope, extras, null, context);

            if (funcopt == null || (funcopt.stackType() != ScopeOptimizer.StackType.TypedLocal && (funcopt.stackType() == ScopeOptimizer.StackType.TypedClass || funcLocalStack.isEmpty()))) {
                sourceBuilder.append("Scope extendScope(");
                if(!function.isLambda())
                    sourceBuilder.append("BaseObject _this");
                sourceBuilder.appendln(") {");
                sourceBuilder.appendln("    return baseScope.extend(_this);");
                sourceBuilder.appendln("}");
            } else if (funcopt.stackType() != ScopeOptimizer.StackType.TypedLocal) {
                sourceBuilder.appendln("Scope extendScope(");
                if(!function.isLambda())
                    sourceBuilder.append("BaseObject _this, ");
                sourceBuilder.appendln("Scopable stack) {");
                sourceBuilder.appendln("    return baseScope.extend(_this, stack);");
                sourceBuilder.appendln("}");
            }
            
            generateExtras(sourceBuilder, extras);

            sourceBuilder.unindent();
            sourceBuilder.appendln("}");
        }
    }
    private void generateExtras(SourceBuilder sourceBuilder, List<Object> extras) {
        for(int i=0; i<extras.size(); i++) {
            java.lang.Object extra = extras.get(i);
            if(extra instanceof SwitchEnum) {
                sourceBuilder.appendln("private enum SwitchEnum" + i + " {");
                sourceBuilder.indent();
                boolean first = true;
                for(java.lang.String key : ((SwitchEnum) extra).keys.keySet()) {
                    if(first)
                        first = false;
                    else
                        sourceBuilder.appendln(",");
                    sourceBuilder.append(key);
                }
                sourceBuilder.appendln();
                sourceBuilder.unindent();
                sourceBuilder.appendln("}");
                sourceBuilder.appendln("private static SwitchEnum" + i + " __switch_enum" + i + "__(BaseObject val) {");
                sourceBuilder.indent();
                for(Map.Entry<java.lang.String, Parsed> entry : ((SwitchEnum) extra).keys.entrySet()) {
                    Parsed part = entry.getValue();
                    sourceBuilder.append("if (");
                    if(part instanceof Long) {
                        sourceBuilder.append("val instanceof net.nexustools.njs.Number.Instance && val.toLong() == ");
                        sourceBuilder.append(java.lang.String.valueOf(((Long)part).value));
                        sourceBuilder.append("L");
                    } else if(part instanceof Number) {
                        sourceBuilder.append("val instanceof net.nexustools.njs.Number.Instance && val.toDouble() == ");
                        double val = ((Number)part).value;
                        if(Double.isNaN(val))
                            sourceBuilder.append("Double.NaN");
                        else if(val == Double.POSITIVE_INFINITY)
                            sourceBuilder.append("Double.POSITIVE_INFINITY");
                        else if(val == Double.NEGATIVE_INFINITY)
                            sourceBuilder.append("Double.NEGATIVE_INFINITY");
                        else {
                            sourceBuilder.append(java.lang.String.valueOf(((Number)part).value));
                            sourceBuilder.append("D");
                        }
                    } else if(part instanceof String) {
                        sourceBuilder.append("val instanceof net.nexustools.njs.String.Instance && ((net.nexustools.njs.String.Instance)val).string.equals(\"");
                        sourceBuilder.append(convertStringSource(((String)part).string));
                        sourceBuilder.append("\")");
                    } else if(part instanceof Undefined) {
                        sourceBuilder.append("val == Undefined.INSTANCE");
                    } else if(part instanceof Null) {
                        sourceBuilder.append("val == Null.INSTANCE");
                    } else
                        throw new RuntimeException("Cannot compile " + part);
                    sourceBuilder.appendln(")");
                    sourceBuilder.appendln("    return SwitchEnum" + i + "." + entry.getKey() + ";");
                }
                sourceBuilder.appendln("return null;");
                sourceBuilder.unindent();
                sourceBuilder.appendln("}");
            } else
                throw new RuntimeException("Cannot compile " + extra);
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
        sourceBuilder.append(className);
        sourceBuilder.append(" extends CompiledScript.");
        sourceBuilder.append(addDebugging ? "Debuggable" : "Optimized");
        sourceBuilder.appendln("{");
        sourceBuilder.indent();
        
        sourceBuilder.append("public ");
        sourceBuilder.append(className);
        sourceBuilder.appendln("(Global global) {");
        sourceBuilder.appendln("    super(global);");
        sourceBuilder.appendln("}");
        
        sourceBuilder.append("public ");
        sourceBuilder.append(className);
        sourceBuilder.appendln("() {}");
        
        List<java.lang.Object> extras = new ArrayList();
        try {
            if (inFunction) {
                ScopeOptimizer variableScope = new ScopeOptimizer();
                try {
                    scanScriptSource(script, variableScope);
                    script.optimizations = new MapStackOptimizations(variableScope.scope, variableScope.stackType, variableScope instanceof FunctionScopeOptimizer ? ((FunctionScopeOptimizer) variableScope).usesArguments : false);
                } catch (CannotOptimize ex) {
                    if (ex instanceof CannotOptimizeUnimplemented) {
                        ex.printStackTrace(System.out);
                    }
                }
            }
            transpileScriptSource(sourceBuilder, script.optimizations == null ? null : new LocalStack(!inFunction), script, script.methodName, fileName, BASE_CLASS_NAME_SCOPE_CHAIN, inFunction ? SourceScope.GlobalFunction : SourceScope.GlobalScript, extras, null, inFunction ? TranspileContext.Function : TranspileContext.Script);
        } catch (RuntimeException t) {
            System.err.println(sourceBuilder.toString());
            throw t;
        }
        
        generateExtras(sourceBuilder, extras);

        if (generateMain) {
            sourceBuilder.appendln("public static void main(String[] args) {");
            sourceBuilder.appendln("    Global global = Utilities.createExtendedGlobal();");
            sourceBuilder.appendln("    Scope scope = new Scope(global);");
            sourceBuilder.appendln("    scope.var(\"arguments\", Utilities.convertArguments(global, args));");
            sourceBuilder.append("    new ");
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
            className = BASE_CLASS_NAME_SCOPE_CHAIN.refName(fileName.substring(0, fileName.length() - 3));
        } else {
            className = BASE_CLASS_NAME_SCOPE_CHAIN.refName(fileName);
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

    public java.lang.String transpile(Reader reader, java.lang.String input, java.lang.String className, java.lang.String pkg, boolean generateMain) {
        return transpileJavaClassSource(parse(reader, className, false), className, input, pkg, false, generateMain);
    }

    public static void main(java.lang.String... args) {
        boolean addDebugging = true, generateMain = true, module = false;
        java.lang.String input = null, output = null, pkg = null, rclassname = null;
        File root = null;
        for (int i = 0; i < args.length; i++) {
            java.lang.String arg = args[i];
            if (arg.startsWith("-") && arg.length() > 1) {
                if (arg.equals("-s") || arg.equals("-strip") || arg.equals("--strip")) {
                    addDebugging = false;
                } else if (arg.equals("-r") || arg.equals("-root") || arg.equals("--root")) {
                    root = new File(args[++i]);
                } else if (arg.equals("-u") || arg.equals("-module") || arg.equals("--module")) {
                    module = true;
                    generateMain = false;
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
            className = compiler.BASE_CLASS_NAME_SCOPE_CHAIN.refName(rclassname);
        } else if (baseName.endsWith(".java")) {
            className = compiler.BASE_CLASS_NAME_SCOPE_CHAIN.refName(baseName.substring(0, baseName.length() - 5));
        } else {
            className = compiler.BASE_CLASS_NAME_SCOPE_CHAIN.refName(baseName);
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
            if(module) {
                final Reader[] sources = new Reader[]{
                    new java.io.StringReader("(function module(exports, require, module, __filename, __dirname) { "),
                    new FileReader(input),
                    new java.io.StringReader("\n});")
                };
                Reader reader = new Reader() {
                    int reader = 0;
                    @Override
                    public int read(char[] cbuf, int off, int len) throws IOException {
                        while(true) {
                            if(reader >= sources.length)
                                return -1;
                            
                            int read = sources[reader].read(cbuf, off, len);
                            if(read <= 0) {
                                reader ++;
                                continue;
                            }
                            
                            return read;
                        }
                    }

                    @Override
                    public void close() throws IOException {
                        for(int i=reader; i<sources.length; i++)
                            sources[i].close();
                        reader = sources.length;
                    }
                };
                if(root == null)
                    source = compiler.transpile(reader, input, className, pkg, generateMain);
                else {
                    java.lang.String rootPath = root.getAbsolutePath();
                    if(!rootPath.endsWith(File.separator) && !rootPath.endsWith("/"))
                        rootPath += File.separator;
                    if(input.startsWith(rootPath)) {
                        java.lang.String name = input.substring(rootPath.length());
                        source = compiler.transpile(reader, name, className, pkg, generateMain);
                    } else
                        source = compiler.transpile(reader, input, className, pkg, generateMain);
                }
            } else {
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
