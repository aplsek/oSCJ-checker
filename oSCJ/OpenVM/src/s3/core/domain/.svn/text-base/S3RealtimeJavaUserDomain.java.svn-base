package s3.core.domain;

import ovm.core.domain.Oop;
import ovm.core.domain.RealtimeJavaUserDomain;
import ovm.core.domain.ReflectiveConstructor;
import ovm.core.domain.ReflectiveMethod;
import ovm.core.domain.ReflectiveField;
import ovm.core.domain.Type;
import ovm.core.domain.WildcardException;
import ovm.core.execution.InvocationMessage;
import ovm.core.repository.JavaNames;
import ovm.core.repository.TypeName;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.VM_Area;
import ovm.core.services.memory.VM_Area.Destructor;
import ovm.core.domain.ReflectiveVirtualFunction;
import ovm.core.domain.RealtimeJavaUserDomain.RealtimeJavaTypes;

/**
 * User-domain implementation for a JVM that supports the Realtime
 * Specification for Java (RTSJ).
 *
 */
public class S3RealtimeJavaUserDomain extends S3JavaUserDomain 
    implements RealtimeJavaUserDomain {

    public String toString() {
	return "RealtimeUserDomain_" + ((int)instanceCounter_);
    }
    

    public void startup() {
        super.startup();
        pln("Initializing the Realtime user domain");
    }


    S3RealtimeJavaUserDomain(TypeName.Scalar mainClassName, 
                             String systemResourcePath,
                             String userResourcePath) {
	super(mainClassName, systemResourcePath, userResourcePath);
    }

    // our common types utility class
    private volatile RealtimeJavaTypes commonTypesInstance;

    public RealtimeJavaTypes commonRealtimeTypes() {
        // here's a race but it's OK because if two JavaTypes objects
        // are created, they have the same contents (hopefully nobody
        // cares about identity and such
        if (commonTypesInstance == null) {
	    Type.Context sctx = getSystemTypeContext();
	    Object r = MemoryPolicy.the().enterMetaDataArea(sctx);
	    try { commonTypesInstance = new RealtimeJavaTypes(this); }
	    finally { MemoryPolicy.the().leave(r); }
        }
        return commonTypesInstance;
    }


    // Reflection Helpers for the threading system

    /** 
     * Field object for the UD javax.realtime.RealtimeThread initArea field.
     */
    public final ReflectiveVirtualFunction VMThread_getInitialArea =
        new ReflectiveVirtualFunction(this,
				      JavaNames.VMThread_getInitialArea);

    /** 
     * Field object for the UD javax.realtime.MemoryArea area field.
     */
    public final ReflectiveField.Reference memoryArea_area =
        new ReflectiveField.Reference(this,
                                      JavaNames.org_ovmj_java_Opaque,
                                      JavaNames.javax_realtime_MemoryArea, 
                                      "area");

    /**
     * Method object for RealtimeThread finalizeThread method
     */
    public final ReflectiveVirtualFunction realtimeThread_finalizeThread =
        new ReflectiveVirtualFunction(this,
				      JavaNames.VMThread_finalizeThread);

    // Define the reflective constructors needed for throwing RT specific
    // exceptions

    ReflectiveConstructor illegalAssignmentError =
        new ReflectiveConstructor(this,
                                  JavaNames.javax_realtime_IllegalAssignmentError,
                                  NO_ARGS);

    ReflectiveConstructor memoryAccessError =
        new ReflectiveConstructor(this,
                                  JavaNames.javax_realtime_MemoryAccessError,
                                  NO_ARGS);



    /**
     * Throws a <tt>MemoryAccessError</tt> always
     */
    public void readBarrierFailed() {
        throw this.
            getCoreServicesAccess().
            processThrowable(memoryAccessError.make());
    }

    /**
     * Throws an <tt>IllegalAssignmentError</tt> always
     */
    public void storeBarrierFailed() {
        throw this.
            getCoreServicesAccess().
            processThrowable(illegalAssignmentError.make());
    }

    /**
     * Helper class for finalizing scope-allocated objects.  While
     * heap-allocated objects are passed to a finalizer thread one at
     * a time, a RT finalizer thread must deal with entire scopes.
     * Hence, the upcall to <code>Object.finalize()</code> can go
     * directly into the destroy method.
     **/
    private static class FinalizeNow extends Destructor {
	ReflectiveMethod rm;
	public int getKind() { return NORMAL; }
	public FinalizeNow(Oop oop, ReflectiveMethod rm) {
	    super(oop);
	    this.rm = rm;
	}
	
	public void destroy(VM_Area home) {
	    Oop oop = home.revive(this);
	    Object r1 = MemoryPolicy.the().enterScratchPadArea();
	    try {
		InvocationMessage imsg = rm.makeMessage();
		VM_Area r2 = MemoryManager.the().setCurrentArea(home);
		try {
		    imsg.invoke(oop);
		} catch (WildcardException _) {
		    BasicIO.out.println("exception in scoped finalizer");
		}
		finally {
		    MemoryManager.the().setCurrentArea(r2);
		}
	    } finally {
		MemoryPolicy.the().leave(r1);
	    }
	}
    }

    public void registerFinalizer(Oop oop) {
	VM_Area a = MemoryManager.the().areaOf(oop);
	if (a == MemoryManager.the().getHeapArea())
	    super.registerFinalizer(oop);
	else if (a != MemoryManager.the().getImmortalArea()) {
	    ReflectiveMethod rm = finalizeVF.dispatch(oop);
	    if (rm.getMethod() != finalizeBase.getMethod())
		a.addDestructor(new FinalizeNow(oop, rm));
	}
    }
}
