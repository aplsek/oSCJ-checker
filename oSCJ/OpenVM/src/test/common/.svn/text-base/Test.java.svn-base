package test.common;

/**
 *
 * @author Filip Pizlo
 */
public interface Test {
    /**
     * Get the test's name.
     */
    public String getName();
    
    /** This is a method that gets called by <code>Harness</code>.
     * @param cback A callback that provides a <code>fail()</code> method.
     * @param providesIsolation <code>true</code> if the isolation property
     *   (Property I) is satisfied.  Property I only holds if the
     *   <code>Harness</code> is providing the following two features:
     *    <ol>
     *     <li> <code>FailureCallback.fail()</code> terminates the test.  If you do
     *          not know what is meant by 'terminates', then see below. </li>
     *     <li> There is a guarantee that for any tests T and T' such that Property I
     *          holds for T, and T' is run after T, the only side effects of T that can
     *          become visible in T' are ones that do not involve the process's address
     *          space.  So, a side effect that involves corrupting memory would not be
     *          visible, but a side effect that involves corrupting the hard drive may
     *          become visible.  It is taken for granted that if T' runs before
     *          T, it will not see T's side effects. </li>
     *    </ol>
     *  Definition of 'terminates': the code for the test should be terminated
     *  in such a way that no code in the address space of that test will be able
     *  to observe that the termination happened.  The only way to achieve this
     *  is via the <code>_exit()</code> syscall or <code>kill(9)</code>.  Exceptions
     *  will not work!
     */
    public void runTest(Harness.FailureCallback cback,
                        boolean providesIsolation);
}

