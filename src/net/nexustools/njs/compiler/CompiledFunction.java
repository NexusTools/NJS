/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs.compiler;

import net.nexustools.njs.AbstractFunction;
import net.nexustools.njs.BaseObject;
import net.nexustools.njs.GenericObject;
import net.nexustools.njs.Global;

/**
 *
 * @author kate
 */
public abstract class CompiledFunction extends AbstractFunction {
	
	public final Global global;
	public CompiledFunction(Global global) {
		super(global);
		this.global = global;
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		BaseObject _this = create();
		call(_this, params);
		return _this;
	}
	
}
