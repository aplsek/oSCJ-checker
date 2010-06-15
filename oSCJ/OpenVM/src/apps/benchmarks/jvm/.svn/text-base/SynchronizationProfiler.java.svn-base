package benchmarks.jvm;

/* 

   This class defines a number of micro benchmarks for measuring
   synchronisation overhead. There are a number of axis to investigate:

   - cost of single level versus nested synchronisation
   - cost of static vs. special vs. virtual calls
   - presence of threads within the wait-set

  The tests will be defined for each of the three call types as well as for
  directly nested synchronisation statements.

  The number of threads in the wait-set can be set using command
  line parameters. Additionally the number of iterations of the test can also be set.
*/

public class SynchronizationProfiler {

    static long waitingThreads = 0;   // number of thread in wait-set
    static long iterations = 10; // number of iterations over which time is measured

    static String usage = "usage: SynchronizationProfiler <test no> <waiting threads> <iterations>";

    public static int getNumberOfTests() { return 27; } // Keep up to date

    public static void main(int testNo, int waiters, int iters) {
        if (waiters >= 0) waitingThreads = waiters;
        else throw new IllegalArgumentException();
        if (iters > 0) iterations = iters;
        else throw new IllegalArgumentException();
        runTest(testNo);
    }

    // can't use this form until String is fixed so we can do the parsing etc
    public static void main (String[] args){
        int testNo = 0;

        try {
            testNo = Integer.parseInt(args[0]);
            if (args.length > 1) {
                int waiters = Integer.parseInt(args[1]);
                if (waiters >= 0) waitingThreads = waiters;
                else throw new IllegalArgumentException();
            }
            if (args.length > 2) {
                int iters = Integer.parseInt(args[2]);
                if (iters > 0) iterations = iters;
                else throw new IllegalArgumentException();
            }
        }
        catch(Throwable t){
            System.out.println(t);
            System.out.println(usage);
            return;
        }
        runTest(testNo);

        if (waitingThreads > 0)
            System.exit(0);
    }

    static void runTest(int testNo) {
        switch(testNo){
            case 1: new SynchronizationProfiler().test1(SynchronizationProfiler.iterations); break;
            case 2: new SynchronizationProfiler().test2(SynchronizationProfiler.iterations); break;
            case 3: new SynchronizationProfiler().test3(SynchronizationProfiler.iterations); break;

            case 4: new SynchronizationProfiler().test4(SynchronizationProfiler.iterations); break;
            case 5: new SynchronizationProfiler().test5(SynchronizationProfiler.iterations); break;
            case 6: new SynchronizationProfiler().test6(SynchronizationProfiler.iterations); break;
			 
            case 7: new SynchronizationProfiler().test7(SynchronizationProfiler.iterations); break;
            case 8: new SynchronizationProfiler().test8(SynchronizationProfiler.iterations); break;
            case 9: new SynchronizationProfiler().test9(SynchronizationProfiler.iterations); break;
			 
            case 10: new SynchronizationProfiler().test10(SynchronizationProfiler.iterations); break;
            case 11: new SynchronizationProfiler().test11(SynchronizationProfiler.iterations); break;
            case 12: new SynchronizationProfiler().test12(SynchronizationProfiler.iterations); break;
            case 13: new SynchronizationProfiler().test13(SynchronizationProfiler.iterations); break;
            case 14: new SynchronizationProfiler().test14(SynchronizationProfiler.iterations); break;

            case 15: new SynchronizationProfiler().test15(SynchronizationProfiler.iterations); break;
            case 16: new SynchronizationProfiler().test16(SynchronizationProfiler.iterations); break;
            case 17: new SynchronizationProfiler().test17(SynchronizationProfiler.iterations); break;
            case 18: new SynchronizationProfiler().test18(SynchronizationProfiler.iterations); break;
            case 19: new SynchronizationProfiler().test19(SynchronizationProfiler.iterations); break;

            case 20: new SynchronizationProfiler().test20(SynchronizationProfiler.iterations); break;
            case 21: new SynchronizationProfiler().test21(SynchronizationProfiler.iterations); break;
            case 22: new SynchronizationProfiler().test22(SynchronizationProfiler.iterations); break;
            case 23: new SynchronizationProfiler().test23(SynchronizationProfiler.iterations); break; 
            case 24: new SynchronizationProfiler().test24(SynchronizationProfiler.iterations); break;

                // this latest test was added late
            case 25: new SynchronizationProfiler().test25(SynchronizationProfiler.iterations); break;
            case 26: new SynchronizationProfiler().test26(SynchronizationProfiler.iterations); break;
            case 27: new SynchronizationProfiler().test27(SynchronizationProfiler.iterations); break;

            default: System.out.println(usage);
        }

    }
    // this array holds the info string about each test
    // Note: the ',' is used to separate the invocation mode from the test description
    //        to allow interpretation as a comma-separated-variable (CSV) file
    public final static String[] info = {
        "",  // ignore zeroth
        "InvokeSpecial , no sync",
        "InvokeSpecial , sync method",
        "InvokeSpecial , sync stmt",

        "InvokeVirtual , no sync",
        "InvokeVirtual , sync method",
        "InvokeVirtual , sync stmt",

        "InvokeStatic , no sync",
        "InvokeStatic , sync method",
        "InvokeStatic , sync stmt",

        "InvokeSpecial , no sync -> no sync",
        "InvokeSpecial , sync method -> sync method",
        "InvokeSpecial , sync method -> sync stmt",
        "InvokeSpecial , sync stmt -> sync method",
        "InvokeSpecial , sync stmt -> sync stmt",

        "InvokeVirtual , no sync -> no sync",
        "InvokeVirtual , sync method -> sync method",
        "InvokeVirtual , sync method -> sync stmt",
        "InvokeVirtual , sync stmt -> sync method",
        "InvokeVirtual , sync stmt -> sync stmt",

        "InvokeStatic , no sync -> no sync",
        "InvokeStatic , sync method -> sync method",
        "InvokeStatic , sync method -> sync stmt",
        "InvokeStatic , sync stmt -> sync method",
        "InvokeStatic , sync stmt -> sync stmt",
// these were added later
        "InvokeSpecial , sync method -> no sync",
        "InvokeVirtual , sync method -> no sync",
        "InvokeStatic , sync method -> no sync",
    };

    // print out stats using CSV format
    public static void printStats(String msg ,long start, long finish, long ops){
        String sep = " , ";
        long duration = finish - start;
        double perOp = duration*1000/(double)ops;
        System.out.println(msg + sep + "Calls" + sep + ops + sep + "Threads" + sep + waitingThreads + sep + "Time" + sep + duration + sep + " ms  (" + perOp + "us/iter)" );
    }


    // note that (static)fillWaitSet uses synchronization so we're ensured that
    // we've touched the relevent monitor before the actual test runs. Hence
    // the monitor will have been constructed if needed.

/** SINGLE LEVEL SYNC **/

/* InvokeSpecial */
    public void test1(long ops){
        fillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            doWorkSP(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[1], startTime, finishTime, ops);
        
    }
    public void test2(long ops){
        fillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            doWorkSPSM(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[2], startTime, finishTime, ops);
        
    }
    public void test3(long ops){
        fillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            doWorkSPSS(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[3], startTime, finishTime, ops);
        
    }


/* InvokeVirtual */
    public void test4(long ops){
        fillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            doWorkV(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[4], startTime, finishTime, ops);
        
    }
    public void test5(long ops){
        fillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            doWorkVSM(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[5], startTime, finishTime, ops);
        
    }
    public void test6(long ops){
        fillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            doWorkVSS(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[6], startTime, finishTime, ops);
        
    }


/* InvokeStatic */
    public void test7(long ops){
        staticfillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            doWorkS(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[7], startTime, finishTime, ops);
        
    }
    public void test8(long ops){
        staticfillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            doWorkSSM(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[8], startTime, finishTime, ops);
        
    }
    public void test9(long ops){
        staticfillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            doWorkSSS(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[9], startTime, finishTime, ops);
        
    }


/** TWO LEVEL SYNC **/

/* InvokeSpecial */
    public void test10(long ops){
        fillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            SPdoWorkSP(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[10], startTime, finishTime, ops);
        
    }
    public void test11(long ops){
        fillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            SPSMdoWorkSPSM(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[11], startTime, finishTime, ops);
        
    }
    public void test12(long ops){
        fillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            SPSMdoWorkSPSS(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[12], startTime, finishTime, ops);
        
    }
    public void test13(long ops){
        fillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            SPSSdoWorkSPSM(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[13], startTime, finishTime, ops);
        
    }
    public void test14(long ops){
        fillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            SPSSdoWorkSPSS(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[14], startTime, finishTime, ops);
        
    }

    // added late
    public void test25(long ops){
        fillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            SPSMdoWorkSP(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[25], startTime, finishTime, ops);
        
    }

/* InvokeVirtual */
    public void test15(long ops){
        fillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            VdoWorkV(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[15], startTime, finishTime, ops);
        
    }
    public void test16(long ops){
        fillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            VSMdoWorkVSM(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[16], startTime, finishTime, ops);
        
    }
    public void test17(long ops){
        fillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            VSMdoWorkVSS(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[17], startTime, finishTime, ops);
        
    }
    public void test18(long ops){
        fillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            VSSdoWorkVSM(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[18], startTime, finishTime, ops);
        
    }
    public void test19(long ops){
        fillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            VSSdoWorkVSS(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[19], startTime, finishTime, ops);
        
    }
    // added late
    public void test26(long ops){
        fillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            VSMdoWorkV(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[26], startTime, finishTime, ops);
        
    }


/* InvokeStatic */
    public void test20(long ops){
        staticfillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            SdoWorkS(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[20], startTime, finishTime, ops);
        
    }
    public void test21(long ops){
        staticfillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            SSMdoWorkSSM(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[21], startTime, finishTime, ops);
        
    }
    public void test22(long ops){
        staticfillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            SSMdoWorkSSS(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[22], startTime, finishTime, ops);
        
    }
    public void test23(long ops){
        staticfillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            SSSdoWorkSSM(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[23], startTime, finishTime, ops);
        
    }
    public void test24(long ops){
        staticfillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            SSSdoWorkSSS(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[24], startTime, finishTime, ops);
        
    }

    // added late
    public void test27(long ops){
        staticfillWaitSet(SynchronizationProfiler.waitingThreads);
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < ops; i++)
            SSMdoWorkS(1);
        long finishTime = System.currentTimeMillis();
        printStats(info[27], startTime, finishTime, ops);
        
    }


    /* we use two counters (one static, one instance) to give the methods some real work
       to do so that the compilers don't try to optimise things away.
    */

    static long staticCount = 0;
    private long count = 0;

    /* we want direct access to the class object for static sync statements, not the long-winded
       process hidden behind the simple foo.class syntax
    */
    static final Class SynchronizationProfilerClassObject = SynchronizationProfiler.class;

    /* we don't want the actually called method to be inlined so we try to prevent this by
       placing a loop in the method body *BUT* we always invoke the method with an argument
       of one to cause a single iteration
    */

    /* invokeVirtual versions */

    protected void doWorkV(int iters){
        for (int i = 0; i < iters; i++)
            count++;
    }

    protected synchronized void doWorkVSM(int iters){
        for (int i = 0; i < iters; i++)
            count++;
    }

    protected void doWorkVSS(int iters){
        synchronized(this){ 
            for (int i = 0; i < iters; i++)
                count++; 
        }
    }

 
    /* invokeSpecial (ie NonVirtual) versions */

    private void doWorkSP(int iters){
        for (int i = 0; i < iters; i++)
            count++;
    }

    private synchronized void doWorkSPSM(int iters){
        for (int i = 0; i < iters; i++)
            count++;
    }

    private void doWorkSPSS(int iters){
        synchronized(this){ 
            for (int i = 0; i < iters; i++)
                count++; 
        }
    }


    /* invokeStatic versions */

    static void doWorkS(int iters){
        for (int i = 0; i < iters; i++)
            staticCount++;
    }

    static synchronized void doWorkSSM(int iters){
        for (int i = 0; i < iters; i++)
            staticCount++;
    }

    static void doWorkSSS(int iters){
        synchronized(SynchronizationProfilerClassObject){ 
            for (int i = 0; i < iters; i++)
                staticCount++; 
        }
    }


    // recursive sync versions of methods - again for each invoke*
    // but only five variations of each

    // invokeSpecial versions

    private void SPdoWorkSP(int iters){
        for (int i = 0; i < iters; i++)
            doWorkSP(iters);
    }
    private synchronized void SPSMdoWorkSPSM(int iters){
        for (int i = 0; i < iters; i++)
            doWorkSPSM(iters);
    }
    private synchronized void SPSMdoWorkSP(int iters){
        for (int i = 0; i < iters; i++)
            doWorkSP(iters);
    }
    private synchronized void SPSMdoWorkSPSS(int iters){
        for (int i = 0; i < iters; i++)
            doWorkSPSS(iters);
    }
    private void SPSSdoWorkSPSM(int iters){
        synchronized(this) { 
            for (int i = 0; i < iters; i++)
                doWorkSPSM(iters); 
        }
    }
    private void SPSSdoWorkSPSS(int iters){
        synchronized(this) { 
            for (int i = 0; i < iters; i++)
                doWorkSPSS(iters); 
        }
    }


    // invokeVirtual versions

    protected void VdoWorkV(int iters){
        for (int i = 0; i < iters; i++)
            doWorkV(iters);
    }
    protected synchronized void VSMdoWorkVSM(int iters){
        for (int i = 0; i < iters; i++)
            doWorkVSM(iters);
    }
    protected synchronized void VSMdoWorkV(int iters){
        for (int i = 0; i < iters; i++)
            doWorkV(iters);
    }
    protected synchronized void VSMdoWorkVSS(int iters){
        for (int i = 0; i < iters; i++)
            doWorkVSS(iters);
    }
    protected void VSSdoWorkVSM(int iters){
        synchronized(this) { 
            for (int i = 0; i < iters; i++)
                doWorkVSM(iters); 
        }
    }
    protected void VSSdoWorkVSS(int iters){
        synchronized(this) { 
            for (int i = 0; i < iters; i++)
                doWorkVSS(iters); 
        }
    }


    // invokeStatic versions

    static void SdoWorkS(int iters){
        for (int i = 0; i < iters; i++)
            doWorkS(iters);
    }
    static synchronized void SSMdoWorkSSM(int iters){
        for (int i = 0; i < iters; i++)
            doWorkSSM(iters);
    }
    static synchronized void SSMdoWorkS(int iters){
        for (int i = 0; i < iters; i++)
            doWorkS(iters);
    }
    static synchronized void SSMdoWorkSSS(int iters){
        for (int i = 0; i < iters; i++)
            doWorkSSS(iters);
    }
    static void SSSdoWorkSSM(int iters){
        synchronized(SynchronizationProfilerClassObject) { 
            for (int i = 0; i < iters; i++)
                doWorkSSM(iters); 
        }
    }
    static void SSSdoWorkSSS(int iters){
        synchronized(SynchronizationProfilerClassObject) { 
            for (int i = 0; i < iters; i++)
                doWorkSSS(iters); 
        }
    }

    // utility methods for filling the wait-set with waiting threads

    private long threadCount = 0; // how many threads in wait-set

    public synchronized void fillWaitSet(long threads){

        if (threads <= 0) return;

        final Thread main = Thread.currentThread();

        Runnable r = new Runnable(){
                public void run(){
                    synchronized(SynchronizationProfiler.this){
//	            System.out.println("waiter " + (threadCount+1) + " about to wait");
                        if (++threadCount == SynchronizationProfiler.waitingThreads) {
                            // this is the last waiter going in so signal main thread so
                            // it can get the monitor lock once we release it
                            main.interrupt();
                        }
                        try {
                            SynchronizationProfiler.this.wait();
                        } catch(InterruptedException ex){}
				// NOT REACHED - assuming no spurious wakeups !!!
                        System.out.println("ERROR: waiting thread returned from wait()!");
                    }
                }
            };
        // create all the threads. Note they can't run far because we hold the monitor

        for(long i = 0; i < threads; i++){
            new Thread(r).start();
        }
   
        // wait for all threads to enter wait-set
        try {
            wait();
            System.out.println("ERROR: main thread returned from wait() without interrupt");
            System.exit(-1);
        }
        catch(InterruptedException ex){ 
//	      System.out.println("Main thread woken up");
        }

        if (threadCount != SynchronizationProfiler.waitingThreads){
            System.out.println("ERROR: main thread released before all waiters waiting!");
            System.out.println("Number of waiters: " + threadCount + " Required Waiters: " + SynchronizationProfiler.waitingThreads );
            System.exit(-1);
        }
    }


    /* Now a static variant of the above */

    private static long staticthreadCount = 0; // how many threads in wait-set

    private static class StaticRunnable implements Runnable {
        final Thread main;
        public StaticRunnable(Thread m){ main = m; }
        public void run(){
            synchronized(SynchronizationProfilerClassObject){
//	         System.out.println(Thread.currentThread().getName() + " waiter " + (staticthreadCount+1) + " about to wait");
                if (++staticthreadCount == SynchronizationProfiler.waitingThreads) {
				// this is the last waiter going in so signal main thread so
				// it can get the monitor lock once we release it
                    main.interrupt();
                }
                try {
                    SynchronizationProfilerClassObject.wait();
                } catch(InterruptedException ex){}
                // NOT REACHED - assuming no spurious wakeups!!!
                System.out.println("ERROR: waiting thread returned from wait()!");
            }
        }
    }


    public static synchronized void staticfillWaitSet(long threads){

        if (threads <= 0) return;

        final Thread main = Thread.currentThread();

        Runnable r = new StaticRunnable(main);
 
        // create all the threads. Note they can't run far because we hold the monitor

        for(long i = 0; i < threads; i++){
            new Thread(r).start();
        }
   
        // wait for all threads to enter wait-set
        try {
            SynchronizationProfilerClassObject.wait();
            System.out.println("ERROR: main thread returned from wait() without interrupt");
            System.exit(-1);
        }
        catch(InterruptedException ex){ 
//	      System.out.println("Main thread woken up");
        }

        if (staticthreadCount != SynchronizationProfiler.waitingThreads){
            System.out.println("ERROR: main thread released before all waiters waiting!");
            System.out.println("Number of waiters: " + staticthreadCount + " Required Waiters: " + SynchronizationProfiler.waitingThreads );
            System.exit(-1);
        }
    }


}
     
