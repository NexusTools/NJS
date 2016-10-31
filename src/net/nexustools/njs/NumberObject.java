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
public abstract class NumberObject implements BaseObject {
	
	Number Number;
	protected Number.Instance number;
	public NumberObject(Number Number) {
		this.Number = Number;
	}
	public NumberObject(Number.Instance number) {
		assert(number != null);
		this.number = number;
		Number = null;
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
		if(number == null) {
			double num;
			try {
				num = Double.valueOf(toString());
			} catch(NumberFormatException ex) {
				num = Double.NaN;
			}
			try {
				number = Number.wrap(num);
			} catch(NullPointerException ex) {
				System.err.println(getClass());
				throw ex;
			}
		}
		return number;
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
