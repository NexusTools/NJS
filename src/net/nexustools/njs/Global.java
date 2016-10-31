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
import net.nexustools.njs.compiler.JavaTranspiler;
import net.nexustools.njs.compiler.NullCompiler;
import net.nexustools.njs.compiler.RuntimeCompiler;

/**
 *
 * @author kate
 */
public class Global extends UniqueObject {
	private static boolean SHOW_UNAVAILABLE_MESSAGE = true;
	public static Compiler createCompiler() {
		try {
			return new JavaTranspiler();
		} catch(Throwable t) {
			try {
				RuntimeCompiler compiler = new RuntimeCompiler();
				if(SHOW_UNAVAILABLE_MESSAGE) {
					SHOW_UNAVAILABLE_MESSAGE = false;
					System.out.println("JavaCompiler unavailable, falling back to RuntimeCompiler");
					t.printStackTrace(System.out);
				}
				return compiler;
			} catch(Throwable tt) {
				if(SHOW_UNAVAILABLE_MESSAGE) {
					SHOW_UNAVAILABLE_MESSAGE = false;
					System.out.println("JavaCompiler and RuntimeCompiler unavailable, compilation at runtime disabled");
					tt.printStackTrace(System.out);
				}
				return new NullCompiler();
			}
		}
	}
	
	public final Compiler compiler;
	public final Function Function = new Function(this);
	public final Object Object = new Object();
	public final String String = new String();
	public final Number Number = new Number();
	public final GeneratorFunction GeneratorFunction;
	public final Boolean Boolean;
	public final Symbol Symbol;
	public final Error Error;
	public final Array Array;
	
	public final Number.Instance NaN;
	public final Number.Instance PositiveOne;
	public final Number.Instance NegativeOne;
	public final Number.Instance Zero;
	public Global() {
		this(createCompiler());
	}
	
	public Global(Compiler compiler) {
		this.compiler = compiler;
		
		super.Number = Number;
		super.String = String;
		
		Symbol = new Symbol(this);
		Symbol.initConstants();
		Object.initPrototype(Object, null);
		Number.initPrototype(Object, null);
		Function.initPrototype(Object, this);
		String.initPrototype(Object, this);
		Symbol.initPrototype(Object, this);
		Number.initConstants();
		
		String.Number = Object.Number = Number;
		NaN = Symbol.iterator.number = Symbol.unscopables.number = this.number = String.number = Symbol.number = Function.number = Number.number = Number.NaN;
		
		PositiveOne = Number.PositiveOne;
		NegativeOne = Number.NegativeOne;
		Zero = Number.Zero;
		
		Object.setupNaN(Number.NaN);
		
		Object.setHidden("__proto__", Function.prototype());
		Function.setHidden("__proto__", Function.prototype());
		String.setHidden("__proto__", Function.prototype());
		Number.setHidden("__proto__", Function.prototype());
		
		String.initPrototypeFunctions(this);
		Object.initPrototypeFunctions(this);
		Function.initPrototypeFunctions(this);
		Number.initPrototypeFunctions(this);
		Symbol.initPrototypeFunctions(this);
		
		GeneratorFunction = new GeneratorFunction(this);
		Boolean = new Boolean(this);
		Error = new Error(this);
		Array = new Array(this);
	}
	
	public void initStandards() {
		setHidden("constructor", Object);
		setHidden("__proto__", Object.prototype());
		
		setHidden("NaN", Number.NaN);
		setHidden("Infinity", Number.PositiveInfinity);
		setHidden("isNaN", new AbstractFunction(this) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return Double.isNaN(params[0].toDouble()) ? Boolean.TRUE : Boolean.FALSE;
			}
		});
		setHidden("Function", Function);
		setHidden("Object", Object);
		setHidden("String", String);
		setHidden("Number", Number);
		setHidden("Symbol", Symbol);
		setHidden("Error", Error);
		setHidden("Array", Array);
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

	private final List<WeakReference<JavaClassWrapper>> CONSTRUCTORS = new ArrayList();
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
			return new Error.Instance(String, Error, Symbol.iterator, NaN, ((Error.JavaException)t).type, ((Error.JavaException) t).getUnderlyingMessage(), JSHelper.extractStack(t.getMessage(), t));
		return new Error.Instance(String, Error, Symbol.iterator, NaN, "JavaError", t.toString(), JSHelper.extractStack("JavaError: " + t.toString(), t));
	}

	private final List<WeakReference<JavaObjectWrapper>> WRAPS = new ArrayList();
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
		
			JavaObjectWrapper wrapper = new JavaObjectWrapper(javaObject, wrap(javaObject.getClass()), this);
			WRAPS.add(new WeakReference(wrapper));
			return wrapper;
		}
	}
	
	public BaseObject javaToJS(java.lang.Object javaObject) {
		return JSHelper.javaToJS(this, javaObject);
	}
	
}
