/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

/**
 *
 * @author kate
 */
public abstract class AbstractFunction extends UniqueObject implements BaseFunction {
	
	public AbstractFunction(Global global) {
		this(global.String, global.Object, global.Function, global.NaN);
	}
	public AbstractFunction(Global global, String.Instance name) {
		this(global.String, global.Object, global.Function, global.NaN, name);
	}
	public AbstractFunction(Global global, java.lang.String name) {
		this(global.String, global.Object, global.Function, global.NaN, global.wrap(name));
	}
	public AbstractFunction(String String, Object Object, Function Function, Number.Instance NaN) {
		super(Function.prototype(), Function);
		
		GenericObject prototype = new GenericObject(Object, NaN);
		prototype.setStorage("constructor", this, false);
		setStorage("prototype", prototype, false);
		if(name() != null)
			setStorage("name", String.wrap(name()), false);
	}
	public AbstractFunction(String String, Object Object, Function Function, Number.Instance NaN, java.lang.String name) {
		this(String, Object, Function, NaN, String.wrap(name));
	}
	public AbstractFunction(String String, Object Object, Function Function, Number.Instance NaN, String.Instance name) {
		super(Function.prototype(), Function);
		
		GenericObject prototype = new GenericObject(Object, NaN);
		prototype.setStorage("constructor", this, false);
		setStorage("prototype", prototype, false);
		setStorage("name", name, false);
	}
	protected AbstractFunction() {}
	
	@Override
	protected void init(Global global) {
		init(global.String, global.Object, global.Function, global.NaN);
	}
	protected GenericObject init(String String, Object Object, Function Function, Number.Instance NaN) {
		if(!(this instanceof Function))
			super.init(Function.prototype(), Function);
		
		try {
			return initPrototype(Object, NaN);
		} finally {
			setStorage("name", String.wrap(name()), false);
		}
	}
	
	protected GenericObject initPrototype(Object Object, Number.Instance NaN) {
		GenericObject prototype;
		if(this instanceof Object) {
			prototype = new GenericObject();
			prototype.setStorage("constructor", this, false);
		} else if(NaN != null)
			prototype = new GenericObject(Object, NaN);
		else
			prototype = new GenericObject(Object, (Number)null);
		
		setStorage("prototype", prototype, false);
		if(this instanceof Function)
			super.init(prototype, this);
		return prototype;
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		throw new Error.JavaException("TypeError", toString() + " is not a constructor");
	}

	@Override
	public final GenericObject prototype() {
		return (GenericObject)get("prototype", OR_REFERENCE_ERROR_BO);
	}
	
	@Override
	public java.lang.String arguments() {
		return "";
	}
	
	@Override
	public java.lang.String name() {
		java.lang.String className = getClass().getName().replaceAll("[^a-zA-Z0-9]", "_");
		if(className.startsWith("net_nexustools_njs_"))
			className = className.substring(19);
		return className;
	}
	
	@Override
	public java.lang.String source() {
		return "[java_code]";
	}
	
}
