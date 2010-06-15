/**
 * ConditionQueueSignaller.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/monitors/ConditionQueueSignaller.java,v 1.4 2004/02/20 08:48:46 jthomas Exp $
 *
 */
package ovm.services.monitors;


/**
 * Defines the signalling side of the abstract notion of a 
 * &quot;condition&quot; as used with the
 * monitors of Hoare & Brinch-Hansen and those used in languages like
 * Modula-3 and Java. This interface provides very few semantic details
 * as they vary between monitor types. More specific monitor types will
 * provide specific semantics; for example 
 * {@link ovm.services.java.JavaMonitor}.
 *
 * @see Monitor
 * @see ConditionQueue
 * 
 * @author David Holmes
 *
 */
public interface ConditionQueueSignaller {

    /**
     * Signals at least one thread waiting upon this condition, such that
     * the thread will be able to return from the wait once it has
     * re-entered the monitor. Which thread is signalled is not specified.
     *
     */
    void signal();

    /**
     * Signals all threads waiting upon this condition, such that
     * they will be able to return from the wait once they have
     * reentered the monitor. The order in which threads are signalled
     * is not specified.
     *
     */
    void signalAll();

}






