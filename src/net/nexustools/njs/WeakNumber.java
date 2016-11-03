/* 
 * Copyright (C) 2016 NexusTools.
 *
 * This library is free software: you can redistribute it and/or modify  
 * it under the terms of the GNU Lesser General Public License as   
 * published by the Free Software Foundation, version 3.0.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.nexustools.njs;

import java.lang.ref.WeakReference;

/**
 *
 * @author Katelyn Slater <ktaeyln@gmail.com>
 */
public class WeakNumber extends Number {
	
	private final WeakReference<Instance>[][] INSTANCES = new WeakReference[0x10000][0];

	@Override
	public Instance wrap(double number) {
		synchronized(INSTANCES) {
			int pos = 0, max;
			int index = (int)(Double.doubleToRawLongBits(number) & 0xFFFF);
			WeakReference[] array = INSTANCES[index];
			max = array.length;
			for(; pos < max; pos++) {
				try {
					WeakReference<Instance> ref = array[pos];
					Instance inst = ref.get();
					if(inst.value == number)
						return inst;
				} catch(NullPointerException ex) {
					break;
				}
			}
			Instance um = new Instance(this, iterator, String, number, true);
			um.seal();
			try {
				array[pos] = new WeakReference(um);
			} catch(IndexOutOfBoundsException ex) {
				WeakReference<Instance>[] newArray = new WeakReference[Utilities.nextPowerOf2(array.length+1)];
				if(pos > 0)
					System.arraycopy(array, 0, newArray, 0, pos);
				newArray[pos] = new WeakReference(um);
				INSTANCES[index] = newArray;
			}
			return um;
		}
	}
	
}
