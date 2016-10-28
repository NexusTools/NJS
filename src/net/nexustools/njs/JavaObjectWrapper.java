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
public class JavaObjectWrapper extends GenericObject {

	public final java.lang.Object javaObject;
	JavaObjectWrapper(java.lang.Object javaObject, JavaClassWrapper constructor) {
		super(constructor.prototype(), constructor);
		this.javaObject = javaObject;
	}
	
}
