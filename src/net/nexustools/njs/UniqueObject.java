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
		super(global.Object, global.NaN);
	}
	public UniqueObject(Global global, Number.Instance number) {
		super(global.Object, number);
	}
	public UniqueObject(Global global, Number Number) {
		super(global.Object, Number);
	}
	public UniqueObject(Object Object) {
		super(Object.prototype(), Object);
	}
	public UniqueObject(BaseObject __proto__, BaseFunction constructor) {
		super(__proto__, constructor);
	}
	public UniqueObject(BaseObject __proto__, BaseFunction constructor, Number.Instance number) {
		super(__proto__, constructor, number);
	}
	public UniqueObject(BaseObject __proto__, BaseFunction constructor, Number Number) {
		super(__proto__, constructor, Number);
	}
	protected UniqueObject() {}
	@Override
	public int hashCode() {
		return super.hashCode() ^ toString().hashCode();
	}
}
