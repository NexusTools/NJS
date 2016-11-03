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
 *
 * @author Katelyn Slater <ktaeyln@gmail.com>
 */
package net.nexustools.njs;

import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
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
public class Global extends GenericObject {
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
	public final GeneratorFunction GeneratorFunction;
	public final Object Object = new Object();
	public final String String = new String();
	public final Number Number = new Number();
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
		GenericObject ObjectPrototype = new GenericObject();
		ObjectPrototype.setHidden("constructor", Object);
		Object.setPrototype(ObjectPrototype);
		GenericObject NumberPrototype = new GenericObject();
		NumberPrototype.setHidden("constructor", Number);
		NumberPrototype.__proto__ = ObjectPrototype;
		Number.setPrototype(NumberPrototype);
		GenericObject FunctionPrototype = new GenericObject();
		FunctionPrototype.setHidden("constructor", Function);
		FunctionPrototype.__proto__ = ObjectPrototype;
		Function.setPrototype(FunctionPrototype);
		GenericObject StringPrototype = new GenericObject();
		StringPrototype.setHidden("constructor", String);
		StringPrototype.__proto__ = ObjectPrototype;
		String.setPrototype(StringPrototype);
		GenericObject SymbolPrototype = new GenericObject();
		SymbolPrototype.setHidden("constructor", Symbol);
		SymbolPrototype.__proto__ = ObjectPrototype;
		Symbol.setPrototype(SymbolPrototype);
		Number.initConstants();
		
		ObjectPrototype.String = NumberPrototype.String = Object.String = String;
		ObjectPrototype.Number = NumberPrototype.Number = String.Number = Object.Number = Number;
		NaN = Number.NaN;
		
		PositiveOne = Number.PositiveOne;
		NegativeOne = Number.NegativeOne;
		Zero = Number.Zero;
		
		Symbol.__proto__ = Number.__proto__ = String.__proto__ = Function.__proto__ = Object.__proto__ = Function.prototype();
		
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
		__proto__ = Object.prototype();
		
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
			return new Error.Instance(String, Error, Symbol.iterator, Number, ((Error.JavaException)t).type, ((Error.JavaException) t).getUnderlyingMessage(), JSHelper.extractStack(t.getMessage(), t));
		return new Error.Instance(String, Error, Symbol.iterator, Number, "JavaError", t.toString(), JSHelper.extractStack("JavaError: " + t.toString(), t));
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
