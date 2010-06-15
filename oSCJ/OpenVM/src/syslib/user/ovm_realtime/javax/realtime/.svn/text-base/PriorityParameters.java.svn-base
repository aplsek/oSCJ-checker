/*
 * $Header: /p/sss/cvs/OpenVM/src/syslib/user/ovm_realtime/javax/realtime/PriorityParameters.java,v 1.1 2004/10/15 01:53:11 dholmes Exp $
 *
 */
package javax.realtime;

/** 
 * Instances of this class should be assigned to threads that are managed by 
 * schedulers which use a single integer to determine execution order. 
 * The base scheduler required by this specification and represented by the 
 * class {@link PriorityScheduler} is  such a scheduler.
 *
 * <h3>OVM Notes</h3>
 * <p>Atomically changing the priority of all threads associated with a
 * PriorityParameters object is extremely difficult to do in a way that
 * would be generally acceptable to a range of real-time applications.
 * The only safe way to approach this is to ensure that the thread doing
 * the changing has a higher priority than that being currently used by or
 * assigned to the PriorityParameters object - this is something the library
 * can not ensure. The RI partially avoids this problem by prohibiting the 
 * changing of priority if any of the threads bound to this object have 
 * already been started, but are not blocked in wait(), or sleep(). However, 
 * the RI does not provide any atomicity control to ensure that this condition
 * holds across the call to change the priority. It is not clear what the RTSJ
 * really requires - but it is known that changing the priority of a running
 * thread makes implementation of the Priority Inheritance Protocol much more
 * difficult. For want of a better answer we'll emulate the RI and delegate to
 * setSchedulingParameters, which will enforce the restrictions.
 *
 * <p>We assume there is a single possible scheduler - and we check that is the
 * case.
 *
 * <p>The specification of setPriority is given in terms of threads not 
 * Schedulables.
 *
 * @author David Holmes
 */
public class PriorityParameters extends SchedulingParameters {

    /** our priority */
    protected volatile int priority;

    /** 
     * Create an instance of {@link SchedulingParameters} with the given 
     * priority.
     * @param priority  The priority assigned to a thread. This value is 
     * used in place of the value set by {@link java.lang.Thread#setPriority}.
     */
    public PriorityParameters(int priority)  {
        // valid priority is determined by the scheduler of the Schedulable
        // this parameter object is associated with. It will reject this
        // parameter object if this priority is out-of-range
	this.priority = priority;
    }
    
    /** 
     * Return our priority.
     * @return the priority value
     */
    public int getPriority()  {
	return priority;
    }


    /**
     * Set the priority value.
     * @param priority The value to which priority is set.  
     * @throws IllegalArgumentException Thrown if the given priority value is 
     * less than the minimum priority of the scheduler of any of the 
     * associated threads or greater then the maximum priority of the 
     * scheduler of any of the associated threads.
     */
    public void setPriority(int priority) {

        int oldPriority = this.priority;

	this.priority = priority;

        int len = schedulables.size();
        for(int i = 0; i < len ; i++) {
            try {
                ((Schedulable)schedulables.data[i]).setSchedulingParameters(this);
            }
            catch(IllegalThreadStateException itse) {
                System.err.println(
                    "WARNING: Schedulable " + schedulables.data[i] + 
                    "was in the wrong state to set the scheduling parameters" +
                    "- it's now inconsistent with those scheduling parameters!"
                    );
            }
            catch(IllegalArgumentException iae) {
                // change failed so rollback.
                // In practice this will fail on the first one but we'll
                // do the "right thing"
                this.priority = oldPriority;
                for (int j = i-1; j >= 0; j--) {
                    try {
                        ((Schedulable)schedulables.data[j]).setSchedulingParameters(this);
                    }
                    catch(IllegalThreadStateException ex) {
                        // if the schedulable is the same as in the main loop
                        // it will be consistent again but in general something
                        // that was set successfully the first time could
                        // fail this time.
                        System.err.println(
                                           "WARNING: Schedulable " + 
                                           schedulables.data[i] + 
                                           "was in the wrong state to set the scheduling parameters" +
                                           "- it's now inconsistent with those scheduling parameters!"
                                           );
                    }
                }
                throw iae;
            }
        }	
    }

    public String toString() {
	return priority + "";
    }

}
