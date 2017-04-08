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

import net.nexustools.njs.BaseFunction;
import net.nexustools.njs.BaseObject;
import net.nexustools.njs.Global;
import net.nexustools.njs.Utilities;
import net.nexustools.njs.Scope;
import net.nexustools.njs.GenericArray;
import net.nexustools.njs.GenericObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.nexustools.njs.Scopable;

/**
 *
 * @author Katelyn Slater <kate@nexustools.com>
 */
public class RuntimeCompiler extends RegexCompiler {

    static {
        if (System.getProperties().containsKey("NJSNOCOMPILING")) {
            throw new RuntimeException("NJSNOCOMPILING");
        }
    }

    private static class ScriptCompilerData {

        final java.lang.String fileName;
        final java.lang.String methodName;
        final Impl[] functionImpls;

        private ScriptCompilerData(Impl[] functionImpls, java.lang.String fileName, java.lang.String methodName) {
            assert (methodName != null);
            this.functionImpls = functionImpls;
            this.methodName = methodName;
            this.fileName = fileName;
        }

        private void exec(Global global, Scope scope) {
            for (Impl impl : functionImpls) {
                BaseFunction func = (BaseFunction) impl.run(global, scope).get();
                scope.var(func.name(), func);
            }
        }
    }
    private final ArrayList<java.lang.String> methodNameStack = new ArrayList();

    private ScriptCompilerData precompile(ScriptData script, java.lang.String fileName) {
        Function[] functions = script.functions.values().toArray(new Function[script.functions.size()]);
        Impl[] functionImpls = new Impl[functions.length];
        for (int i = 0; i < functionImpls.length; i++) {
            StringBuilder builder = new StringBuilder();
            for (java.lang.String methodName : methodNameStack) {
                if (methodName.startsWith("<")) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append('.');
                }
                builder.append(methodName);
            }
            if (builder.length() > 0) {
                builder.append('.');
            }
            Function function = functions[i];
            builder.append(function.name);
            functionImpls[i] = compile(new ScriptCompilerData(null, fileName, builder.toString()), function);
        }
        int min = methodNameStack.size() - 1;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < methodNameStack.size(); i++) {
            java.lang.String methodName = methodNameStack.get(i);
            if (methodName.startsWith("<") && i < min) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('.');
            }
            builder.append(methodName);
        }
        return new ScriptCompilerData(functionImpls, fileName, builder.toString());
    }

    public static interface Referencable {

        public int rows();

        public int columns();

        public BaseObject get();

        public void set(BaseObject value);

        public boolean delete();
    }

    public static interface KnownReferenceable extends Referencable {

        public java.lang.String source();
    }

    public static class ValueReferenceable implements Referencable {

        public final BaseObject value;
        public final int rows, columns;

        public ValueReferenceable(BaseObject value, int rows, int columns) {
            this.value = value;
            this.columns = columns;
            this.rows = rows;
        }

        @Override
        public BaseObject get() {
            return value;
        }

        @Override
        public void set(BaseObject value) {
            throw new net.nexustools.njs.Error.JavaException("ReferenceError", "Cannot set a value without knowing its parent and index");
        }

        @Override
        public boolean delete() {
            throw new net.nexustools.njs.Error.JavaException("ReferenceError", "Cannot delete a value without knowing its parent and index");
        }

        @Override
        public int rows() {
            return rows;
        }

        @Override
        public int columns() {
            return columns;
        }
    }

    public static class Return extends ValueReferenceable {

        public Return(BaseObject object, int rows, int columns) {
            super(object, rows, columns);
        }
    }

    public static interface ParentedReferenceable extends Referencable {

        public Scopable parent();
    }

    public static class StringKeyReferenceable implements KnownReferenceable, ParentedReferenceable {

        public final Scopable parent;
        public final java.lang.String key;
        public final java.lang.String source;
        public final int rows, columns;

        public StringKeyReferenceable(java.lang.String source, java.lang.String key, Scopable parent, int rows, int columns) {
            this.parent = parent;
            this.source = source;
            this.columns = columns;
            this.rows = rows;
            this.key = key;
        }

        @Override
        public Scopable parent() {
            return parent;
        }

        @Override
        public java.lang.String source() {
            return source;
        }

        @Override
        public BaseObject get() {
            return parent.get(key);
        }

        @Override
        public void set(BaseObject value) {
            parent.set(key, value);
        }

        @Override
        public boolean delete() {
            return parent.delete(key);
        }

        @Override
        public int rows() {
            return rows;
        }

        @Override
        public int columns() {
            return columns;
        }
    }

    public static class IntegerKeyReferenceable implements KnownReferenceable, ParentedReferenceable {

        public final int key;
        public final BaseObject parent;
        public final java.lang.String source;
        public final int rows, columns;

        public IntegerKeyReferenceable(java.lang.String source, int key, BaseObject parent, int rows, int columns) {
            assert (key >= 0);
            this.parent = parent;
            this.source = source;
            this.columns = columns;
            this.rows = rows;
            this.key = key;
        }

        @Override
        public Scopable parent() {
            return parent;
        }

        @Override
        public java.lang.String source() {
            return source;
        }

        @Override
        public BaseObject get() {
            return parent.get(key);
        }

        @Override
        public void set(BaseObject value) {
            parent.set(key, value);
        }

        @Override
        public boolean delete() {
            return parent.delete(key);
        }

        @Override
        public int rows() {
            return rows;
        }

        @Override
        public int columns() {
            return columns;
        }
    }

    public static class ObjectKeyReferenceable implements KnownReferenceable, ParentedReferenceable {

        public final BaseObject key;
        public final BaseObject parent;
        public final java.lang.String source;
        public final int rows, columns;

        public ObjectKeyReferenceable(java.lang.String source, BaseObject key, BaseObject parent, int rows, int columns) {
            this.parent = parent;
            this.source = source;
            this.columns = columns;
            this.rows = rows;
            this.key = key;
        }

        @Override
        public Scopable parent() {
            return parent;
        }

        @Override
        public java.lang.String source() {
            return source;
        }

        @Override
        public BaseObject get() {
            if (key instanceof net.nexustools.njs.String.Instance) {
                return parent.get(((net.nexustools.njs.String.Instance) key).string);
            } else if (key instanceof net.nexustools.njs.Number.Instance && ((net.nexustools.njs.Number.Instance) key).value >= 0
                    && ((net.nexustools.njs.Number.Instance) key).value <= java.lang.Integer.MAX_VALUE && ((net.nexustools.njs.Number.Instance) key).value == (int) ((net.nexustools.njs.Number.Instance) key).value) {
                return parent.get((int) ((net.nexustools.njs.Number.Instance) key).value);
            } else {
                return parent.get(key.toString());
            }
        }

        @Override
        public void set(BaseObject val) {
            if (key instanceof net.nexustools.njs.String.Instance) {
                parent.set(((net.nexustools.njs.String.Instance) key).string, val);
            } else if (key instanceof net.nexustools.njs.Number.Instance && ((net.nexustools.njs.Number.Instance) key).value >= 0
                    && ((net.nexustools.njs.Number.Instance) key).value <= java.lang.Integer.MAX_VALUE && ((net.nexustools.njs.Number.Instance) key).value == (int) ((net.nexustools.njs.Number.Instance) key).value) {
                parent.set((int) ((net.nexustools.njs.Number.Instance) key).value, val);
            } else {
                parent.set(key.toString(), val);
            }
        }

        @Override
        public boolean delete() {
            if (key instanceof net.nexustools.njs.String.Instance) {
                return parent.delete(((net.nexustools.njs.String.Instance) key).string);
            } else if (key instanceof net.nexustools.njs.Number.Instance && ((net.nexustools.njs.Number.Instance) key).value >= 0
                    && ((net.nexustools.njs.Number.Instance) key).value <= java.lang.Integer.MAX_VALUE && ((net.nexustools.njs.Number.Instance) key).value == (int) ((net.nexustools.njs.Number.Instance) key).value) {
                return parent.delete((int) ((net.nexustools.njs.Number.Instance) key).value);
            } else {
                return parent.delete(key.toString());
            }
        }

        @Override
        public int rows() {
            return rows;
        }

        @Override
        public int columns() {
            return columns;
        }
    }

    public static interface Impl {

        public Referencable run(Global global, Scope scope);
    }
    public static final Referencable UNDEFINED_REFERENCE = new Referencable() {
        @Override
        public BaseObject get() {
            return net.nexustools.njs.Undefined.INSTANCE;
        }

        @Override
        public void set(BaseObject value) {
        }

        @Override
        public boolean delete() {
            return false;
        }

        @Override
        public int rows() {
            return 0;
        }

        @Override
        public int columns() {
            return 0;
        }
    };
    public static final Impl UNDEFINED = new Impl() {
        @Override
        public Referencable run(Global global, Scope scope) {
            return UNDEFINED_REFERENCE;
        }
    };
    public static final Impl NULL = new Impl() {
        public final Referencable REFERENCE = new Referencable() {
            @Override
            public BaseObject get() {
                return net.nexustools.njs.Null.INSTANCE;
            }

            @Override
            public void set(BaseObject value) {
            }

            @Override
            public boolean delete() {
                return false;
            }

            @Override
            public int rows() {
                return 0;
            }

            @Override
            public int columns() {
                return 0;
            }
        };

        @Override
        public Referencable run(Global global, Scope scope) {
            return REFERENCE;
        }
    };
    
    private void execute(Impl[] impls, Global global, Scope scope) {
        for(Impl impl : impls)
            impl.run(global, scope).get();
    }
    
    private Impl[] compile(ScriptCompilerData data, Parsed[] objects) {
        Impl[] dat = new Impl[objects.length];
        for(int i=0; i<objects.length; i++)
            dat[i] = compile(data, objects[i]);
        return dat;
    }

    private Impl compile(final ScriptCompilerData data, Parsed object) {
        if (DEBUG) {
            System.out.println("Compiling " + describe(object));
        }

        final int rows = object.rows;
        final int columns = object.columns;
        if (object instanceof Long) {
            final long number = ((Long) object).value;
            if (number == 0) {
                return new Impl() {
                    @Override
                    public Referencable run(Global global, Scope scope) {
                        return new ValueReferenceable(global.Zero, rows, columns);
                    }
                };
            }
            if (number == 1) {
                return new Impl() {
                    @Override
                    public Referencable run(Global global, Scope scope) {
                        return new ValueReferenceable(global.PositiveOne, rows, columns);
                    }
                };
            }
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    return new ValueReferenceable(global.wrap(number), rows, columns);
                }
            };
        } else if (object instanceof Number) {
            final double number = ((Number) object).value;
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    return new ValueReferenceable(global.wrap(number), rows, columns);
                }
            };
        } else if (object instanceof String) {
            final java.lang.String string = ((String) object).string;
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    return new ValueReferenceable(global.wrap(string), rows, columns);
                }
            };
        } else if (object instanceof TemplateLiteral) {
            final java.lang.Object[] parts = ((TemplateLiteral) object).parts;
            for(int i=0; i<parts.length; i++)
                if(parts[i] instanceof Parsed)
                    parts[i] = compile(data, (Parsed)parts[i]);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    StringBuilder builder = new StringBuilder();
                    for(java.lang.Object part : parts) {
                        if(part instanceof Impl)
                            builder.append(((Impl)part).run(global, scope).get().toString());
                        else
                            builder.append(part.toString());
                    }
                    return new ValueReferenceable(global.wrap(builder.toString()), rows, columns);
                }
            };
        } else if (object instanceof Reference) {
            final java.lang.String ref = ((Reference) object).ref;
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    return new StringKeyReferenceable(ref, ref, scope, rows, columns);
                }
            };
        } else if (object instanceof ReferenceChain) {
            final Iterable<java.lang.String> chain = ((ReferenceChain) object).chain;
            final java.lang.String full = join(chain, '.');
            final java.lang.String key = ((ReferenceChain) object).chain.remove(((ReferenceChain) object).chain.size() - 1);
            final java.lang.String base = join(chain, '.');
            try {
                java.lang.String k = key;
                if (k.endsWith(".0")) {
                    k = k.substring(0, k.length() - 2);
                }
                final int index = java.lang.Integer.valueOf(k);
                return new Impl() {
                    @Override
                    public Referencable run(Global global, Scope scope) {
                        try {
                            return new IntegerKeyReferenceable(full, index, scope.resolve(chain), rows, columns);
                        } catch (net.nexustools.njs.Error.JavaException err) {
                            if (err.type.equals("TypeError")) {
                                if (err.getUnderlyingMessage().endsWith("from null")) {
                                    throw new net.nexustools.njs.Error.JavaException("TypeError", "Cannot read property \"" + key + "\" from \"" + base + "\" which is null");
                                }
                                if (err.getUnderlyingMessage().endsWith("from undefined")) {
                                    throw new net.nexustools.njs.Error.JavaException("TypeError", "Cannot read property \"" + key + "\" from \"" + base + "\" which is undefined");
                                }
                            }
                            throw err;
                        }
                    }
                };
            } catch (NumberFormatException ex) {
            }
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    try {
                        return new StringKeyReferenceable(full, key, scope.resolve(chain), rows, columns);
                    } catch (net.nexustools.njs.Error.JavaException err) {
                        if (err.type.equals("TypeError")) {
                            if (err.getUnderlyingMessage().endsWith("from null")) {
                                throw new net.nexustools.njs.Error.JavaException("TypeError", "Cannot read property \"" + key + "\" from \"" + base + "\" which is null");
                            }
                            if (err.getUnderlyingMessage().endsWith("from undefined")) {
                                throw new net.nexustools.njs.Error.JavaException("TypeError", "Cannot read property \"" + key + "\" from \"" + base + "\" which is undefined");
                            }
                        }
                        throw err;
                    }
                }
            };
        } else if (object instanceof New) {
            final Impl reference = compile(data, ((New) object).reference);

            if (((New) object).arguments == null || ((New) object).arguments.isEmpty()) {
                return new Impl() {
                    @Override
                    public Referencable run(Global global, Scope scope) {
                        return new ValueReferenceable(((BaseFunction) reference.run(global, scope).get()).construct(), rows, columns);
                    }
                };
            }

            Object[] args = ((New) object).arguments.toArray();
            final Impl[] argr = new Impl[args.length];
            for (int i = 0; i < args.length; i++) {
                argr[i] = compile(data, (Parsed) args[i]);
            }

            switch (args.length) {
                case 1:
                    return new Impl() {
                        @Override
                        public Referencable run(Global global, Scope scope) {
                            return new ValueReferenceable(((BaseFunction) reference.run(global, scope).get()).construct(argr[0].run(global, scope).get()), rows, columns);
                        }
                    };

                case 2:
                    return new Impl() {
                        @Override
                        public Referencable run(Global global, Scope scope) {
                            return new ValueReferenceable(((BaseFunction) reference.run(global, scope).get()).construct(argr[0].run(global, scope).get(), argr[1].run(global, scope).get()), rows, columns);
                        }
                    };

                case 3:
                    return new Impl() {
                        @Override
                        public Referencable run(Global global, Scope scope) {
                            return new ValueReferenceable(((BaseFunction) reference.run(global, scope).get()).construct(argr[0].run(global, scope).get(), argr[1].run(global, scope).get(), argr[2].run(global, scope).get()), rows, columns);
                        }
                    };

                default:
                    return new Impl() {
                        @Override
                        public Referencable run(Global global, Scope scope) {
                            BaseObject[] args = new BaseObject[argr.length];
                            for (int i = 0; i < args.length; i++) {
                                args[i] = argr[i].run(global, scope).get();
                            }
                            return new ValueReferenceable(((BaseFunction) reference.run(global, scope).get()).construct(args), rows, columns);
                        }
                    };
            }
        } else if (object instanceof Call) {
            final Impl reference = compile(data, ((Call) object).reference);

            if (((Call) object).arguments.isEmpty()) {
                return new Impl() {
                    @Override
                    public Referencable run(Global global, Scope scope) {
                        Referencable ref = reference.run(global, scope);
                        BaseFunction func;
                        try {
                            func = ((BaseFunction) ref.get());
                        } catch (ClassCastException ex) {
                            if (ref instanceof KnownReferenceable) {
                                throw new net.nexustools.njs.Error.JavaException("TypeError", ((KnownReferenceable) ref).source() + " is not a function");
                            }
                            throw new net.nexustools.njs.Error.JavaException("TypeError", "is not a function");
                        }
                        if (ref instanceof ParentedReferenceable) {
                            return new ValueReferenceable(func.call(((ParentedReferenceable) ref).parent() instanceof Scope ? net.nexustools.njs.Undefined.INSTANCE : (BaseObject) ((ParentedReferenceable) ref).parent()), rows, columns);
                        } else {
                            return new ValueReferenceable(func.call(global), rows, columns);
                        }
                    }
                };
            }

            Object[] args = ((Call) object).arguments.toArray();
            final Impl[] argr = new Impl[args.length];
            for (int i = 0; i < args.length; i++) {
                argr[i] = compile(data, (Parsed) args[i]);
            }

            switch (args.length) {
                case 1:
                    return new Impl() {
                        @Override
                        public Referencable run(Global global, Scope scope) {
                            Referencable ref = reference.run(global, scope);
                            BaseObject val = ref.get();
                            BaseFunction func;
                            try {
                                func = ((BaseFunction) val);
                            } catch (Exception ex) {
                                if (ref instanceof KnownReferenceable) {
                                    throw new net.nexustools.njs.Error.JavaException("TypeError", ((KnownReferenceable) ref).source() + " is not a function, it is a " + val.typeOf());
                                }
                                throw new net.nexustools.njs.Error.JavaException("TypeError", "is not a function");
                            }
                            if (ref instanceof ParentedReferenceable) {
                                return new ValueReferenceable(func.call(((ParentedReferenceable) ref).parent() instanceof Scope ? net.nexustools.njs.Undefined.INSTANCE : (BaseObject) ((ParentedReferenceable) ref).parent(), argr[0].run(global, scope).get()), rows, columns);
                            } else {
                                return new ValueReferenceable(func.call(global, argr[0].run(global, scope).get()), rows, columns);
                            }
                        }
                    };

                case 2:
                    return new Impl() {
                        @Override
                        public Referencable run(Global global, Scope scope) {
                            Referencable ref = reference.run(global, scope);
                            BaseFunction func;
                            try {
                                func = ((BaseFunction) ref.get());
                            } catch (Exception ex) {
                                if (ref instanceof KnownReferenceable) {
                                    throw new net.nexustools.njs.Error.JavaException("TypeError", ((KnownReferenceable) ref).source() + " is not a function");
                                }
                                throw new net.nexustools.njs.Error.JavaException("TypeError", "is not a function");
                            }
                            if (ref instanceof ParentedReferenceable) {
                                return new ValueReferenceable(func.call(((ParentedReferenceable) ref).parent() instanceof Scope ? net.nexustools.njs.Undefined.INSTANCE : (BaseObject) ((ParentedReferenceable) ref).parent(), argr[0].run(global, scope).get(), argr[1].run(global, scope).get()), rows, columns);
                            } else {
                                return new ValueReferenceable(func.call(global, argr[0].run(global, scope).get(), argr[1].run(global, scope).get()), rows, columns);
                            }
                        }
                    };

                case 3:
                    return new Impl() {
                        @Override
                        public Referencable run(Global global, Scope scope) {
                            Referencable ref = reference.run(global, scope);
                            BaseFunction func;
                            try {
                                func = ((BaseFunction) ref.get());
                            } catch (Exception ex) {
                                if (ref instanceof KnownReferenceable) {
                                    throw new net.nexustools.njs.Error.JavaException("TypeError", ((KnownReferenceable) ref).source() + " is not a function");
                                }
                                throw new net.nexustools.njs.Error.JavaException("TypeError", "is not a function");
                            }
                            if (ref instanceof ParentedReferenceable) {
                                return new ValueReferenceable(func.call(((ParentedReferenceable) ref).parent() instanceof Scope ? net.nexustools.njs.Undefined.INSTANCE : (BaseObject) ((ParentedReferenceable) ref).parent(), argr[0].run(global, scope).get(), argr[1].run(global, scope).get(), argr[2].run(global, scope).get()), rows, columns);
                            } else {
                                return new ValueReferenceable(func.call(global, argr[0].run(global, scope).get(), argr[1].run(global, scope).get(), argr[2].run(global, scope).get()), rows, columns);
                            }
                        }
                    };

                default:
                    return new Impl() {
                        @Override
                        public Referencable run(Global global, Scope scope) {
                            Referencable ref = reference.run(global, scope);
                            BaseFunction func;
                            try {
                                func = ((BaseFunction) ref.get());
                            } catch (Exception ex) {
                                if (ref instanceof KnownReferenceable) {
                                    throw new net.nexustools.njs.Error.JavaException("TypeError", ((KnownReferenceable) ref).source() + " is not a function");
                                }
                                throw new net.nexustools.njs.Error.JavaException("TypeError", "is not a function");
                            }
                            BaseObject[] args = new BaseObject[argr.length];
                            for (int i = 0; i < args.length; i++) {
                                args[i] = argr[i].run(global, scope).get();
                            }
                            if (ref instanceof ParentedReferenceable) {
                                return new ValueReferenceable(func.call(((ParentedReferenceable) ref).parent() instanceof Scope ? net.nexustools.njs.Undefined.INSTANCE : (BaseObject) ((ParentedReferenceable) ref).parent(), args), rows, columns);
                            } else {
                                return new ValueReferenceable(func.call(global, args), rows, columns);
                            }
                        }
                    };
            }
        } else if (object instanceof Multiply) {
            final Impl lhs = compile(data, ((Multiply) object).lhs);
            final Impl rhs = compile(data, ((Multiply) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    return new ValueReferenceable(global.wrap(global.Number.fromValueOf(lhs.run(global, scope).get()).value * global.Number.fromValueOf(rhs.run(global, scope).get()).value), rows, columns);
                }
            };
        } else if (object instanceof Divide) {
            final Impl lhs = compile(data, ((Divide) object).lhs);
            final Impl rhs = compile(data, ((Divide) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    return new ValueReferenceable(global.wrap(global.Number.fromValueOf(lhs.run(global, scope).get()).value / global.Number.fromValueOf(rhs.run(global, scope).get()).value), rows, columns);
                }
            };
        } else if (object instanceof Minus) {
            final Impl lhs = compile(data, ((Minus) object).lhs);
            final Impl rhs = compile(data, ((Minus) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    return new ValueReferenceable(global.wrap(global.Number.fromValueOf(lhs.run(global, scope).get()).value - global.Number.fromValueOf(rhs.run(global, scope).get()).value), rows, columns);
                }
            };
        } else if (object instanceof Percent) {
            final Impl lhs = compile(data, ((Percent) object).lhs);
            final Impl rhs = compile(data, ((Percent) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    return new ValueReferenceable(global.wrap(global.Number.fromValueOf(lhs.run(global, scope).get()).value % global.Number.fromValueOf(rhs.run(global, scope).get()).value), rows, columns);
                }
            };
        } else if (object instanceof And) {
            final Impl lhs = compile(data, ((And) object).lhs);
            final Impl rhs = compile(data, ((And) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    return new ValueReferenceable(global.wrap((long) global.Number.fromValueOf(lhs.run(global, scope).get()).value & (long) global.Number.fromValueOf(rhs.run(global, scope).get()).value), rows, columns);
                }
            };
        } else if (object instanceof Not) {
            final Impl rhs = compile(data, ((Not) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    return new ValueReferenceable(global.wrap(!rhs.run(global, scope).get().toBool()), rows, columns);
                }
            };
        } else if (object instanceof Or) {
            final Impl lhs = compile(data, ((Or) object).lhs);
            final Impl rhs = compile(data, ((Or) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    return new ValueReferenceable(global.wrap((long) global.Number.fromValueOf(lhs.run(global, scope).get()).value | (long) global.Number.fromValueOf(rhs.run(global, scope).get()).value), rows, columns);
                }
            };
        } else if (object instanceof Equals) {
            final Impl lhs = compile(data, ((Equals) object).lhs);
            final Impl rhs = compile(data, ((Equals) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    return new ValueReferenceable(global.wrap(lhs.run(global, scope).get().equals(rhs.run(global, scope).get())), rows, columns);
                }
            };
        } else if (object instanceof NotEquals) {
            final Impl lhs = compile(data, ((NotEquals) object).lhs);
            final Impl rhs = compile(data, ((NotEquals) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    return new ValueReferenceable(global.wrap(!lhs.run(global, scope).get().equals(rhs.run(global, scope).get())), rows, columns);
                }
            };
        } else if (object instanceof StrictEquals) {
            final Impl lhs = compile(data, ((StrictEquals) object).lhs);
            final Impl rhs = compile(data, ((StrictEquals) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    return new ValueReferenceable(lhs.run(global, scope).get().strictEquals(rhs.run(global, scope).get()) ? global.Boolean.TRUE : global.Boolean.FALSE, rows, columns);
                }
            };
        } else if (object instanceof StrictNotEquals) {
            final Impl lhs = compile(data, ((StrictNotEquals) object).lhs);
            final Impl rhs = compile(data, ((StrictNotEquals) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    return new ValueReferenceable(lhs.run(global, scope).get().strictEquals(rhs.run(global, scope).get()) ? global.Boolean.FALSE : global.Boolean.TRUE, rows, columns);
                }
            };
        } else if (object instanceof LessThan) {
            final Impl _lhs = compile(data, ((LessThan) object).lhs);
            final Impl _rhs = compile(data, ((LessThan) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    BaseObject lhs = _lhs.run(global, scope).get();
                    BaseObject rhs = _rhs.run(global, scope).get();

                    if (lhs instanceof net.nexustools.njs.String.Instance && rhs instanceof net.nexustools.njs.String.Instance) {
                        return new ValueReferenceable(Utilities.stringLessThan(((net.nexustools.njs.String.Instance) lhs).string, ((net.nexustools.njs.String.Instance) rhs).string) ? global.Boolean.TRUE : global.Boolean.FALSE, rows, columns);
                    }

                    return new ValueReferenceable((global.Number.fromValueOf(lhs).value < global.Number.fromValueOf(rhs).value) ? global.Boolean.TRUE : global.Boolean.FALSE, rows, columns);
                }
            };
        } else if (object instanceof LessEqual) {
            final Impl _lhs = compile(data, ((LessEqual) object).lhs);
            final Impl _rhs = compile(data, ((LessEqual) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    BaseObject lhs = _lhs.run(global, scope).get();
                    BaseObject rhs = _rhs.run(global, scope).get();

                    if (lhs instanceof net.nexustools.njs.String.Instance && rhs instanceof net.nexustools.njs.String.Instance) {
                        return new ValueReferenceable(Utilities.stringLessEqual(((net.nexustools.njs.String.Instance) lhs).string, ((net.nexustools.njs.String.Instance) rhs).string) ? global.Boolean.TRUE : global.Boolean.FALSE, rows, columns);
                    }

                    return new ValueReferenceable((global.Number.fromValueOf(lhs).value <= global.Number.fromValueOf(rhs).value) ? global.Boolean.TRUE : global.Boolean.FALSE, rows, columns);
                }
            };
        } else if (object instanceof MoreThan) {
            final Impl _lhs = compile(data, ((MoreThan) object).lhs);
            final Impl _rhs = compile(data, ((MoreThan) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    BaseObject lhs = _lhs.run(global, scope).get();
                    BaseObject rhs = _rhs.run(global, scope).get();

                    if (lhs instanceof net.nexustools.njs.String.Instance && rhs instanceof net.nexustools.njs.String.Instance) {
                        return new ValueReferenceable(Utilities.stringMoreThan(((net.nexustools.njs.String.Instance) lhs).string, ((net.nexustools.njs.String.Instance) rhs).string) ? global.Boolean.TRUE : global.Boolean.FALSE, rows, columns);
                    }

                    return new ValueReferenceable((global.Number.fromValueOf(lhs).value > global.Number.fromValueOf(rhs).value) ? global.Boolean.TRUE : global.Boolean.FALSE, rows, columns);
                }
            };
        } else if (object instanceof MoreEqual) {
            final Impl _lhs = compile(data, ((MoreEqual) object).lhs);
            final Impl _rhs = compile(data, ((MoreEqual) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    BaseObject lhs = _lhs.run(global, scope).get();
                    BaseObject rhs = _rhs.run(global, scope).get();

                    if (lhs instanceof net.nexustools.njs.String.Instance && rhs instanceof net.nexustools.njs.String.Instance) {
                        return new ValueReferenceable(Utilities.stringMoreEqual(((net.nexustools.njs.String.Instance) lhs).string, ((net.nexustools.njs.String.Instance) rhs).string) ? global.Boolean.TRUE : global.Boolean.FALSE, rows, columns);
                    }

                    return new ValueReferenceable((global.Number.fromValueOf(lhs).value >= global.Number.fromValueOf(rhs).value) ? global.Boolean.TRUE : global.Boolean.FALSE, rows, columns);
                }
            };
        } else if (object instanceof MultiplyEq) {
            final Impl lhs = compile(data, ((MultiplyEq) object).lhs);
            final Impl rhs = compile(data, ((MultiplyEq) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    Referencable ref = lhs.run(global, scope);

                    net.nexustools.njs.Number.Instance number = global.wrap(global.Number.fromValueOf(ref.get()).value * global.Number.fromValueOf(rhs.run(global, scope).get()).value);
                    ref.set(number);
                    return new ValueReferenceable(number, rows, columns);
                }
            };
        } else if (object instanceof AndEq) {
            final Impl lhs = compile(data, ((AndEq) object).lhs);
            final Impl rhs = compile(data, ((AndEq) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    Referencable ref = lhs.run(global, scope);

                    net.nexustools.njs.Number.Instance number = global.wrap((long)global.Number.fromValueOf(ref.get()).value & (long)global.Number.fromValueOf(rhs.run(global, scope).get()).value);
                    ref.set(number);
                    return new ValueReferenceable(number, rows, columns);
                }
            };
        } else if (object instanceof OrEq) {
            final Impl lhs = compile(data, ((OrEq) object).lhs);
            final Impl rhs = compile(data, ((OrEq) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    Referencable ref = lhs.run(global, scope);

                    net.nexustools.njs.Number.Instance number = global.wrap((long)global.Number.fromValueOf(ref.get()).value | (long)global.Number.fromValueOf(rhs.run(global, scope).get()).value);
                    ref.set(number);
                    return new ValueReferenceable(number, rows, columns);
                }
            };
        } else if (object instanceof Plus) {
            Parsed l = ((Plus) object).lhs;
            final Impl rhs = compile(data, ((Plus) object).rhs);
            if(l == null)
                return new Impl() {
                    @Override
                    public Referencable run(Global global, Scope scope) {
                        BaseObject r = rhs.run(global, scope).get();
                        return new ValueReferenceable(global.Number.fromValueOf(r), rows, columns);
                    }
                };
            final Impl lhs = compile(data, l);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    BaseObject l = Utilities.valueOf(lhs.run(global, scope).get());
                    BaseObject r = Utilities.valueOf(rhs.run(global, scope).get());

                    if (l instanceof net.nexustools.njs.Number.Instance && r instanceof net.nexustools.njs.Number.Instance) {
                        net.nexustools.njs.Number.Instance _lhs = l.toNumber();
                        net.nexustools.njs.Number.Instance _rhs = r.toNumber();
                        return new ValueReferenceable(global.wrap(_lhs.value + _rhs.value), rows, columns);
                    }

                    return new ValueReferenceable(global.wrap(l.toString() + r.toString()), rows, columns);
                }
            };
        } else if (object instanceof PlusEq) {
            final Impl lhs = compile(data, ((PlusEq) object).lhs);
            final Impl rhs = compile(data, ((PlusEq) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    Referencable ref = lhs.run(global, scope);
                    BaseObject l = ref.get();
                    BaseObject r = rhs.run(global, scope).get();

                    BaseObject value;
                    if (l instanceof net.nexustools.njs.Number.Instance && r instanceof net.nexustools.njs.Number.Instance) {
                        net.nexustools.njs.Number.Instance _lhs = global.Number.fromValueOf(l);
                        net.nexustools.njs.Number.Instance _rhs = global.Number.fromValueOf(r);
                        value = global.wrap(_lhs.value + _rhs.value);
                    } else
                        value = global.wrap(l.toString() + r.toString());

                    ref.set(value);
                    return new ValueReferenceable(value, rows, columns);
                }
            };
        } else if (object instanceof MinusEq) {
            final Impl lhs = compile(data, ((MinusEq) object).lhs);
            final Impl rhs = compile(data, ((MinusEq) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    Referencable ref = lhs.run(global, scope);

                    net.nexustools.njs.Number.Instance number = global.wrap(global.Number.fromValueOf(ref.get()).value - global.Number.fromValueOf(rhs.run(global, scope).get()).value);
                    ref.set(number);
                    return new ValueReferenceable(number, rows, columns);
                }
            };
        } else if (object instanceof DivideEq) {
            final Impl lhs = compile(data, ((DivideEq) object).lhs);
            final Impl rhs = compile(data, ((DivideEq) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    Referencable ref = lhs.run(global, scope);

                    net.nexustools.njs.Number.Instance number = global.wrap(global.Number.fromValueOf(ref.get()).value / global.Number.fromValueOf(rhs.run(global, scope).get()).value);
                    ref.set(number);
                    return new ValueReferenceable(number, rows, columns);
                }
            };
        } else if (object instanceof OpenBracket) {
            return compile(data, ((OpenBracket) object).contents);
        } else if (object instanceof Set) {
            final Impl lhs = compile(data, ((Set) object).lhs);
            final Impl rhs = compile(data, ((Set) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    Referencable ref = lhs.run(global, scope);
                    BaseObject r = rhs.run(global, scope).get();
                    ref.set(r);
                    return new ValueReferenceable(r, rows, columns);
                }
            };
        } else if (object instanceof RegexCompiler.Return) {
            final Impl ret = compile(data, ((RegexCompiler.Return) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    return new Return(ret.run(global, scope).get(), rows, columns);
                }
            };
        } else if (object instanceof Var) {
            List<Var.Set> ret = ((Var) object).sets;

            final int len = ret.size();
            final java.lang.Object[] keys = new java.lang.Object[len];
            final Impl[] values = new Impl[len];

            for (int i = 0; i < len; i++) {
                Var.Set set = ret.get(i);
                if(set.lhs instanceof Reference) {
                    keys[i] = ((Reference)set.lhs).ref;
                } else {
                    keys[i] = set.lhs;
                }
                if (set.rhs != null) {
                    values[i] = compile(data, set.rhs);
                }
            }

            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    int i = 0;
                    for (; i < len; i++) {
                        if(keys[i] instanceof java.lang.String) {
                            if (values[i] != null) {
                                scope.var((java.lang.String)keys[i], values[i].run(global, scope).get());
                            } else {
                                scope.var((java.lang.String)keys[i]);
                            }
                        } else {
                            BaseObject input = values[i].run(global, scope).get();
                            NameSet set = (NameSet)keys[i];
                            if(set.array) {
                                int a = 0;
                                for(java.lang.String name : set.names.keySet()) {
                                    scope.var(name, input.get(a++));
                                }
                            } else {
                                for(Map.Entry<java.lang.String, java.lang.String> name : set.names.entrySet()) {
                                    scope.var(name.getValue(), input.get(name.getKey()));
                                }
                            }
                        }
                    }
                    for (; i < keys.length; i++) {
                        if(keys[i] instanceof java.lang.String) {
                            if (values[i] != null) {
                                scope.var((java.lang.String)keys[i], values[i].run(global, scope).get());
                            } else {
                                scope.var((java.lang.String)keys[i]);
                            }
                        } else {
                            BaseObject input = values[i].run(global, scope).get();
                            NameSet set = (NameSet)keys[i];
                            if(set.array) {
                                int a = 0;
                                for(java.lang.String name : set.names.keySet()) {
                                    scope.var(name, input.get(a++));
                                }
                            } else {
                                for(Map.Entry<java.lang.String, java.lang.String> name : set.names.entrySet()) {
                                    scope.var(name.getValue(), input.get(name.getKey()));
                                }
                            }
                        }
                    }
                    return UNDEFINED_REFERENCE;
                }
            };
        } else if (object instanceof Function) {
            final java.lang.String[] args = ((Function) object).arguments.toArray(new java.lang.String[((Function) object).arguments.size()]);
            final java.lang.String name = ((Function) object).name == null ? "<anonymous>" : ((Function) object).name;
            final java.lang.String source = ((Function) object).source;
            final java.lang.String vararg = ((Function) object).vararg;

            StringBuilder argBuilder = new StringBuilder();
            Iterator<java.lang.String> it = ((Function) object).arguments.iterator();
            if (it.hasNext()) {
                argBuilder.append(it.next());
                while (it.hasNext()) {
                    argBuilder.append(", ");
                    argBuilder.append(it.next());
                }
            }
            final java.lang.String arguments = argBuilder.toString();
            final Script impl;
            methodNameStack.add(name);
            try {
                impl = compileScript(((Function) object).impl, data.fileName, true);
            } finally {
                methodNameStack.remove(methodNameStack.size() - 1);
            }
            return new Impl() {
                @Override
                public Referencable run(final Global global, final Scope scope) {
                    CompiledFunction func = new CompiledFunction(global) {
                        @Override
                        public BaseObject call(BaseObject _this, BaseObject... params) {
                            Scope s = scope.extend(_this);
                            s.var("arguments", new net.nexustools.njs.Arguments(global, this, params));
                            int max = Math.min(args.length, params.length);
                            int i = 0;
                            for (; i < max; i++) {
                                s.var(args[i], params[i]);
                            }
                            for (; i < args.length; i++) {
                                s.var(args[i]);
                            }
                            if(vararg != null) {
                                if(params.length > args.length)
                                    s.var(vararg, new GenericArray(global, params, args.length));
                                else
                                    s.var(vararg, new GenericArray(global));
                            }
                            s.var("callee", this);
                            return impl.exec(global, s);
                        }

                        @Override
                        public java.lang.String source() {
                            return source;
                        }

                        @Override
                        public java.lang.String arguments() {
                            return arguments;
                        }

                        @Override
                        public java.lang.String name() {
                            return name;
                        }
                    };
                    return new ValueReferenceable(func, rows, columns);
                }
            };
        } else if (object instanceof RightReference) {
            final java.lang.String source = object.toString();
            final Impl ref = compile(data, ((RightReference) object).ref);
            final Iterable<java.lang.String> keys = ((RightReference) object).chain;
            final java.lang.String key = ((List<java.lang.String>) keys).remove(((List) keys).size() - 1);
            try {
                java.lang.String k = key;
                if (k.endsWith(".0")) {
                    k = k.substring(0, k.length() - 2);
                }
                final int index = java.lang.Integer.valueOf(k);
                return new Impl() {
                    @Override
                    public Referencable run(Global global, Scope scope) {
                        BaseObject lhs = ref.run(global, scope).get();
                        Iterator<java.lang.String> it = keys.iterator();
                        while (it.hasNext()) {
                            lhs = lhs.get(it.next());
                        }
                        return new IntegerKeyReferenceable(source, index, lhs, rows, columns);
                    }
                };
            } catch (NumberFormatException ex) {
            }
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    BaseObject lhs = ref.run(global, scope).get();
                    Iterator<java.lang.String> it = keys.iterator();
                    while (it.hasNext()) {
                        lhs = lhs.get(it.next());
                    }
                    return new StringKeyReferenceable(source, key, lhs, rows, columns);
                }
            };
        } else if (object instanceof OpenArray) {
            final Impl[] entries = new Impl[((OpenArray) object).entries.size()];
            for (int i = 0; i < entries.length; i++) {
                entries[i] = compile(data, ((OpenArray) object).entries.get(i));
            }
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    GenericArray array = new GenericArray(global, entries.length);
                    for (int i = 0; i < entries.length; i++) {
                        array.set(i, entries[i].run(global, scope).get());
                    }
                    return new ValueReferenceable(array, rows, columns);
                }
            };
        } else if (object instanceof OrOr) {
            final Impl lhs = compile(data, ((OrOr) object).lhs);
            final Impl rhs = compile(data, ((OrOr) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    BaseObject l = lhs.run(global, scope).get();
                    if (l.toBool()) {
                        return new ValueReferenceable(l, rows, columns);
                    }
                    return rhs.run(global, scope);
                }
            };
        } else if (object instanceof AndAnd) {
            final Impl lhs = compile(data, ((AndAnd) object).lhs);
            final Impl rhs = compile(data, ((AndAnd) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    BaseObject l = lhs.run(global, scope).get();
                    if (l.toBool()) {
                        return rhs.run(global, scope);
                    }
                    return new ValueReferenceable(l, rows, columns);
                }
            };
        } else if (object instanceof PlusPlus) {
            final Impl _ref = compile(data, ((PlusPlus) object).ref);
            if (((PlusPlus) object).right) {
                return new Impl() {
                    @Override
                    public Referencable run(Global global, Scope scope) {
                        Referencable ref = _ref.run(global, scope);
                        net.nexustools.njs.Number.Instance val = global.Number.fromValueOf(ref.get());
                        ref.set(global.wrap(val.value + 1));
                        return new ValueReferenceable(val, rows, columns);
                    }
                };
            } else {
                return new Impl() {
                    @Override
                    public Referencable run(Global global, Scope scope) {
                        Referencable ref = _ref.run(global, scope);
                        net.nexustools.njs.Number.Instance val = global.Number.fromValueOf(ref.get());
                        ref.set(val = global.wrap(val.value + 1));
                        return new ValueReferenceable(val, rows, columns);
                    }
                };
            }
        } else if (object instanceof MinusMinus) {
            final Impl _ref = compile(data, ((MinusMinus) object).ref);
            if (((MinusMinus) object).right) {
                return new Impl() {
                    @Override
                    public Referencable run(Global global, Scope scope) {
                        Referencable ref = _ref.run(global, scope);
                        net.nexustools.njs.Number.Instance val = global.Number.fromValueOf(ref.get());
                        ref.set(global.wrap(val.value - 1));
                        return new ValueReferenceable(val, rows, columns);
                    }
                };
            } else {
                return new Impl() {
                    @Override
                    public Referencable run(Global global, Scope scope) {
                        Referencable ref = _ref.run(global, scope);
                        net.nexustools.njs.Number.Instance val = global.Number.fromValueOf(ref.get());
                        ref.set(val = global.wrap(val.value - 1));
                        return new ValueReferenceable(val, rows, columns);
                    }
                };
            }
        } else if (object instanceof OpenGroup) {
            final Map<java.lang.String, Impl> compiled = new HashMap();
            for (Map.Entry<java.lang.String, Parsed> entry : ((OpenGroup) object).entries.entrySet()) {
                compiled.put(entry.getKey(), compile(data, entry.getValue()));
            }
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    GenericObject object = new GenericObject(global);
                    for (Map.Entry<java.lang.String, Impl> entry : compiled.entrySet()) {
                        object.setStorage(entry.getKey(), entry.getValue().run(global, scope).get(), true);
                    }
                    return new ValueReferenceable(object, rows, columns);
                }
            };
        } else if (object instanceof If) {
            final Impl condition = compile(data, ((If) object).condition);
            if (((If) object).simpleimpl != null) {
                final Impl impl = compile(data, ((If) object).simpleimpl);
                if (((If) object).el != null) {
                    Else el = ((If) object).el;
                    if (el instanceof ElseIf) {

                    } else {
                        if (el.simpleimpl != null) {
                            final Impl elimpl = compile(data, el.simpleimpl);
                            return new Impl() {
                                @Override
                                public Referencable run(Global global, Scope scope) {
                                    if (condition.run(global, scope).get().toBool()) {
                                        Referencable ref = impl.run(global, scope);
                                        if (ref instanceof Return) {
                                            return ref;
                                        }
                                        ref.get();
                                    } else {
                                        Referencable ref = elimpl.run(global, scope);
                                        if (ref instanceof Return) {
                                            return ref;
                                        }
                                        ref.get();
                                    }

                                    return UNDEFINED_REFERENCE;
                                }
                            };
                        }
                    }
                } else {
                    return new Impl() {
                        @Override
                        public Referencable run(Global global, Scope scope) {
                            Utilities.ReplacementStackTraceElement el = Utilities.renameCall(data.methodName, data.fileName, rows, columns);
                            try {
                                if (condition.run(global, scope).get().toBool()) {
                                    Referencable ref = impl.run(global, scope);
                                    if (ref instanceof Return) {
                                        return ref;
                                    }
                                    ref.get();
                                }
                            } finally {
                                el.finishCall();
                            }

                            return UNDEFINED_REFERENCE;
                        }
                    };
                }
            } else {
                final Script impl = compileScript(((If) object).impl, data.fileName, ScriptType.Block);
                if (((If) object).el != null) {
                    Else el = ((If) object).el;
                    if (el instanceof ElseIf) {

                    } else {
                        if (el.simpleimpl != null) {
                            final Impl elimpl = compile(data, el.simpleimpl);
                            return new Impl() {
                                @Override
                                public Referencable run(Global global, Scope scope) {
                                    if (condition.run(global, scope).get().toBool()) {
                                        BaseObject ret = impl.exec(global, scope);
                                        if (ret != null) {
                                            return new Return(ret, rows, columns);
                                        }
                                    } else {
                                        Referencable ref = elimpl.run(global, scope);
                                        if (ref instanceof Return) {
                                            return ref;
                                        }
                                        ref.get();
                                    }

                                    return UNDEFINED_REFERENCE;
                                }
                            };
                        }
                    }
                } else {
                    return new Impl() {
                        @Override
                        public Referencable run(Global global, Scope scope) {
                            if (condition.run(global, scope).get().toBool()) {
                                BaseObject ret = impl.exec(global, scope);
                                if (ret != null) {
                                    return new Return(ret, rows, columns);
                                }
                            }

                            return UNDEFINED_REFERENCE;
                        }
                    };
                }
            }
        } else if (object instanceof Boolean) {
            final boolean value = ((Boolean) object).value;
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    return new ValueReferenceable(global.wrap(value), rows, columns);
                }
            };
        } else if (object instanceof VariableReference) {
            final java.lang.String source = object.toString();
            final Impl lhs = compile(data, ((VariableReference) object).lhs);
            final Impl ref = compile(data, ((VariableReference) object).ref);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    return new ObjectKeyReferenceable(source, ref.run(global, scope).get(), lhs.run(global, scope).get(), rows, columns);
                }
            };
        } else if (object instanceof Delete) {
            final Impl ref = compile(data, ((Delete) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    return new ValueReferenceable(global.wrap(ref.run(global, scope).delete()), rows, columns);
                }
            };
        } else if (object instanceof IntegerReference) {
            final int integer = (int)((IntegerReference) object).ref;
            if (((IntegerReference) object).lhs == null) {
                return new Impl() {
                    @Override
                    public Referencable run(Global global, Scope scope) {
                        return new ValueReferenceable(new GenericArray(global, new BaseObject[]{global.wrap(integer)}), rows, columns);
                    }
                };
            }

            final java.lang.String source = object.toString();
            final Impl lhs = compile(data, ((IntegerReference) object).lhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    return new IntegerKeyReferenceable(source, integer, lhs.run(global, scope).get(), rows, columns);
                }
            };
        } else if (object instanceof TypeOf) {
            final Impl rhs = compile(data, ((TypeOf) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    return new ValueReferenceable(global.wrap(rhs.run(global, scope).get().typeOf()), rows, columns);
                }
            };
        } else if (object instanceof InstanceOf) {
            final Impl lhs = compile(data, ((InstanceOf) object).lhs);
            final Impl rhs = compile(data, ((InstanceOf) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    return new ValueReferenceable(global.wrap(lhs.run(global, scope).get().instanceOf((BaseFunction) rhs.run(global, scope).get())), rows, columns);
                }
            };
        } else if (object instanceof For) {
            switch(((For)object).type) {
                case InLoop:
                {
                    final Impl storage = compile(data, ((For) object).storage);
                    final Impl impl = compile(data, ((For) object).simpleimpl);
                    final java.lang.String key = ((Reference)((Var) ((For) object).init).sets.get(0).lhs).ref;
                    if (((For) object).init instanceof Let) {
                        return new Impl() {
                            @Override
                            public Referencable run(Global global, Scope scope) {
                                scope = scope.beginBlock();
                                Iterator<java.lang.String> it = storage.run(global, scope).get().deepPropertyNameIterator();
                                while (it.hasNext()) {
                                    scope.let(key, global.String.wrap(it.next()));
                                    Referencable ref = impl.run(global, scope);
                                    if (ref instanceof Return) {
                                        return ref;
                                    }
                                    ref.get();
                                }
                                return UNDEFINED_REFERENCE;
                            }
                        };
                    }
                    
                    return new Impl() {
                        @Override
                        public Referencable run(Global global, Scope scope) {
                            Iterator<java.lang.String> it = storage.run(global, scope).get().deepPropertyNameIterator();
                            while (it.hasNext()) {
                                scope.var(key, global.String.wrap(it.next()));
                                Referencable ref = impl.run(global, scope);
                                if (ref instanceof Return) {
                                    return ref;
                                }
                                ref.get();
                            }
                            return UNDEFINED_REFERENCE;
                        }
                    };
                }
                    
                case OfLoop:
                {
                    final Impl storage = compile(data, ((For) object).storage);
                    final Impl impl = compile(data, ((For) object).simpleimpl);
                    final java.lang.String key = ((Reference)((Var) ((For) object).init).sets.get(0).lhs).ref;
                    if (((For) object).init instanceof Let) {
                        return new Impl() {
                            @Override
                            public Referencable run(Global global, Scope scope) {
                                scope = scope.beginBlock();
                                for (BaseObject forObject : storage.run(global, scope).get()) {
                                    scope.let(key, forObject);
                                    Referencable ref = impl.run(global, scope);
                                    if (ref instanceof Return) {
                                        return ref;
                                    }
                                    ref.get();
                                }
                                return UNDEFINED_REFERENCE;
                            }
                        };
                    }
                    return new Impl() {
                        @Override
                        public Referencable run(Global global, Scope scope) {
                            for (BaseObject forObject : storage.run(global, scope).get()) {
                                scope.var(key, forObject);
                                Referencable ref = impl.run(global, scope);
                                if (ref instanceof Return) {
                                    return ref;
                                }
                                ref.get();
                            }
                            return UNDEFINED_REFERENCE;
                        }
                    };
                }
                    
                case Standard:
                    Parsed _init = ((For) object).init;
                    Parsed[] _loop = ((For) object).loop.toArray(new Parsed[((For) object).loop.size()]);
                    final Impl condition = ((For) object).condition == null ? null : compile(data, ((For) object).condition);
                    if (_init != null && _loop.length > 0) {
                        final Impl init = compile(data, _init);
                        final Impl[] loop = compile(data, _loop);

                        if (((For) object).simpleimpl != null) {
                            final Impl impl = compile(data, ((For) object).simpleimpl);
                            return new Impl() {
                                    @Override
                                    public Referencable run(Global global, Scope scope) {
                                        for (init.run(global, scope).get(); condition.run(global, scope).get().toBool(); execute(loop, global, scope)) {
                                            Referencable ref = impl.run(global, scope);
                                            if (ref instanceof Return) {
                                                return ref;
                                            }
                                            ref.get();
                                        }

                                        return UNDEFINED_REFERENCE;
                                    }
                                };

                        }
                        final Script impl = compileScript(((For) object).impl, data.fileName, ScriptType.Block);
                        return new Impl() {
                            @Override
                            public Referencable run(Global global, Scope scope) {
                                for (init.run(global, scope).get(); condition.run(global, scope).get().toBool(); execute(loop, global, scope)) {
                                    BaseObject ret = impl.exec(global, scope);
                                    if (ret != null) {
                                        return new Return(ret, rows, columns);
                                    }
                                }

                                return UNDEFINED_REFERENCE;
                            }
                        };
                    } else if (_loop.length > 0) {
                        final Impl[] loop = compile(data, _loop);

                        if (((For) object).simpleimpl != null) {
                            final Impl impl = compile(data, ((For) object).simpleimpl);
                            return new Impl() {
                                @Override
                                public Referencable run(Global global, Scope scope) {
                                    for (; condition.run(global, scope).get().toBool(); execute(loop, global, scope)) {
                                        Referencable ref = impl.run(global, scope);
                                        if (ref instanceof Return) {
                                            return ref;
                                        }
                                        ref.get();
                                    }

                                    return UNDEFINED_REFERENCE;
                                }
                            };
                        }
                        final Script impl = compileScript(((For) object).impl, data.fileName, ScriptType.Block);
                        return new Impl() {
                            @Override
                            public Referencable run(Global global, Scope scope) {
                                for (; condition.run(global, scope).get().toBool(); execute(loop, global, scope)) {
                                    BaseObject ret = impl.exec(global, scope);
                                    if (ret != null) {
                                        return new Return(ret, rows, columns);
                                    }
                                }

                                return UNDEFINED_REFERENCE;
                            }
                        };
                    } else if (_init != null) {
                        final Impl init = compile(data, _init);

                        if (((For) object).simpleimpl != null) {
                            final Impl impl = compile(data, ((For) object).simpleimpl);
                            return new Impl() {
                                @Override
                                public Referencable run(Global global, Scope scope) {
                                    for (init.run(global, scope).get(); condition.run(global, scope).get().toBool();) {
                                        Referencable ref = impl.run(global, scope);
                                        if (ref instanceof Return) {
                                            return ref;
                                        }
                                        ref.get();
                                    }

                                    return UNDEFINED_REFERENCE;
                                }
                            };
                        }
                        final Script impl = compileScript(((For) object).impl, data.fileName, ScriptType.Block);
                        return new Impl() {
                            @Override
                            public Referencable run(Global global, Scope scope) {
                                for (init.run(global, scope).get(); condition.run(global, scope).get().toBool();) {
                                    BaseObject ret = impl.exec(global, scope);
                                    if (ret != null) {
                                        return new Return(ret, rows, columns);
                                    }
                                }

                                return UNDEFINED_REFERENCE;
                            }
                        };
                    }

                    if (((For) object).simpleimpl != null) {
                        final Impl impl = compile(data, ((For) object).simpleimpl);
                        return new Impl() {
                            @Override
                            public Referencable run(Global global, Scope scope) {
                                for (; condition.run(global, scope).get().toBool();) {
                                    Referencable ref = impl.run(global, scope);
                                    if (ref instanceof Return) {
                                        return ref;
                                    }
                                    ref.get();
                                }

                                return UNDEFINED_REFERENCE;
                            }
                        };
                    }
                    final Script impl = compileScript(((For) object).impl, data.fileName, ScriptType.Block);
                    return new Impl() {
                        @Override
                        public Referencable run(Global global, Scope scope) {
                            for (; condition.run(global, scope).get().toBool();) {
                                BaseObject ret = impl.exec(global, scope);
                                if (ret != null) {
                                    return new Return(ret, rows, columns);
                                }
                            }

                            return UNDEFINED_REFERENCE;
                        }
                    };
            }
            
            
                    
        } else if (object instanceof Try) {
            Try t = (Try) object;
            final Script impl = compileScript(t.impl, data.fileName, ScriptType.Block);
            if (t.c != null && t.f != null) {
                final java.lang.String key = ((Reference) t.c.condition).ref;
                final Script cimpl = compileScript(t.c.impl, data.fileName, ScriptType.Block);
                final Script fimpl = compileScript(t.f.impl, data.fileName, ScriptType.Block);
                return new Impl() {
                    @Override
                    public Referencable run(Global global, Scope scope) {
                        try {
                            BaseObject ret = impl.exec(global, scope);
                            if (ret != null) {
                                return new Return(ret, rows, columns);
                            }
                        } catch (Throwable t) {
                            if (t instanceof net.nexustools.njs.Error.InvisibleException) {
                                throw (net.nexustools.njs.Error.InvisibleException) t;
                            }

                            Scope catchScope = scope.beginBlock();
                            catchScope.let(key, global.wrap(t));
                            BaseObject ret = cimpl.exec(global, catchScope);
                            if (ret != null) {
                                return new Return(ret, rows, columns);
                            }
                        } finally {
                            BaseObject ret = fimpl.exec(global, scope);
                            if (ret != null) {
                                return new Return(ret, rows, columns);
                            }
                        }

                        return UNDEFINED_REFERENCE;
                    }
                };
            } else if (t.c != null) {
                final java.lang.String key = ((Reference) t.c.condition).ref;
                final Script cimpl = compileScript(t.c.impl, data.fileName, ScriptType.Block);
                return new Impl() {
                    @Override
                    public Referencable run(Global global, Scope scope) {
                        try {
                            BaseObject ret = impl.exec(global, scope);
                            if (ret != null) {
                                return new Return(ret, rows, columns);
                            }
                        } catch (Throwable t) {
                            if (t instanceof net.nexustools.njs.Error.InvisibleException) {
                                throw (net.nexustools.njs.Error.InvisibleException) t;
                            }

                            Scope catchScope = scope.beginBlock();
                            catchScope.let(key, global.wrap(t));
                            BaseObject ret = cimpl.exec(global, catchScope);
                            if (ret != null) {
                                return new Return(ret, rows, columns);
                            }
                        }

                        return UNDEFINED_REFERENCE;
                    }
                };
            } else if (t.f != null) {
                final Script fimpl = compileScript(t.f.impl, data.fileName, ScriptType.Block);
                return new Impl() {
                    @Override
                    public Referencable run(Global global, Scope scope) {
                        try {
                            BaseObject ret = impl.exec(global, scope);
                            if (ret != null) {
                                return new Return(ret, rows, columns);
                            }
                        } finally {
                            BaseObject ret = fimpl.exec(global, scope);
                            if (ret != null) {
                                return new Return(ret, rows, columns);
                            }
                        }

                        return UNDEFINED_REFERENCE;
                    }
                };
            }
        } else if (object instanceof Throw) {
            final Impl rhs = compile(data, ((Throw) object).rhs);
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    throw new net.nexustools.njs.Error.Thrown(rhs.run(global, scope).get());
                }
            };
        } else if (object instanceof While) {
            final Impl condition = compile(data, ((While) object).condition);
            if (((While) object).simpleimpl != null) {
                final Impl impl = compile(data, ((While) object).simpleimpl);
                return new Impl() {
                    @Override
                    public Referencable run(Global global, Scope scope) {
                        while (condition.run(global, scope).get().toBool()) {
                            Referencable ret = impl.run(global, scope);
                            if (ret instanceof Return) {
                                return ret;
                            }
                            ret.get();
                        }

                        return UNDEFINED_REFERENCE;
                    }
                };
            } else {
                final Script impl = compileScript(((While) object).impl, data.fileName, ScriptType.Block);
                return new Impl() {
                    @Override
                    public Referencable run(Global global, Scope scope) {
                        while (condition.run(global, scope).get().toBool()) {
                            BaseObject ret = impl.exec(global, scope);
                            if (ret != null) {
                                return new Return(ret, rows, columns);
                            }
                        }

                        return UNDEFINED_REFERENCE;
                    }
                };
            }
        } else if (object instanceof RegEx) {
            final java.lang.String pattern = ((RegEx) object).pattern;
            final java.lang.String flags = ((RegEx) object).flags;
            return new Impl() {
                @Override
                public Referencable run(Global global, Scope scope) {
                    return new ValueReferenceable(global.RegEx.create(pattern, flags), rows, columns);
                }
            };
        } else if (object instanceof Undefined) {
            return UNDEFINED;
        } else if (object instanceof Null) {
            return NULL;
        }

        throw new UnsupportedOperationException("Cannot compile: " + object + " (" + object.getClass().getSimpleName() + ')');
    }

    private static enum ScriptType {
        Global,
        Function,
        Block
    }

    @Override
    protected Script compileScript(ScriptData script, java.lang.String fileName, boolean inFunction) {
        return compileScript(script, fileName, inFunction ? ScriptType.Function : ScriptType.Global);
    }

    private Script compileScript(final ScriptData script, java.lang.String fileName, final ScriptType scriptType) {
        if (script.impl.length == 0) {
            return new Script() {
                @Override
                public BaseObject exec(Global global, Scope scope) {
                    return net.nexustools.njs.Undefined.INSTANCE;
                }

                @Override
                public java.lang.String toString() {
                    return join(Arrays.asList(script.impl), ';');
                }

                @Override
                public java.lang.String source() {
                    return script.source;
                }

                @Override
                public BaseObject exec(Global global) {
                    return exec(global, null);
                }
            };
        }

        final int rows = script.rows;
        final int columns = script.columns;
        if (scriptType != ScriptType.Global) {
            final ScriptCompilerData precompiled = precompile(script, fileName);
            final Impl[] parts = new Impl[script.impl.length];
            for (int i = 0; i < parts.length; i++) {
                parts[i] = compile(precompiled, script.impl[i]);
            }
            final int max = parts.length;
            return new Script() {
                @Override
                public BaseObject exec(Global global, Scope scope) {
                    if (scope == null) {
                        scope = new Scope(global);
                    }

                    scope.enter();
                    Utilities.ReplacementStackTraceElement el = Utilities.renameCall(precompiled.methodName, precompiled.fileName, rows, columns);
                    try {
                        precompiled.exec(global, scope);
                        for (int i = 0; i < max; i++) {
                            Referencable ref = parts[i].run(global, scope);
                            el.columns = Math.max(el.columns, ref.columns());
                            el.rows = Math.max(el.rows, ref.rows());
                            if (rows > 0) {
                                el.rows = rows;
                            }
                            if (ref instanceof Return) {
                                return ref.get();
                            }
                            ref.get();
                        }
                        return scriptType == ScriptType.Block ? null : net.nexustools.njs.Undefined.INSTANCE;
                    } finally {
                        el.finishCall();
                        scope.exit();
                    }
                }

                @Override
                public java.lang.String toString() {
                    return join(Arrays.asList(script.impl), ';');
                }

                @Override
                public java.lang.String source() {
                    return script.source;
                }

                @Override
                public BaseObject exec(Global global) {
                    return exec(global, null);
                }
            };
        }

        if (script.impl.length == 1) {
            final ScriptCompilerData precompiled = precompile(script, fileName);
            final Impl impl = compile(precompiled, script.impl[0]);
            return new Script() {
                @Override
                public BaseObject exec(Global global, Scope scope) {
                    if (scope == null) {
                        scope = new Scope(global);
                    }
                    scope.enter();
                    Utilities.ReplacementStackTraceElement el = Utilities.renameCall(precompiled.methodName, precompiled.fileName, rows, columns);
                    try {
                        precompiled.exec(global, scope);
                        return impl.run(global, scope).get();
                    } finally {
                        el.finishCall();
                        scope.exit();
                    }
                }

                @Override
                public java.lang.String toString() {
                    return join(Arrays.asList(script.impl), ';');
                }

                @Override
                public java.lang.String source() {
                    return script.source;
                }

                @Override
                public BaseObject exec(Global global) {
                    return exec(global, null);
                }
            };
        }

        final ScriptCompilerData precompiled = precompile(script, fileName);
        final Impl[] parts = new Impl[script.impl.length];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = compile(precompiled, script.impl[i]);
        }
        switch (parts.length) {
            case 2:
                return new Script() {
                    @Override
                    public BaseObject exec(Global global, Scope scope) {
                        if (scope == null) {
                            scope = new Scope(global);
                        }

                        scope.enter();
                        Utilities.ReplacementStackTraceElement el = Utilities.renameCall(precompiled.methodName, precompiled.fileName, rows, columns);
                        try {
                            precompiled.exec(global, scope);
                            parts[0].run(global, scope);
                            Referencable ref = parts[1].run(global, scope);
                            el.columns = Math.max(el.columns, ref.columns());
                            el.rows = Math.max(el.rows, ref.rows());
                            ref.get();
                            return ref.get();
                        } finally {
                            el.finishCall();
                            scope.exit();
                        }
                    }

                    @Override
                    public java.lang.String toString() {
                        return join(Arrays.asList(script.impl), ';');
                    }

                    @Override
                    public java.lang.String source() {
                        return script.source;
                    }

                    @Override
                    public BaseObject exec(Global global) {
                        return exec(global, null);
                    }
                };

            case 3:
                return new Script() {
                    @Override
                    public BaseObject exec(Global global, Scope scope) {
                        if (scope == null) {
                            scope = new Scope(global);
                        }

                        scope.enter();
                        Utilities.ReplacementStackTraceElement el = Utilities.renameCall(precompiled.methodName, precompiled.fileName, rows, columns);
                        try {
                            precompiled.exec(global, scope);
                            parts[0].run(global, scope).get();
                            Referencable ref = parts[1].run(global, scope);
                            el.columns = Math.max(el.columns, ref.columns());
                            el.rows = Math.max(el.rows, ref.rows());
                            ref.get();
                            ref = parts[2].run(global, scope);
                            el.columns = Math.max(el.columns, ref.columns());
                            el.rows = Math.max(el.rows, ref.rows());
                            return ref.get();
                        } finally {
                            el.finishCall();
                            scope.exit();
                        }
                    }

                    @Override
                    public java.lang.String toString() {
                        return join(Arrays.asList(script.impl), ';');
                    }

                    @Override
                    public java.lang.String source() {
                        return script.source;
                    }

                    @Override
                    public BaseObject exec(Global global) {
                        return exec(global, null);
                    }
                };

            default:
                final int max = parts.length;
                return new Script() {
                    @Override
                    public BaseObject exec(Global global, Scope scope) {
                        BaseObject lastValue = null;
                        if (scope == null) {
                            scope = new Scope(global);
                        }

                        scope.enter();
                        Utilities.ReplacementStackTraceElement el = Utilities.renameCall(precompiled.methodName, precompiled.fileName, rows, columns);
                        try {
                            Referencable ref;
                            precompiled.exec(global, scope);
                            for (int i = 0; i < max; i++) {
                                ref = parts[i].run(global, scope);
                                el.columns = Math.max(el.columns, ref.columns());
                                el.rows = Math.max(el.rows, ref.rows());
                                lastValue = ref.get();
                            }
                            return lastValue;
                        } finally {
                            el.finishCall();
                            scope.exit();
                        }
                    }

                    @Override
                    public java.lang.String toString() {
                        return join(Arrays.asList(script.impl), ';');
                    }

                    @Override
                    public java.lang.String source() {
                        return script.source;
                    }

                    @Override
                    public BaseObject exec(Global global) {
                        return exec(global, null);
                    }
                };
        }
    }
}
