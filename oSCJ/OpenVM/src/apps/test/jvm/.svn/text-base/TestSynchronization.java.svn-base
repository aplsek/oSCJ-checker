
package test.jvm;

/**
 * Simple uncontended test for instance and static/class-object synchronization
 * at the method and block level, using both direct monitor inspection checks
 * as well as the <tt>Thread.holdsLock</tt> method.
 * A simple contention test is also provided but without the low-level hooks
 * to see inside the monitor there's not much we can assert during the test.
 * Also checks for IllegalMonitorStateException on wait/notify, and basic
 * interrupt checks
 *
 * @author David Holmes
 */
public class TestSynchronization extends TestBase {

    // static tests need the current instance to do checks
    static TestSynchronization current = null;

    static Class thisClass = TestSynchronization.class;


    public TestSynchronization(Harness domain) {
        super("General synchronized block and methods", domain);
        current = this;
    }


    public void run() {
        nocontentiontest_block();
        nocontentiontest_block_with_exception();
        recursive_nocontentiontest_block();
        recursive_nocontentiontest_block_with_exception();
        
        nocontentiontest_method(1);
        nocontentiontest_method_with_exception(1);
        nocontentiontest_method(5);
        nocontentiontest_method_with_exception(5);

        nocontentiontest_static_method(1);
        nocontentiontest_static_method_with_exception(1);
        nocontentiontest_static_method(5);
        nocontentiontest_static_method_with_exception(5);

        simplecontention_block();
        illegalmonitorstatetest();
        interruptOnWaitTest();
	if (!System.getProperty("org.ovmj.timedWaitRunsFast",
				"false").equals("true"))
	    timedWaitTest();
        interruptDuringWaitTest();
    }

    // utility functions for grouping assertions

    protected void checkCurrentIsOwner(Object mon) {
//         COREassert(LibraryImports.currentThreadOwnsMonitor(mon), 
//           "current thread not owner");
        check_condition(Thread.holdsLock(mon),
                   "current thread not owner");
    }

    protected void checkCurrentNotOwner(Object mon) {
//         COREassert(!LibraryImports.currentThreadOwnsMonitor(mon), 
//           "current thread owner");
        check_condition(!Thread.holdsLock(mon),
                    "current thread is owner");
    }

    protected void checkUnowned(Object mon) {
//         COREassert(LibraryImports.isUnownedMonitor(mon), 
//           "monitor is owned");
        check_condition(!Thread.holdsLock(mon),
                    "unownedmonitor is claimed as held");
    }
    

    protected void checkEntryCount(Object mon, int entryCount) {
//         int count = LibraryImports.getEntryCountForMonitor(mon);
//         COREassert(count == entryCount, 
//           "entry count " + count + " != " + entryCount);
    }        

    
    protected void checkIdleMonitor(Object mon) {
        checkUnowned(mon);
        checkEntryCount(mon, 0);
    }

    protected void checkOwnedUncontended(Object mon, int entryCount) {
        checkCurrentIsOwner(mon);
        checkEntryCount(mon, entryCount);
    }


    void nocontentiontest_block() {
        setModule("no contention test - sync block");

        Object lock = new Object();
        synchronized(lock) {
            checkOwnedUncontended(lock, 1);
        }
        checkIdleMonitor(lock);
    }

    void nocontentiontest_block_with_exception() {
        setModule("no contention test - sync block - with exception");

        Object lock = new Object();
        try {
            synchronized (lock) {
                checkOwnedUncontended(lock, 1);
                throw new Error("Oops");
            }
        } catch (Error e) {
            checkIdleMonitor(lock);
        }
    }

    void recursive_nocontentiontest_block() {
        setModule("recursive - no contention test - sync block");

        Object lock = new Object();
        synchronized(lock) {
            checkOwnedUncontended(lock, 1);
            synchronized(lock) {
                checkOwnedUncontended(lock, 2);
                synchronized(lock) {
                    checkOwnedUncontended(lock, 3);
                    synchronized(lock) {
                        checkOwnedUncontended(lock, 4);
                    }
                    checkOwnedUncontended(lock, 3);
                }
                checkOwnedUncontended(lock, 2);
            }
            checkOwnedUncontended(lock, 1);
        }
        checkIdleMonitor(lock);
    }

    void recursive_nocontentiontest_block_with_exception() {
        setModule("recursive - no contention test - sync block - with exception");

        Object lock = new Object();
        try {
            synchronized (lock) {
                checkOwnedUncontended(lock, 1);
                try {
                    synchronized (lock) {
                        checkOwnedUncontended(lock, 2);
                        try {
                            synchronized (lock) {
                                checkOwnedUncontended(lock, 3);
                                try {
                                    synchronized (lock) {
                                        checkOwnedUncontended(lock, 4);
                                        throw new Error("oops");
                                    }
                                } catch (Error e) {
                                    checkOwnedUncontended(lock, 3);
                                    throw e;
                                }
                            }
                        } catch (Error e) {
                            checkOwnedUncontended(lock, 2);
                            throw e;
                        }
                    }
                } catch (Error e) {
                    checkOwnedUncontended(lock, 1);
                    throw e;
                }
            }
        } catch (Error e) {
            checkIdleMonitor(lock);
        }
    }

    synchronized void syncMethod(int depth, int max) {
        checkOwnedUncontended(this, depth);
        if (depth == max) return;
        else { 
            syncMethod(depth+1, max);
            checkOwnedUncontended(this, depth);
        }
    }

    synchronized void syncMethodWithThrow(int depth, int max) {
        checkOwnedUncontended(this, depth);
        if (depth == max) throw new Error("oops");
        else { 
            try {
                syncMethodWithThrow(depth+1, max);
            }
            catch(Error e) {
                checkOwnedUncontended(this, depth);
                throw e;
            }
        }
    }

    // with methods we can use the same structure for recursive and 
    // non-recursive usage

    void nocontentiontest_method(int recursionLevel) {
        setModule("no contention test - sync method - recursion level " + recursionLevel);
        
        syncMethod(1, recursionLevel);
        checkIdleMonitor(this);
    }

    void nocontentiontest_method_with_exception(int recursionLevel) {
        setModule(
            "no contention test - sync method - with exception - recursion level "
                + recursionLevel);
        try {
            syncMethodWithThrow(1, recursionLevel);
        } catch (Error e) {
            checkIdleMonitor(this);
        }
    }


    static synchronized void syncStaticMethod(int depth, int max) {
        current.checkOwnedUncontended(thisClass, depth);
        if (depth == max) return;
        else { 
            TestSynchronization.syncStaticMethod(depth+1, max);
            current.checkOwnedUncontended(thisClass, depth);
        }
    }

    static synchronized void syncStaticMethodWithThrow(int depth, int max) {
        current.checkOwnedUncontended(thisClass, depth);
        if (depth == max) throw new Error("oops");
        else { 
            try {
                TestSynchronization.syncStaticMethodWithThrow(depth+1, max);
            }
            catch(Error e) {
                current.checkOwnedUncontended(thisClass, depth);
                throw e;
            }
        }
    }

    // with methods we can use the same structure for recursive and 
    // non-recursive usage

    void nocontentiontest_static_method(int recursionLevel) {
        setModule(
            "no contention test - static sync method - recursion level "
                + recursionLevel);
        checkIdleMonitor(thisClass);
        TestSynchronization.syncStaticMethod(1, recursionLevel);
        checkIdleMonitor(thisClass);
    }

    void nocontentiontest_static_method_with_exception(int recursionLevel) {
        setModule(
            "no contention test - static sync method - with exception - recursion level "
                + recursionLevel);
        checkIdleMonitor(thisClass);
        try {
            TestSynchronization.syncStaticMethodWithThrow(1, recursionLevel);
        } catch (Error e) {
            checkIdleMonitor(thisClass);
        }
    }

    // this isn't very interesting because there are no hooks to do the
    // necessary introspection
    void simplecontention_block() {
        setModule("simple contention - block");

        final Object lock = new Object();
        Runnable r = new Runnable() {
                Thread me = null;
                public void run() {
                    checkCurrentNotOwner(lock);
                    synchronized(lock) {
                        checkCurrentIsOwner(lock);
                        Thread.yield();  // let main run - nothing will change
                        checkCurrentIsOwner(lock);
                    }
                    checkIdleMonitor(lock);
                }
            };

        Thread thread2 = new Thread(r, "Thread-2");
        thread2.start();
        // main thread keeps running
        synchronized(lock) {
            checkCurrentIsOwner(lock);
            Thread.yield();  // thread-2 will try to lock and block
            checkCurrentIsOwner(lock);
        }
        // no contextswitch on monitor release as equal priority threads
        Thread.yield(); // switch back to thread-2 so it can acquire
        checkCurrentNotOwner(lock);
        Thread.yield(); // switch back to thread-2 so it can release
        checkIdleMonitor(lock);
    }

    void illegalmonitorstatetest() {
        setModule("IllegalMonitorStateException test on wait/notify/notifyAll");
        Object lock = new Object();
        try {
            lock.wait();
            check_condition(false, "No IllegalMonitorStateException on wait()");
        } catch (IllegalMonitorStateException ex) {
        } catch (Throwable t) {
            check_condition(false, "wait() threw wrong exception: " + t);
        }

        try {
            lock.notify();
            check_condition(false, "No IllegalMonitorStateException on notify()");
        } catch (IllegalMonitorStateException ex) {
        } catch (Throwable t) {
            check_condition(false, "notify() threw wrong exception: " + t);
        }

        try {
            lock.notifyAll();
            check_condition(false, "No IllegalMonitorStateException on notifyAll()");
        } catch (IllegalMonitorStateException ex) {
        } catch (Throwable t) {
            check_condition(false, "notifyAll() threw wrong exception: " + t);
        }
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
                wait();
                check_condition(false, "No interrupted exception");
            }
            catch(InterruptedException ex) {
                check_condition(!cur.isInterrupted(), "interrupt flag set");
            }
        }
        cur.interrupt();
        check_condition(cur.isInterrupted(), "interrupt failed");
        synchronized (this) {
            try {
                wait(2000, 0);
                check_condition(false, "No interrupted exception on timed wait");
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
                    wait(times[i]);
                    finish = System.nanoTime();
                    check_condition( (finish-start) >= (times[i]*1000*1000),
                                     "wait time too short: " + (finish-start) +
                                     " vs. " + times[i]*1000*1000);
                }
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
                wait();
                check_condition(false, "No interrupted exception");
            }
            catch(InterruptedException ex) {
                check_condition(!cur.isInterrupted(), "interrupt flag set");
            }
        }
    }        
        
}
