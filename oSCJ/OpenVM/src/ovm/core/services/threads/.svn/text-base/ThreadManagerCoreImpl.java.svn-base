/**
 * ThreadManagerCoreImpl.java
 * Created on January 28, 2002, 10:25am
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/core/services/threads/ThreadManagerCoreImpl.java,v 1.14 2004/06/08 05:20:45 dholmes Exp $
 */
package ovm.core.services.threads;

import ovm.core.execution.Processor;

/**
 * A concrete implementation of {@link ThreadManager} which provides those 
 * methods
 * whose implementation are intrinsic to the organisation of the OVM itself.
 * A more specific thread manager implementation can subclass this class to 
 * acquire
 * these methods without having to understand the internal OVM representation
 * and structure of things - like code fragments, contexts, frames etc.
 *
 * @author David Holmes
 */
public abstract class ThreadManagerCoreImpl 
    extends ovm.services.ServiceInstanceImpl
    implements ThreadManager {

    /**
     * Returns a reference to the currently executing OVM thread instance.
     * <p>The current thread refernce is, by convention, stored as local
     * variable 0 in the top-most frame of the current context.
     * 
     */
    public OVMThread getCurrentThread() {
	return ((OVMThreadContext)Processor.getCurrentProcessor().getContext()).getThread();
    }

}








