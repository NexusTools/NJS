/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs.compiler;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import net.nexustools.njs.AbstractFunction;
import net.nexustools.njs.BaseFunction;
import net.nexustools.njs.BaseObject;
import net.nexustools.njs.Global;
import net.nexustools.njs.JSHelper;
import net.nexustools.njs.Scope;
import net.nexustools.njs.ConstructableFunction;
import net.nexustools.njs.GenericArray;
import net.nexustools.njs.GenericObject;
import net.nexustools.njs.Scopeable;

import java.util.Iterator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.nexustools.njs.JSHelper.ReplacementStackTraceElement;
import static net.nexustools.njs.compiler.AbstractCompiler.join;
import static net.nexustools.njs.compiler.AbstractCompiler.join;
import static net.nexustools.njs.compiler.AbstractCompiler.join;
import static net.nexustools.njs.compiler.AbstractCompiler.join;
import static net.nexustools.njs.compiler.AbstractCompiler.join;
import static net.nexustools.njs.compiler.AbstractCompiler.join;
import static net.nexustools.njs.compiler.AbstractCompiler.join;
import static net.nexustools.njs.compiler.AbstractCompiler.join;
import static net.nexustools.njs.compiler.AbstractCompiler.join;
import static net.nexustools.njs.compiler.AbstractCompiler.join;
import static net.nexustools.njs.compiler.AbstractCompiler.join;
import static net.nexustools.njs.compiler.AbstractCompiler.join;
import static net.nexustools.njs.compiler.AbstractCompiler.join;
import static net.nexustools.njs.compiler.AbstractCompiler.join;
import static net.nexustools.njs.compiler.AbstractCompiler.join;
import static net.nexustools.njs.compiler.AbstractCompiler.join;

/**
 *
 * @author kate
 */
public class RuntimeCompiler extends AbstractCompiler {
	private static class ScriptCompilerData {
		final java.lang.String fileName;
		final java.lang.String methodName;
		final Runnable[] functionImpls;
		private ScriptCompilerData(Runnable[] functionImpls, java.lang.String fileName, java.lang.String methodName) {
			assert(methodName != null);
			this.functionImpls = functionImpls;
			this.methodName = methodName;
			this.fileName = fileName;
		}
		private void exec(Global global, Scope scope) {
			for(Runnable impl : functionImpls) {
				BaseFunction func = (BaseFunction)impl.run(global, scope).get();
				scope.var(func.name(), func);
			}
		}
	}
	private final ArrayList<java.lang.String> methodNameStack = new ArrayList();
	private ScriptCompilerData precompile(ScriptData script, java.lang.String fileName) {
		Runnable[] functionImpls = new Runnable[script.functions.length];
		for(int i=0; i<functionImpls.length; i++) {
			StringBuilder builder = new StringBuilder();
			for(java.lang.String methodName : methodNameStack) {
				if(methodName.startsWith("<"))
					continue;
				if(builder.length() > 0)
					builder.append('.');
				builder.append(methodName);
			}
			if(builder.length() > 0)
				builder.append('.');
			builder.append(script.functions[i].name);
			functionImpls[i] = compile(new ScriptCompilerData(null, fileName, builder.toString()), script.functions[i]);
		}
		int min = methodNameStack.size()-1;
		StringBuilder builder = new StringBuilder();
		for(int i=0; i<methodNameStack.size(); i++) {
			java.lang.String methodName = methodNameStack.get(i);
			if(methodName.startsWith("<") && i < min)
				continue;
			if(builder.length() > 0)
				builder.append('.');
			builder.append(methodName);
		}
		return new ScriptCompilerData(functionImpls, fileName, builder.toString());
	}
	public static interface Referenceable {
		public BaseObject get();
		public void set(BaseObject value);
		public boolean delete();
	}
	public static interface KnownReferenceable extends Referenceable {
		public java.lang.String source();
	}
	public static class ValueReferenceable implements Referenceable {
		public final BaseObject value;
		public ValueReferenceable(BaseObject value) {
			this.value = value;
		}
		@Override
		public BaseObject get() {
			return value;
		}
		@Override
		public void set(BaseObject value) {
			throw new net.nexustools.njs.Error.JavaException("ReferenceError", "Cannot set a value without knowing its parent and index");
		}
		@Override
		public boolean delete() {
			throw new net.nexustools.njs.Error.JavaException("ReferenceError", "Cannot delete a value without knowing its parent and index");
		}
	}
	public static class Return extends ValueReferenceable {
		public Return(BaseObject object) {
			super(object);
		}
	}
	public static interface ParentedReferenceable extends Referenceable {
		public Scopeable parent();
	}
	public static class StringKeyReferenceable implements KnownReferenceable, ParentedReferenceable {
		public final Scopeable parent;
		public final java.lang.String key;
		public final java.lang.String source;
		public StringKeyReferenceable(java.lang.String source, java.lang.String key, Scopeable parent) {
			this.parent = parent;
			this.source = source;
			this.key = key;
		}
		@Override
		public Scopeable parent() {
			return parent;
		}
		@Override
		public java.lang.String source() {
			return source;
		}
		@Override
		public BaseObject get() {
			return parent.get(key);
		}
		@Override
		public void set(BaseObject value) {
			parent.set(key, value);
		}
		@Override
		public boolean delete() {
			return parent.delete(key);
		}
	}
	public static class IntegerKeyReferenceable implements KnownReferenceable, ParentedReferenceable {
		public final int key;
		public final BaseObject parent;
		public final java.lang.String source;
		public IntegerKeyReferenceable(java.lang.String source, int key, BaseObject parent) {
			this.parent = parent;
			this.source = source;
			this.key = key;
		}
		@Override
		public Scopeable parent() {
			return parent;
		}
		@Override
		public java.lang.String source() {
			return source;
		}
		@Override
		public BaseObject get() {
			return parent.get(key);
		}
		@Override
		public void set(BaseObject value) {
			parent.set(key, value);
		}
		@Override
		public boolean delete() {
			return parent.delete(key);
		}
	}
	public static class ObjectKeyReferenceable implements KnownReferenceable, ParentedReferenceable {
		public final BaseObject key;
		public final BaseObject parent;
		public final java.lang.String source;
		public ObjectKeyReferenceable(java.lang.String source, BaseObject key, BaseObject parent) {
			this.parent = parent;
			this.source = source;
			this.key = key;
		}
		@Override
		public Scopeable parent() {
			return parent;
		}
		@Override
		public java.lang.String source() {
			return source;
		}
		@Override
		public BaseObject get() {
			if(key instanceof net.nexustools.njs.String.Instance)
				return parent.get(((net.nexustools.njs.String.Instance) key).string);
			else if(key instanceof net.nexustools.njs.Number.Instance && ((net.nexustools.njs.Number.Instance)key).number >= 0
					 && ((net.nexustools.njs.Number.Instance)key).number <= java.lang.Integer.MAX_VALUE && ((net.nexustools.njs.Number.Instance)key).number == (int)((net.nexustools.njs.Number.Instance)key).number)
				return parent.get((int)((net.nexustools.njs.Number.Instance)key).number);
			else
				return parent.get(key.toString());
		}
		@Override
		public void set(BaseObject val) {
			if(key instanceof net.nexustools.njs.String.Instance)
				parent.set(((net.nexustools.njs.String.Instance) key).string, val);
			else if(key instanceof net.nexustools.njs.Number.Instance && ((net.nexustools.njs.Number.Instance)key).number >= 0
					 && ((net.nexustools.njs.Number.Instance)key).number <= java.lang.Integer.MAX_VALUE && ((net.nexustools.njs.Number.Instance)key).number == (int)((net.nexustools.njs.Number.Instance)key).number)
				parent.set((int)((net.nexustools.njs.Number.Instance)key).number, val);
			else
				parent.set(key.toString(), val);
		}
		@Override
		public boolean delete() {
			if(key instanceof net.nexustools.njs.String.Instance)
				return parent.delete(((net.nexustools.njs.String.Instance) key).string);
			else if(key instanceof net.nexustools.njs.Number.Instance && ((net.nexustools.njs.Number.Instance)key).number >= 0
					 && ((net.nexustools.njs.Number.Instance)key).number <= java.lang.Integer.MAX_VALUE && ((net.nexustools.njs.Number.Instance)key).number == (int)((net.nexustools.njs.Number.Instance)key).number)
				return parent.delete((int)((net.nexustools.njs.Number.Instance)key).number);
			else
				return parent.delete(key.toString());
		}
	}
	public static interface Runnable {
		public Referenceable run(Global global, Scope scope);
	}
	public static final Referenceable UNDEFINED_REFERENCE = new Referenceable() {
		@Override
		public BaseObject get() {
			return net.nexustools.njs.Undefined.INSTANCE;
		}
		@Override
		public void set(BaseObject value) {}
		@Override
		public boolean delete() {
			return false;
		}
	};
	public static final Runnable UNDEFINED = new Runnable() {
		@Override
		public Referenceable run(Global global, Scope scope) {
			return UNDEFINED_REFERENCE;
		}
	};
	public static final Runnable NULL = new Runnable() {
		public final Referenceable REFERENCE = new Referenceable() {
			@Override
			public BaseObject get() {
				return net.nexustools.njs.Null.INSTANCE;
			}

			@Override
			public void set(BaseObject value) {}

			@Override
			public boolean delete() {
				return false;
			}
		};
		@Override
		public Referenceable run(Global global, Scope scope) {
			return REFERENCE;
		}
	};
	
	private Runnable compile(final ScriptCompilerData data, Part object) {
		if(DEBUG)
			System.out.println("Compiling " + describe(object));
		
		final int rows = object.rows;
		final int columns = object.columns;
		if(object instanceof Integer) {
			final double number = ((Integer)object).value;
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						return new ValueReferenceable(global.wrap(number));
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof Number) {
			final double number = ((Number)object).value;
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						return new ValueReferenceable(global.wrap(number));
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof String) {
			final java.lang.String string = ((String)object).string;
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						return new ValueReferenceable(global.wrap(string));
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof Reference) {
			final java.lang.String ref = ((Reference)object).ref;
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						return new StringKeyReferenceable(ref, ref, scope);
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof ReferenceChain) {
			final Iterable<java.lang.String> chain = ((ReferenceChain)object).chain;
			final java.lang.String full = join(chain, '.');
			final java.lang.String key = ((ReferenceChain)object).chain.remove(((ReferenceChain)object).chain.size()-1);
			final java.lang.String base = join(chain, '.');
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						return new StringKeyReferenceable(full, key, scope.resolve(chain));
					} catch(net.nexustools.njs.Error.JavaException err) {
						if(err.type.equals("TypeError")) {
							if(err.getUnderlyingMessage().endsWith("from null"))
								throw new net.nexustools.njs.Error.JavaException("TypeError", "Cannot read property \"" + key + "\" from \"" + base + "\" which is null");
							if(err.getUnderlyingMessage().endsWith("from undefined"))
								throw new net.nexustools.njs.Error.JavaException("TypeError", "Cannot read property \"" + key + "\" from \"" + base + "\" which is undefined");
						}
						throw err;
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof New) {
			final Runnable reference = compile(data, ((New)object).reference);
			
			if(((New)object).arguments == null || ((New)object).arguments.isEmpty())
				return new Runnable() {
					@Override
					public Referenceable run(Global global, Scope scope) {
						ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
						try {
							return new ValueReferenceable(((BaseFunction)reference.run(global, scope).get()).construct());
						} finally {
							el.finishCall();
						}
					}
				};
			
			Object[] args = ((New)object).arguments.toArray();
			final Runnable[] argr = new Runnable[args.length];
			for(int i=0; i<args.length; i++)
				argr[i] = compile(data, (Part)args[i]);
			
			switch(args.length) {
				case 1:
					return new Runnable() {
						@Override
						public Referenceable run(Global global, Scope scope) {
							ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
							try {
								return new ValueReferenceable(((BaseFunction)reference.run(global, scope).get()).construct(argr[0].run(global, scope).get()));
							} finally {
								el.finishCall();
							}
						}
					};
					
				case 2:
					return new Runnable() {
						@Override
						public Referenceable run(Global global, Scope scope) {
							ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
							try {
								return new ValueReferenceable(((BaseFunction)reference.run(global, scope).get()).construct(argr[0].run(global, scope).get(), argr[1].run(global, scope).get()));
							} finally {
								el.finishCall();
							}
						}
					};
					
				case 3:
					return new Runnable() {
						@Override
						public Referenceable run(Global global, Scope scope) {
							ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
							try {
								return new ValueReferenceable(((BaseFunction)reference.run(global, scope).get()).construct(argr[0].run(global, scope).get(), argr[1].run(global, scope).get(), argr[2].run(global, scope).get()));
							} finally {
								el.finishCall();
							}
						}
					};
					
				default:
					return new Runnable() {
						@Override
						public Referenceable run(Global global, Scope scope) {
							BaseObject[] args = new BaseObject[argr.length];
							ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
							try {
								for(int i=0; i<args.length; i++)
									args[i] = argr[i].run(global, scope).get();
								return new ValueReferenceable(((BaseFunction)reference.run(global, scope).get()).construct(args));
							} finally {
								el.finishCall();
							}
						}
					};
			}
		} else if(object instanceof Call) {
			final Runnable reference = compile(data, ((Call)object).reference);
			
			if(((Call)object).arguments.isEmpty())
				return new Runnable() {
					@Override
					public Referenceable run(Global global, Scope scope) {
						ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
						try {
							Referenceable ref = reference.run(global, scope);
							BaseFunction func;
							try {
								func = ((BaseFunction)ref.get());
							} catch(ClassCastException ex) {
								if(ref instanceof KnownReferenceable)
									throw new net.nexustools.njs.Error.JavaException("TypeError", ((KnownReferenceable)ref).source() + " is not a function");
								throw new net.nexustools.njs.Error.JavaException("TypeError", "is not a function");
							}
							if(ref instanceof ParentedReferenceable)
								return new ValueReferenceable(func.call(((ParentedReferenceable)ref).parent() instanceof Scope ? net.nexustools.njs.Undefined.INSTANCE : (BaseObject)((ParentedReferenceable)ref).parent()));
							else
								return new ValueReferenceable(func.call(global));
						} finally {
							el.finishCall();
						}
					}
				};
			
			Object[] args = ((Call)object).arguments.toArray();
			final Runnable[] argr = new Runnable[args.length];
			for(int i=0; i<args.length; i++)
				argr[i] = compile(data, (Part)args[i]);
			
			switch(args.length) {
				case 1:
					return new Runnable() {
						@Override
						public Referenceable run(Global global, Scope scope) {
							ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
							try {
								Referenceable ref = reference.run(global, scope);
								BaseFunction func;
								try {
									func = ((BaseFunction)ref.get());
								} catch(Exception ex) {
									if(ref instanceof KnownReferenceable)
										throw new net.nexustools.njs.Error.JavaException("TypeError", ((KnownReferenceable)ref).source() + " is not a function");
									throw new net.nexustools.njs.Error.JavaException("TypeError", "is not a function");
								}
								if(ref instanceof ParentedReferenceable)
									return new ValueReferenceable(func.call(((ParentedReferenceable)ref).parent() instanceof Scope ? net.nexustools.njs.Undefined.INSTANCE : (BaseObject)((ParentedReferenceable)ref).parent(), argr[0].run(global, scope).get()));
								else
									return new ValueReferenceable(func.call(global, argr[0].run(global, scope).get()));
							} finally {
								el.finishCall();
							}
						}
					};
					
				case 2:
					return new Runnable() {
						@Override
						public Referenceable run(Global global, Scope scope) {
							ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
							try {
								Referenceable ref = reference.run(global, scope);
								BaseFunction func;
								try {
									func = ((BaseFunction)ref.get());
								} catch(Exception ex) {
									if(ref instanceof KnownReferenceable)
										throw new net.nexustools.njs.Error.JavaException("TypeError", ((KnownReferenceable)ref).source() + " is not a function");
									throw new net.nexustools.njs.Error.JavaException("TypeError", "is not a function");
								}
								if(ref instanceof ParentedReferenceable)
									return new ValueReferenceable(func.call(((ParentedReferenceable)ref).parent() instanceof Scope ? net.nexustools.njs.Undefined.INSTANCE : (BaseObject)((ParentedReferenceable)ref).parent(), argr[0].run(global, scope).get(), argr[1].run(global, scope).get()));
								else
									return new ValueReferenceable(func.call(global, argr[0].run(global, scope).get(), argr[1].run(global, scope).get()));
							} finally {
								el.finishCall();
							}
						}
					};
					
				case 3:
					return new Runnable() {
						@Override
						public Referenceable run(Global global, Scope scope) {
							ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
							try {
								Referenceable ref = reference.run(global, scope);
								BaseFunction func;
								try {
									func = ((BaseFunction)ref.get());
								} catch(Exception ex) {
									if(ref instanceof KnownReferenceable)
										throw new net.nexustools.njs.Error.JavaException("TypeError", ((KnownReferenceable)ref).source() + " is not a function");
									throw new net.nexustools.njs.Error.JavaException("TypeError", "is not a function");
								}
								if(ref instanceof ParentedReferenceable)
									return new ValueReferenceable(func.call(((ParentedReferenceable)ref).parent() instanceof Scope ? net.nexustools.njs.Undefined.INSTANCE : (BaseObject)((ParentedReferenceable)ref).parent(), argr[0].run(global, scope).get(), argr[1].run(global, scope).get(), argr[2].run(global, scope).get()));
								else
									return new ValueReferenceable(func.call(global, argr[0].run(global, scope).get(), argr[1].run(global, scope).get(), argr[2].run(global, scope).get()));
							} finally {
								el.finishCall();
							}
						}
					};
					
				default:
					return new Runnable() {
						@Override
						public Referenceable run(Global global, Scope scope) {
							ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
							try {
								Referenceable ref = reference.run(global, scope);
								BaseFunction func;
								try {
									func = ((BaseFunction)ref.get());
								} catch(Exception ex) {
									if(ref instanceof KnownReferenceable)
										throw new net.nexustools.njs.Error.JavaException("TypeError", ((KnownReferenceable)ref).source() + " is not a function");
									throw new net.nexustools.njs.Error.JavaException("TypeError", "is not a function");
								}
								BaseObject[] args = new BaseObject[argr.length];
								for(int i=0; i<args.length; i++)
									args[i] = argr[i].run(global, scope).get();
								if(ref instanceof ParentedReferenceable)
									return new ValueReferenceable(func.call(((ParentedReferenceable)ref).parent() instanceof Scope ? net.nexustools.njs.Undefined.INSTANCE : (BaseObject)((ParentedReferenceable)ref).parent(), args));
								else
									return new ValueReferenceable(func.call(global, args));
							} finally {
								el.finishCall();
							}
						}
					};
			}
		} else if(object instanceof Multiply) {
			final Runnable lhs = compile(data, ((Multiply)object).lhs);
			final Runnable rhs = compile(data, ((Multiply)object).rhs);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						return new ValueReferenceable(global.Number.from(JSHelper.valueOf(lhs.run(global, scope).get())).multiply(global.Number.from(JSHelper.valueOf(rhs.run(global, scope).get()))));
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof LessThan) {
			final Runnable lhs = compile(data, ((LessThan)object).lhs);
			final Runnable rhs = compile(data, ((LessThan)object).rhs);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						return new ValueReferenceable(global.wrap(global.Number.from(JSHelper.valueOf(lhs.run(global, scope).get())).number < global.Number.from(JSHelper.valueOf(rhs.run(global, scope).get())).number));
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof MoreThan) {
			final Runnable lhs = compile(data, ((MoreThan)object).lhs);
			final Runnable rhs = compile(data, ((MoreThan)object).rhs);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						return new ValueReferenceable(global.wrap(global.Number.from(JSHelper.valueOf(lhs.run(global, scope).get())).number > global.Number.from(JSHelper.valueOf(rhs.run(global, scope).get())).number));
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof MultiplyEq) {
			final Runnable lhs = compile(data, ((MultiplyEq)object).lhs);
			final Runnable rhs = compile(data, ((MultiplyEq)object).rhs);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						Referenceable ref = lhs.run(global, scope);

						net.nexustools.njs.Number.Instance number = global.Number.from(JSHelper.valueOf(ref.get())).multiply(global.Number.from(JSHelper.valueOf(rhs.run(global, scope).get())));
						ref.set(number);
						return new ValueReferenceable(number);
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof Plus) {
			final Runnable lhs = compile(data, ((Plus)object).lhs);
			final Runnable rhs = compile(data, ((Plus)object).rhs);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						BaseObject l = JSHelper.valueOf(lhs.run(global, scope).get());
						if(l instanceof net.nexustools.njs.Number.Instance)
							return new ValueReferenceable(global.Number.from(l).plus(global.Number.from(JSHelper.valueOf(rhs.run(global, scope).get()))));

						StringBuilder builder = new StringBuilder();
						builder.append(l.toString());
						builder.append(JSHelper.valueOf(rhs.run(global, scope).get()).toString());
						return new ValueReferenceable(global.wrap(builder.toString()));
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof OpenBracket) {
			final Runnable contents = compile(data, ((OpenBracket)object).contents);
			final List<java.lang.String> chain = ((OpenBracket)object).chain;
			if(chain.isEmpty())
				return contents;
			
			final java.lang.String full = object.toString();
			final java.lang.String key = chain.remove(chain.size()-1);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						BaseObject obj = contents.run(global, scope).get();
						for(java.lang.String key : chain)
							obj = obj.get(key);
						return new StringKeyReferenceable(full, key, obj);
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof Set) {
			final Runnable lhs = compile(data, ((Set)object).lhs);
			final Runnable rhs = compile(data, ((Set)object).rhs);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						Referenceable ref = lhs.run(global, scope);
						BaseObject r = rhs.run(global, scope).get();
						ref.set(r);
						return new ValueReferenceable(r);
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof AbstractCompiler.Return) {
			final Runnable ret = compile(data, ((AbstractCompiler.Return)object).rhs);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						return new Return(ret.run(global, scope).get());
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof Var) {
			List<Var.Set> ret = ((Var)object).sets;
			
			final int len = ret.size();
			final java.lang.String[] keys = new java.lang.String[len];
			final Runnable[] values = new Runnable[len];
			
			for(int i=0; i<len; i++) {
				Var.Set set = ret.get(i);
				keys[i] = set.lhs;
				if(set.rhs != null)
					values[i] = compile(data, set.rhs);
			}
			
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						int i=0;
						for(; i<len; i++) {
							if(values[i] != null)
								scope.var(keys[i], values[i].run(global, scope).get());
							else
								scope.var(keys[i]);
						}
						for(; i<keys.length; i++) {
							if(values[i] != null)
								scope.var(keys[i], values[i].run(global, scope).get());
							else
								scope.var(keys[i]);
						}
						return UNDEFINED_REFERENCE;
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof Function) {
			final java.lang.String[] args = ((Function)object).arguments.toArray(new java.lang.String[((Function)object).arguments.size()]);
			final java.lang.String name = ((Function)object).name == null ? "<anonymous>" : ((Function)object).name;
			final java.lang.String source = ((Function)object).source;
			
			StringBuilder argBuilder = new StringBuilder();
			Iterator<java.lang.String> it = ((Function)object).arguments.iterator();
			if(it.hasNext()) {
				argBuilder.append(it.next());
				while(it.hasNext()) {
					argBuilder.append(", ");
					argBuilder.append(it.next());
				}
			}
			final java.lang.String arguments = argBuilder.toString();
			final Script impl;
			methodNameStack.add(name);
			try {
				impl = compileScript(((Function)object).impl, data.fileName, true);
			} finally {
				methodNameStack.remove(methodNameStack.size()-1);
			}
			final int subRows = ((Function)object).impl.rows;
			final int subColumns = ((Function)object).impl.columns;
			final java.lang.String stackName = methodNameStack.isEmpty() ? name : join(methodNameStack, '.') + '.' + name;
			return new Runnable() {
				@Override
				public Referenceable run(final Global global, final Scope scope) {
					AbstractFunction func = new ConstructableFunction(global) {
						@Override
						public BaseObject call(BaseObject _this, BaseObject... params) {
							ReplacementStackTraceElement el = JSHelper.renameCall(stackName, data.fileName, subRows, subColumns);
							try {
								Scope s = scope.extend(_this);
								s.var("arguments", new net.nexustools.njs.Arguments(global, this, params));
								int max = Math.min(args.length, params.length);
								int i=0;
								for(; i<max; i++)
									s.var(args[i], params[i]);
								for(; i<args.length; i++)
									s.var(args[i]);
								s.var("callee", this);
								s.enter();
								try {
									return impl.exec(global, s);
								} finally {
									s.exit();
								}
							} finally {
								el.finishCall();
							}
						}
						@Override
						public java.lang.String source() {
							return source;
						}
						@Override
						public java.lang.String arguments() {
							return arguments;
						}
						@Override
						public java.lang.String name() {
							return name;
						}
					};
					return new ValueReferenceable(func);
				}
			};
		} else if(object instanceof RightReference) {
			final java.lang.String source = object.toString();
			final Runnable ref = compile(data, ((RightReference)object).ref);
			final Iterable<java.lang.String> keys = ((RightReference)object).chain;
			final java.lang.String key = ((List<java.lang.String>)keys).remove(((List)keys).size()-1);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						BaseObject lhs = ref.run(global, scope).get();
						Iterator<java.lang.String> it = keys.iterator();
						while(it.hasNext())
							lhs = lhs.get(it.next());
						return new StringKeyReferenceable(source, key, lhs);
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof OpenArray) {
			final Runnable[] entries = new Runnable[((OpenArray)object).entries.size()];
			for(int i=0; i<entries.length; i++)
				entries[i] = compile(data, ((OpenArray)object).entries.get(i));
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						GenericArray array = new GenericArray(global, entries.length);
						for(int i=0; i<entries.length; i++)
							array.set(i, entries[i].run(global, scope).get());
						return new ValueReferenceable(array);
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof Or) {
			final Runnable lhs = compile(data, ((Or)object).lhs);
			final Runnable rhs = compile(data, ((Or)object).rhs);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						BaseObject l = lhs.run(global, scope).get();
						if(JSHelper.isTrue(l))
							return new ValueReferenceable(l);
						return rhs.run(global, scope);
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof PlusPlus) {
			if(((PlusPlus)object).lhs != null) {
				final Runnable lhs = compile(data, ((PlusPlus)object).lhs);
				return new Runnable() {
					@Override
					public Referenceable run(Global global, Scope scope) {
						ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
						try {
							Referenceable ref = lhs.run(global, scope);
							net.nexustools.njs.Number.Instance val = global.toNumber(ref.get());
							System.out.println("Currently: " + val);
							ref.set(global.wrap(val.number + 1));
							return new ValueReferenceable(val);
						} finally {
							el.finishCall();
						}
					}
				};
			} else {
				final Runnable rhs = compile(data, ((PlusPlus)object).rhs);
				return new Runnable() {
					@Override
					public Referenceable run(Global global, Scope scope) {
						ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
						try {
							Referenceable ref = rhs.run(global, scope);
							net.nexustools.njs.Number.Instance val = global.toNumber(ref.get());
							System.out.println("Currently: " + val);
							ref.set(val = global.wrap(val.number+1));
							return new ValueReferenceable(val);
						} finally {
							el.finishCall();
						}
					}
				};
			}
		
		} else if(object instanceof OpenGroup) {
			final Map<java.lang.String, Runnable> compiled = new HashMap();
			for(Map.Entry<java.lang.String, Part> entry : ((OpenGroup)object).entries.entrySet()) {
				compiled.put(entry.getKey(), compile(data, entry.getValue()));
			}
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						GenericObject object = new GenericObject(global);
						for(Map.Entry<java.lang.String, Runnable> entry : compiled.entrySet()) {
							object.setStorage(entry.getKey(), entry.getValue().run(global, scope).get(), true);
						}
						return new ValueReferenceable(object);
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof If) {
			final Runnable condition = compile(data, ((If)object).condition);
			if(((If)object).simpleimpl != null) {
				final Runnable impl = compile(data, ((If)object).simpleimpl);
				if(((If)object).el != null) {
					Else el = ((If)object).el;
					if(el instanceof ElseIf) {
						
					} else {
						if(el.simpleimpl != null) {
							final Runnable elimpl = compile(data, el.simpleimpl);
							return new Runnable() {
								@Override
								public Referenceable run(Global global, Scope scope) {
									ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
									try {
										if(JSHelper.isTrue(condition.run(global, scope).get())) {
											Referenceable ref = impl.run(global, scope);
											if(ref instanceof Return)
												return ref;
											ref.get();
										} else {
											Referenceable ref = elimpl.run(global, scope);
											if(ref instanceof Return)
												return ref;
											ref.get();
										}
									} finally {
										el.finishCall();
									}

									return UNDEFINED_REFERENCE;
								}
							};
						}
					}
				} else
					return new Runnable() {
						@Override
						public Referenceable run(Global global, Scope scope) {
							ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
							try {
								if(JSHelper.isTrue(condition.run(global, scope).get())) {
									Referenceable ref = impl.run(global, scope);
									if(ref instanceof Return)
										return ref;
									ref.get();
								}
							} finally {
								el.finishCall();
							}

							return UNDEFINED_REFERENCE;
						}
					};
			} else {
				final Script impl = compileScript(((If)object).impl, data.fileName, ScriptType.Block);
				if(((If)object).el != null) {
					Else el = ((If)object).el;
					if(el instanceof ElseIf) {
						
					} else {
						if(el.simpleimpl != null) {
							final Runnable elimpl = compile(data, el.simpleimpl);
							return new Runnable() {
								@Override
								public Referenceable run(Global global, Scope scope) {
									ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
									try {
										if(JSHelper.isTrue(condition.run(global, scope).get())) {
											BaseObject ret = impl.exec(global, scope);
											if(ret != null)
												return new Return(ret);
										} else {
											Referenceable ref = elimpl.run(global, scope);
											if(ref instanceof Return)
												return ref;
											ref.get();
										}
									} finally {
										el.finishCall();
									}

									return UNDEFINED_REFERENCE;
								}
							};
						}
					}
				} else
					return new Runnable() {
						@Override
						public Referenceable run(Global global, Scope scope) {
							ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
							try {
								if(JSHelper.isTrue(condition.run(global, scope).get())) {
									BaseObject ret = impl.exec(global, scope);
									if(ret != null)
										return new ValueReferenceable(ret);
								}
							} finally {
								el.finishCall();
							}

							return UNDEFINED_REFERENCE;
						}
					};
			}
		} else if(object instanceof Boolean) {
			final boolean value = ((Boolean)object).value;
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						return new ValueReferenceable(global.wrap(value));
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof VariableReference) {
			final java.lang.String source = object.toString();
			final Runnable lhs = compile(data, ((VariableReference)object).lhs);
			final Runnable ref = compile(data, ((VariableReference)object).ref);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						return new ObjectKeyReferenceable(source, ref.run(global, scope).get(), lhs.run(global, scope).get());
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof Delete) {
			final Runnable ref = compile(data, ((Delete)object).rhs);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						return new ValueReferenceable(global.wrap(ref.run(global, scope).delete()));
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof IntegerReference) {
			final int integer = ((IntegerReference)object).ref;
			final java.lang.String source = object.toString();
			final Runnable lhs = compile(data, ((IntegerReference)object).lhs);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						return new IntegerKeyReferenceable(source, integer, lhs.run(global, scope).get());
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof InstanceOf) {
			final Runnable lhs = compile(data, ((InstanceOf)object).lhs);
			final Runnable rhs = compile(data, ((InstanceOf)object).rhs);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						return new ValueReferenceable(global.wrap(lhs.run(global, scope).get().instanceOf((BaseFunction)rhs.run(global, scope).get())));
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof Try) {
			Try t = (Try)object;
			final Script impl = compileScript(t.impl, data.fileName, ScriptType.Block);
			if(t.c != null && t.f != null) {
				final java.lang.String key = ((Reference)t.c.condition).ref;
				final Script cimpl = compileScript(t.c.impl, data.fileName, ScriptType.Block);
				final Script fimpl = compileScript(t.f.impl, data.fileName, ScriptType.Block);
				return new Runnable() {
					@Override
					public Referenceable run(Global global, Scope scope) {
						ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
						try {
							Scope extended = scope.extend();
							try {
								BaseObject ret = impl.exec(global, extended);
								if(ret != null)
									return new Return(ret);
							} catch(Throwable t) {
								if(t instanceof net.nexustools.njs.Error.InvisibleException)
									throw (net.nexustools.njs.Error.InvisibleException)t;

								extended.set(key, global.wrap(t));
								BaseObject ret = cimpl.exec(global, extended);
								if(ret != null)
									return new Return(ret);
							} finally {
								BaseObject ret = fimpl.exec(global, extended);
								if(ret != null)
									return new Return(ret);
							}
						} finally {
							el.finishCall();
						}
						
						return UNDEFINED_REFERENCE;
					}
				};
			} else if(t.c != null) {
				final java.lang.String key = ((Reference)t.c.condition).ref;
				final Script cimpl = compileScript(t.c.impl, data.fileName, ScriptType.Block);
				return new Runnable() {
					@Override
					public Referenceable run(Global global, Scope scope) {
						ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
						try {
							Scope extended = scope.extend();
							try {
								BaseObject ret = impl.exec(global, extended);
								if(ret != null)
									return new Return(ret);
							} catch(Throwable t) {
								if(t instanceof net.nexustools.njs.Error.InvisibleException)
									throw (net.nexustools.njs.Error.InvisibleException)t;

								extended.var(key, global.wrap(t));
								BaseObject ret = cimpl.exec(global, extended);
								if(ret != null)
									return new Return(ret);
							}
						} finally {
							el.finishCall();
						}
						
						return UNDEFINED_REFERENCE;
					}
				};
			} else if(t.f != null) {
				final Script fimpl = compileScript(t.f.impl, data.fileName, ScriptType.Block);
				return new Runnable() {
					@Override
					public Referenceable run(Global global, Scope scope) {
						ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
						try {
							Scope extended = scope.extend();
							try {
								BaseObject ret = impl.exec(global, extended);
								if(ret != null)
									return new Return(ret);
							} finally {
								BaseObject ret = fimpl.exec(global, extended);
								if(ret != null)
									return new Return(ret);
							}
						} finally {
							el.finishCall();
						}
						
						return UNDEFINED_REFERENCE;
					}
				};
			}
		} else if(object instanceof Throw) {
			final Runnable rhs = compile(data, ((Throw)object).rhs);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						throw new net.nexustools.njs.Error.Thrown(rhs.run(global, scope).get());
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof While) {
			final Runnable condition = compile(data, ((While)object).condition);
			final Script impl = compileScript(((While)object).impl, data.fileName, ScriptType.Block);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					ReplacementStackTraceElement el = JSHelper.renameCall(data.methodName, data.fileName, rows, columns);
					try {
						Scope extended = scope.extend();
						extended.enter();
						try {
							while(JSHelper.isTrue(condition.run(global, scope).get())) {
								BaseObject ret = impl.exec(global, scope);
								if(ret != null)
									return new ValueReferenceable(ret);
							}

							return UNDEFINED_REFERENCE;
						} finally {
							extended.exit();
						}
					} finally {
						el.finishCall();
					}
				}
			};
		} else if(object instanceof RegEx) {
			final Pattern pattern = Pattern.compile(((RegEx)object).pattern);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					return new ValueReferenceable(global.wrap(pattern));
				}
			};
		} else if(object instanceof Undefined)
			return UNDEFINED;
		else if(object instanceof Null)
			return NULL;
		
		throw new UnsupportedOperationException("Cannot compile: " + object + " (" + object.getClass().getSimpleName() + ')');
	}
	private static enum ScriptType {
		Global,
		Function,
		Block
	}
	@Override
	protected Script compileScript(ScriptData script, java.lang.String fileName, boolean inFunction) {
		return compileScript(script, fileName, inFunction ? ScriptType.Function : ScriptType.Global);
	}
	private final HashMap<java.lang.String, WeakReference<Script>> scriptCache = new HashMap();
	protected Script compileScript(ScriptData script, java.lang.String fileName, ScriptType scriptType) {
		Script compiled;
		java.lang.String id = script.toString() + ':' + scriptType;
		synchronized(scriptCache) {
			WeakReference<Script> ref = scriptCache.get(id);
			if(ref == null || (compiled = ref.get()) == null)
				scriptCache.put(id, new WeakReference(compiled = compileScript0(script, fileName, scriptType)));
			return compiled;
		}
	}
	private Script compileScript0(final ScriptData script, java.lang.String fileName, final ScriptType scriptType) {
		if(script.impl.length == 0)
			return new Script() {
				@Override
				public BaseObject exec(Global global, Scope scope) {
					return net.nexustools.njs.Undefined.INSTANCE;
				}
				@Override
				public java.lang.String toString() {
					return join(Arrays.asList(script.impl), ';');
				}
				@Override
				public java.lang.String source() {
					return script.source;
				}
			};
		
		final int rows = script.rows;
		final int columns = script.columns;
		if(scriptType != ScriptType.Global) {
			final ScriptCompilerData precompiled = precompile(script, fileName);
			final Runnable[] parts = new Runnable[script.impl.length];
			for(int i=0; i<parts.length; i++)
				parts[i] = compile(precompiled, script.impl[i]);
			final int max = parts.length;
			return new Script() {
				@Override
				public BaseObject exec(Global global, Scope scope) {
					if(scope == null)
						scope = new Scope.Extended(global);
					
					scope.enter();
					ReplacementStackTraceElement el = JSHelper.renameCall(precompiled.methodName, precompiled.fileName, rows, columns);
					try {
						precompiled.exec(global, scope);
						for(int i=0; i<max; i++) {
							Referenceable ref = parts[i].run(global, scope);
							if(ref instanceof Return)
								return ref.get();
							ref.get();
						}
						return scriptType == ScriptType.Block ? null : net.nexustools.njs.Undefined.INSTANCE;
					} finally {
						el.finishCall();
						scope.exit();
					}
				}
				@Override
				public java.lang.String toString() {
					return join(Arrays.asList(script.impl), ';');
				}
				@Override
				public java.lang.String source() {
					return script.source;
				}
			};
		}
		
		if(script.impl.length == 1) {
			final ScriptCompilerData precompiled = precompile(script, fileName);
			final Runnable impl = compile(precompiled, script.impl[0]);
			return new Script() {
				@Override
				public BaseObject exec(Global global, Scope scope) {
					if(scope == null)
						scope = new Scope(global);
					scope.enter();
					ReplacementStackTraceElement el = JSHelper.renameCall(precompiled.methodName, precompiled.fileName, rows, columns);
					try {
						precompiled.exec(global, scope);
						return impl.run(global, scope).get();
					} finally {
						el.finishCall();
						scope.exit();
					}
				}
				@Override
				public java.lang.String toString() {
					return join(Arrays.asList(script.impl), ';');
				}
				@Override
				public java.lang.String source() {
					return script.source;
				}
			};
		}
		
		final ScriptCompilerData precompiled = precompile(script, fileName);
		final Runnable[] parts = new Runnable[script.impl.length];
		for(int i=0; i<parts.length; i++)
			parts[i] = compile(precompiled, script.impl[i]);
		switch(parts.length) {
			case 2:
				return new Script() {
					@Override
					public BaseObject exec(Global global, Scope scope) {
						if(scope == null)
							scope = new Scope(global);
						
						scope.enter();
						ReplacementStackTraceElement el = JSHelper.renameCall(precompiled.methodName, precompiled.fileName, rows, columns);
						try {
							precompiled.exec(global, scope);
							parts[0].run(global, scope);
							return parts[1].run(global, scope).get();
						} finally {
							el.finishCall();
							scope.exit();
						}
					}
					@Override
					public java.lang.String toString() {
						return join(Arrays.asList(script.impl), ';');
					}
					@Override
					public java.lang.String source() {
						return script.source;
					}
				};
				
			case 3:
				return new Script() {
					@Override
					public BaseObject exec(Global global, Scope scope) {
						if(scope == null)
							scope = new Scope(global);
						
						scope.enter();
						ReplacementStackTraceElement el = JSHelper.renameCall(precompiled.methodName, precompiled.fileName, rows, columns);
						try {
							precompiled.exec(global, scope);
							parts[0].run(global, scope);
							parts[1].run(global, scope);
							return parts[2].run(global, scope).get();
						} finally {
							el.finishCall();
							scope.exit();
						}
					}
					@Override
					public java.lang.String toString() {
						return join(Arrays.asList(script.impl), ';');
					}
					@Override
					public java.lang.String source() {
						return script.source;
					}
				};
				
			default:
				final int max = parts.length;
				return new Script() {
					@Override
					public BaseObject exec(Global global, Scope scope) {
						BaseObject lastValue = null;
						if(scope == null)
							scope = new Scope(global);
						
						scope.enter();
						ReplacementStackTraceElement el = JSHelper.renameCall(precompiled.methodName, precompiled.fileName, rows, columns);
						try {
							precompiled.exec(global, scope);
							for(int i=0; i<max; i++)
								lastValue = parts[i].run(global, scope).get();
							return lastValue;
						} finally {
							el.finishCall();
							scope.exit();
						}
					}
					@Override
					public java.lang.String toString() {
						return join(Arrays.asList(script.impl), ';');
					}
					@Override
					public java.lang.String source() {
						return script.source;
					}
				};
		}
	}
}
