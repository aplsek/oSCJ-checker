package javax.realtime;

/**
 * If an arrival time occurs and should be queued but the queue already 
 * holds a number of times equal to the initial queue length defined by 
 * this then the <code>fire()</code> method shall throw an instance of this. 
 * If the arrival time is a result of a happening to which the instance of 
 * <code>AsyncEventHandler</code> is bound then the arrival time is ignored.
 * 
 * @since 1.0.1 Becomes unchecked
 *
 * @spec RTSJ 1.0.1
 */
public class ArrivalTimeQueueOverflowException extends RuntimeException {

    /**
     * A constructor for <code>ArrivalTimeQueueOverflowException</code>.
     */
    public ArrivalTimeQueueOverflowException () {
    }

    /**
     * A descriptive constructor for 
     * <code>ArrivalTimeQueueOverflowException</code>.
     * @param description A description of the exception.
     */
    public ArrivalTimeQueueOverflowException (String description) {
        super(description);
    }
}
