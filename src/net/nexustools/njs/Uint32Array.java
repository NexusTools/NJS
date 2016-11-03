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
public class Uint32Array extends AbstractFunction {
	public static class Instance extends AbstractArray<int[]> {
		protected final Number Number;
		public Instance(Global global) {
			this(global, (Uint32Array)global.get("Uint32Array"), 0);
		}
		public Instance(Global global, int len) {
			this(global, (Uint32Array)global.get("Uint32Array"), ArrayStorage.BufferStorage.INT.createOrReuse(len));
		}
		protected Instance(Global global, int[] storage) {
			this(global, (Uint32Array)global.get("Uint32Array"), storage);
		}
		public Instance(Global global, Uint32Array Uint32Array) {
			this(global, Uint32Array, 0);
		}
		public Instance(Global global, Uint32Array Uint32Array, int len) {
			this(global, Uint32Array, ArrayStorage.BufferStorage.INT.createOrReuse(len));
		}
		protected Instance(Global global, Uint32Array Uint32Array, int[] storage) {
			super(global, Uint32Array, false, storage);
			this.Number = global.Number;
		}

		@Override
		public BaseObject get0(int index) {
			return Number.wrap(arrayStorage[index] & 0xFFFFFFFFL);
		}

		@Override
		protected void put0(int index, BaseObject obj) throws ArrayIndexOutOfBoundsException {
			try {
				arrayStorage[index] = obj.toInt();
			} catch(NullPointerException ex) {
				arrayStorage[index] = 0;
			}
		}

		@Override
		protected void copy(int[] source, int[] dest, int len) {
			System.arraycopy(source, 0, dest, 0, len);
		}

		@Override
		protected int storageSize() {
			return arrayStorage.length;
		}

		@Override
		protected int[] createStorage(int len) {
			return ArrayStorage.BufferStorage.INT.createOrReuse(len);
		}

		@Override
		protected void releaseStorage(int[] storage) {
			ArrayStorage.BufferStorage.INT.release(storage.length, storage);
		}
	}

	protected final Global global;
	public Uint32Array(final Global global) {
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
	public BaseObject call(BaseObject _this, BaseObject... params) {
		if(!_this.instanceOf(this))
			return construct(params);
		return _this;
	}
	
	
}
