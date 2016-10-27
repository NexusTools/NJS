/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author kate
 */
public class GenericObject implements BaseObject {

	public static interface ArrayOverride {
		public BaseObject get(int i, BaseObject _this, Or<BaseObject> or);
		public boolean delete(int i, BaseObject _this, Or<java.lang.Boolean> or);
		public void set(int i, BaseObject _this, BaseObject val);
		public boolean has(int i, BaseObject _this);
		public int length(BaseObject _this);
	}
	public static interface Property {
		public BaseObject get(BaseObject _this);
		public BaseObject getValue();
		public BaseFunction getSetter();
		public BaseFunction getGetter();
		public void set(BaseObject val, BaseObject _this);
		public boolean configurable();
		public boolean enumerable();
	}
	public static class BasicProperty implements Property {
		public BaseObject value;
		public BasicProperty() {}
		public BasicProperty(BaseObject val) {
			value = val;
		}
		@Override
		public final BaseObject get(BaseObject _this) {
			return value;
		}
		@Override
		public final BaseObject getValue() {
			return value;
		}
		@Override
		public final void set(BaseObject val, BaseObject _this) {
			value = val;
		}
		@Override
		public final BaseFunction getSetter() {
			return null;
		}
		@Override
		public final BaseFunction getGetter() {
			return null;
		}
		@Override
		public final boolean configurable() {
			return true;
		}
		@Override
		public boolean enumerable() {
			return true;
		}
	}
	public static final class HiddenProperty extends BasicProperty {
		public HiddenProperty() {}
		public HiddenProperty(BaseObject val) {
			super(val);
		}
		@Override
		public final boolean enumerable() {
			return false;
		}
	}
	public static final class ReadOnlyProperty implements Property {
		final BaseObject value;
		public ReadOnlyProperty(BaseObject val) {
			value = val;
		}
		@Override
		public final boolean enumerable() {
			return false;
		}

		@Override
		public BaseObject get(BaseObject _this) {
			return value;
		}

		@Override
		public BaseObject getValue() {
			return value;
		}

		@Override
		public BaseFunction getSetter() {
			return null;
		}

		@Override
		public BaseFunction getGetter() {
			return null;
		}

		@Override
		public void set(BaseObject val, BaseObject _this) {
		}

		@Override
		public boolean configurable() {
			return false;
		}
	}
	public static class ExtendedProperty implements Property {
		public BaseObject value;
		public BaseFunction getter;
		public BaseFunction setter;
		public boolean configurable;
		public boolean enumerable;
		public ExtendedProperty() {
			this(true);
		}
		public ExtendedProperty(boolean enumerable) {
			this.enumerable = enumerable;
		}
		@Override
		public final BaseObject get(BaseObject _this) {
			if(getter != null)
				return getter.call(_this);
			return value;
		}
		@Override
		public final BaseObject getValue() {
			return value;
		}
		@Override
		public final void set(BaseObject val, BaseObject _this) {
			if(setter != null)
				setter.call(_this, val);
			else
				value = val;
		}
		@Override
		public final BaseFunction getSetter() {
			return setter;
		}
		@Override
		public final BaseFunction getGetter() {
			return getter;
		}
		@Override
		public final boolean configurable() {
			return configurable;
		}
		@Override
		public final boolean enumerable() {
			return enumerable;
		}
	}
	
	private ArrayOverride arrayOverride;
	private java.lang.Object metaObject;
	private boolean sealed, hasArrayOverride;
	protected final Map<Symbol.Instance, BaseObject> symbols = new HashMap();
	protected final Map<java.lang.String, Property> properties = new HashMap();
	
	public GenericObject(Global global) {
		this(global.Object);
	}
	public GenericObject(Object Object) {
		this(Object.prototype(), Object);
	}
	public GenericObject(BaseObject __proto__, BaseFunction constructor) {
		init(__proto__, constructor);
	}
	protected GenericObject() {}
	
	protected void init(Global global) {
		init(global.Object);
	}
	protected void init(Object Object) {
		init(Object.prototype(), Object);
	}
	protected final void init(BaseObject __proto__, BaseFunction constructor) {
		assert(!JSHelper.isUndefined(__proto__));
		setHidden("constructor", constructor);
		setHidden("__proto__", __proto__);
		
		if(__proto__ instanceof GenericObject) {
			GenericObject ge = (GenericObject)__proto__;
			if(ge.hasArrayOverride)
				setArrayOverride(ge.arrayOverride);
		}
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
	
	@Override
	public void set(int i, BaseObject val) {
		set(i, val, this, OR_VOID);
	}
	
	@Override
	public void set(int i, BaseObject val, BaseObject _this) {
		set(i, val, _this, OR_VOID);
	}

	@Override
	public void set(java.lang.String key, BaseObject val) {
		set(key, val, OR_VOID);
	}

	@Override
	public final void set(java.lang.String key, BaseObject val, BaseObject _this) {
		set(key, val, _this, OR_VOID);
	}

	@Override
	public final void set(java.lang.String key, BaseObject val, Or<Void> or) {
		set(key, val, this, or);
	}

	@Override
	public final void set(java.lang.String key, BaseObject val, BaseObject _this, Or<Void> or) {
		if(hasArrayOverride) {
			while(true) {
				int i;
				try {
					i = JSHelper.toArrayIndex(key);
				} catch(NumberFormatException ex) {
					break;
				}
				arrayOverride.set(i, val, _this);
				return;
			}
		}
		
		assert(key != null);
		Iterator<Map.Entry<java.lang.String, Property>> it = properties.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<java.lang.String, Property> entry = it.next();
			if(entry.getKey().equals(key)) {
				entry.getValue().set(val, _this);
			}
		}
		if(sealed)
			return;
		
		properties.put(key, new BasicProperty(val));
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
	
	public final void setProperty(java.lang.String key, Property property) {
		assert(key != null);
		Iterator<Map.Entry<java.lang.String, Property>> it = properties.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<java.lang.String, Property> entry = it.next();
			if(entry.getKey().equals(key)) {
				entry.setValue(property);
			}
		}
		
		properties.put(key, property);
	}

	@Override
	public final void set(int i, BaseObject val, Or<Void> or) {
		set(i, val, this, or);
	}

	@Override
	public final void set(int i, BaseObject val, BaseObject _this, Or<Void> or) {
		if(hasArrayOverride)
			arrayOverride.set(i, val, _this);
		
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
		if(hasArrayOverride)
			while(true) {
				int i;
				try {
					i = JSHelper.toArrayIndex(key);
				} catch(NumberFormatException ex) {
					break;
				}
				return arrayOverride.get(i, _this, or);
			}
		
		if(key.equals("__proto__")) {
			Iterator<Map.Entry<java.lang.String, Property>> it = properties.entrySet().iterator();
			while(it.hasNext()) {
				Map.Entry<java.lang.String, Property> entry = it.next();
				if(entry.getKey().equals(key))
					return entry.getValue().get(_this);
			}
		} else {
			BaseObject __proto__ = null;
			Iterator<Map.Entry<java.lang.String, Property>> it = properties.entrySet().iterator();
			while(it.hasNext()) {
				Map.Entry<java.lang.String, Property> entry = it.next();
				java.lang.String k = entry.getKey();
				if(k.equals(key))
					return entry.getValue().get(_this);
				else if(k.equals("__proto__"))
					__proto__ = entry.getValue().get(_this);
			}
			
			if(!JSHelper.isUndefined(__proto__))
				return __proto__.get(key, _this, or);
		}
		
		return or.or(key);
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
		if(hasArrayOverride)
			while(true) {
				int i;
				try {
					i = JSHelper.toArrayIndex(key);
				} catch(NumberFormatException ex) {
					break;
				}
				return arrayOverride.delete(i, this, or);
			}
		
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
	
	public final BaseObject getDirectly(java.lang.String key) {
		return getDirectly(key, OR_UNDEFINED);
	}
	
	public final BaseObject getDirectly(java.lang.String key, Or<BaseObject> or) {
		if(hasArrayOverride)
			while(true) {
				int i;
				try {
					i = JSHelper.toArrayIndex(key);
				} catch(NumberFormatException ex) {
					break;
				}
				return arrayOverride.get(i, this, or);
			}
		
		return properties.get(key).get(this);
	}
	
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
	public final BaseObject __proto__() {
		return get("__proto__", OR_NULL);
	}

	@Override
	public final BaseFunction constructor() {
		return (BaseFunction)get("constructor", OR_NULL);
	}
	
	@Override
	public final boolean instanceOf(BaseFunction constructor) {
		if(constructor() == constructor)
			return true;
		
		BaseObject __proto__ = __proto__();
		return __proto__ != null && __proto__.instanceOf(constructor);
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
	public boolean hasProperty(java.lang.String name, BaseObject _this) {
		if(hasArrayOverride)
			try {
				if(arrayOverride.has(JSHelper.toArrayIndex(name), _this))
					return true;
			} catch(NumberFormatException ex) {}
		return properties.containsKey(name);
	}
	
	@Override
	public boolean hasProperty(java.lang.String name) {
		if(hasArrayOverride)
			try {
				if(arrayOverride.has(JSHelper.toArrayIndex(name), this))
					return true;
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
		return ((BaseFunction)get("toString")).call(this).toString();
	}
	
}
