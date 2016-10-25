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
public abstract class AbstractArray<O> extends GenericObject implements ArrayStorage<O> {
	
	protected O arrayStorage;
	protected int actualLength;
	protected AbstractArray(final Global global, BaseObject __proto__, BaseFunction constructor, O storage) {
		super(__proto__, constructor);
		this.arrayStorage = storage;
		this.actualLength = storageSize();
		final Number Number = global.Number;
		defineProperty("length", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return Number.wrap(actualLength);
			}
		}, new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				Number.Instance number = Number.from(params[0]);
				if(!(number instanceof Number.Instance))
					throw new Error.JavaException("RangeError", "Invalid array length");
				double newLength = ((Number.Instance)number).number;
				if(newLength < 0 || newLength > Integer.MAX_VALUE || (int)newLength != newLength)
					throw new Error.JavaException("RangeError", "Invalid array length");
				if(newLength > storageSize()) {
					O newArray = createStorage(nextPowerOf2((int)newLength));
					copy(arrayStorage, newArray, actualLength);
					releaseStorage(arrayStorage);
					actualLength = (int)newLength;
					arrayStorage = newArray;
				}
				return Undefined.INSTANCE;
			}
		});
		setArrayOverride(new ArrayOverride() {
			@Override
			public BaseObject get(int i, Or<BaseObject> or) {
				try {
					return get0(i);
				} catch(ArrayIndexOutOfBoundsException ex) {
					return or.or(java.lang.String.valueOf(i));
				}
			}
			@Override
			public void set(int i, BaseObject val) {
				try {
					put0(i, val);
					actualLength = java.lang.Math.max(actualLength, i+1);
				} catch(ArrayIndexOutOfBoundsException ex) {
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
			public boolean delete(int i, Or<java.lang.Boolean> or) {
				if(i > actualLength)
					return true;
				
				put0(i, null);
				return or.or(java.lang.String.valueOf(i));
			}
			@Override
			public int length() {
				return actualLength;
			}

			@Override
			public boolean has(int i) {
				try {
					return get0(i) != null;
				} catch(ArrayIndexOutOfBoundsException ex) {
					return false;
				}
			}
		});
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
