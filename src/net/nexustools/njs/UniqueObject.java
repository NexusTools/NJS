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
		super(global);
	}
	public UniqueObject(BaseFunction constructor, Global global) {
		super(constructor, global);
	}
	public UniqueObject(Symbol.Instance iterator, String String) {
		super(iterator, String);
	}
	public UniqueObject(BaseFunction constructor, Symbol.Instance iterator, String String, Number Number) {
		super(constructor.prototype(), iterator, String, Number);
	}
	public UniqueObject(BaseObject __proto__, Symbol.Instance iterator, String String, Number Number) {
		super(__proto__, iterator, String, Number);
	}
	protected UniqueObject() {
	}
	@Override
	public int hashCode() {
		return super.hashCode() ^ toString().hashCode();
	}
}
