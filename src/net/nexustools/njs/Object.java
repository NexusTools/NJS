/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

import java.util.HashMap;
import java.util.Map;
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
		setHidden("defineProperty", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				GenericObject target = (GenericObject)params[0];
				java.lang.String key = params[1].toString();
				BaseObject config = params[2];
				
				ExtendedProperty property = new ExtendedProperty(JSHelper.isTrue(config.get("enumerable")));
				property.configurable = JSHelper.isTrue(config.get("configurable"));
				property.getter = (BaseFunction)config.get("get", OR_NULL);
				property.setter = (BaseFunction)config.get("set", OR_NULL);
				property.value = config.get("value");
				target.setProperty(key, property);
				
				return Undefined.INSTANCE;
			}
			@Override
			public java.lang.String name() {
				return "Object_defineProperty";
			}
		});
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
		setHidden("create", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return new GenericObject(params[0], params[0].constructor());
			}
			@Override
			public java.lang.String name() {
				return "Object_create";
			}
		});
		
		final String.Instance _objectNull = global.wrap("[object Null]");
		final String.Instance _objectUndefined = global.wrap("[object Undefined]");
		final String.Instance _objectArguments = global.wrap("[object Arguments]");
		final String.Instance _objectGlobal = global.wrap("[object Global]");
		final String.Instance _objectObject = global.wrap("[object Object]");
		final Map<BaseFunction, String.Instance> constructorNameMap = new HashMap();
		
		GenericObject prototype = prototype();
		prototype.setHidden("toString", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(_this == null || _this instanceof Null)
					return _objectNull;
				if(_this instanceof Undefined)
					return _objectUndefined;
				if(_this instanceof Arguments)
					return _objectArguments;
				if(_this instanceof Global)
					return _objectGlobal;
				
				try {
					BaseFunction constructor = _this.constructor();
					synchronized(constructorNameMap) {
						String.Instance string = constructorNameMap.get(constructor);
						if(string != null)
							return string;
						Class<?> clazz = constructor.getClass();
						Matcher matcher = BUILT_IN.matcher(clazz.getName());
						if(matcher.matches()) {
							constructorNameMap.put(constructor, string = global.wrap("[object " + matcher.group(1) + "]"));
							return string;
						}
					}
				} catch(Error.JavaException ex) {
				} catch(ClassCastException ex) {}
				
				return _objectObject;
			}
			@Override
			public java.lang.String name() {
				return "Object_prototype_toString";
			}
		});
		prototype.setHidden("__defineGetter__", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				_this.defineGetter(params[0].toString(), (BaseFunction)params[1]);
				return Undefined.INSTANCE;
			}
			@Override
			public java.lang.String name() {
				return "Object_prototype_valueOf";
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
