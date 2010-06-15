package test.common;

import ovm.core.Executive;
import ovm.core.execution.Native;
import ovm.util.NoSuchElementException;

/**
 * A collection of tests.  This does not implement Test.  It also does not inherit
 * from TestSuite.  The reason is simple: we have no use for nested test suites.
 * @author Filip Pizlo
 * @author Christian Grothoff
 */
public abstract class TestSuite extends test.TestSuite {
    public static final long DISABLE_ALLOCATION = bit();
    public static final long DISABLE_GC = bit();
    public static final long DISABLE_CLONE = bit();
    public static final long DISABLE_CONSTANT_POOL = bit();

    /**
     * If this is set, we skip not only TestExceptions, but all test cases that
     * require exceptions.
     */
    public static final long DISABLE_EXCEPTIONS = bit();

    /**
     * Disable the StackOverflowError test in TestExceptions
     */
    public static final long DISABLE_STACK_OVERFLOW = bit();
    public static final long DISABLE_FIELD_ACCESS = bit();
    public static final long DISABLE_METHODS = bit();
    public static final long DISABLE_SHIFTS = bit();
    public static final long DISABLE_STATIC_FIELD_ACCESS = bit();
    public static final long DISABLE_IFCFIELDS = bit();
    public static final long DISABLE_STRING = bit();
    public static final long DISABLE_IEEE754 = bit();

    /**
     * Disable all multithreaded tests.
     * <p>
     * No multithreaded tests are defined in this directory, and really, none
     * ever can be, but the notion of threading is common.
     */
    public static final long DISABLE_THREADS = bit();
    /**
     * Specialized test suites should assign each test a bit starting here.
     * @see #bit(Class).
     */
    private static final long NEXT_BIT = bit();
    
    public TestSuite() {
	defaultDisabledTests = DISABLE_GC;
    }

    final static private int ITERATIONS = 1;

    /**
     * The set of tests we enable by default (independent of any
     * command-line options) may depend on the vm-generation time
     * data.  Therefore, we initialize this field in the constructor
     * (which runs at vm-generation time).
     **/
    protected long defaultDisabledTests;

    /**
     * returns a bit mask of tests that should not be run
     */
    public long disabledTests() {
        return defaultDisabledTests;
    }

    /**
     * Invoke this method to run all test cases that are not disabled, and exit
     * with nonzero status if any fail
     */
    public void run(Harness h) {
        run(h,disabledTests());
        if (!h.allGood()) {
            Native.print("Some tests failed.\n");
            Executive.shutdown(1);
        }
    }

    protected void condRun( long cond, long disable, Harness h, Test t) {
        if ( 0 == (disable & cond) )
            h.run( t);
        else {
            h.printPrefix();
            Native.print( " Skipping  Test ");
            Native.print( t.getName());
            Native.print( "\n");
        }
    }
    /**
     * run test cases according to the bit mask provided
     * @param disable a mask of all tests that should NOT be run
     */
    public void run(Harness h,long disable) {
//	h.printPrefix();
//        Native.print( "Disabled test mask is: ");
//        Native.print_hex_long( disable);
//        Native.print( "\n");
        // So that this class gets in the boot image
	h.run(new TestAbstractInterpretation(disable));

        for (int i = 0; i < ITERATIONS; i++) {
            condRun(DISABLE_ALLOCATION, disable,
                    h, new TestAllocation(disable));
            condRun(DISABLE_CLONE, disable,
                    h, new TestClone());
            condRun(DISABLE_CONSTANT_POOL, disable,
                    h, new TestConstantPool());
            condRun(DISABLE_EXCEPTIONS, disable,
                    h, new TestExceptions(disable));
            condRun(DISABLE_FIELD_ACCESS, disable,
                    h, new TestFieldAccess());
            condRun(DISABLE_METHODS, disable,
                    h, new TestMethods());
            condRun(DISABLE_SHIFTS, disable,
                    h, new TestShifts());
            condRun(DISABLE_STATIC_FIELD_ACCESS, disable,
                    h, new TestStaticFieldAccess());
            condRun(DISABLE_STRING, disable,
                    h, new TestString());
            condRun(DISABLE_IFCFIELDS, disable,
                    h, new TestIfcFieldResolution());
            condRun(DISABLE_IEEE754, disable,
                    h, new TestIEEE754());
        }
    }

    private static boolean nextMaskInited = false;
    private static long nextMask;

    private static long bit() {
        if (!nextMaskInited) {
            nextMask=1L;
            nextMaskInited=true;
        }
        long b = nextMask;
        if ( 0 == b )
            throw new NoSuchElementException( "Next test mask bit");
        nextMask = b << 1;
        return b;
    }
    
    private static Class client = null;
    private static long clientMask;
    /**
     * Specialized suites should use this method to assign their mask bits,
     * each passing its own Class object. On the first call with an unfamiliar
     * Class object, the bit returned will be NEXT_BIT.  No call with a
     * different Class object will be able to proceed until this method is
     * called with <code>null</code>.  As long as no initializer exists that
     * passes <code>null</code> in its first call of this method, this should
     * be ok.
     *<p />
     * The value returned by a <code>null</code> is the value that would have
     * been returned in the non-<code>null</code> case, but it may be zero if
     * in the non-<code>null</code> case a <code>NoSuchElementException</code>
     * would have been thrown.  This works well with the
     * <code>DISABLE_ALL</code> convention the specialized classes use.
     **/
    protected static long bit( Class c) {
        if ( null == c ) {
            synchronized ( TestSuite.class ) {
                client = null;
                TestSuite.class.notify();
            }
            return clientMask;
        }
        if ( client != c ) {
            synchronized ( TestSuite.class ) {
                while ( null != client ) {
                    try {
                        TestSuite.class.wait();
                    } catch (InterruptedException e) { }
                }
                client = c;
                clientMask = NEXT_BIT;
            }
        }
        long b = clientMask;
        if ( 0 == b )
            throw new NoSuchElementException( "Next test mask bit for "+c);
        clientMask = b << 1;
        return b;
    }
} // end of TestSuite
