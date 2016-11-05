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
 */
package net.nexustools.njs;

import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author Katelyn Slater <ktaeyln@gmail.com>
 */
public final class Null implements BaseObject {
	public static final Null INSTANCE = new Null();
	private Null() {}

	@Override
	public void set(java.lang.String key, BaseObject val) {
		throw new Error.JavaException("TypeError", "Cannot set property \"" + key + "\" of null");
	}

	@Override
	public void set(int i, BaseObject val) {
		throw new Error.JavaException("TypeError", "Cannot set property \"" + i + "\" of null");
	}

	@Override
	public BaseObject get(java.lang.String key) {
		throw new Error.JavaException("TypeError", "Cannot read property \"" + key + "\" from null");
	}

	@Override
	public BaseObject get(int i) {
		throw new Error.JavaException("TypeError", "Cannot read property \"" + i + "\" from null");
	}

	@Override
	public void defineGetter(java.lang.String key, BaseFunction impl) {
		throw new UnsupportedOperationException("Not supported on null.");
	}

	@Override
	public void defineSetter(java.lang.String key, BaseFunction impl) {
		throw new UnsupportedOperationException("Not supported on null.");
	}

	@Override
	public void seal() {
		throw new UnsupportedOperationException("Not supported on null.");
	}

	@Override
	public Set<java.lang.String> keys() {
		throw new Error.JavaException("TypeError", "Cannot convert undefined or null to object");
	}

	@Override
	public java.lang.Object getMetaObject() {
		throw new UnsupportedOperationException("JSNull does not support meta objects");
	}

	@Override
	public void setMetaObject(java.lang.Object meta) {
		throw new UnsupportedOperationException("JSNull does not support meta objects");
	}

	@Override
	public BaseObject prototypeOf() {
		throw new Error.JavaException("TypeError", "Cannot convert undefined or null to object");
	}

	@Override
	public void setPrototypeOf(BaseObject prototype) {
		throw new Error.JavaException("TypeError", "Cannot convert undefined or null to object");
	}

	@Override
	public boolean instanceOf(BaseFunction constructor) {
		return false;
	}

	@Override
	public Set<java.lang.String> ownPropertyNames() {
		throw new Error.JavaException("TypeError", "Cannot convert undefined or null to object");
	}

	@Override
	public boolean hasOwnProperty(java.lang.String name) {
		throw new Error.JavaException("TypeError", "Cannot read property \"" + name + "\" from null");
	}

	@Override
	public boolean isSealed() {
		return true;
	}

	@Override
	public boolean delete(java.lang.String key) {
		throw new Error.JavaException("TypeError", "Cannot delete property \"" + key + "\" from null");
	}

	@Override
	public boolean delete(int index) {
		throw new Error.JavaException("TypeError", "Cannot delete property \"" + index + "\" from null");
	}

	@Override
	public Set<Symbol.Instance> ownSymbols() {
		throw new UnsupportedOperationException("Not supported on null.");
	}

	@Override
	public void set(Symbol.Instance symbol, BaseObject val) {
		throw new Error.JavaException("TypeError", "Cannot set symbol " + symbol + " on null");
	}

	@Override
	public BaseObject get(Symbol.Instance symbol) {
		throw new Error.JavaException("TypeError", "Cannot read symbol " + symbol + " from null");
	}

	@Override
	public void delete(Symbol.Instance symbol) {
		throw new Error.JavaException("TypeError", "Cannot delete symbol " + symbol + " from null");
	}

	@Override
	public java.lang.String toString() {
		return "null";
	}

	@Override
	public BaseObject get(java.lang.String key, Or<BaseObject> or) {
		throw new Error.JavaException("TypeError", "Cannot read property \"" + key + "\" from null");
	}

	@Override
	public BaseObject get(int index, Or<BaseObject> or) {
		throw new Error.JavaException("TypeError", "Cannot read property \"" + index + "\" from null");
	}

	@Override
	public boolean delete(java.lang.String key, Or<java.lang.Boolean> or) {
		throw new Error.JavaException("TypeError", "Cannot delete property \"" + key + "\" from null");
	}

	@Override
	public boolean delete(int index, Or<java.lang.Boolean> or) {
		throw new Error.JavaException("TypeError", "Cannot delete property \"" + index + "\" from null");
	}

	@Override
	public void set(int i, BaseObject val, Or<Void> or) {
		throw new Error.JavaException("TypeError", "Cannot set property \"" + i + "\" on null");
	}

	@Override
	public void set(java.lang.String key, BaseObject val, Or<Void> or) {
		throw new Error.JavaException("TypeError", "Cannot set property \"" + key + "\" on null");
	}

	@Override
	public void set(int i, BaseObject val, BaseObject _this) {
		throw new Error.JavaException("TypeError", "Cannot set property \"" + i + "\" on null");
	}

	@Override
	public void set(int i, BaseObject val, BaseObject _this, Or<Void> or) {
		throw new Error.JavaException("TypeError", "Cannot set property \"" + i + "\" on null");
	}

	@Override
	public void set(java.lang.String key, BaseObject val, BaseObject _this) {
		throw new Error.JavaException("TypeError", "Cannot set property \"" + key + "\" on null");
	}

	@Override
	public void set(java.lang.String key, BaseObject val, BaseObject _this, Or<Void> or) {
		throw new Error.JavaException("TypeError", "Cannot set property \"" + key + "\" on null");
	}

	@Override
	public BaseObject get(int index, BaseObject _this) {
		throw new Error.JavaException("TypeError", "Cannot get property \"" + index + "\" from null");
	}

	@Override
	public BaseObject get(int index, BaseObject _this, Or<BaseObject> or) {
		throw new Error.JavaException("TypeError", "Cannot get property \"" + index + "\" from null");
	}

	@Override
	public BaseObject get(java.lang.String key, BaseObject _this) {
		throw new Error.JavaException("TypeError", "Cannot get property \"" + key + "\" from null");
	}

	@Override
	public BaseObject get(java.lang.String key, BaseObject _this, Or<BaseObject> or) {
		throw new Error.JavaException("TypeError", "Cannot get property \"" + key + "\" from null");
	}

	@Override
	public byte toByte() {
		return 0;
	}

	@Override
	public short toShort() {
		return 0;
	}

	@Override
	public int toInt() {
		return 0;
	}

	@Override
	public long toLong() {
		return 0;
	}

	@Override
	public Number.Instance toNumber() {
		return null;
	}

	@Override
	public double toDouble() {
		return 0;
	}

	@Override
	public float toFloat() {
		return 0;
	}

	@Override
	public boolean equals(java.lang.Object obj) {
		if(this == obj)
			return true;
		
		return obj == Undefined.INSTANCE;
	}

	@Override
	public java.lang.String typeOf() {
		return "object";
	}

	@Override
	public Iterator<BaseObject> iterator() {
		return new Iterator<BaseObject>() {
			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public BaseObject next() {
				throw new UnsupportedOperationException("Not supported");
			}
		};
	}

	@Override
	public boolean setProperty(java.lang.String key, Property property) {
		return false;
	}

	@Override
	public Property getProperty(java.lang.String key) {
		throw new Error.JavaException("TypeError", "Cannot read property \"" + key + "\" from null");
	}

	@Override
	public Iterator<java.lang.String> deepPropertyNameIterator() {
		return new Iterator<java.lang.String>() {
			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public java.lang.String next() {
				throw new UnsupportedOperationException("Not supported");
			}
		};
	}

	@Override
	public boolean toBool() {
		return false;
	}

	@Override
	public String.Instance _toString() {
		throw new UnsupportedOperationException("Not supported");
	}
	
	
}
