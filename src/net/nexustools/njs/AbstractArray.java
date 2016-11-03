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

/**
 *
 * @author Katelyn Slater <ktaeyln@gmail.com>
 */
public abstract class AbstractArray<O> extends GenericObject implements ArrayStorage<O> {
	
	private abstract class BaseOverride implements ArrayOverride {
		@Override
		public int length(BaseObject _this) {
			return actualLength;
		}
		@Override
		public boolean has(int i, BaseObject _this) {
			assert(i >= 0);
			try {
				return get0(i) != null;
			} catch(ArrayIndexOutOfBoundsException ex) {
				return false;
			}
		}
		@Override
		public int hashCode() {
			return arrayStorage.hashCode();
		}
	}
	private class FixedOverride extends BaseOverride {
		@Override
		public BaseObject get(int i, BaseObject _this, Or<BaseObject> or) {
			assert(i >= 0);
			try {
				BaseObject val = get0(i);
				if(val == null)
					return Null.INSTANCE;
				return val;
			} catch(ArrayIndexOutOfBoundsException ex) {
				return or.or(java.lang.String.valueOf(i));
			}
		}
		@Override
		public void set(int i, BaseObject val, BaseObject _this, Or<Void> or) {
			assert(i >= 0);
			try {
				put0(i, val);
				actualLength = java.lang.Math.max(actualLength, i+1);
			} catch(ArrayIndexOutOfBoundsException ex) {
				or.or(java.lang.String.valueOf(i));
			}
		}
		@Override
		public boolean delete(int i, BaseObject _this, Or<java.lang.Boolean> or) {
			assert(i >= 0);
			if(i > actualLength)
				return or.or(java.lang.String.valueOf(i));

			return false;
		}
	};
	private class SizeableOverride extends BaseOverride {
		@Override
		public BaseObject get(int i, BaseObject _this, Or<BaseObject> or) {
			assert(i >= 0);
			try {
				if(i >= actualLength)
					throw new ArrayIndexOutOfBoundsException();
				BaseObject val = get0(i);
				if(val == null)
					return Null.INSTANCE;
				return val;
			} catch(ArrayIndexOutOfBoundsException ex) {
				return or.or(java.lang.String.valueOf(i));
			}
		}
		@Override
		public void set(int i, BaseObject val, BaseObject _this, Or<Void> or) {
			assert(i >= 0);
			try {
				put0(i, val);
				actualLength = java.lang.Math.max(actualLength, i+1);
			} catch(ArrayIndexOutOfBoundsException ex) {
				int newLength = java.lang.Math.max(actualLength, i+1);
				O newArray = createStorage(Utilities.nextPowerOf2(newLength));
				copy(arrayStorage, newArray, actualLength);
				actualLength = newLength;
				releaseStorage(arrayStorage);
				arrayStorage = newArray;
				put0(i, val);
			}
		}
		@Override
		public boolean delete(int i, BaseObject _this, Or<java.lang.Boolean> or) {
			assert(i >= 0);
			try {
				put0(i, null);
				return true;
			} catch(ArrayIndexOutOfBoundsException ex) {
				return or.or(java.lang.String.valueOf(i));
			}
		}
	};
	
	
	protected O arrayStorage;
	protected int actualLength;
	protected AbstractArray(final Global global, BaseFunction constructor, boolean autoResize, O storage) {
		super(constructor, global);
		this.arrayStorage = storage;
		this.actualLength = storageSize();
		final Number Number0 = global.Number;
		defineProperty("length", new AbstractFunction(global) {
			Number.Instance cached;
			int cachedLength;
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(cachedLength == actualLength && cached != null)
					return cached;
				try {
					return cached = Number0.wrap(actualLength);
				} finally {
					cachedLength = actualLength;
				}
			}
		}, autoResize ? new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				double newLength = params[0].toInt();
				if(newLength < 0)
					throw new Error.JavaException("RangeError", "Invalid array length");
				if(newLength > storageSize()) {
					O newArray = createStorage(Utilities.nextPowerOf2((int)newLength));
					copy(arrayStorage, newArray, actualLength);
					releaseStorage(arrayStorage);
					arrayStorage = newArray;
				}
				actualLength = (int)newLength;
				return Undefined.INSTANCE;
			}
		} : global.NOOP);
		setArrayOverride(autoResize ? new SizeableOverride() : new FixedOverride());
	}
	
	@Override
	public O getArrayStorage() {
		return arrayStorage;
	}
	
	protected abstract BaseObject get0(int index) throws ArrayIndexOutOfBoundsException;
	protected abstract void put0(int index, BaseObject obj) throws ArrayIndexOutOfBoundsException;
	protected abstract void copy(O source, O dest, int len);
	protected abstract int storageSize();
	public int length() {
		return actualLength;
	}
	
	protected abstract O createStorage(int len);
	protected abstract void releaseStorage(O storage);
	
}
