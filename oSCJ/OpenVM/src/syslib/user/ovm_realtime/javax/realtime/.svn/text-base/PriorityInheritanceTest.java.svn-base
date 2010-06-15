package javax.realtime;

/**
 * A test program for the priority-inheritance protocol implementation.
 * This is basically a program that uses the javax.realtime classes but
 * with some assertions that require access to package-restricted
 * implementation details from the ED. 
 *
 * <p>The tests are carefully structured so that we can determine a priori 
 * the event sequence (ie which thread will run when) and verify that sequence
 * at runtime. This allows us to test the priority inheritance is at least 
 * giving the correct relative priorities on any system. However, only the 
 * system specific assertions can tell us that the priority inheritance 
 * implementation is actually doing exactly what it should.
 *
 * @author David Holmes
 *
 */
public class PriorityInheritanceTest {

    /**
     * Helper class for maintaining expected event sequences
     */
    static class Event {
        public final String action;
        public final RealtimeThread expected;
        public RealtimeThread actual;
        public Event(RealtimeThread t, String s) {
            expected = t; action = s;
        }
        public void print() {
            System.out.println(action + " (performed by: " + actual + ")");
        }
        public void check() {
            Assert.check(actual == expected ? Assert.OK :
                         "Expected thread: " + expected.getName() + 
                         " found " + actual.getName() + ":: " + action);
        }
    }

    /**
     * Helper class for maintaining expected event sequences
     */
    static class EventSet {
        final Event[] events;
        int index = 0;
        public EventSet(Event[] events) {
            this.events = events;
        }
        // you can turn off print for quieter tests
        public void process(RealtimeThread t) {
            Event e = events[index++];
            e.actual = t;
            e.check();
            e.print();
        }
    }


    /**
     * Helper class that allows us to give a Runnable to a thread which can 
     * then be set of refer to the 'real' Runnable. This is necessary if the
     * Runnable wants to refer to the thread object (circular initialisation).
     */
    static class RunProxy implements Runnable {
        public Runnable r = null;
        public void run() { r.run(); }
    }

    public static void main(String[] args) {
        test1();
        test2();
        test3();
        test4();
        test5();
        test6();
        test7();
    }

    // these functions make it easier to assert that the state of each thread
    // is what we expect

    /**
     * Checks that the priority inheritance queue of the given thread is in the
     * state expected.
     *
     * @param thread the thread to check
     * @param size the expected size of the queue
     * @param head the expected thread at the head of the queue
     * @param tail the expected thread at the tail of the queue
     */
    private static void checkPIQ(RealtimeThread thread, int size, 
                                 RealtimeThread head, RealtimeThread tail) {
        int actualSize = LibraryImports.getInheritanceQueueSize(thread);
        Assert.check( actualSize == size ? Assert.OK :
                      thread + ": inheritance queue has size " 
                      + actualSize + " expected " + size);
        if (actualSize > 0) {
            Assert.check(LibraryImports.checkInheritanceQueueHead(thread, head)?
                         Assert.OK : thread + " doesn't have head " + head); 
            Assert.check(LibraryImports.checkInheritanceQueueTail(thread, tail)?
                         Assert.OK : thread + " doesn't have tail " + tail); 
        }
    }

    /**
     * Checks that the actual base and active priorities of the given thread
     * matches the expected base and active priorities.
     *
     * @param thread the thread to check
     * @param expectedBase the expected base priroity
     * @param expectedActive the expected active priority
     */
    private static void checkPriority(RealtimeThread thread, 
                                      int expectedBase, 
                                      int expectedActive) {
        int actualBase = LibraryImports.getBasePriority(thread);
        int actualActive = LibraryImports.getActivePriority(thread);
        Assert.check(actualBase == expectedBase ? Assert.OK : 
                     thread + ": expected base priority " + 
                     expectedBase + ", actual " + actualBase);
        Assert.check(actualActive == expectedActive ? Assert.OK : 
                     thread + ": expected active priority " 
                     + expectedActive + ", actual " + actualActive);
    }

    /**
     * A utility function to wait for a set of threads to terminate.
     * We need this as we don't have join() under OVM at present.
     */
    static void join(RealtimeThread[] threads) {
        for(int i = 0; i < threads.length; i++) {
            try { 
                threads[i].join();
            } catch(InterruptedException ex){}
        }
    }

    /**
     * 
     * This test involves three threads (L, M and H) with low, medium
     * and high priorities respectively; and two objects M1 and M2 that
     * will be locked by those threads. The basic scenario is:
     * <ol>
     * <li>L starts and acquires M1
     * <li>M starts and blocks trying to acquire M1
     * <li>L acquires M2
     * <li>H starts and blocks trying to acquire M2
     * <li>L releases M2
     * <li>H acquires then rleases M2 and terminates
     * <li>L releases M1
     * <li>M acquires and releases M1 then terminates
     * <li>L terminates
     * </ol>
     * <p>At each step we check the priority of the active thread  and 
     * each thread's inheritance queue.
     * <p>This demonstrates a thread acquiring priority inheritance entries
     * due to two different monitors.
     */
    private static void test1() {

        System.out.println("Test 1:");

        final Object M1 = new Object();
        final Object M2 = new Object();

        final PriorityParameters low = 
            new PriorityParameters(PriorityScheduler.instance().getMinPriority());

        final PriorityParameters med = 
            new PriorityParameters(PriorityScheduler.instance().getNormPriority());

        final PriorityParameters hi = 
            new PriorityParameters(PriorityScheduler.instance().getMaxPriority());

        RunProxy hp = new RunProxy();
        final RealtimeThread H = new RealtimeThread(hi,
                               null, // release
                               null, // memory
                               null, // memory area
                               null, // group
                               hp);
        H.setName("Thread-H");

        RunProxy mp = new RunProxy();
        final RealtimeThread M = new RealtimeThread(med,
                               null, // release
                               null, // memory
                               null, // memory area
                               null, // group
                               mp);
        M.setName("Thread-M");

        RunProxy lp = new RunProxy();
        final RealtimeThread L = new RealtimeThread(low,
                               null, // release
                               null, // memory
                               null, // memory area
                               null, // group
                               lp);
        L.setName("Thread-L");

        // this is the sequence of events we expect

        final EventSet events = new EventSet(new Event[] {
            new Event(L, "L Acquiring lock of M1"),
            new Event(L, "L Acquired M1, starting thread M"),
            new Event(M, "M started, trying to acquire M1"),
            new Event(L, "L continuing, acquiring M2"),
            new Event(L, "L acquired M2, starting thread H"),
            new Event(H, "H started, acquiring M2"),
            new Event(L, "L continuing, releasing M2"),
            new Event(H, "H acquired M2, about to release"),
            new Event(H, "H released M2, terminating"),
            new Event(L, "L continuing, releasing M1"),
            new Event(M, "M acquired M1, about to release"),
            new Event(M, "M release M1, terminating"),
            new Event(L, "L terminating"),
        });

        lp.r = new Runnable() {
                public void run() {
                    RealtimeThread me = RealtimeThread.currentRealtimeThread();
                    Assert.check(me == L);

                    events.process(me);
                    checkPriority(me, low.getPriority(), low.getPriority());
                    checkPIQ(me, 0, null, null);

                    synchronized(M1) {
                        events.process(me);
                        checkPriority(me, low.getPriority(), low.getPriority());
                        checkPIQ(me, 0, null, null);

                        M.start();
                        events.process(me);
                        checkPIQ(me, 1, M, M);
                        checkPriority(me, low.getPriority(), med.getPriority());
                        checkPIQ(M, 0,null, null);
                        checkPriority(M, med.getPriority(), med.getPriority());

                        synchronized(M2) {
                            events.process(me);
                            checkPIQ(me, 1, M, M);
                            checkPriority(me, low.getPriority(), med.getPriority());

                            H.start();
                            events.process(me);
                            checkPIQ(me, 2, H, M);
                            checkPriority(me, low.getPriority(), hi.getPriority());
                            checkPIQ(M, 0,null, null);
                            checkPriority(M, med.getPriority(), med.getPriority());
                            checkPIQ(H, 0,null, null);
                            checkPriority(H, hi.getPriority(), hi.getPriority());

                        } // release M2
                        events.process(me);
                        checkPIQ(me, 1, M, M);
                        checkPriority(me, low.getPriority(), med.getPriority());
                        checkPIQ(M, 0,null, null);
                        checkPriority(M, med.getPriority(), med.getPriority());
                    } // release M1
                    events.process(me);
                    checkPriority(me, low.getPriority(), low.getPriority());
                    checkPIQ(me, 0, null, null);
                }
            };

        mp.r = new Runnable() {
                public void run() {
                    RealtimeThread me = RealtimeThread.currentRealtimeThread();
                    Assert.check(me == M);
                    events.process(me);
                    checkPriority(me, med.getPriority(), med.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(L, 0,null, null);
                    checkPriority(L, low.getPriority(), low.getPriority());

                    synchronized(M1) {
                        events.process(me);
                        checkPriority(me, med.getPriority(), med.getPriority());
                        checkPIQ(me, 0, null, null);
                        checkPIQ(L, 0,null, null);
                        checkPriority(L, low.getPriority(), low.getPriority());
                    }
                    events.process(me);
                    checkPriority(me, med.getPriority(), med.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(L, 0,null, null);
                    checkPriority(L, low.getPriority(), low.getPriority());
                }
            };

        hp.r = new Runnable() {
                public void run() {
                    RealtimeThread me = RealtimeThread.currentRealtimeThread();
                    Assert.check(me == H);
                    events.process(me);
                    checkPriority(me, hi.getPriority(), hi.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(L, 1,M, M);
                    checkPriority(L, low.getPriority(), med.getPriority());
                    checkPIQ(M, 0,null, null);
                    checkPriority(M, med.getPriority(), med.getPriority());

                    synchronized(M2) {
                        events.process(me);
                        checkPriority(me, hi.getPriority(), hi.getPriority());
                        checkPIQ(me, 0, null, null);
                        checkPIQ(L, 1,M, M);
                        checkPriority(L, low.getPriority(), med.getPriority());
                        checkPIQ(M, 0,null, null);
                        checkPriority(M, med.getPriority(), med.getPriority());

                    }
                    events.process(me);
                    checkPriority(me, hi.getPriority(), hi.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(L, 1,M, M);
                    checkPriority(L, low.getPriority(), med.getPriority());
                    checkPIQ(M, 0,null, null);
                    checkPriority(M, med.getPriority(), med.getPriority());
                }
            };

        // get things started
        L.start();

        join(new RealtimeThread[] { L, M, H});
    }


    /**
     * This test involves three threads (L, M and H) with low, medium
     * and high priorities respectively; and two objects M1 and M2 that
     * will be locked by those threads. The basic scenario is:
     * <ol>
     * <li>L starts and acquires M1
     * <li>M starts and blocks trying to acquire M1
     * <li>L acquires M2
     * <li>H starts and blocks trying to acquire M1
     * <li>L releases M2
     * <li>L releases M1
     * <li>H acquires and releases M1 then terminates
     * <li>M acquires and releases M1 then terminates
     * <li>L terminates
     * </ol>
     * <p>At each step we check the priority of the active thread  and 
     * each thread's inheritance queue.
     * <p>This test demonstrates the replacement of a priority
     * inheritance queue thread when a higher priority thread blocks
     * on the same monitor.
     */
    private static void test2() {

        System.out.println("Test 2:");

        final Object M1 = new Object();
        final Object M2 = new Object();

        final PriorityParameters low = 
            new PriorityParameters(PriorityScheduler.instance().getMinPriority());

        final PriorityParameters med = 
            new PriorityParameters(PriorityScheduler.instance().getNormPriority());

        final PriorityParameters hi = 
            new PriorityParameters(PriorityScheduler.instance().getMaxPriority());

        RunProxy hp = new RunProxy();
        final RealtimeThread H = new RealtimeThread(hi,
                               null, // release
                               null, // memory
                               null, // memory area
                               null, // group
                               hp);
        H.setName("Thread-H");

        RunProxy mp = new RunProxy();
        final RealtimeThread M = new RealtimeThread(med,
                               null, // release
                               null, // memory
                               null, // memory area
                               null, // group
                               mp);
        M.setName("Thread-M");

        RunProxy lp = new RunProxy();
        final RealtimeThread L = new RealtimeThread(low,
                               null, // release
                               null, // memory
                               null, // memory area
                               null, // group
                               lp);
        L.setName("Thread-L");

        // this is the sequence of events we expect

        final EventSet events = new EventSet(new Event[] {
            new Event(L, "L Acquiring lock of M1"),
            new Event(L, "L Acquired M1, starting thread M"),
            new Event(M, "M started, trying to acquire M1"),
            new Event(L, "L continuing, acquiring M2"),
            new Event(L, "L acquired M2, starting thread H"),
            new Event(H, "H started, acquiring M1"),
            new Event(L, "L continuing, releasing M2"),
            new Event(L, "L continuing, releasing M1"),
            new Event(H, "H acquired M1, about to release"),
            new Event(H, "H released M2, terminating"),
            new Event(M, "M acquired M1, about to release"),
            new Event(M, "M release M1, terminating"),
            new Event(L, "L terminating"),
        });

        lp.r = new Runnable() {
                public void run() {
                    RealtimeThread me = RealtimeThread.currentRealtimeThread();
                    Assert.check(me == L);

                    events.process(me);
                    checkPriority(me, low.getPriority(), low.getPriority());
                    checkPIQ(me, 0, null, null);

                    synchronized(M1) {
                        events.process(me);
                        checkPriority(me, low.getPriority(), low.getPriority());
                        checkPIQ(me, 0, null, null);

                        M.start();
                        events.process(me);
                        checkPIQ(me, 1, M, M);
                        checkPriority(me, low.getPriority(), med.getPriority());

                        checkPIQ(M, 0,null, null);
                        checkPriority(M, med.getPriority(), med.getPriority());


                        synchronized(M2) {
                            events.process(me);
                            checkPIQ(me, 1, M, M);
                            checkPriority(me, low.getPriority(), med.getPriority());
                            checkPIQ(M, 0,null, null);
                            checkPriority(M, med.getPriority(), med.getPriority());

                            H.start();
                            events.process(me);
                            checkPIQ(me, 1, H, H);
                            checkPriority(me, low.getPriority(), hi.getPriority());
                            checkPIQ(M, 0,null, null);
                            checkPriority(M, med.getPriority(), med.getPriority());
                            checkPIQ(H, 0,null, null);
                            checkPriority(H, hi.getPriority(), hi.getPriority());
                        } // release M2
                        events.process(me);
                        checkPIQ(me, 1, H, H);
                        checkPriority(me, low.getPriority(), hi.getPriority());
                        checkPIQ(M, 0,null, null);
                        checkPriority(M, med.getPriority(), med.getPriority());
                        checkPIQ(H, 0,null, null);
                        checkPriority(H, hi.getPriority(), hi.getPriority());

                    } // release M1
                    events.process(me);
                    checkPriority(me, low.getPriority(), low.getPriority());
                    checkPIQ(me, 0, null, null);
                }
            };

        mp.r = new Runnable() {
                public void run() {
                    RealtimeThread me = RealtimeThread.currentRealtimeThread();
                    Assert.check(me == M);
                    events.process(me);
                    checkPriority(me, med.getPriority(), med.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPriority(L, low.getPriority(), low.getPriority());
                    checkPIQ(L, 0, null, null);

                    synchronized(M1) {
                        events.process(me);
                        checkPriority(me, med.getPriority(), med.getPriority());
                        checkPIQ(me, 0, null, null);
                        checkPriority(L, low.getPriority(), low.getPriority());
                        checkPIQ(L, 0, null, null);
                    }
                    events.process(me);
                    checkPriority(me, med.getPriority(), med.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPriority(L, low.getPriority(), low.getPriority());
                    checkPIQ(L, 0, null, null);

                }
            };

        hp.r = new Runnable() {
                public void run() {
                    RealtimeThread me = RealtimeThread.currentRealtimeThread();
                    Assert.check(me == H);
                    events.process(me);
                    checkPriority(me, hi.getPriority(), hi.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPriority(L, low.getPriority(), med.getPriority());
                    checkPIQ(L, 1, M, M);
                    checkPriority(M, med.getPriority(), med.getPriority());
                    checkPIQ(M, 0, null, null);

                    synchronized(M1) {
                        events.process(me);
                        checkPriority(me, hi.getPriority(), hi.getPriority());
                        checkPIQ(me, 0, null, null);
                        checkPriority(L, low.getPriority(), low.getPriority());
                        checkPIQ(L, 0, null, null);
                        checkPriority(M, med.getPriority(), med.getPriority());
                        checkPIQ(M, 0, null, null);
                    }
                    events.process(me);
                    checkPriority(me, hi.getPriority(), hi.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPriority(L, low.getPriority(), low.getPriority());
                    checkPIQ(L, 0, null, null);
                    checkPriority(M, med.getPriority(), med.getPriority());
                    checkPIQ(M, 0, null, null);
                }
            };

        // get things started
        L.start();

        join(new RealtimeThread[] { L, M, H});
    }


    /**
     * This test is similar to {@link #test1} but introduces a change to the
     * base priority of the thread that is subject to priority inheritance.
     * This tests that the threads active priority is restored correctly when
     * each monitor is released.
     * <p>This test involves three threads (L, M and H) with low, medium
     * and high priorities respectively; and two objects M1 and M2 that
     * will be locked by those threads. The basic scenario is:
     * <ol>
     * <li>L starts and acquires M1
     * <li>M starts and blocks trying to acquire M1
     * <li>L acquires M2
     * <li>H starts and blocks trying to acquire M2
     * <li>L increases it's priority to be greater than H
     * <li>L releases M2
     * <li>L releases M1
     * <li>L sleeps to allow H and M to run to completion (should really join())
     * <li>H acquires then rleases M2 and terminates
     * <li>M acquires and releases M1 then terminates
     * <li>L terminates
     * </ol>
     * <p>At each step we check the priority of the active thread  and 
     * each thread's inheritance queue.
     */
    private static void test3() {

        System.out.println("Test 3:");

        final Object M1 = new Object();
        final Object M2 = new Object();

        final PriorityParameters low = 
            new PriorityParameters(PriorityScheduler.instance().getMinPriority()+1);

        final PriorityParameters med = 
            new PriorityParameters(PriorityScheduler.instance().getNormPriority());

        final PriorityParameters hi = 
            new PriorityParameters(PriorityScheduler.instance().getMaxPriority()-1);

        final PriorityParameters ultraHi = new PriorityParameters(hi.getPriority()+1); // > H
        final PriorityParameters ultraLow = new PriorityParameters(low.getPriority()-1); // < L

        RunProxy hp = new RunProxy();
        final RealtimeThread H = new RealtimeThread(hi,
                               null, // release
                               null, // memory
                               null, // memory area
                               null, // group
                               hp);
        H.setName("Thread-H");

        RunProxy mp = new RunProxy();
        final RealtimeThread M = new RealtimeThread(med,
                               null, // release
                               null, // memory
                               null, // memory area
                               null, // group
                               mp);
        M.setName("Thread-M");

        RunProxy lp = new RunProxy();
        final RealtimeThread L = new RealtimeThread(
            low,
            null, // release
            null, // memory
            null, // memory area
            null, // group
            lp);
        L.setName("Thread-L");

        // L can't change it's own priority due to the runtime restrictions on priority changes.
        // So we define a separate thread to do it, which has lower priority than L and
        // so will only run after L goes to sleep - during which time L's priority can be
        // changed.
        RunProxy cp = new RunProxy();
        final RealtimeThread changer = new RealtimeThread(
            ultraLow, // sched params
            null, // release
            null, // memory
            null, // memory area
            null, // group
            cp);
        changer.setName("Thread-PriorityChanger");

        // this is the sequence of events we expect

        final EventSet events = new EventSet(new Event[] {
            new Event(L, "L Acquiring lock of M1"),
            new Event(L, "L Acquired M1, starting thread M"),
            new Event(M, "M started, trying to acquire M1"),
            new Event(L, "L continuing, acquiring M2"),
            new Event(L, "L acquired M2, starting thread H"),
            new Event(H, "H started, acquiring M2"),
            new Event(L, "L continuing, starting Changer"),
            new Event(L, "L continuing, doing sleep"),
            new Event(changer, "Changer increasing L's priority"),
            new Event(L, "L continuing, releasing M2"),
            new Event(L, "L continuing, releasing M1"),
            new Event(L, "L about to sleep"),
            new Event(H, "H acquired M2, about to release"),
            new Event(H, "H released M2, terminating"),
            new Event(M, "M acquired M1, about to release"),
            new Event(M, "M release M1, terminating"),
            new Event(L, "L terminating"),
        });

        cp.r = new Runnable() {
                public void run() {
                    RealtimeThread me = RealtimeThread.currentRealtimeThread();
                    events.process(me);
                    L.setSchedulingParameters(ultraHi);
                }
            };

        lp.r = new Runnable() {
                public void run() {
                    RealtimeThread me = RealtimeThread.currentRealtimeThread();
                    Assert.check(me == L);

                    events.process(me);
                    checkPriority(me, low.getPriority(), low.getPriority());
                    checkPIQ(me, 0, null, null);

                    synchronized(M1) {
                        events.process(me);
                        checkPriority(me, low.getPriority(), low.getPriority());
                        checkPIQ(me, 0, null, null);

                        M.start();
                        events.process(me);
                        checkPIQ(me, 1, M, M);
                        checkPriority(me, low.getPriority(), med.getPriority());
                        checkPIQ(M, 0,null, null);
                        checkPriority(M, med.getPriority(), med.getPriority());

                        synchronized(M2) {
                            events.process(me);
                            checkPIQ(me, 1, M, M);
                            checkPriority(me, low.getPriority(), med.getPriority());

                            H.start();
                            events.process(me);
                            checkPIQ(me, 2, H, M);
                            checkPriority(me, low.getPriority(), hi.getPriority());
                            checkPIQ(M, 0,null, null);
                            checkPriority(M, med.getPriority(), med.getPriority());
                            checkPIQ(H, 0,null, null);
                            checkPriority(H, hi.getPriority(), hi.getPriority());

                            changer.start();
                            events.process(me);
                            checkPIQ(me, 2, H, M);
                            checkPriority(me, low.getPriority(), hi.getPriority());
                            checkPIQ(M, 0,null, null);
                            checkPriority(M, med.getPriority(), med.getPriority());
                            checkPIQ(H, 0,null, null);
                            checkPriority(H, hi.getPriority(), hi.getPriority());

                            try { RealtimeThread.sleep(1000);}
                            catch(InterruptedException ex) {}
                            events.process(me);
                            checkPIQ(me, 2, H, M);
                            checkPriority(me, ultraHi.getPriority(), ultraHi.getPriority());
                            checkPIQ(M, 0,null, null);
                            checkPriority(M, med.getPriority(), med.getPriority());
                            checkPIQ(H, 0,null, null);
                            checkPriority(H, hi.getPriority(), hi.getPriority());
                        } // release M2
                        events.process(me);
                        checkPIQ(me, 1, M, M);
                        checkPriority(me, ultraHi.getPriority(), ultraHi.getPriority());
                        checkPIQ(H, 0,null, null);
                        checkPriority(H, hi.getPriority(), hi.getPriority());
                        checkPIQ(M, 0,null, null);
                        checkPriority(M, med.getPriority(), med.getPriority());
                    } // release M1
                    events.process(me);
                    checkPIQ(me, 0, null, null);
                    checkPriority(me, ultraHi.getPriority(), ultraHi.getPriority());
                    checkPIQ(H, 0,null, null);
                    checkPriority(H, hi.getPriority(), hi.getPriority());
                    checkPIQ(M, 0,null, null);
                    checkPriority(M, med.getPriority(), med.getPriority());
                    
                    try { RealtimeThread.sleep(1000);}
                    catch(InterruptedException ex) {}
                    events.process(me);
                    checkPIQ(me, 0, null, null);
                    checkPriority(me, ultraHi.getPriority(), ultraHi.getPriority());
                }
            };

        mp.r = new Runnable() {
                public void run() {
                    RealtimeThread me = RealtimeThread.currentRealtimeThread();
                    Assert.check(me == M);
                    events.process(me);
                    checkPriority(me, med.getPriority(), med.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(L, 0,null, null);
                    checkPriority(L, low.getPriority(), low.getPriority());

                    synchronized(M1) {
                        events.process(me);
                        checkPriority(me, med.getPriority(), med.getPriority());
                        checkPIQ(me, 0, null, null);
                        checkPIQ(L, 0, null, null);
                        checkPriority(L, ultraHi.getPriority(), ultraHi.getPriority());
                    }
                    events.process(me);
                    checkPriority(me, med.getPriority(), med.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(L, 0,null, null);
                    checkPriority(L, ultraHi.getPriority(), ultraHi.getPriority());
                }
            };

        hp.r = new Runnable() {
                public void run() {
                    RealtimeThread me = RealtimeThread.currentRealtimeThread();
                    Assert.check(me == H);
                    events.process(me);
                    checkPriority(me, hi.getPriority(), hi.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(L, 1,M, M);
                    checkPriority(L, low.getPriority(), med.getPriority());
                    checkPIQ(M, 0,null, null);
                    checkPriority(M, med.getPriority(), med.getPriority());

                    synchronized(M2) {
                        events.process(me);
                        checkPriority(me, hi.getPriority(), hi.getPriority());
                        checkPIQ(me, 0, null, null);
                        checkPIQ(L, 0, null, null);
                        checkPriority(L, ultraHi.getPriority(), ultraHi.getPriority());
                        checkPIQ(M, 0,null, null);
                        checkPriority(M, med.getPriority(), med.getPriority());

                    }
                    events.process(me);
                    checkPriority(me, hi.getPriority(), hi.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(L, 0, null, null);
                    checkPriority(L, ultraHi.getPriority(), ultraHi.getPriority());
                    checkPIQ(M, 0,null, null);
                    checkPriority(M, med.getPriority(), med.getPriority());
                }
            };

        // get things started
        L.start();

        join(new RealtimeThread[] { L, M, H});
    }

    /**
     * A variation of {@link #test3} in which we change the base priority
     * to be between M and H, rather than greater than H.
     * <p>This test involves three threads (L, M and H) with low, medium
     * and high priorities respectively; and two objects M1 and M2 that
     * will be locked by those threads. The basic scenario is:
     * <ol>
     * <li>L starts and acquires M1
     * <li>M starts and blocks trying to acquire M1
     * <li>L acquires M2
     * <li>H starts and blocks trying to acquire M2
     * <li>L increases it's priority to be greater than M but less than H
     * <li>L releases M2
     * <li>H acquires then rleases M2 and terminates
     * <li>L releases M1
     * <li>L sleeps to allow M to run to completion (should really join())
     * <li>M acquires and releases M1 then terminates
     * <li>L terminates
     * </ol>
     * <p>At each step we check the priority of the active thread  and 
     * each thread's inheritance queue.
     */
    private static void test4() {

        System.out.println("Test 4:");

        final Object M1 = new Object();
        final Object M2 = new Object();

        final PriorityParameters low = 
            new PriorityParameters(PriorityScheduler.instance().getMinPriority()+1);

        final PriorityParameters med = 
            new PriorityParameters(PriorityScheduler.instance().getNormPriority());

        final PriorityParameters hi = 
            new PriorityParameters(PriorityScheduler.instance().getMaxPriority());

        final PriorityParameters ultraMed = new PriorityParameters(med.getPriority()+1); // > M < H
        final PriorityParameters ultraLow = new PriorityParameters(low.getPriority()-1); // < L

        RunProxy hp = new RunProxy();
        final RealtimeThread H = new RealtimeThread(hi,
                               null, // release
                               null, // memory
                               null, // memory area
                               null, // group
                               hp);
        H.setName("Thread-H");

        RunProxy mp = new RunProxy();
        final RealtimeThread M = new RealtimeThread(med,
                               null, // release
                               null, // memory
                               null, // memory area
                               null, // group
                               mp);
        M.setName("Thread-M");

        RunProxy lp = new RunProxy();
        final RealtimeThread L = new RealtimeThread(
            low,
            null, // release
            null, // memory
            null, // memory area
            null, // group
            lp);
        L.setName("Thread-L");

        // L can't change it's own priority due to the runtime restrictions on priority changes.
        // So we define a separate thread to do it, which has lower priority than L and
        // so will only run after L goes to sleep - during which time L's priority can be
        // changed.
        RunProxy cp = new RunProxy();
        final RealtimeThread changer = new RealtimeThread(
            ultraLow, // sched params
            null, // release
            null, // memory
            null, // memory area
            null, // group
            cp);
        changer.setName("Thread-PriorityChanger");

        // this is the sequence of events we expect

        final EventSet events = new EventSet(new Event[] {
            new Event(L, "L Acquiring lock of M1"),
            new Event(L, "L Acquired M1, starting thread M"),
            new Event(M, "M started, trying to acquire M1"),
            new Event(L, "L continuing, acquiring M2"),
            new Event(L, "L acquired M2, starting thread H"),
            new Event(H, "H started, acquiring M2"),
            new Event(L, "L continuing, starting Changer"),
            new Event(L, "L continuing, doing sleep"),
            new Event(changer, "Changer increasing L's priority"),
            new Event(L, "L continuing, releasing M2"),
            new Event(H, "H acquired M2, about to release"),
            new Event(H, "H released M2, terminating"),
            new Event(L, "L continuing, releasing M1"),
            new Event(L, "L about to sleep"),
            new Event(M, "M acquired M1, about to release"),
            new Event(M, "M release M1, terminating"),
            new Event(L, "L terminating"),
        });

        cp.r = new Runnable() {
                public void run() {
                    RealtimeThread me = RealtimeThread.currentRealtimeThread();
                    events.process(me);
                    L.setSchedulingParameters(ultraMed);
                }
            };

        lp.r = new Runnable() {
                public void run() {
                    RealtimeThread me = RealtimeThread.currentRealtimeThread();
                    Assert.check(me == L);

                    events.process(me);
                    checkPriority(me, low.getPriority(), low.getPriority());
                    checkPIQ(me, 0, null, null);

                    synchronized(M1) {
                        events.process(me);
                        checkPriority(me, low.getPriority(), low.getPriority());
                        checkPIQ(me, 0, null, null);

                        M.start();
                        events.process(me);
                        checkPIQ(me, 1, M, M);
                        checkPriority(me, low.getPriority(), med.getPriority());
                        checkPIQ(M, 0,null, null);
                        checkPriority(M, med.getPriority(), med.getPriority());

                        synchronized(M2) {
                            events.process(me);
                            checkPIQ(me, 1, M, M);
                            checkPriority(me, low.getPriority(), med.getPriority());

                            H.start();
                            events.process(me);
                            checkPIQ(me, 2, H, M);
                            checkPriority(me, low.getPriority(), hi.getPriority());
                            checkPIQ(M, 0,null, null);
                            checkPriority(M, med.getPriority(), med.getPriority());
                            checkPIQ(H, 0,null, null);
                            checkPriority(H, hi.getPriority(), hi.getPriority());

                            changer.start();
                            events.process(me);
                            checkPIQ(me, 2, H, M);
                            checkPriority(me, low.getPriority(), hi.getPriority());
                            checkPIQ(M, 0,null, null);
                            checkPriority(M, med.getPriority(), med.getPriority());
                            checkPIQ(H, 0,null, null);
                            checkPriority(H, hi.getPriority(), hi.getPriority());

                            try { RealtimeThread.sleep(1000);}
                            catch(InterruptedException ex) {}
                            events.process(me);
                            checkPIQ(me, 2, H, M);
                            checkPriority(me, ultraMed.getPriority(), hi.getPriority());
                            checkPIQ(M, 0,null, null);
                            checkPriority(M, med.getPriority(), med.getPriority());
                            checkPIQ(H, 0,null, null);
                            checkPriority(H, hi.getPriority(), hi.getPriority());
                        } // release M2
                        events.process(me);
                        checkPIQ(me, 1, M, M);
                        checkPriority(me, ultraMed.getPriority(), ultraMed.getPriority());
                        checkPIQ(M, 0,null, null);
                        checkPriority(M, med.getPriority(), med.getPriority());
                    } // release M1
                    events.process(me);
                    checkPIQ(me, 0, null, null);
                    checkPriority(me, ultraMed.getPriority(), ultraMed.getPriority());
                    checkPIQ(M, 0,null, null);
                    checkPriority(M, med.getPriority(), med.getPriority());
                    
                    try { RealtimeThread.sleep(1000);}
                    catch(InterruptedException ex) {}
                    events.process(me);
                    checkPIQ(me, 0, null, null);
                    checkPriority(me, ultraMed.getPriority(), ultraMed.getPriority());
                }
            };

        mp.r = new Runnable() {
                public void run() {
                    RealtimeThread me = RealtimeThread.currentRealtimeThread();
                    Assert.check(me == M);
                    events.process(me);
                    checkPriority(me, med.getPriority(), med.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(L, 0,null, null);
                    checkPriority(L, low.getPriority(), low.getPriority());

                    synchronized(M1) {
                        events.process(me);
                        checkPriority(me, med.getPriority(), med.getPriority());
                        checkPIQ(me, 0, null, null);
                        checkPIQ(L, 0, null, null);
                        checkPriority(L, ultraMed.getPriority(), ultraMed.getPriority());
                    }
                    events.process(me);
                    checkPriority(me, med.getPriority(), med.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(L, 0,null, null);
                    checkPriority(L, ultraMed.getPriority(), ultraMed.getPriority());
                }
            };

        hp.r = new Runnable() {
                public void run() {
                    RealtimeThread me = RealtimeThread.currentRealtimeThread();
                    Assert.check(me == H);
                    events.process(me);
                    checkPriority(me, hi.getPriority(), hi.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(L, 1,M, M);
                    checkPriority(L, low.getPriority(), med.getPriority());
                    checkPIQ(M, 0,null, null);
                    checkPriority(M, med.getPriority(), med.getPriority());

                    synchronized(M2) {
                        events.process(me);
                        checkPriority(me, hi.getPriority(), hi.getPriority());
                        checkPIQ(me, 0, null, null);
                        checkPIQ(L, 1, M, M);
                        checkPriority(L, ultraMed.getPriority(), ultraMed.getPriority());
                        checkPIQ(M, 0,null, null);
                        checkPriority(M, med.getPriority(), med.getPriority());

                    }
                    events.process(me);
                    checkPriority(me, hi.getPriority(), hi.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(L, 1, M, M);
                    checkPriority(L, ultraMed.getPriority(), ultraMed.getPriority());
                    checkPIQ(M, 0,null, null);
                    checkPriority(M, med.getPriority(), med.getPriority());
                }
            };

        // get things started
        L.start();

        join(new RealtimeThread[] { L, M, H});
    }

    /**
     * This is a variation of {@link #test1} in which the priority of
     * Thread H is reduced to between L and M, once L has both monitors.
     * This reorders the PI queue.
     *
     * <p>This test involves three threads (L, M and H) with low, medium
     * and high priorities respectively; and two objects M1 and M2 that
     * will be locked by those threads. The basic scenario is:
     * <ol>
     * <li>L starts and acquires M1
     * <li>M starts and blocks trying to acquire M1
     * <li>L acquires M2
     * <li>H starts and blocks trying to acquire M2
     * <li>L drops H's priority below that of M
     * <li>L releases M2
     * <li>L releases M1
     * <li>M acquires and releases M1 then terminates
     * <li>H acquires then releases M2 and terminates
     * <li>L terminates
     * </ol>
     * <p>At each step we check the priority of the active thread  and 
     * each thread's inheritance queue.
     */
    private static void test5() {
        System.out.println("Test 5:");

        final Object M1 = new Object();
        final Object M2 = new Object();

        final PriorityParameters low = 
            new PriorityParameters(PriorityScheduler.instance().getMinPriority()+1);

        final PriorityParameters med = 
            new PriorityParameters(PriorityScheduler.instance().getNormPriority());

        final PriorityParameters hi = 
            new PriorityParameters(PriorityScheduler.instance().getMaxPriority());

        final PriorityParameters subMed = new PriorityParameters(med.getPriority()-1); // < M, > L


        RunProxy hp = new RunProxy();
        final RealtimeThread H = new RealtimeThread(hi,
                               null, // release
                               null, // memory
                               null, // memory area
                               null, // group
                               hp);
        H.setName("Thread-H");

        RunProxy mp = new RunProxy();
        final RealtimeThread M = new RealtimeThread(med,
                               null, // release
                               null, // memory
                               null, // memory area
                               null, // group
                               mp);
        M.setName("Thread-M");

        RunProxy lp = new RunProxy();
        final RealtimeThread L = new RealtimeThread(low,
                               null, // release
                               null, // memory
                               null, // memory area
                               null, // group
                               lp);
        L.setName("Thread-L");


        // this is the sequence of events we expect

        final EventSet events = new EventSet(new Event[] {
            new Event(L, "L Acquiring lock of M1"),
            new Event(L, "L Acquired M1, starting thread M"),
            new Event(M, "M started, trying to acquire M1"),
            new Event(L, "L continuing, acquiring M2"),
            new Event(L, "L acquired M2, starting thread H"),
            new Event(H, "H started, acquiring M2"),
            new Event(L, "L continuing, dropping H's priority and hence my own"),
            new Event(L, "L continuing, releasing M2"),
            new Event(L, "L continuing, releasing M1"),
            new Event(M, "M acquired M1, about to release"),
            new Event(M, "M release M1, terminating"),
            new Event(H, "H acquired M2, about to release"),
            new Event(H, "H released M2, terminating"),
            new Event(L, "L terminating"),
        });

        lp.r = new Runnable() {
                public void run() {
                    RealtimeThread me = RealtimeThread.currentRealtimeThread();
                    Assert.check(me == L);

                    events.process(me);
                    checkPriority(me, low.getPriority(), low.getPriority());
                    checkPIQ(me, 0, null, null);

                    synchronized(M1) {
                        events.process(me);
                        checkPriority(me, low.getPriority(), low.getPriority());
                        checkPIQ(me, 0, null, null);

                        M.start();
                        events.process(me);
                        checkPIQ(me, 1, M, M);
                        checkPriority(me, low.getPriority(), med.getPriority());
                        checkPIQ(M, 0,null, null);
                        checkPriority(M, med.getPriority(), med.getPriority());

                        synchronized(M2) {
                            events.process(me);
                            checkPIQ(me, 1, M, M);
                            checkPriority(me, low.getPriority(), med.getPriority());

                            H.start();
                            events.process(me);
                            checkPIQ(me, 2, H, M);
                            checkPriority(me, low.getPriority(), hi.getPriority());
                            checkPIQ(M, 0,null, null);
                            checkPriority(M, med.getPriority(), med.getPriority());
                            checkPIQ(H, 0,null, null);
                            checkPriority(H, hi.getPriority(), hi.getPriority());


                            H.setSchedulingParameters(subMed);
                            events.process(me);
                            checkPIQ(me, 2, M, H);
                            checkPriority(me, low.getPriority(), med.getPriority());
                            checkPIQ(M, 0,null, null);
                            checkPriority(M, med.getPriority(), med.getPriority());
                            checkPIQ(H, 0,null, null);
                            checkPriority(H, subMed.getPriority(), subMed.getPriority());

                        } // release M2
                        events.process(me);
                        checkPIQ(me, 1, M, M);
                        checkPriority(me, low.getPriority(), med.getPriority());
                        checkPIQ(M, 0,null, null);
                        checkPriority(M, med.getPriority(), med.getPriority());
                        checkPIQ(H, 0,null, null);
                        checkPriority(H, subMed.getPriority(), subMed.getPriority());

                    } // release M1
                    events.process(me);
                    checkPriority(me, low.getPriority(), low.getPriority());
                    checkPIQ(me, 0, null, null);
                }
            };

        mp.r = new Runnable() {
                public void run() {
                    RealtimeThread me = RealtimeThread.currentRealtimeThread();
                    Assert.check(me == M);
                    events.process(me);
                    checkPriority(me, med.getPriority(), med.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(L, 0,null, null);
                    checkPriority(L, low.getPriority(), low.getPriority());

                    synchronized(M1) {
                        events.process(me);
                        checkPriority(me, med.getPriority(), med.getPriority());
                        checkPIQ(me, 0, null, null);
                        checkPIQ(L, 0,null, null);
                        checkPriority(L, low.getPriority(), low.getPriority());
                        checkPIQ(H, 0,null, null);
                        checkPriority(H, subMed.getPriority(), subMed.getPriority());

                    }
                    events.process(me);
                    checkPriority(me, med.getPriority(), med.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(L, 0,null, null);
                    checkPriority(L, low.getPriority(), low.getPriority());
                }
            };

        hp.r = new Runnable() {
                public void run() {
                    RealtimeThread me = RealtimeThread.currentRealtimeThread();
                    Assert.check(me == H);
                    events.process(me);
                    checkPriority(me, hi.getPriority(), hi.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(L, 1,M, M);
                    checkPriority(L, low.getPriority(), med.getPriority());
                    checkPIQ(M, 0,null, null);
                    checkPriority(M, med.getPriority(), med.getPriority());

                    synchronized(M2) {
                        events.process(me);
                        checkPriority(me, subMed.getPriority(), subMed.getPriority());
                        checkPIQ(me, 0, null, null);
                        checkPIQ(L, 0, null, null);
                        checkPriority(L, low.getPriority(), low.getPriority());
                    }
                    events.process(me);
                    checkPriority(me, subMed.getPriority(), subMed.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(L, 0, null, null);
                    checkPriority(L, low.getPriority(), low.getPriority());
                }
            };

        // get things started
        L.start();

        join(new RealtimeThread[] { L, M, H});
    }

    /**
     * This is a combination {@link #test2} and {@link #test5} in which the 
     * priority of Thread H is reduced to between L and M, once L has both 
     * monitors.
     * This reorders the monitors entry queue and hence changed the priority
     * inheritance queue..
     *
     * <p>This test involves three threads (L, M and H) with low, medium
     * and high priorities respectively; and two objects M1 and M2 that
     * will be locked by those threads. The basic scenario is:
     * <ol>
     * <li>L starts and acquires M1
     * <li>M starts and blocks trying to acquire M1
     * <li>L acquires M2
     * <li>H starts and blocks trying to acquire M1
     * <li>L drops H's priority below that of M
     * <li>L releases M2
     * <li>L releases M1
     * <li>M acquires and releases M1 then terminates
     * <li>H acquires then releases M1 and terminates
     * <li>L terminates
     * </ol>
     * <p>At each step we check the priority of the active thread  and 
     * each thread's inheritance queue.
     */
    private static void test6() {
        System.out.println("Test 6:");

        final Object M1 = new Object();
        final Object M2 = new Object();

        final PriorityParameters low = 
            new PriorityParameters(PriorityScheduler.instance().getMinPriority()+1);

        final PriorityParameters med = 
            new PriorityParameters(PriorityScheduler.instance().getNormPriority());

        final PriorityParameters hi = 
            new PriorityParameters(PriorityScheduler.instance().getMaxPriority());

        final PriorityParameters subMed = 
            new PriorityParameters(med.getPriority()-1); // < M, > L


        RunProxy hp = new RunProxy();
        final RealtimeThread H = new RealtimeThread(hi,
                               null, // release
                               null, // memory
                               null, // memory area
                               null, // group
                               hp);
        H.setName("Thread-H");

        RunProxy mp = new RunProxy();
        final RealtimeThread M = new RealtimeThread(med,
                               null, // release
                               null, // memory
                               null, // memory area
                               null, // group
                               mp);
        M.setName("Thread-M");

        RunProxy lp = new RunProxy();
        final RealtimeThread L = new RealtimeThread(low,
                               null, // release
                               null, // memory
                               null, // memory area
                               null, // group
                               lp);
        L.setName("Thread-L");


        // this is the sequence of events we expect

        final EventSet events = new EventSet(new Event[] {
            new Event(L, "L Acquiring lock of M1"),
            new Event(L, "L Acquired M1, starting thread M"),
            new Event(M, "M started, trying to acquire M1"),
            new Event(L, "L continuing, acquiring M2"),
            new Event(L, "L acquired M2, starting thread H"),
            new Event(H, "H started, acquiring M1"),
            new Event(L, "L continuing, dropping H's priority and so my own"),
            new Event(L, "L continuing, releasing M2"),
            new Event(L, "L continuing, releasing M1"),
            new Event(M, "M acquired M1, about to release"),
            new Event(M, "M release M1, terminating"),
            new Event(H, "H acquired M1, about to release"),
            new Event(H, "H released M1, terminating"),
            new Event(L, "L terminating"),
        });

        lp.r = new Runnable() {
                public void run() {
                    RealtimeThread me = RealtimeThread.currentRealtimeThread();
                    Assert.check(me == L);

                    events.process(me);
                    checkPriority(me, low.getPriority(), low.getPriority());
                    checkPIQ(me, 0, null, null);

                    synchronized(M1) {
                        events.process(me);
                        checkPriority(me, low.getPriority(), low.getPriority());
                        checkPIQ(me, 0, null, null);

                        M.start();
                        events.process(me);
                        checkPIQ(me, 1, M, M);
                        checkPriority(me, low.getPriority(), med.getPriority());
                        checkPIQ(M, 0,null, null);
                        checkPriority(M, med.getPriority(), med.getPriority());

                        synchronized(M2) {
                            events.process(me);
                            checkPIQ(me, 1, M, M);
                            checkPriority(me, low.getPriority(), med.getPriority());

                            H.start();
                            events.process(me);
                            checkPIQ(me, 1, H, H);
                            checkPriority(me, low.getPriority(), hi.getPriority());
                            checkPIQ(M, 0,null, null);
                            checkPriority(M, med.getPriority(), med.getPriority());
                            checkPIQ(H, 0,null, null);
                            checkPriority(H, hi.getPriority(), hi.getPriority());


                            H.setSchedulingParameters(subMed);
                            events.process(me);
                            checkPIQ(me, 1, M, M);
                            checkPriority(me, low.getPriority(), med.getPriority());
                            checkPIQ(M, 0,null, null);
                            checkPriority(M, med.getPriority(), med.getPriority());
                            checkPIQ(H, 0,null, null);
                            checkPriority(H, subMed.getPriority(), subMed.getPriority());

                        } // release M2
                        events.process(me);
                        checkPIQ(me, 1, M, M);
                        checkPriority(me, low.getPriority(), med.getPriority());
                        checkPIQ(M, 0,null, null);
                        checkPriority(M, med.getPriority(), med.getPriority());
                        checkPIQ(H, 0,null, null);
                        checkPriority(H, subMed.getPriority(), subMed.getPriority());

                    } // release M1
                    events.process(me);
                    checkPriority(me, low.getPriority(), low.getPriority());
                    checkPIQ(me, 0, null, null);
                }
            };

        mp.r = new Runnable() {
                public void run() {
                    RealtimeThread me = RealtimeThread.currentRealtimeThread();
                    Assert.check(me == M);
                    events.process(me);
                    checkPriority(me, med.getPriority(), med.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(L, 0,null, null);
                    checkPriority(L, low.getPriority(), low.getPriority());

                    synchronized(M1) {
                        events.process(me);
                        checkPriority(me, med.getPriority(), med.getPriority());
                        checkPIQ(me, 0, null, null);
                        checkPIQ(L, 0,null, null);
                        checkPriority(L, low.getPriority(), low.getPriority());
                    }
                    events.process(me);
                    checkPriority(me, med.getPriority(), med.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(L, 0,null, null);
                    checkPriority(L, low.getPriority(), low.getPriority());
                    checkPIQ(H, 0,null, null);
                    checkPriority(H, subMed.getPriority(), subMed.getPriority());

                }
            };

        hp.r = new Runnable() {
                public void run() {
                    RealtimeThread me = RealtimeThread.currentRealtimeThread();
                    Assert.check(me == H);
                    events.process(me);
                    checkPriority(me, hi.getPriority(), hi.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(L, 1,M, M);
                    checkPriority(L, low.getPriority(), med.getPriority());
                    checkPIQ(M, 0,null, null);
                    checkPriority(M, med.getPriority(), med.getPriority());

                    synchronized(M1) {
                        events.process(me);
                        checkPriority(me, subMed.getPriority(), subMed.getPriority());
                        checkPIQ(me, 0, null, null);
                        checkPIQ(L, 0, null, null);
                        checkPriority(L, low.getPriority(), low.getPriority());
                    }
                    events.process(me);
                    checkPriority(me, subMed.getPriority(), subMed.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(L, 0, null, null);
                    checkPriority(L, low.getPriority(), low.getPriority());
                }
            };

        // get things started
        L.start();

        join(new RealtimeThread[] { L, M, H});
    }


    /**
     * This test checks that a thread that acquires a monitor when multiple
     * threads were blocked on it, correctly acquires the head waiter as a
     * priority source (initially of lower priority) and that inheritance
     * occurss when the base priority of the acquiring thread is lowered.
     *
     * <p>This test involves three threads (L, M and H) with low, medium
     * and high priorities respectively; and one object M1 that
     * will be locked by those threads. The basic scenario is:
     * <ol>
     * <li>L starts and acquires M1
     * <li>M starts and blocks trying to acquire M1
     * <li>H starts and blocks trying to acquire M1
     * <li>L releases M1
     * <li>H acquires M1
     * <li>H drops its priority below M and L
     * <li>H releases M1
     * <li>M acquires M1 release it then terminates
     * <li>L terminates
     * <li>H terminates
     * </ol>
     * <p>At each step we check the priority of the active thread  and 
     * each thread's inheritance queue.
     */
    private static void test7() {
        System.out.println("Test 7:");

        final Object M1 = new Object();

        final PriorityParameters low = 
            new PriorityParameters(PriorityScheduler.instance().getMinPriority()+1);

        final PriorityParameters med = 
            new PriorityParameters(PriorityScheduler.instance().getNormPriority());

        final PriorityParameters hi = 
            new PriorityParameters(PriorityScheduler.instance().getMaxPriority());

        final PriorityParameters ulow = 
            new PriorityParameters(low.getPriority()-1); // < M & L


        RunProxy hp = new RunProxy();
        final RealtimeThread H = new RealtimeThread(hi,
                               null, // release
                               null, // memory
                               null, // memory area
                               null, // group
                               hp);
        H.setName("Thread-H");

        RunProxy mp = new RunProxy();
        final RealtimeThread M = new RealtimeThread(med,
                               null, // release
                               null, // memory
                               null, // memory area
                               null, // group
                               mp);
        M.setName("Thread-M");

        RunProxy lp = new RunProxy();
        final RealtimeThread L = new RealtimeThread(low,
                               null, // release
                               null, // memory
                               null, // memory area
                               null, // group
                               lp);
        L.setName("Thread-L");


        // this is the sequence of events we expect

        final EventSet events = new EventSet(new Event[] {
            new Event(L, "L Acquiring lock of M1"),
            new Event(L, "L Acquired M1, starting thread M"),
            new Event(M, "M started, trying to acquire M1"),
            new Event(L, "L starting thread H"),
            new Event(H, "H started, trying to acquire M1"),
            new Event(L, "L continuing, releasing M1"),
            new Event(H, "H acquired M1, about to drop priority"),
            new Event(H, "H dropped prio now releasing M1"),
            new Event(M, "M acquired M1 and releasing"),
            new Event(M, "M terminating"),
            new Event(L, "L terminating"),
            new Event(H, "H terminating"),
        });

        lp.r = new Runnable() {
                public void run() {
                    RealtimeThread me = RealtimeThread.currentRealtimeThread();
                    Assert.check(me == L);

                    events.process(me);
                    checkPriority(me, low.getPriority(), low.getPriority());
                    checkPIQ(me, 0, null, null);

                    synchronized(M1) {
                        events.process(me);
                        checkPriority(me, low.getPriority(), low.getPriority());
                        checkPIQ(me, 0, null, null);

                        M.start();

                        events.process(me);
                        checkPIQ(me, 1, M, M);
                        checkPriority(me, low.getPriority(), med.getPriority());
                        checkPIQ(M, 0,null, null);
                        checkPriority(M, med.getPriority(), med.getPriority());

                        H.start();

                        events.process(me);
                        checkPIQ(me, 1, H, H);
                        checkPriority(me, low.getPriority(), hi.getPriority());
                        checkPIQ(H, 0,null, null);
                        checkPriority(H, hi.getPriority(), hi.getPriority());
                        checkPIQ(M, 0,null, null);
                        checkPriority(M, med.getPriority(), med.getPriority());
                    } // release M1
                    events.process(me);
                    checkPriority(me, low.getPriority(), low.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(H, 0,null, null);
                    checkPriority(H, ulow.getPriority(), ulow.getPriority());
                }
            };


        mp.r = new Runnable() {
                public void run() {
                    RealtimeThread me = RealtimeThread.currentRealtimeThread();
                    Assert.check(me == M);
                    events.process(me);
                    checkPriority(me, med.getPriority(), med.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(L, 0, null, null);
                    checkPriority(L, low.getPriority(), low.getPriority());

                    synchronized(M1) {
                        events.process(me);
                        checkPriority(me, med.getPriority(), med.getPriority());
                        checkPIQ(me, 0, null, null);
                        checkPIQ(L, 0,null, null);
                        checkPriority(L, low.getPriority(), low.getPriority());
                        checkPIQ(H, 0,null, null);
                        checkPriority(H, ulow.getPriority(), ulow.getPriority());

                    }
                    events.process(me);
                    checkPriority(me, med.getPriority(), med.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(L, 0,null, null);
                    checkPriority(L, low.getPriority(), low.getPriority());
                    checkPIQ(H, 0,null, null);
                    checkPriority(H, ulow.getPriority(), ulow.getPriority());
                }
            };


        hp.r = new Runnable() {
                public void run() {
                    RealtimeThread me = RealtimeThread.currentRealtimeThread();
                    Assert.check(me == H);
                    events.process(me);
                    checkPriority(me, hi.getPriority(), hi.getPriority());
                    checkPIQ(me, 0, null, null);
                    checkPIQ(L, 1, M, M);
                    checkPriority(L, low.getPriority(), med.getPriority());
                    checkPIQ(M, 0, null, null);
                    checkPriority(M, med.getPriority(), med.getPriority());

                    synchronized(M1) {
                        events.process(me);
                        checkPriority(me, hi.getPriority(), hi.getPriority());
                        checkPIQ(me, 1, M, M);
                        checkPIQ(L, 0, null, null);
                        checkPriority(L, low.getPriority(), low.getPriority());
                        checkPIQ(M, 0, null, null);
                        checkPriority(M, med.getPriority(), med.getPriority());
                        
                        me.setSchedulingParameters(ulow);

                        events.process(me);
                        checkPriority(me, ulow.getPriority(), med.getPriority());
                        checkPIQ(me, 1, M, M);
                        checkPIQ(L, 0, null, null);
                        checkPriority(L, low.getPriority(), low.getPriority());
                        checkPIQ(M, 0, null, null);
                        checkPriority(M, med.getPriority(), med.getPriority());

                    }
                    events.process(me);
                    checkPriority(me, ulow.getPriority(), ulow.getPriority());
                    checkPIQ(me, 0, null, null);
                }
            };

        // get things started
        L.start();

        join(new RealtimeThread[] { L, M, H});
    }


}
