/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

import java.util.Arrays;

/**
 *
 * @author kate
 */
public class Date extends AbstractFunction {
	public static class Instance extends GenericObject {
		public final java.util.Date date;
		public Instance(Date Date, java.util.Date date) {
			super(Date.prototype(), Date);
			this.date = date;
		}
	}
	
	public Date(final Global global) {
		super(global);
		GenericObject prototype = prototype();
		prototype.setHidden("valueOf", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(_this instanceof Instance)
					return global.wrap(((Instance)_this).date.getTime());
				throw new Error.JavaException("TypeError", "this is not a Date object.");
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
		if(params.length > 0)
			try {
				double value = Double.valueOf(params[0].toString());
				if(Double.isInfinite(value) || Double.isNaN(value) || value > Long.MAX_VALUE || value < 0)
					throw new NumberFormatException();
				return new Instance(this, new java.util.Date((long)value));
			} catch(NumberFormatException ex) {
				return new Instance(this, new java.util.Date(java.util.Date.parse(params[0].toString())));
			}
		return new Instance(this, new java.util.Date());
	}

	@Override
	public BaseObject call(BaseObject _this, BaseObject... params) {
		if(!_this.instanceOf(this))
			return construct(params);
		
		return _this;
	}

}
