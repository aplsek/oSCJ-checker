
package test.jvm;

/**
 * Base class for testcases.
 * @author Christian Grothoff
 * @author Jacques THOMAS
 **/

public abstract class TestBase {
  
    final String description_;
    public final Harness h_;
    final String domain_;
    String module_;

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
	h_.print(s);
    }

    // verbose print
    public void verbose_p(String s) {
        if (verbose) h_.print(s);
    }

    // println - so why call it d? :(
    public void d(String s){
        p(s);
        p("\n");
    }

    public boolean check_condition(boolean condition, String message) {
        if (condition == false)
            fail(message);
        return condition;
    }

    public boolean  check_condition(boolean condition) {
        return check_condition(condition, "Assertion failed.");
    }    


    public void fail(Throwable t) {
        p("Error: Unexpected exception in ");
        d(description_);
        p("Module: ");
        if (module_ != null) {
            d(module_);
        }
        else {
            d("<not set>");
        }
        // this emulates printStackTrace but doesn't make any assumptions 
        // about where the info gets written
        d(t.toString());
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            p("\t");
            d(stack[i].toString());
        }
	h_.fail();
    }

    public void fail(String message) {
        p("Fatal error in ");	
        d(description_);
        p("Module: ");
        if (module_ != null) {
            d(module_);
        }
        else {
            d("<not set>");
        }
        p("Message: ");
        d(message);
	h_.fail();
        // throw new Error();
    }

    public final int runTest() {
        init();
        p("#<");
	p(domain_);
	p("> ==> Running   Test ");
        d(description_);
	try {
	    run();
	} catch (Throwable t) {
	    fail("abnormal return from test: "+t);
	    t.printStackTrace();
	}
        p("#<");
	p(domain_);
	p("> <== Completed Test ");
        d(description_);
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
