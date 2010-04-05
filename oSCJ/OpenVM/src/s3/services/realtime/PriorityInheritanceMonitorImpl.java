
package s3.services.realtime;

import java.util.Comparator;

import ovm.core.services.io.BasicIO;
import ovm.core.services.threads.OVMDispatcher;
import ovm.core.services.threads.OVMThread;
import ovm.core.stitcher.MonitorServicesFactory;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.ThreadDispatchServicesFactory;
import ovm.services.monitors.Monitor;
import ovm.util.OVMError;
import s3.services.java.ulv1.JavaMonitorImpl;
import s3.services.monitors.BasicMonitorImpl;
import s3.core.domain.S3Domain;
/**
 * An extension of the {@link BasicMonitorImpl basic monitor }  supporting the 
 * Basic Priority Inheritance Protocol. 
 * The support for the priority inheritance protocol is not
 * general purpose (hence no interface types capture the semantics) but a
 * specific implementation that works in conjunction with the
 * {@link PriorityInheritanceOVMThreadImpl} and the 
 * {@link PriorityInheritanceDispatcherImpl} in this package. 
 * This implementation accounts for
 * the arbitrary change of a thread's priority programmatically at runtime.
 *
 * <p>This monitor type can only be used in a configuration supporting the
 * priority inheritance protocol.
 *
 * @see <a href="doc-files/PriorityInheritance.html">Priority Inheritance Protocol 
 * Implementation</a>
 *
 * @see PriorityInheritanceDispatcherImpl
 * @see PriorityInheritanceOVMThreadImpl
 *
 * @author David Holmes
 *
 */
public class PriorityInheritanceMonitorImpl extends BasicMonitorImpl { 

    /** The factory object */
    public static final Monitor.Factory factory = new Factory();

    /** Reference to current dispatcher*/
    protected static final PriorityInheritanceDispatcherImpl dispatcher;

    // we want to initialize the statics and check the config but *ONLY* if
    // we are the configured monitor type. The only way to do that is to
    // see if our factory is the configured MonitorFactory.
    // This class will be loaded even if not being used because
    // we are referenced by PriorityInheritanceOVMThreadImpl which probably is
    // being used
    static {
	if (((MonitorServicesFactory) ThreadServiceConfigurator.config.
		getServiceFactory(MonitorServicesFactory.name)).getMonitorFactory() == factory) {
	    
	    OVMDispatcher d = ((ThreadDispatchServicesFactory) ThreadServiceConfigurator.config
		    .getServiceFactory(ThreadDispatchServicesFactory.name)).getThreadDispatcher();
	    
	    if (!(d instanceof PriorityInheritanceDispatcherImpl)) { 
		throw new OVMError.Configuration("PriorityInheritanceMonitorImpl needs PriorityInheritanceDispatcherImpl"); 
	    }
	    dispatcher = (PriorityInheritanceDispatcherImpl) d;
	} else dispatcher = null;
    }

    /**
     * Construct a monitor using the default comparator as configured in
     * the current thread manager. If the current thread manager is not
     * an {@link ovm.util.Ordered} thread manager then an exception is thrown.
     * @throws ClassCastException if the thread manager is not 
     * {@link ovm.util.Ordered}
     */
    protected PriorityInheritanceMonitorImpl() {
        super();
    }

    /**
     * Construct a monitor using the supplied comparator for maintaining
     * the entry queue.
     *
     */
    protected PriorityInheritanceMonitorImpl(Comparator comp) {
        super(comp);
    }



    /**
     * The factory class for creating monitors 
     *
     */
    public static class Factory extends BasicMonitorImpl.Factory {
        public Monitor newInstance() {
            return new PriorityInheritanceMonitorImpl();
        }
        public int monitorSize() {
            return PriorityInheritanceMonitorImpl.sizeOf();
        }
    }


    /**
     *  Returns the actual size of an instance of this class, including the
     *  space needed for the object header and all fields, plus the space
     *  needed for creating referenced objects (and transitively the space
     *  they need to create referenced objects) during construction.
     */
    static int sizeOf() {
	return S3Domain.sizeOfInstance("s3/services/monitors/realtime/PriorityInheritanceMonitorImpl")
		+ constructionSizeOf();
    }


    /**
     * Returns the maximum space allocated during the execution of the
     * constructor of an instance of this class, and transitively the space
     * needed by any object allocation performed in this constructor.
     * Note this doesn't include "temporary" allocations like debug strings
     * etc, but it does include super constructors. Hence for any class the
     * total space needed to do "new" is the base size plus the construction
     * size.
     */
    protected static int constructionSizeOf() {
        // there is no additional allocation in this class so just return
        // whatever our super class construction requirements are
        return BasicMonitorImpl.constructionSizeOf();
    }

    // override the customization hooks to effect the BPIP

    /**
     * Inserts the current thread into the inheritance set of the thread 
     * owning this monitor and ensures all priority releationships are
     * maintained. Ideally this would only be necessary if the current thread's
     * priority were greater than the owner, but the priorities of either 
     * thread could change at any time.
     */
    protected boolean onBlocking(OVMThread current) {
        // Test me: if there is a deadlock situation it is not clear what will
        // happen - so we need to test this.
	if (JavaMonitorImpl.GATHER_MONITOR_STATISTICS)JavaMonitorImpl.M_BLOCK++;
	// when called the current thread is not yet in the entryQ.
        PriorityInheritanceOVMThreadImpl head = (PriorityInheritanceOVMThreadImpl) entryQ.head();
	PriorityInheritanceOVMThreadImpl _owner = (PriorityInheritanceOVMThreadImpl) this.owner;

        if (head == null) {
            if (DEBUG) BasicIO.out.println("\t\tno current waiters in entryQ so adding current to owners (" + _owner + ") inheritance queue");
            _owner.inheritanceQueue.add(((PriorityInheritanceOVMThreadImpl)current).node);
            if (DEBUG) BasicIO.out.println("\t\tcalling maintainPriorityRelations");
            dispatcher.maintainPriorityRelations(_owner);
        } else if (comp.compare(current, head) > 0) {
            if (DEBUG) BasicIO.out.println("\t\tcurrent has higher prirority than entryQ head (" + head + ") so removing head and adding current to owners (" + _owner + ") inheritance queue");
            _owner.inheritanceQueue.remove(head.node);
            _owner.inheritanceQueue.add(((PriorityInheritanceOVMThreadImpl)current).node);
            if (DEBUG) BasicIO.out.println("\t\tcalling maintainPriorityRelations");
            dispatcher.maintainPriorityRelations(_owner);
        }
        else {// else - nothing to do
            if (DEBUG) BasicIO.out.println("\t\tcurrent has lower priority than entryQ head (" + head + ") so doing nothing");
        }
        return true;
    }

    /**
     * Clears the inheritance queue of the current thread of the reference to
     * the head locker of this monitor.
     */
    protected void onExit() {
	// note: this.owner = currentThread
	PriorityInheritanceOVMThreadImpl _owner = (PriorityInheritanceOVMThreadImpl) this.owner;
	PriorityInheritanceOVMThreadImpl head = (PriorityInheritanceOVMThreadImpl) entryQ.head();
	if (head != null) {
	    _owner.inheritanceQueue.remove(head.node);
	}
	// this is probably unnecessary if there was noone in the entryQ
	dispatcher.maintainPriorityRelations(_owner);
    }


    protected void onAcquire() {
	PriorityInheritanceOVMThreadImpl _owner = (PriorityInheritanceOVMThreadImpl) this.owner;
	PriorityInheritanceOVMThreadImpl head = (PriorityInheritanceOVMThreadImpl) entryQ.head();
	if (head != null) {
	    _owner.inheritanceQueue.add(head.node);
	    dispatcher.maintainPriorityRelations(_owner);
	}
    }

}

    




