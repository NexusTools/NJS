/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

import java.lang.ref.WeakReference;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author kate
 */
public class JavaPackageWrapper implements BaseObject {
	
	private final Global global;
	private final java.lang.String pkg;
	private final BaseFunction valueOf;
	private final BaseFunction toString;
	private final ArrayList<WeakReference<JavaPackageWrapper>> cache = new ArrayList();
	public JavaPackageWrapper(final Global global) {
		this(global, "");
	}
	public JavaPackageWrapper(final Global global, final java.lang.String pkg) {
		this.global = global;
		this.pkg = pkg;
		
		final String.Instance _toString = global.wrap("package " + pkg);
		valueOf = new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return JavaPackageWrapper.this;
			}
		};
		toString = new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return _toString;
			}
		};
	}

	@Override
	public BaseObject __proto__() {
		return Undefined.INSTANCE;
	}

	@Override
	public boolean instanceOf(BaseFunction constructor) {
		return false;
	}

	@Override
	public Set<java.lang.String> keys() {
		return ownPropertyNames();
	}

	@Override
	public Set<java.lang.String> ownPropertyNames() {
		return new AbstractSet<java.lang.String>() {
			@Override
			public Iterator<java.lang.String> iterator() {
				return new Iterator<java.lang.String>() {
					@Override
					public boolean hasNext() {
						return false;
					}
					@Override
					public java.lang.String next() {
						throw new UnsupportedOperationException("Not supported");
					}
				};
			}
			@Override
			public int size() {
				return 0;
			}
		};
	}

	@Override
	public boolean hasOwnProperty(java.lang.String name) {
		return false;
	}

	@Override
	public boolean hasProperty(java.lang.String name, BaseObject _this) {
		return false;
	}

	@Override
	public void defineGetter(java.lang.String key, BaseFunction impl) {
	}

	@Override
	public void defineSetter(java.lang.String key, BaseFunction impl) {
	}

	@Override
	public void set(int i, BaseObject val) {
	}

	@Override
	public void set(int i, BaseObject val, Or<Void> or) {
	}

	@Override
	public void set(int i, BaseObject val, BaseObject _this) {
	}

	@Override
	public void set(int i, BaseObject val, BaseObject _this, Or<Void> or) {
	}

	@Override
	public void set(java.lang.String key, BaseObject val, BaseObject _this) {
	}

	@Override
	public void set(java.lang.String key, BaseObject val, BaseObject _this, Or<Void> or) {
	}

	@Override
	public BaseObject get(int index) {
		return get(index, this, OR_UNDEFINED);
	}

	@Override
	public BaseObject get(int index, Or<BaseObject> or) {
		return get(index, this, or);
	}

	@Override
	public BaseObject get(int index, BaseObject _this) {
		return get(index, _this, OR_UNDEFINED);
	}

	@Override
	public BaseObject get(int index, BaseObject _this, Or<BaseObject> or) {
		return Undefined.INSTANCE;
	}

	@Override
	public BaseObject get(java.lang.String key, BaseObject _this) {
		return get(key, _this, OR_UNDEFINED);
	}

	@Override
	public BaseObject get(java.lang.String key, BaseObject _this, Or<BaseObject> or) {
		if(key.equals("toString"))
			return toString;
		if(key.equals("valueOf"))
			return valueOf;
		if(!pkg.isEmpty()) {
			try {
				return global.wrap(Class.forName(pkg + '.' + key));
			} catch (ClassNotFoundException ex) {}
			return getSubPackage(key);
		}
		try {
			return global.wrap(Class.forName(key));
		} catch (ClassNotFoundException ex) {}
		return getSubPackage(key);
	}
	
	public JavaPackageWrapper getSubPackage(java.lang.String key) {
		if(!pkg.isEmpty())
			key = pkg + '.' + key;
		synchronized(cache) {
			JavaPackageWrapper wrapper;
			Iterator<WeakReference<JavaPackageWrapper>> it = cache.iterator();
			while(it.hasNext()) {
				wrapper = it.next().get();
				if(wrapper == null)
					it.remove();
				if(wrapper.pkg.equals(key))
					return wrapper;
			}
			cache.add(new WeakReference(wrapper = new JavaPackageWrapper(global, key)));
			return wrapper;
		}
	}

	@Override
	public boolean delete(int index) {
		return false;
	}

	@Override
	public boolean delete(int index, Or<java.lang.Boolean> or) {
		return false;
	}

	@Override
	public boolean isSealed() {
		return true;
	}

	@Override
	public void seal() {}

	@Override
	public byte toByte() {
		return 0;
	}

	@Override
	public short toShort() {
		return 0;
	}

	@Override
	public int toInt() {
		return 0;
	}

	@Override
	public long toLong() {
		return 0;
	}

	@Override
	public Number.Instance toNumber() {
		return global.NaN;
	}

	@Override
	public double toDouble() {
		return Double.NaN;
	}

	@Override
	public float toFloat() {
		return 0;
	}

	@Override
	public Set<Symbol.Instance> ownSymbols() {
		return new AbstractSet<Symbol.Instance>() {
			@Override
			public Iterator<Symbol.Instance> iterator() {
				return new Iterator<Symbol.Instance>() {
					@Override
					public boolean hasNext() {
						return false;
					}
					@Override
					public Symbol.Instance next() {
						throw new UnsupportedOperationException("Not supported");
					}
				};
			}
			@Override
			public int size() {
				return 0;
			}
		};
	}

	@Override
	public void set(Symbol.Instance symbol, BaseObject val) {}

	@Override
	public BaseObject get(Symbol.Instance symbol) {
		return Undefined.INSTANCE;
	}

	@Override
	public void delete(Symbol.Instance symbol) {}

	@Override
	public void setMetaObject(java.lang.Object meta) {
	}

	@Override
	public Object getMetaObject() {
		return null;
	}

	@Override
	public BaseObject get(java.lang.String key) {
		return get(key, this, OR_UNDEFINED);
	}

	@Override
	public BaseObject get(java.lang.String key, Or<BaseObject> or) {
		return get(key, this, or);
	}

	@Override
	public void set(java.lang.String key, BaseObject val) {
	}

	@Override
	public void set(java.lang.String key, BaseObject val, Or<Void> or) {
		or.or(key);
	}

	@Override
	public boolean delete(java.lang.String key) {
		return false;
	}

	@Override
	public boolean delete(java.lang.String key, Or<java.lang.Boolean> or) {
		return false;
	}

	@Override
	public java.lang.String typeOf() {
		return "package";
	}

	@Override
	public Iterator<BaseObject> iterator() {
		return new Iterator<BaseObject>() {
			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public BaseObject next() {
				throw new UnsupportedOperationException("Not supported");
			}
		};
	}

	@Override
	public boolean setProperty(java.lang.String key, Property property) {
		return false;
	}

	@Override
	public Property getProperty(java.lang.String key) {
		return new BasicProperty(get(key, this, OR_UNDEFINED));
	}

	@Override
	public Iterator<java.lang.String> deepPropertyNameIterator() {
		return new Iterator<java.lang.String>() {
			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public java.lang.String next() {
				throw new UnsupportedOperationException("Not supported");
			}
		};
	}
	
}
