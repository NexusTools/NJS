/* 
 * Copyright (C) 2016 NexusTools.
 *
 * This library is free software: you can redistribute it and/or modify  
 * it under the terms of the GNU Lesser General Public License as   
 * published by the Free Software Foundation, version 3.0.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Katelyn Slater <ktaeyln@gmail.com>
 */
package net.nexustools.njs;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Katelyn Slater <ktaeyln@gmail.com>
 */
public class GenericObject extends NumberObject {

	public static interface ArrayOverride {
		public BaseObject get(int i, BaseObject _this, Or<BaseObject> or);
		public boolean delete(int i, BaseObject _this, Or<java.lang.Boolean> or);
		public void set(int i, BaseObject _this, BaseObject val, Or<Void> or);
		public boolean has(int i, BaseObject _this);
		public int length(BaseObject _this);
	}
	
	BaseObject __proto__;
	private ArrayOverride arrayOverride;
	private java.lang.Object metaObject;
	private boolean sealed, hasArrayOverride;
	protected final Map<Symbol.Instance, BaseObject> symbols = new HashMap();
	protected final Map<java.lang.String, Property> properties = new HashMap();
	protected Symbol.Instance iterator;
	protected String String;
	
	public GenericObject(Global global) {
		this(global.Object.prototype(), global.Symbol.iterator, global.String, global.Number);
	}
	public GenericObject(BaseFunction constructor, Global global) {
		super(global.Number);
		iterator = global.Symbol.iterator;
		String = global.String;
		init(constructor.prototype());
	}
	public GenericObject(Symbol.Instance iterator, String String) {
		super((Number)null);
		this.iterator = iterator;
		this.String = String;
	}
	public GenericObject(Symbol.Instance iterator, String String, Number Number) {
		super(Number);
		this.iterator = iterator;
		this.String = String;
	}
	public GenericObject(BaseFunction constructor, Symbol.Instance iterator, String String, Number Number) {
		this(constructor.prototype(), iterator, String, Number);
	}
	public GenericObject(BaseObject __proto__, Symbol.Instance iterator, String String, Number Number) {
		super(Number);
		this.iterator = iterator;
		this.String = String;
		init(__proto__);
	}
	protected GenericObject() {
		super((Number)null);
	}
	
	protected final void init(Global global) {
		init(global.Object.prototype());
	}
	protected final void init(Object Object) {
		init(Object.prototype());
	}
	protected final void init(BaseObject __proto__) {
		assert(!Utilities.isUndefined(__proto__));
		this.__proto__ = __proto__;
		if(__proto__ instanceof GenericObject) {
			GenericObject ge = (GenericObject)__proto__;
			if(ge.hasArrayOverride)
				setArrayOverride(ge.arrayOverride);
		}
	}
	
	@Override
	public java.lang.String typeOf() {
		return "object";
	}
	
	public final void setArrayOverride(ArrayOverride override) {
		hasArrayOverride = (arrayOverride = override) != null;
	}

	@Override
	public final void defineGetter(java.lang.String key, BaseFunction impl) {
		Property prop = properties.get(key);
		if(prop == null)
			properties.put(key, prop = new ExtendedProperty());
		else if(!(prop instanceof ExtendedProperty)) {
			ExtendedProperty newProp = new ExtendedProperty();
			newProp.setter = prop.getSetter();
			newProp.value = prop.getValue();
			prop = newProp;
		}
		
		((ExtendedProperty)prop).getter = impl;
	}

	@Override
	public final void defineSetter(java.lang.String key, BaseFunction impl) {
		Property prop = properties.get(key);
		if(prop == null)
			properties.put(key, prop = new ExtendedProperty());
		else if(!(prop instanceof ExtendedProperty)) {
			ExtendedProperty newProp = new ExtendedProperty();
			newProp.setter = prop.getSetter();
			newProp.value = prop.getValue();
			prop = newProp;
		}
		
		((ExtendedProperty)prop).getter = impl;
	}
	
	public final void defineProperty(java.lang.String key, BaseFunction getter, BaseFunction setter) {
		ExtendedProperty property = new ExtendedProperty(false);
		property.getter = getter;
		property.setter = setter;
		setProperty(key, property);
	}
	
	private class OrSet implements Or<Void> {

		final BaseObject val;
		OrSet(BaseObject val) {
			this.val = val;
		}
		
		@Override
		public Void or(java.lang.String key) {
			properties.put(key, new BasicProperty(val));
			return null;
		}
		
	}
	
	@Override
	public void set(int i, BaseObject val) {
		set(i, val, this, sealed ? OR_VOID : new OrSet(val));
	}
	
	@Override
	public void set(int i, BaseObject val, BaseObject _this) {
		set(i, val, _this, sealed ? OR_VOID : new OrSet(val));
	}

	@Override
	public void set(java.lang.String key, BaseObject val) {
		set(key, val, this, sealed ? OR_VOID : new OrSet(val));
	}

	@Override
	public final void set(java.lang.String key, BaseObject val, BaseObject _this) {
		set(key, val, _this, sealed ? OR_VOID : new OrSet(val));
	}

	@Override
	public final void set(java.lang.String key, BaseObject val, Or<Void> or) {
		set(key, val, this, or);
	}

	@Override
	public final void set(java.lang.String key, BaseObject val, BaseObject _this, Or<Void> or) {
		Iterator<Map.Entry<java.lang.String, Property>> it = properties.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<java.lang.String, Property> entry = it.next();
			if(entry.getKey().equals(key)) {
				Property prop = entry.getValue();
				BaseFunction setter = prop.getSetter();
				if(setter == null) {
					if(_this == this)
						prop.set(val);
					else
						continue;
				} else
					setter.call(_this, val);
				return;
			}
		}
		
		if(Utilities.isUndefined(__proto__))
			or.or(key);
		else
			__proto__.set(key, val, _this, or);
	}
	
	public final void setStorage(java.lang.String key, BaseObject value, boolean enumerable) {
		assert(key != null);
		setProperty(key, enumerable ? new BasicProperty(value) : new HiddenProperty(value));
	}
	public final void setHidden(java.lang.String key, BaseObject value) {
		assert(key != null);
		setProperty(key, new HiddenProperty(value));
	}
	public final void setReadOnly(java.lang.String key, BaseObject value) {
		assert(key != null);
		setProperty(key, new ReadOnlyProperty(value));
	}
	
	@Override
	public final boolean setProperty(java.lang.String key, Property property) {
		assert(key != null);
		Iterator<Map.Entry<java.lang.String, Property>> it = properties.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<java.lang.String, Property> entry = it.next();
			if(entry.getKey().equals(key)) {
				Property prop = entry.getValue();
				if(!prop.configurable())
					return false;
				entry.setValue(property);
				return true;
			}
		}
		
		properties.put(key, property);
		return true;
	}

	@Override
	public final void set(int i, BaseObject val, Or<Void> or) {
		set(i, val, this, or);
	}

	@Override
	public final void set(int i, BaseObject val, BaseObject _this, Or<Void> or) {
		assert(val != null);
		
		if(hasArrayOverride)
			arrayOverride.set(i, val, _this, or);
		else
			set(java.lang.String.valueOf(i), val, _this, or);
	}

	@Override
	public final BaseObject get(java.lang.String key, BaseObject _this) {
		return get(key, _this, OR_UNDEFINED);
	}

	@Override
	public final BaseObject get(java.lang.String key, Or<BaseObject> or) {
		return get(key, this, or);
	}

	@Override
	public final BaseObject get(java.lang.String key, BaseObject _this, Or<BaseObject> or) {
		Iterator<Map.Entry<java.lang.String, Property>> it = properties.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<java.lang.String, Property> entry = it.next();
			if(entry.getKey().equals(key)) {
				Property prop = entry.getValue();
				BaseFunction getter = prop.getGetter();
				if(getter == null)
					return prop.get();
				return getter.call(_this);
			}
		}
		
		if(Utilities.isUndefined(__proto__))
			return or.or(key);
		
		return __proto__.get(key, _this, or);
	}

	@Override
	public final BaseObject get(java.lang.String key) {
		return get(key, OR_UNDEFINED);
	}
	
	@Override
	public final BaseObject get(int i) {
		return get(i, this);
	}

	@Override
	public final BaseObject get(int i, BaseObject _this) {
		if(hasArrayOverride)
			return arrayOverride.get(i, _this, OR_UNDEFINED);
		return get(java.lang.String.valueOf(i), _this, OR_UNDEFINED);
	}

	@Override
	public final BaseObject get(int i, Or<BaseObject> or) {
		return get(i, this, or);
	}

	@Override
	public final BaseObject get(int i, BaseObject _this, Or<BaseObject> or) {
		if(hasArrayOverride)
			return arrayOverride.get(i, _this, or);
		
		return get(java.lang.String.valueOf(i),  _this, or);
	}
	
	@Override
	public final boolean delete(int i) {
		if(hasArrayOverride)
			return arrayOverride.delete(i, this, OR_TRUE);
		else
			return delete(java.lang.String.valueOf(i), OR_TRUE);
	}
	
	@Override
	public final boolean delete(int i, Or<java.lang.Boolean> or) {
		if(hasArrayOverride)
			return arrayOverride.delete(i, this, or);
		else
			return delete(java.lang.String.valueOf(i), or);
	}
	
	@Override
	public final boolean delete(java.lang.String key) {
		return delete(key, OR_TRUE);
	}
	
	@Override
	public final boolean delete(java.lang.String key, Or<java.lang.Boolean> or) {
		Iterator<Map.Entry<java.lang.String, Property>> it = properties.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<java.lang.String, Property> entry = it.next();
			if(entry.getKey().equals(key)) {
				if(!entry.getValue().configurable())
					return false;
				
				it.remove();
				return true;
			}
		}
		
		return or.or(key);
	}
	
	public final void setDirectly(java.lang.String key, BaseObject val) {
		setProperty(key, new BasicProperty(val));
	}
	
	public final BaseObject getDirectly(java.lang.String key) {
		return getDirectly(key, OR_NULL);
	}
	
	public final BaseObject getDirectly(java.lang.String key, Or<BaseObject> or) {
		Property prop = properties.get(key);
		if(prop == null)
			return or.or(key);
		
		BaseFunction getter = prop.getGetter();
		if(getter != null)
			return getter.call(this);
		return prop.get();
	}
	
	@Override
	public final Property getProperty(java.lang.String key) {
		return properties.get(key);
	}

	@Override
	public Set<java.lang.String> keys() {
		if(hasArrayOverride)
			return new AbstractSet<java.lang.String>() {
				@Override
				public Iterator<java.lang.String> iterator() {
					final int max = arrayOverride.length(GenericObject.this);
					final Iterator<Map.Entry<java.lang.String, Property>> it = properties.entrySet().iterator();
					return new Iterator<java.lang.String>() {
						int pos;
						java.lang.String next;
						@Override
						public boolean hasNext() {
							if(pos < max) {
								next = java.lang.String.valueOf(pos++);
								return true;
							}
							
							while(it.hasNext()) {
								Map.Entry<java.lang.String, Property> entry = it.next();
								if(entry.getValue().enumerable()) {
									next = entry.getKey();
									return true;
								}
							}
							return false;
						}

						@Override
						public java.lang.String next() {
							return next;
						}
					};
				}

				@Override
				public int size() {
					int size = 0;
					for(Property prop : properties.values())
						if(prop.enumerable())
							size ++;
					return size + arrayOverride.length(GenericObject.this);
				}
			};
		return new AbstractSet<java.lang.String>() {
			@Override
			public Iterator<java.lang.String> iterator() {
				final Iterator<Map.Entry<java.lang.String, Property>> it = properties.entrySet().iterator();
				return new Iterator<java.lang.String>() {
					java.lang.String next;
					@Override
					public boolean hasNext() {
						while(it.hasNext()) {
							Map.Entry<java.lang.String, Property> entry = it.next();
							if(entry.getValue().enumerable()) {
								next = entry.getKey();
								return true;
							}
						}
						return false;
					}

					@Override
					public java.lang.String next() {
						return next;
					}
				};
			}

			@Override
			public int size() {
				int size = 0;
				for(Property prop : properties.values())
					if(prop.enumerable())
						size ++;
				return size;
			}
		};
	}
	
	@Override
	public Set<java.lang.String> ownPropertyNames() {
		if(hasArrayOverride)
			return new AbstractSet<java.lang.String>() {
				@Override
				public Iterator<java.lang.String> iterator() {
					final int max = arrayOverride.length(GenericObject.this);
					final Iterator<java.lang.String> it = properties.keySet().iterator();
					return new Iterator<java.lang.String>() {
						int pos;
						java.lang.String next;
						@Override
						public boolean hasNext() {
							if(pos <= max) {
								next = java.lang.String.valueOf(pos++);
								return true;
							}
							
							return it.hasNext();
						}

						@Override
						public java.lang.String next() {
							if(pos <= max)
								return next;
							return it.next();
						}
					};
				}

				@Override
				public int size() {
					return properties.keySet().size() + arrayOverride.length(GenericObject.this);
				}
			};
		return properties.keySet();
	}
	
	@Override
	public final void seal() {
		sealed = true;
	}

	@Override
	public void setPrototypeOf(BaseObject prototype) {
		__proto__ = prototype;
	}
	
	@Override
	public final BaseObject prototypeOf() {
		return __proto__;
	}
	
	@Override
	public boolean instanceOf(BaseFunction constructor) {
		return __proto__ != null && (__proto__ == constructor.prototype() || __proto__.instanceOf(constructor));
	}
	
	@Override
	public final java.lang.Object getMetaObject() {
		return metaObject;
	}

	@Override
	public final void setMetaObject(java.lang.Object meta) {
		metaObject = meta;
	}
	
	@Override
	public boolean hasOwnProperty(java.lang.String name) {
		if(hasArrayOverride)
			try {
				int value = Integer.valueOf(name);
				if(value >= 0)
					return arrayOverride.has(value, this);
			} catch(NumberFormatException ex) {}
		return properties.containsKey(name);
	}

	@Override
	public final boolean isSealed() {
		return sealed;
	}
	
	@Override
	public final Set<Symbol.Instance> ownSymbols() {
		return symbols.keySet();
	}

	@Override
	public final void set(Symbol.Instance symbol, BaseObject val) {
		symbols.put(symbol, val);
	}
	
	@Override
	public final void delete(Symbol.Instance symbol) {
		symbols.remove(symbol);
	}

	@Override
	public final BaseObject get(Symbol.Instance symbol) {
		return symbols.get(symbol);
	}

	@Override
	public java.lang.String toString() {
		try {
			return ((BaseFunction)get("toString")).call(this).toString();
		} catch(RuntimeException ex) {
			System.err.println(getClass());
			throw ex;
		}
	}

	@Override
	public Iterator<java.lang.String> deepPropertyNameIterator() {
		return new Iterator<java.lang.String>() {
			BaseObject next = GenericObject.this;
			List<java.lang.String> alreadySeen = new ArrayList();
			Iterator<java.lang.String> it;
			java.lang.String _next;
			@Override
			public boolean hasNext() {
				while(true) {
					if(it != null)
						while(it.hasNext()) {
							_next = it.next();
							if(!alreadySeen.contains(_next)) {
								alreadySeen.add(_next);
								return true;
							}
						}
					if(next != null) {
						it = next.keys().iterator();
						next = next.prototypeOf();
					} else
						return false;
				}
			}
			@Override
			public java.lang.String next() {
				return _next;
			}
		};
	}

	@Override
	public Iterator<BaseObject> iterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean toBool() {
		return true;
	}
	
}
