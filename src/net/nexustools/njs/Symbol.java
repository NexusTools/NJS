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
	public static class Instance extends GenericObject {
		public final java.lang.String name;
		public Instance(Global global, java.lang.String name) {
			super(global);
			this.name = name;
		}
		@Override
		public java.lang.String toString() {
			return "Symbol(" + name + ')';
		}
	}

	private final Global global;
	public final Instance iterator, unscopables;
	public Symbol(Global global) {
		super(global);
		this.global = global;
		setStorage("iterator", iterator = new Instance(global, "Symbol.iterator"), false);
		setStorage("unscopables", unscopables = new Instance(global, "Symbol.unscopables"), false);
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		return new Instance(global, params[0].toString());
	}

	@Override
	public BaseObject call(BaseObject _this, BaseObject... params) {
		if(!_this.instanceOf(this))
			return construct(params);
		return _this;
	}
}
