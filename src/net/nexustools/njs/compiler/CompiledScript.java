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
package net.nexustools.njs.compiler;

import net.nexustools.njs.AbstractFunction;
import net.nexustools.njs.BaseFunction;
import net.nexustools.njs.BaseObject;
import net.nexustools.njs.Global;
import net.nexustools.njs.Utilities;
import net.nexustools.njs.Scopable;
import net.nexustools.njs.Scope;

/**
 *
 * @author Katelyn Slater <kate@nexustools.com>
 */
public abstract class CompiledScript extends AbstractFunction implements Script {

    public static abstract class Debuggable extends CompiledScript {
    
        public Debuggable() {}
        public Debuggable(Global global) {
            super(global);
        }

        public static BaseObject constructTop(String source, BaseObject _this, BaseObject... params) {
            BaseFunction function;
            try {
                function = (BaseFunction) _this;
            } catch (ClassCastException ex) {
                throw new net.nexustools.njs.Error.JavaException("ReferenceError", source + " is not a function");
            }
            return function.construct(params);
        }

        public static BaseObject callNew(String source, BaseObject _this, BaseObject... params) {
            BaseFunction function;
            try {
                function = (BaseFunction) _this;
            } catch (ClassCastException ex) {
                throw new net.nexustools.njs.Error.JavaException("ReferenceError", source + " is not a function");
            }
            return function.call(_this, params);
        }

        public static BaseObject callTopDynamic(String source, BaseObject key, BaseObject _this, BaseObject... params) {
            BaseFunction function;
            try {
                function = (BaseFunction) Utilities.get(_this, key);
            } catch (ClassCastException ex) {
                throw new net.nexustools.njs.Error.JavaException("ReferenceError", source + " is not a function");
            }
            return function.call(_this, params);
        }

        public static BaseObject callTop(String source, int key, BaseObject _this, BaseObject... params) {
            BaseFunction function;
            try {
                function = (BaseFunction) _this.get(key);
            } catch (ClassCastException ex) {
                throw new net.nexustools.njs.Error.JavaException("ReferenceError", source + " is not a function");
            }
            return function.call(_this, params);
        }

        public static BaseObject callTop(String source, String key, BaseObject _this, BaseObject... params) {
            BaseFunction function;
            try {
                function = (BaseFunction) _this.get(key);
            } catch (ClassCastException ex) {
                throw new net.nexustools.njs.Error.JavaException("ReferenceError", source + " is not a function", ex);
            }
            return function.call(_this, params);
        }

        public static BaseObject callTop(String source, BaseObject _function, BaseObject _this, BaseObject... params) {
            BaseFunction function;
            try {
                function = (BaseFunction) _function;
            } catch (ClassCastException ex) {
                throw new net.nexustools.njs.Error.JavaException("ReferenceError", source + " is not a function");
            }
            return function.call(_this, params);
        }
    }

    public static abstract class Optimized extends CompiledScript {
    
        public Optimized() {}
        public Optimized(Global global) {
            super(global);
        }

        public static BaseObject callTopDynamic(BaseObject key, BaseObject _this, BaseObject... params) {
            return ((BaseFunction) Utilities.get(_this, key)).call(_this, params);
        }

        public static BaseObject callNew(String source, BaseObject _this, BaseObject... params) {
            return ((BaseFunction) _this).call(_this, params);
        }

        public static BaseObject callTop(int key, BaseObject _this, BaseObject... params) {
            return ((BaseFunction) _this.get(key)).call(_this, params);
        }

        public static BaseObject callTop(String key, BaseObject _this, BaseObject... params) {
            return ((BaseFunction) _this.get(key)).call(_this, params);
        }

        public static BaseObject callTop(BaseObject _function, BaseObject _this, BaseObject... params) {
            return ((BaseFunction) _function).call(_this, params);
        }
    }
    
    private final Global global;
    public CompiledScript() {
        global = null;
    }
    public CompiledScript(Global global) {
        super(global);
        this.global = global;
    }

    public BaseObject exec() {
        return exec(Utilities.createExtendedGlobal(), null);
    }

    @Override
    public BaseObject exec(Global global) {
        return exec(global, null);
    }

    public static net.nexustools.njs.Number.Instance plusPlusLeft(Global global, BaseObject key, BaseObject _this) {
        net.nexustools.njs.Number.Instance incremented = global.wrap(global.Number.fromValueOf(Utilities.get(_this, key)).value + 1);
        Utilities.set(_this, key, incremented);
        return incremented;
    }

    public static net.nexustools.njs.Number.Instance plusPlusRight(Global global, BaseObject key, BaseObject _this) {
        net.nexustools.njs.Number.Instance current = global.Number.fromValueOf(Utilities.get(_this, key));
        Utilities.set(_this, key, global.Number.wrap(current.value + 1));
        return current;
    }

    public static net.nexustools.njs.Number.Instance plusPlusLeft(Global global, java.lang.String key, Scopable _this) {
        net.nexustools.njs.Number.Instance incremented = global.wrap(global.Number.fromValueOf(_this.get(key)).value + 1);
        _this.set(key, incremented);
        return incremented;
    }

    public static net.nexustools.njs.Number.Instance plusPlusRight(Global global, java.lang.String key, Scopable _this) {
        net.nexustools.njs.Number.Instance current = global.Number.fromValueOf(_this.get(key));
        _this.set(key, global.Number.wrap(current.value + 1));
        return current;
    }

    public static void plusPlusTop(Global global, BaseObject key, BaseObject _this) {
        Utilities.set(_this, key, global.Number.wrap(global.Number.fromValueOf(Utilities.get(_this, key)).value + 1));
    }

    public static void plusPlusTop(Global global, java.lang.String key, Scopable _this) {
        _this.set(key, global.Number.wrap(global.Number.fromValueOf(_this.get(key)).value + 1));
    }

    public static net.nexustools.njs.Number.Instance minusMinusLeft(Global global, java.lang.String key, Scopable _this) {
        net.nexustools.njs.Number.Instance decremented = global.wrap(global.Number.fromValueOf(_this.get(key)).value - 1);
        _this.set(key, decremented);
        return decremented;
    }

    public static net.nexustools.njs.Number.Instance minusMinusRight(Global global, java.lang.String key, Scopable _this) {
        net.nexustools.njs.Number.Instance current = global.Number.fromValueOf(_this.get(key));
        _this.set(key, global.Number.wrap(current.value - 1));
        return current;
    }

    public static net.nexustools.njs.Number.Instance minusEqual(Global global, java.lang.String key, BaseObject val, Scopable _this) {
        net.nexustools.njs.Number.Instance _val = global.Number.fromValueOf(val);
        
        net.nexustools.njs.Number.Instance current = global.Number.fromValueOf(_this.get(key));
        _this.set(key, current = global.Number.wrap(current.value - _val.value));
        return current;
    }

    public static net.nexustools.njs.Number.Instance minusEqual(Global global, java.lang.String key, double val, Scopable _this) {
        net.nexustools.njs.Number.Instance current = global.Number.fromValueOf(_this.get(key));
        _this.set(key, current = global.Number.wrap(current.value - val));
        return current;
    }

    public static net.nexustools.njs.Number.Instance orEqual(Global global, java.lang.String key, BaseObject val, Scopable _this) {
        net.nexustools.njs.Number.Instance _val = global.Number.fromValueOf(val);
        
        net.nexustools.njs.Number.Instance current = global.Number.fromValueOf(_this.get(key));
        _this.set(key, current = global.Number.wrap((long)current.value | (long)_val.value));
        return current;
    }

    public static net.nexustools.njs.Number.Instance orEqual(Global global, java.lang.String key, double val, Scopable _this) {
        net.nexustools.njs.Number.Instance current = global.Number.fromValueOf(_this.get(key));
        _this.set(key, current = global.Number.wrap((long)current.value | (long)val));
        return current;
    }

    public static net.nexustools.njs.Number.Instance andEqual(Global global, java.lang.String key, BaseObject val, Scopable _this) {
        net.nexustools.njs.Number.Instance _val = global.Number.fromValueOf(val);
        
        net.nexustools.njs.Number.Instance current = global.Number.fromValueOf(_this.get(key));
        _this.set(key, current = global.Number.wrap((long)current.value & (long)_val.value));
        return current;
    }

    public static net.nexustools.njs.Number.Instance andEqual(Global global, java.lang.String key, double val, Scopable _this) {
        net.nexustools.njs.Number.Instance current = global.Number.fromValueOf(_this.get(key));
        _this.set(key, current = global.Number.wrap((long)current.value & (long)val));
        return current;
    }

    public static net.nexustools.njs.Number.Instance dblShiftRightEqual(Global global, java.lang.String key, BaseObject val, Scopable _this) {
        net.nexustools.njs.Number.Instance _val = global.Number.fromValueOf(val);
        
        net.nexustools.njs.Number.Instance current = global.Number.fromValueOf(_this.get(key));
        _this.set(key, current = global.Number.wrap((int)current.value >>> (int)_val.value));
        return current;
    }

    public static net.nexustools.njs.Number.Instance dblShiftRightEqual(Global global, java.lang.String key, double val, Scopable _this) {
        net.nexustools.njs.Number.Instance current = global.Number.fromValueOf(_this.get(key));
        _this.set(key, current = global.Number.wrap((int)current.value >>> (int)val));
        return current;
    }

    public static net.nexustools.njs.Number.Instance multiplyEqual(Global global, java.lang.String key, BaseObject val, Scopable _this) {
        net.nexustools.njs.Number.Instance _val = global.Number.fromValueOf(val);
        
        net.nexustools.njs.Number.Instance current = global.Number.fromValueOf(_this.get(key));
        _this.set(key, current = global.Number.wrap(_val.value * current.value));
        return current;
    }

    public static net.nexustools.njs.Number.Instance multiplyEqual(Global global, java.lang.String key, double val, Scopable _this) {
        net.nexustools.njs.Number.Instance current = global.Number.fromValueOf(_this.get(key));
        _this.set(key, current = global.Number.wrap(val * current.value));
        return current;
    }

    public static net.nexustools.njs.Number.Instance divideEqual(Global global, java.lang.String key, BaseObject val, Scopable _this) {
        net.nexustools.njs.Number.Instance _val = global.Number.fromValueOf(val);
        
        net.nexustools.njs.Number.Instance current = global.Number.fromValueOf(_this.get(key));
        _this.set(key, current = global.Number.wrap(current.value / _val.value));
        return current;
    }

    public static net.nexustools.njs.Number.Instance divideEqual(Global global, java.lang.String key, double val, Scopable _this) {
        net.nexustools.njs.Number.Instance current = global.Number.fromValueOf(_this.get(key));
        _this.set(key, current = global.Number.wrap(current.value / val));
        return current;
    }
    
    public static BaseObject last(BaseObject... values) {
        return values[values.length-1];
    }

    public static BaseObject plusEqual(Global global, java.lang.String key, BaseObject val, Scopable _this) {
        BaseObject result = plus(global, _this.get(key), val);
        _this.set(key, result);
        return result;
    }

    public static BaseObject plus(Global global, BaseObject lhs, BaseObject rhs) {
        lhs = Utilities.valueOf(lhs);
        rhs = Utilities.valueOf(rhs);
        
        if (lhs instanceof net.nexustools.njs.Number.Instance && rhs instanceof net.nexustools.njs.Number.Instance) {
            net.nexustools.njs.Number.Instance _lhs = lhs.toNumber();
            net.nexustools.njs.Number.Instance _rhs = rhs.toNumber();
            return global.wrap(_lhs.value + _rhs.value);
        }

        return global.wrap(lhs.toString() + rhs.toString());
    }

    public static BaseObject orOr(BaseObject lhs, BaseObject rhs) {
        if (lhs.toBool()) {
            return lhs;
        }
        return rhs;
    }

    public static BaseObject andAnd(BaseObject lhs, BaseObject rhs) {
        if (lhs.toBool()) {
            return rhs;
        }
        return lhs;
    }

    public static BaseObject callSet(Scopable _this, java.lang.String key, BaseObject val) {
        _this.set(key, val);
        return val;
    }

    public static BaseObject callSet(BaseObject _this, BaseObject key, BaseObject val) {
        Utilities.set(_this, key, val);
        return val;
    }

    public static BaseObject callSet(BaseObject _this, int key, BaseObject val) {
        _this.set(key, val);
        return val;
    }

    public static boolean moreThan(BaseObject _lhs, BaseObject _rhs) {
        BaseObject lhs = Utilities.valueOf(_lhs);
        BaseObject rhs = Utilities.valueOf(_rhs);

        if (lhs instanceof net.nexustools.njs.String.Instance && rhs instanceof net.nexustools.njs.String.Instance) {
            return Utilities.stringMoreThan(((net.nexustools.njs.String.Instance) lhs).string, ((net.nexustools.njs.String.Instance) rhs).string);
        }

        return lhs.toDouble() > rhs.toDouble();
    }

    public static boolean lessThan(BaseObject _lhs, BaseObject _rhs) {
        BaseObject lhs = Utilities.valueOf(_lhs);
        BaseObject rhs = Utilities.valueOf(_rhs);

        if (lhs instanceof net.nexustools.njs.String.Instance && rhs instanceof net.nexustools.njs.String.Instance) {
            return Utilities.stringLessThan(((net.nexustools.njs.String.Instance) lhs).string, ((net.nexustools.njs.String.Instance) rhs).string);
        }

        return lhs.toDouble() < rhs.toDouble();
    }

    public static boolean moreEqual(BaseObject _lhs, BaseObject _rhs) {
        BaseObject lhs = Utilities.valueOf(_lhs);
        BaseObject rhs = Utilities.valueOf(_rhs);

        if (lhs instanceof net.nexustools.njs.String.Instance && rhs instanceof net.nexustools.njs.String.Instance) {
            return Utilities.stringMoreEqual(((net.nexustools.njs.String.Instance) lhs).string, ((net.nexustools.njs.String.Instance) rhs).string);
        }

        return lhs.toDouble() >= rhs.toDouble();
    }

    public static boolean lessEqual(BaseObject _lhs, BaseObject _rhs) {
        BaseObject lhs = Utilities.valueOf(_lhs);
        BaseObject rhs = Utilities.valueOf(_rhs);

        if (lhs instanceof net.nexustools.njs.String.Instance && rhs instanceof net.nexustools.njs.String.Instance) {
            return Utilities.stringLessEqual(((net.nexustools.njs.String.Instance) lhs).string, ((net.nexustools.njs.String.Instance) rhs).string);
        }

        return lhs.toDouble() <= rhs.toDouble();
    }

    @Override
    public BaseObject call(BaseObject _this, BaseObject... params) {
        return exec(global, Scope.current());
    }

}
