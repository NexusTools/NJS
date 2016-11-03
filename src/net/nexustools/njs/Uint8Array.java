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
public class Uint8Array extends AbstractFunction {
	public static class Instance extends AbstractArray<byte[]> {
		protected final Number Number;
		public Instance(Global global) {
			this(global, (Uint8Array)global.get("Uint8Array"), 0);
		}
		public Instance(Global global, int len) {
			this(global, (Uint8Array)global.get("Uint8Array"), ArrayStorage.BufferStorage.BYTE.createOrReuse(len));
		}
		protected Instance(Global global, byte[] storage) {
			this(global, (Uint8Array)global.get("Uint8Array"), storage);
		}
		public Instance(Global global, Uint8Array Uint8Array) {
			this(global, Uint8Array, 0);
		}
		public Instance(Global global, Uint8Array Uint8Array, int len) {
			this(global, Uint8Array, ArrayStorage.BufferStorage.BYTE.createOrReuse(len));
		}
		protected Instance(Global global, Uint8Array Uint8Array, byte[] storage) {
			super(global, Uint8Array, false, storage);
			this.Number = global.Number;
		}

		@Override
		public BaseObject get0(int index) {
			return Number.wrap(arrayStorage[index] & 0xFF);
		}

		@Override
		protected void put0(int index, BaseObject obj) throws ArrayIndexOutOfBoundsException {
			try {
				arrayStorage[index] = obj.toByte();
			} catch(NullPointerException ex) {
				arrayStorage[index] = 0;
			}
		}

		@Override
		protected final void copy(byte[] source, byte[] dest, int len) {
			System.arraycopy(source, 0, dest, 0, len);
		}

		@Override
		protected final int storageSize() {
			return arrayStorage.length;
		}

		@Override
		protected final byte[] createStorage(int len) {
			return ArrayStorage.BufferStorage.BYTE.createOrReuse(len);
		}

		@Override
		protected final void releaseStorage(byte[] storage) {
			ArrayStorage.BufferStorage.BYTE.release(storage.length, storage);
		}
	}

	protected final Global global;
	public Uint8Array(final Global global) {
		super(global);
		this.global = global;
		GenericObject prototype = (GenericObject)prototype();
		prototype.setHidden("toString", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				StringBuilder builder = new StringBuilder();
				for(int i=0; i<_this.get("length").toInt(); i++) {
					if(i > 0)
						builder.append(',');
					BaseObject value = _this.get(i, OR_NULL);
					if(value != null)
						builder.append(value);
				}
				return global.wrap(builder.toString());
			}
		});
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		return new Instance(global, this, params.length > 0 ? params[0].toInt() : 0);
	}

	@Override
	public final BaseObject call(BaseObject _this, BaseObject... params) {
		if(!_this.instanceOf(this))
			return construct(params);
		return _this;
	}
	
	
}
