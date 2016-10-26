/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
 * @author kate
 */
public class JavaConstructor extends AbstractFunction {
	
	public final Global global;
	public final Class<?> javaClass;
	public final java.lang.String javaClassString;
	private final Map<Integer, List<Constructor>> constructors = new HashMap();
	JavaConstructor(final Global global, final Class<?> javaClass) {
		super(global);
		assert(javaClass != null);
		this.javaClass = javaClass;
		javaClassString = javaClass.getName().replace(".", "_");
		assert(javaClassString != null);
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
		
		GenericObject prototype = prototype();
		if(superClass != null) {
			JavaConstructor superConstructor = global.wrap(superClass);
			prototype.setHidden("__proto__", superConstructor.prototype());
			prototype.setHidden("constructor", superConstructor);
		}
		
		Map<java.lang.String, List<Method>> methods = new HashMap();
		for(final Method method : javaClass.getDeclaredMethods()) {
			if((method.getModifiers() & Modifier.STATIC) != 0)
				continue;
			
			List<Method> meths = methods.get(method.getName());
			if(meths == null)
				methods.put(method.getName(), meths = new ArrayList());
			meths.add(method);
		}
		
		for(Map.Entry<java.lang.String, List<Method>> entry : methods.entrySet()) {
			final Map<Integer, List<Method>> byLength = new HashMap();
			for(Method method : entry.getValue()) {
				List<Method> meths = byLength.get(method.getParameterCount());
				if(meths == null)
					byLength.put(method.getParameterCount(), meths = new ArrayList());
				meths.add(method);
			}
			
			final java.lang.String toStringName = javaClassString + "_prototype_" + entry.getKey();
			prototype.setHidden(entry.getKey(), new AbstractFunction(global) {
				@Override
				public BaseObject call(BaseObject _this, BaseObject... params) {
					if(params.length == 0) {
						try {
							return global.javaToJS(byLength.get(0).get(0).invoke(((JavaObjectWrapper)_this).javaObject));
						} catch (IllegalAccessException ex) {
							throw new Error.JavaException("JavaError", ex.toString(), ex);
						} catch (IllegalArgumentException ex) {
							throw new Error.JavaException("JavaError", ex.toString(), ex);
						} catch (InvocationTargetException ex) {
							throw new Error.JavaException("JavaError", ex.toString(), ex);
						}
					}
					java.lang.Object[] converted = new java.lang.Object[params.length];
					for(Method method : byLength.get(params.length)) {
						again:
						while(true) {
							Class[] types = method.getParameterTypes();
							for(int i=0; i<params.length; i++)
								try {
									converted[i] = JSHelper.jsToJava(params[i], types[i]);
								} catch(ClassCastException ex) {
									break again;
								}
							try {
								return global.wrap(method.invoke(((JavaObjectWrapper)_this).javaObject, converted));
							} catch (IllegalAccessException ex) {
								throw new Error.JavaException("JavaError", ex.toString(), ex);
							} catch (IllegalArgumentException ex) {
								throw new Error.JavaException("JavaError", ex.toString(), ex);
							} catch (InvocationTargetException ex) {
								throw new Error.JavaException("JavaError", ex.toString(), ex);
							}
						}
					}
					throw new UnsupportedOperationException();
				}
				@Override
				public java.lang.String name() {
					return toStringName;
				}
			});
		}
		
		methods.clear();
		for(final Method method : javaClass.getDeclaredMethods()) {
			if((method.getModifiers() & Modifier.STATIC) == 0)
				continue;
			
			List<Method> meths = methods.get(method.getName());
			if(meths == null)
				methods.put(method.getName(), meths = new ArrayList());
			meths.add(method);
		}
		
		for(Map.Entry<java.lang.String, List<Method>> entry : methods.entrySet()) {
			final Map<Integer, List<Method>> byLength = new HashMap();
			for(Method method : entry.getValue()) {
				List<Method> meths = byLength.get(method.getParameterCount());
				if(meths == null)
					byLength.put(method.getParameterCount(), meths = new ArrayList());
				meths.add(method);
			}
			
			final java.lang.String toStringName = javaClassString + "_" + entry.getKey();
			setHidden(entry.getKey(), new AbstractFunction(global) {
				@Override
				public BaseObject call(BaseObject _this, BaseObject... params) {
					if(params.length == 0)
						try {
							return global.javaToJS(byLength.get(0).get(0).invoke(null));
						} catch (IllegalAccessException ex) {
							throw new Error.JavaException("JavaError", ex.toString(), ex);
						} catch (IllegalArgumentException ex) {
							throw new Error.JavaException("JavaError", ex.toString(), ex);
						} catch (InvocationTargetException ex) {
							throw new Error.JavaException("JavaError", ex.toString(), ex);
						}
					java.lang.Object[] converted = new java.lang.Object[params.length];
					List<Method> methods = byLength.get(params.length);
					if(methods != null)
						for(Method method : methods) {
							again:
							while(true) {
								Class[] types = method.getParameterTypes();
								for(int i=0; i<params.length; i++)
									try {
										converted[i] = JSHelper.jsToJava(params[i], types[i]);
									} catch(ClassCastException ex) {
										break again;
									}
								try {
									return global.wrap(method.invoke(null, converted));
								} catch (IllegalAccessException ex) {
									throw new Error.JavaException("JavaError", ex.toString(), ex);
								} catch (IllegalArgumentException ex) {
									throw new Error.JavaException("JavaError", ex.toString(), ex);
								} catch (InvocationTargetException ex) {
									throw new Error.JavaException("JavaError", ex.toString(), ex);
								}
							}
						}
					throw new Error.JavaException("ArgumentError", "Invalid arguments");
				}
				@Override
				public java.lang.String name() {
					return toStringName;
				}
			});
		}
		
		for(Constructor constructor : javaClass.getDeclaredConstructors()) {
			List<Constructor> cons = constructors.get(constructor.getParameterCount());
			if(cons == null)
				constructors.put(constructor.getParameterCount(), cons = new ArrayList());
			cons.add(constructor);
		}
		
		for(final Field field : javaClass.getDeclaredFields()) {
			if((field.getModifiers() & Modifier.STATIC) == 0)
				continue;
			
			defineProperty(field.getName(), new AbstractFunction(global) {
				@Override
				public BaseObject call(BaseObject _this, BaseObject... params) {
					try {
						return global.javaToJS(field.get(null));
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
						field.set(null, JSHelper.jsToJava(params[0], field.getType()));
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

	@Override
	public BaseObject construct(BaseObject... params) {
		java.lang.Object[] converted = new java.lang.Object[params.length];
		for(Constructor constructor : constructors.get(params.length)) {
			next:
			while(true) {
				Class[] types = constructor.getParameterTypes();
				for(int i=0; i<params.length; i++)
					try {
						converted[i] = JSHelper.jsToJava(params[i], types[i]);
					} catch(ClassCastException ex) {
						break next;
					}
				try {
					return global.wrap(constructor.newInstance(converted));
				} catch (InstantiationException ex) {
					throw new Error.JavaException("JavaError", ex.toString(), ex);
				} catch (IllegalAccessException ex) {
					throw new Error.JavaException("JavaError", ex.toString(), ex);
				} catch (IllegalArgumentException ex) {
					throw new Error.JavaException("JavaError", ex.toString(), ex);
				} catch (InvocationTargetException ex) {
					throw new Error.JavaException("JavaError", ex.toString(), ex);
				}
			}
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public BaseObject call(BaseObject _this, BaseObject... params) {
		throw new Error.JavaException("Error", "You must call new on Java Classes");
	}

	@Override
	public java.lang.String name() {
		return javaClassString;
	}
	
}
