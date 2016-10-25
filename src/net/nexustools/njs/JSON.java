/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

import java.util.Iterator;

/**
 *
 * @author kate
 */
public class JSON extends GenericObject {
	
	public JSON(final Global global) {
		super(global);
		setHidden("stringify", new AbstractFunction(global) {
			
			public java.lang.String stringify(BaseObject object) {
				StringBuilder builder = new StringBuilder();
				stringify(object, builder);
				return builder.toString();
			}
			public void stringify(BaseObject object, StringBuilder builder) {
				if(JSHelper.isUndefined(object)) {
					builder.append("null");
					return;
				}
				
				BaseObject toJSON = object.get("toJSON", OR_NULL);
				if(toJSON != null)
					stringify0(((BaseFunction)toJSON).call(object), builder);
				else
					stringify0(object, builder);
			}
			public void stringify0(BaseObject object, StringBuilder builder) {
				if(object instanceof GenericArray) {
					builder.append('[');
					if(((GenericArray)object).length() > 0) {
						stringify(object.get(0), builder);
						for(int i=1; i<((GenericArray)object).length(); i++) {
							builder.append(',');
							stringify(object.get(i), builder);
						}
					}
					builder.append(']');
				} else if(object instanceof String.Instance) {
					builder.append('"');
					builder.append(object.toString());
					builder.append('"');
				} else if(object instanceof Number.Instance) {
					double number = ((Number.Instance)object).number;
					if(Double.isNaN(number) || Double.isInfinite(number))
						builder.append("null");
					
					builder.append(object.toString());
				} else if(object instanceof Boolean.Instance)
					builder.append(object.toString());
				else {
					builder.append('{');
					Iterator<java.lang.String> it = object.keys().iterator();
					if(it.hasNext()) {
						
						java.lang.String key = it.next();
						builder.append('"');
						builder.append(key);
						builder.append("\":");
						stringify(object.get(key), builder);
						
						if(it.hasNext()) {
							do {
								builder.append(',');
								key = it.next();
								builder.append('"');
								builder.append(key);
								builder.append("\":");
								stringify(object.get(key), builder);
							} while(it.hasNext());
						}
					}
					builder.append('}');
				}
			}
			
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				switch(params.length) {
					case 0:
						return global.wrap("undefined");
						
					case 1:
						return global.wrap(stringify(params[0]));
						
					default:
						return global.wrap("undefined");
				}
			}
			@Override
			protected java.lang.String toStringName() {
				return "JSON_stringify";
			}
		});
		setHidden("parse", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
			}
			@Override
			protected java.lang.String toStringName() {
				return "JSON_parse";
			}
		});
	}
	
}
