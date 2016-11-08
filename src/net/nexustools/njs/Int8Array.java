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
 * @author Katelyn Slater <kate@nexustools.com>
 */
public class Int8Array extends Uint8Array {
	public static class Instance extends Uint8Array.Instance {
		public Instance(Global global) {
			this(global, (Int8Array)global.get("Int8Array"), 0);
		}
		public Instance(Global global, int len) {
			this(global, (Int8Array)global.get("Int8Array"), ArrayStorage.BufferStorage.BYTE.createOrReuse(len));
		}
		protected Instance(Global global, byte[] storage) {
			this(global, (Int8Array)global.get("Int8Array"), storage);
		}
		public Instance(Global global, Int8Array Int8Array) {
			this(global, Int8Array, 0);
		}
		public Instance(Global global, Int8Array Int8Array, int len) {
			this(global, Int8Array, ArrayStorage.BufferStorage.BYTE.createOrReuse(len));
		}
		protected Instance(Global global, Int8Array Int8Array, byte[] storage) {
			super(global, Int8Array, storage);
		}
		@Override
		public BaseObject get0(int index) {
			return Number.wrap(arrayStorage[index]);
		}
	}

	public Int8Array(final Global global) {
		super(global);
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
	
}
