/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author kate
 */
public class Scope implements Scopeable {

	public static class Extended extends Scope {
		private final HashMap<java.lang.String, BaseObject> storage = new HashMap();
		public Extended(BaseObject _this, Global global, Scopeable... scopeables) {
			super(_this, global, scopeables);
		}
		public Extended(BaseObject _this, Scopeable... scopeables) {
			super(_this, scopeables);
		}
	
		@Override
		public void var(java.lang.String key, BaseObject val) {
			storage.put(key, val);
		}

		@Override
		public void set(java.lang.String key, BaseObject val, Or<Void> or) {
			Iterator<Map.Entry<java.lang.String, BaseObject>> it = storage.entrySet().iterator();
			while(it.hasNext()) {
				Map.Entry<java.lang.String, BaseObject> entry = it.next();
				if(entry.getKey().equals(key)) {
					entry.setValue(val);
					return;
				}
			}
			
			super.set(key, val, or);
		}

		@Override
		public BaseObject get(java.lang.String key, Or<BaseObject> or) {
			Iterator<Map.Entry<java.lang.String, BaseObject>> it = storage.entrySet().iterator();
			while(it.hasNext()) {
				Map.Entry<java.lang.String, BaseObject> entry = it.next();
				if(entry.getKey().equals(key))
					return entry.getValue();
			}
			
			return super.get(key, or);
		}

		@Override
		public boolean delete(java.lang.String key, Or<java.lang.Boolean> or) {
			if(storage.containsKey(key))
				return false;
			
			return super.delete(key, or);
		}
		
	}

	private static class NotFound extends RuntimeException {}
	private static final NotFound NOT_FOUND = new NotFound();
	private static class OrNotFound<O> implements BaseObject.Or<O> {
		@Override
		public O or(java.lang.String key) {
			throw NOT_FOUND;
		}
	}
	private static final OrNotFound<Void> OR_NOT_FOUND_V = new OrNotFound();
	private static final OrNotFound<BaseObject> OR_NOT_FOUND_BO = new OrNotFound();
	private static final OrNotFound<java.lang.Boolean> OR_NOT_FOUND_B = new OrNotFound();
	
	public final BaseObject _this;
	public final Scopeable[] scopeables;
	public Scope(BaseObject _this, Scopeable... scopeables) {
		this.scopeables = scopeables;
		this._this = _this;
	}
	public Scope(Global global, Scopeable... scopeables) {
		this(global, global, scopeables);
	}
	public Scope(BaseObject _this, Global global, Scopeable... scopeables) {
		if(scopeables.length == 0)
			this.scopeables = new Scopeable[]{global};
		else {
			this.scopeables = new Scopeable[scopeables.length+1];
			System.arraycopy(scopeables, 0, this.scopeables, 0, scopeables.length);
			this.scopeables[scopeables.length] = global;
		}
		this._this = _this;
	}
	
	public final void var(java.lang.String key) {
		var(key, Undefined.INSTANCE);
	}
	public void var(java.lang.String key, BaseObject val) {
		scopeables[scopeables.length-1].set(key, val);
	}

	@Override
	public final void set(java.lang.String key, BaseObject val) {
		set(key, val, OR_REFERENCE_ERROR_V);
	}

	@Override
	public void set(java.lang.String key, BaseObject val, Or<Void> or) {
		for(Scopeable object : scopeables) {
			try {
				object.set(key, val, OR_NOT_FOUND_V);
				return;
			} catch(NotFound f) {}
		}
		
		or.or(key);
	}

	@Override
	public final BaseObject get(java.lang.String key) {
		return get(key, OR_REFERENCE_ERROR_BO);
	}

	@Override
	public BaseObject get(java.lang.String key, Or<BaseObject> or) {
		if(key.equals("this"))
			return _this;
		
		for(Scopeable object : scopeables) {
			try {
				return object.get(key, OR_NOT_FOUND_BO);
			} catch(NotFound f) {}
		}
		
		return or.or(key);
	}

	@Override
	public final boolean delete(java.lang.String key) {
		return delete(key, OR_TRUE);
	}

	@Override
	public boolean delete(java.lang.String key, Or<java.lang.Boolean> or) {
		for(Scopeable object : scopeables)
			try {
				return object.delete(key, OR_NOT_FOUND_B);
			} catch(NotFound f) {}
		
		return or.or(key);
	}
	
	public final Scope extend() {
		return extend(_this);
	}
	
	public final Scope extend(BaseObject _this) {
		Scopeable[] scopeables = new Scopeable[this.scopeables.length+1];
		System.arraycopy(this.scopeables, 0, scopeables, 1, this.scopeables.length);
		scopeables[0] = this;
		return new Extended(_this, scopeables);
	}

	public final BaseObject resolve(Iterable<java.lang.String> chain) {
		Iterator<java.lang.String> it = chain.iterator();
		if(it.hasNext()) {
			Scopeable obj = this;
			do {
				obj = obj.get(it.next());
			} while(it.hasNext());
			return (BaseObject)obj;
		}
		
		return _this;
	}
	
	private static final ThreadLocal<List<Scope>> SCOPE_STACK = new ThreadLocal<List<Scope>>() {
		@Override
		protected List<Scope> initialValue() {
			return new ArrayList();
		}
	};
	public void enter() {
		List<Scope> stack = SCOPE_STACK.get();
		stack.add(this);
	}
	public void exit() {
		List<Scope> stack = SCOPE_STACK.get();
		assert(stack.remove(stack.size()-1) == this);
	}
	public static Scope getCurrent() {
		List<Scope> stack = SCOPE_STACK.get();
		if(stack.isEmpty())
			return null;
		return stack.get(stack.size()-1);
	}

}
