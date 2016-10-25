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
public class Int16Array extends Uint16Array {
	public static class Instance extends Uint16Array.Instance {
		public Instance(Global global) {
			this(global, (Int16Array)global.get("Int16Array"), 0);
		}
		public Instance(Global global, int len) {
			this(global, (Int16Array)global.get("Int16Array"), ArrayStorage.BufferStorage.SHORT.createOrReuse(len));
		}
		public Instance(Global global, short[] storage) {
			this(global, (Int16Array)global.get("Int16Array"), storage);
		}
		public Instance(Global global, Int16Array Int16Array) {
			this(global, Int16Array, 0);
		}
		public Instance(Global global, Int16Array Int16Array, int len) {
			this(global, Int16Array, ArrayStorage.BufferStorage.SHORT.createOrReuse(len));
		}
		public Instance(Global global, Int16Array Int16Array, short[] storage) {
			super(global, Int16Array, storage);
		}
		

		@Override
		public BaseObject get0(int index) {
			return Number.wrap(arrayStorage[index]);
		}
	}

	public Int16Array(final Global global) {
		super(global);
		GenericObject prototype = prototype();
		prototype.setHidden("toString", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				StringBuilder builder = new StringBuilder();
				for(int i=0; i<global.toNumber(_this.get("length")).toInt(); i++) {
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
		return new Instance(global, this, params.length > 0 ? global.toArrayRange(params[0]) : 0);
	}
	
	
}
