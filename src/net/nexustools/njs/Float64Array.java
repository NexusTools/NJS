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

import java.util.Iterator;

/**
 *
 * @author kate
 */
public class Float64Array extends AbstractFunction {
	public static class Instance extends AbstractArray<double[]> {
		private final Number Number;
		public Instance(Global global) {
			this(global, (Float64Array)global.get("Float64Array"), 0);
		}
		public Instance(Global global, int len) {
			this(global, (Float64Array)global.get("Float64Array"), ArrayStorage.BufferStorage.DOUBLE.createOrReuse(len));
		}
		protected Instance(Global global, double[] storage) {
			this(global, (Float64Array)global.get("Float64Array"), storage);
		}
		public Instance(Global global, Float64Array Float64Array) {
			this(global, Float64Array, 0);
		}
		public Instance(Global global, Float64Array Float64Array, int len) {
			this(global, Float64Array, ArrayStorage.BufferStorage.DOUBLE.createOrReuse(len));
		}
		protected Instance(Global global, Float64Array Float64Array, double[] storage) {
			super(global, Float64Array, storage);
			this.Number = global.Number;
		}

		@Override
		public BaseObject get0(int index) {
			return Number.wrap(arrayStorage[index]);
		}

		@Override
		protected void put0(int index, BaseObject obj) throws ArrayIndexOutOfBoundsException {
			try {
				arrayStorage[index] = obj.toDouble();
			} catch(NullPointerException ex) {
				arrayStorage[index] = 0;
			}
		}

		@Override
		protected void copy(double[] source, double[] dest, int len) {
			System.arraycopy(source, 0, dest, 0, len);
		}

		@Override
		protected int storageSize() {
			return arrayStorage.length;
		}

		@Override
		protected double[] createStorage(int len) {
			return ArrayStorage.BufferStorage.DOUBLE.createOrReuse(len);
		}

		@Override
		protected void releaseStorage(double[] storage) {
			ArrayStorage.BufferStorage.DOUBLE.release(storage.length, storage);
		}
	}

	private final Global global;
	public Float64Array(final Global global) {
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
