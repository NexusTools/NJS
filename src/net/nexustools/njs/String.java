/* 
 * Copyright (C) 2017 NexusTools.
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Katelyn Slater <kate@nexustools.com>
 */
public class String extends AbstractFunction {

    public static class Instance extends GenericObject {

        public final boolean _const;
        public final java.lang.String string;

        Instance(Global global, final java.lang.String string, boolean _const) {
            super(global.String, global);
            this.string = string;

            setReadOnly("length", Number.wrap(string.length()));
            this._const = _const;
        }

        Instance(Number.Instance length, Number Number, Symbol.Instance iterator, String String, final java.lang.String string) {
            super(String.prototype(), iterator, String, Number);
            this.string = string;

            setReadOnly("length", length);
            _const = false;
        }

        @Override
        public boolean equals(java.lang.Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj instanceof String.Instance) {
                return string.equals(((String.Instance) obj).string);
            }

            if (obj instanceof java.lang.String) {
                return string.equals((java.lang.String) obj);
            }

            if (obj instanceof Number.Instance) {
                try {
                    return Double.valueOf(string) == ((Number.Instance) obj).value;
                } catch (NumberFormatException ex) {
                }
            }

            return false;
        }

        @Override
        public int hashCode() {
            return string.hashCode();
        }

        @Override
        public Instance clone() {
            return new Instance((Number.Instance) getDirectly("length"), Number, iterator, String, string);
        }

        @Override
        public Instance _toString() {
            return this;
        }

        @Override
        public java.lang.String toString() {
            return string;
        }

        @Override
        public boolean toBool() {
            return string.length() > 0;
        }

        @Override
        public java.lang.String typeOf() {
            return _const ? "string" : "object";
        }

        @Override
        public boolean strictEquals(java.lang.Object obj) {
            if (obj == this) {
                return true;
            }

            return _const && obj instanceof Instance
                    && ((Instance) obj)._const && string.equals(((Instance) obj).string);
        }

    }

    protected Global global;

    public String() {
        this.String = this;
    }

    protected void initPrototypeFunctions(Global global) {
        this.global = global;
        GenericObject prototype = (GenericObject) prototype();
        prototype.setHidden("substring", new AbstractFunction(global) {
            @Override
            public BaseObject call(BaseObject _this, BaseObject... params) {
                if (params.length > 1) {
                    return wrap(((Instance) _this).string.substring(params[0].toInt(), params[1].toInt()));
                } else {
                    return wrap(((Instance) _this).string.substring(params[0].toInt()));
                }
            }

            @Override
            public java.lang.String name() {
                return "String_prototype_substring";
            }
        });
        prototype.setHidden("match", new AbstractFunction(global) {
            @Override
            public BaseObject call(BaseObject _this, BaseObject... params) {
                Pattern pattern;
                try {
                    pattern = (Pattern)((JavaObjectHolder)params[0].get(String.this.global.RegEx.pattern)).javaObject;
                } catch(ClassCastException ex) {
                    pattern = Pattern.compile(params[0].toString());
                }
                
                Matcher matcher = pattern.matcher(_this.toString());
                if(matcher.matches()) {
                    GenericArray array = new GenericArray(String.this.global, matcher.groupCount()+1);
                    for(int i=0; i<=matcher.groupCount(); i++)
                        array.put0(i, String.this.wrap(matcher.group(i)));
                    System.out.println(array);
                    return array;
                } else
                    return Null.INSTANCE;
            }
        });
        prototype.setHidden("indexOf", new AbstractFunction(global) {
            @Override
            public BaseObject call(BaseObject _this, BaseObject... params) {
                return Number.wrap(((Instance) _this).string.indexOf(params[0].toString()));
            }

            @Override
            public java.lang.String name() {
                return "String_prototype_indexOf";
            }
        });
        prototype.setHidden("toString", new AbstractFunction(global) {
            @Override
            public BaseObject call(BaseObject _this, BaseObject... params) {
                return _this;
            }

            @Override
            public java.lang.String name() {
                return "String_prototype_toString";
            }
        });
        setHidden("fromCharCode", new AbstractFunction(global) {
            @Override
            public BaseObject call(BaseObject _this, BaseObject... params) {
                return wrap(java.lang.String.valueOf((char) params[0].toInt()));
            }

            @Override
            public java.lang.String name() {
                return "String_prototype_fromCharCode";
            }
        });
        prototype.setHidden("toUpperCase", new AbstractFunction(global) {
            @Override
            public BaseObject call(BaseObject _this, BaseObject... params) {
                return String.this.wrap(_this.toString().toUpperCase());
            }

            @Override
            public java.lang.String name() {
                return "String_prototype_toUpperCase"; //To change body of generated methods, choose Tools | Templates.
            }
        });
        prototype.setHidden("toLowerCase", new AbstractFunction(global) {
            @Override
            public BaseObject call(BaseObject _this, BaseObject... params) {
                return String.this.wrap(_this.toString().toLowerCase());
            }

            @Override
            public java.lang.String name() {
                return "String_prototype_toLowerCase"; //To change body of generated methods, choose Tools | Templates.
            }
        });
        prototype.setHidden("charCodeAt", new AbstractFunction(global) {
            @Override
            public BaseObject call(BaseObject _this, BaseObject... params) {
                return Number.wrap(((Instance) _this).string.charAt(params.length > 0 ? params[0].toInt() : 0));
            }

            @Override
            public java.lang.String name() {
                return "String_prototype_charCodeAt";
            }
        });
        prototype.setHidden("replace", new AbstractFunction(global) {
            @Override
            public BaseObject call(BaseObject _this, BaseObject... params) {
                return String.this.wrap(_this.toString().replaceAll(params[0].toString(), params[1].toString()));
            }

            @Override
            public java.lang.String name() {
                return "String_prototype_replace";
            }
        });
        prototype.setArrayOverride(new ArrayOverride() {
            @Override
            public BaseObject get(int i, BaseObject _this, Or<BaseObject> or) {
                assert (i >= 0);
                if (i >= ((Instance) _this).string.length()) {
                    return or.or(java.lang.String.valueOf(i));
                }

                return wrap(((Instance) _this).string.substring(i, i + 1));
            }

            @Override
            public boolean delete(int i, BaseObject _this, Or<java.lang.Boolean> or) {
                assert (i >= 0);
                if (i >= ((Instance) _this).string.length()) {
                    return or.or(java.lang.String.valueOf(i));
                }
                return false;
            }

            @Override
            public void set(int i, BaseObject val, BaseObject _this, Or<Void> or) {
            }

            @Override
            public boolean has(int i, BaseObject _this) {
                assert (i >= 0);
                return i < ((Instance) _this).string.length();
            }

            @Override
            public int length(BaseObject _this) {
                return ((Instance) _this).string.length();
            }
        });
    }

    @Override
    public BaseObject construct(BaseObject... params) {
        BaseObject val = params[0];
        if (val instanceof Instance) {
            return ((Instance) val).clone();
        }

        return new Instance(global, val.toString(), false);
    }

    @Override
    public BaseObject call(BaseObject _this, BaseObject... params) {
        if (!_this.instanceOf(this)) {
            return construct(params);
        }
        return _this;
    }
    
    public Instance wrap(java.lang.String string) {
        return new Instance(global, string, true);
    }

    public String.Instance from(BaseObject param) {
        if (param instanceof Instance) {
            return (Instance) param;
        }

        return wrap(param.toString());
    }
}
