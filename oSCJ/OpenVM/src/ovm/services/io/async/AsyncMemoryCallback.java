
package ovm.services.io.async;

import ovm.core.services.memory.VM_Address;

/**
 * Callback used to manage buffers.  This interface tries to solve two
 * problems:
 * <ul>
 * <li>It is inefficient to pass something like a <code>byte[]</code> here, since this
 * would require user domain buffers to be copied into executive domain
 * <code>byte[]</code>.  Passing in an <code>Oop</code> is silly from a
 * software engineering perspective, since it is preferrable to not require
 * the async IO layer to reflectively mess around with arrays from other
 * domains.  Instead, we would like to pass a <code>VM_Address</code>.  But
 * here we immediately run into big problems: the VM_Address would inevitably
 * point to the innards of some <code>byte[]</code>, causing confusion for
 * the GC.  So, between when that VM_Address is created and when it is used,
 * we would have to prevent the GC from running.  This would be silly, since
 * the operation's completion isn't even guaranteed (see the documentation for
 * <code>AsyncCallback</code>).  So,
 * instead we may be tempted to allocate a buffer from the system directly
 * and use that.  But this would still be silly, since in the cases where the
 * operation proceeds immediately, you would end up allocating and copying
 * a large buffer even though you didn't have to.
 * <li>Some implementations of async ops may need continuous access to the
 * buffer for an unbounded amount of time, while other implementations may
 * only need that access for a bounded and short (read: on the order of
 * microseconds on modern hardware).  If the buffer needs to be accessed for
 * only a very short amount of time, it may be possible to simply halt the GC
 * for that span of time.  This will almost always be the most efficient way
 * of 'pinning' the buffer.  However, in there does not exist a good bound on
 * the amount of time that the buffer will have to remain accessible, it may
 * be necessary to create a bounce buffer that is outside of the GC's reach.
 * Since neither the caller nor the implementation may know <i>a priori</i>
 * in what manner the buffer will be used, an efficient mechanism for handling
 * buffers must allow the decision about whether or not to create a bounce buffer
 * to be postponed as late as possible.</li>
 * </ul>
 * This callback interface solves both problems.  We solve the first by
 * allowing the caller to give us an executive domain object that hides
 * from us the true representation of the buffer (whether it is an Oop,
 * a byte[], or something else), and it allows all references to be
 * to the beginning of objects.  This makes GC happy.  The methods in
 * this object then allow us to delay the retrieval of the
 * <code>VM_Address</code> until it is actually needed.
 * <p>
 * The second problem is solved by passing one boolean flag into the
 * method that computes the <code>VM_Address</code>.  This flag, called
 * <code>mayNeedBufferForAnUnboundedPeriodOfTime</code> simply tells the
 * implementor whether or not the span of time during which the buffer
 * is to be used will be bounded.  If it is bounded (the flag is false),
 * the implementation may be able to either halt the GC or else instruct
 * the GC to pin the buffer.  If it is unbounded (the flag is true) and
 * the GC is unable to pin buffers efficiently for extended periods of time,
 * the implementation can proceed to create a bounce buffer.  In this way,
 * the user of <code>AsyncMemoryCallback</code> does not have to know
 * anything other than some minimal amount of information about the nature of
 * his use of the buffer.
 * <p>
 * It should be noted that this callback also works well for scoped memory
 * implementations.  It is relatively easy for the implementation of
 * <code>AsyncMemoryCallback</code> to check if the buffer is in a scope
 * (or in immortal memory).  If so, the implementation does not have to do
 * anything, because the buffer is effectively already 'pinned'.
 * <p>
 * Because the implementation may have to <em>un</em>pin a pinned buffer, or
 * else de-allocate a bounce buffer, this callback provides a second method
 * that the user calls to indicate that the buffer is no longer being used.
 * If the <code>mayNeedBufferForAnUnboundedPeriodOfTime</code> flag is false,
 * the call to the second method is guaranteed to occur within a short period
 * of time after the call to the first.
 * @author Filip Pizlo
 */
public interface AsyncMemoryCallback {
    /**
     * This method may block on a monitor.  As such, it should
     * <emph>never</emph> be called from an interrupt handler (see
     * next paragraph).  This method should only be called once for
     * each AsyncMemoryCallback object.  Therefore, it is OK for an
     * implementation of this method to set flags that will be picked
     * up by <code>doneBuffer()</code>.  You should always ultimately
     * call <code>doneBuffer()</code>; otherwise you run the risk of
     * leaking memory.  But only call <code>doneBuffer()</code> after
     * you are done using the buffer, since <code>doneBuffer()</code>
     * may invalidate the memory used to store it, mark it
     * for re-use, or else instruct the GC that it is OK to move it.
     * <p>
     * If you cannot guarantee that the call to <code>doneBuffer()</code>
     * will happen within some short (read: on the order of microseconds)
     * period of time after the call to <code>getBuffer()</code>, you
     * must pass <code>true</code> for the
     * <code>mayNeedBufferForAnUnboundedPeriodOfTime</code> parameter.
     * It is always safe to pass <code>true</code> for this parameter, so
     * an implementation that does so is more likely to be correct.
     * <p>
     * However, you will get the best performance if you pass in <code>false</code>
     * for <code>mayNeedBufferForAnUnboundedPeriodOfTime</code>.  Passing
     * <code>false</code> acts as a hint to the implementation that the
     * memory management system should not have to bend over backwards to
     * pin the memory (or create bounce buffers), since the period of use
     * will be minimally short.  A usage pattern that will
     * qualify for having a 'bounded period of time' (and hence will jive well
     * with having the <code>mayNeedBufferForAnUnboundedPeriodOfTime</code>
     * parameter set to <code>false</code>) is one in which calls to
     * <code>getBuffer()</code> and <code>doneBuffer()</code> surround a system
     * call that is known not to block.
     * 
     * @param mayNeedBufferForAnUnboundedPeriodOfTime
     *             <code>true</code> if you cannot place an <i>a prior</i> bound on the amount of
     *             time between the call to <code>getBuffer()</code> and the call to
     *             <code>doneBuffer()</code>.
     * @param count the number of bytes desired.
     * @return a pointer to a buffer of size <code>count</code>.  You may
     *         return <code>VM_Address.fromObject(null)</code> if you are
     *         unable to satisfy the request.
     */
    public VM_Address getBuffer(int count,
                                boolean mayNeedBufferForAnUnboundedPeriodOfTime);
    
    /**
     * This method performs synchronization.  As such, it should
     * <emph>never</emph> be called from an interrupt handler.
     * <p>
     * <code>count</code> is used to specify how many bytes, starting
     * from the beginning of the buffer, were modified in a meaningful
     * way.  This interface does not support the case where random
     * chunks of the buffer are modified - it only supports contiguous
     * modification starting at the beginning!
     * @param buf the <code>VM_Address</code> returned from <code>getBuffer()</code>.
     * @param count the number of bytes that were modified.
     */
    public void doneBuffer(VM_Address buf,
                           int count);

    public void pinBuffer();
    public void unpinBuffer();
    
    // get a buffer at the given offset from the "buffer area" no longer than the
    // given length ; the actual length of the buffer can be read by
    // getLastContiguousBufferLength
    public VM_Address getContiguousBuffer( int length, int offset );
    
    public int getLastContiguousBufferLength();
}

