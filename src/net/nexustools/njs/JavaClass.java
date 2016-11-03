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
 * @author Katelyn Slater <ktaeyln@gmail.com>
 */
public class JavaClass extends AbstractFunction {
    
    public JavaClass(final Global global) {
        super(global);
        
        setHidden("forName", new AbstractFunction(global) {
            @Override
            public BaseObject call(BaseObject _this, BaseObject... params) {
				switch(params.length) {
					case 1:
						try {
							return global.wrap(Class.forName(params[0].toString()));
						} catch (ClassNotFoundException ex) {
							throw new Error.JavaException("JavaError", ex.toString(), ex);
						}
					case 3:
						try {
							return global.wrap(Class.forName(params[0].toString(), params[1].toBool(), (ClassLoader)Utilities.jsToJava(params[2], ClassLoader.class)));
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
		
        GenericObject prototype = (GenericObject)prototype();
        prototype.setHidden("getClassLoader", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return global.wrap(((JavaClassWrapper)_this).javaClass.getClassLoader());
			}

			@Override
			public java.lang.String name() {
				return super.name(); //To change body of generated methods, choose Tools | Templates.
			}
			
		});
    }

    @Override
    public BaseObject construct(BaseObject... params) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BaseObject call(BaseObject _this, BaseObject... params) {
	throw new Error.JavaException("Error", "You must call new on JavaClass");
    }
    
}
