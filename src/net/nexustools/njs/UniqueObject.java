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
	public UniqueObject(BaseFunction constructor, Symbol.Instance iterator, String String, Number.Instance number) {
		super(constructor.prototype(), constructor, iterator, String, number);
	}
	public UniqueObject(BaseFunction constructor, Symbol.Instance iterator, String String, Number Number) {
		super(constructor.prototype(), constructor, iterator, String, Number);
	}
	public UniqueObject(BaseFunction constructor, Global global) {
		super(constructor, global);
	}
	public UniqueObject(BaseObject __proto__, BaseFunction constructor, Global global, Number.Instance number) {
		super(__proto__, constructor, global, number);
	}
	public UniqueObject(BaseObject __proto__, BaseFunction constructor, Symbol.Instance iterator, String String, Number.Instance number) {
		super(__proto__, constructor, iterator, String, number);
	}
	public UniqueObject(BaseObject __proto__, BaseFunction constructor, Symbol.Instance iterator, String String, Number Number) {
		super(__proto__, constructor, iterator, String, Number);
	}
	public UniqueObject(BaseFunction constructor, Global global, Number.Instance number) {
		super(constructor, global, number);
	}
	protected UniqueObject() {
		super();
	}
	@Override
	public int hashCode() {
		return super.hashCode() ^ toString().hashCode();
	}
}
