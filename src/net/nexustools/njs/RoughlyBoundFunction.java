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
public class RoughlyBoundFunction extends AbstractFunction {
	
	BaseObject _this;
	BaseFunction _target;
	public RoughlyBoundFunction(Global global) {
		super(global);
		setHidden("call", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return _target.call(_this, params);
			}
		});
		setHidden("valueOf", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return _target;
			}
		});
	}

	@Override
	public BaseObject call(BaseObject _this, BaseObject... params) {
		return _target.call(this._this, params);
	}

	@Override
	public java.lang.String toString() {
		return _target.toString(); //To change body of generated methods, choose Tools | Templates.
	}
	
}
