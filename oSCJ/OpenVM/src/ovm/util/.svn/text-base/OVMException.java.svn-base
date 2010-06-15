package ovm.util;

/**
 * The base checked exception type for the OVM exception hierarchy.
 * Particular checked exceptions should be declared as nested types here
 * rather than using the standard exception types of the java.* packages.
 */
public  class OVMException extends Exception {

    /**
     * Create a new <code>OVMException</code> with an empty message 
     * and a filled in stack trace
     **/
    public OVMException() {
    }
    
    /**
     * Creates a new <code>OVMException</code> with an error message 
     * and a filled in stack trace
     * @param message the error message
     **/
    public OVMException(String message) {
	super(message);
    }

    /**
     * Creates a new <code>OVMException</code> with an error message
     * and a <code>Throwable</code> cause (when another error
     * causes this error), and a filled in stack trace
     * @param message the error message
     * @param cause the <code>Throwable</code> that caused this
     *              error
     **/    
    public OVMException(String message, Throwable cause) {
	super(message);
        initCause(cause);
    }

    /**
     * Creates a new <code>OVMException</code> with
     * a <code>Throwable</code> cause (when another error
     * causes this error), and a filled in stack trace
     * @param cause the <code>Throwable</code> that caused this
     *              error
     **/    
    public OVMException(Throwable cause) {
	super(cause.getMessage());
	initCause(cause);
    }


    /**
     * Return an unchecked {@link OVMRuntimeException} with this as its cause
     */
    public OVMRuntimeException unchecked() {
	return new OVMRuntimeException(this);
    }

    /**
     * Return an unchecked {@link OVMRuntimeException} with this as its cause
     * and with the given message
     */
    public OVMRuntimeException unchecked(String message) {
	return new OVMRuntimeException(message, this);
    }

    /**
     * Return an unchecked {@link OVMError} with this as its cause
     */
    public OVMError fatal() {
	return new OVMError(this);
    }

    /**
     * Return an unchecked {@link OVMError} with this as its cause
     * and with the given message
     */
    public OVMError fatal(String message) {
	return new OVMError(message, this);
    }

    /**
     * Return a new <code>OVMException</code> with the given message
     * that has the current <code>OVMException</code> as its cause.
     */
    public OVMException wrapped(String message) {
	return new OVMException(message, this);
    }


    // NOTE: We do not have a printStacktrace(stream) method because we
    // should only be using the BasicIO mechanism under OVM, not any java.io
    // classes. We can't rewrite such methods in terms of BasicIO because
    // we compile against the JDK versions of the exception base classes,
    // not the Syslib versions.
    

    /**
     * OVM specific IO exception
     */
    public static class IO extends OVMException {
	public IO() {}
	public IO(String message) {
	    super(message);
	}
	public IO(Throwable cause) {
	    super(cause);
	}
	public IO(String message, Throwable cause) {
	    super(message, cause);
	}
    }

    public static class FileNotFound extends IO {
	public FileNotFound() {}
    
	public FileNotFound(String message) {
	    super(message);
	}

	public FileNotFound(Throwable cause) {
	    super(cause);
	}
	public FileNotFound(String message, Throwable cause) {
	    super(message, cause);
	}
    }

    public static class IllegalAccess extends OVMException {
	protected IllegalAccess() { 
	    super();
	}
	public IllegalAccess(String message) {
	    super(message);
	}
	public IllegalAccess(Throwable cause) {
	    super(cause);
	}
	public OVMRuntimeException unchecked() {
	    return new OVMRuntimeException.IllegalAccess(this);
	}
    }

}

