package javax.realtime;

/** 
 * Thrown if construction of any of the wait-free queues is attempted with 
 * the ends of the queues in incompatible memory areas. Also thrown by
 * wait-free queue methods when such an incompatability is detected after the
 * queue is constructed.
 *
 * @spec RTSJ 1.0.1
 */
public class MemoryScopeException extends Exception {

    /**
     * A constructor for <tt>MemoryScopeException</tt>.
     */
    public MemoryScopeException() {}

    /**
     * A descriptive constructor for <tt>MemoryScopeException</tt>.
     * 
     * @param description A description of the exception.
     */
    public MemoryScopeException(String description) {
        super(description);
    }
}
