 
package test.runtime;
import ovm.core.execution.Native;
import ovm.services.realtime.RealtimeOVMThread;
import ovm.services.realtime.RealtimePriorityDispatcher;
import ovm.services.threads.PriorityOVMDispatcher;
import ovm.services.threads.PriorityOVMThread;
import s3.services.realtime.RealtimeBaseThread;

/**
 * Duplicate of {@link TestThreadScheduling} but using 
 * {@link RealtimeBaseThread}.
 * <p>A range of thread scheduling tests. We rely on priority preemptive
 * scheduling with FIFO within a priority level to identify the
 * correctness of the thread operations. This test will work with either a
 * normal priority dispatcher or the RT priority dispatcher - it will be
 * skipped if there is no priority dispatcher.
 *
 * @author David Holmes
 *
 */
public class TestRealtimeThreadScheduling extends TestSyncBase {

    /** Hide our parent dispatcher with a priority one.*/
    protected PriorityOVMDispatcher dispatcher_;

    protected void init() {
        super.init();
        if (super.dispatcher instanceof PriorityOVMDispatcher) {
            dispatcher_ = (PriorityOVMDispatcher) super.dispatcher;
        }
    }

    public TestRealtimeThreadScheduling() {
        super("Basic realtime thread scheduling tests");
    }

    static class LogEntry {
        int prio;
        long timestamp;
        LogEntry next;

        public LogEntry(int prio, long timestamp) {
            this.prio = prio;
            this.timestamp = timestamp;
        }
    }

    private LogEntry logger; 

    private void log() {
        LogEntry e = new LogEntry(
            ((RealtimeOVMThread)dispatcher_.getCurrentThread()).getPriority(),
            Native.getClockTickCount()
            );
        e.next = logger;
        logger = e;
    }

    // the test has to be arranged so that things are logged in
    // priority order. For equal priority they must be in time order
    private void checkLog() {
        if (logger == null || logger.next == null) {
            return;
        }
        int lastPrio = logger.prio;
        long lastTime = logger.timestamp;

        for (LogEntry e = logger; e != null; e = e.next) {
            A((e.prio > lastPrio) || 
              (e.prio == lastPrio && e.timestamp <= lastTime),
              "out of order: current-prio = " + e.prio 
              + ", previous prio = " + lastPrio + ", current-timestamp = "
              + e.timestamp + ", previous timestamp = " + lastTime);

            lastPrio = e.prio;
            lastTime = e.timestamp;
        }
    }

    public void run() {
        if (dispatcher_ == null) {
            p(" SKIPPED: need configuration with priority dispatcher");
            return;
        }
        else if (dispatcher_ instanceof ovm.services.java.JavaDispatcher) {
            p(" SKIPPED: won't work with JavaDispatcher");
            return;
        }
        // needs to work with both RT and normal dispatcher
        int maxNonRTPrio = dispatcher_.getMaxPriority();
        int minNonRTPrio = dispatcher_.getMinPriority();
        int maxRTPrio;
        int minRTPrio;

        PriorityOVMThread current = (PriorityOVMThread) dispatcher_.getCurrentThread();

        if (dispatcher_ instanceof RealtimePriorityDispatcher) {
            minRTPrio = ((RealtimePriorityDispatcher)dispatcher_).getMinRTPriority();
            maxRTPrio = ((RealtimePriorityDispatcher)dispatcher_).getMaxRTPriority();
        }
        else {
            minRTPrio = minNonRTPrio;
            maxRTPrio = maxNonRTPrio;
        }

        int origPriority = current.getPriority();

        // if the current thread is a realtime thread then we can perform
        // the test directly, otherwise we can't raise the priority of a
        // non-RT thread above those of the threads we will create - in which
        // case we need to create a RT thread just to run the test. To make
        // things simple we drop our priority and create a new RT thread
        // regardless.

        dispatcher_.setPriority(current, 
                               current instanceof RealtimeOVMThread ? minRTPrio : minNonRTPrio); // ensure low prio
        final int tempMinPrio = minRTPrio + 1; // > current thread
        final int tempMaxPrio = maxRTPrio - 1; // < RTT we will create
        Runnable r = new Runnable() { 
                public void run() { test1(tempMinPrio, tempMaxPrio); }
            };

        RealtimeBaseThread rtt = new RealtimeBaseThread("Max prio RTT", r);
        dispatcher_.setPriority(rtt, maxRTPrio);
        dispatcher_.startThread(rtt);
        // should only get here when everyone else has finished
        try {
            checkLog();
        }
        finally {
            dispatcher_.setPriority(current, origPriority);
        }
    }

    private void test1(int minPrio, int maxPrio) {
        final int nThreads = 5;
        final int iters = 5;
        A( maxPrio - nThreads >= minPrio, "too many threads for available priority values");

        Runnable r = new Runnable() {
                public void run() {
                    for (int i = 0; i < iters; i++) {
                        log();
                        dispatcher_.yieldCurrentThread();
                    }
                }
            };

        log();

        // start all the threads
        for (int i = 1 ; i <= nThreads; i++) {
            RealtimeBaseThread t = new RealtimeBaseThread("RT-Thread-"+i, r);
            t.setPriority(minPrio + i); 
            dispatcher_.startThread(t);
            log();
        }
    }

}






