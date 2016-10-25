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
public interface Scopeable {
	public static interface Or<R> {
		public R or(java.lang.String key);
	}
	public static final Or<BaseObject> OR_NULL = new Or<BaseObject>() {
		@Override
		public BaseObject or(java.lang.String key) {
			return null;
		}
	};
	public static final Or<BaseObject> OR_UNDEFINED = new Or<BaseObject>() {
		@Override
		public BaseObject or(java.lang.String key) {
			return Undefined.INSTANCE;
		}
	};
	public static final Or<java.lang.Boolean> OR_TRUE = new Or<java.lang.Boolean>() {
		@Override
		public java.lang.Boolean or(java.lang.String key) {
			return true;
		}
	};
	public static final Or<java.lang.Boolean> OR_FALSE = new Or<java.lang.Boolean>() {
		@Override
		public java.lang.Boolean or(java.lang.String key) {
			return false;
		}
	};
	public static final Or<Boolean> OR_REFERENCE_ERROR_B = new Or<Boolean>() {
		@Override
		public Boolean or(java.lang.String key) {
			throw new Error.JavaException("ReferenceError", key + " is not defined");
		}
	};
	public static final Or<BaseObject> OR_REFERENCE_ERROR_BO = new Or<BaseObject>() {
		@Override
		public BaseObject or(java.lang.String key) {
			throw new Error.JavaException("ReferenceError", key + " is not defined");
		}
	};
	public static final Or<Void> OR_REFERENCE_ERROR_V = new Or<Void>() {
		@Override
		public Void or(java.lang.String key) {
			throw new Error.JavaException("ReferenceError", key + " is not defined");
		}
	};
	public static final Or<Void> OR_VOID = new Or<Void>() {
		@Override
		public Void or(java.lang.String key) {
			return null;
		}
	};
	
	public BaseObject get(java.lang.String key);
	public BaseObject get(java.lang.String key, Or<BaseObject> or);
	
	public void set(java.lang.String key, BaseObject val);
	public void set(java.lang.String key, BaseObject val, Or<Void> or);
	
	public boolean delete(java.lang.String key);
	public boolean delete(java.lang.String key, Or<java.lang.Boolean> or);
}
