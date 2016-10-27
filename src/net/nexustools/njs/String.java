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
		Instance(Number Number, final String String, final java.lang.String string) {
			this(Number.wrap(string.length()), String, string);
		}
		Instance(BaseObject length, final String String, final java.lang.String string) {
			super(String.prototype(), String);
			this.String = String;
			this.string = string;
			
			setReadOnly("length", length);
		}
		@Override
		public Instance clone() {
			return new Instance(getDirectly("length"), String, string);
		}
		@Override
		public java.lang.String toString() {
			return string;
		}
	}
	
	private Number Number;
	private final List<WeakReference<Instance>> WRAPS = new ArrayList();
	public String() {}
	
	protected void initPrototypeFunctions(Global global) {
		Number = global.Number;
		GenericObject prototype = prototype();
		prototype.setHidden("toString", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return _this;
			}
		});
		prototype.setHidden("charCodeAt", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return Number.wrap(((Instance)_this).string.charAt(params.length > 0 ? Number.from(params[0]).toInt() : 0));
			}
		});
		prototype.setArrayOverride(new ArrayOverride() {
			@Override
			public BaseObject get(int i, BaseObject _this, Or<BaseObject> or) {
				if(i < 0 || i >= ((Instance)_this).string.length())
					return Undefined.INSTANCE;

				return wrap(((Instance)_this).string.substring(i, i+1));
			}

			@Override
			public boolean delete(int i, BaseObject _this, Or<java.lang.Boolean> or) {
				return false;
			}

			@Override
			public void set(int i, BaseObject val, BaseObject _this) {}

			@Override
			public boolean has(int i, BaseObject _this) {
				return i >= 0 && i < ((Instance)_this).string.length();
			}

			@Override
			public int length(BaseObject _this) {
				return ((Instance)_this).string.length();
			}
		});
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		BaseObject val = params[0];
		if(val instanceof Instance)
			return ((Instance)val).clone();
		
		return new Instance(Number, this, val.toString());
	}
	
	@Override
	public BaseObject call(BaseObject _this, BaseObject... params) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public Instance wrap(java.lang.String string) {
		assert(string != null);
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
			
			assert(Number != null);
			Instance um = new Instance(Number, this, string);
			WRAPS.add(new WeakReference(um));
			um.seal();
			return um;
		}
	}
	
	public String.Instance from(BaseObject param) {
		if(param instanceof Instance)
			return (Instance)param;
		
		return wrap(param.toString());
	}
}
