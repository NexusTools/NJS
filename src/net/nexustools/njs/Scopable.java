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

/**
 *
 * @author Katelyn Slater <kate@nexustools.com>
 */
public interface Scopable {

    public static interface Or<R> {

        public R or(java.lang.String key);
    }
    public static final Or<BaseObject> OR_NULL = new Or<BaseObject>() {
        @Override
        public BaseObject or(java.lang.String key) {
            return null;
        }
    };
    public static final Or<BaseObject> OR_UNDEFINED = new Or<BaseObject>() {
        @Override
        public BaseObject or(java.lang.String key) {
            return Undefined.INSTANCE;
        }
    };
    public static final Or<java.lang.Boolean> OR_TRUE = new Or<java.lang.Boolean>() {
        @Override
        public java.lang.Boolean or(java.lang.String key) {
            return true;
        }
    };
    public static final Or<java.lang.Boolean> OR_FALSE = new Or<java.lang.Boolean>() {
        @Override
        public java.lang.Boolean or(java.lang.String key) {
            return false;
        }
    };
    public static final Or<Boolean> OR_REFERENCE_ERROR_B = new Or<Boolean>() {
        @Override
        public Boolean or(java.lang.String key) {
            throw new Error.JavaException("ReferenceError", key + " is not defined");
        }
    };
    public static final Or<BaseObject> OR_REFERENCE_ERROR_BO = new Or<BaseObject>() {
        @Override
        public BaseObject or(java.lang.String key) {
            throw new Error.JavaException("ReferenceError", key + " is not defined");
        }
    };
    public static final Or<Void> OR_REFERENCE_ERROR_V = new Or<Void>() {
        @Override
        public Void or(java.lang.String key) {
            throw new Error.JavaException("ReferenceError", key + " is not defined");
        }
    };
    public static final Or<Void> OR_VOID = new Or<Void>() {
        @Override
        public Void or(java.lang.String key) {
            return null;
        }
    };

    public BaseObject get(java.lang.String key);

    public BaseObject get(java.lang.String key, Or<BaseObject> or);

    public void set(java.lang.String key, BaseObject val);

    public void set(java.lang.String key, BaseObject val, Or<Void> or);

    public boolean delete(java.lang.String key);

    public boolean delete(java.lang.String key, Or<java.lang.Boolean> or);
}
