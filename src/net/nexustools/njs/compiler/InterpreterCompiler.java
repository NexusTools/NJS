/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs.compiler;

import net.nexustools.njs.AbstractFunction;
import net.nexustools.njs.BaseFunction;
import net.nexustools.njs.BaseObject;
import net.nexustools.njs.Global;
import net.nexustools.njs.JSHelper;
import net.nexustools.njs.Scope;
import net.nexustools.njs.Scopeable;

import java.util.Iterator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.nexustools.njs.ConstructableFunction;
import net.nexustools.njs.GenericArray;
import net.nexustools.njs.GenericObject;

/**
 *
 * @author kate
 */
public class InterpreterCompiler extends AbstractCompiler {
	public static class Referenceable {

		final java.lang.String key;
		final java.lang.String full;
		final Scopeable object;
		public Referenceable(BaseObject object) {
			this(null, null, object);
		}
		public Referenceable(java.lang.String key, Scopeable object) {
			this(key, key, object);
		}
		public Referenceable(java.lang.String key, java.lang.String full, Scopeable object) {
			this.key = key;
			this.full = full;
			this.object = object;
		}

		private BaseObject resolve() {
			if(key != null)
				return object.get(key);
			return (BaseObject)object;
		}
		
		private static java.lang.String join(Iterator<java.lang.String> chain) {
			StringBuilder builder = new StringBuilder();
			if(chain.hasNext()) {
				builder.append(chain.next());
				while(chain.hasNext()) {
					builder.append('.');
					builder.append(chain.next());
				}
			}
			return builder.toString();
		}
	}
	public static class Return extends Referenceable {
		public Return(BaseObject object) {
			super(object);
		}
	}
	public static interface Runnable {
		public Referenceable run(Global global, Scope scope);
	}
	public static final Referenceable UNDEFINED_REFERENCE = new Referenceable(net.nexustools.njs.Undefined.INSTANCE);
	public static final Runnable UNDEFINED = new Runnable() {
		@Override
		public Referenceable run(Global global, Scope scope) {
			return UNDEFINED_REFERENCE;
		}
	};
	public static final Runnable NULL = new Runnable() {
		public final Referenceable REFERENCE = new Referenceable(net.nexustools.njs.Null.INSTANCE);
		@Override
		public Referenceable run(Global global, Scope scope) {
			return REFERENCE;
		}
	};
	
	private Runnable compile(Object object) {
		if(object instanceof Integer) {
			final double number = ((Integer)object).value;
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					return new Referenceable(global.wrap(number));
				}
			};
		} else if(object instanceof Number) {
			final double number = ((Number)object).value;
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					return new Referenceable(global.wrap(number));
				}
			};
		} else if(object instanceof String) {
			final java.lang.String string = ((String)object).string;
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					return new Referenceable(global.wrap(string));
				}
			};
		} else if(object instanceof Reference) {
			final java.lang.String ref = ((Reference)object).ref;
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					return new Referenceable(ref, ref, scope);
				}
			};
		} else if(object instanceof ReferenceChain) {
			final Iterable<java.lang.String> chain = ((ReferenceChain)object).chain;
			final java.lang.String full = Referenceable.join(chain.iterator());
			final java.lang.String key = ((ReferenceChain)object).chain.remove(((ReferenceChain)object).chain.size()-1);
			final java.lang.String base = Referenceable.join(chain.iterator());
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					try {
						return new Referenceable(key, full, scope.resolve(chain));
					} catch(net.nexustools.njs.Error.JavaException err) {
						if(err.type.equals("TypeError")) {
							if(err.getUnderlyingMessage().endsWith("from null"))
								throw new net.nexustools.njs.Error.JavaException("TypeError", "Cannot read property \"" + key + "\" from \"" + base + "\" which is null");
							if(err.getUnderlyingMessage().endsWith("from undefined"))
								throw new net.nexustools.njs.Error.JavaException("TypeError", "Cannot read property \"" + key + "\" from \"" + base + "\" which is undefined");
						}
						throw err;
					}
				}
			};
		} else if(object instanceof New) {
			final Runnable reference = compile(((New)object).reference);
			
			if(((New)object).arguments == null || ((New)object).arguments.isEmpty())
				return new Runnable() {
					@Override
					public Referenceable run(Global global, Scope scope) {
						return new Referenceable(((BaseFunction)reference.run(global, scope).resolve()).construct());
					}
				};
			
			Object[] args = ((New)object).arguments.toArray();
			final Runnable[] argr = new Runnable[args.length];
			for(int i=0; i<args.length; i++)
				argr[i] = compile(args[i]);
			
			switch(args.length) {
				case 1:
					return new Runnable() {
						@Override
						public Referenceable run(Global global, Scope scope) {
							return new Referenceable(((BaseFunction)reference.run(global, scope).resolve()).construct(argr[0].run(global, scope).resolve()));
						}
					};
					
				case 2:
					return new Runnable() {
						@Override
						public Referenceable run(Global global, Scope scope) {
							return new Referenceable(((BaseFunction)reference.run(global, scope).resolve()).construct(argr[0].run(global, scope).resolve(), argr[1].run(global, scope).resolve()));
						}
					};
					
				case 3:
					return new Runnable() {
						@Override
						public Referenceable run(Global global, Scope scope) {
							return new Referenceable(((BaseFunction)reference.run(global, scope).resolve()).construct(argr[0].run(global, scope).resolve(), argr[1].run(global, scope).resolve(), argr[2].run(global, scope).resolve()));
						}
					};
					
				default:
					return new Runnable() {
						@Override
						public Referenceable run(Global global, Scope scope) {
							BaseObject[] args = new BaseObject[argr.length];
							for(int i=0; i<args.length; i++)
								args[i] = argr[i].run(global, scope).resolve();
							return new Referenceable(((BaseFunction)reference.run(global, scope).resolve()).construct(args));
						}
					};
			}
		} else if(object instanceof Call) {
			final Runnable reference = compile(((Call)object).reference);
			
			if(((Call)object).arguments.isEmpty())
				return new Runnable() {
					@Override
					public Referenceable run(Global global, Scope scope) {
						Referenceable ref = reference.run(global, scope);
						BaseFunction func;
						try {
							if(ref.key != null)
								func = ((BaseFunction)ref.resolve());
							else
								func = ((BaseFunction)ref.object);
						} catch(Exception ex) {
							if(ref.full != null)
								throw new net.nexustools.njs.Error.JavaException("TypeError", ref.full + " is not a function");
							throw new net.nexustools.njs.Error.JavaException("TypeError", "is not a function");
						}
						if(ref.key != null)
							return new Referenceable(func.call(ref.object instanceof Scope ? net.nexustools.njs.Undefined.INSTANCE : (BaseObject)ref.object));
						else
							return new Referenceable(func.call(global));
					}
				};
			
			Object[] args = ((Call)object).arguments.toArray();
			final Runnable[] argr = new Runnable[args.length];
			for(int i=0; i<args.length; i++)
				argr[i] = compile(args[i]);
			
			switch(args.length) {
				case 1:
					return new Runnable() {
						@Override
						public Referenceable run(Global global, Scope scope) {
							Referenceable ref = reference.run(global, scope);
							BaseFunction func;
							try {
								if(ref.key != null)
									func = ((BaseFunction)ref.resolve());
								else
									func = ((BaseFunction)ref.object);
							} catch(ClassCastException ex) {
								if(ref.full != null)
									throw new net.nexustools.njs.Error.JavaException("TypeError", ref.full + " is not a function");
								throw new net.nexustools.njs.Error.JavaException("TypeError", "is not a function");
							}
							if(ref.key != null)
								return new Referenceable(func.call(ref.object instanceof Scope ? net.nexustools.njs.Undefined.INSTANCE : (BaseObject)ref.object, argr[0].run(global, scope).resolve()));
							else
								return new Referenceable(func.call(global, argr[0].run(global, scope).resolve()));
						}
					};
					
				case 2:
					return new Runnable() {
						@Override
						public Referenceable run(Global global, Scope scope) {
							Referenceable ref = reference.run(global, scope);
							BaseFunction func;
							try {
								if(ref.key != null)
									func = ((BaseFunction)ref.resolve());
								else
									func = ((BaseFunction)ref.object);
							} catch(Exception ex) {
								if(ref.full != null)
									throw new net.nexustools.njs.Error.JavaException("TypeError", ref.full + " is not a function");
								throw new net.nexustools.njs.Error.JavaException("TypeError", "is not a function");
							}
							if(ref.key != null)
								return new Referenceable(func.call(ref.object instanceof Scope ? net.nexustools.njs.Undefined.INSTANCE : (BaseObject)ref.object, argr[0].run(global, scope).resolve(), argr[1].run(global, scope).resolve()));
							else
								return new Referenceable(func.call(global, argr[0].run(global, scope).resolve(), argr[1].run(global, scope).resolve()));
						}
					};
					
				case 3:
					return new Runnable() {
						@Override
						public Referenceable run(Global global, Scope scope) {
							Referenceable ref = reference.run(global, scope);
							BaseFunction func;
							try {
								if(ref.key != null)
									func = ((BaseFunction)ref.resolve());
								else
									func = ((BaseFunction)ref.object);
							} catch(Exception ex) {
								if(ref.full != null)
									throw new net.nexustools.njs.Error.JavaException("TypeError", ref.full + " is not a function");
								throw new net.nexustools.njs.Error.JavaException("TypeError", "is not a function");
							}
							if(ref.key != null)
								return new Referenceable(func.call(ref.object instanceof Scope ? net.nexustools.njs.Undefined.INSTANCE : (BaseObject)ref.object, argr[0].run(global, scope).resolve(), argr[1].run(global, scope).resolve(), argr[2].run(global, scope).resolve()));
							else
								return new Referenceable(func.call(global, argr[0].run(global, scope).resolve(), argr[1].run(global, scope).resolve(), argr[2].run(global, scope).resolve()));
						}
					};
					
				default:
					return new Runnable() {
						@Override
						public Referenceable run(Global global, Scope scope) {
							Referenceable ref = reference.run(global, scope);
							BaseFunction func;
							try {
								if(ref.key != null)
									func = ((BaseFunction)ref.resolve());
								else
									func = ((BaseFunction)ref.object);
							} catch(Exception ex) {
								if(ref.full != null)
									throw new net.nexustools.njs.Error.JavaException("TypeError", ref.full + " is not a function");
								throw new net.nexustools.njs.Error.JavaException("TypeError", "is not a function");
							}
							BaseObject[] args = new BaseObject[argr.length];
							for(int i=0; i<args.length; i++)
								args[i] = argr[i].run(global, scope).resolve();
							if(ref.key != null)
								return new Referenceable(func.call(ref.object instanceof Scope ? net.nexustools.njs.Undefined.INSTANCE : (BaseObject)ref.object, args));
							else
								return new Referenceable(func.call(global, args));
						}
					};
			}
		} else if(object instanceof Multiply) {
			final Runnable lhs = compile(((Multiply)object).lhs);
			final Runnable rhs = compile(((Multiply)object).rhs);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					return new Referenceable(global.Number.from(JSHelper.valueOf(lhs.run(global, scope).resolve())).multiply(global.Number.from(JSHelper.valueOf(rhs.run(global, scope).resolve()))));
				}
			};
		} else if(object instanceof LessThan) {
			final Runnable lhs = compile(((LessThan)object).lhs);
			final Runnable rhs = compile(((LessThan)object).rhs);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					return new Referenceable(global.wrap(global.Number.from(JSHelper.valueOf(lhs.run(global, scope).resolve())).number < global.Number.from(JSHelper.valueOf(rhs.run(global, scope).resolve())).number));
				}
			};
		} else if(object instanceof MoreThan) {
			final Runnable lhs = compile(((MoreThan)object).lhs);
			final Runnable rhs = compile(((MoreThan)object).rhs);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					return new Referenceable(global.wrap(global.Number.from(JSHelper.valueOf(lhs.run(global, scope).resolve())).number > global.Number.from(JSHelper.valueOf(rhs.run(global, scope).resolve())).number));
				}
			};
		} else if(object instanceof MultiplyEq) {
			final Runnable lhs = compile(((MultiplyEq)object).lhs);
			final Runnable rhs = compile(((MultiplyEq)object).rhs);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					Referenceable ref = lhs.run(global, scope);
					if(ref.key == null)
						throw new net.nexustools.njs.Error.JavaException("SyntaxError", "");
					
					net.nexustools.njs.Number.Instance number = global.Number.from(JSHelper.valueOf(ref.resolve())).multiply(global.Number.from(JSHelper.valueOf(rhs.run(global, scope).resolve())));
					ref.object.set(ref.key, number);
					return new Referenceable(number);
				}
			};
		} else if(object instanceof Plus) {
			final Runnable lhs = compile(((Plus)object).lhs);
			final Runnable rhs = compile(((Plus)object).rhs);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					BaseObject l = JSHelper.valueOf(lhs.run(global, scope).resolve());
					if(l instanceof net.nexustools.njs.Number.Instance)
						return new Referenceable(global.Number.from(l).plus(global.Number.from(JSHelper.valueOf(rhs.run(global, scope).resolve()))));
					
					StringBuilder builder = new StringBuilder();
					builder.append(l.toString());
					builder.append(JSHelper.valueOf(rhs.run(global, scope).resolve()).toString());
					return new Referenceable(global.wrap(builder.toString()));
				}
			};
		} else if(object instanceof OpenBracket) {
			final Runnable contents = compile(((OpenBracket)object).contents);
			final List<java.lang.String> chain = ((OpenBracket)object).chain;
			if(chain.isEmpty())
				return contents;
			
			final java.lang.String full = Referenceable.join(chain.iterator());
			final java.lang.String key = chain.remove(chain.size()-1);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					BaseObject obj = contents.run(global, scope).resolve();
					for(java.lang.String key : chain)
						obj = obj.get(key);
					return new Referenceable(key, full, obj);
				}
			};
		} else if(object instanceof Set) {
			final Runnable lhs = compile(((Set)object).lhs);
			final Runnable rhs = compile(((Set)object).rhs);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					Referenceable ref = lhs.run(global, scope);
					if(ref.key == null)
						throw new net.nexustools.njs.Error.JavaException("ReferenceError", "Cannot set value without knowing its parent and key");
					
					BaseObject r = rhs.run(global, scope).resolve();
					ref.object.set(ref.key, r);
					return new Referenceable(r);
				}
			};
		} else if(object instanceof AbstractCompiler.Return) {
			final Runnable ret = compile(((AbstractCompiler.Return)object).ret);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					return new Return(ret.run(global, scope).resolve());
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
					values[i] = compile(set.rhs);
			}
			
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					int i=0;
					for(; i<len; i++) {
						if(values[i] != null)
							scope.var(keys[i], values[i].run(global, scope).resolve());
						else
							scope.var(keys[i]);
					}
					for(; i<keys.length; i++) {
						if(values[i] != null)
							scope.var(keys[i], values[i].run(global, scope).resolve());
						else
							scope.var(keys[i]);
					}
					return UNDEFINED_REFERENCE;
				}
			};
		} else if(object instanceof Function) {
			final java.lang.String[] args = ((Function)object).arguments.toArray(new java.lang.String[((Function)object).arguments.size()]);
			final java.lang.String source = ((Function)object).source;
			final java.lang.String name = ((Function)object).name;
			
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
			final Script impl = compileScript(((Function)object).impl, true);
			return new Runnable() {
				@Override
				public Referenceable run(final Global global, final Scope scope) {
					AbstractFunction func = new ConstructableFunction(global) {
						@Override
						public BaseObject call(BaseObject _this, BaseObject... params) {
							Scope s = scope.extend(_this);
							s.var("arguments", new net.nexustools.njs.Arguments(global, this, params));
							int max = Math.min(args.length, params.length);
							int i=0;
							for(; i<max; i++)
								s.var(args[i], params[i]);
							for(; i<args.length; i++)
								s.var(args[i]);
							s.var("callee", this);
							return impl.exec(global, s);
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
							return name == null ? "anonymous" : name;
						}
					};
					if(name != null)
						scope.var(name, func);
					return new Referenceable(func);
				}
			};
		} else if(object instanceof RightReference) {
			final Runnable ref = compile(((RightReference)object).ref);
			final Iterable<java.lang.String> keys = ((RightReference)object).chain;
			final java.lang.String key = ((List<java.lang.String>)keys).remove(((List)keys).size()-1);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					BaseObject lhs = ref.run(global, scope).resolve();
					Iterator<java.lang.String> it = keys.iterator();
					while(it.hasNext())
						lhs = lhs.get(it.next());
					return new Referenceable(key, lhs);
				}
			};
		} else if(object instanceof OpenArray) {
			final Runnable[] entries = new Runnable[((OpenArray)object).entries.size()];
			for(int i=0; i<entries.length; i++)
				entries[i] = compile(((OpenArray)object).entries.get(i));
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					GenericArray array = new GenericArray(global, entries.length);
					for(int i=0; i<entries.length; i++)
						array.set(i, entries[i].run(global, scope).resolve());
					return new Referenceable(array);
				}
			};
		} else if(object instanceof Or) {
			final Runnable lhs = compile(((Or)object).lhs);
			final Runnable rhs = compile(((Or)object).rhs);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					BaseObject l = lhs.run(global, scope).resolve();
					if(JSHelper.isTrue(l))
						return new Referenceable(l);
					return rhs.run(global, scope);
				}
			};
		} else if(object instanceof PlusPlus) {
			if(((PlusPlus)object).lhs != null) {
				final Runnable lhs = compile(((PlusPlus)object).lhs);
				return new Runnable() {
					@Override
					public Referenceable run(Global global, Scope scope) {
						Referenceable ref = lhs.run(global, scope);
						if(ref.key == null)
							throw new net.nexustools.njs.Error.JavaException("ReferenceError", "Cannot set value without knowing its parent and key");
						
						net.nexustools.njs.Number.Instance val = global.toNumber(ref.resolve());
						ref.object.set(ref.key, global.wrap(val.number + 1));
						return new Referenceable(val);
					}
				};
			} else {
				final Runnable rhs = compile(((PlusPlus)object).rhs);
				return new Runnable() {
					@Override
					public Referenceable run(Global global, Scope scope) {
						Referenceable ref = rhs.run(global, scope);
						if(ref.key == null)
							throw new net.nexustools.njs.Error.JavaException("ReferenceError", "Cannot set value without knowing its parent and key");
						
						net.nexustools.njs.Number.Instance val = global.toNumber(ref.resolve());
						ref.object.set(ref.key, val = global.wrap(val.number+1));
						return new Referenceable(val);
					}
				};
			}
		} else if(object instanceof While) {
			final Runnable condition = compile(((While)object).condition);
			if(((While)object).simpleimpl != null) {
				final Runnable impl = compile(((While)object).simpleimpl);
				return new Runnable() {
					@Override
					public Referenceable run(Global global, Scope scope) {
						while(JSHelper.isTrue(condition.run(global, scope).resolve())) {
							Referenceable ref = impl.run(global, scope);
							if(ref instanceof Return)
								return ref;
							ref.resolve();
						}

						return UNDEFINED_REFERENCE;
					}
				};
			}
			final Script impl = compileScript(((While)object).impl, ScriptType.Block);
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					while(JSHelper.isTrue(condition.run(global, scope).resolve())) {
						BaseObject ret = impl.exec(global, scope.extend());
						if(ret != null)
							return new Return(ret);
					}
					
					return UNDEFINED_REFERENCE;
				}
			};
		} else if(object instanceof OpenGroup) {
			final Map<java.lang.String, Runnable> compiled = new HashMap();
			for(Map.Entry<java.lang.String, Part> entry : ((OpenGroup)object).entries.entrySet()) {
				compiled.put(entry.getKey(), compile(entry.getValue()));
			}
			return new Runnable() {
				@Override
				public Referenceable run(Global global, Scope scope) {
					GenericObject object = new GenericObject(global);
					for(Map.Entry<java.lang.String, Runnable> entry : compiled.entrySet()) {
						object.setStorage(entry.getKey(), entry.getValue().run(global, scope).resolve(), true);
					}
					return new Referenceable(object);
				}
			};
		} else if(object instanceof Try) {
			final Script impl = compileScript(((Try)object).impl, ScriptType.Block);
			if(((Try)object).c != null && ((Try)object).f != null) {
				final java.lang.String name = ((Reference)((Try)object).c.condition).ref;
				final Script c = compileScript(((Try)object).c.impl, ScriptType.Block);
				final Script f = compileScript(((Try)object).f.impl, ScriptType.Block);
				return new Runnable() {
					@Override
					public Referenceable run(Global global, Scope scope) {
						try {
							BaseObject ret = impl.exec(global, scope.extend());
							if(ret != null)
								return new Return(ret);
						} catch(Throwable t) {
							Scope extended = scope.extend();
							extended.var(name, JSHelper.javaToJS(global, t));
							BaseObject ret = c.exec(global, extended);
							if(ret != null)
								return new Return(ret);
						} finally {
							BaseObject ret = f.exec(global, scope.extend());
							if(ret != null)
								return new Return(ret);
						}
						
						return UNDEFINED_REFERENCE;
					}
				};
			} else if(((Try)object).c != null) {
				final java.lang.String name = ((Reference)((Try)object).c.condition).ref;
				final Script c = compileScript(((Try)object).c.impl, ScriptType.Block);
				return new Runnable() {
					@Override
					public Referenceable run(Global global, Scope scope) {
						try {
							BaseObject ret = impl.exec(global, scope.extend());
							if(ret != null)
								return new Return(ret);
						} catch(Throwable t) {
							Scope extended = scope.extend();
							extended.var(name, JSHelper.javaToJS(global, t));
							BaseObject ret = c.exec(global, extended);
							if(ret != null)
								return new Return(ret);
						}
						
						return UNDEFINED_REFERENCE;
					}
				};
			} else if(((Try)object).f != null) {
				final Script f = compileScript(((Try)object).f.impl, ScriptType.Block);
				return new Runnable() {
					@Override
					public Referenceable run(Global global, Scope scope) {
						try {
							BaseObject ret = impl.exec(global, scope.extend());
							if(ret != null)
								return new Return(ret);
						} finally {
							BaseObject ret = f.exec(global, scope.extend());
							if(ret != null)
								return new Return(ret);
						}
						
						return UNDEFINED_REFERENCE;
					}
				};
			}
		} else if(object instanceof If) {
			final Runnable condition = compile(((If)object).condition);
			if(((If)object).simpleimpl != null) {
				final Runnable impl = compile(((If)object).simpleimpl);
				return new Runnable() {
					@Override
					public Referenceable run(Global global, Scope scope) {
						if(JSHelper.isTrue(condition.run(global, scope).resolve())) {
							Referenceable ref = impl.run(global, scope);
							if(ref instanceof Return)
								return ref;
							ref.resolve();
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
					return new Referenceable(global.wrap(value));
				}
			};
		} else if(object instanceof Undefined)
			return UNDEFINED;
		else if(object instanceof Null)
			return NULL;
		
		throw new UnsupportedOperationException("Cannot compile: " + object + " (" + object.getClass().getSimpleName() + ')');
	}
	
	
	@Override
	protected Script compileScript(final Object[] script, boolean inFunction) {
		return compileScript(script, inFunction ? ScriptType.Function : ScriptType.Global);
	}
	
	public static enum ScriptType {
		Global,
		Function,
		Block
	}
	protected Script compileScript(final Object[] script, ScriptType type) {
		if(script.length == 0)
			return new Script() {
				@Override
				public BaseObject exec(Global global, Scope scope) {
					return net.nexustools.njs.Undefined.INSTANCE;
				}
				@Override
				public java.lang.String toString() {
					return Arrays.toString(script);
				}
			};
		
		switch(type) {
			case Function:
			{
				final Runnable[] parts = new Runnable[script.length];
				for(int i=0; i<parts.length; i++)
					parts[i] = compile(script[i]);
				final int max = parts.length;
				return new Script() {
					@Override
					public BaseObject exec(Global global, Scope scope) {
						if(scope == null)
							scope = new Scope.Extended(global);

						for(int i=0; i<max; i++) {
							Referenceable ref = parts[i].run(global, scope);
							if(ref instanceof Return)
								return ref.resolve();
							ref.resolve();
						}
						return net.nexustools.njs.Undefined.INSTANCE;
					}
					@Override
					public java.lang.String toString() {
						return Arrays.toString(script);
					}
				};
			}
			case Block:
			{
				final Runnable[] parts = new Runnable[script.length];
				for(int i=0; i<parts.length; i++)
					parts[i] = compile(script[i]);
				final int max = parts.length;
				return new Script() {
					@Override
					public BaseObject exec(Global global, Scope scope) {
						for(int i=0; i<max; i++) {
							Referenceable ref = parts[i].run(global, scope);
							if(ref instanceof Return)
								return ref.resolve();
							ref.resolve();
						}
						return null;
					}
					@Override
					public java.lang.String toString() {
						return Arrays.toString(script);
					}
				};
			}
		}
		
		if(script.length == 1) {
			final Runnable impl = compile(script[0]);
			return new Script() {
				@Override
				public BaseObject exec(Global global, Scope scope) {
					return impl.run(global, new Scope(global)).resolve();
				}
				@Override
				public java.lang.String toString() {
					return Arrays.toString(script);
				}
			};
		}
		
		final Runnable[] parts = new Runnable[script.length];
		for(int i=0; i<parts.length; i++)
			parts[i] = compile(script[i]);
		switch(parts.length) {
			case 2:
				return new Script() {
					@Override
					public BaseObject exec(Global global, Scope scope) {
						if(scope == null)
							scope = new Scope(global);
						
						parts[0].run(global, scope);
						return parts[1].run(global, scope).resolve();
					}
					@Override
					public java.lang.String toString() {
						return Arrays.toString(script);
					}
				};
				
			case 3:
				return new Script() {
					@Override
					public BaseObject exec(Global global, Scope scope) {
						if(scope == null)
							scope = new Scope(global);
						
						parts[0].run(global, scope);
						parts[1].run(global, scope);
						return parts[2].run(global, scope).resolve();
					}
					@Override
					public java.lang.String toString() {
						return Arrays.toString(script);
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
						
						for(int i=0; i<max; i++)
							lastValue = parts[i].run(global, scope).resolve();
						return lastValue;
					}
					@Override
					public java.lang.String toString() {
						return Arrays.toString(script);
					}
				};
		}
	}
}
