// $Header: /p/sss/cvs/OpenVM/src/ovm/services/monitors/UnsafeConditionQueueSignaller.java,v 1.1 2004/04/17 19:31:40 pizlofj Exp $

package ovm.services.monitors;

/**
 * Defines the unsafe and trusted signalling side of the abstract notion of a
 * &quot;condition&quot;.  By unsafe and trusted it is meant that the
 * underlying implementation will not attempt to verify that it is safe or
 * even perimissible to send a signal when the <code>signalOneUnsafe()</code>
 * method is called.
 *
 * @author Filip Pizlo
 */
public interface UnsafeConditionQueueSignaller {
    
    void signalOneUnsafe();
    
}

