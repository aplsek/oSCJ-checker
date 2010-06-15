// $Header: /p/sss/cvs/OpenVM/src/test/runtime/TestSignalMonitor.java,v 1.18 2006/04/08 21:08:16 baker29 Exp $

package test.runtime;

import ovm.core.execution.OVMSignals;
import ovm.core.services.threads.OVMDispatcher;
import ovm.core.services.threads.OVMThread;
import ovm.core.services.threads.ThreadManager;
import ovm.core.stitcher.SignalServicesFactory;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.ThreadDispatchServicesFactory;
import ovm.core.stitcher.ThreadServicesFactory;
import ovm.core.stitcher.TimerServicesFactory;
import ovm.services.events.SignalMonitor;
import ovm.services.threads.UserLevelThreadManager;
import s3.services.threads.JLThread;
import test.common.TestBase;
import ovm.core.services.io.BasicIO;
import ovm.core.services.timer.TimerManager;
import s3.core.services.timer.TimerManagerImpl;
/**
 * Test the signal monitor functionality. This is to do in a non-interactive
 * fashion as you need something to generate the signals. We could provide a
 * hook that allows signals to be generated directly, but for now we only
 * use signals that we expect to be occurring ie SIGALRM.
 *
 * @author David Holmes
 */
public class TestSignalMonitor extends TestBase {

    public TestSignalMonitor() {
        super("Signal Monitor");
    }


    private SignalMonitor sigMon;
    private OVMDispatcher dispatcher;
    private OVMThread current;
    
    private TimerManager timer;

    protected void init() {
        ThreadServicesFactory tsf = (ThreadServicesFactory) 
            ThreadServiceConfigurator.config.getServiceFactory(ThreadServicesFactory.name);
        ThreadManager tm = tsf.getThreadManager();
        if (tm instanceof UserLevelThreadManager) {
        }
        ThreadDispatchServicesFactory tdsf = (ThreadDispatchServicesFactory) 
            ThreadServiceConfigurator.config.getServiceFactory(ThreadDispatchServicesFactory.name);
        dispatcher = tdsf.getThreadDispatcher();
        if (dispatcher == null) {
            COREfail("Configuration error: no dispatcher defined");
        }

        SignalServicesFactory ssf = (SignalServicesFactory)
            ThreadServiceConfigurator.config.getServiceFactory(SignalServicesFactory.name);
        sigMon = ssf.getSignalMonitor();
        if (sigMon == null) {
            COREfail("Configuration error: no signal monitor defined");
        }
	
	timer=((TimerServicesFactory)ThreadServiceConfigurator.config.getServiceFactory(TimerServicesFactory.name)).getTimerManager();
    }

    // counts the number of threads that have completed
    volatile int tCount = 0;

    public void run() {
        current = dispatcher.getCurrentThread();
        
	if (timer instanceof TimerManagerImpl) {
	    checkWaitableSignals();
	    waitForBadSig();
	    waitFor(OVMSignals.OVM_SIGALRM);
	    waitForVec(OVMSignals.OVM_SIGALRM);
	    handlerTest();
	    watcherTest();
        
	    if (current instanceof JLThread) {
		//d("running: MTTest");
		runMTTest();
		//            interactiveWatcherTest();
	    }
	    else {
		BasicIO.out.print(" MT Signal Tests SKIPPED: not working with JLThreads");
	    }
	} else {
	    BasicIO.out.print(" skipping because we're not using the default timer manager");
	}
    }


    private void handlerTest() {
        setModule("SignalHandler Tests");
        final int signal = OVMSignals.OVM_SIGALRM;

        SignalMonitor.SignalHandler h = new SignalMonitor.SignalHandler() {
                int firings = 0;
                public void fire(int sig, int count) {
                    ++firings;
//                     BasicIO.out.print(">>> ");
//                     BasicIO.out.print(OVMSignals.sigNames[sig]);
//                     BasicIO.out.print(" Firing ");
//                     BasicIO.out.print(firings);
//                     BasicIO.out.print(" : handler fired with count ");
//                     BasicIO.out.println(count);

                    if (firings == 10) {
                        BasicIO.out.println(">>> Removing sigAlrm handler");
                        sigMon.removeSignalHandler(this, signal);
                    }
                }
            };

       // BasicIO.out.println("Adding sigAlrm handler");
        sigMon.addSignalHandler(h, signal);
    }

/* unused
    private void interactiveWatcherTest() {
        setModule("SignalWatcher Interactive Test");
        SignalMonitor.SignalWatcher w = sigMon.newSignalWatcher();
        int signal = OVMSignals.OVM_SIGUSR1;
        w.addWatch(signal);
        int[] counts = w.getWatchCounts();
        for (int i = 0; i < counts.length; i++) {
            COREassert(counts[i] == -1 || i == signal, "non -1 count on single watcher");
        }
        BasicIO.out.println("Watching " + OVMSignals.sigNames[signal] +
                            " sleeping for 10 seconds");
        JLThread.sleepUninterruptible(10000, 0);
        counts = w.getWatchCounts();
        BasicIO.out.println("Waiting watch count was " + counts[signal]);
        for (int i = 0; i < counts.length; i++) {
            COREassert(counts[i] == -1 || i == signal);
        }
        w.removeWatch(signal);
        counts = w.getWatchCounts();
        for (int i = 0; i < counts.length; i++) {
            COREassert(counts[i] == -1, "non -1 count on empty watcher");
        }

        w.addWatch(signal);
        counts = w.getWatchCounts();
        for (int i = 0; i < counts.length; i++) {
            COREassert(counts[i] == -1 || i == signal, "non -1 count on single watcher");
        }
        counts = w.waitForSignal();
        BasicIO.out.println("Waiting for " + OVMSignals.sigNames[signal]);
        counts = w.waitForSignal();
        BasicIO.out.println("Waiting watch count was " + counts[signal]);
        for (int i = 0; i < counts.length; i++) {
            COREassert(counts[i] == -1 || i == signal);
        }
        w.removeWatch(signal);
        counts = w.getWatchCounts();
        for (int i = 0; i < counts.length; i++) {
            COREassert(counts[i] == -1, "non -1 count on empty watcher");
        }
    }
*/
    private void watcherTest() {
        setModule("SignalWatcher Tests");
        SignalMonitor.SignalWatcher w = sigMon.newSignalWatcher();
        int[] counts =  w.getWatchCounts();
        for (int i = 0; i < counts.length; i++) {
            check_condition(counts[i] == -1, "non -1 count on new watcher");
        }
        counts = w.waitForSignal();
        for (int i = 0; i < counts.length; i++) {
            check_condition(counts[i] == -1, "non -1 count on new watcher");
        }

        final int signal = OVMSignals.OVM_SIGALRM;
        w.addWatch(signal);
        counts =  w.getWatchCounts();
//         BasicIO.out.println("Initial watched count was " + counts[signal]);
        for (int i = 0; i < counts.length; i++) {
            check_condition(counts[i] == -1 || i == signal);
        }
//         BasicIO.out.println("Waiting for " + OVMSignals.sigNames[signal]);
        counts = w.waitForSignal();
//         BasicIO.out.println("Waiting watch count was " + counts[signal]);
        for (int i = 0; i < counts.length; i++) {
            check_condition(counts[i] == -1 || i == signal);
        }
        w.removeWatch(signal);
        counts =  w.getWatchCounts();
        for (int i = 0; i < counts.length; i++) {
            check_condition(counts[i] == -1, "non -1 count on empty watcher");
        }
        counts = w.waitForSignal();
        for (int i = 0; i < counts.length; i++) {
            check_condition(counts[i] == -1, "non -1 count on empty watcher");
        }
    }

     void runMTTest() {
        // set up threads to wait for any allowed signal in the system.
        // EXCEPT for SIGINT otherwise we'll kill the default "kill" behaviour
        // SIGALRM should release them.
        
        final int[] allSigs = new int[OVMSignals.NSIGNALS];
        for (int i = 0; i < allSigs.length; i++) {
            if (sigMon.canMonitor(i)) {
                allSigs[i] = 1;
            }
        }
        allSigs[OVMSignals.OVM_SIGINT] = 0;

        Runnable r = new Runnable() {
                public void run() {
                    try {
                        int[] sigs = new int[OVMSignals.NSIGNALS];
                        System.arraycopy(allSigs, 0, sigs, 0, sigs.length);
                        JLThread cur = JLThread.currentThread();
                        check_condition(sigMon.waitForSignal(sigs), "Error waiting for all signals in " + cur);
                        boolean fired = false;
                        for (int i = 0; i < sigs.length; i++) {
                            if (sigs[i] > 0 ) {
                                fired = true;
//                                 BasicIO.out.println(current + " " + 
//                                                     OVMSignals.sigNames[i] 
//                                                     + " occurred " + sigs[i] 
//                                                     + " times while waiting"); 
                            }
                        }
                        check_condition(fired, "No fired signals marked!");
                        //d("stuff happened in the other thread.");
                    }
                    finally {
                        synchronized(current) { tCount++; }
                    }
                }
            };

        // this must be within the hard-coded limit of the SignalMonitorImpl
        // allowing for other system uses. (current max is 8 so 6 is safe).
        final int nThreads = 6;
        
        for (int i = 0; i < nThreads; i++) {
            new JLThread(r).start();
        }
        while (tCount < nThreads) JLThread.yield();
//         BasicIO.out.println("All threads received the signal they were waiting for\n");
    }

    // Does a basic test on a known good and some known bad signals
    private void checkWaitableSignals() {
        setModule("Checking waitable signals");
        // SIGALRM is the only sig we can almost guarantee to happen
        check_condition(sigMon.canMonitor(OVMSignals.OVM_SIGALRM), 
                   "Can't wait for " + OVMSignals.sigNames[OVMSignals.OVM_SIGALRM]);

        // SIGSEGV is a reserved signal by the OVM Engine (or should be)
        check_condition(!sigMon.canMonitor(OVMSignals.OVM_SIGSEGV), 
                   "Can wait for " + OVMSignals.sigNames[OVMSignals.OVM_SIGSEGV]);

        // SIGKILL is a reserved signal as it can't be caught
        check_condition(!sigMon.canMonitor(OVMSignals.OVM_SIGKILL), 
                   "Can wait for " + OVMSignals.sigNames[OVMSignals.OVM_SIGKILL]);

        // SIGSTOP is a reserved signal as it can't be caught
        check_condition(!sigMon.canMonitor(OVMSignals.OVM_SIGSTOP), 
                   "Can wait for " + OVMSignals.sigNames[OVMSignals.OVM_SIGSTOP]);

        // out of range values 
        check_condition(!sigMon.canMonitor(-1), 
                   "Can wait for sig -1!");
        check_condition(!sigMon.canMonitor(OVMSignals.NSIGNALS), 
                   "Can wait for sig NSIGNALS!");

    }

    // checks waiting on a bad signal is rejected
    private void waitForBadSig() {
        setModule("Testing invalid signals");
        int bad = OVMSignals.OVM_SIGKILL;
        int good = OVMSignals.OVM_SIGALRM;
        check_condition(sigMon.waitForSignal(bad) == 0, 
                   "Allowed to wait for " + OVMSignals.sigNames[bad]);
        int[] sigs = new int[OVMSignals.NSIGNALS];
        sigs[bad] = 1;
        sigs[good] = 1;
        check_condition(!sigMon.waitForSignal(sigs), 
                   "Allowed to wait for " + OVMSignals.sigNames[bad] + 
                   " in vector");
        for (int i = 0; i < sigs.length; i++) {
            check_condition(sigs[i] == 0 || (sigs[i] == 1 && i == bad),
                       "Array not cleared properly on bad sig");
        }
        // now try every signal at once and check the array is cleared
        // properly
        for (int i = 0; i < sigs.length; i++) {
            sigs[i] = 1;
        }
        check_condition(!sigMon.waitForSignal(sigs), 
                   "Allowed to wait for all sigs in sig vector");
        int found = 0;
        for (int i = 0; i < sigs.length; i++) {
            check_condition(sigs[i] == 0 || (sigs[i] == 1 && (found++ == 0)),
                       "Array not cleared properly on all sigs");
        }
    }

    // wait for the given signal using waitForSignal(int sig) 
    private void waitFor(int sig) {
        String name = OVMSignals.sigNames[sig];
        setModule("waiting for " + name + " using waitForSignal(int sig)");
//         BasicIO.out.println("About to wait for " + name);
        int count = sigMon.waitForSignal(sig);
        check_condition(count > 0, "Error waiting for " + name);
//         BasicIO.out.println(name + " occurred " + count + " times while waiting"); 
    }


    // waits for the given signal using the waitForSignal(int[] sigvector) method
    private void waitForVec(int sig) {
        String name = OVMSignals.sigNames[sig];
        setModule("waiting for " + name + " using waitForSignal(int[] sigvec)");
        int[] sigs = new int[OVMSignals.NSIGNALS];
        sigs[sig] = 1;
//         BasicIO.out.println("About to wait for " + name);
        check_condition(sigMon.waitForSignal(sigs), "Error waiting for " + name);
       // int count = sigs[sig];
//         BasicIO.out.println(name + " occurred " + count + " times while waiting"); 
    }

    
}

