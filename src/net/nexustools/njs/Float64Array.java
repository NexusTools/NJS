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
			super(global, Float64Array.prototype(), Float64Array, storage);
			this.Number = global.Number;
		}

		@Override
		public BaseObject get0(int index) {
			return Number.wrap(arrayStorage[index]);
		}

		@Override
		protected void put0(int index, BaseObject obj) throws ArrayIndexOutOfBoundsException {
			try {
				arrayStorage[index] = Number.from(obj).number;
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

	@Override
	public BaseObject call(BaseObject _this, BaseObject... params) {
		if(!_this.instanceOf(this))
			return construct(params);
		return _this;
	}
	
	
}
