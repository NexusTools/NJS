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
import net.nexustools.njs.Scopeable;

/**
 *
 * @author kate
 */
public abstract class CompiledScript implements Script {
	public static abstract class Debuggable extends CompiledScript {
		public static BaseObject constructTop(String source, BaseObject _this, BaseObject... params) {
			BaseFunction function;
			try {
				function = (BaseFunction)_this;
			} catch(ClassCastException ex) {
				throw new net.nexustools.njs.Error.JavaException("ReferenceError", source + " is not a function");
			}
			return function.construct(params);
		}
		public static BaseObject callNew(String source, BaseObject _this, BaseObject... params) {
			BaseFunction function;
			try {
				function = (BaseFunction)_this;
			} catch(ClassCastException ex) {
				throw new net.nexustools.njs.Error.JavaException("ReferenceError", source + " is not a function");
			}
			return function.call(_this, params);
		}
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
		public static BaseObject callNew(String source, BaseObject _this, BaseObject... params) {
			return ((BaseFunction)_this).call(_this, params);
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
	
	public BaseObject exec() {
		return exec(JSHelper.createExtendedGlobal(), null);
	}
	
	public BaseObject exec(Global global) {
		return exec(global, null);
	}
	
	public static net.nexustools.njs.Number.Instance plusPlusLeft(Global global, java.lang.String key, Scopeable _this) {
		net.nexustools.njs.Number.Instance incremented = global.Number.fromValueOf(_this.get(key)).plus(global.PositiveOne);
		_this.set(key, incremented);
		return incremented;
	}
	
	public static net.nexustools.njs.Number.Instance plusPlusRight(Global global, java.lang.String key, Scopeable _this) {
		net.nexustools.njs.Number.Instance current = global.Number.fromValueOf(_this.get(key));
		_this.set(key, current.plus(global.PositiveOne));
		return current;
	}
	
	public static net.nexustools.njs.Number.Instance minusMinusLeft(Global global, java.lang.String key, Scopeable _this) {
		net.nexustools.njs.Number.Instance incremented = global.Number.fromValueOf(_this.get(key)).plus(global.NegativeOne);
		_this.set(key, incremented);
		return incremented;
	}
	
	public static net.nexustools.njs.Number.Instance minusMinusRight(Global global, java.lang.String key, Scopeable _this) {
		net.nexustools.njs.Number.Instance current = global.Number.fromValueOf(_this.get(key));
		_this.set(key, current.plus(global.NegativeOne));
		return current;
	}
	
	public static net.nexustools.njs.Number.Instance percent(Global global, BaseObject lhs, BaseObject rhs) {
		return global.Number.fromValueOf(lhs).percent(global.Number.fromValueOf(rhs));
	}
	
	public static net.nexustools.njs.Number.Instance and(Global global, BaseObject lhs, BaseObject rhs) {
		return global.Number.fromValueOf(lhs).and(global.Number.fromValueOf(rhs));
	}
	
	public static net.nexustools.njs.Number.Instance or(Global global, BaseObject lhs, BaseObject rhs) {
		return global.Number.fromValueOf(lhs).or(global.Number.fromValueOf(rhs));
	}
	
	public static BaseObject plus(Global global, BaseObject lhs, BaseObject rhs) {
		lhs = JSHelper.valueOf(lhs);
		rhs = JSHelper.valueOf(rhs);
		
		net.nexustools.njs.Number.Instance _lhs = lhs.toNumber();
		net.nexustools.njs.Number.Instance _rhs = rhs.toNumber();
		if((!_lhs.isNaN() && !_rhs.isNaN()) || (lhs instanceof net.nexustools.njs.Number.Instance && rhs instanceof net.nexustools.njs.Number.Instance))
			return _lhs.plus(_rhs);
		
		return global.wrap(lhs.toString() + rhs.toString());
	}
	
	public static net.nexustools.njs.Number.Instance minus(Global global, BaseObject lhs, BaseObject rhs) {
		return global.Number.fromValueOf(lhs).minus(global.Number.fromValueOf(rhs));
	}
	
	public static net.nexustools.njs.Number.Instance divide(Global global, BaseObject lhs, BaseObject rhs) {
		return global.Number.fromValueOf(lhs).divide(global.Number.fromValueOf(rhs));
	}
	
	public static net.nexustools.njs.Number.Instance multiply(Global global, BaseObject lhs, BaseObject rhs) {
		return global.Number.fromValueOf(lhs).multiply(global.Number.fromValueOf(rhs));
	}
	
	public static BaseObject orOr(BaseObject lhs, BaseObject rhs) {
		if(JSHelper.isTrue(lhs))
			return lhs;
		return rhs;
	}
	
	public static BaseObject andAnd(BaseObject lhs, BaseObject rhs) {
		if(JSHelper.isTrue(lhs))
			return rhs;
		return lhs;
	}
	
	public static BaseObject less(Global global, BaseObject lhs, BaseObject rhs) {
		net.nexustools.njs.Number.Instance _lhs = global.Number.fromValueOf(lhs);
		net.nexustools.njs.Number.Instance _rhs = global.Number.fromValueOf(rhs);
		return global.wrap(_lhs.number < _rhs.number);
	}
	
	public static BaseObject more(Global global, BaseObject lhs, BaseObject rhs) {
		net.nexustools.njs.Number.Instance _lhs = global.Number.fromValueOf(lhs);
		net.nexustools.njs.Number.Instance _rhs = global.Number.fromValueOf(rhs);
		return global.wrap(_lhs.number > _rhs.number);
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
	
	public static boolean moreThan(Global global, BaseObject lhs, BaseObject rhs) {
		if(lhs instanceof net.nexustools.njs.String.Instance && rhs instanceof net.nexustools.njs.String.Instance)
			return JSHelper.stringMoreThan(((net.nexustools.njs.String.Instance)lhs).string, ((net.nexustools.njs.String.Instance)rhs).string);
		
		return global.Number.fromValueOf(lhs).number > global.Number.fromValueOf(rhs).number;
	}
	
	public static boolean lessThan(Global global, BaseObject lhs, BaseObject rhs) {
		if(lhs instanceof net.nexustools.njs.String.Instance && rhs instanceof net.nexustools.njs.String.Instance)
			return JSHelper.stringLessThan(((net.nexustools.njs.String.Instance)lhs).string, ((net.nexustools.njs.String.Instance)rhs).string);
		
		return global.Number.fromValueOf(lhs).number < global.Number.fromValueOf(rhs).number;
	}
	
	public static boolean moreEqual(Global global, BaseObject lhs, BaseObject rhs) {
		if(lhs instanceof net.nexustools.njs.String.Instance && rhs instanceof net.nexustools.njs.String.Instance)
			return JSHelper.stringMoreEqual(((net.nexustools.njs.String.Instance)lhs).string, ((net.nexustools.njs.String.Instance)rhs).string);
		
		return global.Number.fromValueOf(lhs).number >= global.Number.fromValueOf(rhs).number;
	}
	
	public static boolean lessEqual(Global global, BaseObject lhs, BaseObject rhs) {
		if(lhs instanceof net.nexustools.njs.String.Instance && rhs instanceof net.nexustools.njs.String.Instance)
			return JSHelper.stringLessEqual(((net.nexustools.njs.String.Instance)lhs).string, ((net.nexustools.njs.String.Instance)rhs).string);
		
		return global.Number.fromValueOf(lhs).number <= global.Number.fromValueOf(rhs).number;
	}
	
}
