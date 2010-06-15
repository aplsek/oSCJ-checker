/**
 * User-domain testsuite.
 **/
package test;

/**
 * Like a JUnit testsuite, this class runs a couple of smaller testsuites.
 * Each component suite extends the TestBase class and implements the 
 * run method. Invoke the 'run' method on the TestSuite to run
 * all the tests. 
 * @author Christian Grothoff
 **/
public class TestSuite extends TestBase {
    public static final long DISABLE_ALLOCATION = bit();
    public static final long DISABLE_CLONE = bit();
    public static final long DISABLE_CONSTANT_POOL = bit();
    public static final long DISABLE_CLASS = bit();
    public static final long DISABLE_FILE_IO = bit();
    public static final long DISABLE_STD_IO = bit();
    public static final long DISABLE_INITIALIZERS =  bit();
    public static final long DISABLE_REFLECTION = bit();
    public static final long DISABLE_STRING_BUFFER = bit();
    public static final long DISABLE_SYNCHRONIZATION = bit();
    public static final long DISABLE_CONVERSION =  bit();
    public static final long DISABLE_STRINGCONVERSION =  bit();

    /**
     * If this is set, we skip not only TestExceptions, but all test
     * cases that require exceptions.
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
    public static final long DISABLE_DNS = bit();
    public static final long DISABLE_PAR = bit();
    public static final long DISABLE_IEEE754 = bit();
    
    /**
     * Disable all multithreaded tests.
     **/
    public static final long DISABLE_THREADS = bit();

    
    public TestSuite(long disabled) {
	this();
	explicitDisable |= disabled;
    }
    public TestSuite() {
	this("User-domain TestSuite (non-JVM but requires some static init)");
    }

    public TestSuite(String name) {
	this(name, new Harness() {
		public void print(String s) {
		    LibraryImports.print(s);
		}
		public String getDomain() {
		    return "user";
		}
		public void exitOnFailure() {
		    if (failures > 0) {
                        print("User-domain TestSuite - Halting the OVM due to test failures\n");
                        LibraryImports.halt(failures);
		    }
		}
	    });
    }

    public TestSuite(String name, Harness domain) {
	super(name, domain);
	if (System.getProperty("test.disableExceptions") != null)
	    explicitDisable |= DISABLE_EXCEPTIONS;
    }

    final static private int ITERATIONS = 1;

    static long explicitDisable = 0;

    /**
     * returns a bit mask of tests that should not be run
     */
    public long disabledTests() {
	return explicitDisable;
    }

    /**
     * Invoke this method to run all test cases that are not disabled,
     * and exit with nonzero status if any fail
     */
    public void run() {
	run(disabledTests());
	h_.exitOnFailure();
    }

    /**
     * run test cases according to the bit mask provided
     * @param disable a mask of all tests that should NOT be run
     **/
    public void run(long disable) {
	for (int i = 0; i < ITERATIONS; i ++) {
	    if ((disable & DISABLE_ALLOCATION) == 0)
		new TestAllocation(h_).runTest();
	    if ((disable & DISABLE_CLONE) == 0)
		new TestClone(h_).runTest();
	    if ((disable & DISABLE_CONSTANT_POOL) == 0)
		new TestConstantPool(h_).runTest();
	    if ((disable & DISABLE_EXCEPTIONS) == 0)
		new TestExceptions(h_, disable).runTest();
	    if ((disable & DISABLE_FIELD_ACCESS) == 0)
		new TestFieldAccess(h_).runTest();
	    if ((disable & DISABLE_METHODS) == 0)
		new TestMethods(h_).runTest();
	    if ((disable & DISABLE_SHIFTS) == 0)
		new TestShifts(h_).runTest();
	    if ((disable & DISABLE_STATIC_FIELD_ACCESS) == 0)
		new TestStaticFieldAccess(h_).runTest();
	    if ((disable & DISABLE_STRING) == 0)
		new TestString(h_).runTest();
            if ((disable & DISABLE_IFCFIELDS) == 0)
                new TestIfcFieldResolution(h_).runTest();
            if ((disable & DISABLE_CLASS) == 0)
		new TestClass(h_).runTest(); 
	    if ((disable & DISABLE_FILE_IO) == 0)
		new TestFileIO(h_, disable).runTest(); 
            if ((disable & DISABLE_STD_IO) == 0)
                new TestStdIO(h_, disable).runTest();
	    if ((disable & DISABLE_INITIALIZERS) == 0)
		new TestInitializers(h_, disable).runTest(); 
	    if ((disable & DISABLE_REFLECTION) == 0)
		new TestReflection(h_).runTest(); 
	    if ((disable & DISABLE_STRING_BUFFER) == 0)
		new TestStringBuffer(h_).runTest(); 
	    if ((disable & DISABLE_SYNCHRONIZATION) == 0)
		new TestSynchronization(h_, disable).runTest(); 
	    if ((disable & DISABLE_CONVERSION) == 0)
		new TestConversion(h_, disable).runTest();

	    if ((disable & DISABLE_STRINGCONVERSION) == 0)
		new TestConversionToString(h_, disable).runTest();

	    if ((disable & DISABLE_PAR) == 0)
		new TestPAR(h_, disable).runTest();

	    if ((disable & DISABLE_IEEE754) == 0)
		new TestIEEE754(h_, disable).runTest();
	}
    }

    public static void main(String[] args) {
	if (args.length > 0 && args[0].equals("-no-stack-overflow")
	    || true
	    )
	    // FIXME: we can't pass arguments into the user-domain
	    // yet.  J2c doesn't support stack overflow exceptions.
	    // We can disable the test unconditionally because the
	    // interpreter and simplejit already run the test in the
	    // executive domain
	    explicitDisable |= DISABLE_STACK_OVERFLOW;
	new TestSuite().run();
    }

} // end of TestSuite
  


