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

	public Int8Array(Global global) {
		super(global);
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		return new Instance(global, this, params.length > 0 ? global.toArrayRange(params[0]) : 0);
	}
	
}
