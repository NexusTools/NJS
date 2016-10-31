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
public class Date extends AbstractFunction {
	public static class Instance extends GenericObject {
		public final java.util.Date date;
		public Instance(Date Date, Symbol.Instance iterator, String String, Number.Instance instance, java.util.Date date) {
			super(Date, iterator, String, instance);
			this.date = date;
		}
	}
	
	private final Number Number;
	public Date(final Global global) {
		super(global);
		Number = global.Number;
		GenericObject prototype = prototype();
		prototype.number = global.Zero;
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
		return new Instance(this, iterator, String, Number.wrap(date.getTime()), date);
	}

	@Override
	public BaseObject call(BaseObject _this, BaseObject... params) {
		if(!_this.instanceOf(this))
			return construct(params);
		
		return _this;
	}

}
