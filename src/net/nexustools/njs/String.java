/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author kate
 */
public class String extends AbstractFunction {
	public static class Instance extends GenericObject {
		public final java.lang.String string;
		public final String String;
		public Instance(String String, java.lang.String string) {
			super(String.prototype(), String);
			this.String = String;
			this.string = string;
		}
		@Override
		public Instance clone() {
			return new Instance(String, string);
		}
		@Override
		public java.lang.String toString() {
			return string;
		}
	}
	
	private final List<WeakReference<Instance>> WRAPS = new ArrayList();
	public String() {}
	
	protected void initPrototypeFunctions(Global global) {
		GenericObject prototype = prototype();
		prototype.setHidden("toString", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return _this;
			}
		});
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		BaseObject val = params[0];
		if(val instanceof Instance)
			return ((Instance)val).clone();
		
		return new Instance(this, val.toString());
	}
	
	@Override
	public BaseObject call(BaseObject _this, BaseObject... params) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public Instance wrap(java.lang.String string) {
		synchronized(WRAPS) {
			Iterator<WeakReference<Instance>> it = WRAPS.iterator();
			while(it.hasNext()) {
				WeakReference<Instance> ref = it.next();
				Instance um = ref.get();
				if(um == null)
					it.remove();
				else if(string == um.string)
					return um;
			}
			
			Instance um = new Instance(this, string);
			um.seal();
			WRAPS.add(new WeakReference(um));
			return um;
		}
	}
	
	public String.Instance from(BaseObject param) {
		if(param instanceof Instance)
			return (Instance)param;
		
		return wrap(param.toString());
	}
}
