/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author kate
 */
public class Number extends AbstractFunction {

	public static class Instance extends UniqueObject {
		public final double number;
		private final Number Number;
		Instance(Number Number, double number) {
			super(Number.prototype(), Number);
			this.Number = Number;
			this.number = number;
		}
		Instance(Global global, double number) {
			this(global.Number, number);
		}
		public Instance percent(Instance rhs) {
			return Number.wrap(number % rhs.number);
		}
		public Instance and(Instance rhs) {
			return Number.wrap((long)number & (long)rhs.number);
		}
		public Instance or(Instance rhs) {
			return Number.wrap((long)number | (long)rhs.number);
		}
		public Instance plus(Instance rhs) {
			return Number.wrap(number + rhs.number);
		}
		public Instance minus(Instance rhs) {
			return Number.wrap(number - rhs.number);
		}
		public Instance multiply(Instance rhs) {
			return Number.wrap(number * rhs.number);
		}
		public Instance divide(Instance rhs) {
			return Number.wrap(number / rhs.number);
		}
		public boolean isNaN() {
			return Double.isNaN(number);
		}
		@Override
		public byte toByte() {
			return (byte)number;
		}
		@Override
		public short toShort() {
			return (short)number;
		}
		@Override
		public int toInt() {
			return (int)number;
		}
		@Override
		public long toLong() {
			return (long)number;
		}
		@Override
		public float toFloat() {
			return (float)number;
		}
		@Override
		public double toDouble() {
			return number;
		}
		@Override
		public Instance toNumber() {
			return this;
		}
		@Override
		public Instance clone() {
			return new Instance(Number, number);
		}
		@Override
		public java.lang.String toString() {
			return net.nexustools.njs.Number.toString(number);
		}
		@Override
		public boolean equals(java.lang.Object obj) {
			if(obj == this)
				return !Double.isNaN(number);
			
			if(obj instanceof Instance)
				return ((Instance)obj).number == number && !Double.isNaN(number);
			
			if(obj instanceof java.lang.Number)
				return ((Number)obj).equals(number);
			
			if(obj instanceof java.lang.String)
				try {
					return number == Double.valueOf((java.lang.String)obj);
				} catch(NumberFormatException ex) {
					return false;
				}
			
			if(obj instanceof String.Instance)
				try {
					return number == Double.valueOf(((String.Instance)obj).string);
				} catch(NumberFormatException ex) {
					return false;
				}
			
			return false;
		}
	}
	
	private final List<WeakReference<Instance>> INSTANCES = new ArrayList();
	public Number.Instance NaN;
	public Number.Instance PositiveInfinity;
	public Number.Instance NegativeInfinity;
	public Number.Instance NegativeOne;
	public Number.Instance PositiveOne;
	public Number.Instance Zero;
	public Number() {}

	public void initConstants() {
		NaN = wrap(Double.NaN);
		Zero = wrap(0);
		PositiveOne = wrap(1);
		NegativeOne = wrap(-1);
		PositiveInfinity = wrap(Double.POSITIVE_INFINITY);
		NegativeInfinity = wrap(Double.NEGATIVE_INFINITY);
		PositiveInfinity.seal();
		NegativeInfinity.seal();
		PositiveOne.seal();
		NegativeOne.seal();
		Zero.seal();
		NaN.seal();
	}
	
	protected void initPrototypeFunctions(final Global global) {
		GenericObject prototype = prototype();
		prototype.setHidden("toString", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return global.wrap(Number.toString(((Instance)_this).number));
			}
		});
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		if(params.length == 0)
			return wrap(0).clone();
		
		return params[0].toNumber().clone();
	}
	
	@Override
	public BaseObject call(BaseObject _this, BaseObject... params) {
		if(!_this.instanceOf(this))
			return construct(params);
		return _this;
	}
	
	public final Instance wrap(double number) {
		synchronized(INSTANCES) {
			Iterator<WeakReference<Instance>> it = INSTANCES.iterator();
			while(it.hasNext()) {
				WeakReference<Instance> ref = it.next();
				Instance um = ref.get();
				if(um == null)
					it.remove();
				else if(number == um.number)
					return um;
			}
			
			Instance um = new Instance(this, number);
			um.seal();
			INSTANCES.add(new WeakReference(um));
			return um;
		}
	}
	
	public static java.lang.String toString(double number) {
		java.lang.String dlbString = Double.toString(number);
		int ePos = dlbString.indexOf('E');
		if(ePos > -1) {
			java.lang.String second = dlbString.substring(ePos+1);
			java.lang.String first = dlbString.substring(0, ePos);
			if(first.endsWith(".0"))
				first = first.substring(0, first.length()-2);

			int period = first.indexOf('.');
			if(period > -1) {
				int exp = Integer.valueOf(second);
				if(exp < first.length() - period) {
					int expperiod = exp+period+1;

					if(expperiod >= first.length())
						return first.substring(0, period) + first.substring(period + 1, expperiod);
					return first.substring(0, period) + first.substring(period + 1, expperiod) + '.' + first.substring(expperiod);
				}
			}

			StringBuilder builder = new StringBuilder(first);
			if(second.startsWith("-"))
				builder.append('e');
			else
				builder.append("e+");
			builder.append(second);
			return builder.toString();
		} else if(dlbString.endsWith(".0"))
			return dlbString.substring(0, dlbString.length()-2);
		return dlbString;
	}

	@Override
	public byte toByte() {
		return 0;
	}

	@Override
	public short toShort() {
		return 0;
	}

	@Override
	public int toInt() {
		return 0;
	}

	@Override
	public long toLong() {
		return 0;
	}

	@Override
	public Number.Instance toNumber() {
		return NaN;
	}

	@Override
	public double toDouble() {
		return 0;
	}

	@Override
	public float toFloat() {
		return 0;
	}

	public Instance fromValueOf(BaseObject valueOf) {
		if(valueOf == Null.INSTANCE)
			return Zero;
		if(valueOf == Undefined.INSTANCE)
			return NaN;
		return JSHelper.valueOf(valueOf).toNumber();
	}
	
}
