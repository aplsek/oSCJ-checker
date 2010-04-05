package ovm.util;

import ovm.core.execution.Native;

/**
 * The base error class in the OVM. The kernel should throw exceptions
 * of this type, or one of its nested types, instead of the usual
 * <code>java.lang</code> exceptions (modulo the cases where we can't
 * control that - like <code>NullPointerException</code>.
 * See also {@link OVMRuntimeException} for RTE equivalents for use in OVM and
 * {@link OVMException} for checked exceptions.
 *
 * @author Krzysztof Palacz
 */
public class OVMError extends Error {

    /**
     * Create a new <code>OVMError</code> with an empty message and a 
     * filled in stack trace
     **/
    public OVMError() {
    }
    
    /**
     * Creates a new <code>OVMError</code> with an error message and a filled
     * in stack trace
     * @param message the error message
     **/
    public OVMError(String message) {
	super(message);
    }

    /**
     * Creates a new <code>OVMError</code> with an error message
     * and a <code>Throwable</code> cause (when another error
     * causes this error), and a filled in stack trace
     * @param message the error message
     * @param cause the <code>Throwable</code> that caused this
     *              error
     **/    
    public OVMError(String message, Throwable cause) {
	super(message);
	initCause(cause);
    }
    
    /**
     * Creates a new <code>OVMError</code> with a 
     * <code>Throwable</code> cause (when another error
     * causes this error) and a filled in stack trace
     * @param cause the <code>Throwable</code> that caused this
     *              error
     **/
    public OVMError(Throwable cause) {
	super(cause.getMessage());
	initCause(cause);
    }


    // NOTE: We do not have a printStacktrace(stream) method because we
    // should only be using the BasicIO mechanism under OVM, not any java.io
    // classes. We can't rewrite such methods in terms of BasicIO because
    // we compile against the JDK versions of the exception base classes,
    // not the Syslib versions.

    /**
     * Convenience method to turn a checked exception (potentially) into an
     * unchecked error.
     * <p>Why not just use <code>new OVMError(e)</code>? -- dh
     */
    public static OVMError wrap(Exception e) {
	return new OVMError(e);
    }


    public static class UnsupportedOperation extends OVMError {
	public UnsupportedOperation() {
	}
	/**
	 * Creates a new error given an error message string.
	 * @param message the error message string
	 **/
	public UnsupportedOperation(String message) {
	    super(message);
	}
    }

    public static class ClassCast extends OVMError {
	public ClassCast() {
	}
	/**
	 * Creates a new error given an error message string.
	 * @param message the error message string
	 **/
	public ClassCast(String message) {
	    super(message);
	}
    }

    /*
     * This error should not happen
     */
    public static class Internal extends OVMError {
	public Internal(String message) {
	    super(message);
	}
	public Internal(String message, Throwable e) {
	    super(message, e);
	}

	public Internal(Throwable e) {
	    super("This exeption should not be thrown here: ", e);
	}

    }

    public static class Unimplemented extends OVMError {
	private static final String msg = "FIXME: unimplemented";
	/**
	 * Default constructor, which creates an error with no message and
	 * no causing <code>Throwable</code>
	 **/
	public Unimplemented() {
	    this(msg);
	}
	/**
	 * Creates a new error with a causing <code>Throwable</code> and a
	 * default message indicating something was unimplemented.
	 * @param e the <code>Throwable</code> that caused this error
	 **/
	public Unimplemented(Throwable e) {
	    super(msg, e);
	}
	/**
	 * Creates a new error given an error message string.
	 * @param message the error message string
	 **/
	public Unimplemented(String message) {
	    super(message);
	}
    } // End of Unimplemented


    /*
     * Error thrown when one service detects the absence of another service
     * needed to carry out a request.
     */
    public static class Configuration extends OVMError {
	public Configuration(String message) {
	    super(message);
	}
	public Configuration(String message, Throwable e) {
	    super(message, e);
	}

	public Configuration(Throwable e) {
	    super("This exeption should not be thrown here: ", e);
	}
    }

    /*
     * Error thrown when an illegal argument is passed to a method.
     */
    public static class IllegalArgument extends OVMError {
	public IllegalArgument(String message) {
	    super(message);
	}
	public IllegalArgument(String message, Throwable e) {
	    super(message, e);
	}

	public IllegalArgument(Throwable e) {
	    super("Illegal argument caused by: ", e);
	}

    }

    /*
     * Error thrown when a method is invoked when an object is in
     * the wrong state
     */
    public static class IllegalState extends OVMError {
	public IllegalState(String message) {
	    super(message);
	}
	public IllegalState(String message, Throwable e) {
	    super(message, e);
	}

	public IllegalState(Throwable e) {
	    super("Illegal state caused by: ", e);
	}

    }
    
    /*
     * System (e.g. native system-call) error.
     */
    public static class System extends OVMError {
       private int errno_;

        protected static String makeMessage(String message,int errno) {
            byte[] data=new byte[1024];
            int result=Native.get_specific_error_string(errno,data,data.length);
            // either check result is good or at least assert that it is
            assert result >= 0 && result < data.length:
		"problem with native error string";
            return message+new String(data,0,result);
        }

        public System(String message,int errno) {
            super(makeMessage(message,errno));
            this.errno_=errno;
        }
        public System(String message,int errno,Throwable e) {
            super(makeMessage(message,errno),e);
            this.errno_=errno;
        }

        /**
         * Utility function to check if an action was successful and if
         * not throw an error.
         * @param action description of the action that was attempted
         * @param result the result of the action that was attempted. 
         * If non-zero then an instance of <tt>OVMError.System</tt> will
         * be thrown.
         */
        public static void check(String action,int result) {
            if (result != 0) {
                throw new System("While " + action + ": ", result);
            }
        }
        
        /** a correct version of the above */
        public static void check(String action,int result,int errno) {
            if (result<0) {
                throw new System("While "+action+": ",errno);
            }
        }
        
        /** use this with lseek() */
        public static void check(String action,long result,int errno) {
            if (result<0) {
                throw new System("While "+action+": ",errno);
            }
        }
    }
    
}





