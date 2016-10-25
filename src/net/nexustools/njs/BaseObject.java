/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

import java.util.Set;

/**
 *
 * @author kate
 */
public interface BaseObject extends Scopeable {
	public BaseObject __proto__();
	public BaseFunction constructor();
	public boolean instanceOf(BaseFunction constructor);
	
	public Set<java.lang.String> keys();
	public Set<java.lang.String> ownPropertyNames();
	public boolean hasProperty(java.lang.String name);
	public void defineGetter(java.lang.String key, BaseFunction impl);
	public void defineSetter(java.lang.String key, BaseFunction impl);
	public void set(int i, BaseObject val);
	public void set(int i, BaseObject val, Or<Void> or);
	public BaseObject get(int index);
	public BaseObject get(int index, Or<BaseObject> or);
	public boolean delete(java.lang.String key);
	public boolean delete(int index);
	public boolean delete(int index, Or<java.lang.Boolean> or);
	public boolean isSealed();
	public void seal();
	
	public Set<Symbol.Instance> ownSymbols();
	public void set(Symbol.Instance symbol, BaseObject val);
	public BaseObject get(Symbol.Instance symbol);
	public void delete(Symbol.Instance symbol);
	
	public void setMetaObject(java.lang.Object meta);
	public java.lang.Object getMetaObject();
}
