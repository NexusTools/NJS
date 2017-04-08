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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Katelyn Slater <kate@nexustools.com>
 */
public class Scope implements Scopable {

    private static class BlockScopeable implements Scopable {

        private final HashMap<java.lang.String, BaseObject> arguments = new HashMap();

        public BaseObject let(java.lang.String key, BaseObject val) {
            arguments.put(key, val);
            return val;
        }

        @Override
        public BaseObject get(java.lang.String key) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public BaseObject get(java.lang.String key, Or<BaseObject> or) {
            BaseObject arg = arguments.get(key);
            if (arg != null) {
                return arg;
            }

            return or.or(key);
        }

        @Override
        public void set(java.lang.String key, BaseObject val) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void set(java.lang.String key, BaseObject val, Or<Void> or) {
            Iterator<Map.Entry<java.lang.String, BaseObject>> it = arguments.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<java.lang.String, BaseObject> entry = it.next();
                if (entry.getKey().equals(key)) {
                    entry.setValue(val);
                    return;
                }
            }

            or.or(key);
        }

        @Override
        public boolean delete(java.lang.String key) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public boolean delete(java.lang.String key, Or<java.lang.Boolean> or) {
            BaseObject arg = arguments.get(key);
            if (arg != null) {
                return false;
            }

            return or.or(key);
        }

    }

    public static class Extended extends Scope {

        private final HashMap<java.lang.String, BaseObject> storage;

        private Extended(BaseObject _this, HashMap<java.lang.String, BaseObject> storage, Scopable... scopeables) {
            super(_this, scopeables);
            this.storage = storage;
        }

        public Extended(Global global, Scopable... scopeables) {
            super(global, scopeables);
            storage = new HashMap();
        }

        public Extended(BaseObject _this, Global global, Scopable... scopeables) {
            super(_this, global, scopeables);
            storage = new HashMap();
        }

        private Extended(BaseObject _this, Scopable... scopeables) {
            super(_this, scopeables);
            storage = new HashMap();
        }

        @Override
        public BaseObject var(java.lang.String key, BaseObject val) {
            storage.put(key, val);
            return val;
        }

        @Override
        public void set(java.lang.String key, BaseObject val, Or<Void> or) {
            Iterator<Map.Entry<java.lang.String, BaseObject>> it = storage.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<java.lang.String, BaseObject> entry = it.next();
                if (entry.getKey().equals(key)) {
                    entry.setValue(val);
                    return;
                }
            }

            super.set(key, val, or);
        }

        @Override
        public BaseObject get(java.lang.String key, Or<BaseObject> or) {
            Iterator<Map.Entry<java.lang.String, BaseObject>> it = storage.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<java.lang.String, BaseObject> entry = it.next();
                if (entry.getKey().equals(key)) {
                    return entry.getValue();
                }
            }

            return super.get(key, or);
        }

        @Override
        public boolean delete(java.lang.String key, Or<java.lang.Boolean> or) {
            if (storage.containsKey(key)) {
                return false;
            }

            return super.delete(key, or);
        }

    }

    private static class NotFound extends RuntimeException {
    }
    private static final NotFound NOT_FOUND = new NotFound();

    private static class OrNotFound<O> implements BaseObject.Or<O> {

        @Override
        public O or(java.lang.String key) {
            throw NOT_FOUND;
        }
    }
    private static final OrNotFound<Void> OR_NOT_FOUND_V = new OrNotFound();
    private static final OrNotFound<BaseObject> OR_NOT_FOUND_BO = new OrNotFound();
    private static final OrNotFound<java.lang.Boolean> OR_NOT_FOUND_B = new OrNotFound();

    public final BaseObject _this;
    public final Scopable[] scopeables;

    public Scope(BaseObject _this, Scopable... scopeables) {
        this.scopeables = scopeables;
        this._this = _this;
    }

    public Scope(Global global, Scopable... scopeables) {
        this(global, global, scopeables);
    }

    public Scope(BaseObject _this, Global global, Scopable... scopeables) {
        if (scopeables.length == 0) {
            this.scopeables = new Scopable[]{global};
        } else {
            this.scopeables = new Scopable[scopeables.length + 1];
            System.arraycopy(scopeables, 0, this.scopeables, 0, scopeables.length);
            this.scopeables[scopeables.length] = global;
        }
        this._this = _this;
    }
    
    public final void multilet(BaseObject root, java.lang.String... keys) {
        for(int i=0; i<keys.length; i+=2)
            let(keys[i], root.get(keys[i+1]));
    }

    public final BaseObject let(java.lang.String key, BaseObject val) {
        try {
            ((BlockScopeable) scopeables[0]).let(key, val);
        } catch(ClassCastException ex) {
            ((Scopable) scopeables[0]).set(key, val);
        }
        return val;
    }
    
    public final void multivar(BaseObject root, java.lang.String... keys) {
        for(int i=0; i<keys.length; i+=2)
            var(keys[i], root.get(keys[i+1]));
    }

    public final BaseObject var(java.lang.String key) {
        var(key, Undefined.INSTANCE);
        return Undefined.INSTANCE;
    }

    public BaseObject var(java.lang.String key, BaseObject val) {
        scopeables[scopeables.length - 1].set(key, val);
        return val;
    }

    @Override
    public final void set(java.lang.String key, BaseObject val) {
        set(key, val, OR_REFERENCE_ERROR_V);
    }

    @Override
    public void set(java.lang.String key, BaseObject val, Or<Void> or) {
        for (Scopable object : scopeables) {
            try {
                object.set(key, val, OR_NOT_FOUND_V);
                return;
            } catch (NotFound f) {
            }
        }

        or.or(key);
    }

    @Override
    public final BaseObject get(java.lang.String key) {
        return get(key, OR_REFERENCE_ERROR_BO);
    }

    @Override
    public BaseObject get(java.lang.String key, Or<BaseObject> or) {
        if (key.equals("this")) {
            return _this;
        }

        for (Scopable object : scopeables) {
            try {
                return object.get(key, OR_NOT_FOUND_BO);
            } catch (NotFound f) {
            }
        }

        return or.or(key);
    }

    @Override
    public final boolean delete(java.lang.String key) {
        return delete(key, OR_TRUE);
    }

    @Override
    public boolean delete(java.lang.String key, Or<java.lang.Boolean> or) {
        for (Scopable object : scopeables) {
            try {
                return object.delete(key, OR_NOT_FOUND_B);
            } catch (NotFound f) {
            }
        }

        return or.or(key);
    }

    public final Scope extend(BaseObject _this) {
        Scopable[] scopeables = new Scopable[this.scopeables.length + 1];
        System.arraycopy(this.scopeables, 0, scopeables, 1, this.scopeables.length);
        scopeables[0] = this;
        return new Extended(_this, scopeables);
    }

    public Scope extend(BaseObject _this, Scopable stack) {
        Scopable[] scopeables = new Scopable[this.scopeables.length + 2];
        System.arraycopy(this.scopeables, 0, scopeables, 2, this.scopeables.length);
        scopeables[0] = stack;
        scopeables[1] = this;
        return new Extended(_this, scopeables);
    }

    public Scope beginBlock() {
        Scopable[] scopeables = new Scopable[this.scopeables.length + 1];
        System.arraycopy(this.scopeables, 0, scopeables, 1, this.scopeables.length);
        scopeables[0] = new BlockScopeable();
        return this instanceof Extended ? new Extended(_this, ((Extended) this).storage, scopeables) : new Scope(_this, scopeables);
    }

    public final BaseObject resolve(Iterable<java.lang.String> chain) {
        Iterator<java.lang.String> it = chain.iterator();
        if (it.hasNext()) {
            int index = 0;
            boolean useNumberIndex;
            Scopable obj = this;
            do {
                java.lang.String ref = it.next();
                try {
                    if (ref.endsWith(".0")) {
                        ref = ref.substring(0, ref.length() - 2);
                    }
                    if ((index = java.lang.Integer.valueOf(ref)) < 0) {
                        throw new NumberFormatException();
                    }
                    useNumberIndex = obj instanceof BaseObject;
                } catch (NumberFormatException ex) {
                    useNumberIndex = false;
                }
                if (useNumberIndex) {
                    obj = ((BaseObject) obj).get(index);
                } else {
                    obj = obj.get(ref);
                }
            } while (it.hasNext());
            return (BaseObject) obj;
        }

        return _this;
    }

    private static final ThreadLocal<List<Scope>> SCOPE_STACK = new ThreadLocal<List<Scope>>() {
        @Override
        protected List<Scope> initialValue() {
            return new ArrayList();
        }
    };

    public void enter() {
        List<Scope> stack = SCOPE_STACK.get();
        stack.add(this);
    }

    public void exit() {
        List<Scope> stack = SCOPE_STACK.get();
        assert (stack.remove(stack.size() - 1) == this);
    }

    public static Scope current() {
        List<Scope> stack = SCOPE_STACK.get();
        if (stack.isEmpty()) {
            System.err.println("No current scope, eval will be executed in global scope...");
            return null;
        }
        return stack.get(stack.size() - 1);
    }

}
