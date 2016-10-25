/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

import java.util.Set;

/**
 *
 * @author kate
 */
public final class Undefined implements BaseObject {
	public static final Undefined INSTANCE = new Undefined();
	private Undefined() {}

	@Override
	public void set(java.lang.String key, BaseObject val) {
		throw new Error.JavaException("TypeError", "Cannot set property \"" + key + "\" of undefined");
	}

	@Override
	public void set(int i, BaseObject val) {
		throw new Error.JavaException("TypeError", "Cannot set property \"" + i + "\" of undefined");
	}

	@Override
	public BaseObject get(java.lang.String key) {
		throw new Error.JavaException("TypeError", "Cannot read property \"" + key + "\" from undefined");
	}

	@Override
	public BaseObject get(int i) {
		throw new Error.JavaException("TypeError", "Cannot read property \"" + i + "\" from undefined");
	}

	@Override
	public void defineGetter(java.lang.String key, BaseFunction impl) {
		throw new UnsupportedOperationException("Not supported on undefined.");
	}

	@Override
	public void defineSetter(java.lang.String key, BaseFunction impl) {
		throw new UnsupportedOperationException("Not supported on undefined.");
	}

	@Override
	public void seal() {
		throw new UnsupportedOperationException("Not supported on undefined.");
	}

	@Override
	public Set<java.lang.String> keys() {
		throw new UnsupportedOperationException("Not supported on undefined.");
	}

	@Override
	public java.lang.Object getMetaObject() {
		throw new UnsupportedOperationException("JSUndefined does not support meta objects");
	}

	@Override
	public void setMetaObject(java.lang.Object meta) {
		throw new UnsupportedOperationException("JSUndefined does not support meta objects");
	}

	@Override
	public BaseObject __proto__() {
		throw new Error.JavaException("TypeError", "Cannot read property \"__proto__\" from undefined");
	}

	@Override
	public BaseFunction constructor() {
		throw new Error.JavaException("TypeError", "Cannot read property \"constructor\" from undefined");
	}

	@Override
	public boolean instanceOf(BaseFunction constructor) {
		return false;
	}

	@Override
	public Set<java.lang.String> ownPropertyNames() {
		throw new UnsupportedOperationException("Not supported on undefined.");
	}

	@Override
	public boolean hasProperty(java.lang.String name) {
		throw new Error.JavaException("TypeError", "Cannot read property \"" + name + "\" from undefined");
	}

	@Override
	public boolean delete(java.lang.String key) {
		throw new Error.JavaException("TypeError", "Cannot delete property \"" + key + "\" from undefined");
	}

	@Override
	public boolean delete(int index) {
		throw new Error.JavaException("TypeError", "Cannot delete property \"" + index + "\" from undefined");
	}

	@Override
	public boolean isSealed() {
		return true;
	}

	@Override
	public Set<Symbol.Instance> ownSymbols() {
		throw new UnsupportedOperationException("Not supported on undefined.");
	}

	@Override
	public void set(Symbol.Instance symbol, BaseObject val) {
		throw new Error.JavaException("TypeError", "Cannot set symbol " + symbol + " on undefined");
	}

	@Override
	public BaseObject get(Symbol.Instance symbol) {
		throw new Error.JavaException("TypeError", "Cannot read symbol " + symbol + " from undefined");
	}

	@Override
	public void delete(Symbol.Instance symbol) {
		throw new Error.JavaException("TypeError", "Cannot delete symbol " + symbol + " from undefined");
	}

	@Override
	public java.lang.String toString() {
		return "undefined";
	}

	@Override
	public BaseObject get(java.lang.String key, Or<BaseObject> or) {
		throw new Error.JavaException("TypeError", "Cannot read property \"" + key + "\" from undefined");
	}

	@Override
	public BaseObject get(int index, Or<BaseObject> or) {
		throw new Error.JavaException("TypeError", "Cannot read property \"" + index + "\" from undefined");
	}

	@Override
	public boolean delete(java.lang.String key, Or<java.lang.Boolean> or) {
		throw new Error.JavaException("TypeError", "Cannot delete property \"" + key + "\" from undefined");
	}

	@Override
	public boolean delete(int index, Or<java.lang.Boolean> or) {
		throw new Error.JavaException("TypeError", "Cannot delete property \"" + index + "\" from undefined");
	}

	@Override
	public void set(int i, BaseObject val, Or<Void> or) {
		throw new Error.JavaException("TypeError", "Cannot set property \"" + i + "\" on undefined");
	}

	@Override
	public void set(java.lang.String key, BaseObject val, Or<Void> or) {
		throw new Error.JavaException("TypeError", "Cannot set property \"" + key + "\" on undefined");
	}
}
