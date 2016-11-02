/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author kate
 */
public interface BaseObject extends Scopeable, Iterable<BaseObject> {
	public static interface Property {
		public BaseObject get(BaseObject _this);
		public BaseObject getValue();
		public BaseFunction getSetter();
		public BaseFunction getGetter();
		public void set(BaseObject val, BaseObject _this);
		public boolean configurable();
		public boolean enumerable();
	}
	public static class BasicProperty implements Property {
		public BaseObject value;
		public BasicProperty() {}
		public BasicProperty(BaseObject val) {
			value = val;
		}
		@Override
		public final BaseObject get(BaseObject _this) {
			return value;
		}
		@Override
		public final BaseObject getValue() {
			return value;
		}
		@Override
		public final void set(BaseObject val, BaseObject _this) {
			value = val;
		}
		@Override
		public final BaseFunction getSetter() {
			return null;
		}
		@Override
		public final BaseFunction getGetter() {
			return null;
		}
		@Override
		public final boolean configurable() {
			return true;
		}
		@Override
		public boolean enumerable() {
			return true;
		}
	}
	public static final class HiddenProperty extends BasicProperty {
		public HiddenProperty() {}
		public HiddenProperty(BaseObject val) {
			super(val);
		}
		@Override
		public final boolean enumerable() {
			return false;
		}
	}
	public static final class ReadOnlyProperty implements Property {
		final BaseObject value;
		public ReadOnlyProperty(BaseObject val) {
			value = val;
		}
		@Override
		public final boolean enumerable() {
			return false;
		}

		@Override
		public BaseObject get(BaseObject _this) {
			return value;
		}

		@Override
		public BaseObject getValue() {
			return value;
		}

		@Override
		public BaseFunction getSetter() {
			return null;
		}

		@Override
		public BaseFunction getGetter() {
			return null;
		}

		@Override
		public void set(BaseObject val, BaseObject _this) {
		}

		@Override
		public boolean configurable() {
			return false;
		}
	}
	public static class ExtendedProperty implements Property {
		public BaseObject value;
		public BaseFunction getter;
		public BaseFunction setter;
		public boolean configurable;
		public boolean enumerable;
		public ExtendedProperty() {
			this(true);
		}
		public ExtendedProperty(boolean enumerable) {
			this.enumerable = enumerable;
		}
		@Override
		public final BaseObject get(BaseObject _this) {
			if(getter != null)
				return getter.call(_this);
			return value;
		}
		@Override
		public final BaseObject getValue() {
			return value;
		}
		@Override
		public final void set(BaseObject val, BaseObject _this) {
			if(setter != null)
				setter.call(_this, val);
			else
				value = val;
		}
		@Override
		public final BaseFunction getSetter() {
			return setter;
		}
		@Override
		public final BaseFunction getGetter() {
			return getter;
		}
		@Override
		public final boolean configurable() {
			return configurable;
		}
		@Override
		public final boolean enumerable() {
			return enumerable;
		}
	}

	public BaseObject prototypeOf();
	public void setPrototypeOf(BaseObject prototype);
	public boolean instanceOf(BaseFunction constructor);
	public java.lang.String typeOf();
	
	public Set<java.lang.String> keys();
	public Set<java.lang.String> ownPropertyNames();
	public boolean hasOwnProperty(java.lang.String name);
	public boolean hasProperty(java.lang.String name, BaseObject _this);
	public void defineGetter(java.lang.String key, BaseFunction impl);
	public void defineSetter(java.lang.String key, BaseFunction impl);
	public void set(int i, BaseObject val);
	public void set(int i, BaseObject val, Or<Void> or);
	public void set(int i, BaseObject val, BaseObject _this);
	public void set(int i, BaseObject val, BaseObject _this, Or<Void> or);
	public void set(java.lang.String key, BaseObject val, BaseObject _this);
	public void set(java.lang.String key, BaseObject val, BaseObject _this, Or<Void> or);
	public boolean setProperty(java.lang.String key, Property property);
	public BaseObject get(int index);
	public BaseObject get(int index, Or<BaseObject> or);
	public BaseObject get(int index, BaseObject _this);
	public BaseObject get(int index, BaseObject _this, Or<BaseObject> or);
	public BaseObject get(java.lang.String key, BaseObject _this);
	public BaseObject get(java.lang.String key, BaseObject _this, Or<BaseObject> or);
	public Property getProperty(java.lang.String key);
	public boolean delete(int index);
	public boolean delete(int index, Or<java.lang.Boolean> or);
	public boolean isSealed();
	public void seal();
	
	public byte toByte();
	public short toShort();
	public int toInt();
	public long toLong();
	public Number.Instance toNumber();
	public double toDouble();
	public float toFloat();
	
	public Set<Symbol.Instance> ownSymbols();
	public void set(Symbol.Instance symbol, BaseObject val);
	public BaseObject get(Symbol.Instance symbol);
	public void delete(Symbol.Instance symbol);
	
	public void setMetaObject(java.lang.Object meta);
	public java.lang.Object getMetaObject();
	
	public Iterator<java.lang.String> deepPropertyNameIterator();
}
