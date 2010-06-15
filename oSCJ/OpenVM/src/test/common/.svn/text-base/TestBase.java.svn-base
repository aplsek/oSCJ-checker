package test.common;

import ovm.core.execution.Native;

/**
 * Base class for testcases.
 * @author Christian Grothoff
 * @author Jacques THOMAS
 **/

public abstract class TestBase implements Test {
 
    final String description_;
    public Harness.FailureCallback fc_;
    String module_;
    boolean verbose = false;

    public TestBase(String description) {  description_ = description; }   
    public String getName()             {  return description_;  }
    public void setModule(String m)     {  module_ = m; }
    /** Print string argument. */
    public void p(String string)        {  Native.print(string);  }
    /** Print string argument if verbose mode is on.  */
    public void verbose_p(String s)     {  if (verbose) p(s);  }
    /** Print string argument if verbose mode is on.  */
    public void d(String s)             {  p(s);p("   \n"); } 
    // yes the spaces are a workaround for constant pool bugs

    /**
     * Check the condition, if false will print the message. Note this does not
     * throw an exception. The message is only printed if the method is called
     * when there is class in the call stack that has "TestBase" in its name.
     * [why? I am not sure.-jv]
     */
    public void check_condition(boolean condition, String message) {
        if (condition == false) {
            COREfail("Assertion failed: " + message);
            StackTraceElement[] stack = new Throwable().getStackTrace();

            // find the method that called COREassert, taking into account
            // the different forms of COREassert call this one
            int failIndex = Integer.MAX_VALUE;
            boolean found = false;
            for (int i = 0; i < stack.length; i++) {
                // this isn't an exact test but it is simpler than worrying
                // about the format of classname or methodName, and will only
                // fail from a test class with TestBase in its name, and from
                // a method with COREassert in its name (eg. TestOfTestBase
                // and testCOREassert :-) )
                if (stack[i].getClassName().indexOf("TestBase") >= 0 &&
                    stack[i].getMethodName().indexOf("check_condition") >= 0) {
                    found = true;
                }
                else if (found) {
                    failIndex = i;
                    break;
                }
            }

            for (int i = failIndex; i < stack.length; i++) {
                p("\t");
                d(stack[i].toString());
            }
        }
    }

    /** @see Êtest.common.TestBase#check_condition(boolean, string) */
    public void check_condition(boolean condition) { check_condition(condition, "No text given"); }
    public void COREfail(String message)           {  fc_.fail(description_,module_,message); }

     /** Check condition, if false will print the error message. */
    public void check_err(boolean ok, String context) {
        if (ok) return;
        byte[] buf = new byte[128];
        int len = Native.get_specific_error_string(LibraryImports.getErrno(), buf, buf.length);
        COREfail("Error in " + context + ": " + new String(buf, 0, len));
    }
    
    protected boolean needsIsolation() { return false; }

    /** NEVER call this directly.  Always pass Test objects into a Harness to
     * run them. */
    public final void runTest(Harness.FailureCallback cback,
                              boolean providesIsolation) {
        if (needsIsolation() && !providesIsolation) {
            d("Skipping test because it needs isolation.");
            return;
        }
        fc_=cback;      
        init();
        run();
    }

    /** The actual test logic to be implemented by a subclass */
    public abstract void run();
    /** Pre-test initialization operation that might be needed by some tests */
    protected void init() {}
}
