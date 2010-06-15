package javax.realtime;

import org.ovmj.java.Opaque;
import org.ovmj.util.PragmaNoBarriers;
/**
 * The scope stack is a conceptual data structure defined by the memory
 * usage rules of the Real-time Specification for Java. Each real-time
 * thread (and effectively async event handler) has a current scope stack. 
 * The initial scope stack is determined by the scope stack of the creating 
 * thread/handler and by the initial memory area passed to the thread/handler 
 * constructor.
 * <p>
 * The scope stack then grows/shrinks as different memory areas are entered
 * and left. This would be a simple structure except for the fact that the
 * <tt>executeInArea</tt> method requires the formation of a new scope stack 
 * which temporarily replaces the current one for the duration of that method. 
 * This means that the entire stack has to be "pushed" on entry to the method 
 * and "popped" on exit - which can be done by copying the data structure.
 *
 * <p>The scope stack does not control or influence the "single parent rule"
 * It's main role is to answer the question: "is area X on the current scope 
 * stack?", and to maintain the scope-stack indexing mechanism that the RTSJ 
 * defines. 
 *
 * <p>Rules and definitions:
 * <ul>
 * <li>The current allocation context is the <b>top</b> of the scope stack.
 * <li>The bottom of the scope stack has index zero, and is the outermost
 * scope (even if it is actually heap or immortal).
 * <li>Outer scopes have a longer lifetime than inner ones, and so an outer
 * scope can not hold a reference to an object from an inner scope (that is
 * the RTSJ programming model - though the VM can violate this when safe).
 * <li>The initial memory area of the thread/handler has a constant index in
 * the initial scope stack. If the initial scope stack is pushed, due to
 * executeInArea, it may not be valid to ask for the index of the initial area
 * (this is a somewhat vague area of the spec and the information is completely
 * useless in any case).
 * </ul>
 *
 * <p>The scope stack naturally contains references to memory areas that
 * are "newer" than the area in which the current thread/handler was
 * constructed. These references need to be managed with scope checks disabled.
 *
 * <h3>Memory Management</h3>
 *
 * <p>Memory management for a scope stack is done using an executive domain
 * transient area. These areas allow for dynamic memory allocation without
 * consuming any memory from the current allocation context. This means that
 * we can grow a scope stack as needed by making a new area of the desired 
 * size, copying over the contents, and then freeing an area when it is no
 * longer needed.
 * <p>This memory management approach has to be applied at two levels.
 * Internally, the data structures of a scope stack are kept in a special
 * transient area that grows as needed. Additionally, when a temporary scope 
 * stack is needed, that will also be allocated in its own transient area.
 *
 * <h3>Design Strategy for No-heap Thread Support</h3>
 * <p>It is perfectly valid for a no-heap thread to contain a heap-allocated
 * scoped memory object. In fact there must be either a heap allocated or
 * immortal allocated scope in the scope stack of such a thread. In the case
 * of a heap allocated scope, this is a problem.
 * <p>Logically this heap allocated scope would be encountered when one of
 * three actions occur in the NHRT:
 * <ol>
 * <li>The scope stack is copied during construction of another thread or
 * async event handler
 * <li>All scopes have their reference counts incremented when the NHRT
 * starts execution
 * <li>All scopes have their reference counts decremented as the NHRT 
 * terminates
 * </ol>
 * <p>We avoid these heap accesses as follows:
 * <p>First, we store the ED <tt>ScopedArea</tt> reference (as an 
 * <tt>Opaque</tt>) rather than the actual <tt>MemoryArea</tt> object.
 * (<b>Caution:</b> The ONLY thing you can do with an Opaque is read/write and
 * compare using ==  - You cannot invoke any methods on them, nor do instanceof
 * or do a cast!) This allows the
 * scope stack to be copied without encountering a heap reference as the ED
 * objects are in their own transient area as previously explained.
 * <p>Second, we use a more complicated reference counting scheme that combines
 * a direct reference count (for actual <tt>enter()</tt> calls and use of an
 * initial memory area) with an implicit
 * reference count indicated by a scope having a child scope - in which case 
 * the scope must exist in some threads scope stack. Due to the way the scope
 * stack must be structured, this means that when a thread is started it only
 * has to increment the reference count of the initial memory area. The other
 * scopes in its scope stack are kept alive by the fact that the initial memory
 * area is a child of the next scope in the stack. This avoids the need to 
 * refer to a heap reference when a thread starts, and reduces the cases in 
 * which a heap reference must be accessed when a thread terminates. Note 
 * however that this does complicate the process of reclaiming scopes.
 * <p>Finally, there will be some cases where a terminating NHRT causes the 
 * last child of a heap allocated scope to be removed, thus requiring that the
 * heap allocated scope be processed (run finalizers, release joiners etc).
 * This is unavoidable so either the NHRT must do it, or it must signal some 
 * other thread to do it. Handing off to another thread is very problematic
 * - though it is noted that somehow we must have a way for a NHRT to do this
 * when cleaning up scopes in general. A simpler solution appears to be to
 * change the no-heap thread to a heap-using thread as it terminates. This is
 * okay from the perspective that the thread is terminating and so any GC 
 * delays won't affect application logic executed by the thread. However, it
 * also raises the issue of how the thread "looks" to any finalizers that it
 * executes (for example a finalizer could query if the current thread is a
 * NHRT and assert that touching a heap reference throws an exception - which
 * it wouldn't). 
 *
 * <h3>Thread-Safety</h3>
 * <p>Scope stacks are not thread-safe. Only the current thread should ever 
 * access or modify its own scope stack.
 *
 * @author David Holmes
 */
class ScopeStack {

    // NOTE: no asserts or debug code are executed when mem is the current
    //       allocation context. Otherwise you'd have to adjust memSize to
    //       allow for the assertion error, or the debug messages etc.
    //       Clients that create scopestacks in their own regions need to
    //       check these settings and adjust their sizes accordingly.

    static final boolean DEBUG_SCOPESTACK = false;

    static final boolean ASSERTS_ON = true;

    // utility methods/fields to size what memory is needed by a ScopeStack

    /** Size of a raw ScopeStack object. Note that this doesn't get
        allocated in the region used by this scopestack but rather has
        to be accounted for in the allocation context when a scopestack
        is created - usually when the thread is constructed, but also
        for executeInArea, and for the scope fianlizer thread.
        Note that the creating context needs to allow for debugging/assertions
        in its space considerations.
    */
    static final int baseSize = (int) LibraryImports.sizeOf(ScopeStack.class);

    /** Returns the amount of memory required during construction of a
        scopestack with the given capacity. Not all constructors have the
        same allocation pattern however, and some have their memory usage
        determined by the constructor argument - eg for copy constructors.
        See the constructor docs for details.
    */
    static long  constructionSizeOf(int capacity) {
        long size = OpaqueToIntIdentityHashtable.sizeOf(capacity);
        size += LibraryImports.sizeOfReferenceArray(capacity);
        return size;
    }

    /** 
     * Default initial scope stack capacity - and the increment by which
     * we expand. 
     */
    static final int CAPACITY = 8;

    /** The minimum size our internal region needs to be. Note that any
        exceptions during construction will likely result in an out-of-memory
        condition.
    */
    static final int MEM_SIZE = (int) constructionSizeOf(CAPACITY);


    /** The ED region which holds our internal data structures */
    Opaque mem;

    /** The current size of mem */
    int memSize;

    /** Specialized map that maintains the relationship between a scope
        and its index in the scope stack without ever performing any 
        allocation. As discussed for memory management
        we store the Opaque reference to the ED memory area. Note that
        heap and immortal (which can appear multiple times in a scope-stack)
        are not stored in this map.
        <p>Note: the current implementation of the map always has a capacity
        that is a power of 2.
        <p>Note 2: from a predictability perspective the worst-case lookup
        is O(n) where n is the stack depth. We'd have the same worst-case
        if we simply did a linear scan of the stack array and we'd save the
        memory and computational overhead of using this map. 
     */
    OpaqueToIntIdentityHashtable map;

    /** The actual scope stack, again storing Opaque's, and including scopes
        as well as heap and immortal.
     */
    Opaque[] stack;

    /** Current scope stack size - also acts as next index into the stack */
    int size;

    /** Current scope stack capacity */
    int capacity;


    /**
     * Create a scope stack that is a copy of the given scope stack.
     * The memory used by this constructor is determined by the memory 
     * currently being used by the other scopestack, as we make an identical
     * copy.
     *
     * @param other the scope stack to copy
     */
    ScopeStack(ScopeStack other) throws PragmaNoBarriers {
        if (ASSERTS_ON) {
            Assert.check(other.map != null ? Assert.OK :
                         "copying stack with null map!");
            Assert.check(other.noNulls() ? Assert.OK :
                         "copying stack with null entry");
        }
        if (DEBUG_SCOPESTACK) other.dump(this +": start copy constructor");

        capacity = other.capacity;
        size = other.size;
        memSize = other.memSize;
        mem = LibraryImports.makeExplicitArea(memSize);
        Opaque current = LibraryImports.setCurrentArea(mem);
        try {
            map = new OpaqueToIntIdentityHashtable(other.map);
            stack = (Opaque[]) other.stack.clone();
        } finally {
            LibraryImports.setCurrentArea(current);
        }

        if (ASSERTS_ON)
            Assert.check(noNulls() ? Assert.OK : 
                         "null entry found in scope stack");
        if (DEBUG_SCOPESTACK) dump("after copy constructor");
    }


    /**
     * Create a scope stack that is a copy of the given scope stack using
     * the given indices to delineate the start and end of the stack.
     * The memory used by this constructor is determined by the memory 
     * currently being used by the other scopestack, but with a lower bound
     * defined by <tt>CAPACITY</tt>. The memory used will be the minimum of
     * <tt>other.memSize</tt> and <tt>constructionSizeOf(CAPACITY)</tt>.
     *
     * @param other the scope stack to copy
     * @param start the index at which copying starts
     * @param end   the index at which copying finishes.
     */
    ScopeStack(ScopeStack other, int start, int end) throws PragmaNoBarriers {
        if (ASSERTS_ON) {
            Assert.check(other.noNulls() ? Assert.OK :
                         "copying sub-stack with null entry");
            Assert.check(start >= 0 && start < other.size ? Assert.OK :
                         "start out of range: " + start);
            Assert.check(end >= start && end < other.size ? Assert.OK : 
                         "end out of range: " + end);
        }
        if (DEBUG_SCOPESTACK) other.dump(this +": start copy-range constructor");
        int count = end - start + 1;
        if (count < CAPACITY) {
            capacity = CAPACITY;
            memSize = MEM_SIZE;
        }
        else {
            capacity = other.capacity;
            memSize = other.memSize;
        }
        size = count;

        mem = LibraryImports.makeExplicitArea(memSize);
        Opaque current = LibraryImports.setCurrentArea(mem);
        try {
            map = new OpaqueToIntIdentityHashtable(capacity);
            stack = new Opaque[capacity];
            for (int i = 0, j = start; j <= end; i++, j++) {
                Opaque area = other.stack[j];
		LibraryImports.storeInOpaqueArray(stack, i, area);
                // only store scope areas in the map
                if (area != HeapMemory.instance().area &&
                    area != ImmortalMemory.instance().area)
                    map.put(area, i);
            }
        } finally {
            LibraryImports.setCurrentArea(current);
        }
        if (ASSERTS_ON)
            Assert.check(noNulls() ? Assert.OK : 
                         "null entry found in scope stack");
        if (DEBUG_SCOPESTACK) dump("copy-range constructor");
    }


    /**
     * Create a scope stack with a default capacity that contains only
     * the given memory area.
     * The memory used by this constructor is given by
     *  <tt>constructionSizeOf(CAPACITY)</tt>.
     *
     * @param bottom the bootom area in the scope stack
     */
    ScopeStack(MemoryArea bottom) throws PragmaNoBarriers {
        if (ASSERTS_ON) {
            Assert.check(bottom != null ? Assert.OK :  "null bottom area");
            Assert.check(bottom == HeapMemory.instance() || 
                         bottom == ImmortalMemory.instance() ||
                         LibraryImports.isScope(bottom.area) ? Assert.OK :
                         "INVALID MA Passed!");
        }
        capacity = CAPACITY;
        size = 0;
        memSize = MEM_SIZE;
        mem = LibraryImports.makeExplicitArea(memSize);
        Opaque current = LibraryImports.setCurrentArea(mem);
        try {
            map = new OpaqueToIntIdentityHashtable(capacity);
            stack = new Opaque[capacity];
            LibraryImports.storeInOpaqueArray(stack, 0, bottom.area);
            // only scope areas go in the map
            if (bottom instanceof ScopedMemory)
                map.put(bottom.area, 0);
            size++;
        } finally {
            LibraryImports.setCurrentArea(current);
        }
        if (ASSERTS_ON)
            Assert.check(noNulls() ? Assert.OK : 
                         "null entry found in scope stack");
        if (DEBUG_SCOPESTACK) dump("single-area constructor");
    }


    /**
     * Create an empty scope stack with the given capacity.
     * The memory used by this constructor is given by 
     * <tt>constructionSizeOf(capacity)</tt>.
     *
     * @param bottom the bootom area in the scope stack
     */
    ScopeStack(int capacity) throws PragmaNoBarriers {
        this.capacity = capacity;
        size = 0;
        memSize = (int) constructionSizeOf(capacity);
        mem = LibraryImports.makeExplicitArea(memSize);
        Opaque current = LibraryImports.setCurrentArea(mem);
        try {
            map = new OpaqueToIntIdentityHashtable(capacity);
            stack = new Opaque[capacity];
        } finally {
            LibraryImports.setCurrentArea(current);
        }
        if (DEBUG_SCOPESTACK) dump("empty-stack constructor");
    }



    // debugging
    void dump(String where) {
        Opaque debugArea = null;
        if (Thread.currentThread() instanceof NoHeapRealtimeThread)
            debugArea = ImmortalMemory.instance().area;
        else
            debugArea = HeapMemory.instance().area;

        Opaque current = LibraryImports.setCurrentArea(debugArea);
        try {
            System.out.print("Dump of ");
            System.out.print(this);
            System.out.print("(size = ");
            System.out.print(size);
            System.out.print("): ");
            System.out.print(where);
            for (int i = 0; i < size; i++) {
                System.out.print("  stack[");
                System.out.print(i);
                System.out.print("] = ");
                System.out.println(Integer.
                                   toHexString(LibraryImports.
                                               toAddress(stack[i])));
            }
            if (size == 0) 
                System.out.println(" <empty stack!>");
            // always dump the map so we can see it's in sync with the stack
            System.out.println("Dump of scope map:");
            map.dump();
        }
        finally {
            LibraryImports.setCurrentArea(current);
        }
    }

    boolean noNulls() {
        for (int i = 0; i < size; i++) {
            if (stack[i] == null)
                return false;
        }
        return true;
    }


    /** 
     * Forces the given MemoryArea into this stack at the given index.
     * If an entry at the given index exists it is removed.
     * <p>Used for manually constructing scope stacks.
     * @param index the index where to place the given area. It must be
     * within the current capcity of the scope stack
     * @param ma the area to force in
     * 
     */
    void force(int index, MemoryArea ma) {
        if (ASSERTS_ON) {
            Assert.check(ma !=  ScopedMemory.primordialScope ? Assert.OK :
                         "force() passed primordial scope");
            Assert.check(ma != null ?Assert.OK :  "pushing null area");
            Assert.check( index >= 0 && index < capacity ? Assert.OK :
                          " force() index out of range");
        }
        if (DEBUG_SCOPESTACK) dump("before force");

        Opaque oldArea = stack[index];
        if (oldArea != null) {
            map.remove(oldArea);
        }
        else {
            size++; // not replacing but adding
        }
        LibraryImports.storeInOpaqueArray(stack, index, ma.area);
        if (ma instanceof ScopedMemory) // only scope areas go in the map
            map.put(ma.area, index);

        if (DEBUG_SCOPESTACK) dump("after force");
    }

    /**
     * Pushes the given memory area onto the current scope stack
     * @param ma The memory area to push
     */
    void push(MemoryArea ma) {
        if (ASSERTS_ON) {
            Assert.check(ma !=  ScopedMemory.primordialScope ? Assert.OK :
                         "push() passed primordial scope");
        }
        if (size == capacity) {
            grow();
            if (DEBUG_SCOPESTACK) dump("before push - after grow()");
        }

        LibraryImports.storeInOpaqueArray(stack, size, ma.area);
        if (ma instanceof ScopedMemory) // only scope areas go in the map
            map.put(ma.area, size);
        size++;

        if (DEBUG_SCOPESTACK) dump("after push");
    }

    /** 
     * Pops the last pushed MemoryArea from the scope stack
     */
    void pop() {
        if (DEBUG_SCOPESTACK) dump("before pop");
        --size;
        Opaque area = stack[size];
        if (area != HeapMemory.instance().area &&
            area != ImmortalMemory.instance().area)
            map.remove(area);
        stack[size] = null;
        if (DEBUG_SCOPESTACK) dump("after pop");
    }

    /**
     * Returns the index of the given scoped memory area in the current scope
     * stack, or -1 if the area is not in the current scope stack
     *
     * @param ma the scoped memory area to look for
     */
    int getIndex(ScopedMemory ma) {
        int index = map.get(ma.area);
        if (index == OpaqueToIntIdentityHashtable.NOTFOUND)
            return -1;
        else {
            if (ASSERTS_ON)
                Assert.check(index < size ? Assert.OK : 
                             "index from map is out of range");
            return index;
        }
    }


    /**
     * Return the memory area at the given index in this scope stack, or
     * null if the index is out of range.
     */
    MemoryArea areaAt(int index) {
        if (index >= 0 && index < size) {
            return MemoryArea.getMemoryAreaObject(stack[index]);
        }
        else {
            return null;
        }
    }

    /**
     * Return the 'current' memory area in this scope stack
     */
    MemoryArea getCurrentArea() {
        return MemoryArea.getMemoryAreaObject(stack[size-1]);
    }


    /**
     * Return the depth of this scope stack
     */
    int getDepth() {
        return size;
    }


    /**
     * Frees this scope stack. This should be done as part of thread
     * termination.
     */
    void free() {
        Opaque tmp = mem;
        mem = null; // no dangling reference into returned memory
        LibraryImports.destroyArea(tmp);
    }


    /**
     * Grows the scope stack in increments of CAPACITY.
     */
    void grow() throws PragmaNoBarriers {
        Opaque oldMem = mem;
        capacity += CAPACITY;
        memSize = (int) (constructionSizeOf(capacity) + 
                         OpaqueToIntIdentityHashtable.iteratorSize);
        mem = LibraryImports.makeExplicitArea(memSize);
        Opaque current = LibraryImports.setCurrentArea(mem);
        try {
            // attempt all allocations first
            Opaque[] newStack = new Opaque[capacity];
            OpaqueToIntIdentityHashtable newMap = 
                new OpaqueToIntIdentityHashtable(capacity, map);

            // now copy things across
            LibraryImports.copyArrayElements(stack, 0, newStack, 0, size);

            /* debug info: map.printCollisionInfo(); */

            // finally update actual fields
            map = newMap;
            stack = newStack;
        } finally {
            LibraryImports.setCurrentArea(current);
        }
        LibraryImports.destroyArea(oldMem);
        if (ASSERTS_ON)
            Assert.check(noNulls() ? Assert.OK :
                         "null entry found in scope stack");
    }


    /**
     * Create a copy of the current thread's scope stack, or for a Java thread
     * create a stack containing only the current memory area
     */
    static ScopeStack copyCurrentStack() {
        Thread current = Thread.currentThread();
        if (current instanceof RealtimeThread)
            return new ScopeStack(((RealtimeThread)current).getScopeStack());
        else
            return new ScopeStack(RealtimeThread.getCurrentMemoryArea());
    }


}
