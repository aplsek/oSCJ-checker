/*
 * Common superclass for all unit tests
 */
package test;

/**
 * Base class for testcases.
 * @author Christian Grothoff
 * @author Jacques THOMAS
 **/

public abstract class TestBase {
    /**Value of the constant string indicating success.
     **/
    public static final String SUCCESS = "PASS";

    public final String description_;
    public final Harness h_;
    public final String domain_;
    public String module_;

    public boolean verbose = false;

    public static abstract class Harness {
	protected int failures = 0;
	public void fail() { failures++; }

	public abstract void   print(String s);
	public abstract String getDomain();
	public abstract void   exitOnFailure();
    }

    public TestBase(String description, Harness h) {
        this.description_ = description;
	this.h_ = h;
	this.domain_ = h.getDomain();
    }

    public void setModule(String m) {
        this.module_ = m;
    }

    // print
    public void p(String s){
        Object o = s;

        // force checkcast to catch Strings from wrong domains
        String string = (String)o;
	h_.print(s);
    }

    // verbose print
    public void vp(String s) {
        if (verbose) p(s);
    }

    public void d(String s){
        p(s);
        p("   \n"); // yes the spaces are a workaround for constant pool bugs
    }

    public void COREassert(boolean condition, String message) {
        if (condition == false)
            COREfail(message);
    }

    public void COREassert(boolean condition) {
        COREassert(condition, "Assertion failed.");
    }    

    /**
     * Fail if the value of the message is not equal to value of the String
     * <code>SUCCESS</code>.
     *
     * The motivation for this methood is to have a way to avoid
     * constructing the debuging String that is syntactically cheap and
     * readable.  Usage: <p><code>OVMassert(condition ? "PASS" :
     * message);</code><p> In the above <code>message</code> can be an
     * expression which constructs string by concatenation (or performs any
     * other computationally intensive task. It will only be run if the
     * condition evals to true. And yes, this has to be a verbatim "PASS",
     * not some string assembled out of thin air since the "PASS" is a lame
     * substitute for a language construct. If you really want to assemble
     * it out of thin air, intern it because pointer equality will be used
     * to determine if the message is a "PASS" or not.
     **/
    public void COREassert(String message) {
        if (SUCCESS != message) {
            COREfail(message);
        }
    }

    public void COREfail(String message) {
        p("Fatal error in ");	
        d(description_);
        if (module_ != null) {
            p("Module: ");
            d(module_);
        }
        d(message);
	h_.fail();
        // throw new Error();
    }

    public final int runTest() {
        init();
        p("#<");
	p(domain_);
	p("> [Testing ");
        p(description_);
	try {
	    run();
	} catch (Throwable t) {
	    COREfail("abnormal return from test: "+t);
	    t.printStackTrace();
	}
        p("]\n");
	return h_.failures;
    }

    /** The actual test logic to be implemented by a subclass */
    public abstract void run();

    /** Pre-test initialization operation that might be needed by some tests */
    protected void init() {}

    private static long nextMask = 1L;

    protected static long bit() {
        long b = nextMask;
        if ( 0 == b )
            throw new Error( "Next test mask bit");
        nextMask = b << 1;
        return b;
    }   
}