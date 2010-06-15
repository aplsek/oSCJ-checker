package javax.realtime;

/**
 *  This exception is thrown when a schedulable object or
 * <code>java.lang.Thread</code> attempts to lock an object 
 * governed by an instance of {@link PriorityCeilingEmulation} and the thread 
 * or SO's active priority exceeds the policy's ceiling.
 *
 * @spec RTSJ 1.0.1
 */
public class CeilingViolationException extends IllegalThreadStateException {

    /* There are no public constructors for this class as it is thrown only by
       the implementation.

       As OVM does not support the PCE policy, there are no constructors at all
    */

    private CeilingViolationException() {}

    /**
     * Gets the ceiling of the <code>PriorityCeilingEmulation</code> policy 
     * which was exceeded by the priority of an SO or thread that attempted to 
     * synchronize on an object governed by the policy, which resulted in 
     * throwing of <code>this.</code>
     * 
     * @return The ceiling of the <code>PriorityCeilingEmulation</code> 
     * policy which caused this exception to be thrown.
     */
    public int getCeiling(){
        return 0;
    }

    /**  
     * Gets the active priority of the SO or thread whose 
     * attempt to synchronize resulted in the throwing of this. 
     * @return The synchronizing thread's active priority.
     */
    public int getCallerPriority(){
       return 0;
    }
}
