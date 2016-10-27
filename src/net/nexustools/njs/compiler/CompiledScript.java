/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs.compiler;

import net.nexustools.njs.BaseFunction;
import net.nexustools.njs.BaseObject;
import net.nexustools.njs.Global;
import net.nexustools.njs.JSHelper;
import static net.nexustools.njs.JSHelper.isTrue;
import static net.nexustools.njs.JSHelper.valueOf;
import net.nexustools.njs.Scopeable;

/**
 *
 * @author kate
 */
public abstract class CompiledScript implements Script {
	public static abstract class Debuggable extends CompiledScript {
		public static BaseObject callTopDynamic(String source, BaseObject key, BaseObject _this, BaseObject... params) {
			BaseFunction function;
			try {
				function = (BaseFunction)JSHelper.get(_this, key);
			} catch(ClassCastException ex) {
				throw new net.nexustools.njs.Error.JavaException("ReferenceError", source + " is not a function");
			}
			return function.call(_this, params);
		}
		public static BaseObject callTop(String source, int key, BaseObject _this, BaseObject... params) {
			BaseFunction function;
			try {
				function = (BaseFunction)_this.get(key);
			} catch(ClassCastException ex) {
				throw new net.nexustools.njs.Error.JavaException("ReferenceError", source + " is not a function");
			}
			return function.call(_this, params);
		}
		public static BaseObject callTop(String source, String key, BaseObject _this, BaseObject... params) {
			BaseFunction function;
			try {
				function = (BaseFunction)_this.get(key);
			} catch(ClassCastException ex) {
				throw new net.nexustools.njs.Error.JavaException("ReferenceError", source + " is not a function");
			}
			return function.call(_this, params);
		}
		public static BaseObject callTop(String source, BaseObject _function, BaseObject _this, BaseObject... params) {
			BaseFunction function;
			try {
				function = (BaseFunction)_function;
			} catch(ClassCastException ex) {
				throw new net.nexustools.njs.Error.JavaException("ReferenceError", source + " is not a function");
			}
			return function.call(_this, params);
		}
	}
	public static abstract class Optimized extends CompiledScript {
		public static BaseObject callTopDynamic(BaseObject key, BaseObject _this, BaseObject... params) {
			return ((BaseFunction)JSHelper.get(_this, key)).call(_this, params);
		}
		public static BaseObject callTop(int key, BaseObject _this, BaseObject... params) {
			return ((BaseFunction)_this.get(key)).call(_this, params);
		}
		public static BaseObject callTop(String key, BaseObject _this, BaseObject... params) {
			return ((BaseFunction)_this.get(key)).call(_this, params);
		}
		public static BaseObject callTop(BaseObject _function, BaseObject _this, BaseObject... params) {
			return ((BaseFunction)_function).call(_this, params);
		}
	}
	
	public static BaseObject plus(Global global, BaseObject lhs, BaseObject rhs) {
		lhs = valueOf(lhs);
		rhs = valueOf(rhs);
		if(lhs instanceof net.nexustools.njs.Number.Instance)
			return ((net.nexustools.njs.Number.Instance)lhs).plus(global.toNumber(rhs));
		
		return global.wrap(lhs.toString() + rhs.toString());
	}
	
	public static BaseObject minus(Global global, BaseObject lhs, BaseObject rhs) {
		lhs = valueOf(lhs);
		rhs = valueOf(rhs);
		return global.toNumber(lhs).minus(global.toNumber(rhs));
	}
	
	public static BaseObject divide(Global global, BaseObject lhs, BaseObject rhs) {
		lhs = valueOf(lhs);
		rhs = valueOf(rhs);
		return global.toNumber(lhs).divide(global.toNumber(rhs));
	}
	
	public static BaseObject multiply(Global global, BaseObject lhs, BaseObject rhs) {
		lhs = valueOf(lhs);
		rhs = valueOf(rhs);
		return global.toNumber(lhs).multiply(global.toNumber(rhs));
	}
	
	public static BaseObject or(BaseObject lhs, BaseObject rhs) {
		if(isTrue(lhs))
			return lhs;
		return rhs;
	}
	
	public static BaseObject callSet(Scopeable _this, java.lang.String key, BaseObject val) {
		_this.set(key, val);
		return val;
	}
	
	public static BaseObject callSet(BaseObject _this, BaseObject key, BaseObject val) {
		JSHelper.set(_this, key, val);
		return val;
	}
	
	public static BaseObject callSet(BaseObject _this, int key, BaseObject val) {
		_this.set(key, val);
		return val;
	}
}
