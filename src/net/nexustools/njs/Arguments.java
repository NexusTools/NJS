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
public class Arguments extends GenericObject {
	
	public Arguments(Global global, BaseFunction callee, final BaseObject[] args) {
		super(global);
		if(callee != null)
			setStorage("callee", callee, false);
		for(int i=0; i<args.length; i++)
			setStorage(java.lang.String.valueOf(i), args[i], true);
		setStorage("length", global.wrap(args.length), false);
	}
	
}
