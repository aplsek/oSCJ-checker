package test.rtjvm;

import javax.realtime.*; 

/**
 * A test suite for RTSJ features.  This code expects to run in a
 * realtime JVM configuration
 */
public class TestSuite extends test.jvm.TestSuite {
    static String usage
	= ("<vm-executable> "
	   // Class.forName can't safely be called from static
	   // this static initializer.  It gets confused by inlining,
	   // decides that the calling class is an anonymous class
	   // inside JavaVirtualMachine, and attempts to load
	   // TestSuite from the bootstrap loader.
	   + (true ? "test.rtjvm.TestSuite" : TestSuite.class.getName())
	   + " OPTION*, where OPTION is one of:\n");
    public static final long KNOWN_FAILURES = bit();
    static {
	// The first column of the output message is the same length
	// as everything up to `"' on the second source line.  Hence,
	// an 80 column source line produces a 75 column usage line
	usage += ("  -known-failures    "
		  + "run tests that are expected to fail\n");
    }
    public static final long NO_GC = bit();
    static {
	usage += ("  -no-gc             "
		  + "disable tests that trigger garbage collection\n");
    }

    long flags;



    public TestSuite() {
	this(0L);
    }
    public TestSuite(long flags) {
	this("User-domain tests for RTSJ", flags);
    }
    public TestSuite(String name) { 
        this(name, 0L); 
    }

    public TestSuite(String name, long flags) {
	this(name, 
             new Harness() {
                 public void print(String s) {
                     System.out.print(s);
                 }
                 public String getDomain() {
                     return "user-domain-RT-JVM";
                 }
                 public void exitOnFailure() {
                     if (failures > 0) {
                         System.exit(failures);
                     }
                 }
             },
             flags);
    }

    public TestSuite(String name, Harness domain, long flags) {
	super(name, domain);
        this.flags = flags;
    }


    // need to set this from executInArea to avoid reflective construction
    // into immortal. Can't be a local variable because it would need to be
    // final, but we are trying to set it.
    TestNoHeapThread nht;
    TestNoHeapDynLoad nhl;

    public void run() {
	super.run();

	final boolean workingScopes 
	    = System.getProperty("org.ovmj.supportsScopeChecks",
				 "false").equals("true");
	if (System.getProperty("org.ovmj.supportsGC", "false").equals("true"))
	    System.gc();
	if (workingScopes) {
	    Thread t = new RealtimeThread() {
		    public void run() {
			new TestMemoryAreas(h_, flags).runTest();
		    }
		};
	    t.start();
	    while (true) {
		try {
		    t.join();
		    break;
		} catch (InterruptedException _) { }
	    }
	}

        new TestSynchronization(h_).runTest();


        // need to allocate the NHRT test object in immortal, but we don't
        // run in immortal
	ImmortalMemory.instance().enter( new Runnable() {
		public void run() {
		    if (workingScopes)
			nht = new TestNoHeapThread(h_);
		    nhl = new TestNoHeapDynLoad(h_);
		}
	    });

	if (workingScopes)
	    nht.runTest();
	// No dynamic loading tests without dynamic loading!
	if (!System.getProperty("org.ovmj.staticBuild", "false").equals("true")
	    // FIXME: the combination of MostlyCopyingSplitRegions and
	    // transactions runs out of memory in dynamic loading tests.
	    && !System.getProperty("org.ovmj.supportsTransactions",
				   "false").equals("true"))
	    nhl.runTest();
    }

    static void usage(String opt) {
	System.err.println("invalid argument: " + opt);
	System.err.print(usage);
	System.exit(1);
    }

    static TestSuite ts;  // needs to be static so we can access from Runnable
    
    public static void main(String[] args) {
	long flags = 0;
	for (int i = 0; i < args.length; i++) {
	    String arg = args[i].intern();
	    if (arg == "-known-failures")
		flags |= KNOWN_FAILURES;
	    else if (arg == "-no-gc")
		flags |= NO_GC;
	    else
		usage(arg);
	}
        // need to allocate the test suite and harness etc in immortal so
        // that a failure in a NoHeap thread can access them.
        final long fflags = flags;
        ImmortalMemory.instance().enter( new Runnable() {
                public void run() {
                    ts = new TestSuite(fflags);
                }
            });

	ts.run();
	ts.h_.exitOnFailure();
    }
}
