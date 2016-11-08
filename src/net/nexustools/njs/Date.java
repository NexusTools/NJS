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
public class Date extends AbstractFunction {
	public static class Instance extends GenericObject {
		public final java.util.Date date;
		public Instance(Date Date, Symbol.Instance iterator, String String, Number Number, java.util.Date date) {
			super(Date, iterator, String, Number);
			this.date = date;
		}
	}
	
	public Date(final Global global) {
		super(global);
		GenericObject prototype = (GenericObject)prototype();
		prototype.setHidden("valueOf", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(_this instanceof Instance)
					return global.wrap(((Instance)_this).date.getTime());
				return global.NaN;
			}
			@Override
			public java.lang.String name() {
				return "Date_prototype_valueOf";
			}
		});
		prototype.setHidden("toString", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(_this instanceof Instance)
					return global.wrap(((Instance)_this).date.toString());
				throw new Error.JavaException("TypeError", "this is not a Date object.");
			}
			@Override
			public java.lang.String name() {
				return "Date_prototype_toString";
			}
		});
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		java.util.Date date;
		if(params.length > 0)
			try {
				double value = Double.valueOf(params[0].toString());
				if(Double.isInfinite(value) || Double.isNaN(value) || value > Long.MAX_VALUE || value < 0)
					throw new NumberFormatException();
				date = new java.util.Date((long)value);
			} catch(NumberFormatException ex) {
				date = new java.util.Date(java.util.Date.parse(params[0].toString()));
			}
		else
			date = new java.util.Date();
		return new Instance(this, iterator, String, Number, date);
	}

	@Override
	public BaseObject call(BaseObject _this, BaseObject... params) {
		if(!_this.instanceOf(this))
			return construct(params);
		
		return _this;
	}

}
