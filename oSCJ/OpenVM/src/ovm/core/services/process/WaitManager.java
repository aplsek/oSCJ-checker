// $Header: /p/sss/cvs/OpenVM/src/ovm/core/services/process/WaitManager.java,v 1.9 2004/10/13 17:11:16 pizlofj Exp $

package ovm.core.services.process;

import ovm.core.services.events.*;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.VM_Address;
import ovm.core.execution.*;
import ovm.core.stitcher.*;
import ovm.util.*;
import s3.util.*;
import ovm.core.*;

/**
 * A lazily-initialized service that manages asynchronously waiting on
 * processes to die.
 * <p>
 * By the way, the current implementation is a total hack.  It won't perform
 * well and it doesn't do nearly as much error checking as it should.
 *
 * @author Filip Pizlo
 */
public class WaitManager {

    public static final boolean DEBUG=false;

    private WaitManager() {}
    
    public static interface WaitCallback {
        public void died(int pid,int status);
    }

    public final static WaitCallback noOpCallback=
	new WaitCallback(){
	    public void died(int pid,int status) {
		// no-op
	    }
	};
    
    private static final class NativeHelper implements NativeInterface {
        static native void registerPid(int pid);
	static native void unregisterPid(int pid);
        static native boolean getDeadPid(int[] pid, int[] status);
    }
    
    private static WaitCallback[] waitCallbacks=new WaitCallback[1024];
    private static int[] waitPids=new int[1024];
    private static int numWaitCallbacks=0;

    private static void addWaitCallback(int pid, WaitCallback cback)
	throws PragmaAtomic,
	       PragmaNoBarriers {
        // need some error checking here.
        waitCallbacks[numWaitCallbacks]=cback;
        waitPids[numWaitCallbacks++]=pid;
        NativeHelper.registerPid(pid);  // this will register appropriate signal
                                        // handlers if necessary
    }

    public static boolean removeWaitCallback(int pid, WaitCallback cback)
	throws PragmaAtomic {
	for (int i=0;i<numWaitCallbacks;++i) {
	    if (waitPids[i]==pid && waitCallbacks[i]==cback) {
		waitCallbacks[i]=waitCallbacks[--numWaitCallbacks];
		waitPids[i]=waitPids[numWaitCallbacks];
		NativeHelper.unregisterPid(pid);
		return true;
	    }
	}
	return false;
    }

    private static int[] pid=new int[1];
    private static int[] status=new int[1];

    private static void collectDeadPids() throws PragmaNoBarriers {
	// this is O(n^2)
	try {
	    while (NativeHelper.getDeadPid(pid,status)) {
	        MemoryManager.the().assertSingleReplica( VM_Address.fromObjectNB(pid) );
	        MemoryManager.the().assertSingleReplica( VM_Address.fromObjectNB(status) );
	        
		if (DEBUG) {
		    Native.print_string("got a dead pid!\n");
		    Native.print_string("pid: ");
		    Native.print_int(pid[0]);
		    Native.print_string("\n");
		    Native.print_string("status: ");
		    Native.print_int(status[0]);
		    Native.print_string("\n");
		}
		for (int i=0;i<numWaitCallbacks;++i) {
		    if (DEBUG) {
			Native.print_string("i = ");
			Native.print_int(i);
			Native.print_string("\n");
			Native.print_string("waitPid = ");
			Native.print_int(waitPids[i]);
			Native.print_string("\n");
		    }
		    if (waitPids[i]==pid[0]) {
			if (DEBUG) {
			    Native.print_string("notifying\n");
			}
			waitCallbacks[i].died(pid[0],status[0]);
			
			if (DEBUG) {
			    Native.print_string("removing\n");
			    Native.print_string("numWaitCallbacks = ");
			    Native.print_int(numWaitCallbacks);
			    Native.print_string("\n");
			}
			waitCallbacks[i]=waitCallbacks[--numWaitCallbacks];
			waitPids[i]=waitPids[numWaitCallbacks];
			waitCallbacks[numWaitCallbacks]=null;
			waitPids[numWaitCallbacks]=0;
			--i;
			if (DEBUG) {
			    Native.print_string("removed.\n");
			}
		    }
		}
	    }
	} catch (Throwable e) {
	    throw Executive.panicOnException(e,"in WaitManager.collectDeadPids()");
	}
    }

    private static EventManager.EventProcessor myProcessor=
        new EventManager.EventProcessor(){
	    public String eventProcessorShortName() {
		return "wait";
	    }
	    public void eventFired() {
		collectDeadPids();
	    }
	};
    
    private static boolean processorRegistered=false;
    
    public static void waitForPid(int pid,
                                  WaitCallback cback)
	throws PragmaAtomic {
        if (!processorRegistered) {
            EventManager em = ((EventServicesFactory) IOServiceConfigurator
			       .config
			       .getServiceFactory(EventServicesFactory.name))
                .getEventManager();
            if (em==null) {
                throw new OVMError.Configuration("need a configured event manager");
            }
            
            em.addEventProcessor(myProcessor);
            
            processorRegistered=true;
        }
	if (DEBUG) {
	    Native.print_string("calling addWaitCallback()\n");
	}
	addWaitCallback(pid, cback);
	if (DEBUG) {
	    Native.print_string("calling collectDeadPids()\n");
	}
	collectDeadPids();
	if (DEBUG) {
	    Native.print_string("returning from waitForPid()\n");
	}
    }
}

