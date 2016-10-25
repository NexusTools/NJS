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

	public Int32Array(Global global) {
		super(global);
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		return new Instance(global, this, params.length > 0 ? global.toArrayRange(params[0]) : 0);
	}
	
}
