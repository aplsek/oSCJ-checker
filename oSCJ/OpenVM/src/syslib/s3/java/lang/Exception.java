
package java.lang;

/**
 * Exceptions may be thrown by one part of a Java program and caught
 * by another in order to deal with the cause of the exception, such as
 * mouse movements, keyboard clicking, etc.
 */
public class Exception extends Throwable
{
  static final long serialVersionUID = -3387516993124229948L;

    /** 
     * Instantiate this Exception with an empty message and a
     * filled in stack trace
     */
    public Exception() {
	super();
    }
    
    /**
     * Instantiate this Exception with the given message and a
     * filled in stack trace
     * @param message the message to associate with the Exception.
     */
    public Exception(String message) {
        super(message);
    }

    /**
     * Instantiate this Exception with an empty message and a stack trace
     * if requested. This allows for wrapper exception objects (like a
     * wildcard exception) to not waste time and resources constructing a stack
     * trace that is never needed.
     *
     * @param trace if true then fill in the stack trace, else don't
     *
     */
    protected Exception(boolean trace) {
        super(trace);
    }

    /**
     * Instantiate this Exception with the given message and a stack trace
     * if requested. This allows for wrapper exception objects (like a
     * wildcard exception) to not waste time and resources constructing a stack
     * trace that is never needed.
     *
     * @param message the message to associate with the Exception.
     * @param trace if true then fill in the stack trace, else don't
     *
     */
    protected Exception(String message, boolean trace) {
        super(message, trace);
    }
}
