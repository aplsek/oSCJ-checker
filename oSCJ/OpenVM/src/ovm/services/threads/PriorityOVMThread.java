/*
 * PriorityOVMThread.java
 *
 * Created on November 28, 2001, 2:59 PM
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/threads/PriorityOVMThread.java,v 1.10 2004/09/07 01:03:49 dholmes Exp $
 */
package ovm.services.threads;

import ovm.core.services.threads.OVMThread;
import ovm.core.execution.Native;

/**
 * <code>PriorityOVMThread</code> is an <code>OVMThread</code> with a notion
 * of priority.
 * <p>Exactly what priority means depends on the concrete implementation of
 * the thread. It could correlate to a language-level thread priority, the
 * associated native thread priority (if any), or some other notion of
 * priority. Changing the priority of a thread may or may not change it's
 * priority in the eyes of the system. A concrete thread class will work in 
 * conjunction with a concrete dispatcher, or thread manager, to actually
 * change the runtime behaviour of the thread.
 *
 * @see PriorityOVMDispatcher
 */
public interface PriorityOVMThread extends OVMThread {

    /**
     * sets the priority of this thread
     *
     * @param newPriority the new priority value for this thread. The exact
     * meaning of the priority value depends on the concrete implementation.
     * @throws IllegalArgumentException if the new priority value is out of
     *         range.
     * @see #getPriority
     */
    void setPriority(int newPriority);

    /**
     * Returns the current priority of this thread.
     * @return the current priority of this thread.
     * @see #setPriority
     */
    int getPriority();

    /**
     * A singleton comparator for maintaining thread queues in priority order.
     * Use the {@link #instance} method to access the comparator.
     * <p>Note: this comparator imposes orderings that are
     * inconsistent with equals.
     *
     */
    static final class Comparator
        extends ovm.core.OVMBase
        implements java.util.Comparator
    {
        static final Comparator instance = new Comparator();

        private Comparator() { }
        /**
         * Returns the singleton instance of this class.
         * @return the singleton instance of this class.
         */
        public static java.util.Comparator instance() {
            return instance;
        }

        public int compare(Object o1, Object o2) throws s3.util.PragmaNoPollcheck {
          
          // TODO: could we optimize the IH flag out by using some bits/range of
          // thread priority ? We would then have to remember the old thread's priority,
          // but it'd be better then having the extra branch and two calls here
          
            boolean i1 = ((PriorityOVMThread)o1).getInterruptHandlerFlag();
            boolean i2 = ((PriorityOVMThread)o2).getInterruptHandlerFlag();
            
            if (i1||i2) {
              if (i1&&i2) {
                return 0;
              } 
              if (i1) return 1;
              return -1;
            }
        
            int n1 = ((PriorityOVMThread)o1).getPriority();
            int n2 = ((PriorityOVMThread)o2).getPriority();
            // note: we can't use the "return n1-n2;" optimisation because
            // we don't know that the difference will fit in an int. We'll
            // assume a good compiler/JIT will optimise this itself.
            if (n1 > n2) return 1;
            else if (n1 == n2) return 0;
            else return -1;
        }

    }
}








