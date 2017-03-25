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
 * @author Katelyn Slater <kate@nexustools.com>
 */
public abstract class Number extends AbstractFunction {

	public static class Instance extends GenericObject {
		public final double value;
		public final Number Number;
		public final boolean _const;
		Instance(Number Number, Symbol.Instance iterator, String String, double number, boolean _const) {
			super(Number, iterator, String, (Number)null);
			this.Number = Number;
			this.value = number;
			this._const = _const;
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
			return new Instance(Number, iterator, String, value, false);
		}
		@Override
		public java.lang.String toString() {
			return net.nexustools.njs.Number.toString(value);
		}
		@Override
		public boolean toBool() {
			return value != 0 && !Double.isNaN(value);
		}
		@Override
		public java.lang.String typeOf() {
			return _const ? "number" : "object";
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

		@Override
		public boolean strictEquals(java.lang.Object obj) {
			if(obj == this)
				return true;
			
			return _const && !Double.isNaN(value) && obj instanceof Instance &&
				((Instance)obj)._const && value == ((Instance)obj).value;
		}
	}
	
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
				int radix;
				if(params.length == 0 || (radix = params[0].toInt()) == 10)
					return global.wrap(Number.toString(((Instance)_this).value));
				return global.wrap(Long.toUnsignedString(((Instance)_this).toLong(), radix));
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
	
	public abstract Instance wrap(double number);
	
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

	public Instance fromValueOf(BaseObject valueOf) {
		if(valueOf instanceof Instance && ((Instance)valueOf)._const)
			return (Instance)valueOf;
		if(valueOf instanceof String.Instance && ((String.Instance)valueOf)._const)
			return ((String.Instance)valueOf).toNumber();
		if(valueOf == Null.INSTANCE)
			return Zero;
		if(valueOf == Undefined.INSTANCE)
			return NaN;
		return Utilities.valueOf(valueOf).toNumber();
	}
	
}
