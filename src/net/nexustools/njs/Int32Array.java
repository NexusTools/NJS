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
public class Int32Array extends Uint32Array {
	public static class Instance extends Uint32Array.Instance {
		public Instance(Global global) {
			this(global, (Int32Array)global.get("Int32Array"), 0);
		}
		public Instance(Global global, int len) {
			this(global, (Int32Array)global.get("Int32Array"), ArrayStorage.BufferStorage.INT.createOrReuse(len));
		}
		protected Instance(Global global, int[] storage) {
			super(global, (Int32Array)global.get("Int32Array"), storage);
		}
		public Instance(Global global, Int32Array Int32Array) {
			this(global, Int32Array, 0);
		}
		public Instance(Global global, Int32Array Int32Array, int len) {
			this(global, Int32Array, ArrayStorage.BufferStorage.INT.createOrReuse(len));
		}
		protected Instance(Global global, Int32Array Int32Array, int[] storage) {
			super(global, Int32Array, storage);
		}
		@Override
		public BaseObject get0(int index) {
			return Number.wrap(arrayStorage[index]);
		}
	}

	public Int32Array(final Global global) {
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
