/*
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/threads/ConditionQueueTrackingThread.java,v 1.3 2004/02/20 08:48:51 jthomas Exp $
 *
 */
package ovm.services.threads;

import ovm.services.monitors.ConditionQueue;

/**
 * This is a mix-in interface for threads that keep track of whether or not
 * they are waiting in a {@link ConditionQueue}. 
 * This is useful information if, for example,
 * you need to update queues when a threads priority changes.
 *
 * @author David Holmes
 *
 */
public interface ConditionQueueTrackingThread {

    /**
     * Sets the {@link ConditionQueue} upon which this thread is waiting.
     *
     * @param condQ the condition queue upon which this thread is waiting, or 
     * <code>null</code> if this thread is no longer waiting upon a condition.
     * @throws IllegalStateException if <code>condQ</code> is not 
     * <code>null</code> and this thread is already waiting on a condition
     */
    void setWaitingConditionQueue(ConditionQueue condQ);

    /**
     * Returns the {@link ConditionQueue} upon which this thread is waiting.
     * @return the condition queue upon which this thread is waiting, or 
     * <code>null</code> if this thread is not waiting on a condition
     *
     */
    ConditionQueue getWaitingConditionQueue();

}
