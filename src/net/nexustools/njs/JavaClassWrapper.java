/* 
 * Copyright (C) 2017 NexusTools.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Katelyn Slater <kate@nexustools.com>
 */
public class JavaClassWrapper extends AbstractFunction {

    public static final boolean DEBUG = System.getProperties().containsKey("NJSWRAPDEBUG");

    public final Global global;
    public final Class<?> javaClass;
    public final java.lang.String javaClassString;
    private final Map<Integer, List<Constructor>> constructors = new HashMap();

    JavaClassWrapper(final Global global, final JavaClass JavaClass, final Class<?> javaClass) {
        super(JavaClass, global);
        assert (javaClass != null);
        this.javaClass = javaClass;
        javaClassString = javaClass.getName().replace(".", "_");
        assert (javaClassString != null);
        Class<?> superClass = javaClass.getSuperclass();
        this.global = global;

        setStorage("name", global.wrap(javaClassString), false);
        setHidden("toString", new AbstractFunction(global) {
            @Override
            public BaseObject call(BaseObject _this, BaseObject... params) {
                return global.wrap(javaClass.toString());
            }

            @Override
            public java.lang.String name() {
                return javaClassString + "_toString";
            }
        });

        Map<java.lang.String, List<Method>> methods = new HashMap();
        for (final Method method : javaClass.getDeclaredMethods()) {
            if ((method.getModifiers() & Modifier.STATIC) == 0) {
                continue;
            }

            method.setAccessible(true);
            List<Method> meths = methods.get(method.getName());
            if (meths == null) {
                methods.put(method.getName(), meths = new ArrayList());
            }
            meths.add(method);
        }

        for (Map.Entry<java.lang.String, List<Method>> entry : methods.entrySet()) {
            final Map<Integer, List<Method>> byLength = new HashMap();
            for (Method method : entry.getValue()) {
                List<Method> meths = byLength.get(method.getParameterCount());
                if (meths == null) {
                    byLength.put(method.getParameterCount(), meths = new ArrayList());
                }
                meths.add(method);
            }

            final java.lang.String toStringName = javaClassString + "_prototype_" + entry.getKey();
            setHidden(entry.getKey(), new AbstractFunction(global) {
                @Override
                public BaseObject call(BaseObject _this, BaseObject... params) {
                    return callBestMethod(null, params, byLength);
                }

                @Override
                public java.lang.String name() {
                    return toStringName;
                }
            });
        }

        for (Constructor constructor : javaClass.getDeclaredConstructors()) {
            constructor.setAccessible(true);
            List<Constructor> cons = constructors.get(constructor.getParameterCount());
            if (cons == null) {
                constructors.put(constructor.getParameterCount(), cons = new ArrayList());
            }
            cons.add(constructor);
        }

        GenericObject prototype = (GenericObject) prototype();
        if (superClass != null) {
            prototype.__proto__ = global.wrap(superClass).prototype();
        }

        methods.clear();
        for (final Method method : javaClass.getMethods()) {
            if ((method.getModifiers() & Modifier.STATIC) != 0) {
                continue;
            }

            method.setAccessible(true);
            List<Method> meths = methods.get(method.getName());
            if (meths == null) {
                methods.put(method.getName(), meths = new ArrayList());
            }
            meths.add(method);
        }

        for (Map.Entry<java.lang.String, List<Method>> entry : methods.entrySet()) {
            final Map<Integer, List<Method>> byLength = new HashMap();
            for (Method method : entry.getValue()) {
                List<Method> meths = byLength.get(method.getParameterCount());
                if (meths == null) {
                    byLength.put(method.getParameterCount(), meths = new ArrayList());
                }
                meths.add(method);
            }

            final java.lang.String toStringName = javaClassString + "_" + entry.getKey();
            prototype.setHidden(entry.getKey(), new AbstractFunction(global) {
                @Override
                public BaseObject call(BaseObject _this, BaseObject... params) {
                    return callBestMethod(_this, params, byLength);
                }

                @Override
                public java.lang.String name() {
                    return toStringName;
                }
            });
        }

        for (final Field field : javaClass.getDeclaredFields()) {
            if ((field.getModifiers() & Modifier.STATIC) != 0) {
                continue;
            }

            field.setAccessible(true);
            prototype.defineProperty(field.getName(), new AbstractFunction(global) {
                @Override
                public BaseObject call(BaseObject _this, BaseObject... params) {
                    try {
                        return Utilities.javaToJS(global, field.get(JavaObjectWrapper.unwrap(global, _this)), field.getType());
                    } catch (IllegalArgumentException ex) {
                        throw new Error.JavaException("JavaError", ex.toString(), ex);
                    } catch (IllegalAccessException ex) {
                        throw new Error.JavaException("JavaError", ex.toString(), ex);
                    }
                }
            }, new AbstractFunction(global) {
                @Override
                public BaseObject call(BaseObject _this, BaseObject... params) {
                    try {
                        field.set(JavaObjectWrapper.unwrap(global, _this), Utilities.jsToJava(global, params[0], field.getType()));
                        return Undefined.INSTANCE;
                    } catch (IllegalArgumentException ex) {
                        throw new Error.JavaException("JavaError", ex.toString(), ex);
                    } catch (IllegalAccessException ex) {
                        throw new Error.JavaException("JavaError", ex.toString(), ex);
                    }
                }
            });
        }

        for (final Field field : javaClass.getDeclaredFields()) {
            if ((field.getModifiers() & Modifier.STATIC) == 0) {
                continue;
            }

            field.setAccessible(true);
            defineProperty(field.getName(), new AbstractFunction(global) {
                @Override
                public BaseObject call(BaseObject _this, BaseObject... params) {
                    try {
                        return Utilities.javaToJS(global, field.get(null), field.getType());
                    } catch (IllegalArgumentException ex) {
                        throw new Error.JavaException("JavaError", ex.toString(), ex);
                    } catch (IllegalAccessException ex) {
                        throw new Error.JavaException("JavaError", ex.toString(), ex);
                    }
                }
            }, new AbstractFunction(global) {
                @Override
                public BaseObject call(BaseObject _this, BaseObject... params) {
                    try {
                        field.set(null, Utilities.jsToJava(global, params[0], field.getType()));
                        return Undefined.INSTANCE;
                    } catch (IllegalArgumentException ex) {
                        throw new Error.JavaException("JavaError", ex.toString(), ex);
                    } catch (IllegalAccessException ex) {
                        throw new Error.JavaException("JavaError", ex.toString(), ex);
                    }
                }
            });
        }
    }

    private static final java.lang.Object[] DEFAULT_PARAMS = new java.lang.Object[0];

    @Override
    public BaseObject construct(BaseObject... params) {
        return Utilities.javaToJS(global, construct0(params), javaClass);
    }
    
    private java.lang.Object construct0(BaseObject... params) {
        java.lang.Object[] bestConversion = null;
        Constructor bestConstructor = null;

        if (params.length > 0) {
            double bestAccuracy = 0;
            Utilities.ConversionAccuracy convertAccuracy = new Utilities.ConversionAccuracy();
            java.lang.Object[] converted = new java.lang.Object[params.length];
            for (Constructor constructor : constructors.get(params.length)) {
                Class[] types = constructor.getParameterTypes();
                if (DEBUG) {
                    System.out.println(constructor);
                }
                while (true) {
                    float conversionAccuracy = 0;
                    for (int i = 0; i < params.length; i++) {
                        try {
                            convertAccuracy.accuracy = 0;
                            converted[i] = Utilities.jsToJava(global, params[i], types[i], convertAccuracy);
                            if (DEBUG)
                                System.out.println(types[i] + ": " + convertAccuracy.accuracy);
                            if(convertAccuracy.accuracy <= 0)
                                throw new UnsupportedOperationException();
                            conversionAccuracy += convertAccuracy.accuracy;
                        } catch (UnsupportedOperationException ex) {
                            conversionAccuracy = 0;
                            break;
                        } catch (NumberFormatException ex) {
                            conversionAccuracy = 0;
                            break;
                        }
                    }

                    if (conversionAccuracy > bestAccuracy) {
                        bestConstructor = constructor;
                        bestConversion = converted;
                        bestAccuracy = conversionAccuracy;
                        converted = new java.lang.Object[params.length];
                    }
                    break;
                }
            }
        } else {
            bestConversion = DEFAULT_PARAMS;
            List<Constructor> noParams = constructors.get(0);
            bestConstructor = noParams.isEmpty() ? null : noParams.get(0);
        }

        if (bestConstructor != null) {
            try {
                return bestConstructor.newInstance(bestConversion);
            } catch (IllegalAccessException ex) {
                throw new Error.JavaException("JavaError", "Illegal access", ex);
            } catch (InstantiationException ex) {
                throw new Error.JavaException("JavaError", ex.toString());
            } catch (IllegalArgumentException ex) {
                throw new Error.JavaException("JavaError", "Illegal arguments", ex);
            } catch (InvocationTargetException ex) {
                Throwable target = ex.getTargetException();
                if (target instanceof RuntimeException) {
                    throw (RuntimeException) target;
                }
                if (target instanceof java.lang.Error) {
                    throw (java.lang.Error) target;
                }
                throw new RuntimeException(ex);
            }
        }
        throw new Error.JavaException("JavaError", "Incompatible arguments");
    }

    private BaseObject callBestMethod(BaseObject _this, BaseObject[] params, Map<Integer, List<Method>> byLength) {
        java.lang.Object __this = _this == null ? null : JavaObjectWrapper.unwrap(global, _this);

        Method bestMethod = null;
        java.lang.Object[] bestConversion = null;
        if (params.length > 0) {
            double bestAccuracy = 0;
            Utilities.ConversionAccuracy convertAccuracy = new Utilities.ConversionAccuracy();
            java.lang.Object[] converted = new java.lang.Object[params.length];
            for (Method method : byLength.get(params.length)) {
                Class[] types = method.getParameterTypes();
                if(types.length != params.length)
                    continue;
                if (DEBUG) {
                    System.out.println(method);
                }
                while (true) {
                    float conversionAccuracy = 0;
                    for (int i = 0; i < params.length; i++) {
                        try {
                            convertAccuracy.accuracy = 0;
                            converted[i] = Utilities.jsToJava(global, params[i], types[i], convertAccuracy);
                            if(DEBUG)
                                System.out.println(converted[i] + ": " + convertAccuracy.accuracy);
                            if(convertAccuracy.accuracy <= 0)
                                throw new UnsupportedOperationException();
                            conversionAccuracy += convertAccuracy.accuracy;
                        } catch (UnsupportedOperationException ex) {
                            conversionAccuracy = 0;
                            break;
                        } catch (NumberFormatException ex) {
                            conversionAccuracy = 0;
                            break;
                        }
                    }

                    if (conversionAccuracy > bestAccuracy) {
                        bestMethod = method;
                        bestConversion = converted;
                        bestAccuracy = conversionAccuracy;
                        converted = new java.lang.Object[params.length];
                    }
                    break;
                }
            }
        } else {
            bestConversion = DEFAULT_PARAMS;
            List<Method> noParams = byLength.get(0);
            bestMethod = noParams.isEmpty() ? null : noParams.get(0);
        }

        if (bestMethod == null)
            throw new Error.JavaException("JavaError", "Incompatible arguments");
        
        try {
            return Utilities.javaToJS(global, bestMethod.invoke(__this, bestConversion));
        } catch (IllegalAccessException ex) {
            throw new Error.JavaException("JavaError", "Illegal access", ex);
        } catch (IllegalArgumentException ex) {
            throw new Error.JavaException("JavaError", "Illegal arguments", ex);
        } catch (InvocationTargetException ex) {
            Throwable target = ex.getTargetException();
            if (target instanceof RuntimeException) {
                throw (RuntimeException) target;
            }
            if (target instanceof java.lang.Error) {
                throw (java.lang.Error) target;
            }
            throw new RuntimeException(ex);
        }
    }

    @Override
    public BaseObject call(BaseObject _this, BaseObject... params) {
        _this.set(global.java_object, new JavaObjectHolder(construct0(params)));
        return _this;
    }

    @Override
    public java.lang.String name() {
        return javaClassString;
    }

    @Override
    public java.lang.String typeOf() {
        return "class";
    }

}
