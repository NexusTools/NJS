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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.nexustools.njs.Error;

/**
 *
 * @author Katelyn Slater <ktaeyln@gmail.com>
 */
public abstract class RegexCompiler implements Compiler {
	public static final boolean DEBUG = System.getProperty("NJSDEBUG", "false").equals("true");
	public static java.lang.String convertStringSource(java.lang.String string) {
		return string.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\"", "\\\"");
	}
	public static java.lang.String unparsed(java.lang.Object object) {
		if(object == null)
			return "<unparsed>";
		return object.toString();
	}
	public static java.lang.String describe(java.lang.Object object) {
		if(object == null)
			return "null";
		return "(@" + object.getClass().getSimpleName() + ":" + object + ")";
	}
	public static java.lang.String toString(java.lang.Object object) {
		if(object == null)
			return "";
		return object.toString();
	}
	public static java.lang.String toRPadString(java.lang.Object object) {
		if(object == null)
			return "";
		return object.toString() + ' ';
	}
	public static java.lang.String toLPadString(java.lang.Object object) {
		if(object == null)
			return "";
		return ' ' + object.toString();
	}
	public static java.lang.String join(Iterable<?> able, char with) {
		StringBuilder builder = new StringBuilder();
		join(able, with, builder);
		return builder.toString();
	}
	public static void join(Iterable<?> able, char with, StringBuilder builder) {
		Iterator<?> it = able.iterator();
		if(it.hasNext()) {
			builder.append(it.next());
			while(it.hasNext()) {
				builder.append(with);
				builder.append(it.next());
			}
		}
	}
	public static class ScriptData {
		Function callee;
		final Parsed[] impl;
		final int rows, columns;
		final Function[] functions;
		java.lang.String methodName = null, source;
		public ScriptData(Parsed[] impl, java.lang.String source, int rows, int columns) {
			List<Parsed> imp = new ArrayList();
			List<Function> funcs = new ArrayList();
			for(int i=0; i<impl.length; i++) {
				if(impl[i] instanceof Function && ((Function)impl[i]).name != null) {
					funcs.add((Function)impl[i]);
					continue;
				}
				
				imp.add(impl[i]);
			}
			functions = funcs.toArray(new Function[funcs.size()]);
			this.impl = imp.toArray(new Parsed[imp.size()]);
			this.source = source;
			this.columns = columns;
			this.rows = rows;
		}
		@Override
		public java.lang.String toString() {
			StringBuilder builder = new StringBuilder();
			join(Arrays.asList(functions), ';', builder);
			if(builder.length() > 0)
				builder.append(':');
			join(Arrays.asList(impl), ';', builder);
			return builder.toString();
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
	public static class ParseComplete extends RuntimeException {
		public final ScriptData impl;
		public final java.lang.String source;
		public ParseComplete(ScriptData impl, java.lang.String source) {
			this.impl = impl;
			this.source = source;
		}
	}
	public static abstract class Parsed {
		public int columns, rows;
		public abstract Parsed transform(Parsed part);
		public abstract java.lang.String toSource();
		public java.lang.String toSimpleSource() {
			return toSource();
		}
		@Override
		public java.lang.String toString() {
			return toSimpleSource();
		}
		public boolean isNumber() {
			return this instanceof NumberReferency;
		}
		public boolean isString() {
			return this instanceof StringReferency;
		}
		public java.lang.String primaryType() {
			if(isNumber())
				return "number";
			if(isString())
				return "string";
			return "unknown";
		}
		public abstract boolean isStandalone();
		public abstract boolean isIncomplete();
		public abstract Parsed finish();
	}
	public static interface NumberReferency {}
	public static interface StringReferency {}
	public static interface BaseReferency {
		public Parsed transformFallback(Parsed part);
	}
	public static abstract class PrimitiveReferency extends Parsed implements BaseReferency {
		public boolean newline;
		public PrimitiveReferency extend(DirectReference reference) {
			return new RightReference(this, reference.ref);
		}
		public PrimitiveReferency extend(IntegerReference reference) {
			return new IntegerReference(this, reference.ref);
		}
		@Override
		public Parsed transform(Parsed part) {
			if(DEBUG && !isIncomplete())
				System.out.println('\t' + describe(this) + " -> " + describe(part));
			
			if(part instanceof SemiColon)
				throw new CompleteException();
			if(part instanceof NewLine) {
				newline = true;
				return this;
			}
			
			if(part instanceof DirectReference)
				return extend((DirectReference)part);
			else if(part instanceof IntegerReference)
				return extend((IntegerReference)part);
			else if(part instanceof OpenBracket)
				return new Call(part, this);
			else if(part instanceof InstanceOf)
				return new InstanceOf(this);
			else if(part instanceof MoreThan)
				return new MoreThan(this);
			else if(part instanceof LessThan)
				return new LessThan(this);
			else if(part instanceof MoreEqual)
				return new MoreEqual(this);
			else if(part instanceof LessEqual)
				return new LessEqual(this);
			else if(part instanceof StrictEquals)
				return new StrictEquals(this);
			else if(part instanceof NotEquals)
				return new NotEquals(this);
			else if(part instanceof StrictNotEquals)
				return new StrictNotEquals(this);
			else if(part instanceof Equals)
				return new Equals(this);
			else if(part instanceof Divide)
				return new Divide(this);
			else if(part instanceof Multiply)
				return new Multiply(this);
			else if(part instanceof Plus)
				return new Plus(this);
			else if(part instanceof Minus)
				return new Minus(this);
			else if(part instanceof Set)
				return new Set(this);
			else if(part instanceof Percent)
				return new Percent(this);
			else if(part instanceof And)
				return new And(this);
			else if(part instanceof Or)
				return new Or(this);
			else if(part instanceof AndAnd)
				return new AndAnd(this);
			else if(part instanceof OrOr)
				return new OrOr(this);
			else if(part instanceof Number && ((Number)part).value < 0)
				return new Minus(this, new Number(-((Number)part).value));
			else if(part instanceof Integer && ((Integer)part).value < 0)
				return new Minus(this, new Number(-((Integer)part).value));
			else
				return transformFallback(part);
		}
		
		public Parsed transformFallback(Parsed part) {
			if(newline)
				throw new CompleteException(part);
			
			throw new Error.JavaException("SyntaxError", "Unexpected " + part.toSource() + " after " + describe(this));
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
			if(part instanceof OpenArray)
				return new VariableReference(this);
			else if(part instanceof MultiplyEq)
				return new MultiplyEq(this);
			else if(part instanceof PlusEq)
				return new PlusEq(this);
			else if(part instanceof PlusPlus)
				return new PlusPlus(this);
			else if(part instanceof MinusMinus)
				return new MinusMinus(this);
			return super.transformFallback(part);
		}
		
	}
	public static abstract class VolatileReferency extends Referency {
		@Override
		public Referency extend(DirectReference reference) {
			throw new Error.JavaException("SyntaxError", "Unexpected " + reference);
		}
		@Override
		public Referency extend(IntegerReference reference) {
			throw new Error.JavaException("SyntaxError", "Unexpected " + reference);
		}
	}
	public static class VariableReference extends Referency {
		public Parsed ref;
		private boolean closed;
		public final Referency lhs;
		public VariableReference(Referency lhs) {
			this.lhs = lhs;
		}

		@Override
		public Parsed transform(Parsed part) {
			if(closed)
				return super.transform(part);
			
			if(ref == null)
				ref = part;
			else {
				if(!ref.isIncomplete()) {
					if(part instanceof CloseArray) {
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
		public java.lang.String toSource() {
			StringBuilder builder = new StringBuilder(unparsed(lhs));
			builder.append('[');
			builder.append(unparsed(ref));
			if(closed)
				builder.append(']');
			return builder.toString();
		}
		
	}
	public static class Reference extends Referency {
		public final java.lang.String ref;
		public Reference(java.lang.String ref) {
			this.ref = ref;
		}
		@Override
		public java.lang.String toSource() {
			return ref;
		}

		@Override
		public Referency extend(DirectReference reference) {
			ReferenceChain chain = new ReferenceChain();
			chain.chain.add(ref);
			chain.chain.add(reference.ref);
			return chain;
		}
	}
	public static class OrOr extends RhLh {
		public OrOr() {}
		public OrOr(Parsed lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "||";
		}
	}
	public static class AndAnd extends RhLh {
		public AndAnd() {}
		public AndAnd(Parsed lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "&&";
		}
	}
	public static class Colon extends Helper {
		public Colon() {}
		
		@Override
		public java.lang.String toSource() {
			return ":";
		}
	}
	public static class DirectReference extends Helper {
		public final java.lang.String ref;
		public DirectReference(java.lang.String ref) {
			this.ref = ref;
		}
		@Override
		public java.lang.String toSource() {
			return '.' + ref;
		}
	}
	public static class IntegerReference extends Referency {
		public final Parsed lhs;
		public final int ref;
		public IntegerReference(Parsed lhs, int ref) {
			this.lhs = lhs.finish();
			this.ref = ref;
		}
		public IntegerReference(int ref) {
			this.ref = ref;
			lhs = null;
		}
		@Override
		public java.lang.String toSource() {
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
			if(lhs == null) {
				OpenArray array = new OpenArray();
				array.entries.add(new Number(ref));
				array.closed = true;
				return array;
			}
			return this;
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
		public java.lang.String toSource() {
			StringBuilder builder = new StringBuilder(ref.toString());
			if(!chain.isEmpty()) {
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
	}
	public static class Call extends Referency {
		final List<Parsed> arguments = new ArrayList();
		Parsed reference;
		Parsed currentArgumentPart;
		boolean closed;
		public Call(Parsed original, Parsed ref) {
			columns = original.columns;
			rows = original.rows;
			reference = ref;
		}

		@Override
		public Parsed transform(Parsed part) {
			if(closed)
				return super.transform(part);
			
			if(currentArgumentPart == null) {
				if(part instanceof CloseBracket) {
					closed = true;
					return this;
				}
				currentArgumentPart = part;
			} else {
				if(!currentArgumentPart.isIncomplete()) {
					if(part instanceof CloseBracket) {
						arguments.add(currentArgumentPart);
						currentArgumentPart = null;
						closed = true;
						return this;
					} else if(part instanceof Comma) {
						if(currentArgumentPart.isIncomplete())
							throw new Error.JavaException("SyntaxError", "Unexpected " + part.toSource());
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
		public java.lang.String toSource() {
			StringBuilder builder = new StringBuilder(reference.toString());
			builder.append('(');
			builder.append(join(arguments, '.'));
			if(closed)
				builder.append(')');
			
			return builder.toString();
		}
		
		public Parsed finish() {
			reference = reference.finish();
			if(arguments != null) {
				ListIterator<Parsed> it = arguments.listIterator();
				while(it.hasNext())
					it.set(it.next().finish());
			}
			if(currentArgumentPart != null)
				throw new Error.JavaException("SyntaxError", "Unexpected EOF");
			return this;
		}
	}
	public static class Return extends Rh {
		public Return() {}
		@Override
		public java.lang.String op() {
			return "return";
		}
		@Override
		public Parsed transform(Parsed part) {
			if(rhs == null && part instanceof SemiColon)
				throw new CompleteException();
			
			return super.transform(part);
		}
		@Override
		public boolean isIncomplete() {
			return rhs != null && rhs.isIncomplete();
		}
	}
	public static class Delete extends Rh {
		public Delete() {}
		@Override
		public java.lang.String op() {
			return "delete";
		}
	}
	public static class TypeOf extends RhReferency {
		public TypeOf() {}
		@Override
		public java.lang.String op() {
			return "typeof";
		}
	}
	public static class Yield extends Rh {
		public Yield() {}
		@Override
		public java.lang.String op() {
			return "yield";
		}
	}
	public static class Throw extends Rh {
		public Throw() {}
		@Override
		public java.lang.String op() {
			return "throw";
		}
	}
	public static class Case extends Helper {
		public Case() {}
		@Override
		public java.lang.String toSource() {
			return "case";
		}
	}
	public static class ReferenceChain extends Referency {
		public final List<java.lang.String> chain = new ArrayList();
		public ReferenceChain() {}
		public ReferenceChain(java.lang.String ref) {
			assert(ref != null);
			chain.add(ref);
		}
		@Override
		public java.lang.String toSource() {
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
	}
	public static class OpenBracket extends Referency {
		Parsed contents;
		boolean closed;
		List<java.lang.String> chain = new ArrayList();
		
		public OpenBracket() {}
		@Override
		public java.lang.String toSource() {
			StringBuilder builder = new StringBuilder("(");
			builder.append(unparsed(contents));
			if(closed) {
				builder.append(')');
				if(!chain.isEmpty()) {
					builder.append('.');
					join(chain, '.', builder);
				}
			}
			return builder.toString();
		}
		@Override
		public Parsed transform(Parsed part) {
			if(closed) {
				if(part instanceof SemiColon)
					throw new CompleteException(part);
				
				return super.transform(part);
			}
			
			if(contents == null)
				contents = part;
			else {
				if(!contents.isIncomplete()) {
					if(part instanceof CloseBracket) {
						closed = true;
						return this;
					}
				}
				try {
					contents = contents.transform(part);
				} catch(CompleteException ex) {
					throw new Error.JavaException("EvalError", "CompleteException called when not needed", ex);
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
			contents = contents.finish();
			if((contents instanceof Reference || contents instanceof ReferenceChain) && chain.isEmpty())
				return contents;
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
		public java.lang.String primaryType() {
			return contents.primaryType();
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
		Parsed condition;
		Parsed simpleimpl;
		ScriptData impl;
		public Block(State state) {
			this.state = state;
		}
		public Block() {
			state = State.BeforeCondition;
		}
		
		@Override
		public Parsed transform(Parsed part) {
			switch(state) {
				case BeforeCondition:
					if(part instanceof OpenBracket) {
						state = State.InCondition;
						return this;
					}
					break;
				case InCondition:
					transformCondition(part);
					return this;
				case AfterCondition:
					if(part instanceof NewLine)
						return this;
					if(part instanceof OpenGroup)
						return parse();
					else if(part instanceof If && this instanceof Else)
						return new ElseIf();
					else if(allowSimpleImpl()) {
						simpleimpl = part;
						state = State.InSimpleImpl;
						return this;
					}
					break;
				case InImpl:
					return transformImpl(part);
				case InSimpleImpl:
					if(!simpleimpl.isIncomplete()) {
						if(part instanceof SemiColon) {
							state = State.Complete;
							simpleimpl = simpleimpl.finish();
							return this;
						}
					}
					simpleimpl = simpleimpl.transform(part);
					return this;
				case Complete:
					return complete(part);
			}
			
			throw new Error.JavaException("SyntaxError", "Unexpected " + part.toSource() + " (" + state + ":" + getClass().getSimpleName() + ")");
		}
		public void transformCondition(Parsed part) {
			if(condition == null)
				condition = part;
			else {
				if(!condition.isIncomplete()) {
					if(part instanceof CloseBracket) {
						condition = condition.finish();
						finishCondition();
						return;
					}
				}
				try {
					condition = condition.transform(part);
				} catch(CompleteException ex) {
					throw new net.nexustools.njs.Error.JavaException("SyntaxError", "Unexpected " + part);
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
			if(state == State.InSimpleImpl) {
				state = State.Complete;
				simpleimpl = simpleimpl.finish();
			}
			return this;
		}
	}
	public static class While extends Block {
		public While() {}
		
		@Override
		public java.lang.String toSource() {
			StringBuilder builder = new StringBuilder("while (");
			builder.append(condition);
			builder.append(") {");
			if(simpleimpl != null)
				builder.append(simpleimpl);
			else if(impl == null)
				builder.append("<unparsed>");
			else
				join(Arrays.asList(impl), ';', builder);
			builder.append("}");
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
		public java.lang.String toSource() {
			StringBuilder builder = new StringBuilder("try {");
			if(impl == null)
				builder.append("<unparsed>");
			else
				join(Arrays.asList(impl), ';', builder);
			builder.append("}");
			if(c != null) {
				builder.append(' ');
				builder.append(c);
			}
			if(f != null) {
				builder.append(' ');
				builder.append(f);
			}
			return builder.toString();
		}

		@Override
		public Parsed complete(Parsed part) {
			if(!(part instanceof NewLine)) {
				if(f != null) {
					if(f.isIncomplete())
						f = (Finally)f.transform(part);
					else 
						throw new CompleteException(part);
				} else if(c != null) {
					if(c.isIncomplete())
						c = (Catch)c.transform(part);
					else if(part instanceof Finally) {
						if(f != null)
							f = (Finally)f.transform(part);
						else if(part instanceof Finally)
							f = (Finally)part;
						else
							throw new CompleteException(part);
					} else
						throw new CompleteException(part);
				} else if(part instanceof Catch)
					c = (Catch)part;
				else if(part instanceof Finally)
					f = (Finally)part;
				else
					return super.complete(part);
			}
			
			return this;
		}
	}
	public static class Catch extends Block {
		public Catch() {
			super(Block.State.BeforeCondition);
		}

		@Override
		public boolean allowSimpleImpl() {
			return false;
		}
		
		@Override
		public java.lang.String toSource() {
			StringBuilder builder = new StringBuilder("catch (");
			builder.append(condition);
			builder.append(") {");
			if(impl == null)
				builder.append("<unparsed>");
			else
				join(Arrays.asList(impl), ';', builder);
			builder.append("}");
			return builder.toString();
		}

		@Override
		public boolean isStandalone() {
			return false;
		}
		
	}
	public static class Switch extends Block {
		public static enum CaseState {
			NeedCase,
			InCase,
			HaveCase,
			StartImpl,
			InImpl
			
		}
		public static class Case {
		}
		
		List<Case> cases = new ArrayList();
		CaseState caseState = CaseState.NeedCase;
		public Switch() {}

		@Override
		public Parsed transformImpl(Parsed part) {
			if(part instanceof NewLine)
				return this;
			
			switch(caseState) {
				case NeedCase:
					if(part instanceof RegexCompiler.Case) {
						caseState = CaseState.InCase;
						return this;
					}
					break;
				case InCase:
					if(part instanceof PrimitiveReferency) {
						caseState = CaseState.HaveCase;
						return this;
					}
				case HaveCase:
					if(part instanceof Colon) {
						caseState = CaseState.StartImpl;
						return this;
					}
			}
			
			throw new net.nexustools.njs.Error.JavaException("SyntaxError", "Unexpected " + part.toSource());
		}

		@Override
		public Parsed parse() {
			state = State.InImpl;
			return this;
		}

		@Override
		public boolean allowSimpleImpl() {
			return false;
		}
		
		@Override
		public java.lang.String toSource() {
			StringBuilder builder = new StringBuilder("switch (");
			builder.append(condition);
			builder.append(") {");
			if(impl == null)
				builder.append("<unparsed>");
			else
				join(Arrays.asList(impl), ';', builder);
			builder.append("}");
			return builder.toString();
		}

		@Override
		public boolean isStandalone() {
			return false;
		}
		
	}
	public static class Boolean extends Referency {
		public final java.lang.Boolean value;
		public Boolean(java.lang.Boolean value) {
			this.value = value;
		}
		@Override
		public java.lang.String toSource() {
			return java.lang.String.valueOf(value);
		}
		@Override
		public Referency extend(DirectReference reference) {
			throw new net.nexustools.njs.Error.JavaException("SyntaxError", "Unexpected " + reference);
		}
		@Override
		public Referency extend(IntegerReference reference) {
			throw new net.nexustools.njs.Error.JavaException("SyntaxError", "Unexpected " + reference);
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
		public java.lang.String toSource() {
			StringBuilder builder = new StringBuilder("finally {");
			if(impl == null)
				builder.append("<unparsed>");
			else
				join(Arrays.asList(impl), ';', builder);
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
			DuringLoop
		}
		static enum ForType {
			Standard,
			InLoop,
			OfLoop
		}
		
		Parsed init;
		Parsed loop;
		ForType type = ForType.Standard;
		ForConditionState state = ForConditionState.DuringInit;
		public For() {}
		@Override
		public java.lang.String toSource() {
			StringBuilder builder = new StringBuilder("for (");
			builder.append(unparsed(init));
			builder.append("; ");
			builder.append(unparsed(condition));
			builder.append("; ");
			builder.append(unparsed(loop));
			builder.append(") {");
			if(simpleimpl != null)
				builder.append(simpleimpl);
			else if(impl == null)
				builder.append("<unparsed>");
			else
				join(Arrays.asList(impl), ';', builder);
			builder.append("}");
			return builder.toString();
		}
		@Override
		public void transformCondition(Parsed part) {
			switch(state) {
				case DuringInit:
					if(init == null) {
						if(part instanceof SemiColon) {
							state = ForConditionState.DuringCondition;
							return;
						}
						init = part;
					} else {
						if(!init.isIncomplete()) {
							if(part instanceof SemiColon) {
								init = init.finish();
								state = ForConditionState.DuringCondition;
								return;
							} else if(part instanceof In) {
								type = ForType.InLoop;
								init = init.finish();
								state = ForConditionState.DuringLoop;
								return;
							} else if(part instanceof Of) {
								type = ForType.OfLoop;
								init = init.finish();
								state = ForConditionState.DuringLoop;
								return;
							}
						}
						try {
							init = init.transform(part);
						} catch(CompleteException ex) {
							throw new net.nexustools.njs.Error.JavaException("SyntaxError", "Unexpected " + part);
						}
					}
					break;
				case DuringCondition:
					if(condition == null)
						condition = part;
					else {
						if(!condition.isIncomplete()) {
							if(part instanceof SemiColon) {
								condition = condition.finish();
								state = ForConditionState.DuringLoop;
								return;
							}
						}
						try {
							condition = condition.transform(part);
						} catch(CompleteException ex) {
							throw new net.nexustools.njs.Error.JavaException("SyntaxError", "Unexpected " + part);
						}
					}
					break;
				case DuringLoop:
					if(loop == null) {
						if(part instanceof CloseBracket) {
							finishCondition();
							return;
						}
						loop = part;
					} else {
						if(!loop.isIncomplete()) {
							if(part instanceof CloseBracket) {
								loop = loop.finish();
								finishCondition();
								return;
							}
						}
						try {
							loop = loop.transform(part);
						} catch(CompleteException ex) {
							throw new net.nexustools.njs.Error.JavaException("SyntaxError", "Unexpected " + part);
						}
					}
					break;
			}
			
			return;
		}
	}
	public static class If extends Block {
		public Else el;
		public If() {}
		@Override
		public java.lang.String toSource() {
			StringBuilder builder = new StringBuilder("if (");
			builder.append(condition);
			builder.append(") {");
			if(simpleimpl != null)
				builder.append(simpleimpl);
			else if(impl == null)
				builder.append("<unparsed>");
			else
				join(Arrays.asList(impl), ';', builder);
			builder.append("}");
			if(el != null) {
				builder.append(' ');
				builder.append(el);
			}
			return builder.toString();
		}

		@Override
		public Parsed complete(Parsed part) {
			if(!(part instanceof NewLine)) {
				if(el != null)
					el = (Else)el.transform(part);
				else if(part instanceof Else)
					el = (Else)part;
				else 
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
		public java.lang.String toSource() {
			StringBuilder builder = new StringBuilder("else if (");
			builder.append(condition);
			builder.append(") {");
			if(simpleimpl != null)
				builder.append(simpleimpl);
			else if(impl == null)
				builder.append("<unparsed>");
			else
				join(Arrays.asList(impl), ';', builder);
			builder.append("}");
			if(el != null) {
				builder.append(' ');
				builder.append(el);
			}
			return builder.toString();
		}

		@Override
		public Parsed complete(Parsed part) {
			if(!(part instanceof NewLine)) {
				if(part instanceof Else)
					el = (Else)part;
				else if(el != null)
					el = (Else)el.transform(part);
				else
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
		public java.lang.String toSource() {
			StringBuilder builder = new StringBuilder("else {");
			if(simpleimpl != null)
				builder.append(simpleimpl);
			else if(impl == null)
				builder.append("<unparsed>");
			else
				join(Arrays.asList(impl), ';', builder);
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
			throw new Error.JavaException("SyntaxError", "Unexpected " + part.toSource() + " after " + toString());
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
			throw new Error.JavaException("SyntaxError", "Unexpected " + toSource());
		}
	}
	public static class CloseBracket extends Helper {
		public CloseBracket() {}
		@Override
		public java.lang.String toSource() {
			return ")";
		}
	}
	public static class OpenGroup extends Referency {
		public static enum State {
			Idle,
			NeedKey,
			HaveKey,
			ReadingValue,
			Complete
		}
		
		State state = State.Idle;
		Map<java.lang.String, Parsed> entries = new HashMap();
		java.lang.String currentEntryKey;
		Parsed currentEntry;
		public OpenGroup() {}
		@Override
		public java.lang.String toSource() {
			StringBuilder builder = new StringBuilder("{");
			
			Iterator<Map.Entry<java.lang.String, Parsed>> it = entries.entrySet().iterator();
			if(it.hasNext()) {
				Map.Entry<java.lang.String, Parsed> entry = it.next();
				builder.append(entry.getKey());
				builder.append(':');
				builder.append(entry.getValue());
				while(it.hasNext()) {
					builder.append(',');
					entry = it.next();
					builder.append(entry.getKey());
					builder.append(':');
					builder.append(entry.getValue());
				}
			}
			
			if(state == State.Complete)
				builder.append('}');
			return builder.toString();
		}
		@Override
		public Parsed transform(Parsed part) {
			if(state == State.Complete)
				return super.transform(part);
			if(part instanceof NewLine)
				return this;
			
			switch(state) {
				case Idle:
					if(part instanceof CloseGroup) {
						state = State.Complete;
						return this;
					}
				case NeedKey:
					if(part instanceof Reference)
						currentEntryKey = ((Reference)part).ref;
					else if(part instanceof String)
						currentEntryKey = ((String)part).string;
					else if(part instanceof Number)
						currentEntryKey = net.nexustools.njs.Number.toString(((Number)part).value);
					else
						throw new Error.JavaException("SyntaxError", "Unexpected " + part.toSource());
					
					state = State.HaveKey;
					return this;
					
				case HaveKey:
					if(part instanceof Colon) {
						state = State.ReadingValue;
						return this;
					}
					
				case ReadingValue:
					if(currentEntry == null)
						currentEntry = part;
					else {
						if(!currentEntry.isIncomplete()) {
							if(part instanceof CloseGroup) {
								entries.put(currentEntryKey, currentEntry);
								currentEntry = null;
								state = State.Complete;
								return this;
							}
							if(part instanceof Comma) {
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
			
			
					
			
			throw new Error.JavaException("SyntaxError", "Unexpected " + part.toSource());
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
			return this;
		}
	}
	public static class CloseGroup extends Helper {
		public CloseGroup() {}
		@Override
		public java.lang.String toSource() {
			return "}";
		}
	}
	public static class OpenArray extends Referency {
		boolean closed;
		Parsed currentEntry;
		List<Parsed> entries = new ArrayList();
		public OpenArray() {}
		@Override
		public java.lang.String toSource() {
			StringBuilder builder = new StringBuilder("[");
			join(entries, ',', builder);
			if(closed)
				builder.append(']');
			return builder.toString();
		}
		@Override
		public Parsed transform(Parsed part) {
			if(closed)
				return super.transform(part);
			
			if(currentEntry == null) {
				if(part instanceof CloseArray)
					closed = true;
				else
					currentEntry = part;
				return this;
			} else {
				if(!currentEntry.isIncomplete()) {
					if(part instanceof Comma || part instanceof CloseArray) {
						currentEntry = currentEntry.finish();
						if(currentEntry != null) {
							if(currentEntry.isIncomplete())
								throw new Error.JavaException("SyntaxError", "Expected more after " + currentEntry);
							entries.add(currentEntry);
							currentEntry = null;
						}
						if(part instanceof CloseArray)
							closed = true;
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
	}
	public static class CloseArray extends Helper {
		public CloseArray() {}
		@Override
		public java.lang.String toSource() {
			return "]";
		}
	}
	public static class Comma extends Helper {
		public Comma() {}
		@Override
		public java.lang.String toSource() {
			return ",";
		}
	}
	public static class Null extends VolatileReferency {
		public Null() {}
		@Override
		public java.lang.String toSource() {
			return "null";
		}
	}
	public static class Undefined extends VolatileReferency {
		public Undefined() {}
		@Override
		public java.lang.String toSource() {
			return "undefined";
		}
	}
	public static class Function extends Parsed {
		public static enum State {
			BeforeName,
			BeforeArguments,
			InArguments,
			BeforeBody,
			InBody,
			Complete
		}
		
		boolean isYieldable;
		java.lang.String name;
		java.lang.String uname;
		Parsed currentArgumentPart;
		List<java.lang.String> arguments = new ArrayList();
		State state = State.BeforeName;
		java.lang.String source;
		ScriptData impl;
		public Function() {}
		@Override
		public java.lang.String toSource() {
			StringBuilder builder = new StringBuilder("function");
			if(isYieldable)
				builder.append('*');
			if(name != null) {
				builder.append(' ');
				builder.append(name);
			}
			builder.append('(');
			join(arguments, ',', builder);
			builder.append("){");
			if(impl == null)
				builder.append("<unparsed>");
			else
				join(Arrays.asList(impl), ';', builder);
			builder.append("}");
			return builder.toString();
		}
		@Override
		public java.lang.String toSimpleSource() {
			StringBuilder builder = new StringBuilder("function");
			if(isYieldable)
				builder.append('*');
			if(name != null) {
				builder.append(' ');
				builder.append(name);
			}
			builder.append('(');
			join(arguments, ',', builder);
			builder.append("){ ... }");
			return builder.toString();
		}
		@Override
		public Parsed transform(Parsed part) {
			if(part instanceof NewLine)
				return this;
			if(part instanceof SemiColon)
				return this;
			
			switch(state) {
				case BeforeName:
					if(part instanceof Multiply) {
						isYieldable = true;
						return this;
					}
					if(part instanceof Reference) {
						name = ((Reference)part).ref;
						state = State.BeforeArguments;
						return this;
					}
				case BeforeArguments:
					if(part instanceof OpenBracket) {
						state = State.InArguments;
						return this;
					}
					break;
					
				case InArguments:
					if(currentArgumentPart == null) {
						if(part instanceof CloseBracket) {
							state = State.BeforeBody;
							return this;
						}
						currentArgumentPart = part;
					} else {
						if(part instanceof CloseBracket) {
							arguments.add(((Reference)currentArgumentPart).ref);
							currentArgumentPart = null;
							state = State.BeforeBody;
							return this;
						}
						if(part instanceof Comma) {
							arguments.add(((Reference)currentArgumentPart).ref);
							currentArgumentPart = null;
							return this;
						}
					}
					return this;
					
				case BeforeBody:
					if(part instanceof OpenGroup) {
						state = State.InBody;
						throw new ParseFunction(this);
					}
					
				case InBody:
					if(part instanceof CloseGroup)
						throw new CompleteException(part);
					
				case Complete:
					throw new CompleteException(part);
			}
			
			throw new Error.JavaException("SyntaxError", "Unexpected " + part.toSimpleSource() + " (" + state + ')');
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
			return this;
		}
	}
	public static class SemiColon extends Helper {
		public SemiColon() {}
		@Override
		public java.lang.String toSource() {
			return ";";
		}
	}
	public static class NewLine extends Parsed {
		public NewLine() {}
		@Override
		public java.lang.String toSource() {
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
		public SetPlaceholder() {}
		@Override
		public java.lang.String toSource() {
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
			throw new Error.JavaException("SyntaxError", "Unexpected " + toSource());
		}
	}
	public static abstract class Rh extends Parsed {
		public Parsed rhs;
		public Rh(Parsed rhs) {
			this.rhs = rhs.finish();
		}
		public Rh() {
		}
		public abstract java.lang.String op();
		@Override
		public Parsed transform(Parsed part) {
			if(rhs == null) {
				if(!part.isStandalone())
					throw new net.nexustools.njs.Error.JavaException("SyntaxError", "Unexpected " + part.toSource() + " after " + describe(this));
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
			if(rhs != null)
				rhs = rhs.finish();
			return this;
		}
		@Override
		public java.lang.String toSource() {
			return op() + toLPadString(rhs);
		}
	}
	public static abstract class RhLh extends Rh {
		public final Parsed lhs;
		public RhLh() {
			lhs = null;
		}
		public RhLh(Parsed lhs) {
			this.lhs = lhs.finish();
		}
		public RhLh(Parsed lhs, Parsed rhs) {
			super(rhs);
			this.lhs = lhs.finish();
		}
		@Override
		public Parsed finish() {
			if(lhs == null)
				throw new net.nexustools.njs.Error.JavaException("SyntaxError", "Missing Left-Hand-Side (" + getClass().getSimpleName() + ')');
			return super.finish();
		}
		@Override
		public java.lang.String toSource() {
			return toRPadString(lhs) + op() + toLPadString(rhs);
		}
	}
	public static abstract class RhLhReferency extends RhLh implements BaseReferency {
		private boolean newline;
		
		public RhLhReferency() {}
		public RhLhReferency(Parsed lhs) {
			super(lhs);
		}
		public RhLhReferency(Parsed lhs, Parsed rhs) {
			super(lhs, rhs);
		}
		
		@Override
		public Parsed transform(Parsed part) {
			if(rhs == null) {
				if(part instanceof IntegerReference) {
					rhs = new OpenArray();
					((OpenArray)rhs).entries.add(new Number(((IntegerReference)part).ref));
					((OpenArray)rhs).closed = true;
				} else {
					if(!part.isStandalone())
						throw new net.nexustools.njs.Error.JavaException("SyntaxError", "Unexpected " + part.toSource() + " after " + describe(this));
					rhs = part;
				}
				return this;
			}
			
			if(rhs.isIncomplete()) {
				rhs = rhs.transform(part);
				return this;
			}
			
			if(part instanceof OpenBracket) {
				rhs = rhs.transform(part);
				return this;
			} else if(part instanceof OpenArray) {
				rhs = rhs.transform(part);
				return this;
			} else if(part instanceof DirectReference) {
				rhs = rhs.transform(part);
				return this;
			} else if(part instanceof IntegerReference) {
				rhs = rhs.transform(part);
				return this;
			}
			
			if(part instanceof SemiColon)
				throw new CompleteException();
			if(part instanceof NewLine) {
				newline = true;
				return this;
			}
			
			if(part instanceof InstanceOf)
				return new InstanceOf(this);
			else if(part instanceof MoreThan)
				return new MoreThan(this);
			else if(part instanceof LessThan)
				return new LessThan(this);
			else if(part instanceof MoreEqual)
				return new MoreEqual(this);
			else if(part instanceof LessEqual)
				return new LessEqual(this);
			else if(part instanceof StrictEquals)
				return new StrictEquals(this);
			else if(part instanceof NotEquals)
				return new NotEquals(this);
			else if(part instanceof StrictNotEquals)
				return new StrictNotEquals(this);
			else if(part instanceof Equals)
				return new Equals(this);
			else if(part instanceof Multiply) {
				if(this instanceof Divide)
					return new Multiply(this);
				rhs = new Multiply(rhs);
				return this;
			} else if(part instanceof Divide) {
				rhs = new Divide(rhs);
				return this;
			} else if(part instanceof Plus)
				return new Plus(this);
			else if(part instanceof Minus)
				return new Minus(this);
			else if(part instanceof Set)
				return new Set(this);
			else if(part instanceof Percent)
				return new Percent(this);
			else if(part instanceof And)
				return new And(this);
			else if(part instanceof Or)
				return new Or(this);
			else if(part instanceof AndAnd)
				return new AndAnd(this);
			else if(part instanceof OrOr)
				return new OrOr(this);
			else if(part instanceof Number && ((Number)part).value < 0)
				return new Minus(this, new Number(-((Number)part).value));
			else if(part instanceof Integer && ((Integer)part).value < 0)
				return new Minus(this, new Number(-((Integer)part).value));
			else
				return transformFallback(part);
		}

		public Parsed transformFallback(Parsed part) {
			if(newline)
				throw new CompleteException(part);
			
			throw new Error.JavaException("SyntaxError", "Unexpected " + part.toSource() + " after " + describe(this));
		}
		
	}
	public static abstract class RhReferency extends Rh implements BaseReferency {
		private boolean newline;
		
		public RhReferency() {}
		public RhReferency(Parsed rhs) {
			super(rhs);
		}
		
		@Override
		public Parsed transform(Parsed part) {
			if(rhs == null) {
				if(part instanceof IntegerReference) {
					rhs = new OpenArray();
					((OpenArray)rhs).entries.add(new Number(((IntegerReference)part).ref));
					((OpenArray)rhs).closed = true;
				} else {
					if(!part.isStandalone())
						throw new net.nexustools.njs.Error.JavaException("SyntaxError", "Unexpected " + part.toSource() + " after " + describe(this));
					rhs = part;
				}
				return this;
			}
			
			if(rhs.isIncomplete()) {
				rhs = rhs.transform(part);
				return this;
			}
			
			if(part instanceof OpenBracket) {
				rhs = rhs.transform(part);
				return this;
			} else if(part instanceof OpenArray) {
				rhs = rhs.transform(part);
				return this;
			} else if(part instanceof DirectReference) {
				rhs = rhs.transform(part);
				return this;
			} else if(part instanceof IntegerReference) {
				rhs = rhs.transform(part);
				return this;
			}
			
			if(part instanceof SemiColon)
				throw new CompleteException();
			if(part instanceof NewLine) {
				newline = true;
				return this;
			}
			
			if(part instanceof InstanceOf)
				return new InstanceOf(this);
			else if(part instanceof MoreThan)
				return new MoreThan(this);
			else if(part instanceof LessThan)
				return new LessThan(this);
			else if(part instanceof MoreEqual)
				return new MoreEqual(this);
			else if(part instanceof LessEqual)
				return new LessEqual(this);
			else if(part instanceof StrictEquals)
				return new StrictEquals(this);
			else if(part instanceof NotEquals)
				return new NotEquals(this);
			else if(part instanceof StrictNotEquals)
				return new StrictNotEquals(this);
			else if(part instanceof Equals)
				return new Equals(this);
			else if(part instanceof Multiply) {
				rhs = new Multiply(rhs);
				return this;
			} else if(part instanceof Divide) {
				rhs = new Divide(rhs);
				return this;
			} else if(part instanceof Plus)
				return new Plus(this);
			else if(part instanceof Minus)
				return new Minus(this);
			else if(part instanceof Set)
				return new Set(this);
			else if(part instanceof Percent)
				return new Percent(this);
			else if(part instanceof And)
				return new And(this);
			else if(part instanceof Or)
				return new Or(this);
			else if(part instanceof AndAnd)
				return new AndAnd(this);
			else if(part instanceof OrOr)
				return new OrOr(this);
			else if(part instanceof Number && ((Number)part).value < 0)
				return new Minus(this, new Number(-((Number)part).value));
			else if(part instanceof Integer && ((Integer)part).value < 0)
				return new Minus(this, new Number(-((Integer)part).value));
			else
				return transformFallback(part);
		}

		public Parsed transformFallback(Parsed part) {
			if(newline)
				throw new CompleteException(part);
			
			throw new Error.JavaException("SyntaxError", "Unexpected " + part.toSource() + " after " + describe(this));
		}
		
	}
	public static class Not extends Rh {

		@Override
		public java.lang.String op() {
			return "!";
		}
		
	}
	public static class Set extends RhLh {
		public Set() {}
		public Set(Parsed lhs) {
			super(lhs);
			rows = lhs.rows;
			columns = lhs.columns;
		}
		@Override
		public java.lang.String op() {
			return "=";
		}
		@Override
		public Parsed finish() {
			Parsed myself = super.finish();
			if(rhs instanceof Function && ((Function)rhs).name == null)
				((Function)rhs).name = lhs.toSource();
			return myself;
		}
	}
	public static class MultiplyEq extends RhLh {
		public MultiplyEq() {}
		public MultiplyEq(Parsed lhs) {
			super(lhs);
			rows = lhs.rows;
			columns = lhs.columns;
		}
		@Override
		public java.lang.String op() {
			return "*=";
		}
	}
	public static class Multiply extends RhLhReferency implements NumberReferency {
		public Multiply() {}
		public Multiply(Parsed lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "*";
		}
	}
	public static class Divide extends RhLhReferency implements NumberReferency {
		public Divide() {}
		public Divide(Parsed lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "/";
		}
	}
	public static class Equals extends RhLh {
		public Equals() {}
		public Equals(Parsed lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "==";
		}
	}
	public static class NotEquals extends RhLh {
		public NotEquals() {}
		public NotEquals(Parsed lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "!=";
		}
	}
	public static class StrictNotEquals extends RhLh {
		public StrictNotEquals() {}
		public StrictNotEquals(Parsed lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "!==";
		}
	}
	public static class StrictEquals extends RhLh {
		public StrictEquals() {}
		public StrictEquals(Parsed lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "===";
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
	}
	public static class Plus extends RhLhReferency {
		public Plus() {
			super(new Number(0));
		}
		public Plus(Parsed lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "+";
		}

		public boolean isStringReferenceChain() {
			Parsed l = rhs;
			while(l instanceof Plus)
				l = ((Plus)l).rhs;
			if(l instanceof StringReferency)
				return true;
			l = lhs;
			while(l instanceof Plus)
				l = ((Plus)l).lhs;
			return l instanceof StringReferency;
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
	}
	public static class InstanceOf extends RhLh {
		public InstanceOf() {}
		public InstanceOf(Parsed lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "instanceof";
		}
	}
	public static class PlusEq extends RhLh {
		public PlusEq() {}
		public PlusEq(Parsed lhs) {
			super(lhs);
			rows = lhs.rows;
			columns = lhs.columns;
		}
		@Override
		public java.lang.String op() {
			return "+=";
		}
	}
	public static class PlusPlus extends Referency implements NumberReferency {
		Parsed ref;
		boolean right;
		public PlusPlus() {}
		public PlusPlus(Parsed ref) {
			this.ref = ref;
			rows = ref.rows;
			columns = ref.columns;
			right = true;
		}

		@Override
		public Parsed transform(Parsed part) {
			if(right)
				return super.transform(part);
			
			if(ref == null) {
				ref = part;
				return this;
			} else if(ref.isIncomplete()) {
				ref = ref.transform(part);
				return this;
			}
			
			return super.transform(part);
		}

		@Override
		public Parsed transformFallback(Parsed part) {
			if(right)
				throw new CompleteException(part);
			if(ref == null)
				ref = part;
			else
				ref = ref.transform(part);
			return this;
		}

		@Override
		public boolean isIncomplete() {
			return ref == null || ref.isIncomplete();
		}

		@Override
		public Parsed finish() {
			if(!right)
				ref = ref.finish();
			return this;
		}
		
		@Override
		public java.lang.String toSource() {
			StringBuilder builder = new StringBuilder();
			if(right)
				builder.append(unparsed(ref));
			builder.append("++");
			if(!right)
				builder.append(unparsed(ref));
			return builder.toString();
		}
		
	}
	public static class MinusMinus extends Referency implements NumberReferency {
		Parsed ref;
		boolean right;
		public MinusMinus() {}
		public MinusMinus(Parsed ref) {
			this.ref = ref;
			rows = ref.rows;
			columns = ref.columns;
			right = true;
		}

		@Override
		public Parsed transform(Parsed part) {
			if(right)
				return super.transform(part);
			
			if(ref == null) {
				ref = part;
				return this;
			} else if(ref.isIncomplete()) {
				ref = ref.transform(part);
				return this;
			}
			
			return super.transform(part);
		}

		@Override
		public Parsed transformFallback(Parsed part) {
			if(right)
				throw new CompleteException(part);
			if(ref == null)
				ref = part;
			else
				ref = ref.transform(part);
			return this;
		}

		@Override
		public boolean isIncomplete() {
			return ref == null || ref.isIncomplete();
		}

		@Override
		public Parsed finish() {
			if(!right)
				ref = ref.finish();
			return this;
		}
		
		@Override
		public java.lang.String toSource() {
			StringBuilder builder = new StringBuilder();
			if(right)
				builder.append(unparsed(ref));
			builder.append("--");
			if(!right)
				builder.append(unparsed(ref));
			return builder.toString();
		}
		
	}
	public static class MoreThan extends RhLh {
		public MoreThan() {}
		public MoreThan(Parsed lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return ">";
		}
	}
	public static class LessThan extends RhLh {
		public LessThan() {}
		public LessThan(Parsed lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "<";
		}
	}
	public static class MoreEqual extends RhLh {
		public MoreEqual() {}
		public MoreEqual(Parsed lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return ">=";
		}
	}
	public static class LessEqual extends RhLh {
		public LessEqual() {}
		public LessEqual(Parsed lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "<=";
		}
	}
	public static class In extends Helper {
		@Override
		public java.lang.String toSource() {
			return "in";
		}
	}
	public static class Of extends Helper {
		@Override
		public java.lang.String toSource() {
			return "of";
		}
	}
	public static class New extends Referency {
		boolean closed;
		Parsed reference;
		List<Parsed> arguments;
		Parsed currentPart;
		public New() {}
		@Override
		public Parsed transform(Parsed part) {
			if(closed) 
				return super.transform(part);
			if(part instanceof NewLine)
				return this;
			
			if(reference == null)
				reference = part;
			else if(reference.isIncomplete())
				reference = reference.transform(part);
			else if(arguments != null) {
				if(currentPart == null) {
					if(part instanceof CloseBracket) {
						closed = true;
						return this;
					}
					currentPart = part;
				} else {
					if(!currentPart.isIncomplete()) {
						if(part instanceof CloseBracket) {
							arguments.add(currentPart);
							currentPart = null;
							closed = true;
							return this;
						}
						if(part instanceof Comma) {
							arguments.add(currentPart);
							currentPart = null;
							return this;
						}
					}
					currentPart = currentPart.transform(part);
				}
			} else if(part instanceof OpenBracket)
				arguments = new ArrayList();
			else
				reference = reference.transform(part);
			return this;
		}
		@Override
		public java.lang.String toSource() {
			StringBuilder buffer = new StringBuilder("new ");
			buffer.append(reference);
			if(arguments != null) {
				buffer.append('(');
				join(arguments, ',', buffer);
				if(closed)
					buffer.append(')');
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
			if(arguments != null) {
				ListIterator<Parsed> it = arguments.listIterator();
				while(it.hasNext())
					it.set(it.next().finish());
			}
			if(currentPart != null)
				throw new Error.JavaException("SyntaxError", "Unexpected EOF");
			return this;
		}
	}
	public static class RegEx extends Referency {
		
		final java.lang.String pattern, flags;
		public RegEx(java.lang.String pattern, java.lang.String flags) {
			this.pattern = pattern;
			this.flags = flags;
		}

		@Override
		public java.lang.String toSource() {
			return '/' + pattern + '/' + flags;
		}
		
	}
	public static class Var extends Parsed {
		public static class Set {
			public final java.lang.String lhs;
			public Parsed rhs;

			private Set(java.lang.String ref) {
				lhs = ref;
			}

			@Override
			public java.lang.String toString() {
				if(rhs != null)
					return lhs + " = " + rhs.toSource();
				return lhs;
			}
		}
		Set currentSet;
		List<Set> sets = new ArrayList();
		public Var() {}
		@Override
		public Parsed transform(Parsed part) {
			if(currentSet == null) {
				if(part instanceof Reference) {
					currentSet = new Set(((Reference)part).ref);
					return this;
				}
			} else if(currentSet.rhs != null) {
				if(!currentSet.rhs.isIncomplete()) {
					if(part instanceof Comma) {
						sets.add(currentSet);
						currentSet = null;
						return this;
					} else if(part instanceof SemiColon) {
						sets.add(currentSet);
						currentSet = null;
						throw new CompleteException();
					}
				}
				
				currentSet.rhs = currentSet.rhs.transform(part);
				return this;
			} else if(part instanceof RegexCompiler.Set) {
				currentSet.rhs = new SetPlaceholder();
				return this;
			} else if(part instanceof Comma) {
				sets.add(currentSet);
				currentSet = null;
				return this;
			} else if(part instanceof SemiColon)
				throw new CompleteException();
				
			throw new Error.JavaException("SyntaxError", "Unexpected " + part.toSource());
		}
		@Override
		public java.lang.String toSource() {
			StringBuilder builder = new StringBuilder();
			builder.append(getClass().getSimpleName().toLowerCase());
			builder.append(' ');
			join(sets, ',', builder);
			if(currentSet != null) {
				if(!sets.isEmpty())
					builder.append(", ");
				builder.append(currentSet.lhs);
				if(currentSet.rhs != null) {
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
			return (currentSet == null && sets.isEmpty()) || (currentSet != null && currentSet.rhs != null && currentSet.rhs.isIncomplete());
		}
		@Override
		public Parsed finish() {
			if(currentSet != null) {
				if(currentSet.rhs != null && currentSet.rhs.isIncomplete())
					return this;
				
				sets.add(currentSet);
				currentSet = null;
			}
			for(Set set : sets) {
				if(set.rhs != null) {
					set.rhs = set.rhs.finish();
					if(set.rhs instanceof Function && ((Function)set.rhs).name == null)
						((Function)set.rhs).name = set.lhs;
				}
			}
			return this;
		}
	}
	public static class Let extends Var {
		
	}
	public static class String extends PrimitiveReferency implements StringReferency {
		public final java.lang.String string;
		public String(java.lang.String string) {
			assert(string != null);
			this.string = string.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
		}

		@Override
		public java.lang.String toSource() {
			return '"' + convertStringSource(string) + '"';
		}
		
	}
	public static class Integer extends PrimitiveReferency implements NumberReferency {
		public final int value;
		public Integer(int value) {
			this.value = value;
		}
		@Override
		public java.lang.String toSource() {
			return java.lang.String.valueOf(value);
		}
	}
	public static class Number extends PrimitiveReferency implements NumberReferency {
		public final double value;
		public Number(double value) {
			this.value = value;
		}
		@Override
		public java.lang.String toSource() {
			return java.lang.String.valueOf(value);
		}
		@Override
		public Referency extend(DirectReference reference) {
			throw new net.nexustools.njs.Error.JavaException("SyntaxError", "Unexpected " + reference);
		}
		@Override
		public Referency extend(IntegerReference reference) {
			throw new net.nexustools.njs.Error.JavaException("SyntaxError", "Unexpected " + reference);
		}
	}
	
	public static final java.lang.String NUMBER_REG = "\\-?\\d+(\\.\\d+)?([eE][\\+\\-]?\\d+)?";
	public static final java.lang.String STRING_REG = "(([\"'])(?:(?:\\\\\\\\)|\\\\\\2|(?!\\\\\\2)\\\\|(?!\\2).|[\\n\\r])*\\2)";
	public static final java.lang.String VARIABLE_NAME = "[_$a-zA-Z\\xA0-\\uFFFF][_$a-zA-Z0-9\\xA0-\\uFFFF]*";
	public static final Pattern MULTILINE_COMMENT = Pattern.compile("^(\\/\\*(?:(?!\\*\\/).|[\\n\\r])*\\*\\/)");
	public static final Pattern SINGLELINE_COMMENT = Pattern.compile("^(\\/\\/[^\\n\\r]*([\\n\\r]+|$))");
	public static final Pattern REGEX = Pattern.compile("^/([^/]*[^\\\\])/([gi]*)");
	public static final Pattern STRING = Pattern.compile("^" + STRING_REG);
	public static final Pattern NUMBERGET = Pattern.compile("^\\[(" + NUMBER_REG + ")\\]");
	public static final Pattern STRINGGET = Pattern.compile("^\\[" + STRING_REG + "\\]");
	public static final Pattern VAR = Pattern.compile("^var\\s+(" + VARIABLE_NAME + ")(\\s*,\\s*" + VARIABLE_NAME + ")*");
	public static final Pattern NUMBER = Pattern.compile("^" + NUMBER_REG);
	public static final Pattern VARIABLEGET = Pattern.compile("^\\.\\s*(" + VARIABLE_NAME + ')');
	public static final Pattern VARIABLE = Pattern.compile("^" + VARIABLE_NAME);
	public static final Pattern CLOSE_BRACKET = Pattern.compile("^\\)");
	public static final Pattern OPEN_BRACKET = Pattern.compile("^\\(");
	public static final Pattern WHITESPACE = Pattern.compile("^\\s+");
	public static final Pattern MULTIPLYEQ = Pattern.compile("^\\*=");
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
	public static final Pattern MORETHAN = Pattern.compile("^\\>");
	public static final Pattern LESSTHAN = Pattern.compile("^\\<");
	public static final Pattern MOREEQUAL = Pattern.compile("^\\>=");
	public static final Pattern LESSEQUAL = Pattern.compile("^\\<=");
	public static final Pattern PLUSEQ = Pattern.compile("^\\+=");
	public static final Pattern SEMICOLON = Pattern.compile("^;");
	public static final Pattern NOTEQUALS = Pattern.compile("^!=");
	public static final Pattern EQUALS = Pattern.compile("^==");
	public static final Pattern NEWLINE = Pattern.compile("^\n");
	public static final Pattern ACCESS = Pattern.compile("^\\[");
	public static final Pattern PERCENT = Pattern.compile("^%");
	public static final Pattern OR = Pattern.compile("^\\|");
	public static final Pattern AND = Pattern.compile("^&");
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
			if(currentBuffer.isEmpty())
				return more();
			return currentBuffer;
		}
		public java.lang.String more() throws IOException {
			if(currentBuffer.isEmpty()) {
				int read = reader.read(buffer);
				if(read > 0)
					return currentBuffer = new java.lang.String(buffer, 0, read);
				return null;
			}
			
			int untilFull = buffer.length-currentBuffer.length();
			if(untilFull == 0)
				return null;
			
			int read = reader.read(buffer, 0, untilFull);
			if(read > 0) {
				currentBuffer += new java.lang.String(buffer, 0, read);
				return currentBuffer;
			}
			return null;
		}
		private int countInstances(java.lang.String in, char of) {
			int pos=-1, count=0;
			while((pos = in.indexOf(of, pos+1)) > -1) {
				count++;
			}
			return count;
		}
		private void processChopped(java.lang.String chop) {
			int pos = chop.indexOf('\n');
			if(pos > -1) {
				do {
					rows ++;
					chop = chop.substring(pos+1);
				} while((pos = chop.indexOf('\n')) > -1);
				columns = 1 + chop.length();
			} else
				columns += chop.length();
		}
		public java.lang.String ltrim(int len) {
			if(currentBuffer.length() <= len)
				try {
					processChopped(currentBuffer);
					return currentBuffer;
				} finally {
					currentBuffer = "";
				}
			else
				try {
					java.lang.String chop = currentBuffer.substring(0, len);
					processChopped(chop);
					return chop;
				} finally {
					currentBuffer = currentBuffer.substring(len);
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
				assert(part != null);
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
			return toString.substring(toString.lastIndexOf('$')+1);
		}
		
		public final ScriptData parse(ParserReader reader) throws IOException {
			return parse(reader, new StringBuilder());
		}
		
		public final ScriptData parse(ParserReader reader, StringBuilder builder) throws IOException {
			if(DEBUG)
				System.out.println("[" + this + "] Parsing...");
			FunctionParser functionParser = this instanceof FunctionParser ? (FunctionParser)this : new FunctionParser();
			BlockParser blockParser = this instanceof BlockParser ? (BlockParser)this : new BlockParser(this == functionParser);
			java.lang.String buffer;
			Matcher matcher;
			
			final int sRows = reader.rows;
			final int sColumns = reader.columns;
			
			Parsed currentPart = null;
			List<Parsed> parts = new ArrayList();
			while(true) {
				buffer = reader.current();
				if(buffer == null) {
					eof();
					break;
				}
				next:
				while(true) {
					for(Pattern pat : patterns) {
						matcher = pat.matcher(buffer);
						if(matcher.find()) {
							int rows = reader.rows;
							int columns = reader.columns;
							try {
								match(pat, matcher, reader);
								builder.append(reader.ltrim(matcher.group().length()));
							} catch(PartExchange part) {
								part.part.rows = rows;
								part.part.columns = columns;
								if(currentPart != null) {
									if(!currentPart.isIncomplete() && part.part instanceof CloseGroup) {
										currentPart = currentPart.finish();
										if(currentPart != null) {
											if(!currentPart.isStandalone())
												throw new Error.JavaException("SyntaxError", "Unexpected " + currentPart);
											if(currentPart.isIncomplete())
												throw new Error.JavaException("SyntaxError", "Expected more after " + currentPart);
											parts.add(currentPart);
										}
										if(DEBUG)
											System.out.println("[" + this + "] End...");
										end(sRows, sColumns, parts, builder.toString());
									}
									
									try {
										if(DEBUG)
											System.out.println("[" + this + "] " + describe(currentPart) + " -> " + describe(part.part));
										currentPart = currentPart.transform(part.part);
									} catch(CompleteException ex) {
										if(DEBUG) {
											System.out.println("[" + this + "] Complete: " + ex);
											ex.printStackTrace(System.out);
										}
										currentPart = currentPart.finish();
										if(currentPart != null) {
											if(!currentPart.isStandalone())
												throw new Error.JavaException("SyntaxError", "Unexpected " + currentPart);
											if(currentPart.isIncomplete())
												throw new Error.JavaException("SyntaxError", "Expected more after " + currentPart);
											parts.add(currentPart);
										}
										if(ex.part != null) {
											if(ex.part instanceof CloseGroup) {
												if(DEBUG)
													System.out.println("[" + this + "] End...");
												end(sRows, sColumns, parts, builder.toString());
											}
											if(!ex.part.isStandalone()) {
												if(ex.part instanceof SemiColon) {
													builder.append(reader.ltrim(part.trim));
													currentPart = null;
													break next;
												}
												throw new net.nexustools.njs.Error.JavaException("SyntaxError", "Unexpected " + ex.part);
											}
										}
										currentPart = ex.part;
									} catch(ParseFunction ex) {
										builder.append(reader.ltrim(part.trim));
										try {
											functionParser.parse(reader);
										} catch(ParseComplete ce) {
											ex.function.impl = ce.impl;
											ex.function.impl.callee = ex.function;
											ex.function.source = ce.source;
											ex.function.state = Function.State.Complete;
											builder.append(ce.source);
											builder.append(reader.ltrim(1));
										}
										break next;
									} catch(ParseBlock ex) {
										builder.append(reader.ltrim(part.trim));
										try {
											blockParser.parse(reader);
										} catch(ParseComplete ce) {
											ex.block.impl = ce.impl;
											ex.block.state = Block.State.Complete;
											builder.append(ce.source);
											builder.append(reader.ltrim(1));
										}
										break next;
									}
								} else {
									if(part.part instanceof CloseGroup) {
										if(DEBUG)
											System.out.println("[" + this + "] End...");
										end(sRows, sColumns, parts, builder.toString());
									}
									currentPart = part.part;
								}
								builder.append(reader.ltrim(part.trim));
							} catch(PartComplete part) {
								currentPart = currentPart.finish();
								if(currentPart != null) {
									if(currentPart instanceof CloseGroup) {
										if(DEBUG)
											System.out.println("[" + this + "] End...");
										end(sRows, sColumns, parts, builder.toString());
									}
									if(!currentPart.isStandalone())
										throw new Error.JavaException("SyntaxError", "Unexpected " + currentPart);
									if(currentPart.isIncomplete())
										throw new Error.JavaException("SyntaxError", "Expected more after " + currentPart);
									parts.add(currentPart);
									currentPart = null;
								}
								builder.append(reader.ltrim(part.trim));
							}
							break next;
						}
					}
					java.lang.String next;
					if((next = reader.more()) == null)
						throw new Error.JavaException("SyntaxError", "No matching patterns for " + this + ": " + buffer);
					buffer = next;
				}
			}
			if(currentPart != null) {
				currentPart = currentPart.finish();
				if(currentPart != null) {
					if(!currentPart.isStandalone())
						throw new Error.JavaException("SyntaxError", "Unexpected " + currentPart);
					if(currentPart.isIncomplete())
						throw new Error.JavaException("SyntaxError", "Expected more after " + currentPart);
					parts.add(currentPart);
					currentPart = null;
				}
			}
			return new ScriptData(parts.toArray(new Parsed[parts.size()]), builder.toString(), 1, 1);
		}
		
		public void end(int rows, int columns, List<Parsed> parts, java.lang.String source) {
			throw new Error.JavaException("SyntaxError", "Unexpected }");
		}
		
		public abstract void match(Pattern pattern, Matcher matcher, ParserReader reader);
		public abstract void eof();
	}
	public static class ScriptParser extends RegexParser {
		public ScriptParser() {
			super(NOTSTRICTEQUALS, NOTEQUALS, STRICTEQUALS, EQUALS, COLON, MOREEQUAL, LESSEQUAL, MORETHAN, LESSTHAN, COMMA, NUMBERGET, STRINGGET, NOT, ANDAND, OROR, AND, OR, PERCENT, SET, PLUSPLUS, MINUSMINUS, PLUSEQ, MULTIPLYEQ, PLUS, MINUS, MULTIPLY, SEMICOLON, NEWLINE, NUMBER, VARIABLE, VARIABLEGET, SINGLELINE_COMMENT, MULTILINE_COMMENT, DIVIDE, WHITESPACE, STRING, OPEN_GROUP, CLOSE_GROUP, OPEN_BRACKET, CLOSE_BRACKET, VAR, OPEN_ARRAY, CLOSE_ARRAY, REGEX);
		}
		@Override
		public void match(Pattern pattern, Matcher matcher, ParserReader reader) {
			if(pattern == WHITESPACE || pattern == MULTILINE_COMMENT || pattern == SINGLELINE_COMMENT)
				return; // Ignored
			
			if(pattern == STRING) {
				java.lang.String string = matcher.group(1);
				throw new PartExchange(new String(string.substring(1, string.length()-1)), matcher.group().length());
			}
			if(pattern == NUMBER) {
				try {
					throw new PartExchange(new Integer(java.lang.Integer.valueOf(matcher.group(0))), matcher.group().length());
				} catch(NumberFormatException ex) {
					try {
						throw new PartExchange(new Number(Double.valueOf(matcher.group(0))), matcher.group().length());
					} catch(NumberFormatException eex) {
						throw new PartExchange(new Number(Double.NaN), matcher.group().length());
					}
				}
			}
			if(pattern == VARIABLE) {
				java.lang.String ref = matcher.group();
				if(ref.equals("in"))
					throw new PartExchange(new In(), ref.length());
				else if(ref.equals("of"))
					throw new PartExchange(new Of(), ref.length());
				else if(ref.equals("new"))
					throw new PartExchange(new New(), ref.length());
				else if(ref.equals("null"))
					throw new PartExchange(new Null(), ref.length());
				else if(ref.equals("undefined"))
					throw new PartExchange(new Undefined(), ref.length());
				else if(ref.equals("function"))
					throw new PartExchange(new Function(), ref.length());
				else if(ref.equals("while"))
					throw new PartExchange(new While(), ref.length());
				else if(ref.equals("yield"))
					throw new PartExchange(new Yield(), ref.length());
				else if(ref.equals("instanceof"))
					throw new PartExchange(new InstanceOf(), ref.length());
				else if(ref.equals("if"))
					throw new PartExchange(new If(), ref.length());
				else if(ref.equals("else"))
					throw new PartExchange(new Else(), ref.length());
				else if(ref.equals("delete"))
					throw new PartExchange(new Delete(), ref.length());
				else if(ref.equals("for"))
					throw new PartExchange(new For(), ref.length());
				else if(ref.equals("try"))
					throw new PartExchange(new Try(), ref.length());
				else if(ref.equals("catch"))
					throw new PartExchange(new Catch(), ref.length());
				else if(ref.equals("finally"))
					throw new PartExchange(new Finally(), ref.length());
				else if(ref.equals("true"))
					throw new PartExchange(new Boolean(true), ref.length());
				else if(ref.equals("false"))
					throw new PartExchange(new Boolean(false), ref.length());
				else if(ref.equals("throw"))
					throw new PartExchange(new Throw(), ref.length());
				else if(ref.equals("switch"))
					throw new PartExchange(new Switch(), ref.length());
				else if(ref.equals("typeof"))
					throw new PartExchange(new TypeOf(), ref.length());
				else if(ref.equals("case"))
					throw new PartExchange(new Case(), ref.length());
				else if(ref.equals("return"))
					ret(matcher);
				else if(ref.equals("var"))
					throw new PartExchange(new Var(), ref.length());
				else if(ref.equals("let"))
					throw new PartExchange(new Let(), ref.length());
				else
					throw new PartExchange(new Reference(ref), ref.length());
			}
			if(pattern == VARIABLEGET)
				throw new PartExchange(new DirectReference(matcher.group(1)), matcher.group().length());
			if(pattern == OPEN_BRACKET)
				throw new PartExchange(new OpenBracket(), matcher.group().length());
			if(pattern == CLOSE_BRACKET)
				throw new PartExchange(new CloseBracket(), matcher.group().length());
			if(pattern == OPEN_GROUP)
				throw new PartExchange(new OpenGroup(), matcher.group().length());
			if(pattern == CLOSE_GROUP)
				throw new PartExchange(new CloseGroup(), matcher.group().length());
			if(pattern == OPEN_ARRAY)
				throw new PartExchange(new OpenArray(), matcher.group().length());
			if(pattern == CLOSE_ARRAY)
				throw new PartExchange(new CloseArray(), matcher.group().length());
			
			if(pattern == REGEX)
				throw new PartExchange(new RegEx(matcher.group(1), matcher.group(2)), matcher.group().length());
			
			if(pattern == NOT)
				throw new PartExchange(new Not(), matcher.group().length());
			if(pattern == PLUS)
				throw new PartExchange(new Plus(), matcher.group().length());
			if(pattern == MINUS)
				throw new PartExchange(new Minus(), matcher.group().length());
			if(pattern == DIVIDE)
				throw new PartExchange(new Divide(), matcher.group().length());
			if(pattern == MULTIPLY)
				throw new PartExchange(new Multiply(), matcher.group().length());
			if(pattern == EQUALS)
				throw new PartExchange(new Equals(), matcher.group().length());
			if(pattern == STRICTEQUALS)
				throw new PartExchange(new StrictEquals(), matcher.group().length());
			if(pattern == NOTEQUALS)
				throw new PartExchange(new NotEquals(), matcher.group().length());
			if(pattern == NOTSTRICTEQUALS)
				throw new PartExchange(new StrictNotEquals(), matcher.group().length());
			
			if(pattern == PLUSEQ)
				throw new PartExchange(new PlusEq(), matcher.group().length());
			if(pattern == MULTIPLYEQ)
				throw new PartExchange(new MultiplyEq(), matcher.group().length());
			if(pattern == PLUSPLUS)
				throw new PartExchange(new PlusPlus(), matcher.group().length());
			if(pattern == MINUSMINUS)
				throw new PartExchange(new MinusMinus(), matcher.group().length());
			
			if(pattern == MORETHAN)
				throw new PartExchange(new MoreThan(), matcher.group().length());
			if(pattern == LESSTHAN)
				throw new PartExchange(new LessThan(), matcher.group().length());
			if(pattern == MOREEQUAL)
				throw new PartExchange(new MoreEqual(), matcher.group().length());
			if(pattern == LESSEQUAL)
				throw new PartExchange(new LessEqual(), matcher.group().length());
			
			if(pattern == SET)
				throw new PartExchange(new Set(), matcher.group().length());
			if(pattern == OROR)
				throw new PartExchange(new OrOr(), matcher.group().length());
			if(pattern == ANDAND)
				throw new PartExchange(new AndAnd(), matcher.group().length());
			if(pattern == PERCENT)
				throw new PartExchange(new Percent(), matcher.group().length());
			if(pattern == OR)
				throw new PartExchange(new Or(), matcher.group().length());
			if(pattern == AND)
				throw new PartExchange(new And(), matcher.group().length());
			if(pattern == COLON)
				throw new PartExchange(new Colon(), matcher.group().length());
			
			if(pattern == NUMBERGET) {
				try {
					throw new PartExchange(new IntegerReference(java.lang.Integer.valueOf(matcher.group(1))), matcher.group().length());
				} catch(NumberFormatException ex) {
					throw new PartExchange(new DirectReference(matcher.group(1)), matcher.group().length());
				}
			}
			
			if(pattern == STRINGGET) {
				java.lang.String string = matcher.group(1);
				throw new PartExchange(new DirectReference(string.substring(1, string.length()-1)), matcher.group().length());
			}
			
			if(pattern == SEMICOLON)
				throw new PartExchange(new SemiColon(), matcher.group().length());
			if(pattern == NEWLINE)
				throw new PartExchange(new NewLine(), matcher.group().length());
			if(pattern == COMMA)
				throw new PartExchange(new Comma(), matcher.group().length());
			
			System.out.println(pattern);
			System.out.println(matcher.groupCount());
			for(int i=0; i<=matcher.groupCount(); i++)
				System.out.println(matcher.group(i));
		}
		
		public void ret(Matcher matcher) {
			throw new Error.JavaException("SyntaxError", "Illegal return statement");
		}

		@Override
		public void eof() {}
	}
	public static class BlockParser extends ScriptParser {
		final boolean inFunction;
		public BlockParser(boolean inFunction) {
			this.inFunction = inFunction;
		}
		@Override
		public void end(int rows, int columns, List<Parsed> parts, java.lang.String source) {
			throw new ParseComplete(new ScriptData(parts.toArray(new Parsed[parts.size()]), source, rows, columns), source);
		}
		@Override
		public void ret(Matcher matcher) {
			if(inFunction)
				throw new PartExchange(new Return(), matcher.group().length());
			super.ret(matcher);
		}
	}
	public static class FunctionParser extends ScriptParser {
		@Override
		public void ret(Matcher matcher) {
			throw new PartExchange(new Return(), matcher.group().length());
		}
		@Override
		public void end(int rows, int columns, List<Parsed> parts, java.lang.String source) {
			throw new ParseComplete(new ScriptData(parts.toArray(new Parsed[parts.size()]), source, rows, columns), source);
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
			if(DEBUG)
				System.out.println("Compiling " + join(Arrays.asList(script), ';'));
			return script;
		} catch(net.nexustools.njs.Error.JavaException ex) {
			if(ex.type.equals("SyntaxError") && reader != null) {
				StringBuilder builder = new StringBuilder(ex.getUnderlyingMessage());
				builder.append(" (");
				if(fileName != null) {
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
		} catch(IOException ex) {
			throw new Error.JavaException("EvalError", "IO Exception While Evaluating Script: " + ex.getMessage(), ex);
		}
	}

	@Override
	public final Script compile(Reader source, java.lang.String fileName, boolean inFunction) {
		return compileScript(parse(source, fileName, inFunction), fileName, inFunction);
	}
	
	protected abstract Script compileScript(ScriptData script, java.lang.String fileName, boolean inFunction);
	
}
