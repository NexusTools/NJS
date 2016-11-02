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
	
	protected BaseObject prototype;
	public AbstractFunction(Global global, java.lang.String name) {
		super(global.Function, global);
		
		prototype = create0();
		if(name != null)
			setHidden("name", String.wrap(name));
	}
	public AbstractFunction(Global global) {
		super(global.Function, global);
		
		prototype = create0();
		java.lang.String name = name();
		if(name != null)
			setHidden("name", String.wrap(name));
	}
	protected AbstractFunction() {
	}

	public final GenericObject create0() {
		GenericObject prototype = new GenericObject(((BaseFunction)get("constructor")).prototype(), iterator, String, Number);
		prototype.setHidden("constructor", this);
		return prototype;
	}

	@Override
	public BaseObject create() {
		GenericObject prototype = create0();
		prototype.__proto__ = prototype();
		return prototype;
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		throw new Error.JavaException("TypeError", toString() + " is not a constructor");
	}

	@Override
	public void setPrototype(BaseObject prototype) {
		this.prototype = prototype;
	}

	@Override
	public final BaseObject prototype() {
		return prototype;
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
