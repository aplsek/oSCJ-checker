/**
 * TimedMonitor.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/monitors/TimedMonitor.java,v 1.3 2004/02/20 08:48:47 jthomas Exp $
 *
 */
package ovm.services.monitors;


/**
 * Defines a {@link Monitor} which supports a time-out on trying to
 * enter the monitor. 
 * <p>This interface defines a new timed entry point rather than
 * changing the semantics of the base {@link Monitor#enter} method. This
 * is partly pragmatic because the base method does not have a return value
 * that can be used to signify how the enter terminated, but it
 * also allows for an implementation to have both timed and non-timed enter
 * points.
 *
 * @author David Holmes
 *
 */
public interface TimedMonitor {

    /**
     * Causes the current thread to attempt entry of the monitor. If the
     * monitor is unavailable and the specified timeout elapses
     * this method will return with a <code>false</code> value.
     *
     * <p>The timeout value must be greater than, or equal to zero. A zero
     * timeout has implementation specific semantics - it could mean a balking
     * response, or it could mean an infinite timeout.
     *
     * @return <code>true</code> if the monitor was entered, and 
     * <code>false</code> if the timeout elapsed.
     *
     * @param timeout the timeout in nanoseconds.
     *
     */
    boolean enterTimed(long timeout);

}





