package javax.realtime;

import org.ovmj.java.Opaque;
import org.ovmj.util.PragmaNoBarriers;


/**
 * The scope finalizer thread is responsible for executing the finalizers of
 * objects allocated in scoped memory, on behalf of 
 * <tt>NoHeapRealtimeThread</tt> instances that might encounter heap 
 * references if they did the finalization themselves. In addition, due to
 * the need to process scope "exits" in a consistent order - child then
 * parent - it will also process a scope whose child is being processed.
 * <p>
 * Unlike other system threads this one runs at low-priority by default,
 * only gaining in priority when some other thread needs to (re)enter a scope
 * that is being finalized, or is joining a scope. This priority gain occurs 
 * through the normal priority inheritance mechanism (with a sneaky notifyAll
 * thrown in for the joiners).
 * <p>
 * The scope finalizer thread maintains a queue of scoped memory areas 
 * waiting to be finalized and blocks on this queue while it is empty. When
 * a thread adds a scope to the queue it first assigns the scope lock for that
 * scope to the finalizer thread, then signals that a scope is in the queue.
 * <p><b>The scope finalizer thread acts like a normal heap-using thread and is
 * subject to GC delays as normal. It follows that a NHRT that wants to 
 * reenter or join a scope that had finalizers, may also be delayed by the 
 * GC.</b>
 * <p>
 * When the finalizers execute the current allocation context must be the
 * scope being finalized and the scope stack must be valid with respect to the
 * single parent rule. The simplest way to achieve this would be to copy the
 * scopestack of the thread that hands off the scoped memory area for
 * finalization - however that would be rather expensive in both time and
 * memory use, particulary as the same thread might be handing off multiple
 * scopes in succession as its call stack unwinds. Instead we lazily create
 * a temporary scope stack by considering the parent of the current area being
 * finalized. This is a little painful as we have to find the primordial
 * scope first and then recreate from the children down to the current area.
 * But we can't directly find the child from the parent so we have to size
 * the stack first then build an array of the areas that should be in the stack
 * and then push them on in the right order.
 *
 * @author David Holmes
 */
class ScopeFinalizerThread extends RealtimeThread.VMThread {

    /** The single instance of the scope finalizer thread - this will be
        started by the JVM by forcing static initialization of this class. 
        It is allocated in Immortal memory but runs in heap. 
    */
    static final ScopeFinalizerThread instance = new ScopeFinalizerThread();
    static {
        instance.start();
    }
    
    ScopeFinalizerThread() {
        super(HeapMemory.instance());
	 // run at min by default
        setPriorityInternal(RealtimeJavaDispatcher.MIN_RT_PRIORITY);
        setDaemon(true);  // don't keep the VM alive
        setName("ScopeFinalizerThread");
    }


    static final boolean DEBUG = false;

    /** Reference to the vmThread of this Java thread */
    Opaque me;

    // note we must process the scopes FIFO as they may be parent & child

    /** head of the queue of scoped memory areas needing finalizing */
    ScopedMemory head = null;
    /** tail of the queue of scoped memory areas needing finalizing */
    ScopedMemory tail = null;


    /** add the given area to the queue of areas awaiting finalization 
        and transfer its joiner monitor to this thread
     */
    synchronized void add(ScopedMemory area) throws PragmaNoBarriers {
        if (Assert.ENABLED) {
            RealtimeThread current = RealtimeThread.currentRealtimeThread();
            Assert.check(current instanceof NoHeapRealtimeThread ? Assert.OK :
                         "Heap using thread " + current + 
                         "handed off scope for finalization");
        }
        if (tail != null) {
            tail.nextScope = area;
            tail = area;
        }
        else {
            head = tail = area;
        }
        notify();
        // the finalizer thread runs at the highest priority of any thread
        // waiting for the finalizer to complete. Because joiners fall in this
        // category we have to ensure they become priority inheritance sources
        // for the finalizer thread. Doing a notifyAll now ensures they are on
        // the monitor queue and so the PIP will do the necessary work. If
        // after the finalizers have completed, the joiner is not released,
        // then it will simply see a "spurious wakeup".
        area.joiner.notifyAll();

        LibraryImports.monitorTransfer(area.joiner, me);
        if (DEBUG) dumpQueue("after add");
    }

    /** take the next area from the queue, blocking until one is available */
    synchronized ScopedMemory take() throws PragmaNoBarriers {
        while (head == null)
            try {
                wait();
            }
            catch(InterruptedException ex) {
                throw new InternalError("ScopeFinalizerThread interrupted!");
            }

        ScopedMemory temp = head;
        head = head.nextScope;
        if (head == null) tail = null;
        temp.nextScope = null; // must unlink
        if (DEBUG) dumpQueue("after take");
        return temp;
    }


    /** Inspect the queue, and currently processing scope, to see if any
        scope has the given scope as its parent.
    */
    synchronized boolean isProcessingChildOf(ScopedMemory parent) {
        try {
            if (current != null && current.parent == parent)
                return true;
            for (ScopedMemory s = head; s != null; s = s.nextScope) {
                if (s.parent == parent)
                    return true;
            }
            return false;
        }
        finally {
            if (DEBUG) dumpQueue("isProcessingChildof");
        }
    }


    void dumpQueue(String where) {
        System.out.print(where);
        System.out.println(" Scope-finalizer-thread queue:");
        System.out.println("\t" + current + " (parent = " + 
                           (current == null ? 
                            "N/A" : String.valueOf(current.parent)) + ")");
        int i = 0;
        for (ScopedMemory s = head; s != null; s = s.nextScope) {
            System.out.println("\t[" + i+"]" + s + " (parent = " + 
                               s.parent + ")");
        }
        if (head == null)
            System.out.println("\t<empty>");
    }
            
    /** Flag to indicate whether we have to construct a real scope stack
     *  that includes the currently being finalized memory area. We do this
     *  lazily so that it is only done when absolutely needed
     */
    boolean scopeStackValid = false;

    /** keep hold of our original (minimal) scope stack */
    ScopeStack original = null;

    void saveOriginalScopeStack() {
        original = super.getScopeStack();
    }
        
    /** the scoped memory area being finalized. If null then we're not 
        finalizing anything. Note this can be a scope allocated object
    */
    ScopedMemory current;

    ScopeStack getScopeStack() {
        //System.out.println("ScopeFinalizerThread asked for its ScopeStack");
        if (!scopeStackValid && current != null) {
            saveOriginalScopeStack();
            //super.getScopeStack().dump("Original");

            // make new scope stack as required
            setScopeStack(scopeStackFor(current));
            scopeStackValid = true;
            //super.getScopeStack().dump("Reconstructed");
        }
        return super.getScopeStack();
    }

    void restoreOriginalScopeStack() {
        if (original != null) {
            ScopeStack temp = super.getScopeStack();
            setScopeStack(original);
            //super.getScopeStack().dump("Restored");
            temp.free();
            original = null;
        }
        scopeStackValid = false;
    }


    /** 
     * Creates a temporary scope stack the state of which matches the
     * parent hierarchy of the given current area, with immortal at the
     * bottom. This will always be called when current is the current
     * allocation context - otherwise we messed up somewhere. Because we
     * can't leak a ScopeStack object into current we jump to the heap to
     * do the allocation.
     */
    ScopeStack scopeStackFor(ScopedMemory current) {
        int size = LibraryImports.getHierarchyDepth(current.area);
        ScopeStack temp;
        Opaque currentMA = 
            LibraryImports.setCurrentArea(HeapMemory.instance().area);
        try {
            if (Assert.ENABLED)
                Assert.check(currentMA == current.area ? Assert.OK : 
                             ("wrong current allocation context: " +
                              LibraryImports.getAreaMirror(currentMA)));

            temp = new ScopeStack(size);
        }
        finally {
            LibraryImports.setCurrentArea(currentMA);
        }

        MemoryArea area = current;
        for (int index = size-1; index >= 0; index--) {
            Assert.check (area != null ? Assert.OK : 
                          "Found null area in parent chain!");

            if (area == ScopedMemory.primordialScope) {
                Assert.check (index == 0 ? Assert.OK : 
                              "Found primordial area before reaching the end");
                area = ImmortalMemory.instance();
                temp.force(index, area);
            }
            else {
                temp.force(index, area);
                area = ((ScopedMemory) area).parent;
            }
        }
        return temp;
    }

    public void run() throws PragmaNoBarriers {
        if (DEBUG)
            System.out.println("Scope Finalizer thread running: current MA is "
                           + RealtimeThread.getCurrentMemoryArea() +
                           ", this.MA = " + MemoryArea.getMemoryArea(this));

        me = LibraryImports.getVMThread(this);
        while (true) {
            // need the sync to ensure current is set atomically wrt take()
            // otherwise we can lose the scope when scanning the queue
            synchronized(this) {
                current = take();
            }
            if (DEBUG)
                System.out.println("Scope Finalizer thread working on " + 
                                   current);
            Opaque currentMA = LibraryImports.setCurrentArea(current.area);
            try {
                current.completeDownRef();
                LibraryImports.setCurrentArea(currentMA);
            }
            catch(Throwable t) {
                // the Throwable is allocated in a scope so beware
                LibraryImports.setCurrentArea(currentMA);
                System.out.println("INTERNAL error in scope finalizer thread - exception came from completeDownRef: ");
                t.printStackTrace();
            }
            finally {
                LibraryImports.monitorExit(current.joiner);
                restoreOriginalScopeStack();
            }
            if (DEBUG)
                System.out.println("Scope Finalizer thread done with " + 
                                   current);
        }
    }
}
