/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author kate
 */
public class Object extends AbstractFunction {
	public Object() {}

	public void initPrototypeFunctions(final Global global) {
		setHidden("keys", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return new GenericArray(global, params[0].keys().toArray());
			}
			@Override
			protected java.lang.String toStringName() {
				return "Object_getOwnPropertyNames";
			}
		});
		setHidden("getOwnPropertyNames", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return new GenericArray(global, params[0].ownPropertyNames().toArray());
			}
			@Override
			protected java.lang.String toStringName() {
				return "Object_getOwnPropertyNames";
			}
		});
		
		GenericObject prototype = prototype();
		prototype.setHidden("toString", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(_this == null || _this instanceof Null)
					return global.wrap("[object Null]");
				if(_this instanceof Undefined)
					return global.wrap("[object Undefined]");
				
				try {
					BaseFunction constructor = _this.constructor();
					if(constructor instanceof AbstractFunction) {
						Class<?> clazz = constructor.getClass();
						if(clazz.getName().startsWith("net.nexustools.njs"))
							return global.wrap("[object " + ((AbstractFunction)constructor).toStringName() + "]");
					}
				} catch(Error.JavaException ex) {}
				
				return global.wrap(_this.toString());
			}
			@Override
			protected java.lang.String toStringName() {
				return "Object_prototype_toString";
			}
		});
		prototype.setHidden("valueOf", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return _this;
			}
			@Override
			protected java.lang.String toStringName() {
				return "Object_prototype_valueOf";
			}
		});
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		if(params.length == 0)
			return new GenericObject(this);
		
		BaseObject src = params[0];
		GenericObject copy = new GenericObject(this);
		for(java.lang.String key : src.keys())
			copy.set(key, src.get(key));
		return copy;
	}

	@Override
	public BaseObject call(BaseObject _this, BaseObject... params) {
		if(!(_this instanceof GenericObject))
			return construct(params);
		return _this;
	}
	
}
