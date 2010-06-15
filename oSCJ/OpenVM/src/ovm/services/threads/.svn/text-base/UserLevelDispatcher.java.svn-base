package ovm.services.threads;

import ovm.core.services.threads.OVMDispatcher;

/**
 * A user-level dispatcher works with a {@link UserLevelThreadManager}.
 * It provides hooks that can be invoked by the thread manager at the
 * appropriate time.
 *
 */
public interface UserLevelDispatcher extends OVMDispatcher {

    /**
     * Invoked by the thread manager after a context switch has
     * occurred - ie. this is the first piece of code that a thread
     * runs after the thread manager context switches to it.
     */
    void afterContextSwitch();

}
