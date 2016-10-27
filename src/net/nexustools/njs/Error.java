/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

import java.io.IOException;

/**
 *
 * @author kate
 */
public class Error extends AbstractFunction implements BaseFunction {
	public static class InvisibleException extends RuntimeException {
		public InvisibleException(java.lang.String message) {
			super(message);
		}
		public InvisibleException(java.lang.String message, Throwable cause) {
			super(message, cause);
		}
	}
	public static class ThrowException extends RuntimeException {
		public final BaseObject what;
		public ThrowException(BaseObject what) {
			super(what.toString() + " thrown by NJS");
			this.what = what;
		}
		public ThrowException(java.lang.String message, BaseObject what) {
			super(message);
			this.what = what;
		}
		public ThrowException(java.lang.String message, Throwable cause, BaseObject what) {
			super(message, cause);
			this.what = what;
		}
	}
	public static class JavaException extends RuntimeException {
		public final java.lang.String type;
		public JavaException(java.lang.String type, java.lang.String message) {
			super(message);
			this.type = type;
		}

		public JavaException(java.lang.String type, java.lang.String message, Throwable cause) {
			super(message, cause);
			this.type = type;
		}
		
		public java.lang.String getUnderlyingMessage() {
			return super.getMessage();
		}

		@Override
		public java.lang.String getMessage() {
			return type + ": " + super.getMessage();
		}
	}
	
	public static class Instance extends GenericObject {
		public final java.lang.String name, message, stack;
		public Instance(String String, Error Error, java.lang.String name, java.lang.String message, java.lang.String stack) {
			super(Error.prototype(), Error);
			setHidden("name", String.wrap(this.name = name));
			setHidden("message", String.wrap(this.message = message));
			setHidden("stack", String.wrap(this.stack = stack));
		}
	}
	
	private final String String;
	public Error(final Global global) {
		super(global);
		String = global.String;
		
		GenericObject prototype = prototype();
		prototype.setHidden("toString", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(((Instance)_this).message != null)
					return global.wrap(((Instance)_this).name + ": " + ((Instance)_this).message);
				return global.wrap(((Instance)_this).message);
			}
		});
	}

	@Override
	public BaseObject construct(BaseObject... params) {
		if(params.length > 0)
			return new Instance(String, this, "Error", params[0].toString(), JSHelper.convertStack("Error: " + params[0].toString(), new Throwable()));
		return new Instance(String, this, "Error", null, JSHelper.convertStack("Error", new Throwable()));
	}
	
	@Override
	public BaseObject call(BaseObject _this, BaseObject... params) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
}
