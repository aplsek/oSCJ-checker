package ovm.services.monitors;

import ovm.core.domain.ObjectModel.PragmaModelOp;
import ovm.core.execution.Context;

/**
 * A fast-locking object model maintains 3 possible lock states:
 * <dl><dt>FAST_UNLOCKED</dt>
 * <dt>FAST_LOCKED</dt><dd>with an associated owner and recursion count</dd>
 * <dt>INFLATED</dt><dd>with an assoicated Monitor object (which may be
 * locked or unlocked</dd>
 * 
 * @author Jason Baker, transcribed and embellished by Chapman Flack
 **/
public interface FastLockable extends MonitorMapper {
    String ID = "Lovm/services/monitors/FastLockable;";

    /**
     * Provide a fast and compact locking mechanism.  This method must
     * be both extremely fast and extremely compact, since it is
     * open-coded at the point of a monitorenter instruction.  
     * Ideally, this should be a compare and swap of 0 with the current native 
     * context handle, 
     * but depending on the model and size of a context handle, it may 
     * be necessary to shift the context by some constant.
     *
     * <p>On success, we have transitioned from FAST_UNLOCKED to FAST_LOCKED with
     * a recursion count of 0.  But failure does not imply that the object
     * is in FAST_LOCKED or INFLATED state.
     *
     * @return <tt>true</tt> on success, and <tt>false</tt> on failure
     */
    boolean fastLock() throws PragmaModelOp;

    /**
     * This method attempts to transition from FAST_LOCKED with the current
     * native context handle and a recursion count of zero to FAST_UNLOCKED.
     * Its speed and size contraints resemble those of {@link #fastLock()}. 
     * @return <tt>true</tt> on success, and <tt>false</tt> on failure
     */
    boolean fastUnlock() throws PragmaModelOp;

    /**
     * Note: fastLock and fastUnlock may fail even if the object is in the
     * FAST_UNLOCKED state.
     * @return a snapshot, which may be wrong by the time you look at it, 
     * unless caller has ruled out concurrency/interruption.
     **/
    boolean isFastUnlocked() throws PragmaModelOp;

    /**
     * @return a snapshot, which may be wrong by the time you look at it, 
     * unless caller has ruled out concurrency/interruption. 
     * {@link #isMine()} may be useful for a common use case.
     **/
    boolean isFastLocked() throws PragmaModelOp;

    /**
     * Query if the receiver object has a monitor.
     * @return <code>true</code> iff the receiver has a monitor. 
     * A <tt>false</tt> result is a snapshot, which may be wrong by the time 
     * you look at it, unless the caller has ruled out 
     * concurrency/interruption; a <tt>true</tt> result can be relied
     * on without such precautions--once a monitor, always a monitor.
     **/
    boolean isInflated() throws PragmaModelOp;

    /**
     * Test if the receiver is fast-locked <em>by the current context</em>.
     * Unlike {@link #isFastLocked()}, this method returns a result better 
     * than a snapshot; either result can only be invalidated by action of 
     * the current context, and is good until then.
     * @return <code>true</code> iff the receiver is fast-locked by the current
     * context.
     * @throws PragmaModelOp <em>this is a pragma</em>
     */
    boolean isMine() throws PragmaModelOp;
  
    /**
     * Return the context currently owning this lock, or an
     * undefined value if this object is not FAST_LOCKED.
     * <p>This is a snapshot, which may be wrong by the time you look at it, 
     * unless caller has ruled out concurrency/interruption, or current context
     * is the owner.
     *
     * @return the context urrently owning this lock, or an
     * undefined value if this object is not FAST_LOCKED.
     */
    Context getOwner() throws PragmaModelOp;

    /**
     * Unconditionally store the native-context handle <em>nc</em> into the 
     * bits that are used to store that handle in fast-locked objects. 
     * This will be rather antisocial unless the other bits are already 
     * consistent with a fast-locked state or will be made so, before the state
     * is visible to any other thread, with {@link #setRecursionCount}.
     * @param ctx the context
     * @throws PragmaModelOp <em>this is a pragma</em>
     */
    void setOwner(Context ctx) throws PragmaModelOp;

    /**
     * Return the recursion count of this object, or an undefined value
     * if the object is not FAST_LOCKED
     * <p>This is a snapshot, which may be wrong by the time you look at it, 
     * unless caller has ruled out concurrency/interruption, or current context
     * is the owner.
     * @return the recursion count of this object, or an undefined value
     * if the object is not FAST_LOCKED
     */
    int getRecursionCount() throws PragmaModelOp;

    /**
     * Unconditionally store the recursion count <em>rc</em> into the bits
     * that are used to store that count in fast-locked objects. This will be
     * rather antisocial unless the other bits are already consistent with a
     * fast-locked state or will be made so, before the state is visible to any
     * other thread, with {@link #setOwner}.
     * @param rc a recursion count
     * @throws PragmaModelOp <em>this is a pragma</em>
     */
    void setRecursionCount(int rc) throws PragmaModelOp;
    
    /**
     * Interface to some ObjectModel-wide properties affecting this interface.
     * Any ObjectModel that supports <tt>FastLockable</tt> will implement this
     * interface, so
     * <pre>
     * ((FastLockable.Model)ObjectModel.getObjectModel()).maxRecursionCount()
     * </pre>
     * is an example of how to query.
     * @author Chapman Flack
     */
    interface Model {
        String ID = "Lovm/services/monitors/FastLockable$Model;";

        /**
         * The largest value of <em>recursion-count</em> supported by the
         * configured concrete model.
         * @throws PragmaModelOp <em>this is a pragma</em>
         */
        int maxRecursionCount() throws PragmaModelOp;
    }
}
