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
package net.nexustools.njs;

/**
 *
 * @author Katelyn Slater <ktaeyln@gmail.com>
 */
public class JavaObjectWrapper extends GenericObject {

	public final java.lang.Object javaObject;
	JavaObjectWrapper(java.lang.Object javaObject, JavaClassWrapper constructor, Global global) {
		super(constructor, global);
		this.javaObject = javaObject;
	}

	@Override
	public boolean instanceOf(BaseFunction constructor) {
		if(constructor instanceof JavaClassWrapper)
			return ((JavaClassWrapper)constructor).javaClass.isInstance(javaObject);
		return super.instanceOf(constructor);
	}
	
}
