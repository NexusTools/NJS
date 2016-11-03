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

import java.util.Arrays;

/**
 *
 * @author kate
 */
public abstract class AbstractArray<O> extends GenericObject implements ArrayStorage<O> {
	
	protected O arrayStorage;
	protected int actualLength;
	protected AbstractArray(final Global global, BaseFunction constructor, O storage) {
		super(constructor, global);
		this.arrayStorage = storage;
		this.actualLength = storageSize();
		final Number Number0 = global.Number;
		defineProperty("length", new AbstractFunction(global) {
			Number.Instance cached;
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return Number0.wrap(actualLength);
			}
		}, new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				double newLength = params[0].toInt();
				if(newLength < 0)
					throw new Error.JavaException("RangeError", "Invalid array length");
				if(newLength > storageSize()) {
					O newArray = createStorage(nextPowerOf2((int)newLength));
					copy(arrayStorage, newArray, actualLength);
					releaseStorage(arrayStorage);
					arrayStorage = newArray;
				}
				actualLength = (int)newLength;
				return Undefined.INSTANCE;
			}
		});
		setArrayOverride(new ArrayOverride() {
			@Override
			public BaseObject get(int i, BaseObject _this, Or<BaseObject> or) {
				if(i >=  actualLength)
					return or.or(java.lang.String.valueOf(i));
				try {
					return get0(i);
				} catch(ArrayIndexOutOfBoundsException ex) {
					return or.or(java.lang.String.valueOf(i));
				}
			}
			@Override
			public void set(int i, BaseObject val, BaseObject _this) {
				assert(val != AbstractArray.this);
				try {
					put0(i, val);
					actualLength = java.lang.Math.max(actualLength, i+1);
				} catch(ArrayIndexOutOfBoundsException ex) {
					if(!autoResize())
						return;
					
					int newLength = java.lang.Math.max(actualLength, i+1);
					O newArray = createStorage(nextPowerOf2(newLength));
					copy(arrayStorage, newArray, actualLength);
					actualLength = newLength;
					releaseStorage(arrayStorage);
					arrayStorage = newArray;
					put0(i, val);
				}
			}
			@Override
			public boolean delete(int i, BaseObject _this, Or<java.lang.Boolean> or) {
				if(i > actualLength)
					return true;
				
				if(!autoResize())
					return false;
				
				put0(i, null);
				if(i == actualLength)
					actualLength --;
				return or.or(java.lang.String.valueOf(i));
			}
			@Override
			public int length(BaseObject _this) {
				return actualLength;
			}

			@Override
			public boolean has(int i, BaseObject _this) {
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
			
		});
	}
	
	protected boolean autoResize() {
		return false;
	}
	
	@Override
	public O getArrayStorage() {
		return arrayStorage;
	}
	
	public static int nextPowerOf2(final int a) {
        int b = 1;
        while (b < a)
            b = b << 1;
        return b;
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
