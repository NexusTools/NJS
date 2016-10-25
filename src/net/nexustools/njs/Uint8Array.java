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
			super(global, Uint8Array.prototype(), Uint8Array, storage);
			this.Number = global.Number;
		}

		@Override
		public BaseObject get0(int index) {
			return Number.wrap(arrayStorage[index] & 0xFF);
		}

		@Override
		protected void put0(int index, BaseObject obj) throws ArrayIndexOutOfBoundsException {
			try {
				Number.Instance num = Number.from(obj);
				if(!(num instanceof Number.Instance))
					throw new NullPointerException();
				arrayStorage[index] = ((Number.Instance)num).toByte();
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
	public Uint8Array(Global global) {
		super(global);
		this.global = global;
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		return new Instance(global, this, params.length > 0 ? global.toArrayRange(params[0]) : 0);
	}

	@Override
	public final BaseObject call(BaseObject _this, BaseObject... params) {
		if(!_this.instanceOf(this))
			return construct(params);
		return _this;
	}
	
	
}
