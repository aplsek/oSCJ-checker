// $Header: /p/sss/cvs/OpenVM/src/test/common/Harness.java,v 1.3 2004/04/05 03:25:55 flack Exp $

package test.common;

/**
 *
 * @author Filip Pizlo
 */
public interface Harness {
    /** Run a test.  This may cause all sorts of things to happen.  Anything goes
     * so long as the Test object's runTest() method gets ultimately called. */
    void run(Test test);
    
    /** Returns true if and only if all Tests run so far via the run() method above
     * were able to perform all of their computation without ever calling the fail()
     * method. */
    boolean allGood();
    
    /** Print the prefix used for test messages.
     **/
    void printPrefix();
    
    /** Callback that the <code>Harness</code> passes to <code>Test></code>s when
     * it runs them. */
    public interface FailureCallback {
        
        /** Instruct the Harness that a failure occurred.  There should not be any
         * observable effect on the currently running test (with one exception; see
         * below).  For example, throwing an exception in fail() would be wrong, as
         * this may alter the execution of the current test.
         * <p>
         * There is one exception to the No Observable Effect rule:
         * Something that is outside of the address space may observe the failure.
         * This one exception is here specifically to allow for an implementation
         * that halts the process using <code>_exit()</code> or even
         * <code>kill(9)</code> such that some parent process observes and logs
         * the failure in a 'safe' way.
         *
         * @param description a description of the failure
         * @param module an optional description of the module that failed.  may be
         *               null.
         */
        void fail(String test,
                         String module,
                         String description);
    }
}

