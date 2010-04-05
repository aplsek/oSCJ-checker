package test.runtime;

import ovm.core.Executive;
import ovm.core.services.memory.MemoryManager;
import ovm.util.CommandLine;
import test.common.ClassicHarnessImpl;
import test.common.ForkingHarnessImpl;
import test.common.Harness;
import s3.services.simplejit.bootimage.SimpleJITBootImageCompileObserver;

import s3.services.bootimage.ImageObserver;

/**
 * Core testsuite. Like a JUnit testsuite, this class runs a couple of smaller testsuites.
 * Each component suite extends the TestBase class and implements the run method. Invoke the 
 * run() method on the TestSuite to run all the tests. Some tests will fail if run twice.
 * 
 * @author Christian Grothoff */
public class TestSuite extends test.common.TestSuite {
    private static final Class c = TestSuite.class;
    public static final long DISABLE_ACCESS = bit(c);
    public static final long DISABLE_BASIC_FOR_COMPILER = bit(c);
    public static final long DISABLE_BITFIELD = bit(c);
    public static final long DISABLE_ASYNC_IO = bit(c);
    public static final long DISABLE_DNS = bit(c);
    public static final long DISABLE_TCP_SOCKET = bit(c);
    public static final long DISABLE_BYTE_BUFFER = bit(c);
    public static final long DISABLE_CONVERSION_TO_STRING = bit(c);
    public static final long DISABLE_FINALLY = bit(c);

    /** ConversionToString subtests involving doubles **/
    public static final long DISABLE_DOUBLE_TO_STRING = bit(c);
    public static final long DISABLE_IO = bit(c);
    public static final long DISABLE_FILE_IO = bit(c);
    public static final long DISABLE_STD_IO = bit(c);
    public static final long DISABLE_PARSING = bit(c);
    public static final long DISABLE_PRAGMA = bit(c);
    public static final long DISABLE_RTBENCH = bit(c);
    public static final long DISABLE_REALTIME_THREAD_SCHEDULING = bit(c);
    public static final long DISABLE_REFLECTION = bit(c);
    public static final long DISABLE_SIEVE = bit(c);
    public static final long DISABLE_LINPACK = bit(c);
    public static final long DISABLE_SUBTYPETEST_BYTECODES = bit(c);
    public static final long DISABLE_SUBTYPETEST = bit(c);
    public static final long DISABLE_SYNCHRONIZATION = bit(c);
    public static final long DISABLE_THREAD_CONTEXT_SWITCH = bit(c);
    public static final long DISABLE_THREAD_SCHEDULING = bit(c);
    public static final long DISABLE_SLEEP = bit(c);
    public static final long DISABLE_SIGNAL_MONITOR = bit(c);
    public static final long DISABLE_POLL_CHECK = bit(c);
    public static final long DISABLE_ADT_TOSTRING = bit(c);
    public static final long DISABLE_PRAGMA_ATOMIC = bit(c);
    public static final long DISABLE_ALL = bit(null)-1;
    
    public TestSuite() {
	defaultDisabledTests |= (DISABLE_BASIC_FOR_COMPILER
				 | DISABLE_PARSING
				 | DISABLE_SIEVE
				 | DISABLE_LINPACK
				 | DISABLE_SUBTYPETEST_BYTECODES
				 | DISABLE_THREAD_CONTEXT_SWITCH
				 | DISABLE_RTBENCH);
	if (ImageObserver.the().isJ2c()) {
	    defaultDisabledTests |= (DISABLE_STACK_OVERFLOW
				     | DISABLE_DOUBLE_TO_STRING);

	    defaultDisabledTests &= ~(DISABLE_BASIC_FOR_COMPILER
				      | DISABLE_ASYNC_IO);

	    // Compiling with -ffloat-store makes these tests work in J2c,
	    // but not in the interpreter.  Strange.
	    defaultDisabledTests &= ~(DISABLE_CONSTANT_POOL
				      | DISABLE_STATIC_FIELD_ACCESS);
	} else if (ImageObserver.the()
		   instanceof SimpleJITBootImageCompileObserver) {
	    defaultDisabledTests |= DISABLE_PARSING;
	    defaultDisabledTests &= ~DISABLE_BASIC_FOR_COMPILER;
	}
    }

    private static int testValue;

    protected static void boot_() { testValue++; }

    final static private int ITERATIONS = 1;

    public void run() {
        CommandLine cmd = Executive.getCommandLine();
        if (cmd.consumeOption("noforking")!=null) 
            run(new ClassicHarnessImpl("executive"));
        else 
            run(new ForkingHarnessImpl("executive"));
    }

    public long disabledTests() {
	long disable = super.disabledTests();

        CommandLine cmd = Executive.getCommandLine();
        
	if (MemoryManager.the().supportsGC() && cmd.consumeOption("nogc") == null) 
            disable &= ~DISABLE_GC;
        if (cmd.consumeOption("dobio") != null) 
            disable &= ~DISABLE_ASYNC_IO;
        if (cmd.consumeOption("dort") != null) 
            disable &= ~DISABLE_RTBENCH;
        if (cmd.consumeOption("retest-static") != null) 
            disable &= ~DISABLE_STATIC_FIELD_ACCESS;
        if (testValue != 1) 
            ovm.core.Executive.panic("Static initializer failed, called " + testValue + " times!");
        if ((disable & DISABLE_THREADS) != 0) 
            disable |= threadTests();
        if (cmd.consumeOption("noet") != null)
            disable = DISABLE_ALL;
        if (cmd.consumeOption("gcprof") != null) 
            return ~(DISABLE_ALLOCATION | DISABLE_GC);
	return disable;
    }

    /**
     * @return a mask of all tests that rely on threading
     */
    long threadTests() {
	return (DISABLE_ASYNC_IO
		| DISABLE_REALTIME_THREAD_SCHEDULING
		| DISABLE_SYNCHRONIZATION
		| DISABLE_THREAD_CONTEXT_SWITCH
		| DISABLE_THREAD_SCHEDULING
		| DISABLE_RTBENCH
                | DISABLE_SLEEP
                | DISABLE_SIGNAL_MONITOR
                | DISABLE_PRAGMA_ATOMIC
                );
    }

    /** Invoke this method to run selected testcases.
     **/
    public void run(Harness h,long disable) {

	super.run(h,disable);

	for (int i = 0; i < ITERATIONS; i++) {
            condRun(DISABLE_PRAGMA_ATOMIC, disable, h, new TestPragmaAtomic());
            condRun(DISABLE_ACCESS,        disable, h, new TestAccess());
            condRun(DISABLE_BASIC_FOR_COMPILER, 
                                           disable, h, new TestBasicForCompiler());
            condRun(DISABLE_BITFIELD,      disable, h, new TestBitfield());
            condRun(DISABLE_SLEEP,         disable, h, new TestSleep());
            condRun(DISABLE_POLL_CHECK,    disable, h, new TestPollCheck());
            condRun(DISABLE_BYTE_BUFFER,   disable, h, new TestByteBuffer());
            condRun(DISABLE_CONVERSION_TO_STRING, 
                                           disable, h, new TestConversionToString(disable));
            condRun(DISABLE_IO,            disable, h, new TestIO(disable));
            condRun(DISABLE_FILE_IO,       disable, h, new TestFileIO(disable));
            condRun(DISABLE_STD_IO,        disable, h, new TestStdIO(disable));
	    // Don't include Bundle/RepositoryLoader stuff.  The path
	    // that we use for actually loading code does not use it.
	    // TestParsing has been disaled forever anyway.
//             condRun(DISABLE_PARSING,       disable, h, new TestParsing());
            condRun(DISABLE_PRAGMA,        disable, h, new TestPragma());
            condRun(DISABLE_REALTIME_THREAD_SCHEDULING, 
                                           disable, h, new TestRealtimeThreadScheduling());
            condRun(DISABLE_REFLECTION,    disable, h, new TestReflection());
            condRun(DISABLE_SIEVE,         disable, h, new TestSieve());
            condRun(DISABLE_LINPACK,       disable, h, new TestLinpack());
            condRun(DISABLE_SUBTYPETEST_BYTECODES, 
                                           disable, h, new TestSubtypetestBytecodes());
            condRun(DISABLE_SUBTYPETEST,   disable, h, new TestSubtypetest(disable));
            condRun(DISABLE_SYNCHRONIZATION,disable,h, new TestSynchronization(disable));
            condRun(DISABLE_THREAD_CONTEXT_SWITCH, 
                                           disable, h, new TestThreadContextSwitch());
            condRun(DISABLE_THREAD_SCHEDULING,     
                                           disable, h, new TestThreadScheduling());
            condRun(DISABLE_SIGNAL_MONITOR,disable, h, new TestSignalMonitor());
            condRun(DISABLE_ADT_TOSTRING,  disable, h, new TestADTtoString());
            // keep this till last as it still fails under NPTL threading
            condRun(DISABLE_ASYNC_IO,      disable, h, new TestPipe(disable));
            condRun(DISABLE_ASYNC_IO,      disable, h, new TestSocketpair(disable));
            condRun(DISABLE_ASYNC_IO,      disable, h, new TestTcpSocket(disable));
            condRun(DISABLE_TCP_SOCKET,    disable, h, new TestTcpSocketCreate(disable));
            condRun(DISABLE_DNS,           disable, h, new TestDNS(disable));
            condRun(DISABLE_FINALLY,       disable, h, new TestFinally());
    	}
    }
} 
