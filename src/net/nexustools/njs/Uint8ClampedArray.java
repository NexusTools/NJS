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
public class Uint8ClampedArray extends Uint8Array {
	public static class Instance extends Uint8Array.Instance {
		public Instance(Global global) {
			this(global, (Uint8ClampedArray)global.get("Uint8ClampedArray"), 0);
		}
		public Instance(Global global, int len) {
			this(global, (Uint8ClampedArray)global.get("Uint8ClampedArray"), ArrayStorage.BufferStorage.BYTE.createOrReuse(len));
		}
		protected Instance(Global global, byte[] storage) {
			this(global, (Uint8ClampedArray)global.get("Uint8ClampedArray"), storage);
		}
		public Instance(Global global, Uint8ClampedArray Uint8Array) {
			this(global, Uint8Array, 0);
		}
		public Instance(Global global, Uint8ClampedArray Uint8Array, int len) {
			this(global, Uint8Array, ArrayStorage.BufferStorage.BYTE.createOrReuse(len));
		}
		protected Instance(Global global, Uint8ClampedArray Uint8Array, byte[] storage) {
			super(global, Uint8Array, storage);
		}
		@Override
		protected void put0(int index, BaseObject obj) throws ArrayIndexOutOfBoundsException {
			try {
				Number.Instance num = Number.from(obj);
				if(!(num instanceof Number.Instance))
					throw new NullPointerException();
				arrayStorage[index] = ((Number.Instance)num).toClampedByte();
			} catch(NullPointerException ex) {
				arrayStorage[index] = 0;
			}
		}
	}

	public Uint8ClampedArray(Global global) {
		super(global);
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		return new Instance(global, this, params.length > 0 ? global.toArrayRange(params[0]) : 0);
	}

}
