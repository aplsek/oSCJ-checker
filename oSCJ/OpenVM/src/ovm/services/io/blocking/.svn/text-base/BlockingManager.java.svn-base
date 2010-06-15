// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/blocking/BlockingManager.java,v 1.2 2004/02/20 08:48:34 jthomas Exp $

package ovm.services.io.blocking;

import ovm.core.services.threads.OVMThread;

/**
 * This interface has all of the stuff needed by BlockingCallback that
 * is not provided in a generic way by the threading services.  The
 * very existance of this interface suggests problems elsewhere...
 * @author Filip Pizlo
 */
public interface BlockingManager
    extends ovm.services.ServiceInstance {
    
    /**
     * Returns the IO priority level that should be given to
     * asynchronous requests made by the given thread.
     */
    public int getPriority(OVMThread t);
    
    /**
     * Sets the state of the given OVMThread to indicate that it is
     * blocked on IO.
     */
    public void notifyBlock(OVMThread t);
    
    /**
     * Resets the state of the given OVMThread to indicate that it
     * is no longer blocked on IO.
     */
    public void notifyUnblock(OVMThread t);
    
}


