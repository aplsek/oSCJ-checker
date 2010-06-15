package ovm.core.execution;

import ovm.core.Executive;
import ovm.core.OVMBase;
import ovm.core.domain.Oop;
import ovm.core.domain.WildcardException;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.core.stitcher.InvisibleStitcher;
import ovm.core.stitcher.InvisibleStitcher.PragmaStitchSingleton;
import ovm.services.bytecode.JVMConstants.InvokeSystemArguments;
import ovm.core.execution.Native;
import ovm.util.Iterator;
import ovm.util.OVMError;
import s3.core.domain.S3ByteCode;
import s3.util.PragmaNoInline;
import s3.util.PragmaTransformCallsiteIR;
import s3.util.PragmaInline;
import s3.util.PragmaNoPollcheck;
import ovm.core.services.memory.MemoryManager;

/**
 * Representation of an Execution Context. Different kinds of context were
 * envisaged for OVM, but at the time of writing only thread contexts exist.
 * All execution contexts for the system are maintained in a linked list.
 *
 * @author Grothoff, Flack
 * @author David Holmes - for per-context flags and context list changes
 **/
public class Context extends OVMBase {

    /**
     * Per-context flags used by the CSA as context-local-storage.
     * These are mainly for debugging and testing purposes to watch for
     * recursive entry into critical functions: like monitor processing,
     * and exception throwing.
     */
    public boolean[] flags = new boolean[4];

    public static final int EXCEPTION_THROW_RECURSION = 0;
    public static final int EXCEPTION_GENERATION_RECURSION = 1;
    static final int MONITOR_RECURSION = 2;
    public static final int PROCESSING_GENERATED_EXCEPTION = 3;

    /**
     * Opaque handle to the struct that represents the context on the
     * native level.
     **/
    public int nativeContextHandle_;

    public void funDebugStuff() {
	Native.print("nc: ");
	Native.print_int(nativeContextHandle_);
	Native.print("\n");
    }

    // This could be region allocated
    private Activation activation_;

    /**
     * Per-Context WildcardException instance used to avoid allocation during
     * exception processing. Only one wildcard per thread/context can ever be
     * active at a time - it is either being thrown or unwrapped. No code
     * should ever hold onto a reference to a wildcard beyond the catch clause
     * that caught it.
     */
    private final WildcardException wce = new WildcardException();

    /**
     * Wrap an instance of any domain's throwable type in a
     * WildcardException
     **/
    public Oop makeWildcardException(Oop throwable) {
	wce.setOriginalThrowable(throwable);
	return VM_Address.fromObject(wce).asOop();
    }

    /**
      We maintain a doubly-linked list of Contexts for traversal by the GC.
      A Context is added on construction and removed when destroy() is called.
      Because the current context is never destroyed and the last thread causes
      termination, it follows that the context list is never empty once the
      first context is added during bootstrap.
      <p>
      Note: These contexts could be region allocated in some configurations,
            and the region is assumed/required to have a lifetime greater than
            the context.
    */
    static Context first = null; 
    static Context last = null; // we don't actually need this - DH
    Context next;
    Context prev;
    
    public Context() throws PragmaNoBarriers {
        activation_ = Activation.factory().make();
        
       // not needed if the model has translating barriers on fastlock
       // entries
       //  MemoryManager.the().pin(this);
        nativeContextHandle_ = makeNativeContext((Context) VM_Address.fromObject(this).asAnyObject()); 
        insert(this);
    }

    /** Atomically insert the given context at the head of the list */
    static void insert(Context cur) throws PragmaInline, PragmaNoPollcheck,
                                           PragmaNoBarriers {
	cur.next = first;
	cur.prev = null;
	if (first == null) { // empty list (only happens once)
	    first = last = cur;
	}
	else { // non-empty list
	    first.prev = cur;
	    first = cur;
	}
    }

    /**
     * Destroy this context. Frees the associated native data
     * structures and atomically removes the context from the
     * context list. Must not be invoked on the current context.
     **/
    public void destroy() throws PragmaNoBarriers, PragmaNoPollcheck {
        assert(this != getCurrentContext());
        if (nativeContextHandle_ != 0) {
            Interpreter.destroyNativeContext(nativeContextHandle_);
            nativeContextHandle_ = 0;
	}

        // remove from list - remember the list never empties
	if (next == null) // removing from tail
	    last = prev;
	else              // not at tail
	    next.prev = prev;

	if (prev == null) // removing from head
	    first = next;
	else              // not at head
	    prev.next = next;
    }

    /**
     * Return an iterator over all of the contexts in the VM.
     */
    public static Iterator iterator() {
        // FIXME: we do not want to do allocation here as the GC uses
        // this and the GC is invoked because an allocation couldn't be
        // satisfied. If we define a resetable iterator the GC can preallocate
        // it and reset each time it wants to use it. - DH 6 Jan 2004
	return new ContextIterator();
    }

    /** 
        The iterator has to support limited concurrent modifications to
        the list without allowing contexts to be missed. The supported
        scenario is preemption of the iteration by a no-heap thread at
        safepoints. 
        The actions of a no-heap thread can cause three changes to the list:
        <ol>
        <li>A new NHRT context can be added to the head of the list.
        <lp>
        This context will not be seen by the iterator that is in progress.
        This is not a problem however because a NHRT context does not
        need to be scanned.
        </li>
        <li>A new heap-using context can be added to the head of the list.
        <lp>
        This context will not be seen by the iterator that is in progress.
        This is also not a problem because the thread associated with this
        new context can not actually execute while GC (and thus this
        iteration) is in progress. Hence it has no stack that needs scanning.
        </li>
        <li>A NHRT context can be removed from anywhere in the list.
        <p>
        If the current iteration point is past the removed context there
        is no affect on the iteration.
        </p>
        <p>If the current iteration point is ahead of the removed context then
        again there is no affect on the iteration. (It won't know the context
        was removed.)
        </p>
        <p>If the current iteration point (ie the context to be returned by
        {@link #next}) is the removed context then we have a problem - as the
        context could have been deallocated by the time we try to proceed
        from it to the next context. This must be prohibited, so safe use
        of the iterator must ensure that it never pauses at a context that
        might be removed.
        </p>
        </li>
        </ol>
        <p>Note that for the above properties to hold the iterator must be used
        such that the calls to {@link #hasNext} and {@link #next} are performed
        atomically with respect to insertions and deletions, which themselves
        are performed atomically. And as mentioned, the use of the iterator
        must not allow the context to be returned by <tt>next</tt> to be
        deleted before <tt>next</tt> is called. This is easily done by
        iterating through the list atomically until a non-NHRT context is
        found.
    */
    static class ContextIterator implements Iterator {
        Context ctx;

        ContextIterator() throws PragmaNoBarriers {
            // remember the list is never empty once we have bootstrapped
            // the initial context
            ctx = Context.first;
        }

        public boolean hasNext() {
            return ctx != null;
        }

        // NOTE: we assume use of hasNext() before next(). If you call next()
        // when hasNext would return false you will get NullPointerException.
        public Object next() throws PragmaNoBarriers {
            Context ret = ctx;
            ctx = ret.next;
            return ret;
	}

        public void remove() {
            throw new OVMError.Unimplemented();
        }
    }
	    

    public void beforeMonitorUse() {
        if (flags[MONITOR_RECURSION]) {
            throw Executive.panic("recursive monitor usage");
        }
        flags[MONITOR_RECURSION] = true;
    }

    public void afterMonitorUse() {
        flags[MONITOR_RECURSION] = false;
    }

    /**
     * Get the current Context.
     **/
    public static Context getCurrentContext() {
        return Processor.getCurrentProcessor().getContext();
    }

    /**
     * Initialize this context so that it begins execution in the code 
     * represented by the supplied <tt>InvocationMessage</tt>.
     * @param message the invocation to be executed. This must be bound to a
     * receiver.
     */
    public void initialize(InvocationMessage message) {
	assert message.isBound(): "InvocationMessage not bound?";
	Interpreter.makeActivation(nativeContextHandle_,
				   message.getMethod().getCode(),
				   (InvocationMessage) VM_Address.fromObject(message).asOop() // forwarding
        );
    }

    /**
     * Return an Activation (recycling one that this Context always has in
     * reserve, so this method can't fail on pool exhaustion) corresponding to
     * <em>the user code that called this method</em>.
     * FIXME this implementation assumes more'n it should about how recycling works.
     * @return The Activation so described.
     **/
    public Activation getCurrentActivation() throws PragmaNoInline {
	activation_.setToCurrent(nativeContextHandle_);
	if (this == getCurrentContext())
	    return activation_.caller(activation_);
	return activation_;
        // not me!
    }
    
    /**
     * Factory interface for use in the configuration
     */
    public static abstract class Factory {
        public abstract Context make();
    }

    public static Factory factory() throws PragmaStitchSingleton {
	return (Factory) InvisibleStitcher.singletonFor(Factory.class);
    }

    public void bootPrimordialContext() {
    }
    
    /**
     * Create a native context bound to the given application context. This method
     * is rewritten -- the default behavior is for the hosted situation during
     * bootimage construction.
     * 
     * @param appContext the application context to be bound to this native context. 
     *        An application context is a specialised context class that contains
     *        additional information relevant to the application that is using OVM. 
     *        For example, it may contain a reference to the current thread when 
     *        multi-threading is used.
     * @return a handle to a new native context
     */
    static public int makeNativeContext(Context appContext)
        throws PragmaIRnew_context {
        return 0;
      }

    static public class PragmaIRnew_context extends PragmaTransformCallsiteIR {
        static {
            register(
                PragmaIRnew_context.class.getName(),
                new byte[] {
                    (byte) INVOKE_SYSTEM,
                    (byte) InvokeSystemArguments.NEW_CONTEXT,
                    0 });
        }
    }

    public static void doContextDebug(String when) {
	if (false) {
	    Native.print("Doing context debug when: ");
	    Native.print(when);
	    Native.print("\n");
	    for (Iterator i=Context.iterator();
		 i.hasNext();) {
		((Context)i.next()).funDebugStuff();
	    }
	    Native.print("Done doing context debug.\n");
	}
    }

} // end of Context
