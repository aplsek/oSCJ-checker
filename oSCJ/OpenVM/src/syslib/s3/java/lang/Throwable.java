package java.lang;


import java.io.Serializable;
import ovm.core.OVMBase;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.format.JavaFormat;
import ovm.core.domain.Oop;
import ovm.core.domain.Code;
import ovm.core.domain.DomainDirectory;
import ovm.core.execution.CoreServicesAccess;
import ovm.core.execution.RuntimeExports;


/**
 * Throwable is the superclass of all exceptions that can be raised.
 *
 * <p>We add protected constructors in the exception hierarchy to control
 * whether or not the stacktrace is filled in at construction time.
 * Unfortunately this doesn't work because the OVM exception types don't
 * get compiled against these versions of the core classes. As a workaround
 * we define a protected method that returns true or false depending on whether
 * the stack trace should be filled in at construction time. This can be 
 * overridden by specific exception types.
 *
 */
public class Throwable extends Object implements Serializable  {
    static final long serialVersionUID = -3042686055658047285L;
    static private RuntimeExports rte;

    static void boot_() {
	rte = DomainDirectory.getExecutiveDomain().getRuntimeExports();
    }
    
    private String detailMessage = null;
    private Throwable cause = null;

    public Throwable initCause(Throwable cause) {
	this.cause = cause;
	return this;
    }
    public Throwable getCause() { return cause; }
    
    /** 
     * Instantiate this Throwable with an empty message and a
     * filled in stack trace
     */
    public Throwable() {
	this(null);
    }
    
    /**
     * Instantiate this Throwable with the given message and a
     * filled in stack trace
     * @param message the message to associate with the Throwable.
     */
    public Throwable(String message) {
        this(message, true);
    }

    /**
     * Instantiate this throwable with an empty message and a stack trace
     * if requested. This allows for wrapper exception objects (like a
     * wildcard exception) to not waste time and resources constructing a stack
     * trace that is never needed.
     *
     * @param trace if true then fill in the stack trace, else don't
     *
     */
    protected Throwable(boolean trace) {
        this(null, trace);
    }

    /**
     * Instantiate this throwable with the given message and a stack trace
     * if requested. This allows for wrapper exception objects (like a
     * wildcard exception) to not waste time and resources constructing a stack
     * trace that is never needed.
     *
     * @param message the message to associate with the Throwable.
     * @param trace if true then fill in the stack trace, else don't
     *
     */
    protected Throwable(String message, boolean trace) {
	this.detailMessage = message;
        // we fill in the stack trace only if its asked for by the type
        // and the parameter.  The RuntimeExports thing is a workaround 
	// for subclasses that don't like virtual method invocation (i.e.,
	// AbortedException classes).  Try not to add virtual method 
	// invocation to this method, because PARs will break.
	// PARBEGIN PAREND
	if ((message != RuntimeExports.dontFillStackTrace) && trace &&
	    shouldFillInStackTrace()) 
	    fillInStackTrace();
    }


    /**
     * Return <tt>true</tt> if construction should fill in the stack trace
     * and <tt>false</tt> otherwise.
     */
    protected boolean shouldFillInStackTrace() { return true; }

    /**
     * Get the message associated with this Throwable.
     * @return the error message associated with this Throwable.
     */
    public String getMessage() {
	return detailMessage;
    }
    
    /** 
     * Get a localized version of this Throwable's error message.
     * This method must be overridden in a subclass of Throwable
     * to actually produce locale-specific methods.  The Throwable
     * implementation just returns getMessage().
     *
     * @return a localized version of this error message.
     */
    public String getLocalizedMessage() {
	return getMessage();
    }
    
    /**
     * Get a human-readable representation of this Throwable.
     * @return a human-readable String represting this Throwable.
     */
    public String toString() {
        /* See Bug #650
        String name = Object.getNameOfTypeFor(this);
        */
        String name = this.getNameOfType();
	String message = getLocalizedMessage();
        return (message != null) ? (name + ": " + message) : name;
    }

    private Code[] code;
    private int[] pc;
    private StackTraceElement[] trace = null;
    
    public StackTraceElement[] getStackTrace() {
	if (trace == null) {
	    if (code == null)
		return new StackTraceElement[0];
	    else {
		trace = (StackTraceElement[])(Object)
		    rte.getStackTrace(OVMBase.asOop(this));
	    }
	}
	return (StackTraceElement[])trace.clone();
    }

    /** 
     * Print a stack trace to the standard error stream.
     */
    public void printStackTrace() {
	StringBuffer sb = new StringBuffer();
	for (Throwable outer = null, t = this; t != null;
	     outer = t, t = t.cause) {
	    if (outer != null)
		BasicIO.err.print("Caused by: ");
	    BasicIO.err.println(t);
	    StackTraceElement[] trace = t.getStackTrace();
	    if (trace == null) {
		BasicIO.err.println("no stack trace yet");
	    } else if (trace.length == 0) {
		BasicIO.err.println("weird, 0 stack trace elements");
	    } else {
		for (int i = 0; i < trace.length; i++) {
		    sb.setLength(0);
		    String className = trace[i].getClassName();
		    String methodName = trace[i].getMethodName();
		    String fileName = trace[i].getFileName();
		    int lineNumber = trace[i].getLineNumber();
		    boolean isNative = trace[i].isNativeMethod();
		    sb.append("\tat ");
		    if (className != null) {
			sb.append(className);
			if (methodName != null)
			    sb.append('.');
		    }
		    if (methodName != null)
			sb.append(methodName);
		    sb.append("(");
		    if (fileName != null)
			sb.append(fileName);
		    else
			sb.append(isNative ? "Native Method"
				  : "Unknown Source");
		    if (lineNumber >= 0)
			sb.append(':').append(lineNumber);
		    sb.append(')');
		    BasicIO.err.println(sb.toString());
		}
	    }
	}
    }

    /** 
     * Fill in the stack trace with the current execution stack.
     * Normally used when rethrowing an exception, to strip
     * off those unnecessary and complicated early stack frames that
     * could actually allow a person to determine what really went wrong
     * and why. Some programmers like to do this.
     * @return this same throwable.
     */
    public Throwable fillInStackTrace() {
	if (rte == null) boot_(); // just in case a boot_ method excepts
	rte.fillInStackTrace(OVMBase.asOop(this));
	return this;
    }
}


