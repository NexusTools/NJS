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
	public Error(Global global) {
		super(global);
	}
	
	@Override
	public BaseObject call(BaseObject _this, BaseObject... params) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
}
