
package test.runtime;

import ovm.core.domain.DomainDirectory;
import ovm.core.execution.CoreServicesAccess;
import ovm.core.execution.Native;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.threads.OVMThread;
import ovm.services.monitors.Monitor;
import ovm.services.monitors.MonitorMapper;
import ovm.services.monitors.QueryableMonitor;
import ovm.services.monitors.RecursiveMonitor;
import s3.services.monitors.BasicMonitorImpl;
import s3.services.threads.JLThread;
import s3.util.PragmaTransformCallsiteIR;
import test.common.TestSuite;
/**
 * Provides some basic test of monitor entry and exit in both contended and
 * uncontended cases. We rely on a scheduling discipline that uses either a
 * non-priority, non-preemptive FIFO scheduling, or a priority scheduling with
 * FIFO within a priority level - otherwise we can't control the order in
 * which threads acquire the monitor.
 *
 * <p>Trying to make this test work regardless of the configuration is 
 * just about impossible. The contention test won't run under a JVM config.
 *
 * @author David Holmes
 */
public class TestSynchronization extends TestThreadsBase {

    public TestSynchronization(long disabled) {
        super("Synchronized block and (non-static) methods");
	doThrow = (disabled & TestSuite.DISABLE_EXCEPTIONS) == 0;
    }


    OVMThread current;
    boolean doThrow;

    protected void init() {
        super.init();
        current = dispatcher.getCurrentThread();
    }

    public void run() {
        basicmappertest();
        nocontentiontest_block();
        if (doThrow)
            illegalMonitorStateTest();
	if (doThrow)
	    nocontentiontest_block_with_exception();
        recursive_nocontentiontest_block();
	if (doThrow)
	    recursive_nocontentiontest_block_with_exception();
        nocontentiontest_method(1);
	if (doThrow)
	    nocontentiontest_method_with_exception(1);
        nocontentiontest_method(5);
	if (doThrow)
	    nocontentiontest_method_with_exception(5);
        simpleContention_block();
	if (false && doThrow)
	    checkDoubleExit();
    }



    Monitor getMonitor(Object o) {
        CoreServicesAccess csa
        = DomainDirectory.getExecutiveDomain().getCoreServicesAccess();
        Monitor  mon = csa.ensureMonitor(VM_Address.fromObject(o).asOop());
        checkNewMonitor(mon);
        return mon;
    }
    
    void releaseMonitor(Object o) {
      	MonitorMapper m = (MonitorMapper)VM_Address.fromObject(o).asAnyOop();
        m.releaseMonitor();
    }

    // utility functions for grouping assertions

    void checkNewMonitor(Monitor mon) {
        A(mon != null, "mon null");
        checkIdleMonitor(mon);
    }

    void checkOwner(Monitor mon, OVMThread owner) {
        A(mon.getOwner() == owner, 
          "owner: " + mon.getOwner() + "!= " + owner);
    }

    void checkEntryCount(Monitor m, int entryCount) {
        if (m instanceof RecursiveMonitor) {
            RecursiveMonitor mon = (RecursiveMonitor) m;
            A(mon.entryCount() == entryCount, 
              "entry count " + mon.entryCount() + " != " + entryCount);
        }
    }        

    void checkEntryQueue(Monitor m, int queueLength, 
                         OVMThread owner, OVMThread qhead) {
        if (m instanceof QueryableMonitor) {
            QueryableMonitor mon = (QueryableMonitor) m;
            A(mon.getEntryQueueSize() == queueLength, 
              "entry queue size " + mon.getEntryQueueSize() + " != " + 
              queueLength);
            if (owner != null) {
                A(!mon.isEntering(owner), "owner is considered entering");
            }
            if (qhead != null) {
                A(mon.isEntering(qhead), "qhead not seen as entering");
            }
        }
        if (m instanceof BasicMonitorImpl) {
            checkInternalEntryQueue((BasicMonitorImpl)m, queueLength, qhead);
        }
    }

    void checkInternalEntryQueue(BasicMonitorImpl mon, int queueLength,
                                 OVMThread qhead) {
        if (queueLength == 0) {
            A(mon.getEntryQueue().isEmpty(), "entry queue not empty");
        }
        else {
            A(!mon.getEntryQueue().isEmpty(), "entry queue is empty");
        }
        if (qhead != null) {
            A(mon.getEntryQueue().head() == qhead, 
              "queue head " + mon.getEntryQueue().head() + " != " + qhead);
        }
    }

    
    void checkIdleMonitor(Monitor mon) {
        checkOwner(mon, null);
        checkEntryCount(mon, 0);
        checkEntryQueue(mon, 0, null, null);
    }

    void checkOwnedUncontended(Monitor mon, OVMThread owner, int entryCount) {
        checkOwner(mon, owner);
        checkEntryCount(mon, entryCount);
        checkEntryQueue(mon, 0, owner, null);
    }

    void checkOwnedContended(Monitor mon, OVMThread owner, int entryCount,
                             int queueLength, OVMThread qhead) {
        checkOwner(mon, owner);
        checkEntryCount(mon, entryCount);
        checkEntryQueue(mon, queueLength, owner, qhead);
    }

    void basicmappertest() {
        setModule("Basic monitor mapper test");
        Object lock = new Object();
        Monitor mon = getMonitor(lock);
        Monitor copy = getMonitor(lock);
        A(mon == copy, "getMonitor returned two different objects");
        Object lock2 = new Object();
        Monitor mon2 = getMonitor(lock2);
        copy = getMonitor(lock2);
        A(mon2 == copy, "getMonitor returned two different objects");
        A(mon != mon2, "mon == mon2");

        // NOTE: these release tests assume that releasing and then asking
        // for a new monitor can't return the same instance. That's not a
        // valid assumption in general.
        releaseMonitor(lock);
        releaseMonitor(lock2);
        A(mon != getMonitor(lock), "new mon == old mon");
        A(mon2 != getMonitor(lock2),"new mon2 == old mon2" );
        releaseMonitor(lock);
        releaseMonitor(lock2);
    }

    void illegalMonitorStateTest() {
        setModule("IllegalMonitorStateException test");
        Object lock = new Object();
        Monitor mon = getMonitor(lock);
        try {
            mon.exit();
            A(false, "No IllegalMonitorStateException thrown");
        }
        catch(IllegalMonitorStateException ex) {
        }
        catch(Throwable t) {
            A(false, "Incorrect exception: " + t);
        }
    }

        
    void nocontentiontest_block() {
        setModule("no contention test - sync block");

        Object lock = new Object();
        Monitor mon = getMonitor(lock);
        synchronized(lock) {
            checkOwnedUncontended(mon, current, 1);
        }
        checkIdleMonitor(mon);
        releaseMonitor(lock); // cleanup for long running tests
    }

    void nocontentiontest_block_with_exception() {
        setModule("no contention test - sync block - with exception");

        Object lock = new Object();
        Monitor mon = getMonitor(lock);
        try {
            synchronized(lock) {
                checkOwnedUncontended(mon, current, 1);
                throw new Error("Oops");
            }
        }
        catch(Error e) {
            checkIdleMonitor(mon);
        }
        releaseMonitor(lock); // cleanup for long running tests
    }

    void recursive_nocontentiontest_block() {
        setModule("recursive - no contention test - sync block");

        Object lock = new Object();
        Monitor mon = getMonitor(lock);
        synchronized(lock) {
            checkOwnedUncontended(mon, current, 1);
            synchronized(lock) {
                checkOwnedUncontended(mon, current, 2);
                synchronized(lock) {
                    checkOwnedUncontended(mon, current, 3);
                    synchronized(lock) {
                        checkOwnedUncontended(mon, current, 4);
                    }
                    checkOwnedUncontended(mon, current, 3);
                }
                checkOwnedUncontended(mon, current, 2);
            }
            checkOwnedUncontended(mon, current, 1);
        }
        checkIdleMonitor(mon);
        releaseMonitor(lock); // cleanup for long running tests
    }

    void recursive_nocontentiontest_block_with_exception() {
        setModule("recursive - no contention test - sync block - with exception");

        Object lock = new Object();
        Monitor mon = getMonitor(lock);
        try {
            synchronized(lock) {
                checkOwnedUncontended(mon, current, 1);
                try {
                    synchronized(lock) {
                        checkOwnedUncontended(mon, current, 2);
                        try {
                            synchronized(lock) {
                                checkOwnedUncontended(mon, current, 3);
                                try {
                                    synchronized(lock) {
                                        checkOwnedUncontended(mon, current, 4);
                                        throw new Error("oops");
                                    }
                                }
                                catch(Error e) {
                                    checkOwnedUncontended(mon, current, 3);
                                    throw e;
                                }
                            }
                        }
                        catch(Error e) {
                            checkOwnedUncontended(mon, current, 2);
                            throw e;
                        }
                    }
                }
                catch(Error e) {
                    checkOwnedUncontended(mon, current, 1);
                    throw e;
                }
            }
        }
        catch(Error e) {
            checkIdleMonitor(mon);
        }
        releaseMonitor(lock); // cleanup for long running tests
    }

    // inner class so we can access mapper etc
    class SyncHelper {
        public final Monitor mon = getMonitor(this);

        public synchronized void syncMethod(OVMThread owner, int depth, int max) {
            checkOwnedUncontended(mon, owner, depth);
            if (depth == max) return;
            else { 
                syncMethod(owner, depth+1, max);
                checkOwnedUncontended(mon, owner, depth);
            }
        }

        public synchronized void syncMethodWithThrow(OVMThread owner, int depth, int max) {
            checkOwnedUncontended(mon, owner, depth);
            if (depth == max) throw new Error("oops");
            else { 
                try {
                    syncMethodWithThrow(owner, depth+1, max);
                }
                catch(Error e) {
                    checkOwnedUncontended(mon, owner, depth);
                    throw e;
                }
            }
        }

    }


    // with methods we can use the same structure for recursive and 
    // non-recursive usage

    void nocontentiontest_method(int recursionLevel) {
        setModule("no contention test - sync method - recursion level " + recursionLevel);
        SyncHelper lock = new SyncHelper();
        lock.syncMethod(current, 1, recursionLevel);
        checkIdleMonitor(lock.mon);
    }

    void nocontentiontest_method_with_exception(int recursionLevel) {
        setModule("no contention test - sync method - with exception - recursion level " + recursionLevel);

        SyncHelper lock = new SyncHelper();
        try {
            lock.syncMethodWithThrow(current, 1, recursionLevel);
        }
        catch(Error e) {
            checkIdleMonitor(lock.mon);
        }
    }


    void simpleContention_block() {
        setModule("simple contention - block");

        final SyncHelper lock = new SyncHelper();
        final OVMThread main = dispatcher.getCurrentThread();

        // there is no generic way to create a thread to run our code
        // without knowing what type of thread we are dealing with - or
        // by having all threads support Runnables.
        // So, for now at least, we only deal with JLThreads
        if (! (main instanceof JLThread)) {
            Native.print(" SKIPPED Contention Test: not working with JLThreads");
            return;
        }

        Runnable r = new Runnable() {
                OVMThread me = null;
                public void run() {
                    me = dispatcher.getCurrentThread();
                    checkOwnedUncontended(lock.mon, main, 1);
                    synchronized(lock) {
                        checkOwnedUncontended(lock.mon, me, 1);
                        dispatcher.yieldCurrentThread();  // let main run - nothing will change
                        checkOwnedUncontended(lock.mon, me, 1);
                    }
                    checkIdleMonitor(lock.mon);
                }
            };

        JLThread thread2 = new JLThread(r, "Thread-2");
        thread2.start();
        // main thread keeps running
        synchronized(lock) {
            checkOwnedUncontended(lock.mon, main, 1);
            dispatcher.yieldCurrentThread();  // thread-2 will try to lock and block
            checkOwnedContended(lock.mon, main, 1, 1, thread2);
        }
        // no contextswitch on monitor release as equal priority threads
        dispatcher.yieldCurrentThread(); // switch back to thread-2 so it can acquire
        checkOwnedUncontended(lock.mon, thread2, 1);
        dispatcher.yieldCurrentThread(); // switch back to thread-2 so it can release
        checkIdleMonitor(lock.mon);
    }

    void monitorExit() throws BCmonitorExit {
	COREfail("monitorExit not rewritten");
    }

    static class BCmonitorExit extends PragmaTransformCallsiteIR {
	static {
	    register(BCmonitorExit.class.getName(),
		     new byte[] { (byte) MONITOREXIT });
	}
    }

    boolean always() { return true; }

    synchronized void doubleExit() {
	boolean gotEx = false;
	monitorExit();
	try {
	    // Some versions of javac will try to shrink exception
	    // handlers to exclude return statements.  Make sure that
	    // the return statement is surrounded by two statements
	    // that CAN throw exceptions.
	    if (always())
		return;
	    always();
	} catch (IllegalMonitorStateException _) {
	    gotEx = true;
	}
	check_condition(gotEx, "exception not thrown on double exit");
    }

    void checkDoubleExit() {
	try {
	    doubleExit();
	} catch (IllegalMonitorStateException e) {
	    COREfail(e + " not thrown by return");
	}
    }
}





