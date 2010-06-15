package ovm.core.execution;

import ovm.core.Executive;
import ovm.core.domain.Blueprint;
import ovm.core.domain.Code;
import ovm.core.domain.Domain;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.Method;
import ovm.core.domain.ReflectiveMethod;
import ovm.core.domain.Type;
import ovm.core.repository.JavaNames;
import ovm.core.repository.TypeName;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.services.memory.VM_Address;
import ovm.util.Iterator;
import s3.core.domain.S3ByteCode;
import s3.services.simplejit.dynamic.SimpleJITDynamicJITCompiler;
import ovm.core.OVMBase;
import s3.core.domain.S3Domain;
import s3.util.PragmaMayNotLink;

/**
 * The Engine class provides a mechanism for executin engines
 * (compilers, interpreters, and foreign function interfaces) to
 * interact with each other, and with the rest of the system.
 * <p>
 * Engines may use the state/observer pattern to track important
 * changes to the VM state (such as thread creation and
 * classloading).  And, engines will be asked to dynamically compile
 * methods when no executable code is available.
 **/
public abstract class Engine {
    private static final ReflectiveMethod dynamicCompile =
	new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			     JavaNames.ovm_core_domain_Code,
			     JavaNames.ovm_core_execution_Engine.getGemeinsamTypeName(),
			     "dynamicCompile",
			     new TypeName[] {
				 JavaNames.s3_core_domain_S3ByteCode
			     });

    static public VM_Address getTrampoline(S3ByteCode code) {
	assert (! OVMBase.isBuildTime());
	return Trampoline.the()
 			.compileAndRestart(code,
					   dynamicCompile.getMethod().getCode(),
					   dynamicCompile.getStaticReceiver());
    }

    protected void onThaw(Domain d) { }

     static final boolean dynamicLoadingEnabled =
 	!s3.services.bootimage.ImageObserver.the().isJ2c();

    static public void observeThaw(Domain d) {
	// Should call onThaw() for all engines

	if (!d.isExecutive() && !(Trampoline.the() instanceof NullTrampoline))
	    installTrampolines(d);
    }

    static private void installTrampolines(Domain d) throws PragmaMayNotLink {
	// Update entry points for all unrewritten S3ByteCode
	// objects
	for (Iterator bit = d.getBlueprintIterator(); bit.hasNext(); ) {
	    Blueprint bp = (Blueprint) bit.next();
	    Type t = bp.getType();
	    for (Method.Iterator mit = t.localMethodIterator();
		 mit.hasNext(); ) {
		Code c = mit.next().getCode();
		if (c instanceof S3ByteCode)
		    ((S3ByteCode) c).ensureTrampoline();
	    }
	}
    }

    static public synchronized Code dynamicCompile(S3ByteCode code)
	throws PragmaMayNotLink
    {
	Code oops = code.getMethod().getCode();
	if (oops != code /* || code is quickified for interpreter */)
	    // Oops, someone got here first.
	    return oops;

	// Should try doDynamicCompile() for all engines.
	// Until then, do Hiroshi's thing.
	Domain d =
	    code.getMethod().getDeclaringType().getDomain();
	if (((S3Domain) d).sj != null) {
	    Object r = MemoryPolicy.the().enterHeap();
	    try {
		d.getRewriter().ensureState(code, S3ByteCode.REWRITTEN);
		return SimpleJITDynamicJITCompiler.dynamicCompile(code);
	    } finally {
		MemoryPolicy.the().leave(r);
	    }
	} else {
	    throw Executive.panic("can't compile " + code.getSelector());
	}
    }
}
