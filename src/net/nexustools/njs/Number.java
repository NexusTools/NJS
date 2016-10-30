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
		public byte toByte() {
			return (byte)number;
		}
		public byte toClampedByte() {
			if(number > 255)
				return (byte)255;
			if(number < 0)
				return 0;
			return (byte)number;
		}
		public short toShort() {
			return (short)number;
		}
		public int toInt() {
			return (int)number;
		}
		public long toLong() {
			return (long)number;
		}
		public float toFloat() {
			return (float)number;
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
				return true;
			
			if(obj instanceof Instance)
				return ((Instance)obj).number == number;
			
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
	public Number() {}
	
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
		
		return from(params[0]).clone();
	}
	
	@Override
	public BaseObject call(BaseObject _this, BaseObject... params) {
		if(!_this.instanceOf(this))
			return construct(params);
		return _this;
	}
	
	public Instance wrap(double number) {
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
	
	public Instance from(BaseObject param) {
		if(param == Undefined.INSTANCE)
			return wrap(Double.NaN);
		if(param == Null.INSTANCE)
			return wrap(0);
		
		BaseObject valueOf = param.get("valueOf");
		if(valueOf != null && valueOf instanceof BaseFunction)
			param = ((BaseFunction)valueOf).call(param);
		if(param instanceof Instance)
			return (Instance)param;
		
		try {
			return new Instance(this, Double.valueOf(param.toString()));
		} catch(NumberFormatException ex) {
			return new Instance(this, Double.NaN);
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
}
