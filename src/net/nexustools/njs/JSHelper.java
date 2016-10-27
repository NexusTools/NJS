/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author kate
 */
public class JSHelper {
	public static <O> O jsToJava(final BaseObject jsObject, Class<O> desiredClass) {
		if(jsObject instanceof Undefined || jsObject instanceof Null)
			return null;
		
		if(desiredClass == String.class)
			return (O)jsObject.toString();
		
		if(desiredClass == Runnable.class && jsObject instanceof BaseFunction)
			return (O)new Runnable() {
				@Override
				public void run() {
					((BaseFunction)jsObject).call(Undefined.INSTANCE);
				}
			};
		
		if(jsObject instanceof Number.Instance) {
			if(jsObject instanceof Number.Instance)
				return desiredClass.cast((Double)((Number.Instance)jsObject).number);
			return desiredClass.cast(jsObject.toString());
		}
		
		if(Number.class.isAssignableFrom(desiredClass))
			return desiredClass.cast(Double.valueOf(jsObject.toString()));
		
		if(jsObject instanceof JavaObjectWrapper)
			return desiredClass.cast(((JavaObjectWrapper)jsObject).javaObject);
		if(jsObject instanceof String.Instance)
			return desiredClass.cast(((String.Instance)jsObject).string);
		if(jsObject instanceof AbstractArray)
			return desiredClass.cast(((AbstractArray)jsObject).arrayStorage);
		
		return desiredClass.cast(jsObject);
	}
	public static BaseObject javaToJS(Global global, java.lang.Object javaObject) {
		if(javaObject == null)
			return Null.INSTANCE;
		
		if(javaObject instanceof BaseObject)
			return (BaseObject)javaObject;
		
		if(javaObject instanceof java.lang.String)
			return global.wrap(((java.lang.String)javaObject));
		
		if(javaObject instanceof java.lang.Number)
			return global.wrap(((java.lang.Number)javaObject).doubleValue());

		if(javaObject instanceof Class)
			return global.wrap((Class)javaObject);

		Class<?> javaClass = javaObject.getClass();
		if(javaClass.isArray()) {
			if(javaClass.getComponentType() == Byte.TYPE)
				return new Uint8Array.Instance(global, (Uint8Array)global.get("Uint8Array"), (byte[])javaObject);
			if(javaClass.getComponentType() == Short.TYPE)
				return new Uint16Array.Instance(global, (Uint16Array)global.get("Uint16Array"), (short[])javaObject);
			if(javaClass.getComponentType() == Integer.TYPE)
				return new Uint32Array.Instance(global, (Uint32Array)global.get("Uint32Array"), (int[])javaObject);
			if(javaClass.getComponentType() == Double.TYPE)
				return new Float64Array.Instance(global, (Float64Array)global.get("Float64Array"), (double[])javaObject);

			throw new RuntimeException("Cannot convert array of " + javaClass.getComponentType() + " to JSBaseObject");
		} else
			return global.wrap(javaObject);
	}
	public static Global createStandardGlobal() {
		return createGlobal("eval", "Math", "Date", "JSON", "Uint8Array", "Uint8ClampedArray", "Int8Array", "Uint16Array", "Int16Array", "Uint32Array", "Int32Array", "Float64Array");
	}
	public static Global createExtendedGlobal() {
		return createGlobal("eval", "Math", "Date", "JSON", "Uint8Array", "Uint8ClampedArray", "Int8Array", "Uint16Array", "Int16Array", "Uint32Array", "Int32Array", "Float64Array", "importClass");
	}
	public static Global createGlobal(java.lang.String... standards) {
		final Global global = new Global();
		global.initStandards();
		for(java.lang.String standard : standards) {
			if(standard.equals("JSON"))
				global.setHidden("JSON", new JSON(global));
			else if(standard.equals("Math"))
				global.setHidden("Math", new Math(global));
			else if(standard.equals("Date"))
				global.setHidden("Date", new Date(global));
			else if(standard.equals("Uint8Array"))
				global.setHidden("Uint8Array", new Uint8Array(global));
			else if(standard.equals("Uint8ClampedArray"))
				global.setHidden("Uint8ClampedArray", new Uint8ClampedArray(global));
			else if(standard.equals("Int8Array"))
				global.setHidden("Int8Array", new Int8Array(global));
			else if(standard.equals("Uint16Array"))
				global.setHidden("Uint16Array", new Uint16Array(global));
			else if(standard.equals("Int16Array"))
				global.setHidden("Int16Array", new Int16Array(global));
			else if(standard.equals("Uint32Array"))
				global.setHidden("Uint32Array", new Uint32Array(global));
			else if(standard.equals("Int32Array"))
				global.setHidden("Int32Array", new Int32Array(global));
			else if(standard.equals("Float64Array"))
				global.setHidden("Float64Array", new Float64Array(global));
			else if(standard.equals("eval"))
				global.setHidden("eval", new AbstractFunction(global) {
					@Override
					public BaseObject call(BaseObject _this, BaseObject... params) {
						return global.compiler.eval(params[0].toString(), "eval", false).exec(global, Scope.getCurrent());
					}
					@Override
					public java.lang.String name() {
						return "eval";
					}
				});
			else if(standard.equals("importClass"))
				global.setHidden("importClass", new AbstractFunction(global) {
					@Override
					public BaseObject call(BaseObject _this, BaseObject... params) {
						try {
							return global.wrap(Class.forName(params[0].toString()));
						} catch (ClassNotFoundException ex) {
							throw new Error.JavaException("JavaError", ex.toString(), ex);
						}
					}
					@Override
					public java.lang.String name() {
						return "importClass";
					}
				});
			else
				throw new RuntimeException("Unknown Standard Requested: " + standard);
		}
		return global;
	}

	public static boolean isUndefined(BaseObject object) {
		return object == null || object == Undefined.INSTANCE || object == Null.INSTANCE;
	}
	
	public static BaseObject get(BaseObject _this, BaseObject key) {
		if(key instanceof String.Instance)
			return _this.get(((String.Instance) key).string);
		else if(key instanceof Number.Instance && ((Number.Instance)key).number >= 0
				 && ((Number.Instance)key).number <= Integer.MAX_VALUE && ((Number.Instance)key).number == (int)((Number.Instance)key).number)
			return _this.get((int)((Number.Instance)key).number);
		else
			return _this.get(key.toString());
	}
	
	public static void set(BaseObject _this, BaseObject key, BaseObject val) {
		if(key instanceof String.Instance)
			_this.set(((String.Instance) key).string, val);
		else if(key instanceof Number.Instance && ((Number.Instance)key).number >= 0
				 && ((Number.Instance)key).number <= Integer.MAX_VALUE && ((Number.Instance)key).number == (int)((Number.Instance)key).number)
			_this.set((int)((Number.Instance)key).number, val);
		else
			_this.set(key.toString(), val);
	}
	
	public static boolean delete(BaseObject _this, BaseObject key) {
		if(key instanceof String.Instance)
			return _this.delete(((String.Instance) key).string);
		else if(key instanceof Number.Instance && ((Number.Instance)key).number >= 0
				 && ((Number.Instance)key).number <= Integer.MAX_VALUE && ((Number.Instance)key).number == (int)((Number.Instance)key).number)
			return _this.delete((int)((Number.Instance)key).number);
		else
			return _this.delete(key.toString());
	}
	
	public static int toArrayIndex(java.lang.String input) {
		int value = Integer.valueOf(input);
		if(value < 0)
			throw new NumberFormatException("Cannot be less than zero");
		return value;
	}

	public static BaseObject valueOf(BaseObject val) {
		if(isUndefined(val))
			return val;
		BaseFunction valueOf = (BaseFunction)val.get("valueOf", Scopeable.OR_NULL);
		if(valueOf != null)
			return valueOf.call(val);
		return val;
	}

	public static boolean isTrue(BaseObject valueOf) {
		valueOf = valueOf(valueOf);
		if(isUndefined(valueOf))
			return false;
		if(valueOf instanceof Boolean.Instance)
			return ((Boolean.Instance)valueOf).value;
		if(valueOf instanceof Number.Instance)
			return ((Number.Instance)valueOf).number != 0;
		if(valueOf instanceof String.Instance)
			return !((String.Instance)valueOf).string.isEmpty();
		return true;
	}

	private static final ThreadLocal<List<StackTraceElement>> STACK_REPLACEMENTS = new ThreadLocal<List<StackTraceElement>>() {
		@Override
		protected List<StackTraceElement> initialValue() {
			return new ArrayList();
		}
	};
	public static java.lang.String convertStack(Throwable t) {
		StringBuilder builder = new StringBuilder();
		if(t instanceof Error.JavaException) {
			builder.append(((Error.JavaException)t).type);
			java.lang.String message = ((Error.JavaException)t).getUnderlyingMessage();
			if(message != null) {
				builder.append(": ");
				builder.append(message);
			}
		} else
			builder.append(t.toString());
		
		Iterator<StackTraceElement> it = STACK_REPLACEMENTS.get().iterator();
		for(StackTraceElement el : t.getStackTrace()) {
			builder.append("\n\tat ");
			
			if(it.hasNext()) {
				StackTraceElement el0 = it.next();
				if(el0 != null)
					el = el0;
			}
			
			java.lang.String method = el.getMethodName();
			if(method != null) {
				builder.append(method);
				builder.append(" (");
			}
			
			builder.append(el.getFileName());
			builder.append(':');
			builder.append(el.getLineNumber());
			
			if(method != null)
				builder.append(')');
		}
		return builder.toString();
	}

	public static void renameMethodCall(java.lang.String methodName) {
		StackTraceElement[] stack = new Throwable().getStackTrace();
		List<StackTraceElement> list = STACK_REPLACEMENTS.get();
		int leftPad = stack.length-2;
		while(list.size() < leftPad)
			list.add(null);
		list.add(new StackTraceElement(stack[1].getClassName(), methodName,
                             stack[1].getFileName(), stack[1].getLineNumber()));
	}

	public static void renameCall(java.lang.String methodName, java.lang.String fileName, int lineNumber) {
		StackTraceElement[] stack = new Throwable().getStackTrace();
		List<StackTraceElement> list = STACK_REPLACEMENTS.get();
		int leftPad = stack.length-2;
		while(list.size() < leftPad)
			list.add(null);
		list.add(new StackTraceElement(stack[1].getClassName(), methodName, fileName, lineNumber));
	}

	public static void finishCall() {
		List<StackTraceElement> list = STACK_REPLACEMENTS.get();
		list.remove(list.size()-1);
		while(!list.isEmpty() && list.get(list.size()-1) == null)
			list.remove(list.size()-1);
	}
}
