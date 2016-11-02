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
