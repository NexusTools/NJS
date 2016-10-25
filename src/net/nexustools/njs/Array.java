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
public class Array extends AbstractFunction {

	private final Global global;
	public Array(final Global global) {
		super(global);
		this.global = global;
		GenericObject prototype = prototype();
		prototype.setHidden("push", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				_this.set(global.toNumber(_this.get("length")).toInt(), params[0]);
				return global.toNumber(_this.get("length"));
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
