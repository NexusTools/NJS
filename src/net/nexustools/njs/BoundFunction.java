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
public class BoundFunction extends AbstractFunction {
	
	public final BaseObject targetThis;
	public final BaseFunction targetFunction;
	public BoundFunction(Global global, BaseObject _this, BaseFunction _function) {
		super(global);
		targetFunction = _function;
		targetThis = _this;
	}

	@Override
	public BaseObject call(BaseObject _this, BaseObject... params) {
		JSHelper.renameMethodCall(targetFunction.name() + " [bound]");
		return targetFunction.call(targetThis, params);
	}

	@Override
	public java.lang.String name() {
		if(targetFunction == null)
			return "bound";
		return "bound " + targetFunction.name();
	}
	
}
