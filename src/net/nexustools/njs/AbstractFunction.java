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
	
	private final Object Object;
	public AbstractFunction(Global global, java.lang.String name) {
		super(global.Function, global);
		this.Object = global.Object;
		
		setHidden("prototype", create());
		if(name != null)
			setHidden("name", String.wrap(name));
	}
	public AbstractFunction(Global global) {
		super(global.Function, global);
		this.Object = global.Object;
		
		setHidden("prototype", create());
		java.lang.String name = name();
		if(name != null)
			setHidden("name", String.wrap(name));
	}
	protected AbstractFunction(Object Object) {
		this.Object = Object;
	}
	protected AbstractFunction() {
		this.Object = null;
	}

	@Override
	public BaseObject create() {
		GenericObject prototype;
		if(number == null)
			prototype = new GenericObject(iterator, String);
		else
			prototype = new GenericObject(iterator, String, number);
		prototype.constructor = constructor;
		return prototype;
	}
	
	protected GenericObject initPrototype(Object Object, Global global) {
		GenericObject prototype;
		if(this instanceof Object) {
			prototype = new GenericObject();
			prototype.setStorage("constructor", this, false);
		} else if(global != null)
			prototype = new GenericObject(Object, global);
		else
			prototype = new GenericObject();
		
		setStorage("prototype", prototype, false);
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
