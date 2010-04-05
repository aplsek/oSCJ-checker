package java.lang;

public class Error extends Throwable
{
  static final long serialVersionUID = 4980196508277280342L;

    /** 
     * Instantiate this Error with an empty message and a
     * filled in stack trace
     */
    public Error() {
	super();
    }
    
    /**
     * Instantiate this Error with the given message and a
     * filled in stack trace
     * @param message the message to associate with the Error.
     */
    public Error(String message) {
        super(message);
    }

    public Error(String message, Throwable cause) {
	super(message);
	initCause(cause);
    }

    /**
     * Instantiate this Error with an empty message and a stack trace
     * if requested. This allows for wrapper exception objects (like a
     * wildcard exception) to not waste time and resources constructing a stack
     * trace that is never needed.
     *
     * @param trace if true then fill in the stack trace, else don't
     *
     */
    protected Error(boolean trace) {
        super(trace);
    }

    /**
     * Instantiate this Error with the given message and a stack trace
     * if requested. This allows for wrapper exception objects (like a
     * wildcard exception) to not waste time and resources constructing a stack
     * trace that is never needed.
     *
     * @param message the message to associate with the Error.
     * @param trace if true then fill in the stack trace, else don't
     *
     */
    protected Error(String message, boolean trace) {
        super(message, trace);
    }
}
