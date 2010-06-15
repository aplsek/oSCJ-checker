package java.lang;

/**
 * When the Java Virtual Machine is unable to allocate an object because it 
 * is out of memory and no more memory could be made available by the 
 * garbage collector an <code>OutOfMemoryError</code> is thrown.
 *
 * <p>By default NO stacktrace is created for an OutOfMemoryError.
 */
public class OutOfMemoryError extends VirtualMachineError {

    static final long serialVersionUID = 8228564086184010517L;

    /**
     * Create an error without a message and with no stack trace
     */
    public OutOfMemoryError() {
        super(null, false);
    }
    
    /**
     * Create an error with a message and no stack trace
     */
    public OutOfMemoryError(String s) {
        super(s, false);
    }
    

    /**
     * Create an error with the given message and a stacktrace if requested
     */
    public OutOfMemoryError(String s, boolean trace) {
        super(s, trace);
    }
}
