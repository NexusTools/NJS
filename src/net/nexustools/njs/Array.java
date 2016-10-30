/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author kate
 */
public class Array extends AbstractFunction {

	private final Global global;
	public Array(final Global global) {
		super(global);
		this.global = global;
		GenericObject prototype = prototype();
		prototype.setHidden("shift", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				int length = (int)global.toNumber(_this.get("length")).number;
				if(length < 1)
					return Undefined.INSTANCE;
				
				BaseObject first = _this.get(0);
				for(int i=1; i<length; i++) {
					_this.set(i-1, _this.get(i));
				}
				_this.set("length", global.wrap(length-1));
				return first;
			}
		});
		prototype.setHidden("pop", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				int length = (int)global.toNumber(_this.get("length")).number;
				if(length < 1)
					return Undefined.INSTANCE;
				
				int index = length-1;
				BaseObject popped = _this.get(index);
				_this.delete(index);
				_this.set("length", global.wrap(index));
				return popped;
			}
		});
		prototype.setHidden("filter", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				int length = global.toNumber(_this.get("length")).toInt();
				if(length < 1)
					return new GenericArray(global);
				
				List<BaseObject> copy = new ArrayList();
				BaseFunction filter = (BaseFunction)params[0];
				for(int i=0; i<length; i++) {
					BaseObject value = _this.get(i);
					if(JSHelper.isTrue(filter.call(Undefined.INSTANCE, value)))
						copy.add(value);
				}
				return new GenericArray(global, copy.toArray(new BaseObject[copy.size()]));
			}
		});
		prototype.setHidden("push", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				_this.set(global.toNumber(_this.get("length")).toInt(), params[0]);
				return global.toNumber(_this.get("length"));
			}
		});
		prototype.setHidden("forEach", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				BaseFunction it = (BaseFunction)params[0];
				for(int i=0; i<global.toNumber(_this.get("length")).number; i++) {
					it.call(Undefined.INSTANCE, _this.get(i));
				}
				return Undefined.INSTANCE;
			}
		});
		prototype.setHidden("toString", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				StringBuilder builder = new StringBuilder();
				for(int i=0; i<global.toNumber(_this.get("length")).toInt(); i++) {
					if(i > 0)
						builder.append(',');
					BaseObject value = _this.get(i, OR_NULL);
					if(value != null)
						builder.append(value);
				}
				return global.wrap(builder.toString());
			}
		});
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		if(params.length > 1)
			return new GenericArray(global, this, params);
		return new GenericArray(global, this, params.length > 0 ? global.toArrayRange(params[0]) : 0);
	}

	@Override
	public BaseObject call(BaseObject _this, BaseObject... params) {
		if(!_this.instanceOf(this))
			return construct(params);
		return _this;
	}
	
}
