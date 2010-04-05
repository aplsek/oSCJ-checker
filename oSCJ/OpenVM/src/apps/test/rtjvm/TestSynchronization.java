package test.rtjvm;

import javax.realtime.*;
import test.jvm.TestBase;
/**
 * Additional synchronization tests for RTSJ - using the HighResolutionTime
 * waitForObject method.
 *
 * @author David Holmes
 */
public class TestSynchronization extends TestBase {

    public TestSynchronization(Harness domain) {
        super("HighResolutionTime.waitForObject tests", domain);
    }


    public void run() {
        interruptOnWaitTest();
	if (!System.getProperty("org.ovmj.timedWaitRunsFast",
				"false").equals("true"))
	    timedWaitTest();
        interruptDuringWaitTest();
    }



    void interruptOnWaitTest() {
        setModule("interrupt on wait tests");
        Thread cur = Thread.currentThread();
        check_condition(!Thread.interrupted(), "Already interrupted!!!");
        check_condition(!cur.isInterrupted(), "Already interrupted!!!");

        cur.interrupt();
        check_condition(cur.isInterrupted(), "interrupt failed");
        check_condition(Thread.interrupted(), "interrupt failed");
        check_condition(!cur.isInterrupted(), "interrupted() didn't clear flag");
        cur.interrupt();
        check_condition(cur.isInterrupted(), "interrupt failed");
        synchronized (this) {
            try {
                HighResolutionTime.waitForObject(this, null);
                check_condition(false, "No interrupted exception on null time");
            }
            catch(InterruptedException ex) {
                check_condition(!cur.isInterrupted(), "interrupt flag set");
            }
        }
        cur.interrupt();
        check_condition(cur.isInterrupted(), "interrupt failed");
        synchronized (this) {
            try {
                HighResolutionTime.waitForObject(this, new RelativeTime(1000,0));
                check_condition(false, "No interrupted exception on rel time");
            }
            catch(InterruptedException ex) {
                check_condition(!cur.isInterrupted(), "interrupt flag set");
            }
        }

        cur.interrupt();
        check_condition(cur.isInterrupted(), "interrupt failed");
        synchronized (this) {
            try {
                HighResolutionTime.waitForObject(this, Clock.getRealtimeClock().getTime());
                check_condition(false, "No interrupted exception on abs time");
            }
            catch(InterruptedException ex) {
                check_condition(!cur.isInterrupted(), "interrupt flag set");
            }
        }

    }        

    void timedWaitTest() {
        setModule("timed wait tests");
        long[] times = { 1, 3, 5, 9, 10, 50 };

        long start, finish;
        try {
            for (int i = 0; i < times.length; i++) {
                synchronized(this) {
                    start = System.nanoTime();
                    HighResolutionTime.waitForObject(this, new RelativeTime(times[i], 0));
                    finish = System.nanoTime();
                    check_condition( (finish-start) >= (times[i]*1000*1000),
                                     "rel wait time too short: " + (finish-start) +
                                     " vs. " + times[i]*1000*1000);
                }
            }
        }
        catch(InterruptedException ex) {
            check_condition(false, "Interrupted!");
        }


        // absolute wait in past
        try {
            synchronized(this) {
                HighResolutionTime.waitForObject(this, 
                                                 Clock.getRealtimeClock().getTime());
                // can't really verify anything :(
            }
        }
        catch(InterruptedException ex) {
            check_condition(false, "Interrupted!");
        }

        // absolute wait in near future
        try {
            synchronized(this) {
                HighResolutionTime.waitForObject(this, 
                                                 Clock.getRealtimeClock().getTime().add(500,0));
                // can't really verify anything :(
            }
        }
        catch(InterruptedException ex) {
            check_condition(false, "Interrupted!");
        }

    }


    void interruptDuringWaitTest() {
        setModule("interrupt during wait");

        final Thread cur = Thread.currentThread();
        check_condition(!Thread.interrupted(), "Already interrupted!!!");

        synchronized(this) {
            (new Thread() {
                public void run() {
                    // can't run till main thread wait()s
                    synchronized(TestSynchronization.this) {
                    }
                    cur.interrupt();
                }
                }).start();

            try {
                HighResolutionTime.waitForObject(this, new RelativeTime(1000,0));
                check_condition(false, "No interrupted exception");
            }
            catch(InterruptedException ex) {
                check_condition(!cur.isInterrupted(), "interrupt flag set");
            }
        }


        synchronized(this) {
            (new Thread() {
                public void run() {
                    // can't run till main thread wait()s
                    synchronized(TestSynchronization.this) {
                    }
                    cur.interrupt();
                }
                }).start();

            try {
                HighResolutionTime.waitForObject(this, 
                                                 Clock.getRealtimeClock().getTime().add(1000,0));
                check_condition(false, "No interrupted exception");
            }
            catch(InterruptedException ex) {
                check_condition(!cur.isInterrupted(), "interrupt flag set");
            }
        }

    }        
        
}
