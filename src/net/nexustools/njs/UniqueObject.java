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
public class UniqueObject extends GenericObject {
	public UniqueObject(Global global) {
		super(global.Object);
	}
	public UniqueObject(Object Object) {
		super(Object.prototype(), Object);
	}
	public UniqueObject(BaseObject __proto__, BaseFunction constructor) {
		super(__proto__, constructor);
	}
	protected UniqueObject() {}
	@Override
	public int hashCode() {
		return super.hashCode() ^ toString().hashCode();
	}
}
