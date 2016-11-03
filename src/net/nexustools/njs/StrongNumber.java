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

/**
 *
 * @author Katelyn Slater <ktaeyln@gmail.com>
 */
public class StrongNumber extends Number {
	
	private final Instance[][] INSTANCES = new Instance[0x10000][0];

	@Override
	public Instance wrap(double number) {
		synchronized(INSTANCES) {
			int pos = 0, max;
			int index = (int)(Double.doubleToRawLongBits(number) & 0xFFFF);
			Instance[] array = INSTANCES[index];
			max = array.length;
			for(; pos < max; pos++) {
				try {
					Instance inst = array[pos];
					if(inst.value == number)
						return inst;
				} catch(NullPointerException ex) {
					break;
				}
			}
			Instance um = new Instance(this, iterator, String, number);
			um.seal();
			try {
				array[pos] = um;
			} catch(IndexOutOfBoundsException ex) {
				Instance[] newArray = new Instance[Utilities.nextPowerOf2(array.length+1)];
				if(pos > 0)
					System.arraycopy(array, 0, newArray, 0, pos);
				newArray[pos] = um;
				INSTANCES[index] = newArray;
			}
			return um;
		}
	}
	
}
