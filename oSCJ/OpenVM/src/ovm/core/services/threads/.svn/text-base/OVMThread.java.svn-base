/*
 * OVMThread.java
 *
 * Created on November 28, 2001, 2:59 PM
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/core/services/threads/OVMThread.java,v 1.15 2005/03/22 19:01:08 baker29 Exp $
 */
package ovm.core.services.threads;

/**
 * This class represents threads of execution within the OVM.
 * An <code>OVMThread</code> logically consists of a stack area and a
 * program counter. This class can be specialised to produce representations
 * of language specific threads in particular contexts - for example, a
 * Java thread within a Java Virtual Machine. Such specialisations will provide
 * a means to access the actual language level thread object.
 * 
 * @see ovm.services.java.JavaOVMThread
 * @author David Holmes
 */
public interface OVMThread {

    /**
     * Set the execution context associated with this thread.
     * <p>An implementation may restrict when the context may be set, but
     * should document any such restriction.
     *
     * @param ctx the execution context for this thread
     *
     */
    void setContext(OVMThreadContext ctx);

    /**
     * Returns the execution context associated with this thread.
     * @return the execution context associated with this thread,or
     * <code>null</code> if the context has not been set.
     *
     */
    OVMThreadContext getContext();

    /**
     * The entry point for execution of a thread. Implementations should
     * define this method in such a way that the behaviour of the thread
     * can be specialised as needed.
     *
     */
    void runThread();
    
    boolean setInterruptHandlerFlag(boolean newValue);
    
    boolean getInterruptHandlerFlag();
}





