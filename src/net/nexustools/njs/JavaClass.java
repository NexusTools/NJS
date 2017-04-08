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

import java.util.logging.Level;
import java.util.logging.Logger;
import net.nexustools.njs.compiler.SourceBuilder;

/**
 *
 * @author Katelyn Slater <kate@nexustools.com>
 */
public class JavaClass extends AbstractFunction {

    public JavaClass(final Global global) {
        super(global);

        setHidden("forName", new AbstractFunction(global) {
            @Override
            public BaseObject call(BaseObject _this, BaseObject... params) {
                switch (params.length) {
                    case 1:
                        try {
                            return global.wrap(Class.forName(params[0].toString()));
                        } catch (ClassNotFoundException ex) {
                            throw new Error.JavaException("JavaError", ex.toString(), ex);
                        }
                    case 3:
                        try {
                            return global.wrap(Class.forName(params[0].toString(), params[1].toBool(), (ClassLoader) Utilities.jsToJava(global, params[2], ClassLoader.class)));
                        } catch (ClassNotFoundException ex) {
                            throw new Error.JavaException("JavaError", ex.toString(), ex);
                        }
                }
                throw new Error.JavaException("JavaError", "Invalid arguments");
            }

            @Override
            public java.lang.String name() {
                return "JavaClass_forName";
            }
        });

        ((GenericObject) prototype).setHidden("getClassLoader", new AbstractFunction(global) {
            @Override
            public BaseObject call(BaseObject _this, BaseObject... params) {
                return global.wrap(((JavaClassWrapper) _this).javaClass.getClassLoader());
            }

            @Override
            public java.lang.String name() {
                return "JavaClass_getClassLoader";
            }

        });
    }

    @Override
    public BaseObject construct(BaseObject... params) {
        java.lang.String targetClass = params[0].toString();
        try {
            Class.forName(targetClass);
            throw new RuntimeException(targetClass + " is already implemented...");
        } catch (ClassNotFoundException ex) {}
        Class[] interfaces = new Class[params.length - 2];
        for (int i = 0; i < interfaces.length; i++) {
            java.lang.String _interface = params[i + 1].toString();
            try {
                interfaces[i] = Class.forName(_interface);
                if(!interfaces[i].isInterface())
                    throw new RuntimeException("JavaClass can only implement interfaces, the ability to extend classes is coming in a later release.");
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException("Could not resolve interface `" + _interface + "`", ex);
            }
        }
        BaseObject impl = params[params.length - 1];
        
        SourceBuilder builder = new SourceBuilder();
        builder.appendLicense();
        int lastIndex = targetClass.lastIndexOf(".");
        if(lastIndex > -1) {
            builder.appendln("package " + targetClass.substring(0, lastIndex));
            builder.appendln();
        }

        return Null.INSTANCE;
    }

    @Override
    public BaseObject call(BaseObject _this, BaseObject... params) {
        throw new Error.JavaException("Error", "You must call new on JavaClass");
    }

}
