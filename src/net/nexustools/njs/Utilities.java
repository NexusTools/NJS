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
import java.util.List;
import java.util.Map;

/**
 *
 * @author Katelyn Slater <ktaeyln@gmail.com>
 */
public class Utilities {

	public static boolean stringMoreThan(java.lang.String s1, java.lang.String s2) {
		int l1 = s1.length();
		int l2 = s2.length();
		if (l1 > l2) {
			for (int i = 0; i < l1; i++) {
				char c1 = s1.charAt(i);
				char c2;
				try {
					c2 = s2.charAt(i);
				} catch (IndexOutOfBoundsException ex) {
					c2 = 0;
				}
				if (c1 == c2) {
					continue;
				}

				return c1 > c2;
			}

			return false;
		} else if (l1 < l2) {
			for (int i = 0; i < l1; i++) {
				char c1;
				try {
					c1 = s1.charAt(i);
				} catch (IndexOutOfBoundsException ex) {
					c1 = 0;
				}
				char c2 = s2.charAt(i);
				if (c1 == c2) {
					continue;
				}

				return c1 > c2;
			}

			return false;
		} else {
			for (int i = 0; i < l1; i++) {
				char c1 = s1.charAt(i);
				char c2 = s2.charAt(i);
				if (c1 == c2) {
					continue;
				}

				return c1 > c2;
			}
			return false;
		}
	}

	public static boolean stringLessThan(java.lang.String s1, java.lang.String s2) {
		int l1 = s1.length();
		int l2 = s2.length();
		if (l1 > l2) {
			for (int i = 0; i < l1; i++) {
				char c1 = s1.charAt(i);
				char c2;
				try {
					c2 = s2.charAt(i);
				} catch (IndexOutOfBoundsException ex) {
					c2 = 0;
				}
				if (c1 == c2) {
					continue;
				}

				return c1 < c2;
			}

			return false;
		} else if (l1 < l2) {
			for (int i = 0; i < l1; i++) {
				char c1;
				try {
					c1 = s1.charAt(i);
				} catch (IndexOutOfBoundsException ex) {
					c1 = 0;
				}
				char c2 = s2.charAt(i);
				if (c1 == c2) {
					continue;
				}

				return c1 < c2;
			}

			return false;
		} else {
			for (int i = 0; i < l1; i++) {
				char c1 = s1.charAt(i);
				char c2 = s2.charAt(i);
				if (c1 == c2) {
					continue;
				}

				return c1 < c2;
			}
			return false;
		}
	}

	public static boolean stringMoreEqual(java.lang.String s1, java.lang.String s2) {
		int l1 = s1.length();
		int l2 = s2.length();
		if (l1 > l2) {
			for (int i = 0; i < l1; i++) {
				char c1 = s1.charAt(i);
				char c2;
				try {
					c2 = s2.charAt(i);
				} catch (IndexOutOfBoundsException ex) {
					c2 = 0;
				}
				if (c1 == c2) {
					continue;
				}

				return c1 >= c2;
			}

			return true;
		} else if (l1 < l2) {
			for (int i = 0; i < l1; i++) {
				char c1;
				try {
					c1 = s1.charAt(i);
				} catch (IndexOutOfBoundsException ex) {
					c1 = 0;
				}
				char c2 = s2.charAt(i);
				if (c1 == c2) {
					continue;
				}

				return c1 >= c2;
			}

			return true;
		} else {
			for (int i = 0; i < l1; i++) {
				char c1 = s1.charAt(i);
				char c2 = s2.charAt(i);
				if (c1 == c2) {
					continue;
				}

				return c1 >= c2;
			}
			return true;
		}
	}

	public static boolean stringLessEqual(java.lang.String s1, java.lang.String s2) {
		int l1 = s1.length();
		int l2 = s2.length();
		if (l1 > l2) {
			for (int i = 0; i < l1; i++) {
				char c1 = s1.charAt(i);
				char c2;
				try {
					c2 = s2.charAt(i);
				} catch (IndexOutOfBoundsException ex) {
					c2 = 0;
				}
				if (c1 == c2) {
					continue;
				}

				return c1 <= c2;
			}

			return true;
		} else if (l1 < l2) {
			for (int i = 0; i < l1; i++) {
				char c1;
				try {
					c1 = s1.charAt(i);
				} catch (IndexOutOfBoundsException ex) {
					c1 = 0;
				}
				char c2 = s2.charAt(i);
				if (c1 == c2) {
					continue;
				}

				return c1 <= c2;
			}

			return true;
		} else {
			for (int i = 0; i < l1; i++) {
				char c1 = s1.charAt(i);
				char c2 = s2.charAt(i);
				if (c1 == c2) {
					continue;
				}

				return c1 <= c2;
			}
			return true;
		}
	}

	public static class ConversionAccuracy {

		double accuracy;
	}
	private static final ConversionAccuracy DONT_CARE_ABOUT_ACCURACY = new ConversionAccuracy();

	public static java.lang.Object jsToJava(final BaseObject jsObject, Class<?> desiredClass) {
		return jsToJava(jsObject, desiredClass, DONT_CARE_ABOUT_ACCURACY);
	}

	public static java.lang.Object jsToJava(final BaseObject jsObject, Class<?> desiredClass, ConversionAccuracy accuracy) {
		if (jsObject instanceof Undefined || jsObject instanceof Null) {
			return null;
		}

		if (desiredClass == java.lang.Object.class) {
			return jsObject;
		}

		if (desiredClass.isArray()) {
			Class<?> desiredArrayType = desiredClass.getComponentType();

			if (desiredArrayType == Character.TYPE) {
				if (jsObject instanceof String.Instance) {
					accuracy.accuracy = 1;
				} else if (jsObject instanceof Number.Instance) {
					accuracy.accuracy = 0.75;
				} else {
					accuracy.accuracy = 0.5;
				}
				return jsObject.toString();
			}

			throw new UnsupportedOperationException("Cannot convert array with component type " + desiredArrayType);
		}

		if (desiredClass == java.lang.String.class) {
			if (jsObject instanceof String.Instance) {
				accuracy.accuracy = 1;
			} else if (jsObject instanceof Number.Instance) {
				accuracy.accuracy = 0.75;
			} else {
				accuracy.accuracy = 0.5;
			}
			return jsObject.toString();
		}

		if (desiredClass == Runnable.class && jsObject instanceof BaseFunction) {
			accuracy.accuracy = 1;
			return new Runnable() {
				@Override
				public void run() {
					((BaseFunction) jsObject).call(Undefined.INSTANCE);
				}
			};
		}

		if (desiredClass == java.lang.Boolean.class) {
			if (jsObject instanceof Boolean.Instance) {
				accuracy.accuracy = 1;
			} else if (jsObject instanceof Number.Instance) {
				accuracy.accuracy = 0.75;
			} else {
				accuracy.accuracy = 0.5;
			}
			return (java.lang.Boolean)jsObject.toBool();
		}
		if (desiredClass == java.lang.Boolean.TYPE) {
			if (jsObject instanceof Boolean.Instance) {
				accuracy.accuracy = 1;
			} else if (jsObject instanceof Number.Instance) {
				accuracy.accuracy = 0.75;
			} else {
				accuracy.accuracy = 0.5;
			}
			return jsObject.toBool();
		}

		if (jsObject instanceof Number.Instance) {
			accuracy.accuracy = 1;
			double value = ((Number.Instance) jsObject).value;
			if (desiredClass == Long.class) {
				return (Long) (long) value;
			}
			if (desiredClass == Integer.class) {
				return (Integer) (int) value;
			};
			if (desiredClass == Float.class) {
				return (Float) (float) value;
			};
			if (desiredClass == Double.class) {
				return (Double) value;
			}
			if (desiredClass == Short.class) {
				return (Short) (short) value;
			}
			if (desiredClass == Byte.class) {
				return (Byte) (byte) value;
			}
			if (desiredClass == Integer.TYPE) {
				return (int) value;
			}
			if (desiredClass == Float.TYPE) {
				return (float) value;
			}
			if (desiredClass == Double.TYPE) {
				return value;
			}
			if (desiredClass == Short.TYPE) {
				return (short) value;
			}
			if (desiredClass == Byte.TYPE) {
				return (byte) value;
			}
		}

		if (java.lang.Number.class.isAssignableFrom(desiredClass)) {
			if (jsObject instanceof String.Instance) {
				accuracy.accuracy = 0.75;
			} else {
				accuracy.accuracy = 0.5;
			}

			java.lang.String string = jsObject.toString();
			if (desiredClass == Long.class) {
				return Long.valueOf(string);
			}
			if (desiredClass == Integer.class) {
				return Integer.valueOf(string);
			}
			if (desiredClass == Float.class) {
				return Float.valueOf(string);
			}
			if (desiredClass == Double.class) {
				return Double.valueOf(string);
			}
			if (desiredClass == Short.class) {
				return Short.valueOf(string);
			}
			if (desiredClass == Byte.class) {
				return Byte.valueOf(string);
			}
		}

		if (desiredClass == Integer.TYPE) {
			if (jsObject instanceof String.Instance) {
				accuracy.accuracy = 0.75;
			} else {
				accuracy.accuracy = 0.5;
			}

			java.lang.String string = jsObject.toString();
			return (int) Integer.valueOf(string);
		}
		if (desiredClass == Float.TYPE) {
			if (jsObject instanceof String.Instance) {
				accuracy.accuracy = 0.75;
			} else {
				accuracy.accuracy = 0.5;
			}

			java.lang.String string = jsObject.toString();
			return (float) Float.valueOf(string);
		}
		if (desiredClass == Double.TYPE) {
			if (jsObject instanceof String.Instance) {
				accuracy.accuracy = 0.75;
			} else {
				accuracy.accuracy = 0.5;
			}

			java.lang.String string = jsObject.toString();
			return (double) Double.valueOf(string);
		}
		if (desiredClass == Short.TYPE) {
			if (jsObject instanceof String.Instance) {
				accuracy.accuracy = 0.75;
			} else {
				accuracy.accuracy = 0.5;
			}

			java.lang.String string = jsObject.toString();
			return (short) Short.valueOf(string);
		}
		if (desiredClass == Byte.TYPE) {
			if (jsObject instanceof String.Instance) {
				accuracy.accuracy = 0.75;
			} else {
				accuracy.accuracy = 0.5;
			}

			java.lang.String string = jsObject.toString();
			return (byte) Byte.valueOf(string);
		}
		if (desiredClass == Long.TYPE) {
			if (jsObject instanceof String.Instance) {
				accuracy.accuracy = 0.75;
			} else {
				accuracy.accuracy = 0.5;
			}

			java.lang.String string = jsObject.toString();
			return (long) Long.valueOf(string);
		}

		if (jsObject instanceof JavaObjectWrapper && desiredClass.isInstance(((JavaObjectWrapper) jsObject).javaObject)) {
			accuracy.accuracy = 1;
			return ((JavaObjectWrapper) jsObject).javaObject;
		}

		throw new UnsupportedOperationException(desiredClass.getName() + " from " + jsObject.getClass().getName());
	}

	public static BaseObject javaToJS(Global global, java.lang.Object javaObject) {
		if (javaObject == null) {
			return Null.INSTANCE;
		}

		if (javaObject instanceof BaseObject) {
			return (BaseObject) javaObject;
		}

		if (javaObject instanceof java.lang.String) {
			return global.wrap(((java.lang.String) javaObject));
		}

		if (javaObject instanceof java.lang.Number) {
			return global.wrap(((java.lang.Number) javaObject).doubleValue());
		}
		if (java.lang.Integer.TYPE.isInstance(javaObject)) {
			return global.wrap((int) (Integer) javaObject);
		}

		if (javaObject instanceof Class) {
			return global.wrap((Class) javaObject);
		}

		Class<?> javaClass = javaObject.getClass();
		if (javaClass.isArray()) {
			if (javaClass.getComponentType() == Byte.TYPE) {
				return new Uint8Array.Instance(global, (Uint8Array) global.get("Uint8Array"), (byte[]) javaObject);
			}
			if (javaClass.getComponentType() == Short.TYPE) {
				return new Uint16Array.Instance(global, (Uint16Array) global.get("Uint16Array"), (short[]) javaObject);
			}
			if (javaClass.getComponentType() == Integer.TYPE) {
				return new Uint32Array.Instance(global, (Uint32Array) global.get("Uint32Array"), (int[]) javaObject);
			}
			if (javaClass.getComponentType() == Double.TYPE) {
				return new Float64Array.Instance(global, (Float64Array) global.get("Float64Array"), (double[]) javaObject);
			}

			throw new RuntimeException("Cannot convert array of " + javaClass.getComponentType() + " to JSBaseObject");
		} else {
			return global.wrap(javaObject);
		}
	}

	public static Global createStandardGlobal() {
		return createGlobal("eval", "Math", "Date", "JSON", "Uint8Array", "Uint8ClampedArray", "Int8Array", "Uint16Array", "Int16Array", "Uint32Array", "Int32Array", "Float64Array");
	}

	public static Global createStandardGlobal(net.nexustools.njs.compiler.Compiler compiler) {
		return createGlobal(compiler, "eval", "Math", "Date", "JSON", "Uint8Array", "Uint8ClampedArray", "Int8Array", "Uint16Array", "Int16Array", "Uint32Array", "Int32Array", "Float64Array");
	}

	public static Global createExtendedGlobal() {
		return createGlobal("eval", "Math", "Date", "JSON", "Uint8Array", "Uint8ClampedArray", "Int8Array", "Uint16Array", "Int16Array", "Uint32Array", "Int32Array", "Float64Array", "GeneratorFunction", "importClass", "isJavaClass", "isJavaObject", "isJavaPackage", "PackageRoot", "java", "javax", "print");
	}

	public static Global createExtendedGlobal(net.nexustools.njs.compiler.Compiler compiler) {
		return createGlobal(compiler, "eval", "Math", "Date", "JSON", "Uint8Array", "Uint8ClampedArray", "Int8Array", "Uint16Array", "Int16Array", "Uint32Array", "Int32Array", "Float64Array", "GeneratorFunction", "importClass", "isJavaClass", "isJavaObject", "isJavaPackage", "PackageRoot", "java", "javax", "print");
	}

	public static Global createGlobal(java.lang.String... standards) {
		return createGlobal(Global.createCompiler(), standards);
	}

	public static Global createGlobal(net.nexustools.njs.compiler.Compiler compiler, java.lang.String... standards) {
		final Global global = new Global(compiler);
		global.initStandards();
		JavaPackageWrapper PackageRoot = null;
		for (java.lang.String standard : standards) {
			if (standard.equals("JSON")) {
				global.setHidden("JSON", new JSON(global));
			} else if (standard.equals("Math")) {
				global.setHidden("Math", new Math(global));
			} else if (standard.equals("Date")) {
				global.setHidden("Date", new Date(global));
			} else if (standard.equals("Uint8Array")) {
				global.setHidden("Uint8Array", new Uint8Array(global));
			} else if (standard.equals("Uint8ClampedArray")) {
				global.setHidden("Uint8ClampedArray", new Uint8ClampedArray(global));
			} else if (standard.equals("Int8Array")) {
				global.setHidden("Int8Array", new Int8Array(global));
			} else if (standard.equals("Uint16Array")) {
				global.setHidden("Uint16Array", new Uint16Array(global));
			} else if (standard.equals("Int16Array")) {
				global.setHidden("Int16Array", new Int16Array(global));
			} else if (standard.equals("Uint32Array")) {
				global.setHidden("Uint32Array", new Uint32Array(global));
			} else if (standard.equals("Int32Array")) {
				global.setHidden("Int32Array", new Int32Array(global));
			} else if (standard.equals("Float64Array")) {
				global.setHidden("Float64Array", new Float64Array(global));
			} else if (standard.equals("GeneratorFunction")) {
				global.setHidden("GeneratorFunction", global.GeneratorFunction);
			} else if (standard.equals("PackageRoot")) {
				if (PackageRoot == null) {
					PackageRoot = new JavaPackageWrapper(global);
				}
				global.setHidden("PackageRoot", PackageRoot);
			} else if (standard.equals("java")) {
				if (PackageRoot == null) {
					PackageRoot = new JavaPackageWrapper(global);
				}
				global.setHidden("java", PackageRoot.getSubPackage("java"));
			} else if (standard.equals("javax")) {
				if (PackageRoot == null) {
					PackageRoot = new JavaPackageWrapper(global);
				}
				global.setHidden("javax", PackageRoot.getSubPackage("javax"));
			} else if (standard.equals("isJavaClass")) {
				global.setHidden("isJavaClass", new AbstractFunction(global) {
					@Override
					public BaseObject call(BaseObject _this, BaseObject... params) {
						return params[0] instanceof JavaClassWrapper ? global.Boolean.TRUE : global.Boolean.FALSE;
					}

					@Override
					public java.lang.String name() {
						return "isJavaClass";
					}
				});
			} else if (standard.equals("isJavaObject")) {
				global.setHidden("isJavaObject", new AbstractFunction(global) {
					@Override
					public BaseObject call(BaseObject _this, BaseObject... params) {
						return params[0] instanceof JavaObjectWrapper ? global.Boolean.TRUE : global.Boolean.FALSE;
					}

					@Override
					public java.lang.String name() {
						return "isJavaClass";
					}
				});
			} else if (standard.equals("isJavaPackage")) {
				global.setHidden("isJavaPackage", new AbstractFunction(global) {
					@Override
					public BaseObject call(BaseObject _this, BaseObject... params) {
						return params[0] instanceof JavaPackageWrapper ? global.Boolean.TRUE : global.Boolean.FALSE;
					}

					@Override
					public java.lang.String name() {
						return "isJavaPackage";
					}
				});
			} else if (standard.equals("print")) {
				global.setHidden("print", new AbstractFunction(global) {
					@Override
					public BaseObject call(BaseObject _this, BaseObject... params) {
						renameMethodCall("print");
						System.out.println(params[0].toString());
						return Undefined.INSTANCE;
					}

					@Override
					public java.lang.String name() {
						return "print";
					}
				});
			} else if (standard.equals("eval")) {
				global.setHidden("eval", new AbstractFunction(global) {
					@Override
					public BaseObject call(BaseObject _this, BaseObject... params) {
						renameMethodCall("eval");
						return global.compiler.compile(params[0].toString(), "<eval>", false).exec(global, Scope.current());
					}

					@Override
					public java.lang.String name() {
						return "eval";
					}
				});
			} else if (standard.equals("importClass")) {
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
			} else {
				throw new RuntimeException("Unknown Standard Requested: " + standard);
			}
		}
		return global;
	}

	public static boolean isUndefined(BaseObject object) {
		return object == null || object == Undefined.INSTANCE || object == Null.INSTANCE;
	}

	public static BaseObject get(BaseObject _this, double key) {
		if (key >= 0 && key <= Integer.MAX_VALUE && key == (int)key) {
			return _this.get((int)key);
		} else {
			return _this.get(Number.toString(key));
		}
	}

	public static BaseObject get(BaseObject _this, BaseObject key) {
		if (key instanceof String.Instance) {
			return _this.get(((String.Instance) key).string);
		} else if (key instanceof Number.Instance && ((Number.Instance) key).value >= 0
			&& ((Number.Instance) key).value <= Integer.MAX_VALUE && ((Number.Instance) key).value == (int) ((Number.Instance) key).value) {
			return _this.get((int) ((Number.Instance) key).value);
		} else {
			return _this.get(key.toString());
		}
	}

	public static BaseObject set(BaseObject _this, BaseObject key, BaseObject val) {
		if (key instanceof String.Instance) {
			_this.set(((String.Instance) key).string, val);
		} else if (key instanceof Number.Instance && ((Number.Instance) key).value >= 0
			&& ((Number.Instance) key).value <= Integer.MAX_VALUE && ((Number.Instance) key).value == (int) ((Number.Instance) key).value) {
			_this.set((int) ((Number.Instance) key).value, val);
		} else {
			_this.set(key.toString(), val);
		}
		return val;
	}

	public static boolean delete(BaseObject _this, BaseObject key) {
		if (key instanceof String.Instance) {
			return _this.delete(((String.Instance) key).string);
		} else if (key instanceof Number.Instance && ((Number.Instance) key).value >= 0
			&& ((Number.Instance) key).value <= Integer.MAX_VALUE && ((Number.Instance) key).value == (int) ((Number.Instance) key).value) {
			return _this.delete((int) ((Number.Instance) key).value);
		} else {
			return _this.delete(key.toString());
		}
	}

	public static BaseObject valueOf(BaseObject val) {
		if (isUndefined(val)) {
			return val;
		}
		BaseObject valueOf = val.get("valueOf");
		if (valueOf instanceof BaseFunction) {
			return ((BaseFunction) val.get("valueOf", Scopeable.OR_NULL)).call(val);
		}
		return val;
	}

	public static StackTraceElement[] convertStackTrace(StackTraceElement[] stack) {
		final List<StackElementReplace> list = STACK_REPLACEMENTS.get();
		final StackTraceElement[] converted = new StackTraceElement[stack.length];
		int stackRemaining = stack.length - 1;
		final int target = list.size();
		for (StackTraceElement el : stack) {
			if (stackRemaining < target) {
				StackElementReplace el0 = list.get(stackRemaining);
				if (el0 != null && el0.original.getClassName().equals(el.getClassName())
						&& el0.original.getFileName().equals(el.getFileName()) && el0.original.getMethodName().equals(el.getMethodName())
						&& el.getLineNumber() >= el0.original.getLineNumber() && (el0.replacement == null || el.getLineNumber() <= el0.replacement.maxLineNumber)) {
					el0.update(el.getLineNumber());
					el = el0.replacement.toStackTraceElement();
				}
			}

			converted[stack.length - stackRemaining - 1] = el;
			stackRemaining--;
		}
		return converted;
	}

	public static class ReplacementStackTraceElement {

		public int rows, columns;
		private int maxLineNumber = Integer.MAX_VALUE;
		private final java.lang.String methodName, fileName;
		private final Map<Integer, FilePosition> sourceMap;

		private ReplacementStackTraceElement(java.lang.String methodName, java.lang.String fileName, int rows, int columns) {
			this.methodName = methodName;
			this.fileName = fileName;
			this.rows = rows;
			this.columns = columns;
			sourceMap = null;
		}

		private ReplacementStackTraceElement(java.lang.String methodName, java.lang.String fileName, Map<Integer, FilePosition> SOURCE_MAP) {
			this.methodName = methodName;
			this.fileName = fileName;
			rows = 0;
			columns = 0;
			sourceMap = SOURCE_MAP;
		}

		private StackTraceElement toStackTraceElement() {
			return new StackTraceElement("", methodName, fileName, rows);
		}

		public void finishCall() {
			maxLineNumber = new Throwable().getStackTrace()[1].getLineNumber();
		}

	}

	private static class StackElementReplace {

		ReplacementStackTraceElement replacement;
		final StackTraceElement original;

		public StackElementReplace(StackTraceElement original, ReplacementStackTraceElement replacement) {
			this.original = original;
			this.replacement = replacement;
		}

		private void update(int lineNumber) {
			if(replacement.sourceMap != null) {
				FilePosition pos = null;
				for(Map.Entry<java.lang.Integer, FilePosition> entry : replacement.sourceMap.entrySet()) {
					if(lineNumber < entry.getKey())
						break;
					pos = entry.getValue();
				}
				if(pos != null) {
					replacement.columns = pos.column;
					replacement.rows = pos.row;
				}
			}
		}
	}
	private static final ThreadLocal<Integer> STACK_POSITION = new ThreadLocal<Integer>() {
		@Override
		protected Integer initialValue() {
			return 0;
		}
	};
	private static final ThreadLocal<List<StackElementReplace>> STACK_REPLACEMENTS = new ThreadLocal<List<StackElementReplace>>() {
		@Override
		protected List<StackElementReplace> initialValue() {
			return new ArrayList();
		}
	};

	public static void updateCallPosition(int rows, int columns, int stackPos) {
		ReplacementStackTraceElement el = STACK_REPLACEMENTS.get().get(stackPos).replacement;
		el.columns = columns;
		el.rows = rows;
	}

	public static java.lang.String extractStack(java.lang.String header, Throwable t) {
		StringBuilder builder = new StringBuilder(header);
		final StackTraceElement[] stack = t.getStackTrace();
		final List<StackElementReplace> list = STACK_REPLACEMENTS.get();
		int stackRemaining = stack.length - 1;
		final int target = list.size();
		for (StackTraceElement el : stack) {
			builder.append("\n\tat ");

			if (stackRemaining < target) {
				StackElementReplace el0 = list.get(stackRemaining);
				if (el0 != null && el0.original.getClassName().equals(el.getClassName()) && el0.original.getFileName().equals(el.getFileName())
						&& el0.original.getMethodName().equals(el.getMethodName()) && el.getLineNumber() >= el0.original.getLineNumber()
						&& (el0.replacement == null || el.getLineNumber() <= el0.replacement.maxLineNumber)) {

					el0.update(el.getLineNumber());
					ReplacementStackTraceElement rel = el0.replacement;

					java.lang.String method = rel.methodName;
					boolean hasMethod = method != null && !method.isEmpty();
					if (hasMethod) {
						builder.append(method);
						builder.append(" (");
					}

					java.lang.String fileName = rel.fileName;
					if (fileName == null) {
						builder.append("<unknown source>");
					} else {
						builder.append(fileName);
						if (rel.rows > 0) {
							builder.append(':');
							builder.append(rel.rows);
							if (rel.columns > 0) {
								builder.append(':');
								builder.append(rel.columns);
							}
						}
					}

					if (hasMethod) {
						builder.append(')');
					}

					stackRemaining--;
					continue;
				}
			}

			java.lang.String method = el.getMethodName();
			boolean hasMethod = method != null && !method.isEmpty();
			if (hasMethod) {
				builder.append(method);
				builder.append(" (");
			}

			java.lang.String fileName = el.getFileName();
			if (fileName == null) {
				builder.append("<unknown source>");
			} else {
				builder.append(fileName);
				int lineNumber = el.getLineNumber();
				if (lineNumber > 0) {
					builder.append(':');
					builder.append(lineNumber);
				}
			}

			if (hasMethod) {
				builder.append(')');
			}

			stackRemaining--;
		}
		return builder.toString();
	}
	
	
	public static class FilePosition {
		public final int row, column;
		public FilePosition(int row, int column) {
			this.column = column;
			this.row = row;
		}
	}
	public static ReplacementStackTraceElement renameMethodCall(java.lang.String methodName) {
		ReplacementStackTraceElement replacement;
		StackTraceElement[] stack = new Throwable().getStackTrace();
		List<StackElementReplace> list = STACK_REPLACEMENTS.get();
		int leftPad = stack.length - 2;
		if (list.size() <= leftPad) {
			while (list.size() < leftPad) {
				list.add(null);
			}
			list.add(new StackElementReplace(stack[1], replacement = new ReplacementStackTraceElement(methodName, stack[1].getFileName(), stack[1].getLineNumber(), 0)));
		} else {
			list.set(leftPad, new StackElementReplace(stack[1], replacement = new ReplacementStackTraceElement(methodName, stack[1].getFileName(), stack[1].getLineNumber(), 0)));
		}
		STACK_POSITION.set(leftPad);
		return replacement;
	}
	public static ReplacementStackTraceElement mapCall(java.lang.String methodName, java.lang.String fileName, Map<Integer, FilePosition> SOURCE_MAP) {
		ReplacementStackTraceElement replacement;
		StackTraceElement[] stack = new Throwable().getStackTrace();
		List<StackElementReplace> list = STACK_REPLACEMENTS.get();
		int leftPad = stack.length - 2;
		if (list.size() <= leftPad) {
			while (list.size() < leftPad) {
				list.add(null);
			}
			list.add(new StackElementReplace(stack[1], replacement = new ReplacementStackTraceElement(methodName, fileName, SOURCE_MAP)));
		} else {
			list.set(leftPad, new StackElementReplace(stack[1], replacement = new ReplacementStackTraceElement(methodName, fileName, SOURCE_MAP)));
		}
		STACK_POSITION.set(leftPad);
		return replacement;
	}
	public static ReplacementStackTraceElement renameCall(java.lang.String methodName, java.lang.String fileName, int rows, int columns) {
		ReplacementStackTraceElement replacement;
		StackTraceElement[] stack = new Throwable().getStackTrace();
		List<StackElementReplace> list = STACK_REPLACEMENTS.get();
		int leftPad = stack.length - 2;
		if (list.size() <= leftPad) {
			while (list.size() < leftPad) {
				list.add(null);
			}
			list.add(new StackElementReplace(stack[1], replacement = new ReplacementStackTraceElement(methodName, fileName, rows, columns)));
		} else {
			list.set(leftPad, new StackElementReplace(stack[1], replacement = new ReplacementStackTraceElement(methodName, fileName, rows, columns)));
		}
		STACK_POSITION.set(leftPad);
		return replacement;
	}

	public static Arguments convertArguments(Global global, java.lang.String... arguments) {
		return new Arguments(global, null, convertArray(global, arguments));
	}

	public static BaseObject[] convertArray(Global global, java.lang.String[] array) {
		BaseObject[] converted = new BaseObject[array.length];
		for (int i = 0; i < converted.length; i++) {
			converted[i] = global.wrap(array[i]);
		}
		return converted;
	}

	public static BaseObject[] convertArray(Global global, java.lang.Object[] array) {
		BaseObject[] converted = new BaseObject[array.length];
		for (int i = 0; i < converted.length; i++) {
			converted[i] = Utilities.javaToJS(global, array[i]);
		}
		return converted;
	}
	
	public static int nextPowerOf2(final int a) {
        int b = 1;
        while (b < a)
            b = b << 1;
        return b;
    }

}
