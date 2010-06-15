/**
 * QueryableConditionQueue.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/monitors/QueryableConditionQueue.java,v 1.5 2007/06/03 01:25:47 baker29 Exp $
 *
 */
package ovm.services.monitors;

import ovm.core.services.threads.OVMThread;

/**
 * Defines a {@link ConditionQueue} that supports querying of 
 * the number of threads waiting upon the condition.
 * <p>This is a mix-in interface that should be implemented along with a
 * specific condition queue type.
 *
 * @author David Holmes
 *
 */
public interface QueryableConditionQueue extends ConditionQueue {

    /**
     * Returns the number of threads currently waiting on this condition.
     *
     * @return the number of threads waiting on this condition.
     *
     */
    int getWaitQueueSize();

    /**
     * Queries if the specified thread is waiting on this condition.
     *
     * @param t the thread to look for
     *
     * @return <code>true</code> if the specified thread is waiting
     * on this condition, and <code>false</code> otherwise.
     *
     */
     boolean isWaiting(OVMThread t);

}
