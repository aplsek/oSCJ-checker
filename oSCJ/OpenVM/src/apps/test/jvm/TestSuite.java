/**
 * JVM-level Test Suite
 **/
package test.jvm;

/**
 * Test programs that must be run under the user-level JVM. These test
 * some basic aspects of the JVM/OVM implementation, as opposed to TCK style
 * tests that chekc for conformance to specification.
 *
 * @author David Holmes
 **/
public class TestSuite extends TestBase {
    public static final long DISABLE_DNS_LATENCY = bit();

    public TestSuite() {
	this("User-domain JVM TestSuite)");
    }
    public TestSuite(String name) {
	this(name, new Harness() {
		public void print(String s) {
		    System.out.print(s);
		}
		public String getDomain() {
		    return "user-domain-JVM";
		}
		public void exitOnFailure() {
		    if (failures > 0) {
                        System.exit(failures);
		    }
		}
	    });
    }

    public TestSuite(String name, Harness domain) {
	super(name, domain);
    }

    final static private int ITERATIONS = 1;

    public void run() {
	test.TestSuite.main(new String[0]);
	new TestAllocation(h_).runTest();
	for (int i = 0; i < ITERATIONS; i ++) {
	    new TestSynchronization(h_).runTest();
	}
        new TestTLock("tlock", h_).runTest();
	new TestDNS(h_, 0).runTest(); 
        new TestReflection(h_).runTest();
        new TestBinaryIO(h_).runTest();
	if (false)
	    // This makes our TestFinalization failures much easier to
	    // reproduce.  However, it does not make them any easier
	    // to understand.
	    new TestSocketIO(h_).runTest();
	new TestFinalization(h_).runTest();
	if (!System.getProperty("org.ovmj.staticBuild", "false").equals("true"))
	    new TestDynLoad(h_).runTest();
    }
    

    public static void main(String[] args) {
	TestSuite ts = 	new TestSuite();
	ts.run();
	ts.h_.exitOnFailure();
    }
}
