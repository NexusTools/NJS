/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author kate
 */
public interface ArrayStorage<O> {
	public static abstract class BufferStorage<O> {
		public static final BufferStorage<byte[]> BYTE = new BufferStorage<byte[]>() {
			@Override
			public byte[] create(int size) {
				return new byte[size];
			}
		};
		public static final BufferStorage<short[]> SHORT = new BufferStorage<short[]>() {
			@Override
			public short[] create(int size) {
				return new short[size];
			}
		};
		public static final BufferStorage<int[]> INT = new BufferStorage<int[]>() {
			@Override
			public int[] create(int size) {
				return new int[size];
			}
		};
		public static final BufferStorage<double[]> DOUBLE = new BufferStorage<double[]>() {
			@Override
			public double[] create(int size) {
				return new double[size];
			}
		};
		private final Map<Integer, List<O>> storage = new HashMap();
		public O createOrReuse(int size) {
			synchronized(storage) {
				Iterator<Map.Entry<Integer, List<O>>> it = storage.entrySet().iterator();
				while(it.hasNext()) {
					Map.Entry<Integer, List<O>> entry = it.next();
					if(entry.getKey() == size) {
						List<O> l = entry.getValue();
						try {
							return l.remove(l.size()-1);
						} finally {
							if(l.isEmpty())
								it.remove();
						}
					}
				}
			}
			return create(size);
		}
		public void release(int size, O buffer) {
			synchronized(storage) {
				List<O> list = storage.get(size);
				if(list == null)
					storage.put(size, list = new ArrayList());
				list.add(buffer);
			}
		}
		public abstract O create(int size);
	}
	public O getArrayStorage();
}
