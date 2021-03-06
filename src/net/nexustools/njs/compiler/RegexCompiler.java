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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.nexustools.njs.Error;

/**
 *
 * @author Katelyn Slater <kate@nexustools.com>
 */
public abstract class RegexCompiler implements Compiler {

    public static final boolean DEBUG = System.getProperty("NJSDEBUG", "false").equals("true");
    
    public static class Unexpected extends RuntimeException {
        private final Parsed current, next;
        public Unexpected(Parsed current, Parsed next) {
            this.current = current;
            this.next = next;
        }
    }

    public static java.lang.String convertStringSource(java.lang.String string) {
        StringBuilder builder = new StringBuilder();
        for(char c : string.toCharArray()) {
            switch(c) {
                case '\t':
                    builder.append("\\t");
                    continue;
                case '\n':
                    builder.append("\\n");
                    continue;
                case '\r':
                    builder.append("\\r");
                    continue;
                case '"':
                    builder.append("\\\"");
                    continue;
                case '\\':
                    builder.append("\\\\");
                    continue;
                default:
                    if(c < 12) {
                        builder.append("\\u");
                        java.lang.String hex = java.lang.Integer.toString(c, 16);
                        while(hex.length() < 4)
                            hex = "0" + hex;
                        builder.append(hex);
                    } else
                        builder.append(c);
            }
        }
        return builder.toString();
    }

    public static java.lang.String unparsed(java.lang.Object object) {
        if (object == null) {
            return "<unparsed>";
        }
        return object.toString();
    }

    public static java.lang.String unparsedToSource(Parsed object) {
        if (object == null) {
            return "<unparsed>";
        }
        return object.toSource();
    }

    public static java.lang.String describe(java.lang.Object object) {
        if (object == null) {
            return "null";
        }
        return "(@" + object.getClass().getSimpleName() + ":" + object + ")";
    }

    public static java.lang.String toString(java.lang.Object object) {
        if (object == null) {
            return "";
        }
        return object.toString();
    }

    public static java.lang.String toRPadString(java.lang.Object object) {
        if (object == null) {
            return "";
        }
        return object.toString() + ' ';
    }

    public static java.lang.String toLPadString(java.lang.Object object) {
        if (object == null) {
            return "";
        }
        return ' ' + object.toString();
    }

    public static java.lang.String join(Iterable<?> able, char with) {
        StringBuilder builder = new StringBuilder();
        join(able, with, builder);
        return builder.toString();
    }

    public static void join(Iterable<?> able, char with, StringBuilder builder) {
        Iterator<?> it = able.iterator();
        if (it.hasNext()) {
            builder.append(it.next());
            while (it.hasNext()) {
                builder.append(with);
                builder.append(it.next());
            }
        }
    }

    public static void joinToSource(Iterable<Parsed> able, char with, StringBuilder builder) {
        Iterator<Parsed> it = able.iterator();
        if (it.hasNext()) {
            builder.append(it.next().toSource());
            while (it.hasNext()) {
                builder.append(with);
                builder.append(it.next().toSource());
            }
        }
    }

    public static class ScriptData {

        public Function callee;
        public final Parsed[] impl;
        public final int rows, columns;
        public java.lang.Object optimizations;
        public java.lang.String methodName = null, source;
        public final Map<java.lang.String, Function> functions;

        public ScriptData(Parsed[] impl, java.lang.String source, int rows, int columns) {
            functions = new HashMap();
            List<Parsed> imp = new ArrayList();
            for (int i = 0; i < impl.length; i++) {
                Parsed im = impl[i];
                if (im instanceof Function && ((Function) im).name != null) {
                    assert (((Function) im).name != null && !((Function) im).name.isEmpty());
                    functions.put(((Function) im).name, (Function) im);
                    continue;
                }

                imp.add(impl[i]);
            }
            this.impl = imp.toArray(new Parsed[imp.size()]);
            this.source = source;
            this.columns = columns;
            this.rows = rows;
        }
        
        public void transform(Transformer transformer) {
            for(int i=0; i<impl.length; i++)
                impl[i] = impl[i].transform(transformer);
            for(Function func : functions.values())
                func.impl.transform(transformer);
        }

        @Override
        public java.lang.String toString() {
            StringBuilder builder = new StringBuilder();
            join(Arrays.asList(functions), ';', builder);
            if (builder.length() > 0) {
                builder.append(':');
            }
            join(Arrays.asList(impl), ';', builder);
            return builder.toString();
        }
    }

    public static class Reparse extends RuntimeException {

        final java.lang.String buffer;
        final Parsed transform;

        public Reparse(java.lang.String buffer, Parsed transform) {
            this.buffer = buffer;
            this.transform = transform;
        }
    }

    public static class CompleteException extends RuntimeException {

        final Parsed part;

        public CompleteException() {
            this(null);
        }

        public CompleteException(Parsed part) {
            this.part = part;
        }

        @Override
        public java.lang.String toString() {
            return describe(part);
        }
    }

    public static class ParseFunction extends RuntimeException {

        public final Function function;

        public ParseFunction(Function function) {
            this.function = function;
        }
    }

    public static class ParseBlock extends RuntimeException {

        public final Block block;

        public ParseBlock(Block block) {
            this.block = block;
        }
    }

    public static class ParseSwitch extends RuntimeException {

        public final Switch block;

        public ParseSwitch(Switch block) {
            this.block = block;
        }
    }

    public static class ParseComplete extends RuntimeException {

        public final ScriptData impl;
        public final java.lang.String source;

        public ParseComplete(ScriptData impl, java.lang.String source) {
            this.impl = impl;
            this.source = source;
        }
    }
    public static interface Transformer {
        public Parsed transform(Parsed input);
    }

    public static abstract class Parsed {

        public int columns, rows;

        public abstract Parsed transform(Parsed part);
        public Parsed transform(Transformer transformer) {
            return transformer.transform(this);
        }

        public java.lang.String toSource() {
            return toSource(false);
        }
        public abstract java.lang.String toSource(boolean simple);

        public java.lang.String toSimpleSource() {
            return toSource(true);
        }

        @Override
        public java.lang.String toString() {
            return toSimpleSource();
        }
        
        public boolean isTrue() {
            return false;
        }

        public boolean isNumber() {
            return this instanceof NumberReferency;
        }

        public boolean isNumberOrBool() {
            return isNumber() || isBoolean();
        }

        public boolean isBoolean() {
            return false;
        }

        public boolean isString() {
            return this instanceof StringReferency;
        }

        public java.lang.String primaryType() {
            if (isNumber()) {
                return "number";
            }
            if (isString()) {
                return "string";
            }
            return "unknown";
        }

        public abstract boolean isStandalone();

        public abstract boolean isIncomplete();

        public abstract Parsed finish();
    }

    public static interface NumberReferency {
    }

    public static interface StringReferency {
    }

    public static interface BaseReferency {

        public int precedence();

        public Parsed transformFallback(Parsed part);
    }

    public static abstract class PrimitiveReferency extends Parsed implements BaseReferency {

        public PrimitiveReferency extend(DirectReference reference) {
            return new RightReference(this, reference.ref);
        }

        public PrimitiveReferency extend(IntegerReference reference) {
            return new IntegerReference(this, reference.ref);
        }

        @Override
        public Parsed transform(Parsed part) {
            if (DEBUG && !isIncomplete()) {
                System.out.println('\t' + describe(this) + " -> " + describe(part));
            }

            if (part instanceof SemiColon) {
                throw new CompleteException();
            }

            if (part instanceof DirectReference) {
                return extend((DirectReference) part);
            } else if (part instanceof IntegerReference) {
                return extend((IntegerReference) part);
            } else if (part instanceof OpenBracket) {
                return new Call(part, this);
            } else if (part instanceof InstanceOf) {
                return new InstanceOf(this);
            } else if (part instanceof MoreThan) {
                return new MoreThan(this);
            } else if (part instanceof LessThan) {
                return new LessThan(this);
            } else if (part instanceof MoreEqual) {
                return new MoreEqual(this);
            } else if (part instanceof LessEqual) {
                return new LessEqual(this);
            } else if (part instanceof StrictEquals) {
                return new StrictEquals(this);
            } else if (part instanceof NotEquals) {
                return new NotEquals(this);
            } else if (part instanceof StrictNotEquals) {
                return new StrictNotEquals(this);
            } else if (part instanceof Equals) {
                return new Equals(this);
            } else if (part instanceof Divide) {
                return new Divide(this);
            } else if (part instanceof Multiply) {
                return new Multiply(this);
            } else if (part instanceof Plus) {
                return new Plus(this);
            } else if (part instanceof Minus) {
                return new Minus(this);
            } else if (part instanceof Set) {
                return new Set(this);
            } else if (part instanceof Percent) {
                return new Percent(this);
            } else if (part instanceof AndEq) {
                return new AndEq(this);
            } else if (part instanceof OrEq) {
                return new OrEq(this);
            } else if (part instanceof And) {
                return new And(this);
            } else if (part instanceof Or) {
                return new Or(this);
            } else if (part instanceof AndAnd) {
                return new AndAnd(this);
            } else if (part instanceof In) {
                return new In(this);
            } else if (part instanceof OrOr) {
                return new OrOr(this);
            } else if (part instanceof Number && ((Number) part).value < 0) {
                return new Minus(this, new Number(-((Number) part).value));
            } else if (part instanceof Long && ((Long) part).value < 0) {
                return new Minus(this, new Number(-((Long) part).value));
            } else if (part instanceof RegEx) {
                throw new Reparse(((RegEx)part).pattern + "/" + ((RegEx)part).flags, new Divide());
            } else {
                return transformFallback(part);
            }
        }

        public Parsed transformFallback(Parsed part) {
            throw new Unexpected(this, part);
        }

        @Override
        public boolean isIncomplete() {
            return false;
        }

        @Override
        public boolean isStandalone() {
            return true;
        }

        public Parsed finish() {
            return this;
        }
    }

    public static abstract class Referency extends PrimitiveReferency {

        @Override
        public Parsed transformFallback(Parsed part) {
            if (part instanceof OpenArray) {
                return new VariableReference(this);
            } else if (part instanceof MultiplyEq) {
                return new MultiplyEq(this);
            } else if (part instanceof DivideEq) {
                return new DivideEq(this);
            } else if (part instanceof PlusEq) {
                return new PlusEq(this);
            } else if (part instanceof MinusEq) {
                return new MinusEq(this);
            } else if (part instanceof PlusPlus) {
                return new PlusPlus(this);
            } else if (part instanceof MinusMinus) {
                return new MinusMinus(this);
            } else if (part instanceof ShiftRight) {
                return new ShiftRight(this);
            } else if (part instanceof ShiftLeft) {
                return new ShiftLeft(this);
            } else if (part instanceof DoubleShiftRight) {
                return new DoubleShiftRight(this);
            } else if (part instanceof DoubleShiftRightEq) {
                return new DoubleShiftRightEq(this);
            } else if (part instanceof Fork) {
                return new Fork(this);
            }
            return super.transformFallback(part);
        }

    }

    public static abstract class VolatileReferency extends Referency {

        @Override
        public Referency extend(DirectReference reference) {
            throw new Unexpected(this, reference);
        }

        @Override
        public Referency extend(IntegerReference reference) {
            throw new Unexpected(this, reference);
        }
    }

    public static class VariableReference extends Referency {

        public Parsed ref;
        private boolean closed;
        public Referency lhs;

        public VariableReference(Referency lhs) {
            this.lhs = lhs;
        }

        @Override
        public Parsed transform(Transformer transformer) {
            lhs = (Referency)lhs.transform(transformer);
            return super.transform(transformer);
        }
        
        @Override
        public Parsed transform(Parsed part) {
            if (closed) {
                return super.transform(part);
            }

            if (ref == null) {
                ref = part;
            } else {
                if (!ref.isIncomplete()) {
                    if (part instanceof CloseArray) {
                        ref = ref.finish();
                        closed = true;
                        return this;
                    }
                }

                ref = ref.transform(part);
            }

            return this;
        }

        @Override
        public boolean isIncomplete() {
            return !closed || ref == null || ref.isIncomplete();
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder builder = new StringBuilder(unparsedToSource(lhs));
            builder.append('[');
            builder.append(unparsedToSource(ref));
            if (closed) {
                builder.append(']');
            }
            return builder.toString();
        }

        @Override
        public int precedence() {
            return 19;
        }

    }

    public static class Reference extends Referency {

        public final java.lang.String ref;

        public Reference(java.lang.String ref) {
            this.ref = ref;
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return ref;
        }

        @Override
        public Parsed transform(Parsed part) {
            if (part instanceof Lambda) {
                Function function = new Function();
                function.arguments.add(ref);
                function.type = Function.Type.Lambda;
                function.state = Function.State.InLambda;
                function.storage = new LambdaReturn(function);
                return function;
            }

            return super.transform(part);
        }

        @Override
        public Referency extend(DirectReference reference) {
            ReferenceChain chain = new ReferenceChain();
            chain.chain.add(ref);
            chain.chain.add(reference.ref);
            return chain;
        }

        @Override
        public int precedence() {
            return 19;
        }
    }

    public static class OrOr extends RhLhReferency {

        public OrOr() {
        }

        public OrOr(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return "||";
        }

        @Override
        public int precedence() {
            return 5;
        }
    }

    public static class AndAnd extends RhLhReferency {

        public AndAnd() {
        }

        public AndAnd(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return "&&";
        }

        @Override
        public int precedence() {
            return 6;
        }
    }

    public static class Colon extends Helper {

        public Colon() {
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return ":";
        }
    }

    public static class DirectReference extends Helper {

        public final java.lang.String ref;

        public DirectReference(java.lang.String ref) {
            this.ref = ref;
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return '.' + ref;
        }
    }

    public static class IntegerReference extends Referency {

        public Parsed lhs;
        public final long ref;

        public IntegerReference(Parsed lhs, long ref) {
            this.lhs = lhs.finish();
            this.ref = ref;
        }

        public IntegerReference(long ref) {
            this.ref = ref;
            lhs = null;
        }

        @Override
        public Parsed transform(Transformer transformer) {
            lhs = lhs.transform(transformer);
            return super.transform(transformer);
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return lhs + "[" + ref + ']';
        }

        @Override
        public boolean isStandalone() {
            return false;
        }

        @Override
        public boolean isIncomplete() {
            return false;
        }

        public Parsed finish() {
            if (lhs == null) {
                OpenArray array = new OpenArray();
                array.entries.add(new Number(ref));
                array.closed = true;
                return array;
            }
            return this;
        }

        @Override
        public int precedence() {
            return 19;
        }
    }

    public static class RightReference extends Referency {

        public final Parsed ref;
        public final List<java.lang.String> chain = new ArrayList();

        public RightReference(Parsed ref, java.lang.String key) {
            this.ref = ref;
            chain.add(key);
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder builder = new StringBuilder(ref.toString());
            if (!chain.isEmpty()) {
                builder.append('.');
                join(chain, '.', builder);
            }
            return builder.toString();
        }

        @Override
        public Referency extend(DirectReference reference) {
            chain.add(reference.ref);
            return this;
        }

        @Override
        public int precedence() {
            return 19;
        }
    }

    public static class Call extends Referency {

        public Parsed reference;
        public final List<Parsed> arguments = new ArrayList();
        Parsed currentArgumentPart;
        boolean closed;

        public Call(Parsed original, Parsed ref) {
            columns = original.columns;
            rows = original.rows;
            reference = ref;
        }
        public Call(Parsed ref) {
            this(ref, ref);
        }

        @Override
        public Parsed transform(Parsed part) {
            if (closed) {
                return super.transform(part);
            }

            if (currentArgumentPart == null) {
                if (part instanceof CloseBracket) {
                    closed = true;
                    return this;
                }
                if(part instanceof DirectReference) {
                    currentArgumentPart = new OpenArray();
                    ((OpenArray)currentArgumentPart).entries.add(new String(((DirectReference)part).ref));
                    ((OpenArray)currentArgumentPart).closed = true;
                } else if(part instanceof IntegerReference) {
                    currentArgumentPart = new OpenArray();
                    ((OpenArray)currentArgumentPart).entries.add(new Long(((IntegerReference)part).ref));
                    ((OpenArray)currentArgumentPart).closed = true;
                } else
                    currentArgumentPart = part;
            } else {
                if (!currentArgumentPart.isIncomplete()) {
                    if (part instanceof CloseBracket) {
                        arguments.add(currentArgumentPart);
                        currentArgumentPart = null;
                        closed = true;
                        return this;
                    } else if (part instanceof Comma) {
                        if (currentArgumentPart.isIncomplete()) {
                            throw new Unexpected(this, part);
                        }
                        arguments.add(currentArgumentPart);
                        currentArgumentPart = null;
                        return this;
                    }
                }
                currentArgumentPart = currentArgumentPart.transform(part);
            }

            return this;
        }

        @Override
        public boolean isStandalone() {
            return true;
        }

        @Override
        public boolean isIncomplete() {
            return !closed;
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder builder = new StringBuilder(reference.toString());
            builder.append('(');
            builder.append(join(arguments, ','));
            if (closed) {
                builder.append(')');
            }

            return builder.toString();
        }

        public Parsed finish() {
            reference = reference.finish();
            if (arguments != null) {
                ListIterator<Parsed> it = arguments.listIterator();
                while (it.hasNext()) {
                    it.set(it.next().finish());
                }
            }
            if (currentArgumentPart != null) {
                throw new Error.JavaException("SyntaxError", "Unexpected finish");
            }
            return this;
        }

        @Override
        public int precedence() {
            return 18;
        }
    }

    public static class Return extends Rh {

        public Return(Parsed rh) {
            super(rh);
        }
        public Return() {
        }

        @Override
        public java.lang.String op() {
            return "return";
        }

        @Override
        public Parsed transform(Parsed part) {
            if (rhs == null && part instanceof SemiColon) {
                throw new CompleteException();
            }

            return super.transform(part);
        }

        @Override
        public boolean isIncomplete() {
            return rhs != null && rhs.isIncomplete();
        }
    }

    public static class LambdaReturn extends Return {
        
        public final Function function;
        public LambdaReturn(Function function) {
            this.function = function;
        }

        @Override
        public Parsed transform(Parsed part) {
            if(rhs == null && part instanceof OpenGroup)
                throw new ParseFunction(function);
            
            return super.transform(part);
        }
        
    }

    public static class Delete extends Rh {

        public Delete() {
        }

        @Override
        public java.lang.String op() {
            return "delete";
        }
    }

    public static class TypeOf extends RhReferency {

        public TypeOf() {
        }

        @Override
        public java.lang.String op() {
            return "typeof";
        }

        @Override
        public int precedence() {
            return 16;
        }
    }

    public static class Yield extends Rh {

        public Yield() {
        }

        @Override
        public boolean isStandalone() {
            return true;
        }

        @Override
        public java.lang.String op() {
            return "yield";
        }
    }

    public static class Throw extends Rh {

        public Throw() {
        }

        @Override
        public java.lang.String op() {
            return "throw";
        }
    }

    public static class Break extends Helper {

        public Break() {
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return "break";
        }

        @Override
        public Parsed finish() {
            return this;
        }

        @Override
        public boolean isIncomplete() {
            return false;
        }

        @Override
        public boolean isStandalone() {
            return true;
        }
        
        
        
    }

    public static class Case extends Helper {
        
        public boolean closed;
        public Parsed ref;

        public Case() {}

        @Override
        public boolean isStandalone() {
            return true;
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return "case " + ref + ":";
        }

        @Override
        public Parsed transform(Parsed part) {
            if(ref == null) {
                if(part instanceof String || part instanceof Long || part instanceof Number || part instanceof Undefined || part instanceof Null)
                    ref = part;
                else
                    throw new Error.JavaException("SyntaxError", "Only strings and numbers supported for switch case, encountered " + describe(part));
                return this;
            } else if(part instanceof Colon) {
                closed = true;
                return this;
            }
            
            if(closed)
                throw new CompleteException(part);
            return super.transform(part);
        }

        @Override
        public boolean isIncomplete() {
            return !closed;
        }
        
        @Override
        public Parsed finish() {
            ref = ref.finish();
            return this;
        }
        
    }
    
    public static class Default extends Helper {
        
        public boolean closed;

        public Default() {}

        @Override
        public boolean isStandalone() {
            return true;
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return "default:";
        }

        @Override
        public Parsed transform(Parsed part) {
            if(part instanceof Colon) {
                closed = true;
                return this;
            }
            
            if(closed)
                throw new CompleteException(part);
            return super.transform(part);
        }

        @Override
        public boolean isIncomplete() {
            return !closed;
        }
        
        @Override
        public Parsed finish() {
            return this;
        }
        
    }

    public static class ReferenceChain extends Referency {

        public final List<java.lang.String> chain = new ArrayList();

        public ReferenceChain() {
        }

        public ReferenceChain(java.lang.String ref) {
            assert (ref != null);
            chain.add(ref);
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return join(chain, '.');
        }

        @Override
        public boolean isIncomplete() {
            return chain.isEmpty();
        }

        @Override
        public Referency extend(DirectReference reference) {
            chain.add(reference.ref);
            return this;
        }

        @Override
        public int precedence() {
            return 19;
        }
    }
    
    public static class MultiBracket extends Referency {
        
        boolean closed;
        Parsed currentPart;
        List<Parsed> parts = new ArrayList();
        public MultiBracket(Parsed part) {
            parts.add(part.finish());
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder source = new StringBuilder("(");
            for(Parsed parsed : parts) {
                if(source.length() > 1)
                    source.append(", ");
                source.append(parsed.toSimpleSource());
            }
            if(currentPart != null) {
                if(source.length() > 1)
                    source.append(", ");
                source.append(currentPart.toSimpleSource());
            }
            source.append(')');
            return source.toString();
        }
        
        @Override
        public Parsed transform(Parsed part) {
            if (closed) {
                if (part instanceof SemiColon) {
                    throw new CompleteException(part);
                }
                if (part instanceof Lambda) {
                    Function function = new Function();
                    for(Parsed _part : parts)
                        function.arguments.add(((Reference)_part).ref);
                    function.type = Function.Type.Lambda;
                    function.state = Function.State.InLambda;
                    function.storage = new LambdaReturn(function);
                    return function;
                }

                return super.transform(part);
            }

            if (currentPart == null) {
                if(part instanceof CloseBracket)
                    closed = true;
                else
                    currentPart = part;
            } else {
                if (!currentPart.isIncomplete()) {
                    if (part instanceof CloseBracket) {
                        parts.add(currentPart.finish());
                        currentPart = null;
                        closed = true;
                        return this;
                    }
                    if (part instanceof Comma) {
                        parts.add(currentPart.finish());
                        currentPart = null;
                        return this;
                    }
                }
                try {
                    currentPart = currentPart.transform(part);
                } catch (CompleteException ex) {
                    throw new Error.JavaException("SyntaxError", "CompleteException thrown when not needed: " + describe(ex.part), ex);
                }
            }

            return this;
        }

        @Override
        public boolean isIncomplete() {
            return !closed;
        }

        @Override
        public int precedence() {
            return 20;
        }
        
    }

    public static class OpenBracket extends Referency {

        Parsed contents;
        boolean closed;

        public OpenBracket() {
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder builder = new StringBuilder("(");
            builder.append(unparsedToSource(contents));
            if(closed)
                builder.append(")");
            return builder.toString();
        }

        @Override
        public Parsed transform(Parsed part) {
            if (closed) {
                if (part instanceof SemiColon) {
                    throw new CompleteException(part);
                }
                if(part instanceof Lambda) {
                    Function function = new Function();
                    if(contents != null)
                        function.arguments.add(((Reference)contents).ref);
                    function.type = Function.Type.Lambda;
                    function.state = Function.State.InLambda;
                    function.storage = new LambdaReturn(function);
                    return function;
                }

                return super.transform(part);
            }

            if (contents == null) {
                if(part instanceof CloseBracket)
                    closed = true;
                else
                    contents = part;
            } else {
                if (!contents.isIncomplete()) {
                    if (part instanceof CloseBracket) {
                        closed = true;
                        return this;
                    }
                    if (part instanceof Comma) {
                        return new MultiBracket(contents);
                    }
                }
                try {
                    contents = contents.transform(part);
                } catch (CompleteException ex) {
                    if(ex.part != null)
                        throw new Error.JavaException("SyntaxError", "CompleteException thrown when not needed: " + describe(ex.part), ex);
                }
            }

            return this;
        }

        @Override
        public boolean isStandalone() {
            return true;
        }

        @Override
        public boolean isIncomplete() {
            return !closed;
        }

        public Parsed finish() {
            if(contents != null) {
                contents = contents.finish();
                /*if ((contents instanceof Reference || contents instanceof ReferenceChain)) {
                    return contents;
                }*/
            }
            return this;
        }

        @Override
        public boolean isNumber() {
            return contents.isNumber();
        }

        @Override
        public boolean isString() {
            return contents.isString();
        }

        @Override
        public boolean isBoolean() {
            return contents.isBoolean(); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public java.lang.String primaryType() {
            return contents.primaryType();
        }

        @Override
        public int precedence() {
            return 20;
        }

    }

    public static abstract class Block extends Parsed {

        enum State {
            BeforeCondition,
            InCondition,
            AfterCondition,
            InSimpleImpl,
            InImpl,
            Complete
        }
        State state;
        public Parsed condition;
        public Parsed simpleimpl;
        public ScriptData impl;

        public Block(State state) {
            this.state = state;
        }

        public Block() {
            state = State.BeforeCondition;
        }

        @Override
        public Parsed transform(Transformer transformer) {
            if(simpleimpl == null)
                for(int i=0; i<impl.impl.length; i++)
                    impl.impl[i] = impl.impl[i].transform(transformer);
            else
                simpleimpl = simpleimpl.transform(transformer);
            if(condition != null)
                condition = condition.transform(transformer);
            return super.transform(transformer);
        }
        
        public void complete() {
            state = Block.State.Complete;
        }

        @Override
        public Parsed transform(Parsed part) {
            switch (state) {
                case BeforeCondition:
                    if (part instanceof OpenBracket) {
                        state = State.InCondition;
                        return this;
                    }
                    break;
                case InCondition:
                    transformCondition(part);
                    return this;
                case AfterCondition:
                    if (part instanceof OpenGroup) {
                        return parse();
                    } else if (part instanceof If && this instanceof Else) {
                        return new ElseIf();
                    } else if (allowSimpleImpl()) {
                        if(part instanceof SemiColon) {
                            state = State.Complete;
                            throw new CompleteException();
                        }
                        simpleimpl = part;
                        state = State.InSimpleImpl;
                        return this;
                    }
                    break;
                case InImpl:
                    return transformImpl(part);
                case InSimpleImpl:
                    if (!simpleimpl.isIncomplete()) {
                        if (part instanceof SemiColon) {
                            state = State.Complete;
                            simpleimpl = simpleimpl.finish();
                            return this;
                        }
                    }
                    try {
                        simpleimpl = simpleimpl.transform(part);
                    } catch(CompleteException ex) {
                        if (!simpleimpl.isIncomplete()) {
                            state = State.Complete;
                            simpleimpl = simpleimpl.finish();
                            return complete(part);
                        }
                    }
                    return this;
                case Complete:
                    return complete(part);
            }

            throw new Unexpected(this, part);
        }

        public void transformCondition(Parsed part) {
            if (condition == null) {
                condition = part;
            } else {
                if (!condition.isIncomplete()) {
                    if (part instanceof CloseBracket) {
                        condition = condition.finish();
                        finishCondition();
                        return;
                    }
                }
                try {
                    condition = condition.transform(part);
                } catch (CompleteException ex) {
                    throw new Unexpected(this, part);
                }
            }
        }

        public void finishCondition() {
            state = State.AfterCondition;
        }

        public Parsed transformImpl(Parsed part) {
            throw new UnsupportedOperationException();
        }

        public Parsed parse() {
            throw new ParseBlock(this);
        }

        public Parsed complete(Parsed part) {
            state = State.Complete;
            throw new CompleteException(part);
        }

        @Override
        public boolean isIncomplete() {
            return state != State.Complete;
        }

        @Override
        public boolean isStandalone() {
            return true;
        }

        public boolean allowSimpleImpl() {
            return true;
        }

        public Parsed finish() {
            if (state == State.InSimpleImpl) {
                state = State.Complete;
                simpleimpl = simpleimpl.finish();
            }
            return this;
        }
    }

    public static class While extends Block {

        public While() {
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder builder = new StringBuilder("while (");
            builder.append(condition.toSource());
            builder.append(") {");
            if (simpleimpl != null) {
                builder.append(simpleimpl);
            } else if (impl == null) {
                builder.append("<unparsed>");
            } else {
                joinToSource(Arrays.asList(impl.impl), ';', builder);
            }
            builder.append("}");
            return builder.toString();
        }
    }
    
    public static class Do extends Block {
        enum State {
            BeforeImpl,
            InImpl,
            BeforeWhile,
            BeforeCondition,
            InCondition,
            Complete
        }
        
        public State state = State.BeforeImpl;

        public Do() {
        }

        @Override
        public void complete() {
            state = State.BeforeWhile;
        }

        @Override
        public boolean isIncomplete() {
            return state != State.Complete;
        }

        @Override
        public Parsed transform(Parsed part) {
            switch(state) {
                case BeforeImpl:
                    if(part instanceof OpenGroup)
                        return parse();
                    break;
                case BeforeWhile:
                    if(part instanceof While) {
                        state = State.BeforeCondition;
                        return this;
                    }
                    break;
                case BeforeCondition:
                    if(part instanceof OpenBracket) {
                        state = State.InCondition;
                        return this;
                    }
                    break;
                case InCondition:
                    transformCondition(part);
                    return this;
                case Complete:
                    throw new CompleteException(part);
            }

            throw new Unexpected(this, part);
        }

        @Override
        public void finishCondition() {
            state =  State.Complete;
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder builder = new StringBuilder("do {");
            if (simpleimpl != null) {
                builder.append(simpleimpl);
            } else if (impl == null) {
                builder.append("<unparsed>");
            } else {
                joinToSource(Arrays.asList(impl.impl), ';', builder);
            }
            builder.append("} while(");
            builder.append(condition);
            builder.append(")");
            return builder.toString();
        }
    }

    public static class Try extends Block {

        public Catch c;
        public Finally f;

        public Try() {
            super(State.AfterCondition);
        }

        @Override
        public boolean allowSimpleImpl() {
            return false;
        }

        @Override
        public Parsed transform(Parsed part) {
            if(part instanceof Call) {
                if(((Reference)((Call)part).reference).ref.equals("catch"))
                    return transform(new Catch(((Call)part).arguments.get(0)));
            }
                    
            return super.transform(part); //To change body of generated methods, choose Tools | Templates.
        }
        
        

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder builder = new StringBuilder("try {");
            if (impl == null) {
                builder.append("<unparsed>");
            } else {
                joinToSource(Arrays.asList(impl.impl), ';', builder);
            }
            builder.append("}");
            if (c != null) {
                builder.append(' ');
                builder.append(c);
            }
            if (f != null) {
                builder.append(' ');
                builder.append(f);
            }
            return builder.toString();
        }

        @Override
        public Parsed complete(Parsed part) {
            if (f != null) {
                if (f.isIncomplete()) {
                    f = (Finally) f.transform(part);
                } else {
                    throw new CompleteException(part);
                }
            } else if (c != null) {
                if (c.isIncomplete()) {
                    c = (Catch) c.transform(part);
                } else if (part instanceof Finally) {
                    if (f != null) {
                        f = (Finally) f.transform(part);
                    } else if (part instanceof Finally) {
                        f = (Finally) part;
                    } else {
                        throw new CompleteException(part);
                    }
                } else {
                    throw new CompleteException(part);
                }
            } else if (part instanceof Catch) {
                c = (Catch) part;
            } else if (part instanceof Finally) {
                f = (Finally) part;
            } else {
                return super.complete(part);
            }

            return this;
        }
    }

    public static class Catch extends Block {

        public Catch() {
            super(Block.State.BeforeCondition);
        }
        public Catch(Parsed condition) {
            super(Block.State.AfterCondition);
            this.condition = condition;
        }

        @Override
        public boolean allowSimpleImpl() {
            return false;
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder builder = new StringBuilder("catch (");
            builder.append(condition.toSource());
            builder.append(") {");
            if (impl == null) {
                builder.append("<unparsed>");
            } else {
                joinToSource(Arrays.asList(impl.impl), ';', builder);
            }
            builder.append("}");
            return builder.toString();
        }

        @Override
        public boolean isStandalone() {
            return false;
        }

    }

    public static class Switch extends Block {

        public static class Case {
        }

        public Switch() {
        }

        @Override
        public Parsed parse() {
            throw new ParseSwitch(this);
        }

        @Override
        public boolean allowSimpleImpl() {
            return false;
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder builder = new StringBuilder("switch (");
            builder.append(condition.toSource());
            builder.append(") {");
            if (impl == null) {
                builder.append("<unparsed>");
            } else {
                joinToSource(Arrays.asList(impl.impl), ';', builder);
            }
            builder.append("}");
            return builder.toString();
        }

        @Override
        public boolean isStandalone() {
            return true;
        }

    }

    public static class Boolean extends Referency {

        public final java.lang.Boolean value;

        public Boolean(java.lang.Boolean value) {
            this.value = value;
        }

        @Override
        public boolean isTrue() {
            return value;
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return java.lang.String.valueOf(value);
        }

        @Override
        public Referency extend(DirectReference reference) {
            throw new Unexpected(this, reference);
        }

        @Override
        public Referency extend(IntegerReference reference) {
            throw new Unexpected(this, reference);
        }

        @Override
        public java.lang.String primaryType() {
            return "boolean";
        }

        @Override
        public boolean isBoolean() {
            return true;
        }

        @Override
        public int precedence() {
            return -1;
        }
    }

    public static class Finally extends Block {

        public Finally() {
            super(Block.State.AfterCondition);
        }

        @Override
        public boolean allowSimpleImpl() {
            return false;
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder builder = new StringBuilder("finally {");
            if (impl == null) {
                builder.append("<unparsed>");
            } else {
                joinToSource(Arrays.asList(impl.impl), ';', builder);
            }
            builder.append("}");
            return builder.toString();
        }

        @Override
        public boolean isStandalone() {
            return false;
        }
    }

    public static class For extends Block {

        static enum ForConditionState {
            DuringInit,
            DuringCondition,
            DuringLoop,
            AfterInOf
        }

        static enum ForType {
            Standard,
            InLoop,
            OfLoop
        }

        public Parsed init, storage;
        public ForType type = ForType.Standard;
        public List<Parsed> loop = new ArrayList();
        ForConditionState state = ForConditionState.DuringInit;

        public For() {
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder builder = new StringBuilder("for (");
            switch(type) {
                case InLoop:
                    builder.append(unparsedToSource(init));
                    builder.append(" in ");
                    builder.append(unparsedToSource(condition));
                    break;
                case OfLoop:
                    builder.append(unparsedToSource(init));
                    builder.append(" of ");
                    builder.append(unparsedToSource(condition));
                    break;
                case Standard:
                    builder.append(unparsedToSource(init));
                    builder.append("; ");
                    builder.append(unparsedToSource(condition));
                    for(Parsed l : loop) {
                        builder.append("; ");
                        builder.append(unparsedToSource(l));
                    }
                    break;
            }
            builder.append(") {");
            if (simpleimpl != null) {
                builder.append(simpleimpl.toSource());
            } else if (impl == null) {
                builder.append("<unparsed>");
            } else {
                joinToSource(Arrays.asList(impl.impl), ';', builder);
            }
            builder.append("}");
            return builder.toString();
        }

        @Override
        public void transformCondition(Parsed part) {
            switch (state) {
                case DuringInit:
                    if (init == null) {
                        if (part instanceof SemiColon) {
                            state = ForConditionState.DuringCondition;
                            return;
                        }
                        if(part instanceof Var)
                            init = part;
                        else
                            init = new Var().transform(part);
                    } else {
                        if (!init.isIncomplete()) {
                            if (part instanceof SemiColon) {
                                init = init.finish();
                                state = ForConditionState.DuringCondition;
                                return;
                            }
                            if (part instanceof In) {
                                type = ForType.InLoop;
                                init = init.transform(new Set());
                                state = ForConditionState.AfterInOf;
                                return;
                            }
                            if (part instanceof Of) {
                                type = ForType.OfLoop;
                                init = init.transform(new Set());
                                state = ForConditionState.AfterInOf;
                                return;
                            }
                        }
                        try {
                            init = init.transform(part);
                        } catch (CompleteException ex) {
                            condition = condition.finish();
                            state = ForConditionState.DuringCondition;
                        }
                    }
                    break;
                case DuringCondition:
                    if (condition == null) {
                        if (part instanceof SemiColon) {
                            state = ForConditionState.DuringLoop;
                            return;
                        }
                        condition = part;
                    } else {
                        if (!condition.isIncomplete()) {
                            if (part instanceof SemiColon) {
                                condition = condition.finish();
                                state = ForConditionState.DuringLoop;
                                return;
                            }
                        }
                        try {
                            condition = condition.transform(part);
                        } catch (CompleteException ex) {
                            condition = condition.finish();
                            state = ForConditionState.DuringLoop;
                        }
                    }
                    break;
                case DuringLoop:
                    if (storage == null) {
                        if (part instanceof CloseBracket) {
                            finishCondition();
                            return;
                        }
                        storage = part;
                    } else {
                        if (!storage.isIncomplete()) {
                            if (part instanceof CloseBracket) {
                                loop.add(storage.finish());
                                storage = null;
                                finishCondition();
                                return;
                            }
                            if (part instanceof Comma) {
                                loop.add(storage.finish());
                                storage = null;
                                return;
                            }
                        }
                        try {
                            storage = storage.transform(part);
                        } catch (CompleteException ex) {
                            storage = storage.finish();
                        }
                    }
                    break;
                case AfterInOf:
                    if (!init.isIncomplete()) {
                        if (part instanceof CloseBracket) {
                            init = init.finish();
                            finishCondition();
                            return;
                        }
                    }
                    try {
                        init = init.transform(part);
                    } catch (CompleteException ex) {
                        throw new Unexpected(this, part);
                    }
                    break;
                    
            }

            return;
        }
    }

    public static class If extends Block {

        public Else el;

        public If() {
        }

        @Override
        public Parsed transform(Transformer transformer) {
            if(el != null)
                el = (Else)el.transform(transformer);
            return super.transform(transformer);
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder builder = new StringBuilder("if (");
            builder.append(condition);
            builder.append(") {");
            if (simpleimpl != null) {
                builder.append(simpleimpl);
            } else if (impl == null) {
                builder.append("<unparsed>");
            } else if(simple) {
                builder.append("...");
            } else {
                joinToSource(Arrays.asList(impl.impl), ';', builder);
            }
            builder.append("}");
            if (el != null) {
                builder.append(' ');
                builder.append(el.toSource(simple));
            }
            return builder.toString();
        }

        @Override
        public Parsed complete(Parsed part) {
            if (el != null) {
                el = (Else) el.transform(part);
            } else if (part instanceof Else) {
                el = (Else) part;
            } else {
                return super.complete(part);
            }

            return this;
        }
    }

    public static class ElseIf extends Else {

        public Else el;

        public ElseIf() {
            super(State.BeforeCondition);
        }

        @Override
        public Parsed transform(Transformer transformer) {
            if(el != null)
                el = (Else)el.transform(transformer);
            return super.transform(transformer);
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder builder = new StringBuilder("else if (");
            builder.append(condition);
            builder.append(") {");
            if (simpleimpl != null) {
                builder.append(simpleimpl);
            } else if (impl == null) {
                builder.append("<unparsed>");
            } else if(simple) {
                builder.append("...");
            } else {
                joinToSource(Arrays.asList(impl.impl), ';', builder);
            }
            builder.append("}");
            if (el != null) {
                builder.append(' ');
                builder.append(el);
            }
            return builder.toString();
        }

        @Override
        public Parsed complete(Parsed part) {
            if (el != null) {
                el = (Else) el.transform(part);
            } else if (part instanceof Else) {
                el = (Else) part;
            } else {
                return super.complete(part);
            }

            return this;
        }
    }

    public static class Else extends Block {

        public Else() {
            super(State.AfterCondition);
        }

        protected Else(State state) {
            super(state);
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder builder = new StringBuilder("else {");
            if (simpleimpl != null) {
                builder.append(simpleimpl);
            } else if (impl == null) {
                builder.append("<unparsed>");
            } else if(simple) {
                builder.append("...");
            } else {
                joinToSource(Arrays.asList(impl.impl), ';', builder);
            }
            builder.append("}");
            return builder.toString();
        }

        @Override
        public boolean isStandalone() {
            return false;
        }
    }

    public static abstract class Helper extends Parsed {

        @Override
        public Parsed transform(Parsed part) {
            if(part instanceof SemiColon)
                throw new CompleteException();
            throw new Unexpected(this, part);
        }

        @Override
        public boolean isStandalone() {
            return false;
        }

        @Override
        public boolean isIncomplete() {
            return true;
        }

        @Override
        public Parsed finish() {
            return null;
        }
    }

    public static class CloseBracket extends Helper {

        public CloseBracket() {
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return ")";
        }
    }

    public static class OpenGroup extends Referency {

        public static enum State {
            NeedKey,
            HaveKey,
            ReadingValue,
            Complete
        }

        State state = State.NeedKey;
        Map<java.lang.String, Parsed> entries = new HashMap();
        java.lang.String currentEntryKey;
        Parsed currentEntry;

        public OpenGroup() {
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder builder = new StringBuilder("{");

            Iterator<Map.Entry<java.lang.String, Parsed>> it = entries.entrySet().iterator();
            if (it.hasNext()) {
                Map.Entry<java.lang.String, Parsed> entry = it.next();
                builder.append(entry.getKey());
                builder.append(':');
                builder.append(entry.getValue());
                while (it.hasNext()) {
                    builder.append(',');
                    entry = it.next();
                    builder.append(entry.getKey());
                    builder.append(':');
                    builder.append(entry.getValue());
                }
            }

            if (state == State.Complete) {
                builder.append('}');
            }
            return builder.toString();
        }

        @Override
        public Parsed transform(Parsed part) {
            if (state == State.Complete) {
                return super.transform(part);
            }

            switch (state) {
                case NeedKey:
                    if (part instanceof CloseGroup) {
                        state = State.Complete;
                        return this;
                    }
                    if (part instanceof Reference) {
                        currentEntryKey = ((Reference) part).ref;
                    } else if (part instanceof String) {
                        currentEntryKey = ((String) part).string;
                    } else if (part instanceof Number) {
                        currentEntryKey = net.nexustools.njs.Number.toString(((Number) part).value);
                    } else {
                        throw new Unexpected(this, part);
                    }

                    state = State.HaveKey;
                    return this;

                case HaveKey:
                    if (part instanceof Colon) {
                        state = State.ReadingValue;
                        return this;
                    }
                    if (part instanceof Comma) {
                        entries.put(currentEntryKey, new Reference(currentEntryKey));
                        state = State.NeedKey;
                        return this;
                    }
                    if (part instanceof CloseGroup) {
                        entries.put(currentEntryKey, new Reference(currentEntryKey));
                        state = State.Complete;
                        return this;
                    }
                    break;

                case ReadingValue:
                    if (currentEntry == null) {
                        currentEntry = part;
                    } else {
                        if (!currentEntry.isIncomplete()) {
                            if (part instanceof CloseGroup) {
                                entries.put(currentEntryKey, currentEntry);
                                currentEntry = null;
                                state = State.Complete;
                                return this;
                            }
                            if (part instanceof Comma) {
                                entries.put(currentEntryKey, currentEntry);
                                currentEntry = null;
                                state = State.NeedKey;
                                return this;
                            }
                        }
                        currentEntry = currentEntry.transform(part);
                    }
                    return this;

            }

            throw new Unexpected(this, part);
        }

        @Override
        public boolean isStandalone() {
            return true;
        }

        @Override
        public boolean isIncomplete() {
            return state != State.Complete;
        }

        public Parsed finish() {
            if (state == State.Complete) {
                for (Map.Entry<java.lang.String, Parsed> entry : entries.entrySet()) {
                    Parsed rhs = entry.getValue();
                    if (rhs instanceof Function) {
                        ((Function) rhs).name = entry.getKey();
                    }
                }
            }

            return this;
        }

        @Override
        public int precedence() {
            return -1;
        }
    }

    public static class CloseGroup extends Helper {

        public CloseGroup() {
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return "}";
        }
    }

    public static class OpenArray extends Referency {

        boolean closed;
        Parsed currentEntry;
        public List<Parsed> entries = new ArrayList();

        public OpenArray(List<Parsed> entries) {
            this.entries.addAll(entries);
            closed = true;
        }
        public OpenArray() {
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder builder = new StringBuilder("[");
            join(entries, ',', builder);
            if (closed) {
                builder.append(']');
            }
            return builder.toString();
        }

        @Override
        public Parsed transform(Parsed part) {
            if (closed) {
                return super.transform(part);
            }

            if (currentEntry == null) {
                if (part instanceof CloseArray) {
                    closed = true;
                } else {
                    currentEntry = part;
                }
                return this;
            } else {
                if (!currentEntry.isIncomplete()) {
                    if (part instanceof Comma || part instanceof CloseArray) {
                        currentEntry = currentEntry.finish();
                        if (currentEntry != null) {
                            if (currentEntry.isIncomplete()) {
                                throw new Error.JavaException("SyntaxError", "Expected more after " + describe(currentEntry) + " at " + currentEntry.rows + ":" + currentEntry.columns);
                            }
                            entries.add(currentEntry);
                            currentEntry = null;
                        }
                        if (part instanceof CloseArray) {
                            closed = true;
                        }
                        return this;
                    }
                }

                currentEntry = currentEntry.transform(part);
                return this;
            }
        }

        @Override
        public boolean isStandalone() {
            return true;
        }

        @Override
        public boolean isIncomplete() {
            return !closed;
        }

        public Parsed finish() {
            return this;
        }

        @Override
        public java.lang.String primaryType() {
            return "array";
        }

        @Override
        public int precedence() {
            return -1;
        }
    }

    public static class CloseArray extends Helper {

        public CloseArray() {
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return "]";
        }
    }

    public static class Comma extends Helper {

        public Comma() {
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return ",";
        }
    }

    public static class Null extends VolatileReferency {

        public Null() {
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return "null";
        }

        @Override
        public int precedence() {
            return -1;
        }
    }

    public static class Undefined extends VolatileReferency {

        public Undefined() {
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return "undefined";
        }

        @Override
        public int precedence() {
            return -1;
        }
    }
    
    public static class ClassMethod extends Function {
        public enum Type {
            Normal,
            Static,
            Symbol,
            Constructor,
            Getter,
            Setter
        }
        Parsed symbolRef;
        final Class clazz;
        Type type = Type.Normal;
        public ClassMethod(Class clazz) {
            state = State.InArguments;
            this.clazz = clazz;
        }

        @Override
        public void complete() {
            super.complete();
            clazz.complete();
        }
        
    }

    public static class Class extends Referency {
        private enum State {
            BeforeName,
            AfterName,
            AfterExtends,
            AfterExtender,
            InBody,
            InSymbol,
            GetterStart,
            SetterStart,
            BeforeMethodArgs,
            InMethod,
            Complete
        }
        
        private State state = State.BeforeName;
        ClassMethod currentClassMethod = new ClassMethod(this);
        List<ClassMethod> methods = new ArrayList();
        java.lang.String name, _extends;
        
        public void complete() {
            state = Class.State.InBody;
            methods.add(currentClassMethod);
            currentClassMethod = new ClassMethod(this);
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder builder = new StringBuilder("class ");
            builder.append(name);
            if(_extends != null) {
                builder.append(" extends ");
                builder.append(_extends);
            }
            builder.append(" {\n");
            builder.append("}");
            return builder.toString();
        }

        @Override
        public int precedence() {
            return -1;
        }

        @Override
        public boolean isIncomplete() {
            return state != State.Complete;
        }

        @Override
        public Parsed transform(Parsed part) {
            switch(state) {
                case BeforeName:
                    if(part instanceof Reference) {
                        name = ((Reference)part).ref;
                        state = State.AfterName;
                        return this;
                    }
                    break;
                case AfterExtends:
                    if(part instanceof Reference) {
                        _extends = ((Reference)part).ref;
                        state = State.AfterExtender;
                        return this;
                    }
                    break;
                case AfterName:
                    if(part instanceof ExtendsKeyword) {
                        state = State.AfterExtends;
                        return this;
                    }
                case AfterExtender:
                    if(part instanceof OpenGroup) {
                        state = State.InBody;
                        return this;
                    }
                    break;
                case InBody:
                    if(part instanceof Getter) {
                        currentClassMethod.type = ClassMethod.Type.Getter;
                        return this;
                    }
                    if(part instanceof Setter) {
                        currentClassMethod.type = ClassMethod.Type.Setter;
                        return this;
                    }
                    if(part instanceof Constructor) {
                        currentClassMethod.type = ClassMethod.Type.Constructor;
                        state = State.BeforeMethodArgs;
                        return this;
                    }
                    if(part instanceof Reference) {
                        currentClassMethod.name = ((Reference) part).ref;
                        state = State.BeforeMethodArgs;
                        return this;
                    }
                    if(part instanceof CloseGroup) {
                        state = State.Complete;
                        return this;
                    }
                    if(part instanceof OpenArray) {
                        state = State.InSymbol;
                        currentClassMethod.type = ClassMethod.Type.Symbol;
                        return this;
                    }
                    break;
                case InSymbol:
                    if(currentClassMethod.symbolRef == null) {
                        currentClassMethod.symbolRef = part;
                        return this;
                    } else if(!(currentClassMethod.symbolRef.isIncomplete())) {
                        if(part instanceof CloseArray) {
                            currentClassMethod.symbolRef = currentClassMethod.symbolRef.finish();
                            state = State.BeforeMethodArgs;
                            return this;
                        }
                    }
                    
                    currentClassMethod.symbolRef = currentClassMethod.symbolRef.transform(part);
                    return this;
                case BeforeMethodArgs:
                    if(part instanceof OpenBracket) {
                        state = State.InMethod;
                        return this;
                    }
                    break;
                case InMethod:
                    currentClassMethod.transform(part);
                    return this;
                case Complete:
                    if(part instanceof SemiColon)
                        throw new CompleteException();
            }
            
            if(state == State.Complete)
                return super.transform(part);
            throw new Unexpected(this, part);
        }
        
        
        
    }
    
    public static class ClassKeyword extends Reference {
        
        public ClassKeyword(java.lang.String ref) {
            super(ref);
        }
        
    }

    public static class Super extends ClassKeyword {
        public Super() {
            super("super");
        }
    }

    public static class ExtendsKeyword extends ClassKeyword {
        public ExtendsKeyword() {
            super("extends");
        }
    }

    public static class Constructor extends ClassKeyword {
        public Constructor() {
            super("constructor");
        }
    }

    public static class Getter extends ClassKeyword {
        public Getter() {
            super("get");
        }
    }

    public static class Setter extends ClassKeyword {
        public Setter() {
            super("set");
        }
    }

    public static class Extends extends Referency {

        @Override
        public java.lang.String toSource(boolean simple) {
            return "extends";
        }

        @Override
        public int precedence() {
            return -1;
        }
        
    }

    public static class Function extends Referency {
        
        public void complete() {
            state = Function.State.Complete;
        }

        @Override
        public int precedence() {
            return -1;
        }

        public static enum Type {
            Standard,
            Generator,
            Lambda
        }
        public static enum State {
            BeforeName,
            BeforeArguments,
            InArguments,
            WaitingForVarArg,
            HasVarArg,
            BeforeBody,
            InBody,
            Complete,
            InLambda
        }

        Type type;
        Parsed call;
        Parsed storage;
        java.lang.String name;
        public java.lang.String vararg;
        public List<java.lang.String> arguments = new ArrayList();
        State state = State.BeforeName;
        java.lang.String source;
        public ScriptData impl;

        public Function() {
        }
        
        public boolean isLambda() {
            return type == Type.Lambda;
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder builder;
            if(type == Type.Lambda) {
                builder = new StringBuilder("(");
                join(arguments, ',', builder);
                builder.append(") => ");
                if (impl == null) {
                    if(storage == null)
                        builder.append("<unparsed>");
                    else
                        builder.append(storage);
                } else {
                    joinToSource(Arrays.asList(impl.impl), ';', builder);
                }
            } else {
                builder = new StringBuilder("function");
                if (type == Type.Generator) {
                    builder.append('*');
                }
                if (name != null) {
                    builder.append(' ');
                    builder.append(name);
                }
                builder.append('(');
                join(arguments, ',', builder);
                builder.append("){");
                if (impl == null) {
                    builder.append("<unparsed>");
                } else {
                    joinToSource(Arrays.asList(impl.impl), ';', builder);
                }
                builder.append("}");
            }
            return builder.toString();
        }

        @Override
        public java.lang.String toSimpleSource() {
            StringBuilder builder;
            if(type == Type.Lambda) {
                builder = new StringBuilder("(");
                join(arguments, ',', builder);
                builder.append(") => ");
                if (impl == null) {
                    builder.append("<unparsed>");
                } else {
                    builder.append("...");
                }
            } else {
                builder = new StringBuilder("function");
                if (type == Type.Generator) {
                    builder.append('*');
                }
                if (name != null) {
                    builder.append(' ');
                    builder.append(name);
                }
                builder.append('(');
                join(arguments, ',', builder);
                builder.append("){");
                if (impl == null) {
                    builder.append("<unparsed>");
                } else {
                    builder.append("...");
                }
                builder.append("}");
            }
            return builder.toString();
        }

        @Override
        public Parsed transform(Parsed part) {
            switch (state) {
                case BeforeName:
                    if (part instanceof Multiply) {
                        type = Type.Generator;
                        return this;
                    }
                    if (part instanceof Reference) {
                        name = ((Reference) part).ref;
                        state = State.BeforeArguments;
                        return this;
                    }
                case BeforeArguments:
                    if (part instanceof OpenBracket) {
                        state = State.InArguments;
                        return this;
                    }
                    break;
                    
                case WaitingForVarArg:
                    vararg = ((Reference) part).ref;
                    state = State.HasVarArg;
                    return this;
                    
                case HasVarArg:
                    if (part instanceof CloseBracket) {
                        state = State.BeforeBody;
                        return this;
                    }
                    break;

                case InArguments:
                    if (storage == null) {
                        if(part instanceof VarArgs) {
                            state = State.WaitingForVarArg;
                            return this;
                        }
                        if (part instanceof CloseBracket) {
                            state = State.BeforeBody;
                            return this;
                        }
                        storage = part;
                    } else {
                        if (part instanceof CloseBracket) {
                            arguments.add(((Reference) storage).ref);
                            storage = null;
                            state = State.BeforeBody;
                            return this;
                        }
                        if (part instanceof Comma) {
                            arguments.add(((Reference) storage).ref);
                            storage = null;
                            return this;
                        }
                    }
                    return this;

                case BeforeBody:
                    if (part instanceof OpenGroup) {
                        state = State.InBody;
                        throw new ParseFunction(this);
                    }
                    break;

                case Complete:
                    return super.transform(part);
                    
                case InLambda:
                    if(!storage.isIncomplete()) {
                        if (part instanceof SemiColon) {
                            System.out.println(describe(storage));
                            throw new CompleteException();
                        }
                    }
                    storage = storage.transform(part);
                    return this;
            }

            throw new Unexpected(this, part);
        }

        @Override
        public Parsed transformFallback(Parsed part) {
            throw new CompleteException(part);
        }

        @Override
        public boolean isStandalone() {
            return true;
        }

        @Override
        public boolean isIncomplete() {
            if(state == State.Complete)
                return false;
            if(state == State.InLambda)
                return storage == null || storage.isIncomplete();
            return true;
        }

        @Override
        public Parsed finish() {
            if(state == State.InLambda) {
                storage = storage.finish();
                impl = new ScriptData(new Parsed[]{storage}, toSource(), storage.rows, storage.columns);
                impl.methodName = "<lambda>";
                impl.callee = this;
                storage = null;
                
                state = State.Complete;
            }
            return this;
        }
    }

    public static class SemiColon extends Helper {

        public SemiColon() {
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return ";";
        }
    }

    public static class NewLine extends Parsed {

        public NewLine() {
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return "\\n";
        }

        @Override
        public Parsed transform(Parsed part) {
            return part;
        }

        @Override
        public boolean isStandalone() {
            return true;
        }

        @Override
        public boolean isIncomplete() {
            return false;
        }

        public Parsed finish() {
            return null;
        }
    }

    public static class SetPlaceholder extends Parsed {

        public SetPlaceholder() {
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return "<>";
        }

        @Override
        public Parsed transform(Parsed part) {
            return part;
        }

        @Override
        public boolean isStandalone() {
            return false;
        }

        @Override
        public boolean isIncomplete() {
            return true;
        }

        public Parsed finish() {
            throw new Unexpected(this, this);
        }
    }

    public static abstract class Rh extends Parsed {

        public Parsed rhs;

        public Rh(Parsed rhs) {
            this.rhs = rhs.finish();
        }

        public Rh() {
        }

        @Override
        public Parsed transform(Transformer transformer) {
            if(rhs != null)
                rhs = rhs.transform(transformer);
            return super.transform(transformer);
        }

        public abstract java.lang.String op();

        @Override
        public Parsed transform(Parsed part) {
            if (rhs == null) {
                if (!part.isStandalone()) {
                    throw new Unexpected(this, part);
                }
                rhs = part;
                return this;
            }

            rhs = rhs.transform(part);
            return this;
        }

        @Override
        public boolean isStandalone() {
            return true;
        }

        @Override
        public boolean isIncomplete() {
            return rhs == null || rhs.isIncomplete();
        }

        @Override
        public Parsed finish() {
            if (rhs != null) {
                rhs = rhs.finish();
            }
            return this;
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return op() + toLPadString(rhs);
        }
    }

    public static abstract class RhLh extends Rh {

        public Parsed lhs;

        public RhLh() {
            lhs = null;
        }

        public RhLh(Parsed lhs) {
            if(lhs != null) {
                rows = lhs.rows;
                columns = lhs.columns;
                this.lhs = lhs.finish();
            } else
                this.lhs = null;
        }

        public RhLh(Parsed lhs, Parsed rhs) {
            super(rhs);
            rows = lhs.rows;
            columns = lhs.columns;
            this.lhs = lhs.finish();
        }

        @Override
        public Parsed transform(Transformer transformer) {
            if(lhs != null)
                lhs = lhs.transform(transformer);
            return super.transform(transformer);
        }
        
        public boolean requireLHS() {
            return true;
        }

        @Override
        public Parsed finish() {
            if (lhs == null && requireLHS()) {
                throw new net.nexustools.njs.Error.JavaException("SyntaxError", "Missing Left-Hand-Side (" + getClass().getSimpleName() + ") at " + rows + ":" + columns);
            }
            return super.finish();
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return toRPadString(lhs) + op() + toLPadString(rhs);
        }
    }

    public static abstract class RhLhReferency extends RhLh implements BaseReferency {
        public RhLhReferency() {
        }

        public RhLhReferency(Parsed lhs) {
            super(lhs);
        }

        public RhLhReferency(Parsed lhs, Parsed rhs) {
            super(lhs, rhs);
        }

        @Override
        public Parsed transform(Parsed part) {
            if (rhs == null) {
                if (part instanceof IntegerReference) {
                    rhs = new OpenArray();
                    ((OpenArray) rhs).entries.add(new Long(((IntegerReference) part).ref));
                    ((OpenArray) rhs).closed = true;
                } else if(part instanceof DirectReference) {
                    rhs = new OpenArray();
                    ((OpenArray) rhs).entries.add(new String(((DirectReference) part).ref));
                    ((OpenArray) rhs).closed = true;
                } else {
                    if (!part.isStandalone()) {
                        throw new Unexpected(this, part);
                    }
                    rhs = part;
                }
                return this;
            }

            if (rhs.isIncomplete()) {
                rhs = rhs.transform(part);
                return this;
            }

            if (part instanceof OpenBracket) {
                rhs = rhs.transform(part);
                return this;
            } else if (part instanceof OpenArray) {
                rhs = rhs.transform(part);
                return this;
            } else if (part instanceof DirectReference) {
                rhs = rhs.transform(part);
                return this;
            } else if (part instanceof IntegerReference) {
                rhs = rhs.transform(part);
                return this;
            }

            boolean useRhs = rhs instanceof Rh && rhs instanceof BaseReferency;
            int precedence = useRhs ? ((BaseReferency) rhs).precedence() : precedence();

            if (part instanceof InstanceOf) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new InstanceOf(rhs);
                    return this;
                }
                return new InstanceOf(this);
            } else if (part instanceof ShiftLeft) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new ShiftLeft(rhs);
                    return this;
                }
                return new ShiftLeft(this);
            } else if (part instanceof ShiftRight) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new ShiftRight(rhs);
                    return this;
                }
                return new ShiftRight(this);
            } else if (part instanceof DoubleShiftRightEq) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new DoubleShiftRightEq(rhs);
                    return this;
                }
                return new DoubleShiftRightEq(this);
            } else if (part instanceof DoubleShiftRight) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new DoubleShiftRight(rhs);
                    return this;
                }
                return new DoubleShiftRight(this);
            } else if (part instanceof MoreThan) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new MoreThan(rhs);
                    return this;
                }
                return new MoreThan(this);
            } else if (part instanceof LessThan) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new LessThan(rhs);
                    return this;
                }
                return new LessThan(this);
            } else if (part instanceof MoreEqual) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new MoreEqual(rhs);
                    return this;
                }
                return new MoreEqual(this);
            } else if (part instanceof LessEqual) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new LessEqual(rhs);
                    return this;
                }
                return new LessEqual(this);
            } else if (part instanceof StrictEquals) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new StrictEquals(rhs);
                    return this;
                }
                return new StrictEquals(this);
            } else if (part instanceof NotEquals) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new NotEquals(rhs);
                    return this;
                }
                return new NotEquals(this);
            } else if (part instanceof StrictNotEquals) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new StrictNotEquals(rhs);
                    return this;
                }
                return new StrictNotEquals(this);
            } else if (part instanceof Equals) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new Equals(rhs);
                    return this;
                }
                return new Equals(this);
            } else if (part instanceof Multiply) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new Multiply(rhs);
                    return this;
                }
                return new Multiply(this);
            } else if (part instanceof Divide) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new Divide(rhs);
                    return this;
                }
                return new Divide(this);
            } else if (part instanceof Plus) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new Plus(rhs);
                    return this;
                }
                return new Plus(this);
            } else if (part instanceof Minus) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new Minus(rhs);
                    return this;
                }
                return new Minus(this);
            } else if (part instanceof Set) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new Set(rhs);
                    return this;
                }
                return new Set(this);
            } else if (part instanceof Percent) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new Percent(rhs);
                    return this;
                }
                return new Percent(this);
            } else if (part instanceof And) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new And(rhs);
                    return this;
                }
                return new And(this);
            } else if (part instanceof Or) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new Or(rhs);
                    return this;
                }
                return new Or(this);
            } else if (part instanceof AndAnd) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new AndAnd(rhs);
                    return this;
                }
                return new AndAnd(this);
            } else if (part instanceof OrOr) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new OrOr(rhs);
                    return this;
                }
                return new OrOr(this);
            } else if (part instanceof Number && ((Number) part).value < 0) {
                return new Minus(this, new Number(-((Number) part).value));
            } else if (part instanceof Long && ((Long) part).value < 0) {
                return new Minus(this, new Number(-((Long) part).value));
            } else if (part instanceof Fork) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new Fork(rhs);
                    return this;
                }
                return new Fork(this);
            } else {
                return transformFallback(part);
            }
        }

        public Parsed transformFallback(Parsed part) {
            rhs = rhs.transform(part);
            return this;
        }

    }

    public static abstract class RhReferency extends Rh implements BaseReferency {

        public RhReferency() {
        }

        public RhReferency(Parsed rhs) {
            super(rhs);
        }

        @Override
        public Parsed transform(Parsed part) {
            if (rhs == null) {
                if (part instanceof IntegerReference) {
                    rhs = new OpenArray();
                    ((OpenArray) rhs).entries.add(new Long(((IntegerReference) part).ref));
                    ((OpenArray) rhs).closed = true;
                } else {
                    if (!part.isStandalone()) {
                        throw new Unexpected(this, part);
                    }
                    rhs = part;
                }
                return this;
            }

            if (rhs.isIncomplete()) {
                rhs = rhs.transform(part);
                return this;
            }

            if (part instanceof OpenBracket) {
                rhs = rhs.transform(part);
                return this;
            } else if (part instanceof OpenArray) {
                rhs = rhs.transform(part);
                return this;
            } else if (part instanceof DirectReference) {
                rhs = rhs.transform(part);
                return this;
            } else if (part instanceof IntegerReference) {
                rhs = rhs.transform(part);
                return this;
            }

            if (part instanceof SemiColon) {
                throw new CompleteException();
            }

            boolean useRhs = rhs instanceof Rh && rhs instanceof BaseReferency;
            int precedence = useRhs ? ((BaseReferency) rhs).precedence() : precedence();

            if (part instanceof InstanceOf) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new InstanceOf(rhs);
                    return this;
                }
                return new InstanceOf(this);
            } else if (part instanceof MoreThan) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new MoreThan(rhs);
                    return this;
                }
                return new MoreThan(this);
            } else if (part instanceof LessThan) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new LessThan(rhs);
                    return this;
                }
                return new LessThan(this);
            } else if (part instanceof MoreEqual) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new MoreEqual(rhs);
                    return this;
                }
                return new MoreEqual(this);
            } else if (part instanceof LessEqual) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new LessEqual(rhs);
                    return this;
                }
                return new LessEqual(this);
            } else if (part instanceof StrictEquals) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new StrictEquals(rhs);
                    return this;
                }
                return new StrictEquals(this);
            } else if (part instanceof NotEquals) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new NotEquals(rhs);
                    return this;
                }
                return new NotEquals(this);
            } else if (part instanceof StrictNotEquals) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new StrictNotEquals(rhs);
                    return this;
                }
                return new StrictNotEquals(this);
            } else if (part instanceof Equals) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new Equals(rhs);
                    return this;
                }
                return new Equals(this);
            } else if (part instanceof Multiply) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new Multiply(rhs);
                    return this;
                }
                return new Multiply(this);
            } else if (part instanceof Divide) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new Divide(rhs);
                    return this;
                }
                return new Divide(this);
            } else if (part instanceof Plus) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new Plus(rhs);
                    return this;
                }
                return new Plus(this);
            } else if (part instanceof Minus) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new Minus(rhs);
                    return this;
                }
                return new Minus(this);
            } else if (part instanceof Set) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new Set(rhs);
                    return this;
                }
                return new Set(this);
            } else if (part instanceof Percent) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new Percent(rhs);
                    return this;
                }
                return new Percent(this);
            } else if (part instanceof And) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new And(rhs);
                    return this;
                }
                return new And(this);
            } else if (part instanceof Or) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new Or(rhs);
                    return this;
                }
                return new Or(this);
            } else if (part instanceof AndAnd) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new AndAnd(rhs);
                    return this;
                }
                return new AndAnd(this);
            } else if (part instanceof OrOr) {
                if (precedence < ((BaseReferency) part).precedence()) {
                    rhs = rhs.transform(part);
                    return this;
                }
                if (useRhs) {
                    rhs = new OrOr(rhs);
                    return this;
                }
                return new OrOr(this);
            } else if (part instanceof Number && ((Number) part).value < 0) {
                return new Minus(this, new Number(-((Number) part).value));
            } else if (part instanceof Long && ((Long) part).value < 0) {
                return new Minus(this, new Number(-((Long) part).value));
            } else {
                return transformFallback(part);
            }
        }

        public Parsed transformFallback(Parsed part) {

            throw new Unexpected(this, part);
        }

    }

    public static class Not extends Rh {

        @Override
        public java.lang.String op() {
            return "!";
        }

    }

    public static class Set extends RhLhReferency {

        public Set() {
        }

        public Set(Parsed lhs) {
            super(lhs);
        }

        public Set(Parsed lhs, Parsed rhs) {
            super(lhs, rhs);
        }

        @Override
        public java.lang.String op() {
            return "=";
        }

        @Override
        public Parsed finish() {
            Parsed myself = super.finish();
            if (rhs instanceof Function && ((Function) rhs).name == null) {
                ((Function) rhs).name = lhs.toSource();
            }
            return myself;
        }

        @Override
        public int precedence() {
            return 3;
        }
    }

    public static class MultiplyEq extends RhLhReferency {

        public MultiplyEq() {
        }

        public MultiplyEq(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return "*=";
        }

        @Override
        public int precedence() {
            return 3;
        }
    }
    
    public static class DivideEq extends RhLhReferency {

        public DivideEq() {
        }

        public DivideEq(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return "/=";
        }

        @Override
        public int precedence() {
            return 3;
        }
    }

    public static class Multiply extends RhLhReferency implements NumberReferency {

        public Multiply() {
        }

        public Multiply(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return "*";
        }

        @Override
        public int precedence() {
            return 14;
        }
    }

    public static class Divide extends RhLhReferency implements NumberReferency {

        public Divide() {
        }

        public Divide(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return "/";
        }

        @Override
        public int precedence() {
            return 14;
        }
    }

    public static class DoubleShiftRight extends RhLhReferency implements NumberReferency {

        public DoubleShiftRight() {
        }

        public DoubleShiftRight(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return ">>>";
        }

        @Override
        public int precedence() {
            return 12;
        }
    }
    
    public static class DoubleShiftRightEq extends RhLhReferency implements NumberReferency {

        public DoubleShiftRightEq() {
        }

        public DoubleShiftRightEq(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return ">>>=";
        }

        @Override
        public int precedence() {
            return 12;
        }
    }

    public static class ShiftLeft extends RhLhReferency implements NumberReferency {

        public ShiftLeft() {
        }

        public ShiftLeft(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return "<<";
        }

        @Override
        public int precedence() {
            return 12;
        }
    }

    public static class ShiftRight extends RhLhReferency implements NumberReferency {

        public ShiftRight() {
        }

        public ShiftRight(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return ">>";
        }

        @Override
        public int precedence() {
            return 12;
        }
    }

    public static class Equals extends RhLhReferency {

        public Equals() {
        }

        public Equals(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return "==";
        }

        @Override
        public int precedence() {
            return 10;
        }
    }

    public static class NotEquals extends RhLhReferency {

        public NotEquals() {
        }

        public NotEquals(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return "!=";
        }

        @Override
        public int precedence() {
            return 10;
        }
    }

    public static class StrictNotEquals extends RhLhReferency {

        public StrictNotEquals() {
        }

        public StrictNotEquals(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return "!==";
        }

        @Override
        public int precedence() {
            return 10;
        }
    }

    public static class StrictEquals extends RhLhReferency {

        public StrictEquals() {
        }

        public StrictEquals(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return "===";
        }

        @Override
        public int precedence() {
            return 10;
        }
    }

    public static class Percent extends RhLhReferency implements NumberReferency {

        public Percent() {
            super();
        }

        public Percent(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return "%";
        }

        @Override
        public int precedence() {
            return 14;
        }
    }

    public static class Or extends RhLhReferency {

        public Or() {
            super();
        }

        public Or(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return "|";
        }

        @Override
        public int precedence() {
            return 7;
        }

        @Override
        public boolean isNumber() {
            return true;
        }

        @Override
        public java.lang.String primaryType() {
            return "number";
        }

    }

    public static class OrEq extends RhLhReferency {

        public OrEq() {}
        public OrEq(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return "|=";
        }

        @Override
        public int precedence() {
            return 7;
        }

        @Override
        public boolean isNumber() {
            return true;
        }

        @Override
        public java.lang.String primaryType() {
            return "number";
        }

    }

    public static class AndEq extends RhLhReferency {

        public AndEq() {}
        public AndEq(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return "&=";
        }

        @Override
        public int precedence() {
            return 9;
        }

        @Override
        public boolean isNumber() {
            return true;
        }

        @Override
        public java.lang.String primaryType() {
            return "number";
        }

    }

    public static class And extends RhLhReferency {

        public And() {
            super();
        }

        public And(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return "&";
        }

        @Override
        public int precedence() {
            return 9;
        }

        @Override
        public boolean isNumber() {
            return true;
        }

        @Override
        public java.lang.String primaryType() {
            return "number";
        }
    }

    public static class Plus extends RhLhReferency {

        public Plus() {
            super(null);
        }

        public Plus(Parsed lhs) {
            super(lhs);
        }

        @Override
        public boolean requireLHS() {
            return false;
        }

        @Override
        public java.lang.String op() {
            return "+";
        }

        public boolean isStringReferenceChain() {
            Parsed l = rhs;
            while (l instanceof Plus) {
                l = ((Plus) l).rhs;
            }
            if (l instanceof StringReferency) {
                return true;
            }
            l = lhs;
            while (l instanceof Plus) {
                l = ((Plus) l).lhs;
            }
            return l instanceof StringReferency;
        }

        @Override
        public boolean isString() {
            return lhs.isString() || rhs.isString();
        }

        @Override
        public boolean isNumber() {
            return lhs == null || (!lhs.isString() && !rhs.isString());
        }

        @Override
        public int precedence() {
            return 13;
        }

    }

    public static class Minus extends RhLhReferency implements NumberReferency {

        public Minus() {
            super(new Number(0));
        }

        public Minus(Parsed lhs) {
            super(lhs);
        }

        public Minus(Parsed lhs, Parsed rhs) {
            super(lhs, rhs);
        }

        @Override
        public java.lang.String op() {
            return "-";
        }

        @Override
        public int precedence() {
            return 13;
        }

        @Override
        public boolean isNumber() {
            return true;
        }

        @Override
        public java.lang.String primaryType() {
            return "number";
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            if(lhs.toString().equals("0.0"))
                return "-" + (rhs == null ? "" : rhs.toSimpleSource());
            return super.toSource(simple);
        }
    }

    public static class InstanceOf extends RhLhReferency {

        public InstanceOf() {
        }

        public InstanceOf(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return "instanceof";
        }

        @Override
        public int precedence() {
            return 11;
        }
    }

    public static class PlusEq extends RhLhReferency {

        public PlusEq() {
        }

        public PlusEq(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return "+=";
        }

        @Override
        public int precedence() {
            return 3;
        }

        @Override
        public boolean isNumber() {
            return true;
        }

        @Override
        public java.lang.String primaryType() {
            return "number";
        }
    }
    
    public static class MinusEq extends RhLhReferency {

        public MinusEq() {
        }

        public MinusEq(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return "-=";
        }

        @Override
        public int precedence() {
            return 3;
        }

        @Override
        public boolean isNumber() {
            return true;
        }

        @Override
        public java.lang.String primaryType() {
            return "number";
        }
    }

    public static class PlusPlus extends Referency implements NumberReferency {

        Parsed ref;
        boolean right;

        public PlusPlus() {
        }

        public PlusPlus(Parsed ref) {
            this.ref = ref;
            rows = ref.rows;
            columns = ref.columns;
            right = true;
        }

        @Override
        public Parsed transform(Transformer transformer) {
            ref = ref.transform(transformer);
            return super.transform(transformer);
        }

        @Override
        public Parsed transform(Parsed part) {
            if (right) {
                return super.transform(part);
            }

            if (ref == null) {
                ref = part;
                return this;
            } else if (ref.isIncomplete()) {
                ref = ref.transform(part);
                return this;
            }

            return super.transform(part);
        }

        @Override
        public Parsed transformFallback(Parsed part) {
            if (right) {
                throw new CompleteException(part);
            }
            if (ref == null) {
                ref = part;
            } else {
                ref = ref.transform(part);
            }
            return this;
        }

        @Override
        public boolean isIncomplete() {
            return ref == null || ref.isIncomplete();
        }

        @Override
        public Parsed finish() {
            if (!right) {
                ref = ref.finish();
            }
            return this;
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder builder = new StringBuilder();
            if (right) {
                builder.append(unparsedToSource(ref));
            }
            builder.append("++");
            if (!right) {
                builder.append(unparsedToSource(ref));
            }
            return builder.toString();
        }

        @Override
        public boolean isNumber() {
            return true;
        }

        @Override
        public java.lang.String primaryType() {
            return "number";
        }

        @Override
        public int precedence() {
            return right ? 17 : 16;
        }

    }

    public static class MinusMinus extends Referency implements NumberReferency {

        Parsed ref;
        boolean right;

        public MinusMinus() {
        }

        public MinusMinus(Parsed ref) {
            this.ref = ref;
            rows = ref.rows;
            columns = ref.columns;
            right = true;
        }

        @Override
        public Parsed transform(Transformer transformer) {
            ref = ref.transform(transformer);
            return super.transform(transformer);
        }

        @Override
        public Parsed transform(Parsed part) {
            if (right) {
                return super.transform(part);
            }

            if (ref == null) {
                ref = part;
                return this;
            } else if (ref.isIncomplete()) {
                ref = ref.transform(part);
                return this;
            }

            return super.transform(part);
        }

        @Override
        public Parsed transformFallback(Parsed part) {
            if (right) {
                throw new CompleteException(part);
            }
            if (ref == null) {
                ref = part;
            } else {
                ref = ref.transform(part);
            }
            return this;
        }

        @Override
        public boolean isIncomplete() {
            return ref == null || ref.isIncomplete();
        }

        @Override
        public Parsed finish() {
            if (!right) {
                ref = ref.finish();
            }
            return this;
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder builder = new StringBuilder();
            if (right) {
                builder.append(unparsedToSource(ref));
            }
            builder.append("--");
            if (!right) {
                builder.append(unparsedToSource(ref));
            }
            return builder.toString();
        }

        @Override
        public int precedence() {
            return right ? 17 : 16;
        }

        @Override
        public boolean isNumber() {
            return true;
        }

        @Override
        public java.lang.String primaryType() {
            return "number";
        }

    }

    public static class MoreThan extends RhLhReferency {

        public MoreThan() {
        }

        public MoreThan(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return ">";
        }

        @Override
        public int precedence() {
            return 11;
        }

        @Override
        public java.lang.String primaryType() {
            return "boolean";
        }

        @Override
        public boolean isBoolean() {
            return true;
        }

    }

    public static class LessThan extends RhLhReferency {

        public LessThan() {
        }

        public LessThan(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return "<";
        }

        @Override
        public int precedence() {
            return 11;
        }

        @Override
        public java.lang.String primaryType() {
            return "boolean";
        }

        @Override
        public boolean isBoolean() {
            return true;
        }
    }

    public static class MoreEqual extends RhLhReferency {

        public MoreEqual() {
        }

        public MoreEqual(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return ">=";
        }

        @Override
        public int precedence() {
            return 11;
        }

        @Override
        public java.lang.String primaryType() {
            return "boolean";
        }

        @Override
        public boolean isBoolean() {
            return true;
        }
    }

    public static class LessEqual extends RhLhReferency {

        public LessEqual() {
        }

        public LessEqual(Parsed lhs) {
            super(lhs);
        }

        @Override
        public java.lang.String op() {
            return "<=";
        }

        @Override
        public int precedence() {
            return 11;
        }

        @Override
        public java.lang.String primaryType() {
            return "boolean";
        }

        @Override
        public boolean isBoolean() {
            return true;
        }
    }

    public static class In extends RhLhReferency {
        
        public In() {}
        public In(Parsed lhs) {
            super(lhs);
        }

        @Override
        public int precedence() {
            return 11;
        }

        @Override
        public java.lang.String primaryType() {
            return "boolean";
        }

        @Override
        public boolean isBoolean() {
            return true;
        }

        @Override
        public java.lang.String op() {
            return "in";
        }
    }

    public static class Of extends Helper implements BaseReferency {

        @Override
        public java.lang.String toSource(boolean simple) {
            return "of";
        }

        @Override
        public int precedence() {
            return 11;
        }

        @Override
        public Parsed transformFallback(Parsed part) {
            throw new UnsupportedOperationException("Not supported");
        }
    }

    public static class New extends Referency {

        boolean closed;
        Parsed reference;
        List<Parsed> arguments;
        Parsed currentPart;

        public New() {
        }

        @Override
        public Parsed transform(Parsed part) {
            if (closed) {
                return super.transform(part);
            }

            if (reference == null) {
                reference = part;
            } else if (reference.isIncomplete()) {
                reference = reference.transform(part);
            } else if (arguments != null) {
                if (currentPart == null) {
                    if (part instanceof CloseBracket) {
                        closed = true;
                        return this;
                    }
                    currentPart = part;
                } else {
                    if (!currentPart.isIncomplete()) {
                        if (part instanceof CloseBracket) {
                            arguments.add(currentPart);
                            currentPart = null;
                            closed = true;
                            return this;
                        }
                        if (part instanceof Comma) {
                            arguments.add(currentPart);
                            currentPart = null;
                            return this;
                        }
                    }
                    currentPart = currentPart.transform(part);
                }
            } else if (part instanceof OpenBracket) {
                arguments = new ArrayList();
            } else {
                reference = reference.transform(part);
            }
            return this;
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder buffer = new StringBuilder("new ");
            buffer.append(reference);
            if (arguments != null) {
                buffer.append('(');
                join(arguments, ',', buffer);
                if (closed) {
                    buffer.append(')');
                }
            }
            return buffer.toString();
        }

        @Override
        public boolean isStandalone() {
            return true;
        }

        @Override
        public boolean isIncomplete() {
            return reference == null || reference.isIncomplete() || (!closed && arguments != null);
        }

        @Override
        public Parsed finish() {
            reference = reference.finish();
            if (arguments != null) {
                ListIterator<Parsed> it = arguments.listIterator();
                while (it.hasNext()) {
                    it.set(it.next().finish());
                }
            }
            if (currentPart != null) {
                throw new Error.JavaException("SyntaxError", "Unexpected finish");
            }
            return this;
        }

        @Override
        public int precedence() {
            return arguments == null || arguments.isEmpty() ? 18 : 19;
        }
    }

    public static class Fork extends Referency {
        public enum State {
            None,
            InSuccess,
            InFailure,
            Complete
        }
        
        Parsed condition, success, failure;
        State state;
        public Fork() {
            state = State.None;
        }
        public Fork(Parsed condition) {
            this.condition = condition;
            state = State.InSuccess;
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return condition + " ? " + success + " : " + failure;
        }

        @Override
        public Parsed transform(Parsed part) {
            switch(state) {
                case InSuccess:
                    if(success == null)
                        success = part;
                    else {
                        if(part instanceof Colon)
                            state = State.InFailure;
                        else
                            success = success.transform(part);
                    }
                    break;
                case InFailure:
                    if(failure == null)
                        failure = part;
                    else {
                        if(part instanceof SemiColon)
                            state = State.Complete;
                        else
                            try {
                                // TODO: Handle precedence on myself and children here...
                                failure = failure.transform(part);
                            } catch(CompleteException ex) {
                                state = State.Complete;
                            }
                    }
                    break;
                default:
                    return super.transform(part);
            }
            return this;
        }

        @Override
        public boolean isIncomplete() {
            return !(state == State.Complete || (state == State.InFailure && failure != null && !failure.isIncomplete()));
        }

        @Override
        public Parsed finish() {
            condition = condition.finish();
            success = success.finish();
            failure = failure.finish();
            return super.finish();
        }
        
        

        @Override
        public int precedence() {
            return 4;
        }
    }

    public static class RegEx extends Referency {

        final java.lang.String pattern, flags;

        public RegEx(java.lang.String pattern, java.lang.String flags) {
            this.pattern = pattern;
            this.flags = flags;
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return '/' + pattern + '/' + flags;
        }

        @Override
        public int precedence() {
            return -1;
        }

    }
    
    // TODO: Better error handling...
    public static class NameSet extends Parsed {
        private boolean closed;
        public final boolean array;
        private java.lang.String currentKey;
        private java.lang.String currentValue;
        public final Map<java.lang.String, java.lang.String> names = new java.util.LinkedHashMap();
        
        public NameSet(boolean array) {
            this.array = array;
        }

        @Override
        public Parsed transform(Parsed part) {
            if(part instanceof Reference) {
                if(currentKey == null)
                    currentKey = ((Reference)part).ref;
                else if(currentValue == null && !array)
                    currentValue = ((Reference)part).ref;
            } else if(part instanceof Colon)
                return this;
            else if(part instanceof Comma) {
                names.put(currentKey, currentValue == null ? currentKey : currentValue);
                currentKey = currentValue = null;
            } else if(part instanceof CloseArray && array) {
                names.put(currentKey, currentValue == null ? currentKey : currentValue);
                closed = true;
            } else if(part instanceof CloseGroup && !array) {
                names.put(currentKey, currentValue == null ? currentKey : currentValue);
                closed = true;
            } else {
                throw new Unexpected(this, part);
            }
            return this;
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder builder = new StringBuilder(array ? "[ " : "{ ");
            for(Map.Entry<java.lang.String, java.lang.String> name : names.entrySet()) {
                if(builder.length() > 2)
                    builder.append(", ");
                
                java.lang.String key = name.getKey();
                builder.append(key);
                if(!array) {
                    java.lang.String val = name.getValue();
                    if(!key.equals(val)) {
                        builder.append(": ");
                        builder.append(val);
                    }
                }
            }
            builder.append(array ? " ]" : " }");
            return builder.toString();
        }

        @Override
        public boolean isStandalone() {
            return true;
        }

        @Override
        public boolean isIncomplete() {
            return !closed;
        }

        @Override
        public Parsed finish() {
            return this;
        }
        
    }

    public static class Var extends Parsed {

        public static class Set {

            public Parsed lhs;
            public Parsed rhs;

            private Set(boolean array) {
                lhs = new NameSet(array);
            }
            private Set(java.lang.String ref) {
                lhs = new Reference(ref);
            }

            @Override
            public java.lang.String toString() {
                if (rhs != null) {
                    return lhs + " = " + rhs.toSource();
                }
                return lhs.toString();
            }
        }
        Set currentSet;
        public List<Set> sets = new ArrayList();

        public Var() {
        }

        @Override
        public Parsed transform(Transformer transformer) {
            for(Set set : sets) {
                set.lhs = set.lhs.transform(transformer);
                if(set.rhs != null)
                    set.rhs = set.rhs.transform(transformer);
            }
            return super.transform(transformer);
        }

        @Override
        public Parsed transform(Parsed part) {
            if (currentSet == null) {
                if (part instanceof Reference) {
                    currentSet = new Set(((Reference) part).ref);
                    return this;
                } else if(part instanceof OpenGroup) {
                    currentSet = new Set(false);
                    return this;
                } else if(part instanceof OpenArray) {
                    currentSet = new Set(true);
                    return this;
                }
            } else if (currentSet.lhs.isIncomplete()) {
                currentSet.lhs = currentSet.lhs.transform(part);
                return this;
            } else if (currentSet.rhs != null) {
                if (!currentSet.rhs.isIncomplete()) {
                    if (part instanceof Comma) {
                        sets.add(currentSet);
                        currentSet = null;
                        return this;
                    } else if (part instanceof SemiColon) {
                        sets.add(currentSet);
                        currentSet = null;
                        throw new CompleteException();
                    }
                }

                currentSet.rhs = currentSet.rhs.transform(part);
                return this;
            } else if (part instanceof RegexCompiler.Set) {
                currentSet.rhs = new SetPlaceholder();
                return this;
            } else if (part instanceof Comma) {
                sets.add(currentSet);
                currentSet = null;
                return this;
            } else if (part instanceof SemiColon) {
                throw new CompleteException();
            }

            throw new Unexpected(this, part);
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder builder = new StringBuilder();
            builder.append(getClass().getSimpleName().toLowerCase());
            builder.append(' ');
            join(sets, ',', builder);
            if (currentSet != null) {
                if (!sets.isEmpty()) {
                    builder.append(", ");
                }
                builder.append(currentSet.lhs);
                if (currentSet.rhs != null) {
                    builder.append(" = ");
                    builder.append(currentSet.rhs);
                }
            }
            return builder.toString();
        }

        @Override
        public boolean isStandalone() {
            return true;
        }

        @Override
        public boolean isIncomplete() {
            return (currentSet == null && sets.isEmpty()) || (currentSet != null && ((currentSet.rhs != null && currentSet.rhs.isIncomplete()) || (currentSet.lhs == null || currentSet.lhs.isIncomplete())));
        }

        @Override
        public Parsed finish() {
            if (currentSet != null) {
                if (currentSet.rhs != null && currentSet.rhs.isIncomplete()) {
                    return this;
                }

                sets.add(currentSet);
                currentSet = null;
            }
            for (Set set : sets) {
                if (set.rhs != null) {
                    set.rhs = set.rhs.finish();
                    if (set.rhs instanceof Function && ((Function) set.rhs).name == null && set.lhs instanceof Reference) {
                        ((Function) set.rhs).name = ((Reference)set.lhs).ref;
                    }
                }
            }
            return this;
        }
    }

    public static class Let extends Var {
    }
    
    public static class VarArgs extends Helper {

        @Override
        public java.lang.String toSource(boolean simple) {
            return "...";
        }
        
    }
    
    public static class Lambda extends Helper {

        @Override
        public java.lang.String toSource(boolean simple) {
            return "=>";
        }
        
    }

    public static class String extends PrimitiveReferency implements StringReferency {

        private static final Pattern HEX_ESCAPED = Pattern.compile("\\\\x([a-fA-F0-9]{2})");
        public final java.lang.String string;
        
        public static java.lang.String decode(java.lang.String string) {
            string = string.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\");
            
            Matcher matcher;
            while((matcher = HEX_ESCAPED.matcher(string)).find()) {
                string = string.substring(0, matcher.regionStart())
                        + (char)(int)java.lang.Integer.valueOf(matcher.group(1), 16)
                        + string.substring(matcher.regionEnd());
            }
            
            return string;
        }

        public String(java.lang.String string) {
            assert (string != null);
            this.string = decode(string);
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return '"' + convertStringSource(string) + '"';
        }

        @Override
        public boolean isNumber() {
            try {
                if (Double.isNaN(Double.valueOf(string))) {
                    throw new NumberFormatException();
                }
                return true;
            } catch (NumberFormatException ex) {
                return false;
            }
        }

        @Override
        public int precedence() {
            return -1;
        }

    }
    
    public static class TemplateLiteral extends PrimitiveReferency implements StringReferency {
        private static java.lang.String decode(java.lang.String string) {
            return String.decode(string.replace("\\`", "`"));
        }
        public final Object[] parts;

        public TemplateLiteral(java.lang.String string, RegexParser parser) {
            assert (string != null);
            int pos = 0, index;
            
            List data = new ArrayList();
            while((index = string.indexOf("${", pos)) > -1) {
                if(index > pos)
                    data.add(decode(string.substring(pos, index)));
                
                int end = string.indexOf("}", index);
                if(end == -1)
                    break;
                
                try {
                    ScriptData parsed = parser.parse(new ParserReader(new StringReader(string.substring(index+2, end))));
                    assert(parsed.impl.length == 1);
                    data.add(parsed.impl[0].finish());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                pos = end+1;
            }
            if(pos < string.length())
                data.add(decode(string.substring(pos)));
            
            parts = data.toArray();
        }

        @Override
        public Parsed transform(Parsed part) {
            if (part instanceof In)
                return new In(this);
            
            return super.transform(part);
        }
        
        @Override
        public java.lang.String toSource(boolean simple) {
            StringBuilder builder = new StringBuilder("`");
            for(java.lang.Object part : parts)
                if(part instanceof Parsed) {
                    builder.append("${");
                    builder.append(part);
                    builder.append("}");
                } else
                    builder.append(part);
            builder.append("`");
            return builder.toString();
        }

        @Override
        public boolean isNumber() {
            return false;
        }

        @Override
        public int precedence() {
            return -1;
        }

    }

    public static class Long extends PrimitiveReferency implements NumberReferency {

        public final long value;

        public Long(long value) {
            this.value = value;
        }

        @Override
        public boolean isTrue() {
            return value != 0;
        }
        
        @Override
        public java.lang.String toSource(boolean simple) {
            return java.lang.String.valueOf(value);
        }

        @Override
        public int precedence() {
            return -1;
        }
    }

    public static class Number extends PrimitiveReferency implements NumberReferency {

        public final double value;

        public Number(double value) {
            this.value = value;
        }

        @Override
        public boolean isTrue() {
            return value != 0;
        }

        @Override
        public java.lang.String toSource(boolean simple) {
            return java.lang.String.valueOf(value);
        }

        @Override
        public Referency extend(DirectReference reference) {
            throw new Unexpected(this, reference);
        }

        @Override
        public Referency extend(IntegerReference reference) {
            throw new Unexpected(this, reference);
        }

        @Override
        public int precedence() {
            return -1;
        }
    }

    public static final java.lang.String NUMBER_REG = "\\-?\\d+(\\.\\d+)?([eE][\\+\\-]?\\d+)?";
    public static final java.lang.String STRING_REG = "(([\"'])(?:(?:\\\\\\\\)|\\\\\\2|(?!\\\\\\2)\\\\|(?!\\2).|[\\n\\r])*\\2)";
    public static final java.lang.String VARIABLE_NAME = "[_$a-zA-Z\\xA0-\\uFFFF][_$a-zA-Z0-9\\xA0-\\uFFFF]*";
    public static final Pattern MULTILINE_COMMENT = Pattern.compile("^(\\/\\*(?:(?!\\*\\/).|[\\n\\r])*\\*\\/)");
    public static final Pattern SINGLELINE_COMMENT = Pattern.compile("^(\\/\\/[^\\n\\r]*([\\n\\r]+|$))");
    public static final Pattern REGEX = Pattern.compile("^/((|\\\\/|[^/\\n])*)/([gi]*)");
    public static final Pattern STRING = Pattern.compile("^" + STRING_REG);
    public static final Pattern TEMPLATE_LITERAL = Pattern.compile("^`((\\\\`|[^`])+)`");
    public static final Pattern VARARGS = Pattern.compile("^\\.\\.\\.");
    public static final Pattern LAMBDA = Pattern.compile("^=>");
    public static final Pattern NUMBERGET = Pattern.compile("^\\[(" + NUMBER_REG + ")\\]");
    public static final Pattern STRINGGET = Pattern.compile("^\\[" + STRING_REG + "\\]");
    public static final Pattern VAR = Pattern.compile("^var\\s+(" + VARIABLE_NAME + ")(\\s*,\\s*" + VARIABLE_NAME + ")*");
    public static final Pattern HEX = Pattern.compile("^-?0x([0-9a-fA-F]+)");
    public static final Pattern OCT = Pattern.compile("^-?0[oO]?([0-9]+)");
    public static final Pattern NUMBER = Pattern.compile("^" + NUMBER_REG);
    public static final Pattern VARIABLEGET = Pattern.compile("^\\.\\s*(" + VARIABLE_NAME + ')');
    public static final Pattern VARIABLE = Pattern.compile("^" + VARIABLE_NAME);
    public static final Pattern CLOSE_BRACKET = Pattern.compile("^\\)");
    public static final Pattern OPEN_BRACKET = Pattern.compile("^\\(");
    public static final Pattern WHITESPACE = Pattern.compile("^\\s+");
    public static final Pattern MULTIPLYEQ = Pattern.compile("^\\*=");
    public static final Pattern DIVIDEEQ = Pattern.compile("^/=");
    public static final Pattern CLOSE_GROUP = Pattern.compile("^\\}");
    public static final Pattern OPEN_GROUP = Pattern.compile("^\\{");
    public static final Pattern CLOSE_ARRAY = Pattern.compile("^\\]");
    public static final Pattern OPEN_ARRAY = Pattern.compile("^\\[");
    public static final Pattern STRICTEQUALS = Pattern.compile("^===");
    public static final Pattern NOTSTRICTEQUALS = Pattern.compile("^!==");
    public static final Pattern PLUSPLUS = Pattern.compile("^\\+\\+");
    public static final Pattern MINUSMINUS = Pattern.compile("^\\-\\-");
    public static final Pattern MULTIPLY = Pattern.compile("^\\*");
    public static final Pattern DIVIDE = Pattern.compile("^/");
    public static final Pattern MINUS = Pattern.compile("^\\-");
    public static final Pattern MINUSEQ = Pattern.compile("^\\-=");
    public static final Pattern MORETHAN = Pattern.compile("^\\>");
    public static final Pattern LESSTHAN = Pattern.compile("^\\<");
    public static final Pattern MOREEQUAL = Pattern.compile("^\\>=");
    public static final Pattern LESSEQUAL = Pattern.compile("^\\<=");
    public static final Pattern SHIFTRIGHT = Pattern.compile("^\\>\\>");
    public static final Pattern SHIFTLEFT = Pattern.compile("^\\<\\<");
    public static final Pattern SHIFTRIGHTEQ = Pattern.compile("^\\>\\>=");
    public static final Pattern SHIFTLEFTEQ = Pattern.compile("^\\<\\<=");
    public static final Pattern DBLSHIFTRIGHT = Pattern.compile("^\\>\\>\\>");
    public static final Pattern DBLSHIFTRIGHTEQ = Pattern.compile("^\\>\\>\\>=");
    public static final Pattern FORKSTART = Pattern.compile("^\\?");
    public static final Pattern PLUSEQ = Pattern.compile("^\\+=");
    public static final Pattern SEMICOLON = Pattern.compile("^;");
    public static final Pattern NOTEQUALS = Pattern.compile("^!=");
    public static final Pattern EQUALS = Pattern.compile("^==");
    public static final Pattern NEWLINE = Pattern.compile("^(\r\n|\r|\n)");
    public static final Pattern ACCESS = Pattern.compile("^\\[");
    public static final Pattern PERCENT = Pattern.compile("^%");
    public static final Pattern OR = Pattern.compile("^\\|");
    public static final Pattern AND = Pattern.compile("^&");
    public static final Pattern OREQUAL = Pattern.compile("^\\|=");
    public static final Pattern ANDEQUAL = Pattern.compile("^&=");
    public static final Pattern OROR = Pattern.compile("^\\|\\|");
    public static final Pattern ANDAND = Pattern.compile("^&&");
    public static final Pattern NOT = Pattern.compile("^!");
    public static final Pattern COMMA = Pattern.compile("^,");
    public static final Pattern COLON = Pattern.compile("^:");
    public static final Pattern PLUS = Pattern.compile("^\\+");
    public static final Pattern SET = Pattern.compile("^=");

    public static class ParserReader {

        public final Reader reader;
        private java.lang.String currentBuffer = "";
        final char[] buffer = new char[8096];

        public ParserReader(Reader reader) {
            this.reader = reader;
        }

        public java.lang.String current() throws IOException {
            if (currentBuffer.isEmpty()) 
                return more();
            if(currentBuffer.length() < 4048)
                more();
            return currentBuffer;
        }

        public java.lang.String more() throws IOException {
            if (currentBuffer.isEmpty()) {
                int read = reader.read(buffer);
                if (read > 0) {
                    return currentBuffer = new java.lang.String(buffer, 0, read);
                }
                return null;
            }

            int untilFull = buffer.length - currentBuffer.length();
            if (untilFull == 0) {
                return null;
            }

            int read = reader.read(buffer, 0, untilFull);
            if (read > 0) {
                return currentBuffer += new java.lang.String(buffer, 0, read);
            }
            return null;
        }

        private int countInstances(java.lang.String in, char of) {
            int pos = -1, count = 0;
            while ((pos = in.indexOf(of, pos + 1)) > -1) {
                count++;
            }
            return count;
        }

        private void processChopped(java.lang.String chop) {
            int pos = chop.indexOf('\n');
            if (pos > -1) {
                do {
                    rows++;
                    chop = chop.substring(pos + 1);
                } while ((pos = chop.indexOf('\n')) > -1);
                columns = 1 + chop.length();
            } else {
                columns += chop.length();
            }
        }

        public java.lang.String ltrim(int len) {
            if (currentBuffer.length() <= len) {
                try {
                    processChopped(currentBuffer);
                    return currentBuffer;
                } finally {
                    currentBuffer = "";
                }
            } else {
                try {
                    java.lang.String chop = currentBuffer.substring(0, len);
                    processChopped(chop);
                    return chop;
                } finally {
                    currentBuffer = currentBuffer.substring(len);
                }
            }
        }

        private int rows = 1, columns = 1;

        public int rows() {
            return rows;
        }

        public int columns() {
            return columns;
        }
    }

    public static abstract class RegexParser {

        public static class PartExchange extends RuntimeException {

            final int trim;
            final Parsed part;

            public PartExchange(Parsed part, int trim) {
                assert (part != null);
                this.part = part;
                this.trim = trim;
            }
        }

        public static class PartComplete extends RuntimeException {

            final int trim;

            public PartComplete(int trim) {
                this.trim = trim;
            }
        }

        private final Pattern[] patterns;

        public RegexParser(Pattern... patterns) {
            this.patterns = patterns;
        }

        @Override
        public java.lang.String toString() {
            java.lang.String toString = super.toString();
            return toString.substring(toString.lastIndexOf('$') + 1);
        }

        public final ScriptData parse(ParserReader reader) throws IOException {
            try {
                return parse(reader, new StringBuilder());
            } catch(Unexpected ex) {
                throw new Error.JavaException("SyntaxError", "Unexpected " + describe(ex.next) + " after " + describe(ex.current) + " at " + ex.next.rows + ":" + ex.next.columns, ex);
            }
        }
        
        public boolean inFunction() {
            return false;
        }
        
        public Parsed transform(Parsed input) {
            return input;
        }

        public final ScriptData parse(ParserReader reader, StringBuilder builder) throws IOException {
            if (DEBUG) {
                System.out.println("[" + this + "] Parsing...");
            }
            FunctionParser functionParser = this instanceof FunctionParser ? (FunctionParser) this : new FunctionParser();
            BlockParser blockParser = this instanceof BlockParser ? (BlockParser) this : new BlockParser(inFunction());
            SwitchParser switchParser = this instanceof SwitchParser ? (SwitchParser) this : new SwitchParser(inFunction());
            Matcher matcher;

            final int sRows = reader.rows;
            final int sColumns = reader.columns;

            boolean newline = false;
            Parsed currentPart = null;
            List<Parsed> queue = new ArrayList();
            List<Parsed> parts = new ArrayList();
            next:
            while (true) {
                try {
                    if(!queue.isEmpty())
                        throw new PartExchange(queue.remove(0), 0);
                    if (reader.current() == null) {
                        eof();
                        break;
                    }
                    for (Pattern pat : patterns) {
                        matcher = pat.matcher(reader.currentBuffer);
                        if (matcher.find()) {
                            match(pat, matcher, reader);
                            builder.append(reader.ltrim(matcher.group().length()));
                            continue next;
                        }
                    }
                    if (reader.more() == null) {
                        throw new Error.JavaException("SyntaxError", "No matching patterns for " + this + ": " + reader.currentBuffer.substring(0, Math.min(20, reader.currentBuffer.length())));
                    }
                    continue;
                } catch (PartExchange part) {
                    if(part.part.rows <= 0) {
                        part.part.rows = reader.rows;
                        part.part.columns = reader.columns;
                    }
                    if(part.part instanceof NewLine) {
                        builder.append(reader.ltrim(1));
                        newline = true;
                        continue;
                    }
                    if (currentPart != null) {
                        if (!currentPart.isIncomplete()) {
                            if(part.part instanceof CloseGroup) {
                                currentPart = currentPart.finish();
                                if (currentPart != null) {
                                    if (!currentPart.isStandalone()) {
                                        throw new Error.JavaException("SyntaxError", "Unexpected " + describe(currentPart) + " at " + currentPart.rows + ":" + currentPart.columns);
                                    }
                                    if (currentPart.isIncomplete()) {
                                        throw new Error.JavaException("SyntaxError", "Expected more after " + describe(currentPart) + " at " + currentPart.rows + ":" + currentPart.columns);
                                    }
                                    parts.add(currentPart);
                                }
                                if (DEBUG) {
                                    System.out.println("[" + this + "] End...");
                                }
                                end(sRows, sColumns, parts, builder.toString());
                            }
                        }

                        try {
                            if (DEBUG) {
                                System.out.println("[" + this + "] " + describe(currentPart) + " -> " + describe(part.part));
                            }
                            try {
                                currentPart = currentPart.transform(part.part);
                            } catch (Reparse reparse) {
                                reader.ltrim(part.trim);
                                reader.currentBuffer = reparse.buffer + reader.currentBuffer;
                                currentPart = currentPart.transform(reparse.transform);
                                newline = false;
                                continue;
                            } catch(Unexpected ex) {
                                if(newline) {
                                    if(currentPart instanceof Block && ((Block)currentPart).simpleimpl != null && ((Block)currentPart).state == Block.State.InSimpleImpl) {
                                        ((Block)currentPart).simpleimpl = ((Block)currentPart).simpleimpl.finish();
                                        ((Block)currentPart).state = Block.State.Complete;
                                    } else {
                                        currentPart = currentPart.finish();
                                        if (currentPart != null) {
                                            if (!currentPart.isStandalone()) {
                                                throw new Error.JavaException("SyntaxError", "Unexpected " + describe(currentPart) + " at " + currentPart.rows + ":" + currentPart.columns, ex);
                                            }
                                            if (currentPart.isIncomplete()) {
                                                throw new Error.JavaException("SyntaxError", "Expected more after " + describe(currentPart) + " at " + currentPart.rows + ":" + currentPart.columns, ex);
                                            }
                                            parts.add(currentPart);
                                            currentPart = null;
                                        }
                                    }
                                    queue.add(ex.next);
                                    newline = false;
                                } else {
                                    throw ex;
                                }
                            }
                        } catch (CompleteException ex) {
                            if (DEBUG) {
                                System.out.println("[" + this + "] Complete: " + ex);
                                ex.printStackTrace(System.out);
                            }
                            currentPart = currentPart.finish();
                            if (currentPart != null) {
                                if (!currentPart.isStandalone()) {
                                    throw new Error.JavaException("SyntaxError", "Unexpected " + describe(currentPart) + " at " + currentPart.rows + ":" + currentPart.columns, ex);
                                }
                                if (currentPart.isIncomplete()) {
                                    throw new Error.JavaException("SyntaxError", "Expected more after " + describe(currentPart) + " at " + currentPart.rows + ":" + currentPart.columns, ex);
                                }
                                parts.add(currentPart);
                            }
                            if (ex.part != null) {
                                if (ex.part instanceof CloseGroup) {
                                    if (DEBUG) {
                                        System.out.println("[" + this + "] End...");
                                    }
                                    end(sRows, sColumns, parts, builder.toString());
                                }
                                if (!ex.part.isStandalone()) {
                                    if (ex.part instanceof SemiColon) {
                                        builder.append(reader.ltrim(part.trim));
                                        currentPart = null;
                                        continue;
                                    }
                                    throw new net.nexustools.njs.Error.JavaException("SyntaxError", "Unexpected " + describe(ex.part) + " at " + ex.part.rows + ":" + ex.part.columns, ex);
                                }
                            }
                            try {
                                currentPart = transform(ex.part);
                            } catch (Reparse reparse) {
                                reader.ltrim(part.trim);
                                reader.currentBuffer = reparse.buffer + reader.currentBuffer;
                                currentPart = reparse.transform;
                                newline = false;
                                continue;
                            }
                        } catch (ParseFunction ex) {
                            builder.append(reader.ltrim(part.trim));
                            try {
                                functionParser.parse(reader);
                            } catch (ParseComplete ce) {
                                ex.function.impl = ce.impl;
                                ex.function.impl.callee = ex.function;
                                ex.function.source = ce.source;
                                ex.function.complete();
                                builder.append(ce.source);
                                builder.append(reader.ltrim(1));
                            }
                            newline = false;
                            continue;
                        } catch (ParseBlock ex) {
                            builder.append(reader.ltrim(part.trim));
                            try {
                                blockParser.parse(reader);
                            } catch (ParseComplete ce) {
                                ex.block.impl = ce.impl;
                                ex.block.complete();
                                builder.append(ce.source);
                                builder.append(reader.ltrim(1));
                            }
                            newline = false;
                            continue;
                        } catch (ParseSwitch ex) {
                            builder.append(reader.ltrim(part.trim));
                            try {
                                switchParser.parse(reader);
                            } catch (ParseComplete ce) {
                                ex.block.impl = ce.impl;
                                ex.block.state = Block.State.Complete;
                                builder.append(ce.source);
                                builder.append(reader.ltrim(1));
                            }
                            newline = false;
                            continue;
                        }
                    } else {
                        if (part.part instanceof CloseGroup) {
                            if (DEBUG) {
                                System.out.println("[" + this + "] End...");
                            }
                            end(sRows, sColumns, parts, builder.toString());
                        } else
                            try {
                                currentPart = transform(part.part);
                            } catch (Reparse reparse) {
                                reader.ltrim(part.trim);
                                reader.currentBuffer = reparse.buffer + reader.currentBuffer;
                                currentPart = reparse.transform;
                                newline = false;
                                continue;
                            }
                    }
                    builder.append(reader.ltrim(part.trim));
                    newline = false;
                } catch (PartComplete part) {
                    currentPart = currentPart.finish();
                    if (currentPart != null) {
                        if (currentPart instanceof CloseGroup) {
                            if (DEBUG) {
                                System.out.println("[" + this + "] End...");
                            }
                            end(sRows, sColumns, parts, builder.toString());
                        }
                        if (!currentPart.isStandalone()) {
                            throw new Error.JavaException("SyntaxError", "Unexpected " + describe(currentPart) + " at " + currentPart.rows + ":" + currentPart.columns);
                        }
                        if (currentPart.isIncomplete()) {
                            throw new Error.JavaException("SyntaxError", "Expected more after " + describe(currentPart) + " at " + currentPart.rows + ":" + currentPart.columns);
                        }
                        parts.add(currentPart);
                        currentPart = null;
                    }
                    builder.append(reader.ltrim(part.trim));
                }
            }
            if (currentPart != null) {
                currentPart = currentPart.finish();
                if (currentPart != null) {
                    if (!currentPart.isStandalone()) {
                        throw new Error.JavaException("SyntaxError", "Unexpected " + describe(currentPart) + " at " + currentPart.rows + ":" + currentPart.columns);
                    }
                    if (currentPart.isIncomplete()) {
                        throw new Error.JavaException("SyntaxError", "Expected more after " + describe(currentPart) + " at " + currentPart.rows + ":" + currentPart.columns);
                    }
                    parts.add(currentPart);
                    currentPart = null;
                }
            }
            return new ScriptData(parts.toArray(new Parsed[parts.size()]), builder.toString(), 1, 1);
        }

        public void end(int rows, int columns, List<Parsed> parts, java.lang.String source) {
            throw new Error.JavaException("SyntaxError", "Unexpected } at " + rows + ":" + columns);
        }

        public abstract void match(Pattern pattern, Matcher matcher, ParserReader reader);

        public abstract void eof();
    }

    public static class ScriptParser extends RegexParser {

        public ScriptParser() {
            super(SINGLELINE_COMMENT, MULTILINE_COMMENT, LAMBDA, OCT, HEX, REGEX, DBLSHIFTRIGHTEQ, SHIFTLEFTEQ, SHIFTRIGHTEQ, DBLSHIFTRIGHT, SHIFTLEFT, SHIFTRIGHT, NOTSTRICTEQUALS, NOTEQUALS, STRICTEQUALS, EQUALS, COLON, MOREEQUAL, LESSEQUAL, MORETHAN, LESSTHAN, COMMA, NUMBERGET, STRINGGET, NOT, ANDAND, OROR, ANDEQUAL, OREQUAL, AND, OR, PERCENT, SET, PLUSPLUS, MINUSMINUS, PLUSEQ, MINUSEQ, DIVIDEEQ, MULTIPLYEQ, PLUS, MINUS, MULTIPLY, SEMICOLON, NEWLINE, NUMBER, VARIABLE, VARIABLEGET, DIVIDE, WHITESPACE, TEMPLATE_LITERAL, STRING, OPEN_GROUP, CLOSE_GROUP, OPEN_BRACKET, CLOSE_BRACKET, VAR, OPEN_ARRAY, CLOSE_ARRAY, FORKSTART, VARARGS);
        }

        @Override
        public void match(Pattern pattern, Matcher matcher, ParserReader reader) {
            if (pattern == WHITESPACE || pattern == MULTILINE_COMMENT || pattern == SINGLELINE_COMMENT) {
                return; // Ignored
            }
            if (pattern == LAMBDA) {
                throw new PartExchange(new Lambda(), matcher.group().length());
            }
            if (pattern == VARARGS) {
                throw new PartExchange(new VarArgs(), matcher.group().length());
            }
            if (pattern == STRING) {
                java.lang.String string = matcher.group(1);
                throw new PartExchange(new String(string.substring(1, string.length() - 1)), matcher.group().length());
            }
            if (pattern == TEMPLATE_LITERAL) {
                java.lang.String string = matcher.group(1);
                throw new PartExchange(new TemplateLiteral(string, this), matcher.group().length());
            }
            if (pattern == HEX) {
                try {
                    long val = java.lang.Long.valueOf(matcher.group(1), 16);
                    if(matcher.group().startsWith("-"))
                        val = -val;
                    throw new PartExchange(new Long(val), matcher.group().length());
                } catch (NumberFormatException ex) {
                    try {
                        throw new PartExchange(new Number(Double.valueOf(matcher.group(0))), matcher.group().length());
                    } catch (NumberFormatException eex) {
                        throw new PartExchange(new Number(Double.NaN), matcher.group().length());
                    }
                }
            }
            if (pattern == OCT) {
                try {
                    long val = java.lang.Long.valueOf(matcher.group(1), 8);
                    if(matcher.group().startsWith("-"))
                        val = -val;
                    throw new PartExchange(new Long(val), matcher.group().length());
                } catch (NumberFormatException ex) {
                    try {
                        throw new PartExchange(new Number(Double.valueOf(matcher.group(0))), matcher.group().length());
                    } catch (NumberFormatException eex) {
                        throw new PartExchange(new Number(Double.NaN), matcher.group().length());
                    }
                }
            }
            if (pattern == NUMBER) {
                try {
                    throw new PartExchange(new Long(java.lang.Long.valueOf(matcher.group(0))), matcher.group().length());
                } catch (NumberFormatException ex) {
                    try {
                        throw new PartExchange(new Number(Double.valueOf(matcher.group(0))), matcher.group().length());
                    } catch (NumberFormatException eex) {
                        throw new PartExchange(new Number(Double.NaN), matcher.group().length());
                    }
                }
            }
            if (pattern == VARIABLE) {
                java.lang.String ref = matcher.group();
                if (ref.equals("in")) {
                    throw new PartExchange(new In(), ref.length());
                } else if (ref.equals("of")) {
                    throw new PartExchange(new Of(), ref.length());
                } else if (ref.equals("new")) {
                    throw new PartExchange(new New(), ref.length());
                } else if (ref.equals("null")) {
                    throw new PartExchange(new Null(), ref.length());
                } else if (ref.equals("undefined")) {
                    throw new PartExchange(new Undefined(), ref.length());
                } else if (ref.equals("function")) {
                    throw new PartExchange(new Function(), ref.length());
                } else if (ref.equals("while")) {
                    throw new PartExchange(new While(), ref.length());
                } else if (ref.equals("yield")) {
                    throw new PartExchange(new Yield(), ref.length());
                } else if (ref.equals("instanceof")) {
                    throw new PartExchange(new InstanceOf(), ref.length());
                } else if (ref.equals("do")) {
                    throw new PartExchange(new Do(), ref.length());
                } else if (ref.equals("if")) {
                    throw new PartExchange(new If(), ref.length());
                } else if (ref.equals("else")) {
                    throw new PartExchange(new Else(), ref.length());
                } else if (ref.equals("delete")) {
                    throw new PartExchange(new Delete(), ref.length());
                } else if (ref.equals("for")) {
                    throw new PartExchange(new For(), ref.length());
                } else if (ref.equals("try")) {
                    throw new PartExchange(new Try(), ref.length());
                } else if (ref.equals("catch")) {
                    throw new PartExchange(new Catch(), ref.length());
                } else if (ref.equals("finally")) {
                    throw new PartExchange(new Finally(), ref.length());
                } else if (ref.equals("true")) {
                    throw new PartExchange(new Boolean(true), ref.length());
                } else if (ref.equals("false")) {
                    throw new PartExchange(new Boolean(false), ref.length());
                } else if (ref.equals("throw")) {
                    throw new PartExchange(new Throw(), ref.length());
                } else if (ref.equals("switch")) {
                    throw new PartExchange(new Switch(), ref.length());
                } else if (ref.equals("typeof")) {
                    throw new PartExchange(new TypeOf(), ref.length());
                } else if (ref.equals("case")) {
                    throw new PartExchange(new Case(), ref.length());
                } else if (ref.equals("super")) {
                    throw new PartExchange(new Super(), ref.length());
                } else if (ref.equals("get")) {
                    throw new PartExchange(new Getter(), ref.length());
                } else if (ref.equals("constructor")) {
                    throw new PartExchange(new Constructor(), ref.length());
                } else if (ref.equals("extends")) {
                    throw new PartExchange(new ExtendsKeyword(), ref.length());
                } else if (ref.equals("set")) {
                    throw new PartExchange(new Setter(), ref.length());
                } else if (ref.equals("class")) {
                    throw new PartExchange(new Class(), ref.length());
                } else if (ref.equals("break")) {
                    throw new PartExchange(new Break(), ref.length());
                } else if (ref.equals("default")) {
                    throw new PartExchange(new Default(), ref.length());
                } else if (ref.equals("return")) {
                    ret(matcher);
                } else if (ref.equals("var")) {
                    throw new PartExchange(new Var(), ref.length());
                } else if (ref.equals("let") || ref.equals("const")) {
                    throw new PartExchange(new Let(), ref.length());
                } else {
                    throw new PartExchange(new Reference(ref), ref.length());
                }
            }
            if (pattern == VARIABLEGET) {
                throw new PartExchange(new DirectReference(matcher.group(1)), matcher.group().length());
            }
            if (pattern == OPEN_BRACKET) {
                throw new PartExchange(new OpenBracket(), matcher.group().length());
            }
            if (pattern == CLOSE_BRACKET) {
                throw new PartExchange(new CloseBracket(), matcher.group().length());
            }
            if (pattern == OPEN_GROUP) {
                throw new PartExchange(new OpenGroup(), matcher.group().length());
            }
            if (pattern == CLOSE_GROUP) {
                throw new PartExchange(new CloseGroup(), matcher.group().length());
            }
            if (pattern == OPEN_ARRAY) {
                throw new PartExchange(new OpenArray(), matcher.group().length());
            }
            if (pattern == CLOSE_ARRAY) {
                throw new PartExchange(new CloseArray(), matcher.group().length());
            }

            if (pattern == REGEX) {
                throw new PartExchange(new RegEx(matcher.group(1), matcher.group(2)), matcher.group().length());
            }

            if (pattern == FORKSTART) {
                throw new PartExchange(new Fork(), matcher.group().length());
            }

            if (pattern == NOT) {
                throw new PartExchange(new Not(), matcher.group().length());
            }
            if (pattern == PLUS) {
                throw new PartExchange(new Plus(), matcher.group().length());
            }
            if (pattern == MINUS) {
                throw new PartExchange(new Minus(), matcher.group().length());
            }
            if (pattern == DIVIDE) {
                throw new PartExchange(new Divide(), matcher.group().length());
            }
            if (pattern == MULTIPLY) {
                throw new PartExchange(new Multiply(), matcher.group().length());
            }
            if (pattern == SHIFTRIGHT) {
                throw new PartExchange(new ShiftRight(), matcher.group().length());
            }
            if (pattern == SHIFTLEFT) {
                throw new PartExchange(new ShiftLeft(), matcher.group().length());
            }
            if (pattern == DBLSHIFTRIGHT) {
                throw new PartExchange(new DoubleShiftRight(), matcher.group().length());
            }
            if (pattern == DBLSHIFTRIGHTEQ) {
                throw new PartExchange(new DoubleShiftRightEq(), matcher.group().length());
            }
            if (pattern == EQUALS) {
                throw new PartExchange(new Equals(), matcher.group().length());
            }
            if (pattern == STRICTEQUALS) {
                throw new PartExchange(new StrictEquals(), matcher.group().length());
            }
            if (pattern == NOTEQUALS) {
                throw new PartExchange(new NotEquals(), matcher.group().length());
            }
            if (pattern == NOTSTRICTEQUALS) {
                throw new PartExchange(new StrictNotEquals(), matcher.group().length());
            }

            if (pattern == PLUSEQ) {
                throw new PartExchange(new PlusEq(), matcher.group().length());
            }
            if (pattern == MINUSEQ) {
                throw new PartExchange(new MinusEq(), matcher.group().length());
            }
            if (pattern == MULTIPLYEQ) {
                throw new PartExchange(new MultiplyEq(), matcher.group().length());
            }
            if (pattern == DIVIDEEQ) {
                throw new PartExchange(new DivideEq(), matcher.group().length());
            }
            if (pattern == PLUSPLUS) {
                throw new PartExchange(new PlusPlus(), matcher.group().length());
            }
            if (pattern == MINUSMINUS) {
                throw new PartExchange(new MinusMinus(), matcher.group().length());
            }

            if (pattern == MORETHAN) {
                throw new PartExchange(new MoreThan(), matcher.group().length());
            }
            if (pattern == LESSTHAN) {
                throw new PartExchange(new LessThan(), matcher.group().length());
            }
            if (pattern == MOREEQUAL) {
                throw new PartExchange(new MoreEqual(), matcher.group().length());
            }
            if (pattern == LESSEQUAL) {
                throw new PartExchange(new LessEqual(), matcher.group().length());
            }

            if (pattern == SET) {
                throw new PartExchange(new Set(), matcher.group().length());
            }
            if (pattern == OROR) {
                throw new PartExchange(new OrOr(), matcher.group().length());
            }
            if (pattern == ANDAND) {
                throw new PartExchange(new AndAnd(), matcher.group().length());
            }
            if (pattern == PERCENT) {
                throw new PartExchange(new Percent(), matcher.group().length());
            }
            if (pattern == OREQUAL) {
                throw new PartExchange(new OrEq(), matcher.group().length());
            }
            if (pattern == ANDEQUAL) {
                throw new PartExchange(new AndEq(), matcher.group().length());
            }
            if (pattern == OR) {
                throw new PartExchange(new Or(), matcher.group().length());
            }
            if (pattern == AND) {
                throw new PartExchange(new And(), matcher.group().length());
            }
            if (pattern == COLON) {
                throw new PartExchange(new Colon(), matcher.group().length());
            }

            if (pattern == NUMBERGET) {
                try {
                    throw new PartExchange(new IntegerReference(java.lang.Integer.valueOf(matcher.group(1))), matcher.group().length());
                } catch (NumberFormatException ex) {
                    throw new PartExchange(new DirectReference(matcher.group(1)), matcher.group().length());
                }
            }

            if (pattern == STRINGGET) {
                java.lang.String string = matcher.group(1);
                throw new PartExchange(new DirectReference(string.substring(1, string.length() - 1)), matcher.group().length());
            }

            if (pattern == SEMICOLON) {
                throw new PartExchange(new SemiColon(), matcher.group().length());
            }
            if (pattern == NEWLINE) {
                throw new PartExchange(new NewLine(), matcher.group().length());
            }
            if (pattern == COMMA) {
                throw new PartExchange(new Comma(), matcher.group().length());
            }

            System.out.println(pattern);
            System.out.println(matcher.groupCount());
            for (int i = 0; i <= matcher.groupCount(); i++) {
                System.out.println(matcher.group(i));
            }
        }

        public void ret(Matcher matcher) {
            throw new PartExchange(new Return(), matcher.group().length());
        }

        @Override
        public void eof() {
        }
    }

    public static class BlockParser extends ScriptParser {

        final boolean inFunction;

        public BlockParser(boolean inFunction) {
            this.inFunction = inFunction;
        }

        @Override
        public boolean inFunction() {
            return inFunction;
        }

        @Override
        public void end(int rows, int columns, List<Parsed> parts, java.lang.String source) {
            throw new ParseComplete(new ScriptData(parts.toArray(new Parsed[parts.size()]), source, rows, columns), source);
        }

        @Override
        public void ret(Matcher matcher) {
            if (inFunction) {
                throw new PartExchange(new Return(), matcher.group().length());
            }
            super.ret(matcher);
        }
    }

    public static class FunctionParser extends ScriptParser {

        @Override
        public boolean inFunction() {
            return true;
        }

        @Override
        public void ret(Matcher matcher) {
            throw new PartExchange(new Return(), matcher.group().length());
        }

        @Override
        public void end(int rows, int columns, List<Parsed> parts, java.lang.String source) {
            throw new ParseComplete(new ScriptData(parts.toArray(new Parsed[parts.size()]), source, rows, columns), source);
        }
    }

    public static class SwitchParser extends BlockParser {
        public SwitchParser(boolean inFunction) {
            super(inFunction);
        }
        
        @Override
        public Parsed transform(Parsed input) {
            if(input instanceof Reference) {
                if(((Reference)input).ref.equals("case"))
                    return new Case();
                if(((Reference)input).ref.equals("default"))
                    return new Default();
            }
            return input;
        }
        
    }

    @Override
    public final Script compile(java.lang.String source, java.lang.String fileName, boolean inFunction) {
        return compile(new StringReader(source), fileName, inFunction);
    }

    protected final ScriptData parse(Reader source, java.lang.String fileName, boolean inFunction) {
        ParserReader reader = null;
        RegexParser parser = inFunction ? new FunctionParser() : new ScriptParser();
        try {
            ScriptData script = parser.parse(reader = new ParserReader(source));
            if (DEBUG) {
                System.out.println("Compiling " + join(Arrays.asList(script), ';'));
            }
            return script;
        } catch (net.nexustools.njs.Error.JavaException ex) {
            if (ex.type.equals("SyntaxError") && reader != null) {
                StringBuilder builder = new StringBuilder(ex.getUnderlyingMessage());
                builder.append(" (");
                if (fileName != null) {
                    builder.append(fileName);
                    builder.append(':');
                }
                builder.append(reader.rows());
                builder.append(':');
                builder.append(reader.columns());
                builder.append(')');
                throw new net.nexustools.njs.Error.JavaException("SyntaxError", builder.toString(), ex);
            }
            throw ex;
        } catch (IOException ex) {
            throw new Error.JavaException("EvalError", "IO Exception While Evaluating Script: " + ex.getMessage(), ex);
        }
    }

    @Override
    public final Script compile(Reader source, java.lang.String fileName, boolean inFunction) {
        return compileScript(parse(source, fileName, inFunction), fileName, inFunction);
    }

    protected abstract Script compileScript(ScriptData script, java.lang.String fileName, boolean inFunction);

}
