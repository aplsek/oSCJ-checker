package s3.core.execution;

import ovm.core.Executive;
import ovm.core.domain.Blueprint;
import ovm.core.domain.ConstantResolvedInstanceFieldref;
import ovm.core.domain.ConstantResolvedInstanceMethodref;
import ovm.core.domain.ConstantResolvedStaticFieldref;
import ovm.core.domain.ConstantResolvedStaticMethodref;
import ovm.core.domain.Domain;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.JavaDomain;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.domain.WildcardException;
import ovm.core.execution.Activation;
import ovm.core.execution.Context;
import ovm.core.execution.CoreServicesAccess;
import ovm.core.execution.InvocationMessage;
import ovm.core.execution.Native;
import ovm.core.execution.ReturnMessage;
import ovm.core.repository.Constants;
import ovm.core.repository.JavaNames;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.UTF8Store;
import ovm.core.services.events.EventManager;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Area;
import ovm.core.services.threads.OVMThread;
import ovm.core.services.threads.ThreadManager;
import ovm.core.stitcher.EventServicesFactory;
import ovm.core.stitcher.InterruptServicesFactory;
import ovm.core.stitcher.IOServiceConfigurator;
import ovm.core.stitcher.MonitorServicesFactory;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.ThreadServicesFactory;
import ovm.services.bytecode.JVMConstants;
import ovm.services.events.InterruptMonitor;
import ovm.services.java.JavaMonitor;
import ovm.services.java.realtime.RealtimeJavaMonitor;
import ovm.services.monitors.Monitor;
import ovm.services.monitors.MonitorMapper;
import ovm.services.monitors.TransferableMonitor;
import ovm.util.UnicodeBuffer;
import s3.core.domain.MachineSizes;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3Constants;
import s3.core.domain.S3Domain;
import s3.core.domain.S3Field;
import s3.core.domain.S3Type;
import s3.services.java.ulv1.InterruptHandlerMonitorImpl;
import s3.services.simplejit.dynamic.SimpleJITDynamicJITCompiler;
import s3.services.transactions.EDAbortedException;
import s3.services.transactions.Transaction;
import s3.util.PragmaAtomic;
import s3.util.PragmaNoInline;
import s3.util.PragmaNoPollcheck;

/*
 * The member names for this class's vtable struct in structs.h are determined
 * according to JNI conventions, which allow short names when methods are not
 * overloaded but require an unwieldy long form for overloaded methods. Those
 * names have to be used in the interpreter C code, so let's make our lives
 * easy and avoid overloading in this class. -Chapman Flack
 */
 /**
  * A specific implementation of {@link CoreServicesAccess}. This is currently
  * a hybrid kernel and user-domain CSA, that supports both bytecode operations
  * and a RuntimeExports interface.
  *
  * @author Vitek, Grothoff, Holmes, Palacz
 */
public class S3CoreServicesAccess extends CoreServicesAccess
    implements JVMConstants {


    public static final boolean REFLECTION_DEBUGGING = false;
    
    /** The thread manager in the current configuration */
    static private final ThreadManager threadMan;

    /** The event manager in the current configuration - if any */
    static private final EventManager eventMan;

    /** The monitor factory in the current configuration */
    static private final Monitor.Factory monitorFactory;
    
    static private final InterruptMonitor intMon;

    static {
        monitorFactory = 
            ((MonitorServicesFactory) ThreadServiceConfigurator.config
             .getServiceFactory(MonitorServicesFactory.name))
            .getMonitorFactory();

        ThreadServicesFactory tsf = (ThreadServicesFactory)
            ThreadServiceConfigurator.config.
            getServiceFactory(ThreadServicesFactory.name);
        if (tsf != null) {
            threadMan = tsf.getThreadManager();
        }
        else
            threadMan = null;

        EventServicesFactory esf = 
            (EventServicesFactory)IOServiceConfigurator.config.
            getServiceFactory(EventServicesFactory.name);
        if (esf != null) {
            eventMan = esf.getEventManager();
        }
        else
            eventMan = null;
            
        InterruptServicesFactory isf = (InterruptServicesFactory)
          ThreadServiceConfigurator.config.getServiceFactory(
                InterruptServicesFactory.name);
                
        if (isf != null) {
          intMon = isf.getInterruptMonitor();
        } else {
          intMon = null;
        }
            
    }


    // perhaps a subclass will need a set and get method
    // this should be turned off at the very beginning and then turned
    // on for good. getStackTrace() depends on it
    static private boolean traceOn_ = false;

    // The domain we are the CSA for
    private S3Domain domain;

    // Pre-allocated instance of OOME
    private Oop outOfMemoryError;

    private Oop executiveOutOfMemoryError;

    
    public Domain getDomain() { return domain; }

    static final boolean DEBUG_ENTRY_COUNT = false;

    static public class S3Factory extends Factory {
	public CoreServicesAccess make(Domain d) {
	    return new S3CoreServicesAccess((S3Domain)d);
	}
    }

    protected S3CoreServicesAccess(S3Domain d) { domain = d; }


    public void boot() {
        traceOn_ = true;
	// allocate OOME's in the meta-data memory region
	Object current = MemoryPolicy.the().enterMetaDataArea(domain.getSystemTypeContext()); 
	try {
	    outOfMemoryError = domain.makeThrowable
		(Throwables.OUT_OF_MEMORY_ERROR, null, null);
	}
	finally {
	    MemoryPolicy.the().leave(current);
	}
	Domain ed = DomainDirectory.getExecutiveDomain();
	executiveOutOfMemoryError
	    = (((S3CoreServicesAccess) ed.getCoreServicesAccess())
	       .outOfMemoryError);
    }


    public Oop clone(Oop oop) //throws PragmaAtomic 
    {
	return MemoryManager.the().clone(oop);
    }


    public Oop allocateObject(Blueprint.Scalar bp)
	//throws PragmaAtomic
    {
        return MemoryManager.the().allocate(bp);
    }



    /** atomically allocates the array - the length has already been checked.
     */
    protected Oop allocateArrayInternal(Blueprint.Array bp, int arraylength)
   {//PARBEGIN     throws PragmaAtomic {
	return MemoryManager.the().allocateArray(bp, arraylength);
    }


    public Oop allocateArray(Blueprint.Array bp, int arraylength) {
        if (arraylength < 0) {
	    this.generateThrowable(Throwables.NEGATIVE_ARRAY_SIZE_EXCEPTION, 0);
            return null; // NOT REACHED
        }
        return allocateArrayInternal(bp, arraylength);
    }

    public Oop allocateMultiArray(Blueprint.Array bp,
				  int pos,
				  int[] arraylengths) {

        for (int i = 0; i < arraylengths.length; i++) {
            if (arraylengths[i] < 0) {
                this.generateThrowable(Throwables.NEGATIVE_ARRAY_SIZE_EXCEPTION, 0);
                return null; // NOT REACHED
            }
        }
        return allocateMultiArrayInternal(bp, pos, arraylengths);
    }

    /** atomically allocates the array at pos - the length has already 
        been checked.
     */
    protected Oop allocateMultiArrayInternal(Blueprint.Array bp,
					     int pos,
					     int[] arraylengths)
//PARBEGIN	throws PragmaAtomic
// ?? why not atomic anymore
    {
	int len = arraylengths[pos];
        Oop ret = MemoryManager.the().allocateArray(bp, len);
	Blueprint cbpt = bp.getComponentBlueprint();
        if (pos < arraylengths.length - 1) {
            for (int i = 0; i < len; i++) {
                Oop elem =
                    allocateMultiArrayInternal(cbpt.asArray(), pos + 1, arraylengths);
//		VM_Address contents = bp.addressOfElement(ret, i);
//                contents.setAddress(VM_Address.fromObject(elem));
                    MemoryManager.the().setReferenceArrayElement(ret, i, elem);
            }
        }
        return ret;
    }

    /**
     * A version of allocateMultiArray without a stack-allocated int
     * array. Instead, it accepts a VM_Address pointing to an int C
     * array. However, the order of elements is reversed. Used by
     * SimpleJIT.
     */
    public Oop _allocateMultiArray(Blueprint.Array bp,
				   VM_Address dimensionArray,
				   int dimensionArrayLength) {
	int[] arraylengths = new int[dimensionArrayLength];
	for(int i = 0; i < dimensionArrayLength; i++) {
	    arraylengths[i] = dimensionArray.getInt();
	    dimensionArray = dimensionArray.sub(4);
	}
        return allocateMultiArray(bp, 0, arraylengths);
    }

    /**
     * Resolve a String given the UTF8 Index for the current Domain.
     * (used so far only by J2c's lazy String resolution, everyone else
     * uses resolveLDC).
     */
    public Oop resolveUTF8(int utf8Index) {
	return domain.internString(utf8Index);
    }

    /**
     * Resolve the type and allocate.
     * <p><b>Note:</b> This method may not be used by all execution
     * engines so you can not assume that class initialization has occurred
     * explicitly. The class being instantiated may already be marked as
     * initialized due to having no actual static initialization needs.
     * Consquently, the class mirror may not have been created when we have
     * an instance of the class.
     *
     * @param cpIndex the index into the constant pool
     * @return the allocated object
     */
    public Oop resolveNEW(int cpIndex,
			  Constants pool) {
	S3Constants s3 = (S3Constants)pool;
	try {
	    Blueprint bp = s3.resolveClassAt(cpIndex);
	    // trigger static initialization...
	    if (! domain.isExecutive()) 
		initializeBlueprint(bp.getSharedState());
	    return this.allocateObject(bp.asScalar());
	} catch (LinkageException le) {
	    this.processThrowable(le.toLinkageError(domain));
	    return null; // NOT REACHED!
	}
    }


    /**
     * A variant of resolveNEW without allocation (for SJ).
     */
    public void resolveNew(int cpIndex,
			   Constants pool) {
	S3Constants s3 = (S3Constants)pool;
	try {
	    Blueprint bp = s3.resolveClassAt(cpIndex);
	    bp.ensureSubtypeInfo();
	    // trigger static initialization...
	    if (! domain.isExecutive()) 
		initializeBlueprint(bp.getSharedState());
	} catch (LinkageException le) {
	    this.processThrowable(le.toLinkageError(domain));
	}
    }

    /**
     * Is this called for .class/shared-state LDCs?  If so, it should
     * go through initializeBlueprint.
     **/
    public void resolveClass(int cpIndex,
			     Constants pool) {
	S3Constants s3 = (S3Constants)pool;
	try {
	    s3.resolveClassAt(cpIndex);
	} catch (LinkageException le) {
	    this.processThrowable(le.toLinkageError(domain));
	}
    }

    /**
     * Resolve the string (or shared state) at the given CP index.  
     * For OVMIR, this triggers either
     * - string resolution of the CP entry and replacement of LDC
     *   with LDC_REF_QUICK; the String is returned
     * - string already resolved, just replace LDC with LDC_REF_QUICK
     *   and return the string
     * - shared-state resolution (which may trigger static initializers!),
     *   replacement of LDC with LDC_REF_QUICK and return of the SharedState
     * - shared-state is resolved, just replace LDC with LDC_REF_QUICK and
     *   return the shared state
     * - binder (again, with/without resolution)
     *
     * For Java bytecode (not OVMIR'ified), it can also happen that the
     * constant entry is a primitive. Then:
     * - constant is a non-wide primitive, so replace LDC with
     *   LDC_QUICK and go crazy trying to pass the value properly typed
     *   onto the interpreter stack (that's why this should not happen...)
     *
     * Resolve the constant at cpIndex in the constant pool of the current
     * method.  FIXME: we may want to make our life easier by ALSO passing
     * the S3Constants instance of which the constant needs to be resolved.
     * Sadly, the current Specification does not allow us to do that.
     * To be resolved later...
     *
     * We may also want to implement patching of the bytecode stream to
     * LDC_REF_QUICK at some point (can be done either here or by the
     * interpreter).  Oh, and run static initializers when resolving a
     * shared state...
     *
     * @param cpIndex the index into the constant pool
     * @return the resolved object
     */
    public Oop resolveLDC(int cpIndex,
			  Constants pool) {
	S3Constants s3 = (S3Constants)pool;
	return s3.resolveConstantAt(cpIndex);
    }

    /**
     * A variant of resolveLDC without returning the constant (for SJ).
     */
    public void resolveLdc(int cpIndex,
			   Constants pool) {
	S3Constants s3 = (S3Constants)pool;
	s3.resolveConstantAt(cpIndex);
    }

    public int resolveInstanceField(int cpIndex,
				    Constants pool) {
	try {
	    S3Constants s3 = (S3Constants)pool;
	    ConstantResolvedInstanceFieldref ifi =
		s3.resolveInstanceField(cpIndex);
	    S3Field field = (S3Field)ifi.getField();
	    return field.getOffset();
	} catch (LinkageException e) {
	    processThrowable(e.toLinkageError(domain));
	    return -1;
	}
    }
    public int resolveInstanceMethod(int cpIndex, Constants pool) {
        try {
            S3Constants s3 = (S3Constants) pool;
            ConstantResolvedInstanceMethodref vmi = s3.resolveInstanceMethod(cpIndex);
            return vmi.getOffset();
        } catch (LinkageException e) {
	    processThrowable(e.toLinkageError(domain));
	    return -1;
        }
    }
    public int resolveStaticField(int cpIndex, Constants pool) {
        try {
            S3Constants s3 = (S3Constants) pool;
            ConstantResolvedStaticFieldref sfi = s3.resolveStaticField(cpIndex);
	    if (!domain.isExecutive())
		initializeBlueprint(sfi.getSharedState());
            S3Field field = (S3Field) sfi.getField();
            return field.getOffset();
        } catch (LinkageException e) {
	    processThrowable(e.toLinkageError(domain));
	    return -1;
        }
    }
    public int resolveStaticMethod(int cpIndex, Constants pool) {
        try {
            S3Constants s3 = (S3Constants) pool;
            ConstantResolvedStaticMethodref smi = s3.resolveStaticMethod(cpIndex);
	    if (!domain.isExecutive())
		initializeBlueprint(smi.getSharedState());
            return smi.getOffset();
        } catch (LinkageException e) {
	    processThrowable(e.toLinkageError(domain));
	    return -1;
        }
    }

    public int resolveInterfaceMethod(int cpIndex,
				      Constants pool) {
	try {
	    S3Constants s3 = (S3Constants)pool;
	    return s3.resolveInterfaceMethod(cpIndex).getOffset();
	} catch (LinkageException e) {
	    processThrowable(e.toLinkageError(domain));
	    return -1;
	}
    }


    /** 
     * Flag controlling whether class initialization should take place.
     * Initially <tt>false</tt> this should be set via a call to
     * {@link #enableClassInitialization} once the user-domain has reached
     * a state where that is appriopriate.
     */
    private boolean classInitializationEnabled = false; 

    public void enableClassInitialization() {
	// d("class initialization enabled");
        classInitializationEnabled = true;
	// ovm.core.execution.Interpreter.startTracing(true);
    }

    static final boolean DEBUG_INIT = false;
    static String[] indents;

    // avoid logger for debugging output
    public static void pln(String msg) {
        BasicIO.out.println(msg);
    }

    static {
        if (DEBUG_INIT) {
            indents = new String[] { 
                "",
                " ",
                "  ",
                "   ",
                "    ",
                "     ",
                "      ",
                "       ",
                "        ",
                "         ",
                "          ",
            };
        }
    }

    int initLevel = -1; // used for debugging

    /**
     * Implements the class initialization protocol as defined in the JVMS
     * section 2.17.5.
     * <p>This method is called in response to a NEW, INVOKESTATIC, PUTSTATIC
     * or GETSTATIC for a given class. It is also called in response to a
     * reflective request for the <tt>Class</tt> object of a given class.
     *
     * <p>Class initialization involves two parts:
     * <ul>
     * <li>Creating the <tt>Class</tt> instance (known as the class mirror)
     * for each type; and</li>
     * <li>Performing the actual class initialization as required by the JVMS
     * </li>
     * </ul>
     * <p>The first part can be done at any time, independently of the second
     * part. This means that we don't need to take an special steps during
     * domain startup to ensure class mirrors can be created.
     * <p>The second part requires special care during bootstrap because
     * can't execute arbitrary static initializers until the core user-domain
     * class have been initialized. This predominantly means initialization of
     * the threading subsystem.
     * <p>To solve the problem of initializing the threading system
     * without initializing most of the classes in the core libraries, the
     * system starts with class initialization disabled. This will still
     * create the Class objects as needed, but will not process the super 
     * class nor invoke the <tt>clinit</tt> method. Once the minimal
     * initialization has been carried out class initialization can be enabled
     * using {@link #enableClassInitialization}. The code that runs while
     * class initialization is disabled must not rely on any form of static
     * initialization either directly or indirectly. This can be achieved by
     * careful control over the code that gets executed, but it should be noted
     * that generating an exception during this time will lead to
     * recursive failures that will crash the VM. Also note that until
     * class initialization is enabled the startup code can not access any
     * string literals, as they require interning which in turn relies on
     * static initialization which is disabled.
     *
     * <p><b>Implementation Notes</b>
     * <p>This method is invoked in three ways:
     * <ul>
     * <li>As a side-effect of constant pool resolution (this takes care of
     * static accesses)
     * </li>
     * <li>As a side-effect of <tt>NEW</tt> resolution, (this takes care of
     * object construction); and
     * </li>
     * <li>Explicitly within reflective invocation sequences that
     * otherwise avoid the above initialization points.
     * <p>Reflective invocations that commence in the user-domain do not 
     * usually need this explicit call because such invocation must start 
     * with either the <tt>Class</tt> object, or an instance of a class. 
     * In both cases initialization must have already occurred. 
     * The exceptions to this are the call that retrieves the <tt>Class</tt> 
     * object itself, and invocations of <tt>getType()</tt> on a
     * <tt>java.lang.reflect.Field</tt> object, or <tt>getReturnType</tt>
     * or <tt>getExceptionTypes</tt> on a <tt>java.lang.reflect.Field</tt> 
     * object - so such calls trigger class initialization (in these latter 
     * cases it is unclear from the JVM spec or API docs whether initialization
     * must occur, but we perform it anyway - the JDK does not seem to do this,
     * so we need to check.
     * <p>
     * <b>Note:</b> <tt>Class.forName</tt> can be asked for a Class object
     * for a class that is NOT initialized. This means that subsequent use of
     * that class object can't assume that the class is initialized and so
     * reflective invocations using the class object may need to trigger
     * class initialization.
     * <p>This leaves reflective invocations initiated in the executive-domain.
     * There appear to be four such execution paths:
     * <ul>
     * <li>Reflective execution of the Launcher itself by the UserDomain. 
     * </li>
     * <li>Reflective construction of exception objects via 
     * {@link ovm.core.domain.JavaDomain#makeThrowable JavaDomain.makeThrowable}. 
     * </li>
     * <li>Reflective construction of stack trace element arrays
     * {@link s3.core.domain.S3Domain#makeStackTraceElement 
     * S3Domain.makeStackTraceElement} 
     * <li>Reflective construction of user-domain strings from executive
     * domain strings, using 
     * {@link ovm.core.domain.Domain#makeString Domain.makeString} 
     * </li>
     * </ul>
     * <p>In the first case we deal with this in 
     * <tt>S3JavaUserDomain.runReflectively</tt>. The other cases have to
     * construct and then <tt>instantiate</tt> an 
     * {@link ovm.core.execution.InstantiationMessage InstantiationMessage}.
     * By placing the initialization call in <tt>instantiate</tt> we deal with
     * the above cases, and also deal with the un-ininitialized 
     * <tt>Class</tt> object case (because before a reflective invocation can
     * be performed a <tt>Constructor</tt> or <tt>Method</tt> object has to
     * be obtained). To speed up the common reflective case where the class
     * is already initialized, we check to see if the class has been 
     * initialized - this is a simple query on the <tt>Type</tt> object that
     * we already have a reference to.
     * </li>
     * </ul>
     *<p>Depending on the execution engine being used, once initialization has
     * taken place it may be possible to dynamically rewrite the instruction
     * that caused the initialization check, such that the check is not
     * performed again.
     * <p><b>Note:</b> An execution engine may optimize things such that
     * types for which there is no <tt>clinit</tt> in the execution hierarchy
     * are marked as initialized upon creation. Such types will not have a
     * corresponding class mirror, so the reflection methods that need to
     * acquire the class mirror can not assume that it exists, but must ensure
     * it is created.
     *
     * @param sharedState the shared-state object of the type to be initialized
     *
     * @return the sharedState object (simplifies the specification)
     */
    public Oop initializeBlueprint(Oop sharedState) {
        Blueprint shstBP = sharedState.getBlueprint();
        Blueprint instBP = shstBP.getInstanceBlueprint();
	S3Type instType = (S3Type) instBP.getType();

        // used for debugging only
        String typeName = null;
        if (DEBUG_INIT) 
            typeName = instType.getUnrefinedName().toString();

        if (DEBUG_INIT) initLevel++;
        try {
            // We want a fast exit path that avoids synchronization if the class
            // has already been initialized. Although this might appear to
            // violate the spec it is safe and correct as long as the state field
            // has volatile semantics (as per JSR-133) and the state is set as
            // the very last action before releasing the lock on the Class object.
            //
            // This fast-path is also required to deal with initialization of
            // class Class otherwise we have an infinite loop.
            //
            // - David Holmes
            
            Type.State state = instType.getLifecycleState();
            
            if (state == Type.State.INITIALIZED) {
                if (DEBUG_INIT) 
                    pln(indents[initLevel] + "Skipping initialized class on entry " 
                        + typeName);
                return sharedState;
            }
            
            // logically this is where we define the Class object and perform
            // class initialization. However, creating the Class object incurs
            // a bit of overhead so we defer actual creation
            // of the Class object until it is asked for. The sharedState 
            // object and the Class object will share the same monitor when the
            // Class object is created, so this protocol is correct per the
            // JVMS. To make things a little clearer we rename the shared-state
            // object and just refer to it as the class object.
            Oop clazz = sharedState; 
            
            // Now lock the Class object and see what state it is in
            OVMThread current = threadMan.getCurrentThread();
            if (DEBUG_INIT) pln(indents[initLevel] + current + " acquiring lock for class " + typeName);
            monitorEnter(sharedState);
            try {
                // if initialization is in progress by another thread then wait
                state = instType.getLifecycleState();
                if (state == Type.State.INITIALIZING) {
                    if (instType.setInitializerThread(current) == current) {
                        // we are recursively initializing so we can just
                        // return immediately.
                        if (DEBUG_INIT) pln(indents[initLevel] + "Recursive initialization of " + typeName);        
                        return sharedState;
                    }
                    else {
                        do {
                            if (DEBUG_INIT) pln(indents[initLevel] + "Thread " + current + " waiting for in-progress initialization of " + typeName);        
                            // FIXME: What should happen if the thread is interrupted
                            // while waiting? We can't throw InterruptedException.
                            // Should we throw a general error? Or just ignore
                            // the interrupt? The JVMS doesn't consider this.
                            if (!monitorWaitAbortable(clazz)) {
                                pln("Warning: thread interruption during class initialization is being ignored");
                            }
                        } while((state = instType.getLifecycleState()) == 
                                Type.State.INITIALIZING);
                        
                        // assert: state == Type.State.INITIALIZED ||
                        //         state == Type.State.ERRONEOUS
                    }
                }
                
                if (state == Type.State.INITIALIZED) {
                    if (DEBUG_INIT) pln(indents[initLevel] + "Skipping initialized class after locking and/or waiting: " + typeName);
                    return sharedState;
                }
                else if (state == Type.State.ERRONEOUS) {
                    if (DEBUG_INIT) pln(indents[initLevel] + "Found erroneous initialization state for " + typeName);
		    Oop messageOop =  domain.makeString
			(UnicodeBuffer.factory().wrap
			 ("Erroneous class initialization state: " +
			  instType.getUnrefinedName()));
		    Oop exc = 
			domain.makeThrowable(Throwables.NO_CLASS_DEF_FOUND_ERROR,
					     messageOop,
					     null);
		    throw new WildcardException(exc);
                } else {
                    // assert: state == Type.State.PREPARED
                    
                    // check if initialization is enabled. It won't be during
                    // the user-domain (aka JVM) startup phase.
                    // By doing the test down here we only add the cost of the
                    // check to the slowest path.
                    if (!classInitializationEnabled) {
                        if (DEBUG_INIT) pln(indents[initLevel] + "Class initialization disabled when processing " 
                                            + typeName);        
                        return sharedState;
                    }
                    
                    instType.setLifecycleState(Type.State.INITIALIZING);
                    // set current thread as initialization thread
                    if (instType.setInitializerThread(current) != current) {
                        //                    pln(indents[initLevel] + "Error: prepared class had initializer thread " 
                        //                        + typeName);        
			Oop msgOop = domain.makeString
			    (UnicodeBuffer.factory().wrap
			     ("Prepared class had initializer set"));
			Oop exc = 
			    domain.makeThrowable(Throwables.VIRTUAL_MACHINE_ERROR,
						 msgOop,
						 null);
			throw new WildcardException(exc);
                        // NOT REACHED
                    }
                    else {
                        if (DEBUG_INIT) pln(indents[initLevel] + "Continuing initialization of " + typeName);        
                        // continue once lock released
                    }
                }
            }
            finally {
                if (DEBUG_INIT) pln(indents[initLevel] + current + " releasing lock for class " + typeName);
                monitorExit(clazz);
            }
            
            // Initialize the superclass
            
            Type superClass = instType.getSuperclass();
            if (superClass != null) {
                if (DEBUG_INIT) pln(indents[initLevel] + "About to initialize super class of: " + typeName);
                try {
                    initializeBlueprint(domain.blueprintFor(superClass).getSharedState());
                    if (DEBUG_INIT) pln(indents[initLevel] + "Completed initialization of super class of: " + typeName);
                } catch (WildcardException e) { // catch-all
                    if (DEBUG_INIT) {
                        pln(indents[initLevel] + "Exception initializing superclass of: " + typeName);
                        pln(indents[initLevel] + current + " acquiring lock for class " + typeName);
                    }
                    // need to re-lock clazz, notify all waiters,
                    // mark as erroneous, release lock and re-throw ex - in that order
                    monitorEnter(sharedState);
                    try {
                        monitorSignalAll(sharedState);
                        instType.setLifecycleState(Type.State.ERRONEOUS); 
                    }
                    finally {
                        if (DEBUG_INIT) pln(indents[initLevel] + current + " releasing lock for class " + typeName);
                        monitorExit(clazz);
                    }
                    throw e;
                }
            } else {
                // pln("No super class found for: " + typeName);
            }
            
            // Now execute the <clinit> method if any
            Selector.Method bsel 
                = Selector.Method.make(JavaNames.CLINIT, 
				       shstBP.getName().asCompound());
            ReturnMessage clinitRet = null;
            Oop exception = null;  // if this is non-null then clinitRet is non-null

            Method clinit = shstBP.asScalar().getType().asScalar().getMethod(bsel.getUnboundSelector());
            if (clinit != null) {
                Type.Context loader = shstBP.getType().getContext();
                Object r1 = MemoryPolicy.the().enterClinitArea(loader);
		try {
		    // Avoid allocating an arbitrary number of
		    // InvocationMessages in a scope.  Allocate them
		    // in immortal space instead.  The number of
		    // InvocationMessages we leak will be linear with
		    // the number of classes
		    InvocationMessage msg = new InvocationMessage(clinit);
		    if (DEBUG_INIT) 
			pln(indents[initLevel]
			    + "Started <clinit> for " + typeName);

                    if (REFLECTION_DEBUGGING) {
                      Native.print_string("Instantiation message created in initializeBlueprint, if system crashes, try_to_add_method: ");
                      Native.print_string(bsel.getDefiningClass().toString()+" "+bsel.getUnboundSelector().toString()+"\n");
                    }
                    clinitRet = msg.invoke(sharedState);
                    exception = clinitRet.getException();
                    if (DEBUG_INIT && exception != null)
			pln(indents[initLevel] +
			    "Exception during <clinit> of " + typeName);
                }
		finally { MemoryPolicy.the().leave(r1);	}
                if (DEBUG_INIT)
		    pln(indents[initLevel]
			+ "Completed <clinit> for " + typeName);
            } else {
                if (DEBUG_INIT)
		    pln(indents[initLevel]
			+ "No <clinit> found for: " + typeName);
            }
	    
            
            // Finally re-lock the clazz, wake up any waiters, set the class
            // state appropriately and either return or throw as needed
            if (DEBUG_INIT) pln(indents[initLevel] + current + " acquiring lock for class " + typeName);
            monitorEnter(sharedState);
            try {
                monitorSignalAll(sharedState);
                if (exception == null) {
                    instType.setLifecycleState(Type.State.INITIALIZED);
                    if (DEBUG_INIT) pln(indents[initLevel] + "finished initializer " + typeName);
                    return sharedState;
                }
                
                if (DEBUG_INIT) pln(indents[initLevel] + "doing exception exit from initializeBlueprint " + typeName);
                instType.setLifecycleState(Type.State.ERRONEOUS);
                Type extype = exception.getBlueprint().getType();
		// FIXME: Type.isSubtypeOf
                if (extype.isSubtypeOf(domain.commonTypes().java_lang_Error)) {
                    clinitRet.rethrowWildcard();
                    // NOT REACHED
                    return null;
                } else {
		    Oop eiie = domain.makeThrowable(Throwables
						    .EXCEPTION_IN_INITIALIZER_ERROR,
						    null, exception);
		    throw new WildcardException(eiie);
                }
            }
            finally {
                if (DEBUG_INIT) pln(indents[initLevel] + current + " releasing lock for class " + typeName);
                monitorExit(sharedState);
            }
        }
        finally {
            if (DEBUG_INIT) initLevel--;
        }
    }
    

    public String toString() {
        return domain + " CSA instance";
    }

    /** Flag to control debug printing for exception processing */
    // uncomment static final if you want to produce "optimised" code
    private static final boolean DEBUG_THROW = false;

    /** Flag to control debug printing for exception generation */
    // uncomment static final if you want to produce "optimised" code
    private static final boolean DEBUG_GEN = false;


    /** {@inheritDoc}
     * <p><b>WARNING</b>: programmatic use of this method must be aware
     * that the exception generated may be a user-domain exception, while this
     * is executive-domain code. When that occurs the UD exception will not be
     * seen by the ED code, it will not be caught and finally clauses (either
     * explicit or implicit via Pragma's) wil not be executed. Such methods
     * should appear as top-level methods invoked directly by the VM, or via
     * simple forwarding from RuntimeExports.
     */
    public void generateThrowable(int type, int meta)
	throws PragmaNoInline
    {
        generateThrowableInternal(type, meta, null);
    }

    /** {@inheritDoc}
     * <p><b>WARNING</b>: programmatic use of this method must be aware
     * that the exception generated may be a user-domain exception, while this
     * is executive-domain code. When that occurs the UD exception will not be
     * seen by the ED code, it will not be caught and finally clauses (either
     * explicit or implicit via Pragma's) wil not be executed. Such methods
     * should appear as top-level methods invoked directly by the VM, or via
     * simple forwarding from RuntimeExports.
     */
    public void generateThrowableWithMessage(int type,  byte[] message) 
        throws PragmaNoInline
    {
        generateThrowableInternal(type, 0, message == null ? null : new String(message));
    }

    /** {@inheritDoc}
     * <p><b>WARNING</b>: programmatic use of this method must be aware
     * that the exception generated may be a user-domain exception, while this
     * is executive-domain code. When that occurs the UD exception will not be
     * seen by the ED code, it will not be caught and finally clauses (either
     * explicit or implicit via Pragma's) wil not be executed. Such methods
     * should appear as top-level methods invoked directly by the VM, or via
     * simple forwarding from RuntimeExports.
     */
    public void generateThrowableWithString(int type,  String message) 
        throws PragmaNoInline
    {
        generateThrowableInternal(type, 0, message);
    }


    // NOTE: the stack depth must be the same however this gets called,
    // so it must never be called directly. - DH
    private void generateThrowableInternal(int type, int meta, String message) 
        throws PragmaNoInline
    {
        //FIXME: we have no way to know whether meta is 'valid' and if it
        //       should be passed through to the exception constructor.

        if (DEBUG_GEN) {
            // no allocation used here
            Native.print_string("DEBUG: generateThrowable with type: ");
            Native.print_int(type);
            Native.print_string("\n");
        }

        Context ctx = Context.getCurrentContext();

	// We want to throw OutOfMemoryError as soon as possible to
	// avoid allocation. We also avoid the generation recursion check
        // as it is quite likely that we could get an OOME while trying to
        // generate another exception
	if (type == Throwables.OUT_OF_MEMORY_ERROR) {
            if (DEBUG_GEN) {
                Native.print_string("DEBUG: memory exhaustion - generating OOME\n");
            }
	    processThrowable(outOfMemoryError);
	}


	if (domain.isExecutive() && type != Throwables.STACK_OVERFLOW_ERROR) {
	    int nameIndex = JavaNames.throwables[type].getShortNameIndex();
	    Executive.panic("internal " + UTF8Store._.getUtf8(nameIndex));
	}

        // watch for exceptions within the exception code NOTE: we avoid
        // anything that might itself cause an exception - like toString or
        // string concatenation (other than concatenation of constants of
        // course)
        if (ctx.flags[Context.EXCEPTION_GENERATION_RECURSION]) {
	    // this is all debugging code on the way to dying
	    MemoryManager.the().enableAllDebug();
	    MemoryPolicy.the().enterExceptionSafeArea();
	    Native.print_string("EXCEPTION_GENERATION_RECURSION -- dying\n");
	    // check type is known
	    if (type >= 0 && type < JavaNames.throwables.length) {
		BasicIO.out.println(JavaNames.throwables[type].toString());
	    }
	    else {
		BasicIO.out.print("Unknown exception: ");
		BasicIO.out.println(type);
	    }
            throw Executive.panic("generateThrowable: "
				  + "Generating secondary exception within "
				  + "exception generation code");
        }

	
        ctx.flags[Context.EXCEPTION_GENERATION_RECURSION] = true;

        if (DEBUG_GEN) {
            Object ma = MemoryPolicy.the().enterExceptionSafeArea();
            try {
                Activation act = ctx.getCurrentActivation();
                act = act.caller(act); // get public generateThrowable
                act = act.caller(act); // get real caller
                BasicIO.out.println("Calling method was " + 
                                    act.getCode().getMethod() + " in " +
                                    act.getCode().getMethod().getDeclaringType() );
                
                BasicIO.out.println("Domain is " + domain);
            }
            finally {
                MemoryPolicy.the().leave(ma);
            }
        }

        // set this now so fillInStackTrace knows
	ctx.flags[Context.PROCESSING_GENERATED_EXCEPTION] = true;

        Oop exception;

        if (message == null) message = "";
	// check type is in range
	if (type >= 0 && type < Throwables.N_THROWABLES) {
	    exception = domain.makeThrowable
		(type,
		 domain.makeString(UnicodeBuffer.factory().wrap(message)), 
		 null);
	} else {
	    BasicIO.out.print("unsupported exception type: ");
	    BasicIO.out.println(type);
	    // use the last array entry for type
	    exception = 
		domain.makeThrowable
		(Throwables.THROWABLE,
		 domain.makeString(UnicodeBuffer.factory().wrap
				   ("unsupported exception type")), 
		 null);
	}
        ctx.flags[Context.EXCEPTION_GENERATION_RECURSION] = false;
        throw processThrowable(exception); // never returns
    }

    public Oop makeThrowable(int code, Oop message, Oop throwableCause){
        return domain.makeThrowable(code, message, throwableCause);
    } 

    /**
     * Provides a translation from executive-domain exceptions into user-domain
     * exceptions, if such a translation exists. 
     * The translations supported are:
     * <dl>
     * <dt>WilcardException</dt><dd>The original UD Throwable</dd>
     * <dt>OutOfMemoryError</dt><dd>UD OutOfMemoryError</dd>
     * <dt>EDAbortedException</dt><dd>The AbortedExceptiont thrown by a PAR in the ED</dd>
     * <dt>StackOverflowError</dt><dd>UD StackOverflowError (NOT IMPLEMENTED YET)</dd>
     * </dl>
     * <p>If there is no translation then we panic as it means an ED exception
     * is trying to cross into the UD which signifies a fatal error in the ED.
     * Once we consider ourselves out of development mode we will instead turn
     * the unexpected exception into a VMError.
     * <p><b>Note:</b> This should only ever be called on a user-domains CSA
     * instance.
     * 
     * PARBEGIN It takes an object because EDAbortedException is not a subclass of 
     * throwable at runtime PAREND
     *
     * @param throwable The ED Throwable object
     * @return the translated UD Throwable object
     */
    public Oop translateThrowable(Object throwable) {
        if (throwable == executiveOutOfMemoryError) {
            assert(outOfMemoryError != executiveOutOfMemoryError);
            if (DEBUG_THROW) {
                Native.print_string("DEBUG: Translating ED OOME to UD OOME\n");
            }
            return outOfMemoryError;
	} else if (throwable instanceof WildcardException) {
            if (DEBUG_THROW) {
                Object ma = MemoryPolicy.the().enterExceptionSafeArea();
                try {
                    BasicIO.out.print("DEBUG: Translating a WildCardException: original throwable is - ");
                    BasicIO.out.println(((WildcardException)throwable).getOriginalThrowable().getBlueprint().getType().getUnrefinedName());
                }
                finally {
                    MemoryPolicy.the().leave(ma);
                }
            }
	    Oop ret = ((WildcardException)throwable).getUserThrowable();
	    if (ret != null) {
                return ret;
	    }
	    else { 
                return translateThrowable(((WildcardException)throwable).getExecutiveThrowable());
            }
	} else if (throwable instanceof StackOverflowError) {
            if (DEBUG_THROW) {
                Native.print_string("DEBUG: Translating StackOverflowError\n");
            }
            return domain.makeThrowable(Throwables.STACK_OVERFLOW_ERROR,
                                        domain.makeString(UnicodeBuffer.factory().wrap("")), 
                                        null);

        } else if (throwable == Transaction.the().getEDA()) { // PARBEGIN PAREND
            if (DEBUG_THROW) {
        	Native.print_string("DEBUG: Translating EDAbortedException to AbortedException\n");
            }
            return (Oop) Transaction.the().getUDException();
        }
        else { // no translation so panic
            if (DEBUG_THROW) {
                Native.print_string("DEBUG: aborting - executive domain, exception passing into user-domain\n");
            }
            // try to tell the user what's gone wrong
	    throw Executive.panicOnException((Throwable) throwable, "DOMAIN BOUNDARY ERROR");
        }
    }

//     public Oop translateThrowable(Throwable throwable) {
//         if (throwable == executiveOutOfMemoryError) {
//             assert(outOfMemoryError != executiveOutOfMemoryError);
//             if (DEBUG_THROW) {
//                 Native.print_string("DEBUG: Translating ED OOME to UD OOME\n");
//             }
//             return outOfMemoryError;
// 	} else if (throwable instanceof WildcardException) {
//             if (DEBUG_THROW) {
//                 Object ma = MemoryPolicy.the().enterExceptionSafeArea();
//                 try {
//                     BasicIO.out.print("DEBUG: Translating a WildCardException: original throwable is - ");
//                     BasicIO.out.println(((WildcardException)throwable).getOriginalThrowable().getBlueprint().getType().getUnrefinedName());
//                 }
//                 finally {
//                     MemoryPolicy.the().leave(ma);
//                 }
//             }
// 	    Oop ret = ((WildcardException)throwable).getUserThrowable();
// 	    if (ret != null) {
//                 return ret;
// 	    }
// 	    else { 
//                 return translateThrowable(((WildcardException)throwable).getExecutiveThrowable());
//             }
//         } else { // turn it into a UD version
//             return ((s3.core.domain.S3JavaUserDomain)domain).morphThrowable(throwable);
//         }
//     }

    /**
     * Raise an exception by forwarding to the current activation.
     * CSA.processThrowable(), rather than
     * Activation.processThrowable(), should be used to start
     * exception processing, because the compiler is free to special
     * case this method.
     **/
    public Error processThrowable(Oop throwable) throws PragmaNoInline {
        if (DEBUG_THROW) {
            Object ma = MemoryPolicy.the().enterExceptionSafeArea();
            try {
                // show if this is the ED or UD CSA
                // warning: it seems that in some circumstances the implicit toString
                // and other indirect string concatenation causes the scratchpad to become exhausted
                BasicIO.out.println("DEBUG: Entered processThrowable for: " + this);
            }
            finally {
                MemoryPolicy.the().leave(ma);
            }
        }
	Activation act = Context.getCurrentContext().getCurrentActivation();
	act.processThrowable(throwable);
	return Executive.panic("processThrowable returns");
    }

    // gotcha: it may be that the user domain will call the executive domain
    // pollingEventHook().  that's fine, since this method works exactly the
    // same way for both UD and ED.
    public void pollingEventHook()
        throws s3.util.PragmaAtomic
    {
        // this code almost makes J2c blow.  Problem is that the abstract interpreter
        // and the OvmIR are not aware of the true stack ins/outs of INVOKE_SYSTEM
        // instructions.  So, when I previously had methods for disabling and enabling
        // tracing that used PUSH and POP, J2c's build would break here.  Now it
        // works because beginOverrideTracing() and endOverrideTracing() do not
        // take in or return anything.  If you ever change them so that they do,
        // you'll have issues in J2c to deal with... -FP
        ovm.core.execution.Interpreter.beginOverrideTracing();
        try {
            eventMan.processEvents();
        } catch (NullPointerException npe) {
            // no eventManager configured 
        } finally {
            ovm.core.execution.Interpreter.endOverrideTracing();
        }
    }
    
    public void putFieldBarrier(Oop src, int offset, Oop tgt, int aSrc, int aTgt) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
      s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
	MemoryManager.the().putFieldBarrier(this, src, offset, tgt, aSrc, aTgt);
    }
    
    public void aastoreWriteBarrier(Oop src, int offset, Oop tgt, int aSrc, int aTgt) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
	MemoryManager.the().aastoreBarrier(this, src, offset, tgt, aSrc, aTgt);
    }

    public void aastoreBarrier(Oop src, int index, Oop tgt, int componentSize, int aSrc, int aTgt) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
	MemoryManager.the().aastoreBarrier(this, src, index, tgt, componentSize, aSrc, aTgt);
    }

    public void bastoreBarrier(Oop src, int index, byte newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
	MemoryManager.the().bastoreBarrier(this, src, index, newValue, componentSize, aSrc);
    }

    public void castoreBarrier(Oop src, int index, char newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
	MemoryManager.the().castoreBarrier(this, src, index, newValue, componentSize, aSrc);
    }

    public void dastoreBarrier(Oop src, int index, double newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
	MemoryManager.the().dastoreBarrier(this, src, index, newValue, componentSize, aSrc);
    }

    public void fastoreBarrier(Oop src, int index, float newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
	MemoryManager.the().fastoreBarrier(this, src, index, newValue, componentSize, aSrc);
    }

    public void iastoreBarrier(Oop src, int index, int newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
	MemoryManager.the().iastoreBarrier(this, src, index, newValue, componentSize, aSrc);
    }

    public void lastoreBarrier(Oop src, int index, long newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
	MemoryManager.the().lastoreBarrier(this, src, index, newValue, componentSize, aSrc);
    }

    public void sastoreBarrier(Oop src, int index, short newValue, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
	MemoryManager.the().sastoreBarrier(this, src, index, newValue, componentSize, aSrc);
    }

    public Oop aaloadBarrier(Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
	return MemoryManager.the().aaloadBarrier(this, src, index, componentSize, aSrc);
    }

    public byte baloadBarrier(Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
	return MemoryManager.the().baloadBarrier(this, src, index, componentSize, aSrc);
    }

    public char caloadBarrier(Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
	return MemoryManager.the().caloadBarrier(this, src, index, componentSize, aSrc);
    }

    public double daloadBarrier(Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
	return MemoryManager.the().daloadBarrier(this, src, index, componentSize, aSrc);
    }

    public float faloadBarrier(Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
	return MemoryManager.the().faloadBarrier(this, src, index, componentSize, aSrc);
    }

    public int ialoadBarrier(Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
	return MemoryManager.the().ialoadBarrier(this, src, index, componentSize, aSrc);
    }

    public long laloadBarrier(Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
	return MemoryManager.the().laloadBarrier(this, src, index, componentSize, aSrc);
    }
    
    public short saloadBarrier(Oop src, int index, int componentSize, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
	return MemoryManager.the().saloadBarrier(this, src, index, componentSize, aSrc);
    }

    public int acmpneBarrier(Oop v1, Oop v2, int aV1, int aV2) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
	return MemoryManager.the().acmpneBarrier(this, v1, v2, aV1, aV2);
    }

    public int acmpeqBarrier(Oop v1, Oop v2, int aV1, int aV2) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
          s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints, s3.util.PragmaCAlwaysInline { 
	return MemoryManager.the().acmpeqBarrier(this, v1, v2, aV1, aV2);
    }
    
    public void readBarrier(Oop src) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers {
	MemoryManager.the().readBarrier(this, src);
    }

    public Oop checkingTranslatingReadBarrier(Oop src, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
      s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints {
	return MemoryManager.the().checkingTranslatingReadBarrier(this, src, aSrc);
    }

    public Oop nonCheckingTranslatingReadBarrier(Oop src, int aSrc) throws s3.util.PragmaNoPollcheck, ovm.core.services.memory.PragmaNoBarriers,
      s3.util.PragmaAssertNoExceptions, s3.util.PragmaAssertNoSafePoints  {
	return MemoryManager.the().nonCheckingTranslatingReadBarrier(this, src, aSrc);
    }

    protected boolean SYNCHRONIZATION_ENABLED = false;

    public void enableSynchronization() {
        SYNCHRONIZATION_ENABLED = true;
    }

    /**
     * A fast-lock implemenation must override ensureMonitor, but
     * should be able to use the rest of S3CoreServicesAccess as-is.
     * In the fast-lock case, this may be called on a fast-locked
     * object for perfectly reasonable java programs.  Consider the
     * following snippet
     * <code>synchronized (condition) { condition.wait(); }</code>
     * It may well execute before any other thread touches
     * <code>condition</code>.
     **/

    public Monitor ensureMonitor(Oop o) throws PragmaAtomic {
	MonitorMapper mm = (MonitorMapper) o.asAnyOop();
	Monitor mon = mm.getMonitor();
	if (mon == null) {
	    Object r = MemoryPolicy.the().enterAreaForMonitor(o);
	    try { 
	      mon =  monitorFactory.newInstance(); 
	    } finally { 
	      MemoryPolicy.the().leave(r); 
            }
            
            mm.setMonitor(mon);
            MemoryManager.the().registerMonitor(mon, VM_Address.fromObject(o));
            
//            BasicIO.out.println("ensureMonitor: creating fresh monitor "+mon);
	    
	}
        else {
//             BasicIO.out.println("ensureMonitor: found existing monitor "+mon);
        }
        
	return mon;
    }

    public void createInterruptHandlerMonitor(Oop o, int interruptIndex) throws PragmaAtomic {

	MonitorMapper mm = (MonitorMapper) o.asAnyOop();
	Monitor mon = mm.getMonitor();
	
	if (mon != null) {
	  throw new RuntimeException("Interrupt handler already has a monitor.");
	}
	
        Object r = MemoryPolicy.the().enterAreaForMonitor(o);
        try { 
          mon = new InterruptHandlerMonitorImpl( interruptIndex, intMon );
        } finally {
          MemoryPolicy.the().leave(r); 
        }
	    
        mm.setMonitor(mon);
    }

    // ### WARNING WARNING WARNING ###
    // Code paths through monitor enter/exit must not unconditionally
    // invoke any code that itself is synchronized. If this happens
    // once the recursion check is entered we'll get a panic, otherwise
    // we'll get a stack overflow. Conditional use of synchronized code
    // might work outside of the recursion check. Things to avoid:
    // - toString() calls that might use the default Object.toString as
    //   that uses getClass().getName() which will use getType/blueprintFor
    //   and they use synchronization in the repository
    // - arraycopy either directly or via a StringBuffer.append that tries
    //   to grow the buffer, because GC/allocator locks may be needed
    // - allocation because GC/allocator locks might be required
    // - exception throwing because of most of the above

    static final boolean DEBUG_MONITOR = false;

    /** 
     * The monitor recursion check ensures that code that uses synchronized
     * methods or statements is not used by code the implements monitors.
     * This typically happens when debug code is enabled and we do extra
     * string and I/O operations, but sometimes happens by accident. If we
     * enable the recursion check then monitorEnter and monitorExit need to
     * be executed atomically so that between setting the recursion flag and 
     * doing the actual monitor action we don't invoke processEvents which
     * might (mainly due to debug code) result in synchronized code being 
     * called which results in the recursion panic being triggered.
     * Bottom line: if you set this to true then uncomment PragmaAtomic from
     * the throws clauses.
     */
    static final boolean CHECK_MONITOR_RECURSION = false;

    public void monitorEnter(Oop o) throws PragmaNoPollcheck {
        // monitorEnter may be called not only before
        // threading is bootstrapped, but before the allocator
        // is initialized, due to sync in the repository
        if (!SYNCHRONIZATION_ENABLED)
            return;

        if (DEBUG_MONITOR) {
            if (o == null) Native.print_string("DEBUG: null object\n");
        }
	assert (domain.isExecutive() ? true : o != null): 
	    "null object for monitorEnter";
        Monitor mon = ((MonitorMapper) o.asAnyOop()).getMonitor();
        if (mon == null) {
//            Native.print_string("monitorEnter: calling ensureMonitor\n");
            mon = ensureMonitor(o);
        }

        if (DEBUG_MONITOR) {
            if (mon == null) Native.print_string("DEBUG: null monitor!!!!!!!\n");
            else {
              Native.print_string("DEBUG: retrieved monitor:");
              Native.print_ptr( VM_Address.fromObjectNB(mon));
              Native.print_string("\n");
            }
        }
/*
        Native.print_string("monitorEnter object:");
        Native.print_ptr(VM_Address.fromObject(o));
        Native.print_string(" objectNB:");
        Native.print_ptr(VM_Address.fromObject(o));        
        Native.print_string(" monitor: ");
        Native.print_ptr(VM_Address.fromObject(mon));
        Native.print_string(" monitorNB: ");
        Native.print_ptr(VM_Address.fromObjectNB(mon));
        Native.print_string("\n");
*/
	assert mon != null: "CSA.monitorEnter: Mapper returned null monitor";

        if (CHECK_MONITOR_RECURSION) {
            // enable recursion protection
            Context ctx = Context.getCurrentContext();
            ctx.beforeMonitorUse();
            try {
                mon.enter();
            } finally {
                ctx.afterMonitorUse();
            }
        }
        else {
            mon.enter();
        }
    }

    public void monitorExit(Oop o) throws PragmaNoPollcheck {
        // monitorExit may be called not only before
        // threading is bootstrapped, but before the allocator
        // is initialized due to sync in the repository
        if (!SYNCHRONIZATION_ENABLED)
            return;
        if (DEBUG_MONITOR) {
            Native.print_string("DEBUG: entered monitorExit\n");
            if (o == null) Native.print_string("DEBUG: null object\n");
        }

/*
        Native.print_string("monitorExit object:");
        Native.print_ptr(VM_Address.fromObject(o));
        Native.print_string(" objectNB:");
        Native.print_ptr(VM_Address.fromObject(o));
        Native.print_string("\n");        
*/
        assert (domain.isExecutive() ? true : o != null): 
	    "null object for monitorExit";
        Monitor mon = ((MonitorMapper) o.asAnyOop()).getMonitor();
/*
        Native.print_string("monitorExit object:");
        Native.print_ptr(VM_Address.fromObject(o));
        Native.print_string(" objectNB:");
        Native.print_ptr(VM_Address.fromObject(o));        
        Native.print_string(" monitor: ");
        Native.print_ptr(VM_Address.fromObject(mon));
        Native.print_string(" is null: ");
        Native.print_string( (mon == null) ? "yes" : "no" );
        Native.print_string(" monitorNB: ");
        Native.print_ptr(VM_Address.fromObjectNB(mon));
        Native.print_string("\n");
*/
        // If mon is null then we have unbalanced synchronization which should
        // not be possible and so indicates a VM error.
        // If this assert fails we will go into an infinite loop due to the
        // way javac compiles sync blocks
	assert mon != null: "CSA.monitorExit: Mapper returned null monitor";

        if (CHECK_MONITOR_RECURSION) {
            // enable recursion protection        
            Context ctx = Context.getCurrentContext();
            ctx.beforeMonitorUse();
            
            try {
                mon.exit();
            }
            catch (IllegalMonitorStateException ex) {
                // FIXME: add to translateThrowable set?
                if (domain.isExecutive()) {
                    throw ex;
                }
                else {
                    this.generateThrowable(Throwables.ILLEGAL_MONITOR_STATE_EXCEPTION, 0);
                }
            } finally {
                ctx.afterMonitorUse();
            }
        }
        else {
            try {
                mon.exit();
            }
            catch (IllegalMonitorStateException ex) {
                // FIXME: add to translateThrowable set?
                if (domain.isExecutive()) {
                    throw ex;
                }
                else {
                    this.generateThrowable(Throwables.ILLEGAL_MONITOR_STATE_EXCEPTION, 0);
                }
            }
        }
    }


    // I believe that getMonitorOwner(), and getMonitorEntryCount(), 
    // are used purely for invasive tests.
    // If this is the case, they can safely take the slow path
    // (through ensureMonitor()) - Jason Baker Sept 2004

    // test function - not used normally
    public Oop getMonitorOwner(Oop o) {
        return VM_Address.fromObject(ensureMonitor(o).getOwner()).asOop();
    }
    // test function - not used normally
    public int getMonitorEntryCount(Oop o) {
        return ((ovm.services.monitors.RecursiveMonitor)ensureMonitor(o)).entryCount();
    }

    // used by Thread.holdsLock implementation - overridden for fast-locks
    public boolean currentThreadOwnsMonitor(Oop o) {
        return ensureMonitor(o).getOwner() == threadMan.getCurrentThread();
    }


    public void monitorTransfer(Oop o, OVMThread newOwner) {
        Monitor mon = ((MonitorMapper) o.asAnyOop()).getMonitor();
        if (mon == null) 
            mon = ensureMonitor(o);
        TransferableMonitor tmon = (TransferableMonitor) mon;
        tmon.transfer(newOwner);
        // any exception are internal errors as this isn't part of any
        // application API. So those errors will cause an abort
    }
  
    public void monitorSignal(Oop o) {
        try {
            ((JavaMonitor)ensureMonitor(o)).signal();
        }
        catch (IllegalMonitorStateException ex) {
            // FIXME: add to translateThrowable set?
            if (domain.isExecutive()) {
                throw ex;
            }
            else {
                this.generateThrowable(Throwables.ILLEGAL_MONITOR_STATE_EXCEPTION, 0);
            }
        }
    }

    public void monitorSignalAll(Oop o) {
        // if it's not a Java monitor it means this isn't a JVM config
        // so wait/notify don't apply. Really we should have an alternative
        // initializeBlueprint for that case. Note that wait will not be called
        // in such a config unless the executive domain is multi-threaded
        Monitor mon = ensureMonitor(o);
        if (mon instanceof JavaMonitor) {
            try {
                ((JavaMonitor)mon).signalAll();
            }
            catch (IllegalMonitorStateException ex) {
                // FIXME: add to translateThrowable set?
                if (domain.isExecutive()) {
                    throw ex;
                }
                else {
                    this.generateThrowable(Throwables.ILLEGAL_MONITOR_STATE_EXCEPTION, 0);
                }
            }
        }
    }

    public boolean monitorWaitAbortable(Oop o) {
        try {
            return ((JavaMonitor)ensureMonitor(o)).waitAbortable(null);
        }
        catch (IllegalMonitorStateException ex) {
            // FIXME: add to translateThrowable set?
            if (domain.isExecutive()) {
                throw ex;
            }
            else {
                this.generateThrowable(Throwables.ILLEGAL_MONITOR_STATE_EXCEPTION, 0);
            }
            return false; // NOT REACHED
        }
    }

    public int monitorWaitTimedAbortable(Oop o, long timeout) {
        try {
            return ((JavaMonitor)ensureMonitor(o)).waitTimedAbortable(null, timeout);
        }
        catch (IllegalMonitorStateException ex) {
            // FIXME: add to translateThrowable set?
            if (domain.isExecutive()) {
                throw ex;
            }
            else {
                this.generateThrowable(Throwables.ILLEGAL_MONITOR_STATE_EXCEPTION, 0);
            }
            return -1; // NOT REACHED
        }
    }


    public int monitorWaitAbsoluteTimedAbortable(Oop o, long timeout) {
        try {
            return ((RealtimeJavaMonitor)ensureMonitor(o)).waitAbsoluteTimedAbortable(null, timeout);
        }
        catch (IllegalMonitorStateException ex) {
            // FIXME: add to translateThrowable set?
            if (domain.isExecutive()) {
                throw ex;
            }
            else {
                this.generateThrowable(Throwables.ILLEGAL_MONITOR_STATE_EXCEPTION, 0);
            }
            return -1; // NOT REACHED
        }
    }

    public Oop getPrimitiveClass(char tag) {
        TypeName tn = TypeName.Primitive.make(tag);
        Type t;
        Oop sharedState;
        try {
            // Note: primitive type Class objects don't represent actual
            // classes, so there is no class initialization to be done.
            // Also primitives are only in the system type context.
            t = domain.getSystemTypeContext().typeFor(tn);
            sharedState = domain.blueprintFor(t).getSharedState();
            // create the class if needed
            return ((S3Type)t).getClassMirror(this, sharedState);
        } catch (LinkageException e) {
            throw Executive.panicOnException(e);
        }
    }
} // End of S3CoreServicesAccess
