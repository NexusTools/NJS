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

/**
 *
 * @author Katelyn Slater <ktaeyln@gmail.com>
 */
public class GenericArray extends AbstractArray<BaseObject[]> {
	private static final BaseObject[] EMPTY = new BaseObject[0];
	public GenericArray(Global global) {
		this(global, global.Array, EMPTY);
	}
	public GenericArray(Global global, java.lang.Object[] array) {
		this(global, global.Array, Utilities.convertArray(global, array));
	}
	public GenericArray(Global global, int len) {
		this(global, global.Array, new BaseObject[len]);
	}
	public GenericArray(Global global, Array Array) {
		this(global, Array, EMPTY);
	}
	public GenericArray(Global global, Array Array, int len) {
		super(global, Array, true, new BaseObject[len]);
	}
	public GenericArray(Global global, Array Array, BaseObject[] storage) {
		super(global, Array, true, storage);
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
