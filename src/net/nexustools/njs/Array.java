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
 */
package net.nexustools.njs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.math3.util.FastMath;

/**
 *
 * @author Katelyn Slater <kate@nexustools.com>
 */
public class Array extends AbstractFunction {

	private final Global global;
	public Array(final Global global) {
		super(global);
		this.global = global;
		setHidden("from", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				BaseObject target = params[0];
				int length = target.get("length").toInt();
				if(length < 1)
					return new GenericArray(global);
				
				// TODO: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/from
				
				BaseObject[] copy = new BaseObject[length];
				for(int i=0; i<length; i++)
					copy[i] = target.get(i);
				return new GenericArray(global, copy);
			}
		});
		setHidden("isArray", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return params[0] instanceof GenericArray ? global.Boolean.TRUE : global.Boolean.FALSE;
			}
		});
		setHidden("of", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return new GenericArray(global, params);
			}
		});
		GenericObject prototype = (GenericObject)prototype();
		prototype.setHidden("valueOf", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(_this instanceof GenericArray) {
					switch(((GenericArray)_this).length()) {
						case 0:
							return global.Zero;
						case 1:
							return _this.get(0);
						default:
							return global.wrap(Array.toString(_this));
					}
				}
				
				switch(_this.get("length").toInt()) {
					case 0:
						return global.Zero;
					case 1:
						return _this.get(0);
					default:
						return global.wrap(Array.toString(_this));
				}
			}
		});
		prototype.setHidden("fill", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				int start, end;
				BaseObject value;
				switch(params.length) {
					case 0:
						start = 0;
						value = Undefined.INSTANCE;
						end = _this.get("length").toInt();
						break;
					case 1:
						start = 0;
						value = params[0];
						end = _this.get("length").toInt();
						break;
					case 2:
						value = params[0];
						start = params[1].toInt();
						end = _this.get("length").toInt();
						break;
					default:
						value = params[0];
						start = params[1].toInt();
						end = params[2].toInt();
						break;
				}
				for(; start < end; start++) {
					_this.set(start, value);
				}
				return _this;
			}
		});
		prototype.setHidden("shift", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				int length = _this.get("length").toInt();
				if(length < 1)
					return Undefined.INSTANCE;
				
				BaseObject first = _this.get(0);
				for(int i=1; i<length; i++) {
					_this.set(i-1, _this.get(i));
				}
				_this.set("length", global.wrap(length-1));
				return first;
			}
		});
		prototype.setHidden("reverse", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				int length = _this.get("length").toInt();
				if(length < 2)
					return _this;
				
				int index = length-1;
				int mid = (int)FastMath.ceil((double)length/2.0);
				BaseObject[] buffer = new BaseObject[mid];
				for(int i=0; i<length; i++) {
					if(i >= mid) {
						_this.set(i, buffer[index-i]);
					} else {
						buffer[i] = _this.get(i);
						_this.set(i, _this.get(index-i));
					}
				}
				return _this;
			}
		});
		prototype.setHidden("sort", new AbstractFunction(global) {
			final Comparator<BaseObject> BUILT_IN_COMPARE_FUNCTION = new Comparator<BaseObject>() {
				
				public char charAt(java.lang.String str, int pos) {
					try {
						return str.charAt(pos);
					} catch(IndexOutOfBoundsException ex) {
						return 0;
					}
				}
				
				@Override
				public int compare(BaseObject o1, BaseObject o2) {
					java.lang.String s1 = o1.toString();
					java.lang.String s2 = o2.toString();
					
					int l1 = s1.length();
					int l2 = s2.length();
					if(l1 == l2) {
						for(int i=0; i<l1; i++) {
							char c1 = s1.charAt(i);
							char c2 = s2.charAt(i);
							if(c1 == c2)
								continue;
							
							return c1 > c2 ? 1 : -1;
						}
						return 0;
					} else {
						int len = java.lang.Math.max(l1, l2);
						
						for(int i=0; i<len; i++) {
							char c1 = charAt(s1, i);
							char c2 = charAt(s2, i);
							if(c1 == c2)
								continue;
							
							return c1 > c2 ? 1 : -1;
						}
						
						return 0;
					}
				}
			};
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				final Comparator<BaseObject> comparator;
				if(params.length > 0) {
					final BaseFunction compareFunction = (BaseFunction)params[0];
					comparator = new Comparator<BaseObject>() {
						@Override
						public int compare(BaseObject o1, BaseObject o2) {
							return compareFunction.call(Undefined.INSTANCE, o1, o2).toInt();
						}
					};
				} else
					comparator = BUILT_IN_COMPARE_FUNCTION;
				
				if(_this instanceof GenericArray) {
					GenericArray ga = (GenericArray)_this;
					Arrays.sort(ga.arrayStorage, 0, ga.length(), comparator);
				} else {
					int length = _this.get("length").toInt();
					if(length > 0) {
						BaseObject[] objects = new BaseObject[length];
						for(int i=0; i<length; i++)
							objects[i] = _this.get(i);
						Arrays.sort(objects, comparator);
						for(int i=0; i<length; i++)
							_this.set(i, objects[i]);
					}
				}
				return _this;
			}
		});
		prototype.setHidden("pop", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				int length = _this.get("length").toInt();
				if(length < 1)
					return Undefined.INSTANCE;
				
				int index = length-1;
				BaseObject popped = _this.get(index);
				_this.delete(index);
				_this.set("length", global.wrap(index));
				return popped;
			}
		});
		prototype.setHidden("filter", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				int length = _this.get("length").toInt();
				if(length < 1)
					return new GenericArray(global);
				
				List<BaseObject> copy = new ArrayList();
				BaseFunction filter = (BaseFunction)params[0];
				for(int i=0; i<length; i++) {
					BaseObject value = _this.get(i);
					if(filter.call(Undefined.INSTANCE, value).toBool())
						copy.add(value);
				}
				return new GenericArray(global, copy.toArray(new BaseObject[copy.size()]));
			}
		});
		prototype.setHidden("push", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				_this.set(_this.get("length").toInt(), params[0]);
				return _this.get("length");
			}
		});
		prototype.setHidden("forEach", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				BaseFunction it = (BaseFunction)params[0];
				for(int i=0; i<_this.get("length").toInt(); i++) {
					it.call(Undefined.INSTANCE, _this.get(i));
				}
				return Undefined.INSTANCE;
			}
		});
		prototype.setHidden("toString", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return global.wrap(Array.toString(_this));
			}
		});
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		if(params.length > 1)
			return new GenericArray(global, this, params);
		return new GenericArray(global, this, params.length > 0 ? params[0].toInt() : 0);
	}

	@Override
	public BaseObject call(BaseObject _this, BaseObject... params) {
		if(!_this.instanceOf(this))
			return construct(params);
		return _this;
	}
	
	public static java.lang.String toString(BaseObject _this) {
		StringBuilder builder = new StringBuilder();
		for(int i=0; i<_this.get("length").toInt(); i++) {
			if(i > 0)
				builder.append(',');
			BaseObject value = _this.get(i, OR_NULL);
			if(value != null)
				builder.append(value);
		}
		return builder.toString();
	}
	
}
