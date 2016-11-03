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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
					double number = ((Number.Instance)object).value;
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
						return Undefined.INSTANCE;
						
					case 1:
						if(params[0] == Undefined.INSTANCE)
							return Undefined.INSTANCE;
						return global.wrap(stringify(params[0]));
						
					default:
						return global.wrap("undefined");
				}
			}
			@Override
			public java.lang.String name() {
				return "JSON_stringify";
			}
		});
		setHidden("parse", new AbstractFunction(global) {
			final Gson GSON;
			{
				GsonBuilder gsonBuilder = new GsonBuilder();
				gsonBuilder.registerTypeAdapter(BaseObject.class, new JsonDeserializer<BaseObject>() {
					@Override
					public BaseObject deserialize(JsonElement je, Type type, JsonDeserializationContext jdc) throws JsonParseException {
						if(je.isJsonNull())
							return Null.INSTANCE;
						if(je.isJsonPrimitive()) {
							JsonPrimitive primitive = je.getAsJsonPrimitive();
							if(primitive.isBoolean())
								return primitive.getAsBoolean() ? global.Boolean.TRUE : global.Boolean.FALSE;
							if(primitive.isNumber())
								return global.wrap(primitive.getAsDouble());
							if(primitive.isString())
								return global.wrap(primitive.getAsString());
							throw new UnsupportedOperationException(primitive.toString());
						}
						if(je.isJsonObject()) {
							GenericObject go = new GenericObject(global);
							JsonObject jo = je.getAsJsonObject();
							for(Map.Entry<java.lang.String, JsonElement> entry : jo.entrySet()) {
								go.set(entry.getKey(), deserialize(entry.getValue(), type, jdc));
							}
							return go;
						}
						if(je.isJsonArray()) {
							JsonArray ja = je.getAsJsonArray();
							BaseObject[] array = new BaseObject[ja.size()];
							for(int i=0; i<array.length; i++) {
								array[i] = deserialize(ja.get(i), type, jdc);
							}
							return new GenericArray(global, array);
						}
						throw new UnsupportedOperationException(je.toString());
					}
				});
				GSON = gsonBuilder.create();
			}
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				try {
					return GSON.fromJson(params[0].toString(), BaseObject.class);
				} catch(com.google.gson.JsonSyntaxException ex) {
					throw new Error.JavaException("SyntaxError", "Unexpected token", ex);
				}
			}
			@Override
			public java.lang.String name() {
				return "JSON_parse";
			}
		});
	}
	
}
