package ovm.core.execution;


import ovm.core.OVMBase;
import ovm.core.domain.Blueprint;
import ovm.core.domain.Domain;
import ovm.core.domain.Oop;
import ovm.core.repository.Constants;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.threads.OVMThread;
import ovm.core.stitcher.InvisibleStitcher;
import ovm.core.stitcher.InvisibleStitcher.PragmaStitchSingleton;
import ovm.services.monitors.Monitor;

/*
 * The member names for this class's vtable struct in structs.h are determined
 * according to JNI conventions, which allow short names when methods are not
 * overloaded but require an unwieldy long form for overloaded methods. Those
 * names have to be used in the interpreter C code, so let's make our lives
 * easy and avoid overloading in this class. -Chapman Flack
 */
 /**
  * <tt>CoreServicesAccess</tt> defines the interface between executing code
  * and the OVM kernel services that implement the execution of that code.
  * There can be many different CSA instances within the VM: one per domain,
  * one per thread etc. The minimum level of granularity is expected to be
  * one per domain - which initially means one for the executive domain (the
  * kernel) and one for the user-domain. The CSA is typicallys accessed in 
  * two ways:
  * <ol>
  * <li>As a series of upcalls from the execution engine to invoke the 
  * semantics of executing a particular byte-code (such as <tt>NEW<tt> or
  * <tt>ATHROW</tt> or <tt>MONITORENTER</tt>).
  * <li>As a series of downcalls from &quot;client&quot; code to VM services.
  * For example, for a user-domain the CSA defines all of the functionality
  * expected by the {@link RuntimeExports} mechanism, by forwarding to the
  * appropriate kernel services (such as the allocator, GC, thread manager
  * etc).
  * </ol>
  * <h3>FIXME</h3><p>It seems to me that the CSA itself should define a bytecode/
  * IRcode specific interface (eg methods that implement NEW, ATHROW, INSTANCEOF etc),
  * and that specialised subclasses should extend with context specific 
  * features. For example, JavaCoreServicesAccess would extend CSA to include
  * the interface required by the RuntimeExports mechanism. I think this
  * separation will help when it comes to having different CSA instances for
  * the kernel and the user-domain. <b>David Holmes/b>
  *
  * @author Vitek, Grothoff
 **/
public abstract class CoreServicesAccess
    extends OVMBase {
    /**
     * This boot method must be invoked first!
     * It's primary job is to initialise the allocator.
     * It's secondary job is to initialize anything that depends on the
     * order of initialization.
     **/
    public abstract void boot();

    /**
     * Allocate a new Oop instance corresponding to the passed in
     * blueprint.  As this is the allocation code it is in error for
     * anything it calls to try and perform an allocation. The most likely
     * culprit is debugging code that uses string concatenation; or a bug
     * that causes an exception to be created. We detect recursive entry of
     * the allocation and terminate the VM if this occurs. 
     * 
     * @param bp The blueprint for which we will create an Oop instance
     * @return the Oop instance so created.  
     */
    public abstract Oop allocateObject(Blueprint.Scalar bp);

    /**
     * Allocate memory for an array.  <p>As this is the allocation code it
     * is in error for anything it calls to try and perform an
     * allocation. The most likely culprit is debugging code that uses
     * string concatenation; or a bug that causes an exception to be
     * created. We detect recursive entry of the allocation and terminate
     * the VM if this occurs.  
     * @param bp the blueprint of the array
     * @param arraylength the length of the array
     * @return the reference to the freshly allocated array
     **/
    public abstract Oop allocateArray(Blueprint.Array bp, int arraylength);

    /**
     * Allocate memory for a multi-dimensional array.  <p>As this is the
     * allocation code it is in error for anything it calls to try and
     * perform an allocation. The most likely culprit is debugging code
     * that uses string concatenation; or a bug that causes an exception to
     * be created. We detect recursive entry of the allocation and
     * terminate the VM if this occurs.  
     * @param bp - blueprint representing the multidimensional array
     * @param arraylengths - the lengths of each subarray as it corresponds
     *                     to the order on the operand stack for MULTIANEWARRAY
     **/
    public abstract Oop allocateMultiArray(Blueprint.Array bp,
					   int pos,
					   int [] arraylengths);


    /**
     * A version of allocateMultiArray without a stack-allocated int
     * array. Instead, it accepts a VM_Address pointing to an int C
     * array. However, the order of elements is reversed. Used by
     * SimpleJIT.
     */
    public abstract Oop _allocateMultiArray(Blueprint.Array bp,
					    VM_Address dimensionArray,
					    int dimensionArrayLength);


    /**
     * Run the initializer if not yet run, otherwise do nothing.
     * Very much subject to change, removal etc.
     * @return the argument (because it's easier to specify the
     *         opcode if it does)
     **/
    public abstract Oop initializeBlueprint(Oop sharedState);

    /**
     * Resolve the type specified at cpIndex in the constant pool of
     * the current method and allocate an object of that type.  FIXME:
     * should quickify the instruction stream (currently not possible
     * in the interpreter since the index changes due to LinkSet / CP
     * index distinction).
     *
     * @param cpIndex the index into the constant pool
     * @return the allocated object
     */
    public abstract Oop resolveNEW(int cpIndex,
				   Constants pool);

    /** A variant of resolveNEW without allocation (for SJ) */
    public abstract void resolveNew(int cpIndex,
				    Constants pool);

    public abstract void resolveClass(int cpIndex,
				      Constants pool);
    public abstract int resolveInstanceField(int cpIndex,
					     Constants pool);
    public abstract int resolveInstanceMethod(int cpIndex,
					      Constants pool);
    public abstract int resolveStaticField(int cpIndex,
					   Constants pool);
    public abstract int resolveStaticMethod(int cpIndex,
					    Constants pool);
    public abstract int resolveInterfaceMethod(int cpIndex,
					       Constants pool);

    /**
     * Resolve the constant at cpIndex in the constant pool of the current
     * method. 
     *
     * @param cpIndex the index into the constant pool
     * @return the resolved object
     */
    public abstract Oop resolveLDC(int cpIndex,
				   Constants pool);

    /** A variant of resolveLDC without returning the constant (for SJ) */
    public abstract void resolveLdc(int cpIndex,
				    Constants pool);

    /**
     * Resolve a String given the UTF8 Index for the current Domain.
     * (used so far only by J2c's lazy String resolution, everyone else
     * uses resolveLDC).
     */
    public abstract Oop resolveUTF8(int utf8Index);
				    

    /**
     * Generates and throws an exception or an error detected by the VM.
     * An error or exception of type 'type' has occured at the current
     * execution point of the VM - typically a null dereference, or a
     * failed array bounds check. A new exception object is created to
     * represent this exception and {@link #processThrowable} is invoked to
     * handle the exception.  
     * <p>This code (or code it invokes) must never
     * throw exceptions nor try to catch exceptions. To detect if this
     * occurs we set up a check for recursive entry into the exception
     * handling code. If this happens then we abort.
     * @param type the type of the exception (see {@link
     * ovm.services.bytecode.JVMConstants.Throwables})
     * @param meta meta-information for the exception (e.g. the index in an
     * index out of bounds exception)
     *
     */
    public abstract void generateThrowable(int type, int meta);

    /**
     * Generates and throws an exception or an error detected by the VM.
     * An error or exception of type 'type' has occured at the current
     * execution point of the VM.
     * A new exception object is created to
     * represent this exception and {@link #processThrowable} is invoked to
     * handle the exception.  
     * <p>This code (or code it invokes) must never
     * throw exceptions nor try to catch exceptions. To detect if this
     * occurs we set up a check for recursive entry into the exception
     * handling code. If this happens then we abort.
     * @param type the type of the exception (see {@link
     * ovm.services.bytecode.JVMConstants.Throwables})
     * @param message a character string representing the message to be
     * placed into the exception object.
     * @deprecated This method was defined for use by native code that throws
     * explicit exceptions, but the native invocation mechanism no longer
     * requires that.
     *
     */
    public abstract void generateThrowableWithMessage(int type,  byte[] message);

    /**
     * Generates and throws an exception or an error detected by the VM.
     * An error or exception of type 'type' has occured at the current
     * execution point of the VM.
     * A new exception object is created to
     * represent this exception and {@link #processThrowable} is invoked to
     * handle the exception.  
     * <p>This code (or code it invokes) must never
     * throw exceptions nor try to catch exceptions. To detect if this
     * occurs we set up a check for recursive entry into the exception
     * handling code. If this happens then we abort.
     * @param type the type of the exception (see {@link
     * ovm.services.bytecode.JVMConstants.Throwables})
     * @param message a character string representing the message to be
     * placed into the exception object.
     */
    public abstract void generateThrowableWithString(int type,  String message);

    /**
     * Process a <code>athrow</code> instruction, or explicitly 
     * &quot;throw&quot; an exception object that may be from a different
     * domain. Finds the appropriate handler and makes the interpreter 
     * continue at that point. A particular execution engine may not need
     * to use this hook.
     * <p><b>Beware:</b> using this to explicitly throw non-executive domain
     * exceptions needs to be done with great care. Such exceptions will not
     * be caught in the executive domain, except by code looking to catch
     * <tt>WildcardExceptions</tt>, and so finally clauses would never execute
     * and monitors may not not be released. In general you either ensure that
     * the next action is a return to the user-domain (such as from 
     * <tt>RuntimeExports</tt>, or you manually insert all needed finally
     * clause code, or you change your API and don't throw the UD exception
     * at that point.
     * 
     * <p>This code (or code it invokes) must never throw exceptions nor try to
     * catch exceptions. To detect if this occurs we set up a check for
     * recursive entry into the exception handling code. If this happens
     * then we abort.  
     * <p>Beware that any debugging code in here should use
     * the most primitive facilities available and avoid things like string
     * concatenation, or toString() invocations that might generate nested
     * exceptions.
     *
     * @param throwable the Throwable that was thrown
     * @return null, doesn't return but one can do throw processThrowable()
     *         to syntactically exit from a method.
     **/
    public abstract Error processThrowable(Oop throwable);

    public abstract Oop translateThrowable(Object throwable);
 
    /**
     * Executed in response to the event manager native code determining
     * that events have occurred that need servicing.
     */
    public abstract void pollingEventHook();


    public abstract void putFieldBarrier(Oop src, int offset, Oop target, int aSrc, int aTgt) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
      s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline;
    
    public abstract void aastoreWriteBarrier(Oop src, int offset, Oop target, int aSrc, int aTgt) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
      s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline;    

    public abstract void aastoreBarrier(Oop src, int index, Oop target, int componentSize, int aSrc, int aTgt) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
      s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline;    

    public abstract void bastoreBarrier(Oop src, int index, byte newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
      s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline;    

    public abstract void castoreBarrier(Oop src, int index, char newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
      s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline;    

    public abstract void dastoreBarrier(Oop src, int index, double newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
      s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline;    

    public abstract void fastoreBarrier(Oop src, int index, float newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
      s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline;    

    public abstract void iastoreBarrier(Oop src, int index, int newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
      s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline;    

    public abstract void sastoreBarrier(Oop src, int index, short newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
      s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline;    

    public abstract void lastoreBarrier(Oop src, int index, long newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
      s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline;    

    public abstract Oop aaloadBarrier(Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
      s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline;    

    public abstract byte baloadBarrier(Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
      s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline;    

    public abstract char caloadBarrier(Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
      s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline;    

    public abstract double daloadBarrier(Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
      s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline;    

    public abstract float faloadBarrier(Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
      s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline;    

    public abstract int ialoadBarrier(Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
      s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline;    

    public abstract long laloadBarrier(Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
      s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline;    

    public abstract short saloadBarrier(Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
      s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline;    

    public abstract int acmpneBarrier(Oop v1, Oop v2, int aV1, int aV2) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
      s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline;    

    public abstract int acmpeqBarrier(Oop v1, Oop v2, int aV1, int aV2) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
      s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline;    
      
    public abstract void readBarrier(Oop src) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers;
    
    public abstract Oop checkingTranslatingReadBarrier(Oop src, int aSrc) 
      throws s3.util.PragmaNoPollcheck, 
        ovm.core.services.memory.PragmaNoBarriers, 
        s3.util.PragmaAssertNoExceptions, 
        s3.util.PragmaAssertNoSafePoints;
        
    public abstract Oop nonCheckingTranslatingReadBarrier(Oop src, int aSrc) 
      throws s3.util.PragmaNoPollcheck, 
        ovm.core.services.memory.PragmaNoBarriers, 
        s3.util.PragmaAssertNoExceptions,
        s3.util.PragmaAssertNoSafePoints;

    /**
     * Performs monitor entry in response to the execution of the
     * <code>monitor_enter</code> bytecode, or entry to a method with the
     * <code>ACC_SYNCHRONIZED</code> bit set.  <p>The interpreter will
     * invoke this method when it encounters the <code>monitor_enter</code>
     * bytecode. This method is then responsible for effecting monitor
     * entry for the current thread, using the appropriate monitor
     * implementation (probably found via the 
     * {@link ovm.core.stitcher.ThreadServiceConfigurator}).
     *
     * @param o the object to perform monitor entry on. This will either be
     * a normal object for synchronized blocks or methods, or the
     * shared-state object for synchronized static methods.
     *
     * @see ovm.services.monitors
     * @see ovm.services.java.JavaMonitor
     * @see ovm.services.java.JavaMonitorMapper
     **/
    public abstract void monitorEnter(Oop o);

    /**
     * <p>The interpreter will invoke this method when it encounters
     * the <code>monitor_exit</code> bytecode, or return (normal or via a
     * throw) from a method with the <code>ACC_SYNCHRONIZED</code> bit set.
     * This method is then responsible
     * for effecting monitor exit for the current thread, using the
     * appropriate monitor implementation (probably found via the
     * {@link ovm.core.stitcher.ThreadServiceConfigurator}).
     *
     *
     * <p><b>NOTE</b>: due to the way that exception tables are generated
     * by some compilers (notably javac 1.4) an infinite loop will occur if
     * monitor exit throws an exception when it is part of a synchronized
     * block. From a language position the monitor <b>must</b> be released
     * before an exception can propagate, but there is no way to detect the
     * situations where the monitor is either not owned by the calling
     * thread, or the exception occurred after the release.  Consequently,
     * any exception that occurs during a monitorexit must be treated as a
     * fatal exception and abort the VM. 
     *
     * @param o the object to perform monitor exit on. This will either be
     * a normal object for synchronized blocks or methods, or the
     * shared-state object for synchronized static methods.
     * @throws Nothing - see above comments
     *
     *
     * @see ovm.services.monitors
     * @see ovm.services.java.JavaMonitor
     * @see ovm.services.java.JavaMonitorMapper
     **/
    public abstract void monitorExit(Oop o);


    /**
     * An empty method for a profiling purpose.
     * @deprecated
     **/
    public void emptyCall() {}
    

    // monitor methods

    /**
     * Return the monitor associated with o, allocating one if needed.
     * This is the single point at which monitors may be
     * allocated.  It is called from monitorEnter, as well as
     * wait/signal operations.  It is not called from monitorExit,
     * since bytecode verification has already ensured that
     * getMonitor() will return non-null.
     **/
    public abstract Monitor ensureMonitor(Oop o);

    /**
     * Return a Monitor object that behaves like o's monitor.
     * This method exists to support sharing between Java Class
     * and shared-state objects.  By default, this method is
     * equivalent to <code>ensureMonitor</code>, but a fast-lock
     * implemenation may choose to return a forwarding object without
     * inflating o's monitor.
     **/
    public Monitor aliasMonitor(Oop o) {
	return ensureMonitor(o);
    }
    

    /**
     * Informs the CSA that the threading system has been initialized to the
     * point where monitor synchronization operations can be carried out.
     */
    public abstract void enableSynchronization();

    //----------- (NOT SO) TEMPORARILY HERE--------------

    public abstract void monitorTransfer(Oop o, OVMThread newOwner);

    public abstract boolean currentThreadOwnsMonitor(Oop o);

    public abstract Oop getMonitorOwner(Oop o);

    public abstract void monitorSignal(Oop o);

    public abstract void monitorSignalAll(Oop o);

    public abstract boolean monitorWaitAbortable(Oop o);

    public abstract int monitorWaitTimedAbortable(Oop o, long timeout);

    public abstract int monitorWaitAbsoluteTimedAbortable(Oop o, long timeout);

    /**
     * Enables the class initialization process.
     * <p>Class initialization is initially disabled to give control
     * over how the user-domain initializes its state. It should be enabled
     * by a call to this method once the user-domain is in an 
     * appropriate state.
     */
    public abstract void enableClassInitialization();

    public abstract Oop clone(Oop obj);

    public abstract Oop /*Class*/ getPrimitiveClass(char tag);   
    public abstract Domain getDomain();


    public static abstract class Factory {
	public abstract CoreServicesAccess make(Domain d);
	public static Factory the() throws PragmaStitchSingleton {
	    return (Factory) InvisibleStitcher.singletonFor(Factory.class);
	}
    }
    
    public abstract void createInterruptHandlerMonitor( Oop o, int interruptIndex );
} // End of CoreServicesAccess



