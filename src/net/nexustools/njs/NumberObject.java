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
 *
 * @author Katelyn Slater <ktaeyln@gmail.com>
 */
package net.nexustools.njs;

/**
 *
 * @author kate
 */
public abstract class NumberObject implements BaseObject {
	
	Number Number;
	public NumberObject(Number Number) {
		this.Number = Number;
	}

	@Override
	public byte toByte() {
		return toNumber().toByte();
	}

	@Override
	public short toShort() {
		return toNumber().toShort();
	}

	@Override
	public int toInt() {
		return toNumber().toInt();
	}

	@Override
	public long toLong() {
		return toNumber().toLong();
	}

	@Override
	public Number.Instance toNumber() {
		double num;
		try {
			num = Double.valueOf(toString());
		} catch(NumberFormatException ex) {
			num = Double.NaN;
		}
		try {
			return Number.wrap(num);
		} catch(NullPointerException ex) {
			System.err.println(getClass());
			throw ex;
		}
	}

	@Override
	public double toDouble() {
		return toNumber().toDouble();
	}

	@Override
	public float toFloat() {
		return toNumber().toFloat();
	}
	
}
