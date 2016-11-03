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
package net.nexustools.njs.compiler;

import net.nexustools.njs.AbstractFunction;
import net.nexustools.njs.BaseObject;
import net.nexustools.njs.GenericObject;
import net.nexustools.njs.Global;

/**
 *
 * @author Katelyn Slater <ktaeyln@gmail.com>
 */
public abstract class CompiledFunction extends AbstractFunction {
	
	public final Global global;
	public CompiledFunction(Global global) {
		super(global);
		this.global = global;
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		BaseObject _this = createPrototype();
		call(_this, params);
		return _this;
	}
	
}
