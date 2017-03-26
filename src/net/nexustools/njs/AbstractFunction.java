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
public abstract class AbstractFunction extends GenericObject implements BaseFunction {

    protected BaseObject prototype;

    public AbstractFunction(Global global, java.lang.String name) {
        super(global.Function, global);

        prototype = createPrototype0();
        if (name != null) {
            setHidden("name", String.wrap(name));
        }
    }

    public AbstractFunction(BaseFunction constructor, Global global) {
        super(constructor, global);

        prototype = createPrototype0();
        java.lang.String name = name();
        if (name != null) {
            setHidden("name", String.wrap(name));
        }
    }

    public AbstractFunction(Global global) {
        super(global.Function, global);

        prototype = createPrototype0();
        java.lang.String name = name();
        if (name != null) {
            setHidden("name", String.wrap(name));
        }
    }

    protected AbstractFunction() {
    }

    public final BaseObject createPrototype0() {
        GenericObject prototype;
        if (__proto__ instanceof GenericObject) {
            prototype = new GenericObject(((BaseFunction) ((GenericObject) __proto__).getDirectly("constructor")).prototype(), iterator, String, Number);
        } else {
            prototype = new GenericObject(((BaseFunction) __proto__.get("constructor")).prototype(), iterator, String, Number);
        }
        prototype.setHidden("constructor", this);
        return prototype;
    }

    @Override
    public final BaseObject createPrototype() {
        GenericObject prototype;
        if (__proto__ instanceof GenericObject) {
            prototype = new GenericObject(((BaseFunction) ((GenericObject) __proto__).getDirectly("constructor")).prototype(), iterator, String, Number);
        } else {
            prototype = new GenericObject(((BaseFunction) __proto__.get("constructor")).prototype(), iterator, String, Number);
        }
        prototype.__proto__ = prototype();
        return prototype;
    }

    @Override
    public BaseObject construct(BaseObject... params) {
        throw new Error.JavaException("TypeError", toString() + " is not a constructor");
    }

    @Override
    public void setPrototype(BaseObject prototype) {
        this.prototype = prototype;
    }

    @Override
    public final BaseObject prototype() {
        return prototype;
    }

    @Override
    public java.lang.String arguments() {
        return "";
    }

    @Override
    public java.lang.String name() {
        java.lang.String className = getClass().getName().replaceAll("[^_a-zA-Z0-9\\xA0-\\uFFFF]", "_");
        if (className.startsWith("net_nexustools_njs_")) {
            className = className.substring(19);
        }
        return className;
    }

    @Override
    public java.lang.String source() {
        return "[java_code]";
    }

}
