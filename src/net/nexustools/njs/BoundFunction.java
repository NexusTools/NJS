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
 *
 * @author Katelyn Slater <ktaeyln@gmail.com>
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
