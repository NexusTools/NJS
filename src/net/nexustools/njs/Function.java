/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

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
		GenericObject prototype = prototype();
		prototype.setHidden("toString", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				JSHelper.renameMethodCall("toString");
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
					JSHelper.finishCall();
				}
			}
			@Override
			public java.lang.String toString() {
				return "Function_prototype_toString";
			}
		});
		prototype.setStorage("apply", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				throw new RuntimeException();
			}
			@Override
			public java.lang.String name() {
				return "Function_prototype_apply";
			}
		}, false);
		prototype.setStorage("call", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
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
			}
			@Override
			public java.lang.String name() {
				return "Function_prototype_call";
			}
		}, false);
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		final java.lang.String source = params[1].toString();
		final java.lang.String arguments = params[0].toString();
		final Script compiled = global.compiler.eval(source, "Function", true);
		final java.lang.String[] args = arguments.split("\\s*,\\s*");
		
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
				scope.enter();
				JSHelper.renameCall(name(), "Function", 0, 0);
				try {
					return compiled.exec(global, scope);
				} finally {
					JSHelper.finishCall();
					scope.exit();
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
