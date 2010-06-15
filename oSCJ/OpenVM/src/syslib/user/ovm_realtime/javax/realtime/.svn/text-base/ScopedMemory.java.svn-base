package javax.realtime;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.ovmj.java.Opaque;
import org.ovmj.util.PragmaForwardCallingContext;
/**
 * ScopedMemory implementation.
 *
 * @author Filip pizlo (original)
 * @author David Holmes (reworked for NHRT's, plus RTSJ 1.0.1 compliance)
 */
public abstract class ScopedMemory extends MemoryArea {

    /** Helper objects for the join protocol. Basically a mutable Boolean */
    static class Joiner {
        boolean ready = true;
    }

    /** 
     * The joiner for this scoped memory area. All synchronization is performed
     * on this object. Scopes can only be entered or exited when the lock
     * on this object is held.
     */
    Joiner joiner = new Joiner();

    static final boolean DEBUG_REFCOUNT = false;

    static final boolean ASSERTS_ON = true;
    
    static final boolean SUPPORT_SCOPE_AREA_OF =
	LibraryImports.supportScopeAreaOf();

    /** Link used for queueing finalizable areas */
    ScopedMemory nextScope;
    
    /** The direct reference count of this scoped memory area. */
    int refCount = 0;

    /*------------------------------------------------------
      The reference counting logic works as follows. Logically the
      reference count tracks whether this is the current MA for any SO, an
      MA in a scope stack of any SO, or the initial MA of any SO; plus the
      additional checks for active timers etc.

      Our actual refCount tracks the number of enter() calls that are made,
      the number of SO's for which this is the initial MA, and the number of
      active timers in an area.

      When the scope stack is copied we don't physically update the ref count
      for each scope in the stack (which might cause a NHRT to encounter a
      heap allocated scope). Instead we note that a scope can never be 
      reclaimed if it has a child scope attached. Note also that because
      creation of an SO in heap or immortal causes it to have an initial
      scope stack consisting of only heap or immortal, that we are guaranteed
      that if a scope is in a scope stack copied as part of SO creation, and
      the scope is not the IMA for that SO, then the scope must have a child
      scope.

      Consequently reclaiming a scope occurs when the refCount==0 && it has 
      no children.

      We further note that a scope can only lose its child when the child
      resets its parent. Hence resetting the parent causes a check of the
      parent to see if it also needs to be reclaimed. But even this check is
      not needed on the completion of an enter() call as the parent can't
      possibly be reclaimed then as its ref count must be > 0 due to either
      it being entered directly, or if it was the IMA, the upRef that occurs
      for an IMA when the thread is started.

      Whether or not we have a child is tracked only in the ED, so we
      make a LibraryImports call to check that.

      To preserve what we just described we have to ensure that scopes can not
      be processed out-of-order. This can occur when a scope is handed over to
      the scope-finalizer thread to run finalizers and do any post-finalization
      clean up necessary of the scope is unused once finalizers have run. To
      preserve the relative ordering we check that a scope does not have any
      child scopes that are awaiting processing by the scope finalizer thread,
      and if it does then we hand that scope to the finalizer thread too. 


      To ensure that new children can't be added or removed while this is happening
      we synchronize on the parent's joiner during a setParent and resetParent.
      ------------------------------------------------------*/


    /** The portal for this scoped memory area. This is required to violate
        RTSJ assignment semantics as it holds the portal object that must be
        allocated in the memory this scope represents - which is different to
        the memory are in which this scope itself is allocated. Barrier checks
        are elided when setting/reading this field.
    */
    private Object portal;

    /** Reference to our parent scope if we currently have one. 
    */
    ScopedMemory parent;

    /** The primordial scope acts as the parent for any scope for which no
        scoped memory appears above it in the current scope stack.
     */
    static final ScopedMemory primordialScope = new ScopedMemory() {
            public String toString() {
                return "PrimordialScope";
            }
        };

    // these things are needed for some computation. - FIXME: get rid of these
    private AbsoluteTime absTimeTemp = new AbsoluteTime();
    private AbsoluteTime absTimeTemp2 = new AbsoluteTime();


    /** A constant ZERO relative time */
    static final RelativeTime ZERO = new RelativeTime(0,0);


    /** No-op constructor used only the the primordial scope */
    private ScopedMemory() {
        super();
    }

    
    public ScopedMemory(long size) {
	this(size, null);
    }
    public ScopedMemory(long size, Runnable logic) {
	super(size, logic);
    }
    public ScopedMemory(SizeEstimator size) {
	this(size, null);
    }
    public ScopedMemory(SizeEstimator size, Runnable logic) {
	super(size, logic);
    }

    // debugging
    void validate() {
        Assert.check(LibraryImports.isScope(this.area), "ScopedMemory VM_Area is not a ScopedArea");
        System.out.print("ScopedMemory constructed: ");
        System.out.flush();
        LibraryImports.showAddress(this.area);
    }


    public long getMaximumSize() {
        return size(); // we allocate max at the start
    }

    /** 
     * @throws InaccessibleAreaException if <tt>this</tt> is not
     * on the scope stack of the current thread
     */
    void checkAccessible(RealtimeThread current) 
        throws InaccessibleAreaException {
        if (current.getScopeStack().getIndex(this) < 0)
            throw new InaccessibleAreaException();
    }

    void checkAccessible() throws InaccessibleAreaException {
        checkAccessible(RealtimeThread.currentRealtimeThread());
    }

    /**
     * @return the current real-time thread
     * @throws IllegalThreadStateException if the current thread is not a
     * real-time thread
     */
    RealtimeThread getCurrentRealtimeThread() {
        Thread current = Thread.currentThread();
        if (current instanceof RealtimeThread)
            return (RealtimeThread) current;
        throw new IllegalThreadStateException("Java threads can't use ScopeMemory");
    }

    // we have to override most of the public methods of MemoryArea to
    // exclude Java threads and declare the right exceptions

    public void enter() throws IllegalThreadStateException {
	enter(logic);
    }

    public void enter(Runnable logic) throws IllegalThreadStateException {
        if (logic == null)
            throw new IllegalArgumentException("null logic not permitted");
        RealtimeThread current = getCurrentRealtimeThread();
        enterImpl(current, logic);
    }

    public void executeInArea(Runnable logic) throws
               IllegalThreadStateException, InaccessibleAreaException {
        if (logic == null)
            throw new IllegalArgumentException("null logic not permitted");
        RealtimeThread current = getCurrentRealtimeThread();
        checkAccessible(current);
        execInAreaImpl(current, logic);
    }

    public Object newArray(Class type, int number)
	throws IllegalAccessException, NegativeArraySizeException,
               IllegalThreadStateException, InaccessibleAreaException {
        RealtimeThread current = getCurrentRealtimeThread();
        checkAccessible(current);
        return newArrayImpl(current, type, number);
    }

    public Object newInstance(Constructor cons, Object[] args)
        throws IllegalAccessException, InstantiationException,
               InvocationTargetException,
               IllegalThreadStateException, InaccessibleAreaException,
	       PragmaForwardCallingContext
    {
        RealtimeThread current = getCurrentRealtimeThread();
        checkAccessible(current);
        return newInstanceImpl(current, cons, args);
    }

    
    public Object newInstance(Class klass)
        throws IllegalAccessException,InstantiationException,
               IllegalThreadStateException, InaccessibleAreaException,
	       PragmaForwardCallingContext
    {
        RealtimeThread current = getCurrentRealtimeThread();
        checkAccessible(current);
        return newInstanceImpl(current, klass);
    }



    /** Actual setting of the portal */
    void doSetPortal(Object o) throws org.ovmj.util.PragmaNoBarriers {
        portal = o;
    }

    public void setPortal(Object o) {
        RealtimeThread current = getCurrentRealtimeThread();
        checkAccessible(current);
        if (o == null) return; // nulls mean a no-op
        if (SUPPORT_SCOPE_AREA_OF && getMemoryArea(o) != this) {
            throw new IllegalAssignmentError("portal object not allocated in target memory area");
        }
        doSetPortal(o);
    }
    
    Object doGetPortal() throws org.ovmj.util.PragmaNoBarriers {
        return portal;
    }

    public Object getPortal() {
        // have to check if the portal could be assigned to a field of an 
        // object allocated in the current allocation context.
        // As the portal object must be allocated in this memory area, then
        // the only place it can be stored is in an inner memory area. Which
        // means that the current area is either 'this' or a descendent of
        // this.
	// FIXME: what if currentMA is not VM_ScopedArea?  then we get
	// an ED class cast exception
        Opaque currentMA = LibraryImports.getCurrentArea();
        if (!SUPPORT_SCOPE_AREA_OF ||
	    currentMA == this.area || 
            LibraryImports.isProperDescendant(currentMA, this.area)) {
            
            return doGetPortal();
        }
        throw new IllegalAssignmentError("portal object inaccessible");
    }

    public int getReferenceCount() {
        // must sync as a return of zero is only allowed if finalization
        // has been completed.
        synchronized (joiner) {
            // we only need return an "indication" of how many SO's have access
            // to this scope. If the actual refCount is 0 but we have a child 
            // scope then the answer is at least 1, so we return 1.
            if (refCount > 0)
                return refCount;
            else if (LibraryImports.hasChildArea(this.area))
                return 1;
            else
                return 0;
        }
    }


    /** 
     * Find 'this' in the current scope stack and return a copy of the stack
     * from zero to 'this'
     */
    ScopeStack unwindUpToMe(RealtimeThread current) {
        int index = current.getScopeStack().getIndex(this); // will be >=0
        return new ScopeStack(current.getScopeStack(), 0, index);
    }


    // NOTE: we always lock child then parent, in that order, so that deadlock
    //       is not possible

    /**
     * Set this scope's parent to be the given memory area, if it is a scope, 
     * else sets it to be the primordial scope. Also sets the parent of our
     * VM_Area. The parent's joiner is locked while
     * we do this to ensure adding/removing child scopes is synchronized with
     *  scope entry/exit protocols.
     */
    void setParent(MemoryArea newParent) {
        if (!(newParent instanceof ScopedMemory))
            newParent = primordialScope;

        ScopedMemory p = (ScopedMemory) newParent;
        synchronized(p.joiner) {
            this.parent = p;
            LibraryImports.setParentArea(this.area, p.area);
        }
    }
                
    /** Clears the parent of this scope, and its associated VM area
     */
    void resetParent() {
        synchronized(parent.joiner) {
            parent = null;
            LibraryImports.resetParentArea(this.area);
        }
    }

    /** 
     * Increment the refCount when we know the refCount is not currently zero.
     * This is only called on the initial area of a thread (except one that
     * needed to be pushed on construction) when the thread is started.
     */
    void upRef() {
        synchronized (joiner) {
            if (ASSERTS_ON) 
                Assert.check(refCount != 0, "zero refCount in upRef!");
            refCount++;
            if (DEBUG_REFCOUNT || true) {
                System.out.println(this + " upRef by " + 
                                   Thread.currentThread().getName() + 
                                   " refcount = " + refCount);
            }
        }
    }

    /** 
     * Decrement the refCount when we know the refCount is not currently one.
     * This is only called when a scope allocated Timer is destroyed, or when
     * thread start throws OOME and we need to undo an upRef.
     */
    void downRef() {
        synchronized (joiner) {
            if (ASSERTS_ON) 
                Assert.check(refCount > 1 ? Assert.OK : 
                             "refCount <=1 in downRef!");
            refCount--;
            if (DEBUG_REFCOUNT)
                System.out.println(this + " downRef by " + 
                                   Thread.currentThread().getName() + 
                                   " refcount = " + refCount);
        }
    }


    /**
     * Increment the refCount when doing an enter into this area. The refCount
     * could be zero so we have to do all the right checks.
     */
    void upRefForEnter(RealtimeThread t) throws ScopedCycleException {
        // Note: allocation here occurs in our outer area
        synchronized (joiner) {
            if (refCount == 0) {
                if (!LibraryImports.hasChildArea(this.area)) {
                    if (ASSERTS_ON) 
                        Assert.check(parent == null ? Assert.OK :
                                     "scope has parent when it shouldn't!");
                    if (DEBUG_REFCOUNT) 
                        System.out.println(this + " upRefForEnter by " + 
                                           t.getName() + 
                                           " doing zeroToOneHook and setting parent");
                    // final check to avoid an obscure case that finalizers
                    // can cause
                    if (LibraryImports.areaOf(t) == this.area)
                        throw new IllegalThreadStateException("finalizer induced attempt to reclaim scope that holds the current thread");

                    zeroToOneHook();
                    setParent(t.getScopeStack().getCurrentArea());
                }
            } 
            else {
                if (ASSERTS_ON) 
                    Assert.check(parent != null ? Assert.OK :
                                 "scope has no parent when it should!");
                MemoryArea current = t.getScopeStack().getCurrentArea();
                if ((current instanceof ScopedMemory && current != parent) ||
                    (!(current instanceof ScopedMemory) 
                     && parent != primordialScope))
                    throw 
                        new ScopedCycleException("Single parent rule violated: " + 
                                                 this + " can't be entered from " + 
                                                 current + " because " + 
                                                 parent + " is its parent.");
            }
            refCount++;
            if (DEBUG_REFCOUNT) 
                System.out.println(this + " upRefForEnter by " + t.getName() + 
                                   " refcount = " + refCount);
            joiner.ready = false; // join must block if scope entered
        }
    }


    /**
     * Hook invoked when the refCount is transitioning from zero to one.
     * This implementation of the hook is responsible for actually clearing
     * this memory area from its previous use. A subclass (in package for
     * experimentation purposes) must call super.zeroToOneHook, or implement
     * this code.
     * <p>This is only called on a true zero to one transition. If finalization
     * causes an upRef then this hook will not be called as the refCount 
     * appears to be one when finalizers run. This is crucial as the finalizer
     * upRef must never cause the scope to be cleared.
     * <p>This method is called with the joiner lock held.
     */
    void zeroToOneHook() {
        // we clear on entry to avoid the problem of trying to clear
        // when the thread doing the clear is allocated in this area.
        // As this is a constant time operation the delay is okay from a
        // predictability perspective.
        LibraryImports.resetArea(this.area);
    }

    /**
     * Hook invoked when the refCount is transitioning from oneToZero. At the
     * time this is called the refCount is ONE, so that any up-refs don't 
     * cause the zeroToOneHook to run. 
     * The job of this hook is to run the finalizers.
     * If a subclass (for in-package experimentation) overrides this then they
     * should invoked <tt>super.oneToZeroHook</tt>.
     * <p>This method is called with the joiner lock held.
     */
    void oneToZeroHook() {
        runFinalizers();
    }


    /** 
     * Runs all the finalizers for objects allocated in this scope. First all
     * existing finalizers are run, then if there are more finalizers due to
     * objects created by the first set of finalizer, we run those, but only if
     * the reference count is unchanged. This continues until either there are 
     * no more finalizers to execute, or the reference count has increased 
     * from one.
     */
    void runFinalizers() {
        if (ASSERTS_ON) 
            Assert.check(refCount == 1 ? Assert.OK : 
                         "Incorrect refCount running finalizers: " + refCount);
        boolean moreToDo = false;
        do {
            moreToDo = LibraryImports.runFinalizers(this.area);
        } while (moreToDo && refCount == 1);
    }


    /**
     * Decrement the reference count, potentially taking the count to zero
     * resulting in the finalization of objects in the scope. The actual 
     * clearing of the scope occurs on the next entry - if any.
     * This is only invoked at the end of an enter call. Note that even though
     * we may clear our parent scope here, we do not need to check if the
     * parent needs to be finalized as it cannot be the case. The parent is
     * either another scope that we entered directly (in which case we don't 
     * finalize until we exit it directly) or it is the initial MA of this
     * thread; or it is an upper scope that we revisited through 
     * executeInArea. 
     * If it is the initial MA then either at construction or thread
     * start an upRef was done, so we're guaranteed that the parent has a 
     * none-zero ref count even if clearing the parent causes it to lose its
     * last child.
     * If it is an upper scope reentered through executeInArea then we are 
     * guaranteed that either it has a child other than this scope 
     * (part of the scope stack prior to the executeInArea call), or it has a 
     * non-zero refcount (because this thread already entered it explicitly).
     * <p>
     * The above analysis can break if we are not careful about how we process 
     * scopes in the scope finalizer thread. If the only child of this scope 
     * is awaiting finalization by the scope finalizer thread, then 
     * decrementing the refCount to zero here would require that the 
     * resetParent call by the child DID check if the parent needed 
     * finalization. To avoid that problem we preserve the order in which 
     * scopes are finalized by always handing the current scope to the 
     * scope-finalizer thread if any of its children are in the queue waiting 
     * to be processed by the scope finalizer thread. 
     */
    void downRefAfterEnter() {
        // we can't use a sync block because if we hand-off to the 
        // finalizer thread it owns the monitor
        boolean needExit = true;
        LibraryImports.monitorEnter(joiner);
        try {
            if (refCount > 1) { // nothing special to do
                if (ASSERTS_ON) 
                    Assert.check(parent != null ? Assert.OK : 
                                 "scope has no parent when it should!");
                if (DEBUG_REFCOUNT) 
                    System.out.println(this + 
                                       " downRefAfterEnter refCount == " 
                                       + refCount);
                refCount--;
            }
            else if (refCount == 1) {
                // if refCount is 1 this may be a special downRef, but because
                // finalization may upRef again, without the MA being cleared
                // we don't want to actually drop the refCount yet.

                // If curent thread is a NHRTT and we need to do finalization 
                // or we have a child that is being finalized by the finalizer 
                // thread we have to hand over to the finalizer thread; 
                //otherwise we can process this ourselves.
                boolean hasChild = LibraryImports.hasChildArea(this.area);
                if (Thread.currentThread() instanceof NoHeapRealtimeThread) {
                    if (!hasChild) {
                        if (DEBUG_REFCOUNT) 
                            System.out.println(this + " downRefAfterEnter: NHRTT refCount==1 && no child - doing handoff");
                        ScopeFinalizerThread.instance.add(this);
                        needExit = false;
                        return;
                    }
                }
                if (hasChild && 
                    ScopeFinalizerThread.instance.isProcessingChildOf(this))  {
                    if (DEBUG_REFCOUNT) 
                        System.out.println(this + " downRefAfterEnter: refCount==1 child in processing - doing handoff");
                    ScopeFinalizerThread.instance.add(this);
                    needExit = false;
                    return;
                }

                // safe to process this ourselves. No scope can become our child as we hold our lock.

                if (hasChild) { 
                    // nothing special                
                    if (ASSERTS_ON) 
                        Assert.check(parent != null ? Assert.OK : 
                                     "scope has no parent when it should!");
                    if (DEBUG_REFCOUNT) 
                        System.out.println(this + 
                                           " downRefAfterEnter refCount=1 but" 
                                           + " still has child");
                    refCount--;
                }
                else {
                    if (DEBUG_REFCOUNT) 
                        System.out.println(this + 
                                           " downRefAfterEnter refCount==1 and no child");
                    
                    oneToZeroHook();  // run finalizers
                    // now drop refCount and finish clean up
                    if (refCount-- == 1) {
                        if (LibraryImports.hasChildArea(this.area)) { 
                            // nothing special to do, we've been resurrected
                            if (ASSERTS_ON) 
                                Assert.check(parent != null ? Assert.OK : 
                                             "scope has no parent but it should!");
                            if (DEBUG_REFCOUNT) 
                                System.out.println(this + " downRefAfterEnter"+
                                                   " new child after" +
                                                   "zeroToOneHook");
                        }
                        else {
                            if (ASSERTS_ON)  {
                                if (parent != primordialScope) {
                                    boolean haskids = 
                                        LibraryImports.hasMultipleChildren(parent.area);
                                    if (parent.refCount == 0 ) {
                                        Assert.check(haskids ? Assert.OK :
                                                     "parent had zero refCount" +
                                                     " and single child about to be cleared!!");
                                    }
                                }
                            }
                            resetParent();
                            joiner.ready=true;
                            joiner.notifyAll();
                            doSetPortal(null);
                            if (DEBUG_REFCOUNT) 
                                System.out.println(this + " downRefAfterEnter reset parent etc");
                        }
                    }
                    else {
                        if (DEBUG_REFCOUNT) 
                            System.out.println(this + " downRefAfterEnter refCount non-zero after" +
                                               "zeroToOneHook");
                    }
                }
            }
            else {
                throw new InternalError("refCount invalid: " + refCount);
            }
        }
        finally {
            if (needExit)
                LibraryImports.monitorExit(joiner);
            if (DEBUG_REFCOUNT) 
                System.out.println(this + 
                                   " downRefAfterEnter complete: refCount =" + 
                                   refCount);
        }
    }

    /* flag to indicate this area was handed-off to the scope finalizer
       thread as part of the cleanup of the initial memory area (IMA) of
       a terminating thread. This requires an extra step in the clean-up.
    */
    boolean doIMAProcessing = false;

    /** 
     * Performs the leanup of a terminating thread's initial memory area
     * (IMA). We need to repeat some of the checks in downRefForEnter because
     * since determining the IMA needed to be handed off, the IMA may have
     * been used by another thread and may not need cleaninup now - just
     * decrement of the ref count. If cleanup is needed then we hand-off to
     * the scope finalizer thread, but mark this scope for special processing
     * as a cleanup of an IMA may require a cleanup of the parent scope and
     * its parent, etc.
     */
    void doIMACleanup() {
        boolean needExit = true;
        LibraryImports.monitorEnter(joiner);
        try {
            if (refCount > 1) { // nothing special to do
                if (ASSERTS_ON) 
                    Assert.check(parent != null ? Assert.OK : 
                                 "scope has no parent when it should!");
                if (DEBUG_REFCOUNT) 
                    System.out.println(this + 
                                       " doIMACleanup refCount == " 
                                       + refCount);
                refCount--;
            }
            else if (refCount == 1) {
                if (DEBUG_REFCOUNT) 
                            System.out.println(this + " doIMACleanup: refCount==1 -  doing handoff");
                doIMAProcessing = true;
                ScopeFinalizerThread.instance.add(this);
                needExit = false;
                return;
            }
            else {
                throw new InternalError("refCount invalid: " + refCount);
            }
        }
        finally {
            if (needExit)
                LibraryImports.monitorExit(joiner);
            if (DEBUG_REFCOUNT) 
                System.out.println(this + 
                                   " doIMACleaup complete: refCount =" + 
                                   refCount);
        }
    }

    /** Performed by the scope finalizer thread on behalf of another thread.
        This completes the downRef protocol by running finalizers and
        resetting parent etc if needed. Finally the scope is unlocked.
     */
    void completeDownRef() {
        if (ASSERTS_ON) 
            Assert.check(refCount == 1 ? Assert.OK : 
                         "completeDownRef - ref count was " + refCount);
        boolean hasChild = LibraryImports.hasChildArea(this.area);
        if (hasChild) { 
            // nothing special                
            if (ASSERTS_ON) 
                Assert.check(parent != null ? Assert.OK : 
                             "scope has no parent when it should!");
            if (DEBUG_REFCOUNT) 
                System.out.println(this + " completeDownRef refCount==1 but still has child");
            refCount--;
        }
        else {
            if (DEBUG_REFCOUNT) 
                System.out.println(this + 
                                   " completeDownRef refCount=1 and no child");
                    
            oneToZeroHook();  // run finalizers
            // now drop refCount and finish clean up
            if (refCount-- == 1) {
                if (LibraryImports.hasChildArea(this.area)) { 
                    // nothing special to do, we've been resurrected
                    if (ASSERTS_ON) 
                        Assert.check(parent != null ? Assert.OK : 
                                     "scope has no parent when it should!"); 
                    if (DEBUG_REFCOUNT) 
                        System.out.println(this + " completeDownRef new child after zeroToOneHook");
                }
                else {
                    if (parent != primordialScope) {
                        if (doIMAProcessing) {
                            if (DEBUG_REFCOUNT) 
                                System.out.println(this + 
                                           " completeDownRef parent.checkLastChild");

                            parent.checkLastChild(true);
                        }
                        else {
                            if (ASSERTS_ON) {
                                boolean haskids = 
                                    LibraryImports.hasMultipleChildren(parent.area);
                                if (parent.refCount == 0 ) {
                                    Assert.check(haskids ? Assert.OK :
                                                 "parent ("+ parent +
                                                 ") had zero refCount" +
                                                 " and single child("+ this +
                                             ") about to be cleared!!");
                                }
                            }
                        }
                    }
                    else {
                        System.out.println(this + 
                                           " completeDownRef parent is primordial");

                    }
                    resetParent();
                    joiner.ready = true;
                    // the joiner.notifyAll() was done previously when this 
                    // area was added to the finalizer thread queue. 
                    // We haven't released the lock so any waiters are
                    // queued on the lock
                    
                    doSetPortal(null);
                    if (DEBUG_REFCOUNT) 
                        System.out.println(this + 
                                           " completeDownRef reset parent etc");
                }
            }
            else {
                if (DEBUG_REFCOUNT) 
                    System.out.println(this + 
                                       " completeDownRef refCount non-zero" + 
                                       " after zeroToOneHook");
            }
        }

    }

    /**
     * The first part of a two-part protocol for decrementing the reference
     * count as a thread terminates and the thread is allocated in this scope.
     * We must ensure that termination of the thread, and releasing of joiners
     * (or simply allowing enters to proceed) occurs atomically with respect
     * to the termination of the thread. This is achieved in part by performing
     * the actual memory area clear on entry. The rest is achieved by acquiring
     * the join lock here, but not releasing it. Instead the second part of the
     * protocol is implemented by {@link #finishDownRefForTermination} which
     * releases the lock - and is invoked by the VM as part of the thread
     * termination process.
     * <p>
     * This process is compicated by the scope finaliser thread. If a child 
     * scope of this area is being processed by the scope finalizer thread
     * then this area must be too. But we can't hand-off here as we have to
     * ensure the thread can terminate before the scope can be reclaimed.
     * So we simply return false if this is the case and the thread will
     * defer the hand-off until thread-finalization time.
     * <p><b>NOTE:</b>This method should only be called by a real-time thread
     * that is in the process of terminating and was itself allocated in this
     * scoped memory area.
     * <p><b>NOTE 2: </b> While a thread terminates it will hold the joiner
     * lock, thus allowing that thread to re-enter the scope, or for any other
     * code it invokes to do an up-ref etc. 
     *
     *
     * @return true if we did the down ref and false if we need to hand-off
     * to the scope finalizer thread.
     */
    final boolean startDownRefForTermination() {
        if (DEBUG_REFCOUNT)
            System.out.println(this + " startDownRefForTermination: " + 
                               Thread.currentThread().getName() + 
                               " locking " + this);
        // note we don't release this monitor in this method unless
        // returning false
        LibraryImports.monitorEnter(joiner);
        if (DEBUG_REFCOUNT)
            System.out.println(this + " startDownRefForTermination: " + 
                               Thread.currentThread().getName() + 
                               " locked " + this);
        try {
            if (refCount > 1) { // nothing special to do
                if (ASSERTS_ON) 
                    Assert.check(parent != null ? Assert.OK :
                                 "scope has no parent when it should!");
                if (DEBUG_REFCOUNT) 
                    System.out.println(this + " startDownRefTerm refCount == " + 
                                       refCount);
                refCount--;
            }
            else if (refCount == 1) {

                // if refCount is 1 this may be a special downRef, but because
                // finalization may upRef again, without the MA being cleared
                // we don't want to actually drop the refCount yet.

                // if the scope finalizer thread is processing our child
                // then we can't be processed now

                boolean hasChild = LibraryImports.hasChildArea(this.area);
                if (hasChild && 
                    ScopeFinalizerThread.instance.isProcessingChildOf(this))  {
                    if (DEBUG_REFCOUNT) 
                        System.out.println(this + " startDownRef returning due to need to hand-off to scope finalizer thread");
                    LibraryImports.monitorExit(joiner);
                    return false;
                }

                // okay we can process.

                if (hasChild) { 
                    // nothing special                
                    if (ASSERTS_ON) 
                        Assert.check(parent != null ? Assert.OK :
                                     "scope has no parent when it should!");
                    if (DEBUG_REFCOUNT) 
                        System.out.println(this + 
                                           " startDownRefTerm refCount==1 but "
                                           + "still has child");
                    refCount--;
                }
                else {
                    if (DEBUG_REFCOUNT) 
                        System.out.println(this + 
                                           " startDownRefTerm refCount=1 and no child");

                    oneToZeroHook();  // run finalizers
                    // now drop refCount and finish clean up
                    if (refCount-- == 1) {
                        if (LibraryImports.hasChildArea(this.area)) { 
                            // nothing special to do, we've been resurrected
                            if (ASSERTS_ON) 
                                Assert.check(parent != null ? Assert.OK :
                                             "scope has no parent when it should!");
                            
                            if (DEBUG_REFCOUNT) 
                                System.out.println(this + " startDownRefTerm new child " +
                                                   "after zeroToOneHook");
                        }
                        else {
                            // snapshot parent before we reset
                            ScopedMemory par = parent;
                            resetParent();
                            joiner.ready = true;
                            joiner.notifyAll();
                            doSetPortal(null);
                            if (DEBUG_REFCOUNT) 
                                System.out.println(this + " startDownRefTerm reset parent etc - checking parent's last child");
                            if (par != primordialScope) {
                                par.checkLastChild(false);
                                if (DEBUG_REFCOUNT) 
                                    System.out.println(this + " startDownRefTerm  checked parent for last child");

                            }
                            if (DEBUG_REFCOUNT) 
                                System.out.println(this + " startDownRefTerm complete");


                        }
                    }
                    else { // resurrected
                        if (DEBUG_REFCOUNT) 
                            System.out.println(this + 
                                               " startDownRefTerm refCount non-zero"
                                               + " after zeroToOneHook");
                    }
                }
            }
            else {
                throw new InternalError("refCount invalid: " + refCount);
            }
        }
        catch (Error e) {
            LibraryImports.monitorExit(joiner);
            throw e;
        }
        catch (RuntimeException  rte) {
            LibraryImports.monitorExit(joiner);
            throw rte;
        }
        return true;
    }



    /**
     * Called as part of the initial memory area cleanup during thread
     * termination.
     * @param doUnlock if true then the monitor of this.joiner is released 
     * before returning; otherwise it remains locked with the expectation that
     * the terminating thread will unlock it during the second phase of the
     * termination protocol. Cleanup that has been handed-off to the scope
     * finalizer thread will always perform the unlock here.
     */
    final void checkLastChild(boolean doUnlock) {
        LibraryImports.monitorEnter(joiner);
        try {
            if (refCount > 0) { // nothing to do
                if (DEBUG_REFCOUNT) 
                    System.out.println(this + 
                                       " checkLastChild - no action: refCount == " 
                                       + refCount);
                return;
            }
            if (LibraryImports.hasMultipleChildren(this.area)) { // nothing to do
                if (DEBUG_REFCOUNT) 
                    System.out.println(this + 
                                       " checkLastChild no action: multiple children");
                return;
            }

            // refCount == 0 and last child about to go away
            
            refCount = 1; // set so that a finalizer can't cause a zeroToOne
                          // transition that would clear this area
            oneToZeroHook();  // run finalizers
        
            if (refCount > 1 || 
                LibraryImports.hasMultipleChildren(this.area)) { 
                if (DEBUG_REFCOUNT) 
                    System.out.println(this + " checkLastChild: resurrected");
                refCount--;
                return;
            }

            if (DEBUG_REFCOUNT) 
                System.out.println(this + " checkLastChild: clearing parent etc");

            // snapshot parent before resetting
            ScopedMemory par = parent;
            resetParent();
            joiner.ready=true;
            joiner.notifyAll();
            doSetPortal(null);
            refCount--;
            // now clean-up and process our parent the same way
            if (par != primordialScope) {
                if (DEBUG_REFCOUNT) 
                    System.out.println(this + " checkLastChild: checking parent for last child");

                par.checkLastChild(doUnlock);
                if (DEBUG_REFCOUNT) 
                    System.out.println(this + " checkLastChild: checked parent");

            }

            if (DEBUG_REFCOUNT) 
                System.out.println(this + " checkLastChild: done");

        }
        finally {
            if (doUnlock) {
                LibraryImports.monitorExit(joiner);
            }
        }
    }




    /**
     * The second part of the two-part protocol for cleaning up the scope
     * stack as a potentially scope-allocated thread terminates. In this
     * phase we unlock the joiner of this scope, if it was locked in the
     * first phase.
     * This method is invoked by the VM during a VM critical section.
     */
    final void finishDownRefForTermination() 
        throws org.ovmj.util.PragmaNoPollcheck {
        // we don't know if we were locked so all we can do is ask first.
        // We could save this information somewhere, in the scopestack perhaps
        // to speed this up.
        if (Thread.holdsLock(this.joiner)) {
            if (DEBUG_REFCOUNT)
                System.out.println(this + " finishDownRefForTermination: " + 
                                   Thread.currentThread().getName() + 
                                   "unlocking " + this);
            LibraryImports.monitorExit(joiner);
            if (DEBUG_REFCOUNT)
                System.out.println(this + " finishDownRefForTermination: " + 
                                   Thread.currentThread().getName() + 
                                   "unlocked " + this);

        }
        else {
            if (DEBUG_REFCOUNT)
                System.out.println(this + " finishDownRefForTermination: " + 
                                   Thread.currentThread().getName() + 
                                   " doesn't hold lock on " + this);

        }
    }


    // FIXME: don't use HRT objects here just use raw times in nanos and use
    //        the direct API's that tell you whether the wait timed out or not.
    
    /**
     * @return true if the ref count actually dropped to zero, 
     * false if the time expired first.
     */
    boolean joinImpl(HighResolutionTime t) throws InterruptedException {
        // we hold the lock when called
        if (t instanceof RelativeTime) {
            if (t.getMilliseconds() == 0 &&
                t.getNanoseconds() == 0) {
                while (!joiner.ready) {
                    joiner.wait();
                }
            } else {
                return joinImpl(t.absolute(Clock.getRealtimeClock(),
                                           absTimeTemp));
            }
        } else {
            while (!joiner.ready) {
                HighResolutionTime.waitForObject(joiner, t);
                Clock.getRealtimeClock().getTime(absTimeTemp2);
                if (t.compareTo(absTimeTemp2)<=0) {
                    return false;
                }
            }
        }
        
        return true;
    }

    public void join(HighResolutionTime t) throws InterruptedException {
        synchronized (joiner) {
            joinImpl(t);
        }
    }

    public void join() throws InterruptedException {
	join(ZERO);
    }
    
    public void joinAndEnter(Runnable logic, HighResolutionTime t)
	throws IllegalThreadStateException,
               InterruptedException   {

        RealtimeThread current = getCurrentRealtimeThread();
	
        synchronized (joiner) {
            joinImpl(t);
            try {
                upRefForEnter(current);
            } catch (ScopedCycleException e) {
                throw new InternalError(
                    "got ScopedCycleException from upRefForEnter() inside joinAndEnter()");
            }
        }
        
        current.getScopeStack().push(this);
        Opaque currentMA = LibraryImports.setCurrentArea(this.area);
        try {
            logic.run();
        } 
        catch (Throwable e) {
            reThrowTBE(currentMA, e);
        }
        finally {
            try {
                downRefAfterEnter();
            }
            finally {
                current.getScopeStack().pop();
                LibraryImports.setCurrentArea(currentMA);
            }
        }
    }

    public void joinAndEnter(Runnable logic)
	throws InterruptedException  {
	joinAndEnter(logic, ZERO);
    }

    public void joinAndEnter(HighResolutionTime t)
	throws InterruptedException  {
	if (logic==null) {
	    throw new IllegalArgumentException("null Runnable logic");
	}
	joinAndEnter(logic, t);
    }

    public void joinAndEnter() throws InterruptedException {
	joinAndEnter(ZERO);
    }
}


