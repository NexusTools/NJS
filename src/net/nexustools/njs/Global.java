/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.nexustools.njs.compiler.Compiler;
import net.nexustools.njs.compiler.JavaCompiler;
import net.nexustools.njs.compiler.RuntimeCompiler;

/**
 *
 * @author kate
 */
public class Global extends GenericObject {
	private static boolean SHOW_UNAVAILABLE_MESSAGE = true;
	public static Compiler createCompiler() {
		try {
			return new JavaCompiler();
		} catch(Throwable t) {
			if(SHOW_UNAVAILABLE_MESSAGE) {
				SHOW_UNAVAILABLE_MESSAGE = false;
				System.out.println("JavaCompiler unavailable, falling back to RuntimeCompiler");
				t.printStackTrace(System.out);
			}
			return new RuntimeCompiler();
		}
	}
	
	public final Compiler compiler;
	public final Function Function = new Function(this);
	public final Object Object = new Object();
	public final String String = new String();
	public final Number Number = new Number();
	public final Boolean Boolean;
	public final Symbol Symbol;
	public final Error Error;
	public final Array Array;
	
	public final Number.Instance NaN;
	public final Number.Instance PositiveInfinity;
	public final Number.Instance NegativeInfinity;
	
	public Global() {
		this(createCompiler());
	}
	
	public Global(Compiler compiler) {
		this.compiler = compiler;
		
		Object.initPrototype(Object);
		Function.initPrototype(Object);
		String.initPrototype(Object);
		Number.initPrototype(Object);
		
		Object.setHidden("__proto__", Function.prototype());
		Function.setHidden("__proto__", Function.prototype());
		String.setHidden("__proto__", Function.prototype());
		Number.setHidden("__proto__", Function.prototype());
		
		String.initPrototypeFunctions(this);
		Object.initPrototypeFunctions(this);
		Function.initPrototypeFunctions(this);
		Number.initPrototypeFunctions(this);
		
		Boolean = new Boolean(this);
		Symbol = new Symbol(this);
		Error = new Error(this);
		Array = new Array(this);
		
		NaN = Number.wrap(Double.NaN);
		PositiveInfinity = Number.wrap(Double.POSITIVE_INFINITY);
		NegativeInfinity = Number.wrap(Double.NEGATIVE_INFINITY);
		PositiveInfinity.seal();
		NegativeInfinity.seal();
		NaN.seal();
	}
	
	public void initStandards() {
		setStorage("constructor", Object, false);
		setStorage("__proto__", Object.prototype(), false);
		
		setStorage("NaN", NaN, false);
		setStorage("isNaN", new AbstractFunction(this) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return Double.isNaN(Number.from(params[0]).number) ? Boolean.TRUE : Boolean.FALSE;
			}
		}, false);
		setStorage("Function", Function, false);
		setStorage("Object", Object, false);
		setStorage("String", String, false);
		setStorage("Number", Number, false);
		setStorage("Symbol", Symbol, false);
		setStorage("Error", Error, false);
		setStorage("Array", Array, false);
	}
	
	public Number.Instance wrap(double number) {
		return Number.wrap(number);
	}
	
	public Boolean.Instance wrap(boolean bool) {
		return bool ? Boolean.TRUE : Boolean.FALSE;
	}
	
	public String.Instance wrap(java.lang.String string) {
		return String.wrap(string);
	}

	private static final List<WeakReference<JavaClassWrapper>> CONSTRUCTORS = new ArrayList();
	public JavaClassWrapper wrap(Class<?> javaClass) {
		assert(javaClass != null);
		synchronized(CONSTRUCTORS) {
			Iterator<WeakReference<JavaClassWrapper>> it = CONSTRUCTORS.iterator();
			while(it.hasNext()) {
				WeakReference<JavaClassWrapper> ref = it.next();
				JavaClassWrapper constructor = ref.get();
				if(constructor == null)
					it.remove();
				else if(constructor.javaClass == javaClass)
					return constructor;
			}
		
			JavaClassWrapper constructor = new JavaClassWrapper(this, javaClass);
			CONSTRUCTORS.add(new WeakReference(constructor));
			return constructor;
		}
	}
	
	public BaseObject wrap(Throwable t) {
		if(t instanceof Error.Thrown)
			return ((Error.Thrown)t).what;
		if(t instanceof Error.JavaException)
			return new Error.Instance(String, Error, ((Error.JavaException)t).type, ((Error.JavaException) t).getUnderlyingMessage(), JSHelper.convertStack(t.getMessage(), t));
		return new Error.Instance(String, Error, "JavaError", t.toString(), JSHelper.convertStack("JavaError: " + t.toString(), t));
	}

	private static final List<WeakReference<JavaObjectWrapper>> WRAPS = new ArrayList();
	public BaseObject wrap(java.lang.Object javaObject) {
		if(javaObject == null)
			return Undefined.INSTANCE;
		
		synchronized(WRAPS) {
			Iterator<WeakReference<JavaObjectWrapper>> it = WRAPS.iterator();
			while(it.hasNext()) {
				WeakReference<JavaObjectWrapper> ref = it.next();
				JavaObjectWrapper wrapper = ref.get();
				if(wrapper == null)
					it.remove();
				else if(wrapper.javaObject == javaObject)
					return wrapper;
			}
		
			JavaObjectWrapper wrapper = new JavaObjectWrapper(javaObject, wrap(javaObject.getClass()));
			WRAPS.add(new WeakReference(wrapper));
			return wrapper;
		}
	}

	public Number.Instance toNumber(BaseObject param) {
		return Number.from(param);
	}
	
	public int toArrayRange(BaseObject param) {
		Number.Instance number = Number.from(param);
		double newLength = ((Number.Instance)number).number;
		if(newLength < 0 || newLength > Integer.MAX_VALUE || (int)newLength != newLength)
			throw new Error.JavaException("RangeError", "Invalid array length");
		return ((Number.Instance)number).toInt();
	}
	
	public BaseObject javaToJS(java.lang.Object javaObject) {
		return JSHelper.javaToJS(this, javaObject);
	}
	
}
