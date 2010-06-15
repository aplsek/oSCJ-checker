package javax.realtime;
import java.lang.reflect.Constructor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import org.ovmj.java.Opaque;
import org.ovmj.util.PragmaNoBarriers;
import org.ovmj.util.PragmaForwardCallingContext;
/**
 * Memory area.
 * <h3>OVM Notes</h3>
 * <p>We seperate maintenance of the scope stack itself, from the issue of
 * updating the reference count for a given area. Hence the logical entry of
 * an area requires both a pushing on the scope stack, and then an upRef
 * on the area.
 *
 * @author Jason Baker (original version - ScopeStackNode and UD ScopeTree)
 * @author Filip Pizlo (original version - ScopeStackNode and UD ScopeTree)
 * @author David Holmes (ScopeStack version with ED tree for NHRT support)
 * 
 */
public class MemoryArea {

    /** Our ED memory region - this is itself allocated in its own region for
        scoped memory
     */
    final Opaque area;

    /** The associated logic to execute in enter(), if any */
    final Runnable logic;

   
    /**
     * Package constructor for use by heap and immortal instances only
     * @param a The ED memory region corresponding to heap or immortal as
     * appropriate.
     */
    MemoryArea(Opaque a) {
        this(a, -1, null);
    }

    /**
     * Package constructor used only for the primordial scope
     */
    MemoryArea() {
        area = null;
        logic = null;
    }

    private MemoryArea(Opaque area, long sizeInBytes, Runnable logic)  
        throws PragmaNoBarriers {
        if (area != null) {
            this.area = area;
            this.logic = null;
        }
        else {
            if (sizeInBytes <= 0)
                throw new IllegalArgumentException("size must be > 0");
            // this is an OVM restriction due to the underlying API's taking
            // int instead of long
            if (sizeInBytes > Integer.MAX_VALUE)
                throw new OutOfMemoryError("size too big");

            this.area = LibraryImports.makeArea(this, (int) sizeInBytes);
            this.logic = logic;
        }
    }

    protected MemoryArea(long sizeInBytes) {
        this(sizeInBytes, null);

    }
    
    protected MemoryArea(long sizeInBytes, Runnable logic) {
        this(null, sizeInBytes, logic);
    }
    
    protected MemoryArea(SizeEstimator size) {
        this(null, size.getEstimate(), null);
    }
    
    protected MemoryArea(SizeEstimator size, Runnable logic) {
        this(null, size.getEstimate(), logic);
    }
    
    
    /** 
     * Increase reference count and establish parent relationship if necessary.
     * If the ref count transitions from zero to one cleanup actions are
     * performed if necessary. 'this' is not yet the current memory area.
     * This is performed prior to entering a memory area, and is also called
     * when a scoped memory area must be pushed on the construction of a
     * schedulable object.
     * <p>The default implementation does nothing, as only scoped memory
     * requires special handling.
     *
     * @param current reference to the current thread that is doing the enter
     */
    void upRefForEnter(RealtimeThread current) throws ScopedCycleException { }

    /** 
     * Decrease reference count and tear down parent relationship if necessary.
     * If the reference count transitions from one to zero then the memory
     * area is "finalized". 'this' is still the current memory area.
     * <p>The default implementation does nothing, as only scoped memory
     * requires special handling.
     */
    void downRefAfterEnter() { }

   
    /** 
     * Creates and returns a new scope stack consisting only of this. 
     * (ScopedMemory will override to produce a more appropriate scope stack.)
     */
    ScopeStack unwindUpToMe(RealtimeThread current) {
        return new ScopeStack(this);
    }


    /** space for temporary region when making a temporary scope stack.
        Normally it just needs to hold the actual ScopeStack object, but
        assertions and debug code can change that.
    */
    static final int TEMPSIZE;
    static {
        if (ScopedMemory.DEBUG_REFCOUNT || ScopeStack.DEBUG_SCOPESTACK)
            TEMPSIZE = 10*1024; // add room for debugging output. If the
                                // stack is deep this value will need adjusting
        else if (ScopeStack.ASSERTS_ON)
            TEMPSIZE = 2*1024;  // extra room for possible assertion error
        else
            TEMPSIZE = ScopeStack.baseSize;
    }
        
    /** 
     * Unwinds the scope stack so that <tt>this</tt> is the current area,
     * executes the logic and then restores the original scope stack and
     * current allocation context.
     * @param current the current real-time thread
     * @param logic the code to execute in the context of this area
     */
    final void execInAreaImpl(RealtimeThread current, Runnable logic) {
        //System.out.println("Making explicit area of size: " + TEMPSIZE);
        Opaque temp = LibraryImports.makeExplicitArea(TEMPSIZE);
        //System.out.println("Actual area size: " + LibraryImports.getAreaSize(temp));
        //System.out.println("Remaining space: " + LibraryImports.memoryRemaining(temp));
        try {
            Opaque currentMA = LibraryImports.setCurrentArea(temp);
            try {
                ScopeStack oldStack = current.getScopeStack();
                ScopeStack newStack = unwindUpToMe(current);
                try {
                    current.setScopeStack(newStack);
                    try { 
                        LibraryImports.setCurrentArea(area);
                        logic.run();
                    } finally {
                        current.setScopeStack(oldStack);
                    }
                }
                finally {
                    newStack.free();
                }
            } finally {
                LibraryImports.setCurrentArea(currentMA);
            }
        }
        finally {
            //            System.out.println("Remaining space after use: " + LibraryImports.memoryRemaining(temp));

            LibraryImports.destroyArea(temp);
        }
    }
    
    /**
     * Increment the ref count of this, push this onto the scope stack, execute
     * the given logic, restore the scope stack and decrement the ref count 
     * of this.
     * @param current the current real-time thread
     * @param logic the code to run
     * @throws ScopedCycleException if this is already parented and the current
     * top scope is not that parent
     */
    final void enterImpl(RealtimeThread current,Runnable logic) 
        throws ScopedCycleException {
        upRefForEnter(current);
        current.getScopeStack().push(this);
        Opaque currentMA = LibraryImports.setCurrentArea(area);
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


    /** 
     * Performs enter/executeInArea for a plain Java thread
     */
    final void enterJavaThread(Runnable logic) {
        // just set this to be current and restore old current after
        Opaque currentMA = LibraryImports.getCurrentArea();
        LibraryImports.setCurrentArea(area);
        try {
            logic.run();
        } 
        finally {
            LibraryImports.setCurrentArea(currentMA);
        }
    }
    
    static MemoryArea getMemoryAreaObject(Opaque area) {
        if (area == ImmortalMemory.instance.area)
            return ImmortalMemory.instance;
        else if (area == HeapMemory.instance.area)
            return HeapMemory.instance;
        else
            return LibraryImports.getAreaMirror(area);
    }



    /**
     * Performs newArray for a plain Java thread
     */
    final Object newArrayJava(Class type, int number)
	throws IllegalAccessException, NegativeArraySizeException {
        Opaque currentMA = LibraryImports.getCurrentArea();
        LibraryImports.setCurrentArea(area);
        try {
            return Array.newInstance(type, number);
        } 
        finally {
            LibraryImports.setCurrentArea(currentMA);
        }
    }


    final Object newArrayImpl(RealtimeThread current, Class type, int number)
	throws IllegalAccessException, NegativeArraySizeException {
        Opaque temp = LibraryImports.makeExplicitArea(TEMPSIZE);
        try {
            Opaque currentMA = LibraryImports.setCurrentArea(temp);
            try {
                ScopeStack oldStack = current.getScopeStack();
                ScopeStack newStack = unwindUpToMe(current);
                try {
                    current.setScopeStack(newStack);
                    try { 
                        LibraryImports.setCurrentArea(area);
                        return Array.newInstance(type, number);
                    } finally {
                        current.setScopeStack(oldStack);
                    }
                }
                finally {
                    newStack.free();
                }
            } finally {
                LibraryImports.setCurrentArea(currentMA);
            }
        }
        finally {
            LibraryImports.destroyArea(temp);
        }
    }


    /**
     * Performs newInstance for a plain Java thread
     * @throws IllegalThreadStateException if not permitted for a Java thread
     */
    Object newInstanceJava(Class klass)
        throws IllegalAccessException,InstantiationException,
	       PragmaForwardCallingContext
    {
        Opaque currentMA = LibraryImports.getCurrentArea();
        LibraryImports.setCurrentArea(area);
        try {
            return klass.newInstance();
        } 
        finally {
            LibraryImports.setCurrentArea(currentMA);
        }
    }

    
    Object newInstanceImpl(RealtimeThread current, Class klass)
        throws IllegalAccessException,InstantiationException,
	       PragmaForwardCallingContext
    {
        Opaque temp = LibraryImports.makeExplicitArea(TEMPSIZE);
        try {
            Opaque currentMA = LibraryImports.setCurrentArea(temp);
            try {
                ScopeStack oldStack = current.getScopeStack();
                ScopeStack newStack = unwindUpToMe(current);
                try {
                    current.setScopeStack(newStack);
                    try { 
                        LibraryImports.setCurrentArea(area);
                        return klass.newInstance();
                    } finally {
                        current.setScopeStack(oldStack);
                    }
                }
                finally {
                    newStack.free();
                }
            } finally {
                LibraryImports.setCurrentArea(currentMA);
            }
        }
        finally {
            LibraryImports.destroyArea(temp);
        }
    }


    Object newInstanceJava(Constructor cons, Object[] args)
        throws IllegalAccessException, InstantiationException,
               InvocationTargetException, PragmaForwardCallingContext
    {
        Opaque currentMA = LibraryImports.getCurrentArea();
        LibraryImports.setCurrentArea(area);
        try {
            return cons.newInstance(args);
        } 
        finally {
            LibraryImports.setCurrentArea(currentMA);
        }
    }


    Object newInstanceImpl(RealtimeThread current, Constructor cons, 
                           Object[] args)
        throws IllegalAccessException,InstantiationException,
               InvocationTargetException,
	       PragmaForwardCallingContext
    {
        Opaque temp = LibraryImports.makeExplicitArea(TEMPSIZE);
        try {
            Opaque currentMA = LibraryImports.setCurrentArea(temp);
            try {
                ScopeStack oldStack = current.getScopeStack();
                ScopeStack newStack = unwindUpToMe(current);
                try {
                    current.setScopeStack(newStack);
                    try { 
                        LibraryImports.setCurrentArea(area);
                        return cons.newInstance(args);
                    } finally {
                        current.setScopeStack(oldStack);
                    }
                }
                finally {
                    newStack.free();
                }
            } finally {
                LibraryImports.setCurrentArea(currentMA);
            }
        }
        finally {
            LibraryImports.destroyArea(temp);
        }
    }




    // logic for dealing with scope allocated exceptions that try to
    // propagate out of their scope

    static final boolean DEBUG_TBE = false;

    static void dump(String msg, Throwable problem) {
        System.err.println(msg);
        System.err.println("--- Dumping stacktrace");
        try {
            problem.printStackTrace();
        }
        catch(Throwable t) {
            System.err.println("stacktrace on problem generated exception");
        }
    }


    /**
     * Check if <tt>e</tt> can be stored in the given outer memory area, if so
     * then rethrow it, else wrap it in a <tt>ThrowBoundaryError</tt> allocated
     * in the outer memory area and throw that.
     * @param outer our outer memory area
     * @param e the exception object being thrown
     * @return Never - allways throws an exception
     * @throws E or <tt>ThrowBoundaryError</tt>
     */
    static void reThrowTBE(Opaque outerMA, Throwable e) {
        MemoryArea eArea = getMemoryArea(e);
        if (eArea instanceof ScopedMemory) {
            // if e was allocated in scope then there is no way that outer
            // can store a reference to it. If outer is heap or immortal then
            // it can't hold a reference to scope. If outer is a scope, then it
            // would have to be an inner scope to hold a reference to e, and
            // it is an outer scope. Hence if e is in scope we must wrap.
            if (DEBUG_TBE) System.err.println("Wrapping in TBE");
            try {
                // this is for debugging only until we come up with a way to
                // pass the real exception info through to the TBE
                e.printStackTrace();
            }
            catch(Throwable t) { 
                // ignore
            }
            throw wrapTBE(e, eArea, outerMA);
        }
        if (DEBUG_TBE) 
            System.out.println("Compatible MA no need to wrap:" +
                               "outer MA is " + outerMA + ", exception MA is "
                               + eArea);
        throw rethrowUnchecked(e);
    }

    static Error rethrowUnchecked(Throwable e) {
        if (e instanceof Error) {
            throw (Error)e;
        } else if (e instanceof RuntimeException) {
            throw (RuntimeException)e;
        } else {
            throw new Error("Unexpected checked exception",e);
        }
    }


    /**
     * Convert a thrown exception into a {@link ThrowBoundaryError} allocated
     * in the given outer memory area.
     *
     * <p>We have have to step out to the MA in which we will see and process
     * the ThrowBoundaryError. This means the TBE instance AND the string
     * constructed from the original throwable must be allocated in the
     * outer MA. We're assuming here that <tt>e.toString()</tt> will actually
     * create new values and not return an existing scope allocated value.
     * If that occurs we fallback to trying to extract e's type
     *
     * @param e the original exception
     * @param eArea the allocation context of <tt>e</tt>
     * @param outerMa the outer memory area in which to allocate the
     * <tt>ThrowBoundaryError</tt> instance to be thrown
     */
    static ThrowBoundaryError wrapTBE(Throwable e, MemoryArea eArea,
                                      Opaque outerMa) {
        Opaque current = LibraryImports.setCurrentArea(outerMa);        
        String msg = null;
        try {
            try {
                msg = e.toString();
                // check where the string was allocated: anywhere is safe
                // except for the same allocation context as e
                if (getMemoryArea(msg) == eArea) {
                    try {
                        if (DEBUG_TBE) 
                            System.err.println("e.toString() => scoped " + 
                                               "- trying class name instead");
                        msg = e.getClass().getName();
                    }
                    catch (Throwable t2) {
                        msg = "< unidentifiable exception>";
                        if (DEBUG_TBE) 
                            dump("Exception on getClass().getName()", t2);
                    }
                }
                if (DEBUG_TBE) {
                    try {
                        e.printStackTrace();
                    }
                    catch(Throwable _) {}
                }
            } 
            catch (Throwable t) {
                try {
                    if (DEBUG_TBE) dump("Exception on e.toString()", t);
                    msg = e.getClass().getName();
                }
                catch (Throwable t2) {
                    if (DEBUG_TBE) 
                        dump("Exception on getClass().getName()", t2);
                    msg = "< unidentifiable exception>";
                }
            }
            // any exceptions from this are unmanageable and should be
            // considered internal errors
            return new ThrowBoundaryError(msg);
        } finally {
            LibraryImports.setCurrentArea(current);
        }
    }


    // our public interface

    /**
     * Gets the <code>MemoryArea</code> in which the given object is located.
     * 
     * @return The current instance of <code>MemoryArea</code> of the object.
     */
    public static MemoryArea getMemoryArea(Object object) {
        return getMemoryAreaObject(LibraryImports.areaOf(object));
    }

    public long memoryRemaining() {
        return LibraryImports.memoryRemaining(area);
    }

    public long memoryConsumed() {
        return LibraryImports.memoryConsumed(area);
    }
   
    public long size() {
        return LibraryImports.getAreaSize(area);
    }

    public void enter() {
	enter(logic);
    }

    public void enter(Runnable logic)  {
        if (logic == null)
            throw new IllegalArgumentException("null logic not permitted");

        Thread current = Thread.currentThread();
        if (current instanceof RealtimeThread) {
            enterImpl((RealtimeThread) current, logic);
        }
        else {
            enterJavaThread(logic);
        }
    }

    public void executeInArea(Runnable logic) {
        if (logic == null)
            throw new IllegalArgumentException("null logic not permitted");

        Thread current = Thread.currentThread();
        if (current instanceof RealtimeThread) {
            execInAreaImpl((RealtimeThread) current, logic);
        }
        else {
            enterJavaThread(logic);
        }
    }

    public Object newArray(Class type, int number)
	throws IllegalAccessException, NegativeArraySizeException {
        Thread current = Thread.currentThread();
        if (current instanceof RealtimeThread) {
            return newArrayImpl((RealtimeThread) current, type, number);
        }
        else {
            return newArrayJava(type, number);
        }
    }

    public Object newInstance(Constructor cons, Object[] args)
        throws IllegalAccessException,
               InstantiationException,
               InvocationTargetException,
	       PragmaForwardCallingContext
    {
        Thread current = Thread.currentThread();
        if (current instanceof RealtimeThread) {
            return newInstanceImpl((RealtimeThread)current, cons, args);
        }
        else {
            return newInstanceJava(cons, args);
        }
    }

    
    public Object newInstance(Class klass)
        throws IllegalAccessException,InstantiationException,
	       PragmaForwardCallingContext
    {

        Thread current = Thread.currentThread();
        if (current instanceof RealtimeThread) {
            return newInstanceImpl((RealtimeThread)current, klass);
        }
        else {
            return newInstanceJava(klass);
        }
    }

}


