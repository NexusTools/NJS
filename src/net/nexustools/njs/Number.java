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
		public final double value;
		private final Number Number;
		Instance(Number Number, Symbol.Instance iterator, String String, double number) {
			super(Number, iterator, String, (Number)null);
			this.Number = Number;
			this.value = number;
		}
		Instance(Number Number, Global global, double number) {
			super(Number, global);
			this.Number = Number;
			this.value = number;
		}
		Instance(Global global, double number) {
			this(global.Number, global, number);
		}
		public Instance percent(Instance rhs) {
			return Number.wrap(value % rhs.value);
		}
		public Instance and(Instance rhs) {
			return Number.wrap((long)value & (long)rhs.value);
		}
		public Instance or(Instance rhs) {
			return Number.wrap((long)value | (long)rhs.value);
		}
		public Instance plus(Instance rhs) {
			return Number.wrap(value + rhs.value);
		}
		public Instance minus(Instance rhs) {
			return Number.wrap(value - rhs.value);
		}
		public Instance multiply(Instance rhs) {
			return Number.wrap(value * rhs.value);
		}
		public Instance divide(Instance rhs) {
			return Number.wrap(value / rhs.value);
		}
		public boolean isNaN() {
			return Double.isNaN(value);
		}
		@Override
		public byte toByte() {
			return (byte)value;
		}
		@Override
		public short toShort() {
			return (short)value;
		}
		@Override
		public int toInt() {
			return (int)value;
		}
		@Override
		public long toLong() {
			return (long)value;
		}
		@Override
		public float toFloat() {
			return (float)value;
		}
		@Override
		public double toDouble() {
			return value;
		}
		@Override
		public Instance toNumber() {
			return this;
		}
		@Override
		public Instance clone() {
			return new Instance(Number, iterator, String, value);
		}
		@Override
		public java.lang.String toString() {
			return net.nexustools.njs.Number.toString(value);
		}
		@Override
		public boolean equals(java.lang.Object obj) {
			if(obj == this)
				return !Double.isNaN(value);
			
			if(obj instanceof Instance)
				return ((Instance)obj).value == value && !Double.isNaN(value);
			
			if(obj instanceof java.lang.Number)
				return ((Number)obj).equals(value);
			
			if(obj instanceof java.lang.String)
				try {
					return value == Double.valueOf((java.lang.String)obj);
				} catch(NumberFormatException ex) {
					return false;
				}
			
			if(obj instanceof String.Instance)
				try {
					return value == Double.valueOf(((String.Instance)obj).string);
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
		GenericObject prototype = (GenericObject)prototype();
		prototype.setHidden("toString", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return global.wrap(Number.toString(((Instance)_this).value));
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
				else if(number == um.value)
					return um;
			}
			
			Instance um = new Instance(this, iterator, String, number);
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
