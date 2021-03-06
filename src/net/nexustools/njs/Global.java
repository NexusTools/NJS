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

import net.nexustools.njs.compiler.Compiler;
import net.nexustools.njs.compiler.JavaTranspiler;
import net.nexustools.njs.compiler.RuntimeCompiler;
import net.nexustools.njs.compiler.NullCompiler;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Katelyn Slater <kate@nexustools.com>
 */
public class Global extends GenericObject {

    private static boolean SHOW_UNAVAILABLE_MESSAGE = true;

    public static Compiler createCompiler() {
        try {
            return new JavaTranspiler();
        } catch (Throwable t) {
            try {
                RuntimeCompiler compiler = new RuntimeCompiler();
                if (SHOW_UNAVAILABLE_MESSAGE) {
                    SHOW_UNAVAILABLE_MESSAGE = false;
                    System.out.println("JavaCompiler unavailable, falling back to RuntimeCompiler");
                    t.printStackTrace(System.out);
                }
                return compiler;
            } catch (Throwable tt) {
                if (SHOW_UNAVAILABLE_MESSAGE) {
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
    public final String String;
    public final Number Number;
    public final Boolean Boolean;
    public final Symbol Symbol;
    public final Error Error;
    public final Array Array;
    public final RegEx RegEx;

    public final JavaClass JavaClass;

    public final BaseFunction NOOP;

    public final Number.Instance NaN;
    public final Number.Instance PositiveOne;
    public final Number.Instance NegativeOne;
    public final Number.Instance PositiveInfinity;
    public final Number.Instance NegativeInfinity;
    public final Number.Instance Zero;
    
    public final Symbol.Instance java_object;

    public Global() {
        this(createCompiler());
    }

    public Global(Compiler compiler) {
        this.compiler = compiler;

        Number = super.Number = new Number();
        String = super.String = new String();

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
        Symbol.iterator.__proto__ = SymbolPrototype;
        Symbol.unscopables.__proto__ = SymbolPrototype;
        java_object = Symbol.create("java_object");
        Number.initConstants();

        ObjectPrototype.String = NumberPrototype.String = Object.String = Number.String = String;
        ObjectPrototype.Number = NumberPrototype.Number = String.Number = Object.Number = Number.Number = Number;
        NaN = Number.NaN;

        PositiveOne = Number.PositiveOne;
        NegativeOne = Number.NegativeOne;
        PositiveInfinity = Number.PositiveInfinity;
        NegativeInfinity = Number.NegativeInfinity;
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
        RegEx = new RegEx(this);
        JavaClass = new JavaClass(this);

        NOOP = new AbstractFunction() {
            @Override
            public BaseObject call(BaseObject _this, BaseObject... params) {
                return Undefined.INSTANCE;
            }
        };
    }

    public void initStandards() {
        setHidden("constructor", Object);
        __proto__ = Object.prototype();

        setHidden("NaN", Number.NaN);
        setHidden("Infinity", Number.PositiveInfinity);
        setHidden("isNaN", Number.isNaN);
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
        synchronized (CONSTRUCTORS) {
            Iterator<WeakReference<JavaClassWrapper>> it = CONSTRUCTORS.iterator();
            while (it.hasNext()) {
                WeakReference<JavaClassWrapper> ref = it.next();
                JavaClassWrapper constructor = ref.get();
                if (constructor == null) {
                    it.remove();
                } else if (constructor.javaClass == javaClass) {
                    return constructor;
                }
            }

            JavaClassWrapper constructor = new JavaClassWrapper(this, JavaClass, javaClass);
            CONSTRUCTORS.add(new WeakReference(constructor));
            return constructor;
        }
    }

    public BaseObject wrap(Throwable t) {
        if (t instanceof Error.Thrown) {
            return ((Error.Thrown) t).what;
        }
        if (t instanceof Error.JavaException) {
            return new Error.Instance(String, Error, Symbol.iterator, Number, ((Error.JavaException) t).type, ((Error.JavaException) t).getUnderlyingMessage(), Utilities.extractStack(t.getMessage(), t));
        }
        return new Error.Instance(String, Error, Symbol.iterator, Number, "JavaError", t.toString(), Utilities.extractStack("JavaError: " + t.toString(), t));
    }

    public BaseObject wrap(java.lang.Object javaObject) {
        return wrap(javaObject, null);
    }

    private final List<WeakReference<JavaObjectWrapper>> OBJECTS = new ArrayList();
    public BaseObject wrap(java.lang.Object javaObject, Class<?> desiredClass) {
        assert (javaObject != null);
        if (desiredClass == null) {
            desiredClass = javaObject.getClass();
        }
        synchronized(OBJECTS) {
            JavaClassWrapper javaClass = wrap(desiredClass);
            Iterator<WeakReference<JavaObjectWrapper>> it = OBJECTS.iterator();
            while (it.hasNext()) {
                WeakReference<JavaObjectWrapper> ref = it.next();
                JavaObjectWrapper wrapper = ref.get();
                if (wrapper == null) {
                    it.remove();
                } else if (wrapper.javaObject == javaObject && wrapper.get("constructor") == javaClass) {
                    return wrapper;
                }
            }
            JavaObjectWrapper wrapper = new JavaObjectWrapper(javaObject, javaClass, this);
            OBJECTS.add(new WeakReference(wrapper));
            return wrapper;
        }
    }

}
