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
public abstract class ConstructableFunction extends AbstractFunction {
	
	public ConstructableFunction(Global global) {
		super(global);
	}
	public ConstructableFunction(String String, Object Object, Function Function) {
		super(String, Object, Function);
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		GenericObject _this = new GenericObject(prototype(), this);
		call(_this, params);
		return _this;
	}
	
}
