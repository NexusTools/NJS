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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.nexustools.njs.Error;

/**
 *
 * @author kate
 */
public abstract class AbstractCompiler implements Compiler {
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
	public static class Reference implements Part {
		public final java.lang.String ref;
		public Reference(java.lang.String ref) {
			this.ref = ref;
		}
		@Override
		public Part transform(Part part) {
			if(part instanceof SemiColon)
				throw new CompleteException();
			
			if(part instanceof DirectReference) {
				ReferenceChain chain = new ReferenceChain();
				chain.chain.add(ref);
				chain.chain.add(((DirectReference)part).ref);
				return chain;
			} else if(part instanceof OpenBracket)
				return new Call(this);
			else if(part instanceof MultiplyEq)
				return new MultiplyEq(this);
			else if(part instanceof PlusEq)
				return new PlusEq(this);
			else if(part instanceof PlusPlus)
				return new PlusPlus(this);
			else if(part instanceof Multiply)
				return new Multiply(this);
			else if(part instanceof Plus)
				return new Plus(this);
			else if(part instanceof Set)
				return new Set(this);
			else if(part instanceof Or)
				return new Or(this);
			
			throw new Error.JavaException("SyntaxError", "Unexpected " + part);
		}
		@Override
		public java.lang.String toString() {
			return ref;
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
	public static class Or implements Part {
		public final Part lhs;
		public Part rhs;
		public Or() {
			lhs = null;
		}
		public Or(Part lhs) {
			this.lhs = lhs;
		}
		@Override
		public Part transform(Part part) {
			if(rhs == null)
				rhs = part;
			else
				rhs = rhs.transform(part);
			
			return this;
		}
		@Override
		public java.lang.String toString() {
			return lhs + " || " + rhs;
		}
		@Override
		public boolean isStandalone() {
			return true;
		}
		@Override
		public boolean isIncomplete() {
			return rhs == null || rhs.isIncomplete();
		}
		public Part finish() {
			lhs.finish();
			rhs.finish();
			return this;
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
	public static class RightReference implements Part {
		public final Part ref;
		public final List<java.lang.String> chain = new ArrayList();
		public RightReference(Part ref, java.lang.String key) {
			this.ref = ref;
			chain.add(key);
		}
		@Override
		public Part transform(Part part) {
			if(part instanceof DirectReference)
				chain.add(((DirectReference)part).ref);
			else
				throw new Error.JavaException("SyntaxError", "Unexpected " + part);
			
			return this;
		}
		@Override
		public java.lang.String toString() {
			return ref.toString() + chain;
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
			return this;
		}
	}
	public static class Call implements Part {
		final List<Part> arguments = new ArrayList();
		Part reference;
		Part currentArgumentPart;
		boolean closed;
		public Call(Part ref) {
			reference = ref;
		}

		@Override
		public Part transform(Part part) {
			if(closed) {
				if(part instanceof Plus)
					return new Plus(this);
				if(part instanceof Multiply)
					return new Multiply(this);
				if(part instanceof SemiColon)
					throw new CompleteException();
				if(part instanceof NewLine)
					return this;
				if(part instanceof Reference)
					throw new CompleteException(part);
				if(part instanceof OpenBracket)
					return new Call(this);
				if(part instanceof DirectReference)
					return new RightReference(this, ((DirectReference)part).ref);
				throw new Error.JavaException("SyntaxError", "Unexpected " + part);
			}
			
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
	public static class ReferenceChain implements Part {
		public final List<java.lang.String> chain = new ArrayList();
		public ReferenceChain() {}
		public ReferenceChain(java.lang.String ref) {
			assert(ref != null);
			chain.add(ref);
		}
		@Override
		public Part transform(Part part) {
			if(part instanceof NewLine)
				return this;
			
			if(part instanceof Reference) {
				assert(((Reference)part).ref != null);
				chain.add(((Reference)part).ref);
			} else if(part instanceof DirectReference) {
				assert(((DirectReference)part).ref != null);
				chain.add(((DirectReference)part).ref);
			} else if(part instanceof OpenBracket)
				return new Call(this);
			else if(part instanceof Set)
				return new Set(this);
			else
				throw new Error.JavaException("SyntaxError", "Unexpected " + part);
			
			return this;
		}
		@Override
		public java.lang.String toString() {
			return join(chain, '.');
		}
		@Override
		public boolean isStandalone() {
			return true;
		}
		@Override
		public boolean isIncomplete() {
			return chain.isEmpty();
		}
		public Part finish() {
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
			Complete
		}
		java.lang.Object[] impl;
		State state = State.BeforeCondition;
	}
	public static class While extends Block {
		Part condition;
		boolean closed;
		
		public While() {}
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
					if(part instanceof OpenGroup)
						throw new ParseBlock(this);
					break;
				case Complete:
					throw new CompleteException(part);
			}
			
			throw new Error.JavaException("SyntaxError", "Unexpected " + part + " (" + state + ")");
		}
		@Override
		public java.lang.String toString() {
			return "while(" + condition + "){" + join(Arrays.asList(impl), ';');
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
			return this;
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
		public OpenGroup() {}
		@Override
		public java.lang.String toString() {
			return "{";
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
			return true;
		}
		public Part finish() {
			throw new Error.JavaException("SyntaxError", "Unexpected " + this);
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
				if(part instanceof Comma || part instanceof CloseArray) {
					currentEntry = currentEntry.finish();
					if(currentEntry != null) {
						if(currentEntry.isIncomplete())
							throw new Error.JavaException("SyntaxError", "Expected more after " + this);
						entries.add(currentEntry);
						currentEntry = null;
					}
				} else
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
	public static class Set implements Part {
		public Part lhs, rhs;
		public Set() {}
		private Set(Part lhs) {
			this.lhs = lhs;
		}
		@Override
		public java.lang.String toString() {
			return lhs + " = " + rhs;
		}
		@Override
		public Part transform(Part part) {
			if(part instanceof OpenGroup) {
				throw new RuntimeException();
			}
			
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
		public Part finish() {
			return this;
		}
	}
	public static class MultiplyEq implements Part {
		public Part lhs, rhs;
		public boolean hasNewLine;
		public MultiplyEq() {}
		private MultiplyEq(Part lhs) {
			this.lhs = lhs;
		}
		@Override
		public java.lang.String toString() {
			return lhs + " *= " + rhs;
		}
		@Override
		public Part transform(Part part) {
			if(part instanceof SemiColon)
				throw new CompleteException();
			if(part instanceof NewLine) {
				hasNewLine = true;
				return this;
			}
			
			if(rhs == null) {
				rhs = part;
				return this;
			} else if(part instanceof Multiply)
				return new Multiply(this);
			else if(part instanceof Plus)
				return new Plus(this);
			
			if(rhs.isIncomplete()) {
				rhs = rhs.transform(part);
				return this;
			}
			
			if(hasNewLine)
				throw new CompleteException(part);
			
			throw new Error.JavaException("SyntaxError", "Unexpected " + part);
		}
		@Override
		public boolean isStandalone() {
			return true;
		}
		@Override
		public boolean isIncomplete() {
			return rhs == null;
		}
		public Part finish() {
			lhs = lhs.finish();
			rhs = rhs.finish();
			return this;
		}
	}
	public static class Multiply implements Part {
		public Part lhs, rhs;
		public Multiply() {}
		private Multiply(Part lhs) {
			this.lhs = lhs;
		}
		@Override
		public java.lang.String toString() {
			return lhs + " * " + rhs;
		}
		@Override
		public Part transform(Part part) {
			if(part instanceof SemiColon)
				throw new CompleteException();
			
			if(rhs == null) {
				rhs = part;
				return this;
			} else if(part instanceof Multiply)
				return new Multiply(this);
			else if(part instanceof Plus)
				return new Plus(this);
			
			if(rhs.isIncomplete()) {
				rhs = rhs.transform(part);
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
			return rhs == null;
		}
		public Part finish() {
			lhs = lhs.finish();
			rhs = rhs.finish();
			
			if(lhs instanceof Number && rhs instanceof Number)
				return new Number(((Number)lhs).value * ((Number)rhs).value);
			if(lhs instanceof Number && rhs instanceof Integer)
				return new Number(((Number)lhs).value * ((Integer)rhs).value);
			if(lhs instanceof Integer && rhs instanceof Number)
				return new Number(((Integer)lhs).value * ((Number)rhs).value);
			if(lhs instanceof Integer && rhs instanceof Integer)
				return new Integer(((Integer)lhs).value * ((Integer)rhs).value);
			return this;
		}
	}
	public static class Plus implements Part {
		public Part lhs, rhs;
		public Plus() {}
		private Plus(Part lhs) {
			this.lhs = lhs;
		}
		@Override
		public java.lang.String toString() {
			return lhs + " + " + rhs;
		}
		@Override
		public Part transform(Part part) {
			if(rhs == null) {
				rhs = part;
				return this;
			} else if(part instanceof Multiply)
				return new Multiply(this);
			else if(part instanceof Plus)
				return new Plus(this);
			
			rhs = rhs.transform(part);
			return this;
		}
		@Override
		public boolean isStandalone() {
			return true;
		}
		@Override
		public boolean isIncomplete() {
			return rhs == null;
		}
		public Part finish() {
			rhs = rhs.finish();
			if(lhs != null) {
				lhs = lhs.finish();

				if(lhs instanceof Number && rhs instanceof Number)
					return new Number(((Number)lhs).value + ((Number)rhs).value);
				if(lhs instanceof Number && rhs instanceof Integer)
					return new Number(((Number)lhs).value + ((Integer)rhs).value);
				if(lhs instanceof Integer && rhs instanceof Number)
					return new Number(((Integer)lhs).value + ((Number)rhs).value);
				if(lhs instanceof Integer && rhs instanceof Integer)
					return new Integer(((Integer)lhs).value + ((Integer)rhs).value);
			}
			return this;
		}
	}
	public static class PlusEq implements Part {
		public Part lhs, rhs;
		public PlusEq() {}
		private PlusEq(Part lhs) {
			this.lhs = lhs;
		}
		@Override
		public java.lang.String toString() {
			return lhs + " += " + rhs;
		}
		@Override
		public Part transform(Part part) {
			if(rhs == null) {
				rhs = part;
				return this;
			} else if(part instanceof Multiply)
				return new Multiply(this);
			else if(part instanceof Plus)
				return new Plus(this);
			
			rhs = rhs.transform(part);
			return this;
		}
		@Override
		public boolean isStandalone() {
			return true;
		}
		@Override
		public boolean isIncomplete() {
			return rhs == null;
		}
		public Part finish() {
			lhs = lhs.finish();
			rhs = rhs.finish();
			return this;
		}
	}
	public static class PlusPlus implements Part {
		public Part lhs;
		public PlusPlus() {}
		private PlusPlus(Part lhs) {
			this.lhs = lhs;
		}
		@Override
		public java.lang.String toString() {
			return lhs + " ++";
		}
		@Override
		public Part transform(Part part) {
			throw new CompleteException(part);
		}
		@Override
		public boolean isStandalone() {
			return true;
		}
		@Override
		public boolean isIncomplete() {
			return lhs == null;
		}
		public Part finish() {
			if(lhs == null)
				throw new net.nexustools.njs.Error.JavaException("SyntaxError", "Unexpected " + this);
			lhs = lhs.finish();
			return this;
		}
	}
	public static class New implements Part {
		boolean closed;
		Part reference;
		boolean newline;
		List<Part> arguments;
		Part currentPart;
		public New() {}
		@Override
		public Part transform(Part part) {
			
			if(closed) {
				if(part instanceof NewLine) {
					newline = true;
					return this;
				}
				if(part instanceof OpenBracket)
					return new Call(this);
				
				if(newline)
					throw new CompleteException(part);
				
				throw new Error.JavaException("SyntaxError", "Unexpected " + part);
			}
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
				System.out.println(currentSet.rhs + " -> " + part);
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
			}
				
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
	public static final Pattern MULTIPLY = Pattern.compile("^\\*");
	public static final Pattern PLUSPLUS = Pattern.compile("^\\+\\+");
	public static final Pattern PLUSEQ = Pattern.compile("^\\+=");
	public static final Pattern SEMICOLON = Pattern.compile("^;");
	public static final Pattern NEWLINE = Pattern.compile("^\n");
	public static final Pattern ACCESS = Pattern.compile("^\\[");
	public static final Pattern OR = Pattern.compile("^\\|\\|");
	public static final Pattern COMMA = Pattern.compile("^,");
	public static final Pattern PLUS = Pattern.compile("^\\+");
	public static final Pattern SET = Pattern.compile("^=");
	
	private static class ParserReader {
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
		public java.lang.String ltrim(int len) {
			if(currentBuffer.length() <= len)
				try {
					return currentBuffer;
				} finally {
					currentBuffer = "";
				}
			else
				try {
					return currentBuffer.substring(0, len);
				} finally {
					currentBuffer = currentBuffer.substring(len);
				}
		}
	}
	private static interface Parser {
		public java.lang.Object[] parse(ParserReader reader) throws IOException;
	}
	private static abstract class RegexParser implements Parser {
		public static class PartExchange extends RuntimeException {
			final int trim;
			final Part part;
			private PartExchange(Part part, int trim) {
				this.part = part;
				this.trim = trim;
			}
		}
		public static class PartComplete extends RuntimeException {
			final int trim;
			private PartComplete(int trim) {
				this.trim = trim;
			}
		}
		
		private final Pattern[] patterns;
		public RegexParser(Pattern... patterns) {
			this.patterns = patterns;
		}
		
		@Override
		public final java.lang.Object[] parse(ParserReader reader) throws IOException {
			FunctionParser functionParser = this instanceof FunctionParser ? (FunctionParser)this : new FunctionParser();
			BlockParser blockParser = this instanceof BlockParser ? (BlockParser)this : new BlockParser();
			StringBuilder builder = new StringBuilder();
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
								if(part.part instanceof CloseGroup) {
									if(currentPart != null) {
										currentPart = currentPart.finish();
										if(currentPart != null)
											parts.add(currentPart);
									}
									end(parts, builder.toString());
								}
								/*StringBuilder builder2 = new StringBuilder();
								if(currentPart != null) {
									builder2.append(currentPart);
									builder2.append(" (");
									builder2.append(currentPart.getClass().getSimpleName());
									builder2.append(')');
								}
								builder2.append(" -> ");
								builder2.append(part.part);
								builder2.append(" (");
								builder2.append(part.part.getClass().getSimpleName());
								builder2.append(')');
								System.out.println(builder2);*/
								if(currentPart != null) {
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
								} else
									currentPart = part.part;
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
	private static class ScriptParser extends RegexParser {
		public ScriptParser() {
			super(COMMA, NUMBERGET, STRINGGET, OR, SET, PLUSPLUS, PLUSEQ, MULTIPLYEQ, PLUS, MULTIPLY, SEMICOLON, NEWLINE, NUMBER, VARIABLE, VARIABLEGET, SINGLELINE_COMMENT, MULTILINE_COMMENT, WHITESPACE, STRING, OPEN_GROUP, CLOSE_GROUP, OPEN_BRACKET, CLOSE_BRACKET, VAR, OPEN_ARRAY, CLOSE_ARRAY);
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
			
			if(pattern == PLUS)
				throw new PartExchange(new Plus(), matcher.group().length());
			if(pattern == MULTIPLY)
				throw new PartExchange(new Multiply(), matcher.group().length());
			
			if(pattern == PLUSEQ)
				throw new PartExchange(new PlusEq(), matcher.group().length());
			if(pattern == MULTIPLYEQ)
				throw new PartExchange(new MultiplyEq(), matcher.group().length());
			
			if(pattern == PLUSPLUS)
				throw new PartExchange(new PlusPlus(), matcher.group().length());
			
			if(pattern == SET)
				throw new PartExchange(new Set(), matcher.group().length());
			if(pattern == OR)
				throw new PartExchange(new Or(), matcher.group().length());
			
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
	private static class BlockParser extends ScriptParser {
		public void end(List<Part> parts, java.lang.String source) {
			throw new ParseComplete(parts.toArray(), source);
		}
	}
	private static class FunctionParser extends ScriptParser {
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
		try {
			Parser parser = inFunction ? new FunctionParser() : new ScriptParser();
			return createScript(parser.parse(new ParserReader(source)), inFunction);
		} catch(IOException ex) {
			throw new Error.JavaException("EvalError", "IO Exception While Evaluating Script: " + ex.getMessage(), ex);
		}
	}
	
	protected abstract Script createScript(java.lang.Object[] script, boolean inFunction);
	
}
