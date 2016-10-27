/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author kate
 */
public class Error extends AbstractFunction implements BaseFunction {
	public static class ConvertedException extends RuntimeException {
		public ConvertedException(java.lang.String message) {
			super(message);
		}
		public ConvertedException(java.lang.String message, Throwable cause) {
			super(message, cause);
		}

		@Override
		public void printStackTrace(final PrintStream s) {
			printStackTrace((Appendable)s);
		}

		@Override
		public void printStackTrace(PrintWriter s) {
			printStackTrace((Appendable)s);
		}
		
		private static void printStackTrace(Appendable a, Throwable t, StackTraceElement[] enclosingTrace, Set<Throwable> dejaVu) throws IOException {
			if (dejaVu.contains(t)) {
				a.append("\t[CIRCULAR REFERENCE:" + t + "]\n");
			} else {
				dejaVu.add(t);
				StackTraceElement[] trace = JSHelper.convertStackTrace(t.getStackTrace());
				int m = trace.length - 1;
				int n = enclosingTrace.length - 1;
				while (m >= 0 && n >=0 && trace[m].equals(enclosingTrace[n])) {
					m--; n--;
				}
				int framesInCommon = trace.length - 1 - m;

				// Print our stack trace
				a.append(t.toString() + '\n');
				for (int i = 0; i <= m; i++)
					a.append("\tat " + trace[i] + '\n');
				if (framesInCommon != 0)
					a.append("\t... " + framesInCommon + " more\n");
			}
		}
		
		public void printStackTrace(Appendable a) {
			Set<Throwable> dejaVu = Collections.newSetFromMap(new IdentityHashMap<Throwable, java.lang.Boolean>());
			dejaVu.add(this);
					
			try {
				StackTraceElement[] enclosing = JSHelper.convertStackTrace(getStackTrace());
				a.append(toString());
				for (int i = 0; i < enclosing.length; i++)
					a.append("\n\tat " + enclosing[i]);
				for(Throwable supressed : getSuppressed()) {
					a.append("\nSuppressed ");
					printStackTrace(a, supressed, enclosing, dejaVu);
				}
				Throwable cause = getCause();
				if(cause != null) {
					a.append("\nCaused by ");
					printStackTrace(a, cause, enclosing, dejaVu);
				}
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
	}
	public static class InvisibleException extends ConvertedException {
		public InvisibleException(java.lang.String message) {
			super(message);
		}
		public InvisibleException(java.lang.String message, Throwable cause) {
			super(message, cause);
		}
	}
	public static class ThrowException extends ConvertedException {
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
	public static class JavaException extends ConvertedException {
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
			if(message != null)
				setHidden("message", String.wrap(this.message = message));
			else
				this.message = message;
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
				BaseObject message = _this.get("message", OR_NULL);
				if(message != null)
					return global.wrap(_this.get("name") + ": " + message);
				return ((BaseFunction)_this.get("toString")).call(_this);
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
