/*
 * $Header: /p/sss/cvs/OpenVM/src/syslib/user/ovm_realtime/javax/realtime/AsyncEvent.java,v 1.1 2004/10/15 01:53:11 dholmes Exp $
 */
package javax.realtime;
/**
 * An asynchronous event represents something that can happen, like a
 * light turning red.  It can have a set of handlers associated with it, and
 * when the event occurs, the handler is scheduled by the scheduler to which
 * it holds a reference (see {@link AsyncEventHandler} and {@link Scheduler}).
 *
 * <p> A major motivator for this style of building events is that we expect
 * to have lots of events and lots of event handlers.  An event handler is
 * logically very similar to a thread, but it is intended to have
 * a much lower cost (in both time and space) -  assuming that a relatively 
 * small number of events are fired and in the process of being handled at 
 * once. {@link AsyncEvent#fire} differs from a method call
 * because the handler (a) has scheduling parameters and (b) is executed
 * asynchronously.
 *
 */
public class AsyncEvent {

    /**  
     * The set of handlers associated with this event. This is package access
     * as some async events are used internally by the implementation and
     * access to them is controlled via other means - hence we don't want to
     * have to use the public API's.
     */
    final IdentityArraySet handlers = new IdentityArraySet();

    /** The lock used to protect access to the handler list */
    final Object lock = handlers;

    /* Events can be bound to implementation defined happenings.
       Logically you should have a single happening per event but this is
       not required. We have no happenings at this time - and don't envisage
       any.
    */

    /** The number of happenings we support. */
    static final int HAPPENINGS = 0;

    /** The list of bindings for each happening - lazily initialised */
    static final IdentityArraySet[] bindings = new IdentityArraySet[HAPPENINGS];

    /** the lock protecting access to the bindings */
    static final Object bindingLock = bindings;

    /**
     * Create a new <code>AsyncEvent</code> Object.
     */
    public AsyncEvent() {
    } 


    /* these are the handler list management methods */

    /**
     * Add a handler to the set of handlers associated with this event. 
     * An instance of <code>AsyncEvent</code> may have more than one 
     * associated handler.
     * <p>Since this affects the constraints expressed in the release 
     * parameters of the existing schedulable objects, this may change the
     * feasibility of the current schedule.
     *
     * @param handler The new handler to add to the list of handlers already
     * associated with <code>this</code>. 
     * If <code>handler</code> is null then nothing happens.
     */   
    public void addHandler(AsyncEventHandler handler) {
        if (handler != null) {
            synchronized(lock) {
                handler.startIfNotStarted();
                handlers.add(handler); // ignores duplicates
            }
        }
    }

    /**
     * Remove a handler from the set associated with this event.
     * @param handler The handler to be disassociated from this. 
     * If <code>null</code> nothing happens. 
     * If not already associated with <code>this</code> then nothing happens.
     */
    public void removeHandler(AsyncEventHandler handler) {
        synchronized(lock) {
            handlers.remove(handler); // deals with null
        }
    }

    /**
     * Returns <code>true</code> if and only if this event is handled by 
     *this handler.
     * @param handler The handler to be tested to determine if it is
     * associated with this. 
     * @return <code>true</code> if the handler is associated with this event
     * and <code>false</code> otherwise.
     */
    public boolean handledBy(AsyncEventHandler handler) {
        synchronized(lock) {
            return handlers.contains(handler);
        }
    }
    
    /**
     * Associate a new handler with this event and remove all existing 
     * handlers.
     * <p>Since this affects the constraints expressed in the release 
     * parameters of the existing schedulable objects, this may change 
     * the feasibility of the current schedule.
     *
     * @param handler The new instance of {@link AsyncEventHandler} to be
     * associated with <code>this</code>. 
     * If <code>handler</code> is <code>null</code> then no handler will be
     * associated with this (i.e., remove all handlers).  
     */
    public void setHandler(AsyncEventHandler handler) {
        synchronized(lock) {
            handlers.clear();
            if (handler != null) {
                handler.startIfNotStarted();
                handlers.add(handler);
            }
        }
    }
  
  
    /**
     * Create a {@link ReleaseParameters} object appropriate to the release 
     * characteristics of this event. The default is the most pessimistic:
     * {@link AperiodicParameters}. This is typically called by code that is 
     * setting up a handler for this event that will fill in the parts of the 
     * release parameters for which it has values, e.g., cost.
     *
     * @return A new {@link ReleaseParameters} object. 
     */
    public ReleaseParameters createReleaseParameters() {
        return new AperiodicParameters(null, null, null, null);
    }


    /* methods for managing bindings */
    
    /**
     * Binds this to an external event, a happening. 
     * The meaningful values of happening are implementation dependent. 
     * This instance of <code>AsyncEvent</code> is considered to have 
     * occurred whenever the happening occurs.
     *
     * @param happening An implementation dependent value that binds this
     * instance of <code>AsyncEvent</code> to a happening.
     *
     * @throws UnknownHappeningException If the String value is not
     * supported by the implementation.
     */
    public void bindTo(String happening) {
        throw new UnknownHappeningException("Unsupported happening");
    }
  
    /** 
     * Removes a binding to an external event, a happening. 
     * The meaningful values of happening are implementation dependent.
     *
     * @param happening An implementation dependent value representing some
     * external event to which this instance of AsyncEvent is bound.
     *
     * @throws UnknownHappeningException If this instance of 
     * <code>AsyncEvent</code> is not bound to the given happening or the 
     * given String value is not supported by the implementation.
     */
    public void unbindTo(String happening) {
        throw new UnknownHappeningException("AsyncEvent not bound to this happening");
    }


    /* the actual firing of the event */
      
    /**
     * Fire this instance of <code>AsyncEvent</code>. The 
     * {@link AsyncEventHandler#run run()} methods of instances of
     * {@link AsyncEventHandler} associated with this event will be made 
     * ready to run.
     */
    public void fire() {
        synchronized(lock) {
            for (int i = 0; i < handlers.size; i++) {
                ((AsyncEventHandler)handlers.data[i]).releaseHandler();
            }
        }
    }
  
}
