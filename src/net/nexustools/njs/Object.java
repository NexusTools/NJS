/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author kate
 */
public class Object extends AbstractFunction {
	public static final Pattern BUILT_IN = Pattern.compile("^net\\.nexustools\\.njs\\.([a-zA-Z])$");
	
	public Object() {}

	public void initPrototypeFunctions(final Global global) {
		setHidden("keys", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return new GenericArray(global, params[0].keys().toArray());
			}
			@Override
			public java.lang.String name() {
				return "Object_getOwnPropertyNames";
			}
		});
		setHidden("getOwnPropertyNames", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return new GenericArray(global, params[0].ownPropertyNames().toArray());
			}
			@Override
			public java.lang.String name() {
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
					if(constructor instanceof BaseFunction) {
						Class<?> clazz = constructor.getClass();
						Matcher matcher = BUILT_IN.matcher(clazz.getName());
						if(matcher.matches())
							return global.wrap("[object " + matcher.group(1) + "]");
					}
				} catch(Error.JavaException ex) {}
				
				return global.wrap("[object Object]");
			}
			@Override
			public java.lang.String name() {
				return "Object_prototype_toString";
			}
		});
		prototype.setHidden("valueOf", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return _this;
			}
			@Override
			public java.lang.String name() {
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
