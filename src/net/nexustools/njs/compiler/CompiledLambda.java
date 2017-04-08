/*
 * Copyright (C) 2017 NexusTools.
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

import net.nexustools.njs.BaseObject;
import net.nexustools.njs.LambdaFunction;
import net.nexustools.njs.Global;

/**
 *
 * @author Katelyn Slater
 */
public abstract class CompiledLambda extends LambdaFunction {

    public final Global global;

    public CompiledLambda(BaseObject _this, Global global) {
        super(_this, global);
        this.global = global;
    }

}
