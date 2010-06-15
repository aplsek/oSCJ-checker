package java.lang;

/**
 * Exceptions may be thrown by one part of a Java program and caught
 * by another in order to deal with exceptional conditions.  
 * All exceptions which are subclasses of <code>RuntimeException</code>
 * can be thrown at any time during the execution of a Java virtual machine.
 * Methods which throw these exceptions are not required to declare them
 * in their throws clause.
 *
 */
public class RuntimeException extends Exception {

  static final long serialVersionUID = -7034897190745766939L;

    /** 
     * Instantiate this RuntimeException with an empty message and a
     * filled in stack trace
     */
    public RuntimeException() {
	super();
    }
    
    /**
     * Instantiate this RuntimeException with the given message and a
     * filled in stack trace
     * @param message the message to associate with the RuntimeException.
     */
    public RuntimeException(String message) {
        super(message);
    }

    /**
     * Instantiate this RuntimeException with an empty message and a stack trace
     * if requested. This allows for wrapper exception objects (like a
     * wildcard exception) to not waste time and resources constructing a stack
     * trace that is never needed.
     *
     * @param trace if true then fill in the stack trace, else don't
     *
     */
    protected RuntimeException(boolean trace) {
        super(trace);
    }

    /**
     * Instantiate this RuntimeException with the given message and a stack trace
     * if requested. This allows for wrapper exception objects (like a
     * wildcard exception) to not waste time and resources constructing a stack
     * trace that is never needed.
     *
     * @param message the message to associate with the RuntimeException.
     * @param trace if true then fill in the stack trace, else don't
     *
     */
    protected RuntimeException(String message, boolean trace) {
        super(message, trace);
    }
}
