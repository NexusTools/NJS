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
package net.nexustools.njs;

import net.nexustools.njs.JSHelper.ReplacementStackTraceElement;
import net.nexustools.njs.compiler.Script;

/**
 *
 * @author kate
 */
public class Function extends AbstractFunction {
	private final Global global;
	public Function(Global global) {
		this.global = global;
	}

	public void initPrototypeFunctions(final Global global) {
		GenericObject prototype = (GenericObject)prototype();
		prototype.defineProperty("name", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return String.wrap(((BaseFunction)_this).name());
			}
		}, global.NOOP);
		prototype.defineProperty("prototype", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return ((BaseFunction)_this).prototype();
			}
		}, new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				((BaseFunction)_this).setPrototype(params[0]);
				return Undefined.INSTANCE;
			}
		});
		prototype.setHidden("toString", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				ReplacementStackTraceElement el = JSHelper.renameMethodCall("toString");
				try {
					if(_this instanceof BaseFunction) {
						StringBuilder builder = new StringBuilder("function");
						java.lang.String name = ((BaseFunction)_this).name();
						if(name != null && !name.startsWith("<")) {
							builder.append(' ');
							builder.append(name);
						}
						builder.append('(');
						builder.append(((BaseFunction)_this).arguments());
						builder.append("){");
						java.lang.String source = ((BaseFunction)_this).source();
						if(source.indexOf('\n') > -1)
							builder.append(source);
						else {
							builder.append(' ');
							builder.append(source);
							builder.append(' ');
						}
						builder.append('}');
						return global.wrap(builder.toString());
					}
					throw new Error.JavaException("TypeError", "this is not instance of Function");
				} finally {
					el.finishCall();
				}
			}
			@Override
			public java.lang.String toString() {
				return "Function_prototype_toString";
			}
		});
		prototype.setStorage("bind", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return new BoundFunction(global, params[0], (BaseFunction)_this);
			}
			@Override
			public java.lang.String name() {
				return "Function_prototype_bind";
			}
		}, false);
		prototype.setStorage("apply", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				BaseObject __this;
				BaseObject[] args;
				switch(params.length) {
					case 0:
						__this = Undefined.INSTANCE;
						args = new BaseObject[0];
						break;
					case 1:
						__this = params[0];
						args = new BaseObject[0];
						break;
					default:
						__this = params[0];
						BaseObject _args = params[1];
						int length = _args.get("length").toInt();
						args = new BaseObject[length];
						for(int i=0; i<length; i++)
							args[i] = _args.get(i);
						break;
				}
				
				return ((BaseFunction)_this).call(__this, args);
			}
			@Override
			public java.lang.String name() {
				return "Function_prototype_apply";
			}
		}, false);
		prototype.setStorage("call", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				ReplacementStackTraceElement el = JSHelper.renameMethodCall("Function.prototype.call");
				try {
					switch(params.length) {
						case 0:
							return ((BaseFunction)_this).call(Undefined.INSTANCE);

						case 1:
							return ((BaseFunction)_this).call(params[0]);

						case 2:
							return ((BaseFunction)_this).call(params[0], params[1]);

						case 3:
							return ((BaseFunction)_this).call(params[0], params[1], params[2]);

						case 4:
							return ((BaseFunction)_this).call(params[0], params[1], params[2], params[3]);

						default:
							final BaseObject target = params[0];
							final BaseObject[] ps = new BaseObject[params.length-1];
							System.arraycopy(params, 1, ps, 0, ps.length);
							return ((BaseFunction)_this).call(target, ps);
					}
				} finally {
					el.finishCall();
				}
			}
			@Override
			public java.lang.String name() {
				return "Function_prototype_call";
			}
		}, false);
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		final java.lang.String source;
		final java.lang.String[] args;
		final java.lang.String arguments;
		switch(params.length) {
			case 0:
				source = "";
				arguments = "";
				args = new java.lang.String[0];
				break;
			case 1:
				arguments = "";
				source = params[0].toString();
				args = new java.lang.String[0];
				break;
			case 2:
				source = params[1].toString();
				arguments = params[0].toString();
				args = arguments.split("\\s*,\\s*");
				break;
			default:
				int argCount = params.length-1;
				source = params[argCount].toString();
				args = new java.lang.String[argCount];
				StringBuilder _arguments = new StringBuilder();
				for(int i=0; i<argCount; i++) {
					if(_arguments.length() > 0)
						_arguments.append(", ");
					_arguments.append(args[i] = params[i].toString());
				}
				arguments = _arguments.toString();
		}
		
		final Script compiled = global.compiler.compile(source, "<Function>", true);
		return new AbstractFunction(global, "<Function>") {
			@Override
			public BaseObject call(BaseObject _this, final BaseObject... params) {
				final AbstractFunction self = this;
				Scope scope = new Scope.Extended(_this, global) {
					{
						var("callee", self);
						for(int i=0; i<java.lang.Math.min(params.length, args.length); i++)
							var(args[i], params[i]);
						var("arguments", new Arguments(global, self, params));
					}
				};
				return compiled.exec(global, scope);
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
				return get("name").toString();
			}
		};
	}

	@Override
	public BaseObject call(BaseObject _this, BaseObject... params) {
		if(!_this.instanceOf(this))
			return construct(params);
		return _this;
	}
	
}
