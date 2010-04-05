// $Header: /p/sss/cvs/OpenVM/src/test/runtime/TestSleep.java,v 1.12 2004/08/25 01:12:29 dholmes Exp $

package test.runtime;

import test.common.TestBase;

import ovm.core.services.threads.OVMThread;
import ovm.core.services.threads.OVMDispatcher;
import ovm.core.services.threads.ThreadManager;
import ovm.services.threads.TimedSuspensionThreadManager;
import ovm.services.threads.UserLevelThreadManager;

import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.ThreadDispatchServicesFactory;
import ovm.core.stitcher.ThreadServicesFactory;


import ovm.core.services.timer.TimeConversion;
import ovm.core.execution.Native;

import s3.services.threads.JLThread;

/**
 *
 * Test sleep and absolute sleep functions for different times to ensure
 * that sleeps never return early. You'd have to print out the info and
 * examine it to see if they are sleeping longer than expected.
 * This is a low-level thread-manager test that should work for most 
 * practical configurations.
 *
 * @author Filip Pizlo, David Holmes
 */
public class TestSleep extends TestBase {

    // semi-random range of sleep times: 0, 1, 10, 100, 1000 are the main
    // ones, the others are there just in case of any bugs related to
    // exact multiples of the tick rate
    // NOTE: 0 is not valid for use with the TimedSuspensionThreadManager but 
    //       is with 'higher-level' API's like JLThread.sleep. So we use 0 as a
    //       value but skip it for the raw TSTM tests. Hence 0 must be the 
    //       first entry.
    long[] sleepTimes = { 0, 1, 3, 10, 12, 23, 50, 67, 100, 156, 500, 743, 1000 };

    long[] sleepNanos;

    public TestSleep() {
        super("Sleep");
        sleepNanos = new long[sleepTimes.length];
        for (int i = 0; i < sleepTimes.length; i++ ) {
            sleepNanos[i] = TimeConversion.NANOS_PER_MILLI * sleepTimes[i];
        }
    }


    private TimedSuspensionThreadManager sleepMan;
    private UserLevelThreadManager threadMan;
    private OVMDispatcher dispatcher;
    private OVMThread current;

    protected void init() {
        ThreadServicesFactory tsf = (ThreadServicesFactory) 
            ThreadServiceConfigurator.config.getServiceFactory(ThreadServicesFactory.name);
        ThreadManager tm = tsf.getThreadManager();
        if (tm instanceof TimedSuspensionThreadManager) {
            sleepMan = (TimedSuspensionThreadManager) tm;
        }
        if (tm instanceof UserLevelThreadManager) {
            threadMan = (UserLevelThreadManager) tm;
        }
        ThreadDispatchServicesFactory tdsf = (ThreadDispatchServicesFactory) 
            ThreadServiceConfigurator.config.getServiceFactory(ThreadDispatchServicesFactory.name);
        dispatcher = tdsf.getThreadDispatcher();
        if (dispatcher == null) {
            COREfail("Configuration error: no dispatcher defined");
        }
    }

    public void run() {
        if (sleepMan == null ) {
            p(" SKIPPED: not working with TimedSuspensionThreadManager");
            return;
        }
        current = dispatcher.getCurrentThread();

        sleepRel();
        sleepAbsolute();

        if (current instanceof JLThread) {
            JLThreadSleepRel();
            JLThreadSleepAbsolute();
        }
        else {
            p(" SKIPPED: not working with JLThreads");
        }

    }

    private void JLThreadSleepRel() {
        setModule("JLThread sleep relative");
        for (int i = 0; i < sleepNanos.length; i++ ) {
            long start = Native.getCurrentTime();
            long elapsed = 0;
            JLThread.sleepUninterruptible(sleepTimes[i], 0);
            elapsed = Native.getCurrentTime() - start;
            verbose_p("sleep blocked: elapsed-expected = " + 
               (elapsed-sleepNanos[i]) + " ns\n");
            check_condition(elapsed >= sleepNanos[i], 
                       "Didn't sleep long enough: " + 
                       (elapsed / TimeConversion.NANOS_PER_MILLI) + 
                       " ms instead of " + sleepTimes[i] +
                       " ms (diff = " + (elapsed-sleepNanos[i]) + " ns");
        }
    }

    private void JLThreadSleepAbsolute() {
        setModule("JLThread sleep absolute");
        for (int i = 0; i < sleepNanos.length; i++ ) {
            long start = Native.getCurrentTime();
            long end = 0;
            if (JLThread.sleepAbsoluteUninterruptible(start+sleepNanos[i])) {
                end = Native.getCurrentTime();
                verbose_p("sleepAbsolute blocked: actual-expected = " + 
                   (end-(start+sleepNanos[i])) + " ns\n");
            }
            else {
                end = Native.getCurrentTime();
                verbose_p("sleepAbsolute no-block: actual-expected = " + 
                   (end-(start+sleepNanos[i])) + "ns \n");
            } 
            check_condition(end >= (start+sleepNanos[i]), 
                       "Didn't sleep long enough by: " + 
                       (end - (start+sleepNanos[i])) +
                       " ns");
        }

    }


    private void sleepRel() {
        setModule("thread manager sleep relative");
        if (threadMan == null ) {
            p(" TEST SKIPPED: not working with UserLevelThreadManager");
        }

        try {
            // skip sleepNanos[0] 
            for (int i = 1; i < sleepNanos.length; i++ ) {
                boolean enabled = threadMan.setReschedulingEnabled(false);
                try {
                    long start = Native.getCurrentTime();
                    long elapsed = 0;
                    sleepMan.sleep(current, sleepNanos[i]);
                    elapsed = Native.getCurrentTime() - start;
                    verbose_p("sleep blocked: elapsed-expected = " + 
                       (elapsed-sleepNanos[i]) + " ns\n");
                    check_condition(elapsed >= sleepNanos[i], 
                               "Didn't sleep long enough: " + 
                               (elapsed / TimeConversion.NANOS_PER_MILLI) + 
                               " ms instead of " + sleepTimes[i] +
                               " ms (diff = " + (elapsed-sleepNanos[i]) + "ns)");
                }
                finally {
                    threadMan.setReschedulingEnabled(enabled);
                }
            }
        }
        catch (ClassCastException ex) {
            p(" SKIPPED: not working with thread that supports sleep");
        }
    }

    private void sleepAbsolute() {
        setModule("thread manager sleep absolute");
        if (threadMan == null ) {
            p(" SKIPPED: not working with UserLevelThreadManager");
        }

        try {
            for (int i = 0; i < sleepNanos.length; i++ ) {
                boolean enabled = threadMan.setReschedulingEnabled(false);
                try {
                    long start = Native.getCurrentTime();
                    long end = 0;
                    if (sleepMan.sleepAbsolute(current, start+sleepNanos[i])) {
                        end = Native.getCurrentTime();
                        verbose_p("sleepAbsolute blocked: actual-expected = " + 
                           (end-(start+sleepNanos[i])) + " ns\n");
                    }
                    else {
                        end = Native.getCurrentTime();
                        verbose_p("sleepAbsolute no-block: actual-expected = " + 
                           (end-(start+sleepNanos[i])) + " ns\n");
                    } 
                    check_condition(end >= (start+sleepNanos[i]), 
                               "Didn't sleep long enough by: " + 
                               (end - (start+sleepNanos[i])) +
                               " ns");
                }
                finally {
                    threadMan.setReschedulingEnabled(enabled);
                }
            }

        }
        catch (ClassCastException ex) {
            p(" SKIPPED: not working with thread that supports sleep");
        }

    }
    
}

