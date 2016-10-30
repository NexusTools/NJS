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
public class Boolean extends AbstractFunction {
	public static class Instance extends GenericObject {
		public final boolean value;
		private Instance(Boolean Boolean, boolean value) {
			super(Boolean.prototype(), Boolean);
			this.value = value;
		}
		@Override
		public boolean equals(java.lang.Object obj) {
			if(obj == this)
				return true;
			
			if(obj instanceof Instance)
				return ((Instance)obj).value == value;
			
			return ((Number.Instance)obj).number == 1;
		}
	}

	public final Instance FALSE, TRUE;
	public Boolean(Global global) {
		super(global);
		FALSE = new Instance(this, false);
		TRUE = new Instance(this, true);
		FALSE.seal();
		TRUE.seal();
		
		final String.Instance _true = global.wrap("true");
		final String.Instance _false = global.wrap("false");
		
		GenericObject prototype = prototype();
		prototype.setHidden("toString", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return JSHelper.isTrue(_this) ? _true : _false;
			}
		});
	}

	@Override
	public BaseObject call(BaseObject _this, BaseObject... params) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
}
