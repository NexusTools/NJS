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
public class GenericArray extends AbstractArray<BaseObject[]> {
	public GenericArray(Global global) {
		this(global, global.Array, 0);
	}
	public GenericArray(Global global, java.lang.Object[] array) {
		this(global, global.Array, JSHelper.convertArray(global, array));
	}
	public GenericArray(Global global, int len) {
		this(global, global.Array, new BaseObject[len]);
	}
	public GenericArray(Global global, Array Array) {
		this(global, Array, 0);
	}
	public GenericArray(Global global, Array Array, int len) {
		super(global, Array, new BaseObject[len]);
	}
	public GenericArray(Global global, Array Array, BaseObject[] storage) {
		super(global, Array, storage);
	}

	@Override
	protected boolean autoResize() {
		return true;
	}

	@Override
	protected void put0(int index, BaseObject obj) throws ArrayIndexOutOfBoundsException {
		arrayStorage[index] = obj;
	}

	@Override
	protected void copy(BaseObject[] source, BaseObject[] dest, int len) {
		System.arraycopy(source, 0, dest, 0, len);
	}

	@Override
	protected int storageSize() {
		return arrayStorage.length;
	}

	@Override
	protected BaseObject[] createStorage(int len) {
		return new BaseObject[len];
	}

	@Override
	protected void releaseStorage(BaseObject[] storage) {}

	@Override
	protected BaseObject get0(int index) throws ArrayIndexOutOfBoundsException {
		return arrayStorage[index];
	}
	
}
