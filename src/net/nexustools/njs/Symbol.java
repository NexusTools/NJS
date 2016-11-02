/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

/**
 *
 * @author kate
 */
public class Symbol extends AbstractFunction {
	public static class Instance extends UniqueObject {
		public final java.lang.String name;
		public Instance(java.lang.String name) {
			super();
			this.name = name;
		}
		public Instance(Symbol Symbol, java.lang.String name, Global global) {
			super(Symbol, global);
			this.name = name;
		}
	}

	private final Global global;
	public Instance unscopables;
	public Symbol(final Global global) {
		super();
		this.global = global;
		if(true)
			return;
		
	}

	protected void initPrototypeFunctions(final Global global) {
		GenericObject prototype = (GenericObject)prototype();
		prototype.setHidden("toString", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return global.wrap("Symbol(" + ((Instance)_this).name + ')');
			}
			@Override
			public java.lang.String name() {
				return "Symbol_prototype_toString";
			}
		});
		prototype.setHidden("valueOf", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				JSHelper.ReplacementStackTraceElement el = JSHelper.renameMethodCall("Symbol.prototype.valueOf");
				try {
					throw new Error.JavaException("TypeError", "Cannot convert a Symbol value to a number");
				} finally {
					el.finishCall();
				}
			}
			@Override
			public java.lang.String name() {
				return "Symbol_prototype_toString";
			}
		});
	}
	
	public void initConstants() {
		setHidden("iterator", iterator = new Instance("Symbol.iterator"));
		setHidden("unscopables", unscopables = new Instance("Symbol.unscopables"));
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		return new Instance(this, params[0].toString(), global);
	}

	@Override
	public BaseObject call(BaseObject _this, BaseObject... params) {
		if(!_this.instanceOf(this))
			return construct(params);
		return _this;
	}
}
