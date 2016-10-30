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
		this(global.String, global.Object, global.Function);
	}
	public AbstractFunction(Global global, String.Instance name) {
		this(global.String, global.Object, global.Function, name);
	}
	public AbstractFunction(Global global, java.lang.String name) {
		this(global.String, global.Object, global.Function, global.wrap(name));
	}
	public AbstractFunction(String String, Object Object, Function Function) {
		super(Function.prototype(), Function);
		
		GenericObject prototype = new GenericObject(Object);
		prototype.setStorage("constructor", this, false);
		setStorage("prototype", prototype, false);
		if(name() != null)
			setStorage("name", String.wrap(name()), false);
	}
	public AbstractFunction(String String, Object Object, Function Function, java.lang.String name) {
		this(String, Object, Function, String.wrap(name));
	}
	public AbstractFunction(String String, Object Object, Function Function, String.Instance name) {
		super(Function.prototype(), Function);
		
		GenericObject prototype = new GenericObject(Object);
		prototype.setStorage("constructor", this, false);
		setStorage("prototype", prototype, false);
		setStorage("name", name, false);
	}
	protected AbstractFunction() {}
	
	@Override
	protected void init(Global global) {
		init(global.String, global.Object, global.Function);
	}
	protected GenericObject init(String String, Object Object, Function Function) {
		if(!(this instanceof Function))
			super.init(Function.prototype(), Function);
		
		try {
			return initPrototype(Object);
		} finally {
			setStorage("name", String.wrap(name()), false);
		}
	}
	
	protected GenericObject initPrototype(Object Object) {
		GenericObject prototype;
		if(this instanceof Object) {
			prototype = new GenericObject();
			prototype.setStorage("constructor", this, false);
		} else
			prototype = new GenericObject(Object);
		
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
