package ovm.services.io.async;

/**
 * This is a handle that you get for an asynchronous operation.  Currently,
 * the only thing you can use this handle for is canceling the request.  Note
 * that it is perfectly safe to throw this handle away when you get it.
 *
 * @author Filip Pizlo
 */
public interface AsyncHandle {
    /**
     * This returns <code>true</code> if there is some reasonably small bound
     * on the amount of time that passes between a call to <code>cancel()</code>
     * and when the operation is actually canceled.  If this returns
     * <code>false</code>, there is a good chance that the operation will
     * complete before the cancellation takes place. */
    public boolean canCancelQuickly();

    /**
     * Cancel the operation.  Note that if you call this after the operation
     * already completed, nothing will happen.  (Operations tend to complete in calls
     * to <code>AsyncFinalizer.finish()</code>, but this is not generally true -
     * so it is possible for you to have never received a <code>ready()</code>
     * call, but the operation is already done.)  Likewise, calling this when the
     * operation has already been canceled will result in nothing happening.
     * <p>
     * When the operation is actually canceled, the <code>AsyncCallback</code>'s
     * <code>ready()</code> method will be called with a finalizer that reports
     * the error passed in to this method.
     */
    public void cancel(IOException error);
}

