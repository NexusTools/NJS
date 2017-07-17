/* 
 * Copyright (C) 2017 NexusTools.
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
 * @author Katelyn Slater <kate@nexustools.com>
 */
public class Uint8ClampedArray extends Uint8Array {
	public static class Instance extends Uint8Array.Instance {
		public Instance(Global global) {
			this(global, (Uint8ClampedArray)global.get("Uint8ClampedArray"), 0);
		}
		public Instance(Global global, int len) {
			this(global, (Uint8ClampedArray)global.get("Uint8ClampedArray"), ArrayStorage.BufferStorage.BYTE.createOrReuse(len));
		}
		protected Instance(Global global, byte[] storage) {
			this(global, (Uint8ClampedArray)global.get("Uint8ClampedArray"), storage);
		}
		public Instance(Global global, Uint8ClampedArray Uint8Array) {
			this(global, Uint8Array, 0);
		}
		public Instance(Global global, Uint8ClampedArray Uint8Array, int len) {
			this(global, Uint8Array, ArrayStorage.BufferStorage.BYTE.createOrReuse(len));
		}
		protected Instance(Global global, Uint8ClampedArray Uint8Array, byte[] storage) {
			super(global, Uint8Array, storage);
		}
		@Override
		protected void put0(int index, BaseObject obj) throws ArrayIndexOutOfBoundsException {
			try {
				double number = obj.toDouble();
				if(number > 255)
					number = 255;
				if(number < 0)
					number = 0;
				arrayStorage[index] = (byte)number;
			} catch(NullPointerException ex) {
				arrayStorage[index] = 0;
			}
		}
	}

	public Uint8ClampedArray(Global global) {
		super(global);
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		return new Instance(global, this, params.length > 0 ? params[0].toInt() : 0);
	}

}
