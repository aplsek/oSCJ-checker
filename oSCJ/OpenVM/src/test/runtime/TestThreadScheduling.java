
package test.runtime;
import ovm.core.execution.Native;
import ovm.services.threads.PriorityOVMDispatcher;
import ovm.services.threads.PriorityOVMThread;
import ovm.util.HashSet;
import ovm.util.Iterator;
import s3.services.threads.JLThread;
/**
 * A range of thread scheduling tests. We rely on priority preemptive
 * scheduling with FIFO within a priority level to identify the
 * correctness of the thread operations.
 *
 * <p><b>NOTE:</b>This test will only run if the executing thread is a
 * JLThread instance.
 *
 * @author David Holmes
 *
 */
public class TestThreadScheduling extends TestSyncBase {

    /** Hide our parent dispatcher with a priority one.*/  // why?
    protected PriorityOVMDispatcher dispatcher;

    protected void init() {
        super.init();
        if (super.dispatcher instanceof PriorityOVMDispatcher) {
            dispatcher = (PriorityOVMDispatcher) super.dispatcher;
        }
    }

    public TestThreadScheduling() {
        super("Various thread related tests");
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
/*
    private void printLog() {
        for (LogEntry e = logger; e != null; e = e.next) {
            BasicIO.out.println(e.prio + ", " + e.timestamp);
        }
    }
*/
    private void log() {
        LogEntry e = new LogEntry(
            ((PriorityOVMThread)dispatcher.getCurrentThread()).getPriority(),
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
        if (dispatcher == null) {
            p(" SKIPPED: need configuration with priority dispatcher");
        } else {
            if (dispatcher.getCurrentThread() instanceof JLThread) {
                test1();
                iteratorTest();
            } else {
                p(" SKIPPED: not working with JLThreads");
            }
        }
    }

    public void test1() {
        // because we are using JLThread instances we use priority range
        // information from that class.
	int maxPrio = JLThread.getMaxPriority();
	int minPrio = JLThread.getMinPriority();

        final int nThreads = 5;
        A(maxPrio - nThreads - 1 >= minPrio, "too many threads for available priority levels");

        final int iters = 5;

        Runnable r = new Runnable() {
                public void run() {
                    for (int i = 0; i < iters; i++) {
                        log();
                        dispatcher.yieldCurrentThread();
                    }
                }
            };

        PriorityOVMThread testThread = (PriorityOVMThread) dispatcher.getCurrentThread();
        // main thread needs top priority
        int origPriority = testThread.getPriority();
        dispatcher.setPriority(testThread, maxPrio);
        try {
            log();
            // start all the threads
            for (int i = 1; i <= nThreads; i++) {
                JLThread t = new JLThread(r, "Thread-"+i);
                t.setPriority(minPrio + 1); 
                t.start();
                log();
            }
            // drop our priority so we don't run again until all
            // others have terminated
            dispatcher.setPriority(testThread, minPrio);
            // don't log again as we'll break the order
//        printLog();
            checkLog();
        }
        finally {
            // should restore original priority so other tests have a sane
            // starting point
            dispatcher.setPriority(testThread, origPriority);
        }
    }

    public void iteratorTest() {

        final int nThreads = 10; // max == 10: 0 .. 9
        final boolean[] stopflags = new boolean[nThreads];
        HashSet threads = new HashSet(nThreads);

        Iterator iter = dispatcher.iterator();
        JLThread main = JLThread.currentThread();

        A(iter.hasNext(), "empty iterator");
        A(iter.next() == main, "wrong thread");
        A(!iter.hasNext(), "too many threads initially");

        // we name the thread "0", "1" etc because we don't have the string to
        // primitive conversion routines - so we use a hack.

        Runnable r = new Runnable() {
                public void run() {
                    String me = JLThread.currentThread().getName();
                    char digit = me.charAt(0);
                    int index = digit - '0'; 
                    boolean stopped = false;
                    while(!stopped) {
                        synchronized(stopflags) {
                            stopped = stopflags[index];
                        }
                        if (!stopped) {
                            JLThread.yield();
                        }
                    }
                }
            };

        // NOTE: it is critical that the current thread and the new
        // threads have the same priority as we use yield to reschedule
        // (one day we will have join() using the wait() mechanism

        for( int i = 0; i < nThreads; i++) {
            JLThread t = new JLThread(r, i+"");
            threads.add(t);
            t.start();
            iter = dispatcher.iterator();
            for(int j = 0; j <= i+1; j++) {
                A(iter.hasNext(), "missing thread");
                JLThread t2 = (JLThread)iter.next();
                A(threads.contains(t2) || t2 == main, "unknown thread!");
            }
            A(!iter.hasNext(), "too many threads after additions");
        }

        // now stop each thread, wait for it to die and recheck the
        // iterator from the dispatcher
        A(threads.size() == nThreads, "problem with hashset");
        Iterator threadIter = threads.iterator();
        while(threadIter.hasNext()) {
            JLThread t3 = (JLThread) threadIter.next();
            threadIter.remove();
            int index = t3.getName().charAt(0) - '0';     
            synchronized(stopflags) {
                stopflags[index] = true;
            }
            while (t3.isAlive()) {
                JLThread.yield();
            }
            iter = dispatcher.iterator();

            for(int i = threads.size(); i >= 0; i--) {
                A(iter.hasNext(), "missing thread after stop");
                JLThread t4 = (JLThread) iter.next();
                A(t4 != t3, "dead thread found");
            }
            A(!iter.hasNext(), "too many threads after stop");
        }

        iter = dispatcher.iterator();
        A(iter.hasNext(), "empty iterator");
        A(iter.next() == main, "wrong thread at end");
        A(!iter.hasNext(), "too many items at end");
        
    }

}











