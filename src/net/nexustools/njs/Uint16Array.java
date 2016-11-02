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
public class Uint16Array extends AbstractFunction {
	public static class Instance extends AbstractArray<short[]> {
		protected final Number Number;
		public Instance(Global global) {
			this(global, (Uint16Array)global.get("Uint16Array"), 0);
		}
		public Instance(Global global, int len) {
			this(global, (Uint16Array)global.get("Uint16Array"), ArrayStorage.BufferStorage.SHORT.createOrReuse(len));
		}
		protected Instance(Global global, short[] storage) {
			this(global, (Uint16Array)global.get("Uint16Array"), storage);
		}
		public Instance(Global global, Uint16Array Uint16Array) {
			this(global, Uint16Array, 0);
		}
		public Instance(Global global, Uint16Array Uint16Array, int len) {
			this(global, Uint16Array, ArrayStorage.BufferStorage.SHORT.createOrReuse(len));
		}
		protected Instance(Global global, Uint16Array Uint16Array, short[] storage) {
			super(global, Uint16Array, storage);
			this.Number = global.Number;
		}

		@Override
		public BaseObject get0(int index) {
			return Number.wrap(arrayStorage[index] & 0xFFFF);
		}

		@Override
		protected void put0(int index, BaseObject obj) throws ArrayIndexOutOfBoundsException {
			try {
				arrayStorage[index] = obj.toShort();
			} catch(NullPointerException ex) {
				arrayStorage[index] = 0;
			}
		}

		@Override
		protected void copy(short[] source, short[] dest, int len) {
			System.arraycopy(source, 0, dest, 0, len);
		}

		@Override
		protected int storageSize() {
			return arrayStorage.length;
		}

		@Override
		protected short[] createStorage(int len) {
			return ArrayStorage.BufferStorage.SHORT.createOrReuse(len);
		}

		@Override
		protected void releaseStorage(short[] storage) {
			ArrayStorage.BufferStorage.SHORT.release(storage.length, storage);
		}
	}

	protected final Global global;
	public Uint16Array(final Global global) {
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
