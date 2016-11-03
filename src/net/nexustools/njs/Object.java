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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Katelyn Slater <ktaeyln@gmail.com>
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
				
				ExtendedProperty property = new ExtendedProperty(config.get("enumerable").toBool());
				property.configurable = config.get("configurable").toBool();
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
		setHidden("getPrototypeOf", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return params[0].prototypeOf();
			}
			@Override
			public java.lang.String name() {
				return "Object_getOwnPropertyNames";
			}
		});
		setHidden("create", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				GenericObject genericObject = new GenericObject(global);
				genericObject.__proto__ = params[0];
				return genericObject;
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
		
		GenericObject prototype = (GenericObject)prototype();
		prototype.defineProperty("__proto__", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return _this.prototypeOf();
			}
		}, new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				_this.setPrototypeOf(params[0]);
				return Undefined.INSTANCE;
			}
		});
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
		prototype.setHidden("__defineSetter__", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				_this.defineSetter(params[0].toString(), (BaseFunction)params[1]);
				return Undefined.INSTANCE;
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
			return new GenericObject(this, iterator, String, Number);
		
		BaseObject src = params[0];
		GenericObject copy = new GenericObject(this, iterator, String, Number);
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
