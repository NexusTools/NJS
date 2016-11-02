/* 
 * Copyright (c) 2016 NexusTools.
 * 
 * This program is free software: you can redistribute it and/or modify  
 * it under the terms of the GNU Lesser General Public License as   
 * published by the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * Lesser General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.nexustools.njs;

/**
 *
 * @author kate
 */
public class Symbol extends AbstractFunction {
	public static class Instance extends GenericObject {
		public final java.lang.String name;
		public Instance(java.lang.String name) {
			super();
			this.name = name;
		}
		public Instance(Symbol Symbol, java.lang.String name, Global global) {
			super(Symbol, global);
			this.name = name;
		}
	}

	private final Global global;
	public Instance unscopables;
	public Symbol(final Global global) {
		super();
		this.global = global;
		if(true)
			return;
		
	}

	protected void initPrototypeFunctions(final Global global) {
		GenericObject prototype = (GenericObject)prototype();
		prototype.setHidden("toString", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return global.wrap("Symbol(" + ((Instance)_this).name + ')');
			}
			@Override
			public java.lang.String name() {
				return "Symbol_prototype_toString";
			}
		});
		prototype.setHidden("valueOf", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				JSHelper.ReplacementStackTraceElement el = JSHelper.renameMethodCall("Symbol.prototype.valueOf");
				try {
					throw new Error.JavaException("TypeError", "Cannot convert a Symbol value to a number");
				} finally {
					el.finishCall();
				}
			}
			@Override
			public java.lang.String name() {
				return "Symbol_prototype_toString";
			}
		});
	}
	
	public void initConstants() {
		setHidden("iterator", iterator = new Instance("Symbol.iterator"));
		setHidden("unscopables", unscopables = new Instance("Symbol.unscopables"));
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		return new Instance(this, params[0].toString(), global);
	}

	@Override
	public BaseObject call(BaseObject _this, BaseObject... params) {
		if(!_this.instanceOf(this))
			return construct(params);
		return _this;
	}
}
