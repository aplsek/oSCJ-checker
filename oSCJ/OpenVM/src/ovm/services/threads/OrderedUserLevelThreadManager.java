/*
 * OrderedUserLevelThreadManager.java
 *
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/threads/OrderedUserLevelThreadManager.java,v 1.3 2004/02/20 08:48:51 jthomas Exp $
 */
package ovm.services.threads;


/**
 * The <code>OrderedUserLevelThreadManager</code> is a convenience interface
 * which combines {@link UserLevelThreadManager} with {@link ovm.util.Ordered}
 * to allow control of the order of the ready queue.
 * <P>The order
 * in which threads are scheduled for execution is determined by the
 * {@link java.util.Comparator} associated with the ready queue. This
 * comparator would be set according to the current scheduling policy.
 *
 */
public interface OrderedUserLevelThreadManager 
    extends UserLevelThreadManager, ovm.util.Ordered {

    // no new methods
}











