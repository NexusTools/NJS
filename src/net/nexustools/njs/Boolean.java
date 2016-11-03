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
 * @author Katelyn Slater <ktaeyln@gmail.com>
 */
public class Boolean extends AbstractFunction {
	public static class Instance extends GenericObject {
		public final boolean value;
		private Instance(Boolean Boolean, Number.Instance number, Global global, boolean value) {
			super(Boolean, global);
			this.value = value;
		}
		@Override
		public boolean equals(java.lang.Object obj) {
			if(obj == this)
				return true;
			
			if(obj instanceof Instance)
				return ((Instance)obj).value == value;
			
			return ((Number.Instance)obj).value == 1;
		}
		@Override
		public boolean toBool() {
			return value;
		}
	}

	public final Instance FALSE, TRUE;
	public Boolean(Global global) {
		super(global);
		FALSE = new Instance(this, global.Zero, global, false);
		TRUE = new Instance(this, global.PositiveOne, global, true);
		FALSE.seal();
		TRUE.seal();
		
		final String.Instance _true = global.wrap("true");
		final String.Instance _false = global.wrap("false");
		
		GenericObject prototype = (GenericObject)prototype();
		prototype.setHidden("toString", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return _this.toBool() ? _true : _false;
			}
		});
	}

	@Override
	public BaseObject call(BaseObject _this, BaseObject... params) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
}
