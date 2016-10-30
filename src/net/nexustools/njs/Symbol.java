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
		public Instance(Symbol Symbol, java.lang.String name) {
			super(Symbol.prototype(), Symbol);
			this.name = name;
		}
	}

	public final Instance iterator, unscopables;
	public Symbol(final Global global) {
		super(global);
		setHidden("iterator", iterator = new Instance(this, "Symbol.iterator"));
		setHidden("unscopables", unscopables = new Instance(this, "Symbol.unscopables"));
		GenericObject prototype = prototype();
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
		// Cannot convert a Symbol value to a number
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		return new Instance(this, params[0].toString());
	}

	@Override
	public BaseObject call(BaseObject _this, BaseObject... params) {
		if(!_this.instanceOf(this))
			return construct(params);
		return _this;
	}
}
