/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
 * @author kate
 */
public abstract class AbstractCompiler implements Compiler {
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
	public static class CompleteException extends RuntimeException {
		final Part part;
		public CompleteException() {
			this(null);
		}
		public CompleteException(Part part) {
			this.part = part;
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
		public final java.lang.Object[] impl;
		public final java.lang.String source;
		public ParseComplete(java.lang.Object[] impl, java.lang.String source) {
			this.impl = impl;
			this.source = source;
		}
	}
	public static interface Part {
		public Part transform(Part part);
		public boolean isStandalone();
		public boolean isIncomplete();
		public Part finish();
	}
	public static abstract class Referency implements Part {
		public boolean newline;
		public abstract Referency extend(DirectReference reference);
		@Override
		public Part transform(Part part) {
			if(part instanceof SemiColon)
				throw new CompleteException();
			if(part instanceof NewLine)
				newline = true;
			
			if(part instanceof DirectReference) {
				return extend((DirectReference)part);
			} else if(part instanceof OpenBracket)
				return new Call(this);
			else if(part instanceof InstanceOf)
				return new InstanceOf(this);
			else if(part instanceof MultiplyEq)
				return new MultiplyEq(this);
			else if(part instanceof PlusEq)
				return new PlusEq(this);
			else if(part instanceof PlusPlus)
				return new PlusPlus(this);
			else if(part instanceof MoreThan)
				return new MoreThan(this);
			else if(part instanceof StrictEquals)
				return new StrictEquals(this);
			else if(part instanceof NotEquals)
				return new NotEquals(this);
			else if(part instanceof NotStrictEquals)
				return new NotStrictEquals(this);
			else if(part instanceof Equals)
				return new Equals(this);
			else if(part instanceof LessThan)
				return new LessThan(this);
			else if(part instanceof Multiply)
				return new Multiply(this);
			else if(part instanceof Plus)
				return new Plus(this);
			else if(part instanceof Set)
				return new Set(this);
			else if(part instanceof And)
				return new And(this);
			else if(part instanceof Or)
				return new Or(this);
			
			if(newline)
				throw new CompleteException(part);
			
			throw new Error.JavaException("SyntaxError", "Unexpected " + part);
		}

		@Override
		public boolean isIncomplete() {
			return false;
		}

		@Override
		public boolean isStandalone() {
			return true;
		}
		
		public Part finish() {
			return this;
		}
	}
	public static class Reference extends Referency {
		public final java.lang.String ref;
		public Reference(java.lang.String ref) {
			this.ref = ref;
		}
		@Override
		public java.lang.String toString() {
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
	public static class Or extends RhLh {
		public Or() {}
		public Or(Part lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "||";
		}
	}
	public static class And extends RhLh {
		public And() {}
		public And(Part lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "&&";
		}
	}
	public static class Colon implements Part {
		public Colon() {}
		
		@Override
		public Part transform(Part part) {
			throw new Error.JavaException("SyntaxError", "Unexpected " + part);
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
		public Part finish() {
			return null;
		}

		@Override
		public java.lang.String toString() {
			return ":";
		}
		
		
	}
	public static class DirectReference implements Part {
		public final java.lang.String ref;
		public DirectReference(java.lang.String ref) {
			this.ref = ref;
		}
		@Override
		public Part transform(Part part) {
			throw new Error.JavaException("SyntaxError", "Unexpected " + part);
		}
		@Override
		public java.lang.String toString() {
			return '.' + ref;
		}
		@Override
		public boolean isStandalone() {
			return false;
		}
		@Override
		public boolean isIncomplete() {
			return true;
		}
		public Part finish() {
			return this;
		}
	}
	public static class RightReference extends Referency {
		public final Part ref;
		public final List<java.lang.String> chain = new ArrayList();
		public RightReference(Part ref, java.lang.String key) {
			this.ref = ref;
			chain.add(key);
		}
		@Override
		public java.lang.String toString() {
			return ref.toString() + chain;
		}

		@Override
		public Referency extend(DirectReference reference) {
			chain.add(reference.ref);
			return this;
		}
	}
	public static class Call extends Referency {
		final List<Part> arguments = new ArrayList();
		Part reference;
		Part currentArgumentPart;
		boolean closed;
		public Call(Part ref) {
			reference = ref;
		}

		@Override
		public Part transform(Part part) {
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
							throw new Error.JavaException("SyntaxError", "Unexpected " + part);
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
		public java.lang.String toString() {
			return reference.toString() + '(' + join(arguments, '.') + ')';
		}
		
		public Part finish() {
			reference.finish();
			if(arguments != null) {
				ListIterator<Part> it = arguments.listIterator();
				while(it.hasNext())
					it.set(it.next().finish());
			}
			if(currentArgumentPart != null)
				throw new Error.JavaException("SyntaxError", "Unexpected EOF");
			return this;
		}

		@Override
		public Referency extend(DirectReference reference) {
			return new RightReference(this, reference.ref);
		}
	}
	public static class Return implements Part {
		Part ret;
		public Return() {}
		@Override
		public Part transform(Part part) {
			if(part instanceof SemiColon)
				throw new CompleteException();
			
			if(ret == null)
				ret = part;
			else
				ret = ret.transform(part);
			
			return this;
		}
		@Override
		public java.lang.String toString() {
			return "return " + ret;
		}
		@Override
		public boolean isStandalone() {
			return true;
		}
		@Override
		public boolean isIncomplete() {
			return ret != null && ret.isIncomplete();
		}
		public Part finish() {
			return this;
		}
	}
	public static class Delete implements Part {
		Part ref;
		public Delete() {}
		@Override
		public Part transform(Part part) {
			if(part instanceof SemiColon)
				throw new CompleteException();
			
			if(ref == null)
				ref = part;
			else
				ref = ref.transform(part);
			
			return this;
		}
		@Override
		public java.lang.String toString() {
			return "delete " + ref;
		}
		@Override
		public boolean isStandalone() {
			return true;
		}
		@Override
		public boolean isIncomplete() {
			return ref != null && ref.isIncomplete();
		}
		public Part finish() {
			if(ref != null)
				ref.finish();
			return this;
		}
	}
	public static class Yield implements Part {
		Part ret;
		public Yield() {}
		@Override
		public Part transform(Part part) {
			if(part instanceof SemiColon)
				throw new CompleteException();
			
			if(ret == null)
				ret = part;
			else
				ret = ret.transform(part);
			
			return this;
		}
		@Override
		public java.lang.String toString() {
			return "return " + ret;
		}
		@Override
		public boolean isStandalone() {
			return true;
		}
		@Override
		public boolean isIncomplete() {
			return ret != null && ret.isIncomplete();
		}
		public Part finish() {
			return this;
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
		public java.lang.String toString() {
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
	public static class OpenBracket implements Part {
		Part contents;
		boolean closed;
		List<java.lang.String> chain = new ArrayList();
		Call call;
		
		public OpenBracket() {}
		@Override
		public java.lang.String toString() {
			StringBuilder builder = new StringBuilder("(");
			builder.append(contents);
			builder.append(')');
			if(!chain.isEmpty()) {
				builder.append('.');
				join(chain, '.', builder);
			}
			return builder.toString();
		}
		@Override
		public Part transform(Part part) {
			if(closed) {
				if(part instanceof SemiColon)
					throw new CompleteException(part);
				if(part instanceof DirectReference) {
					chain.add(((DirectReference)part).ref);
					return this;
				}
				if(part instanceof OpenBracket)
					return new Call(this);
				if(part instanceof Set)
					return new Set(this);
				
				throw new Error.JavaException("SyntaxError", "Unexpected " + part);
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
				contents = contents.transform(part);
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
		public Part finish() {
			contents = contents.finish();
			if((contents instanceof Reference || contents instanceof ReferenceChain) && chain.isEmpty())
				return contents;
			return this;
		}
	}
	public static abstract class Block implements Part {
		enum State {
			BeforeCondition,
			InCondition,
			AfterCondition,
			InSimpleImpl,
			Complete
		}
		State state;
		Part condition;
		Part simpleimpl;
		java.lang.Object[] impl;
		public Block(State state) {
			this.state = state;
		}
		public Block() {
			state = State.BeforeCondition;
		}
		
		@Override
		public Part transform(Part part) {
			switch(state) {
				case BeforeCondition:
					if(part instanceof OpenBracket) {
						state = State.InCondition;
						return this;
					}
					break;
				case InCondition:
					if(condition == null)
						condition = part;
					else {
						if(!condition.isIncomplete()) {
							if(part instanceof CloseBracket) {
								condition.finish();
								state = State.AfterCondition;
								return this;
							}
						}
						condition = condition.transform(part);
					}
					return this;
				case AfterCondition:
					if(part instanceof NewLine)
						return this;
					if(part instanceof OpenGroup)
						throw new ParseBlock(this);
					else if(part instanceof If && this instanceof Else)
						return new ElseIf();
					else if(allowSimpleImpl()) {
						simpleimpl = part;
						state = State.InSimpleImpl;
						return this;
					}
					break;
				case InSimpleImpl:
					if(!simpleimpl.isIncomplete()) {
						if(part instanceof SemiColon) {
							state = State.Complete;
							simpleimpl = simpleimpl.finish();
							throw new CompleteException();
						}
					}
					simpleimpl = simpleimpl.transform(part);
					return this;
				case Complete:
					return complete(part);
			}
			
			throw new Error.JavaException("SyntaxError", "Unexpected " + part + " (" + state + ":" + getClass().getSimpleName() + ")");
		}
		public Part complete(Part part) {
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
		public Part finish() {
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
		public java.lang.String toString() {
			StringBuilder builder = new StringBuilder("while (");
			builder.append(condition);
			builder.append(") {");
			if(impl == null)
				builder.append("[unparsed]");
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
		public java.lang.String toString() {
			StringBuilder builder = new StringBuilder("try {");
			if(impl == null)
				builder.append("[unparsed]");
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
		public Part complete(Part part) {
			if(!(part instanceof NewLine)) {
				if(c != null) {
					if(c.isIncomplete())
						c = (Catch)c.transform(part);
					else if(f instanceof Finally) {
						if(f != null)
							f = (Finally)f.transform(part);
						else if(part instanceof Finally)
							f = (Finally)part;
						else
							throw new Error.JavaException("SyntaxError", "Unexpected " + part);
					} else
						throw new Error.JavaException("SyntaxError", "Unexpected " + part);
				} else if(f != null) {
					if(f.isIncomplete())
						f = (Finally)c.transform(part);
					else 
						throw new Error.JavaException("SyntaxError", "Unexpected " + part);
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
		public java.lang.String toString() {
			StringBuilder builder = new StringBuilder("catch (");
			builder.append(condition);
			builder.append(") {");
			if(impl == null)
				builder.append("[unparsed]");
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
		public Referency extend(DirectReference reference) {
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
		public java.lang.String toString() {
			StringBuilder builder = new StringBuilder("finally {");
			if(impl == null)
				builder.append("[unparsed]");
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
	public static class If extends Block {
		public Else el;
		public If() {}
		@Override
		public java.lang.String toString() {
			StringBuilder builder = new StringBuilder("if (");
			builder.append(condition);
			builder.append(") {");
			if(impl == null)
				builder.append("[unparsed]");
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
		public Part complete(Part part) {
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
		public java.lang.String toString() {
			StringBuilder builder = new StringBuilder("else if (");
			builder.append(condition);
			builder.append(") {");
			if(impl == null)
				builder.append("[unparsed]");
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
		public Part complete(Part part) {
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
		public java.lang.String toString() {
			StringBuilder builder = new StringBuilder("else {");
			if(impl == null)
				builder.append("[unparsed]");
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
	public static class CloseBracket implements Part {
		public CloseBracket() {}
		@Override
		public java.lang.String toString() {
			return ")";
		}
		@Override
		public Part transform(Part part) {
				throw new Error.JavaException("SyntaxError", "Unexpected " + part);
		}
		@Override
		public boolean isStandalone() {
			return false;
		}
		@Override
		public boolean isIncomplete() {
			return true;
		}
		public Part finish() {
			throw new Error.JavaException("SyntaxError", "Unexpected " + this);
		}
	}
	public static class OpenGroup implements Part {
		public static enum State {
			Idle,
			NeedKey,
			HaveKey,
			ReadingValue,
			Complete
		}
		
		State state = State.Idle;
		Map<java.lang.String, Part> entries = new HashMap();
		java.lang.String currentEntryKey;
		Part currentEntry;
		public OpenGroup() {}
		@Override
		public java.lang.String toString() {
			return "{";
		}
		@Override
		public Part transform(Part part) {
			if(part instanceof NewLine)
				return this;
			
			switch(state) {
				case Idle:
					if(part instanceof CloseGroup)
						return this;
				case NeedKey:
					if(part instanceof Reference)
						currentEntryKey = ((Reference)part).ref;
					else if(part instanceof String)
						currentEntryKey = ((String)part).string;
					else if(part instanceof Number)
						currentEntryKey = net.nexustools.njs.Number.toString(((Number)part).value);
					else
						throw new Error.JavaException("SyntaxError", "Unexpected " + part);
					
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
			
			
					
			
			throw new Error.JavaException("SyntaxError", "Unexpected " + part);
		}
		@Override
		public boolean isStandalone() {
			return true;
		}
		@Override
		public boolean isIncomplete() {
			return state != State.Complete;
		}
		public Part finish() {
			return this;
		}
	}
	public static class CloseGroup implements Part {
		public CloseGroup() {}
		@Override
		public java.lang.String toString() {
			return "}";
		}
		@Override
		public Part transform(Part part) {
			throw new Error.JavaException("SyntaxError", "Unexpected " + part);
		}
		@Override
		public boolean isStandalone() {
			return false;
		}
		@Override
		public boolean isIncomplete() {
			return true;
		}
		public Part finish() {
			throw new Error.JavaException("SyntaxError", "Unexpected " + this);
		}
	}
	public static class OpenArray implements Part {
		Part currentEntry;
		List<Part> entries = new ArrayList();
		public OpenArray() {}
		@Override
		public java.lang.String toString() {
			return entries.toString();
		}
		@Override
		public Part transform(Part part) {
			if(currentEntry == null) {
				if(!(part instanceof CloseArray))
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
			return currentEntry != null;
		}
		public Part finish() {
			return this;
		}
	}
	public static class CloseArray implements Part {
		public CloseArray() {}
		@Override
		public java.lang.String toString() {
			return "]";
		}
		@Override
		public Part transform(Part part) {
				throw new Error.JavaException("SyntaxError", "Unexpected " + part);
		}
		@Override
		public boolean isStandalone() {
			return false;
		}
		@Override
		public boolean isIncomplete() {
			return true;
		}
		public Part finish() {
			throw new Error.JavaException("SyntaxError", "Unexpected " + this);
		}
	}
	public static class Comma implements Part {
		public Comma() {}
		@Override
		public java.lang.String toString() {
			return ",";
		}
		@Override
		public Part transform(Part part) {
				throw new Error.JavaException("SyntaxError", "Unexpected " + part);
		}
		@Override
		public boolean isStandalone() {
			return false;
		}
		@Override
		public boolean isIncomplete() {
			return false;
		}
		public Part finish() {
			throw new Error.JavaException("SyntaxError", "Unexpected " + this);
		}
	}
	public static class Null implements Part {
		public Null() {}
		@Override
		public java.lang.String toString() {
			return "null";
		}
		@Override
		public Part transform(Part part) {
			if(part instanceof SemiColon)
				throw new CompleteException();
			throw new Error.JavaException("SyntaxError", "Unexpected " + part);
		}
		@Override
		public boolean isStandalone() {
			return true;
		}
		@Override
		public boolean isIncomplete() {
			return false;
		}
		public Part finish() {
			return this;
		}
	}
	public static class Undefined implements Part {
		public Undefined() {}
		@Override
		public java.lang.String toString() {
			return "null";
		}
		@Override
		public Part transform(Part part) {
			throw new Error.JavaException("SyntaxError", "Unexpected " + part);
		}
		@Override
		public boolean isStandalone() {
			return true;
		}
		@Override
		public boolean isIncomplete() {
			return false;
		}
		public Part finish() {
			return this;
		}
	}
	public static class Function implements Part {
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
		Part currentArgumentPart;
		List<java.lang.String> arguments = new ArrayList();
		State state = State.BeforeName;
		java.lang.String source;
		java.lang.Object[] impl;
		public Function() {}
		@Override
		public java.lang.String toString() {
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
				builder.append("[unparsed]");
			else
				join(Arrays.asList(impl), ';', builder);
			builder.append("}");
			return builder.toString();
		}
		@Override
		public Part transform(Part part) {
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
			
			throw new Error.JavaException("SyntaxError", "Unexpected " + part + " (" + state + ')');
		}
		@Override
		public boolean isStandalone() {
			return true;
		}
		@Override
		public boolean isIncomplete() {
			return state != State.Complete;
		}
		public Part finish() {
			return this;
		}
	}
	public static class SemiColon implements Part {
		public SemiColon() {}
		@Override
		public java.lang.String toString() {
			return ";";
		}
		@Override
		public Part transform(Part part) {
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
		public Part finish() {
			return null;
		}
	}
	public static class NewLine implements Part {
		public NewLine() {}
		@Override
		public java.lang.String toString() {
			return "\n";
		}
		@Override
		public Part transform(Part part) {
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
		public Part finish() {
			return null;
		}
	}
	public static class SetPlaceholder implements Part {
		public SetPlaceholder() {}
		@Override
		public java.lang.String toString() {
			return "<>";
		}
		@Override
		public Part transform(Part part) {
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
		public Part finish() {
			throw new Error.JavaException("SyntaxError", "Unexpected " + this);
		}
	}
	public static abstract class Rh implements Part {
		public Part rhs;
		public Rh() {
		}
		public abstract java.lang.String op();
		@Override
		public Part transform(Part part) {
			if(rhs == null) {
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
		public Part finish() {
			rhs = rhs.finish();
			return this;
		}
		@Override
		public java.lang.String toString() {
			return op() + toLPadString(rhs);
		}
	}
	public static abstract class RhLh extends Rh {
		public final Part lhs;
		public RhLh() {
			lhs = null;
		}
		public RhLh(Part lhs) {
			this.lhs = lhs.finish();
		}
		@Override
		public Part finish() {
			if(lhs == null)
				throw new net.nexustools.njs.Error.JavaException("SyntaxError", "Missing Left-Hand-Side (" + getClass().getSimpleName() + ')');
			return super.finish();
		}
		@Override
		public java.lang.String toString() {
			return toRPadString(lhs) + op() + toLPadString(rhs);
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
		public Set(Part lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "=";
		}
	}
	public static class MultiplyEq extends RhLh {
		public MultiplyEq() {}
		public MultiplyEq(Part lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "*=";
		}
	}
	public static class Multiply extends RhLh {
		public Multiply() {}
		public Multiply(Part lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "*";
		}
	}
	public static class Equals extends RhLh {
		public Equals() {}
		public Equals(Part lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "==";
		}
	}
	public static class NotEquals extends RhLh {
		public NotEquals() {}
		public NotEquals(Part lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "!=";
		}
	}
	public static class NotStrictEquals extends RhLh {
		public NotStrictEquals() {}
		public NotStrictEquals(Part lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "!==";
		}
	}
	public static class StrictEquals extends RhLh {
		public StrictEquals() {}
		public StrictEquals(Part lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "===";
		}
	}
	public static class Plus extends RhLh {
		public Plus() {
			super(new Number(0));
		}
		public Plus(Part lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "+";
		}
	}
	public static class InstanceOf extends RhLh {
		public InstanceOf() {}
		public InstanceOf(Part lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "instanceof";
		}
	}
	public static class PlusEq extends RhLh {
		public PlusEq() {}
		public PlusEq(Part lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "+=";
		}
	}
	public static class PlusPlus extends RhLh {
		public PlusPlus() {}
		public PlusPlus(Part lhs) {
			super(lhs);
		}

		@Override
		public Part transform(Part part) {
			if(lhs != null)
				throw new CompleteException(part);
			return super.transform(part);
		}

		@Override
		public boolean isIncomplete() {
			return (lhs != null && lhs.isIncomplete()) || (rhs != null && rhs.isIncomplete());
		}

		@Override
		public java.lang.String op() {
			return "++";
		}

		@Override
		public Part finish() {
			if(rhs != null)
				rhs = rhs.finish();
			return this;
		}
		
	}
	public static class MoreThan extends RhLh {
		public MoreThan() {}
		public MoreThan(Part lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return ">";
		}
	}
	public static class LessThan extends RhLh {
		public LessThan() {}
		public LessThan(Part lhs) {
			super(lhs);
		}
		@Override
		public java.lang.String op() {
			return "<";
		}
	}
	public static class New extends Referency {
		boolean closed;
		Part reference;
		List<Part> arguments;
		Part currentPart;
		public New() {}
		@Override
		public Part transform(Part part) {
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
					if(!currentPart.isIncomplete())
						if(part instanceof CloseBracket) {
							arguments.add(currentPart);
							currentPart = null;
							closed = true;
							return this;
						}
					currentPart = currentPart.transform(part);
				}
			} else if(part instanceof OpenBracket)
				arguments = new ArrayList();
			
			return this;
		}
		@Override
		public java.lang.String toString() {
			StringBuilder buffer = new StringBuilder("new ");
			buffer.append(reference);
			if(arguments != null) {
				buffer.append('(');
				join(arguments, ',', buffer);
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
		public Part finish() {
			reference.finish();
			if(arguments != null) {
				ListIterator<Part> it = arguments.listIterator();
				while(it.hasNext())
					it.set(it.next().finish());
			}
			if(currentPart != null)
				throw new Error.JavaException("SyntaxError", "Unexpected EOF");
			return this;
		}

		@Override
		public Referency extend(DirectReference reference) {
			return new RightReference(this, reference.ref);
		}
	}
	public static class RegEx extends Referency {
		
		final java.lang.String pattern, flags;
		public RegEx(java.lang.String pattern, java.lang.String flags) {
			this.pattern = pattern;
			this.flags = flags;
		}

		@Override
		public Referency extend(DirectReference reference) {
			return new RightReference(this, reference.ref);
		}

		@Override
		public java.lang.String toString() {
			return '/' + pattern + '/' + flags;
		}
		
	}
	public static class Var implements Part {
		public static class Set {
			public final java.lang.String lhs;
			public Part rhs;

			private Set(java.lang.String ref) {
				lhs = ref;
			}

			@Override
			public java.lang.String toString() {
				return lhs + " = " + rhs;
			}
		}
		Set currentSet;
		List<Set> sets = new ArrayList();
		public Var() {}
		@Override
		public Part transform(Part part) {
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
			} else if(part instanceof AbstractCompiler.Set) {
				currentSet.rhs = new SetPlaceholder();
				return this;
			} else if(part instanceof SemiColon)
				throw new CompleteException();
				
			throw new Error.JavaException("SyntaxError", "Unexpected " + part);
		}
		@Override
		public java.lang.String toString() {
			return "var " + join(sets, ',');
		}
		@Override
		public boolean isStandalone() {
			return true;
		}
		@Override
		public boolean isIncomplete() {
			return sets.isEmpty() || (currentSet != null && currentSet.rhs != null && currentSet.rhs.isIncomplete());
		}
		@Override
		public Part finish() {
			if(currentSet != null) {
				if(currentSet.rhs != null && currentSet.rhs.isIncomplete())
					return this;
				
				sets.add(currentSet);
				currentSet = null;
			}
			return this;
		}
	}
	public static class String implements Part {
		public final java.lang.String string;
		public String(java.lang.String string) {
			assert(string != null);
			this.string = string;
		}

		@Override
		public Part transform(Part part) {
			if(part instanceof SemiColon)
				throw new CompleteException();
			throw new Error.JavaException("SyntaxError", "Unexpected " + part);
		}
		@Override
		public boolean isStandalone() {
			return true;
		}
		@Override
		public boolean isIncomplete() {
			return false;
		}
		@Override
		public Part finish() {
			return this;
		}

		@Override
		public java.lang.String toString() {
			return '"' + string + '"';
		}
		
	}
	public static class Integer implements Part {
		public final int value;
		private boolean newline;
		public Integer(int value) {
			this.value = value;
		}
		@Override
		public Part transform(Part part) {
			if(part instanceof NewLine) {
				newline = true;
				return this;
			}
			if(part instanceof SemiColon)
				return this;
			if(part instanceof Multiply)
				return new Multiply(this);
			if(part instanceof Plus)
				return new Plus(this);
			if(newline)
				throw new CompleteException(part);
			throw new Error.JavaException("SyntaxError", "Unexpected " + part);
		}
		@Override
		public boolean isStandalone() {
			return true;
		}
		@Override
		public boolean isIncomplete() {
			return false;
		}
		@Override
		public java.lang.String toString() {
			return java.lang.String.valueOf(value);
		}
		@Override
		public Part finish() {
			return this;
		}
	}
	public static class Number implements Part {
		public final double value;
		public Number(double value) {
			this.value = value;
		}
		@Override
		public Part transform(Part part) {
				throw new Error.JavaException("SyntaxError", "Unexpected " + part);
		}
		@Override
		public boolean isStandalone() {
			return true;
		}
		@Override
		public boolean isIncomplete() {
			return false;
		}
		@Override
		public java.lang.String toString() {
			return java.lang.String.valueOf(value);
		}
		@Override
		public Part finish() {
			return this;
		}
	}
	
	public static final java.lang.String NUMBER_REG = "\\-?\\d+(\\.\\d+)?([eE][\\+\\-]?\\d+)?";
	public static final java.lang.String STRING_REG = "(([\"'])(?:(?:\\\\\\\\)|\\\\\\2|(?!\\\\\\2)\\\\|(?!\\2).|[\\n\\r])*\\2)";
	public static final java.lang.String VARIABLE_NAME = "[_$a-zA-Z\\xA0-\\uFFFF][_$a-zA-Z0-9\\xA0-\\uFFFF]*";
	public static final Pattern MULTILINE_COMMENT = Pattern.compile("^(\\/\\*(?:(?!\\*\\/).|[\\n\\r])*\\*\\/)");
	public static final Pattern SINGLELINE_COMMENT = Pattern.compile("^(\\/\\/[^\\n\\r]*[\\n\\r]+)");
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
	public static final Pattern MULTIPLY = Pattern.compile("^\\*");
	public static final Pattern MORETHAN = Pattern.compile("^\\>");
	public static final Pattern LESSTHAN = Pattern.compile("^\\<");
	public static final Pattern PLUSEQ = Pattern.compile("^\\+=");
	public static final Pattern SEMICOLON = Pattern.compile("^;");
	public static final Pattern NOTEQUALS = Pattern.compile("^!=");
	public static final Pattern EQUALS = Pattern.compile("^==");
	public static final Pattern NEWLINE = Pattern.compile("^\n");
	public static final Pattern ACCESS = Pattern.compile("^\\[");
	public static final Pattern OR = Pattern.compile("^\\|\\|");
	public static final Pattern AND = Pattern.compile("^&&");
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
		public java.lang.String ltrim(int len) {
			if(currentBuffer.length() <= len)
				try {
					columns += countInstances(currentBuffer, '\n');
					return currentBuffer;
				} finally {
					currentBuffer = "";
				}
			else
				try {
					java.lang.String chop = currentBuffer.substring(0, len);
					columns += countInstances(chop, '\n');
					return chop;
				} finally {
					currentBuffer = currentBuffer.substring(len);
				}
		}

		private int columns;
		public int columns() {
			return columns;
		}
	}
	public static interface Parser {
		public java.lang.Object[] parse(ParserReader reader) throws IOException;
	}
	public static abstract class RegexParser implements Parser {
		public static class PartExchange extends RuntimeException {
			final int trim;
			final Part part;
			public PartExchange(Part part, int trim) {
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
		public final java.lang.Object[] parse(ParserReader reader) throws IOException {
			return parse(reader, new StringBuilder());
		}
		
		public final java.lang.Object[] parse(ParserReader reader, StringBuilder builder) throws IOException {
			FunctionParser functionParser = this instanceof FunctionParser ? (FunctionParser)this : new FunctionParser();
			BlockParser blockParser = this instanceof BlockParser ? (BlockParser)this : new BlockParser(this == functionParser);
			java.lang.String buffer;
			Matcher matcher;
			
			Part currentPart = null;
			List<Part> parts = new ArrayList();
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
							try {
								match(pat, matcher, reader);
								builder.append(reader.ltrim(matcher.group().length()));
							} catch(PartExchange part) {
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
										end(parts, builder.toString());
									}
									
									try {
										currentPart = currentPart.transform(part.part);
									} catch(CompleteException ex) {
										currentPart = currentPart.finish();
										if(currentPart != null) {
											if(!currentPart.isStandalone())
												throw new Error.JavaException("SyntaxError", "Unexpected " + currentPart);
											if(currentPart.isIncomplete())
												throw new Error.JavaException("SyntaxError", "Expected more after " + currentPart);
											parts.add(currentPart);
										}
										if(ex.part instanceof CloseGroup)
											end(parts, builder.toString());
										currentPart = ex.part;
									} catch(ParseFunction ex) {
										builder.append(reader.ltrim(part.trim));
										try {
											functionParser.parse(reader);
										} catch(ParseComplete ce) {
											ex.function.impl = ce.impl;
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
									if(part.part instanceof CloseGroup)
										end(parts, builder.toString());
									currentPart = part.part;
								}
								builder.append(reader.ltrim(part.trim));
							} catch(PartComplete part) {
								currentPart = currentPart.finish();
								if(currentPart != null) {
									if(currentPart instanceof CloseGroup)
										end(parts, builder.toString());
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
						throw new Error.JavaException("SyntaxError", "No matching patterns for " + getClass().getSimpleName() + ": " + buffer);
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
			return parts.toArray();
		}
		
		public void end(List<Part> parts, java.lang.String source) {
			throw new Error.JavaException("SyntaxError", "Unexpected }");
		}
		
		public abstract void match(Pattern pattern, Matcher matcher, ParserReader reader);
		public abstract void eof();
	}
	public static class ScriptParser extends RegexParser {
		public ScriptParser() {
			super(NOTSTRICTEQUALS, NOTEQUALS, STRICTEQUALS, EQUALS, COLON, MORETHAN, LESSTHAN, COMMA, NUMBERGET, STRINGGET, NOT, AND, OR, SET, PLUSPLUS, PLUSEQ, MULTIPLYEQ, PLUS, MULTIPLY, SEMICOLON, NEWLINE, NUMBER, VARIABLE, VARIABLEGET, SINGLELINE_COMMENT, MULTILINE_COMMENT, WHITESPACE, STRING, OPEN_GROUP, CLOSE_GROUP, OPEN_BRACKET, CLOSE_BRACKET, VAR, OPEN_ARRAY, CLOSE_ARRAY, REGEX);
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
				if(ref.equals("new"))
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
				else if(ref.equals("return"))
					ret(matcher);
				else if(ref.equals("var"))
					throw new PartExchange(new Var(), ref.length());
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
			if(pattern == MULTIPLY)
				throw new PartExchange(new Multiply(), matcher.group().length());
			if(pattern == EQUALS)
				throw new PartExchange(new Equals(), matcher.group().length());
			if(pattern == STRICTEQUALS)
				throw new PartExchange(new StrictEquals(), matcher.group().length());
			if(pattern == NOTEQUALS)
				throw new PartExchange(new NotEquals(), matcher.group().length());
			if(pattern == NOTSTRICTEQUALS)
				throw new PartExchange(new NotStrictEquals(), matcher.group().length());
			
			if(pattern == PLUSEQ)
				throw new PartExchange(new PlusEq(), matcher.group().length());
			if(pattern == MULTIPLYEQ)
				throw new PartExchange(new MultiplyEq(), matcher.group().length());
			if(pattern == PLUSPLUS)
				throw new PartExchange(new PlusPlus(), matcher.group().length());
			
			if(pattern == MORETHAN)
				throw new PartExchange(new MoreThan(), matcher.group().length());
			if(pattern == LESSTHAN)
				throw new PartExchange(new LessThan(), matcher.group().length());
			
			if(pattern == SET)
				throw new PartExchange(new Set(), matcher.group().length());
			if(pattern == OR)
				throw new PartExchange(new Or(), matcher.group().length());
			if(pattern == AND)
				throw new PartExchange(new And(), matcher.group().length());
			if(pattern == COLON)
				throw new PartExchange(new Colon(), matcher.group().length());
			
			if(pattern == NUMBERGET)
				throw new PartExchange(new DirectReference(matcher.group(1)), matcher.group().length());
			
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
		public void end(List<Part> parts, java.lang.String source) {
			throw new ParseComplete(parts.toArray(), source);
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
		public void end(List<Part> parts, java.lang.String source) {
			throw new ParseComplete(parts.toArray(), source);
		}
	}

	@Override
	public final Script eval(java.lang.String source, boolean inFunction) {
		return eval(new StringReader(source), inFunction);
	}

	@Override
	public final Script eval(Reader source, boolean inFunction) {
		ParserReader reader = null;
		RegexParser parser = inFunction ? new FunctionParser() : new ScriptParser();
		try {
			return compileScript(parser.parse(reader = new ParserReader(source)), inFunction);
		} catch(net.nexustools.njs.Error.JavaException ex) {
			if(ex.type.equals("SyntaxError") && reader != null)
				throw new net.nexustools.njs.Error.JavaException("SyntaxError", ex.getUnderlyingMessage() + " (" + reader.columns() + ')', ex);
			throw ex;
		} catch(IOException ex) {
			throw new Error.JavaException("EvalError", "IO Exception While Evaluating Script: " + ex.getMessage(), ex);
		}
	}
	
	protected abstract Script compileScript(java.lang.Object[] script, boolean inFunction);
	
}
