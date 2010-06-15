/*
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/ServiceInstanceImpl.java,v 1.6 2004/10/13 00:15:11 pizlofj Exp $
 *
 */
package ovm.services;

/**
 * An abstract base class that implements {@link ServiceInstance} and
 * for convenience defines empty implementations of its methods. With
 * the exception of {@link #init} it is not uncommon for the 
 * service instance methods to be no-ops for a given service. This class
 * avoids the need to re-define empty methods in each service, or the
 * need to factor the interface into individual methods.
 *
 *
 * @author David Holmes
 *
 */
public abstract class ServiceInstanceImpl 
    extends ovm.core.OVMBase 
    implements ServiceInstance {

    /** Subclasses should set this when {@link #init} has completed */
    protected volatile boolean isInited = false;

    /** Subclasses should set this when {@link #start} has completed */
    protected volatile boolean isStarted = false;

    public abstract void init();

    /**
     * A no-op. Subclasses should override if they have specific
     * start-up actions to perform.
     */
    public void start() {
        isStarted = true;
    }

    /**
     * A no-op. Subclasses should override with specific actions, if
     * appropriate.
     */
    public void aboutToShutdown() {
    }

    /**
     * A no-op. Subclasses should override with specific actions.
     */
    public void stop() {
    }

    /**
     * A no-op. Subclasses should override with specific actions.
     */
    public void destroy() {
    }

    
    public boolean isInited() { return isInited; }

    public boolean isStarted() { return isStarted; }
}

