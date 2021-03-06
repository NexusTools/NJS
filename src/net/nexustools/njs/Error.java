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

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 *
 * @author Katelyn Slater <kate@nexustools.com>
 */
public class Error extends AbstractFunction implements BaseFunction {

    public static class ConvertedException extends RuntimeException {

        public ConvertedException(java.lang.String message) {
            super(message);
        }

        public ConvertedException(java.lang.String message, Throwable cause) {
            super(message, cause);
        }

        public void printOriginalStackTrace() {
            super.printStackTrace(System.err);
        }

        @Override
        public void printStackTrace(final PrintStream s) {
            printStackTrace((Appendable) s);
        }

        @Override
        public void printStackTrace(PrintWriter s) {
            printStackTrace((Appendable) s);
        }

        private static void printStackTrace(Appendable a, Throwable t, StackTraceElement[] enclosingTrace, Set<Throwable> dejaVu) throws IOException {
            if (dejaVu.contains(t)) {
                a.append("\t[CIRCULAR REFERENCE:" + t + "]\n");
            } else {
                dejaVu.add(t);
                StackTraceElement[] trace = Utilities.convertStackTrace(t.getStackTrace());
                int m = trace.length - 1;
                int n = enclosingTrace.length - 1;
                while (m >= 0 && n >= 0 && trace[m].equals(enclosingTrace[n])) {
                    m--;
                    n--;
                }
                int framesInCommon = trace.length - 1 - m;

                // Print our stack trace
                a.append(t.toString() + '\n');
                for (int i = 0; i <= m; i++) {
                    a.append("\tat " + toString(trace[i]) + '\n');
                }
                if (framesInCommon != 0) {
                    a.append("\t... " + framesInCommon + " more\n");
                }
            }
        }

        public static java.lang.String toString(StackTraceElement el) {
            if (el.getClassName().equals("")) {
                StringBuilder builder = new StringBuilder();
                java.lang.String method = el.getMethodName();
                boolean hasMethod = method != null && !method.isEmpty();
                if (hasMethod) {
                    builder.append(method);
                    builder.append(" (");
                }

                java.lang.String fileName = el.getFileName();
                if (fileName == null) {
                    builder.append("<unknown source>");
                } else {
                    builder.append(fileName);
                    int lineNumber = el.getLineNumber();
                    if (lineNumber > 0) {
                        builder.append(':');
                        builder.append(lineNumber);
                    }
                }

                if (hasMethod) {
                    builder.append(')');
                }

                return builder.toString();
            } else {
                return el.toString();
            }
        }

        public void printStackTrace(Appendable a) {
            printStackTrace(this, a);
        }

        public static void printStackTrace(Throwable t, Appendable a) {
            Set<Throwable> dejaVu = Collections.newSetFromMap(new IdentityHashMap<Throwable, java.lang.Boolean>());
            dejaVu.add(t);

            try {
                StackTraceElement[] enclosing = Utilities.convertStackTrace(t.getStackTrace());
                a.append(t.toString());
                for (int i = 0; i < enclosing.length; i++) {
                    a.append("\n\tat " + toString(enclosing[i]));
                }
                for (Throwable supressed : t.getSuppressed()) {
                    a.append("\nSuppressed ");
                    printStackTrace(a, supressed, enclosing, dejaVu);
                }
                Throwable cause = t.getCause();
                if (cause != null) {
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

    public static class Thrown extends ConvertedException {

        public final BaseObject what;

        public Thrown(BaseObject what) {
            super(what.toString());
            this.what = what;
        }

        public Thrown(java.lang.String message, BaseObject what) {
            super(message);
            this.what = what;
        }

        public Thrown(java.lang.String message, Throwable cause, BaseObject what) {
            super(message, cause);
            this.what = what;
        }
    }

    public static class JavaException extends ConvertedException {

        public final java.lang.String type;

        public JavaException(java.lang.String message) {
            this("Error", message);
        }

        public JavaException(java.lang.String message, Throwable cause) {
            this("Error", message, cause);
        }

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

        public Instance(String String, Error Error, Symbol.Instance iterator, Number Number, java.lang.String name, java.lang.String message, java.lang.String stack) {
            super(Error, iterator, String, Number);
            setHidden("name", String.wrap(this.name = name));
            if (message != null) {
                setHidden("message", String.wrap(this.message = message));
            } else {
                this.message = message;
            }
            setHidden("stack", String.wrap(this.stack = stack));
        }
    }

    private final String String;

    public Error(final Global global) {
        super(global);
        String = global.String;

        GenericObject prototype = (GenericObject) prototype();
        prototype.setHidden("inspect", new AbstractFunction(global) {
            @Override
            public BaseObject call(BaseObject _this, BaseObject... params) {
                return global.wrap(_this.get("stack"));
            }
        });
        prototype.setHidden("toString", new AbstractFunction(global) {
            @Override
            public BaseObject call(BaseObject _this, BaseObject... params) {
                BaseObject message = _this.get("message", OR_NULL);
                if (message != null) {
                    return global.wrap(_this.get("name") + ": " + message);
                }
                return global.wrap(_this.get("name"));
            }
        });
    }

    @Override
    public BaseObject construct(BaseObject... params) {
        if (params.length > 0) {
            return new Instance(String, this, iterator, Number, "Error", params[0].toString(), Utilities.extractStack("Error: " + params[0].toString(), new Throwable()));
        }
        return new Instance(String, this, iterator, Number, "Error", null, Utilities.extractStack("Error", new Throwable()));
    }

    @Override
    public BaseObject call(BaseObject _this, BaseObject... params) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
