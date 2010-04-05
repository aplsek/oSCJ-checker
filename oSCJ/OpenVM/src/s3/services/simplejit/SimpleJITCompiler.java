package s3.services.simplejit;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Method;
import ovm.core.execution.NativeConstants;
import ovm.core.repository.Selector;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.VM_Area;
import ovm.util.OVMError;
import s3.util.PragmaTransformCallsiteIR;
import s3.services.simplejit.SimpleJITAnalysis;

public class SimpleJITCompiler {
    private final boolean debugPrint;
    private final boolean opt;
    private final boolean verbose;
    private final CompilerVMInterface compilerVMInterface;
    private SimpleJIT jit;

    protected SimpleJITCompiler
	(boolean debugPrint,
	 boolean opt,
	 boolean verbose,
	 CompilerVMInterface compilerVMInterface,
	 SimpleJITAnalysis anal) {
        this.debugPrint = debugPrint;
        this.opt = opt;
        this.verbose = verbose;
        this.compilerVMInterface = compilerVMInterface;
        if ((NativeConstants.OVM_X86 && NativeConstants.OVM_PPC)
            || ((! NativeConstants.OVM_X86) && (! NativeConstants.OVM_PPC))) {
            throw new Error("OVM_X86 xor OVM_PPC != 1 in NativeConstants.java");
        } else if (NativeConstants.OVM_X86) {
            jit = new s3.services.simplejit.x86.SimpleJITImpl(
                    compilerVMInterface, debugPrint, opt, anal);
        } else if (NativeConstants.OVM_PPC) {
            jit = new s3.services.simplejit.powerpc.SimpleJITImpl(
                    compilerVMInterface, debugPrint, opt, anal);
        }
    }

    public boolean verbose() { return verbose; }
    public CompilerVMInterface getCompilerVMInterface() {
        return compilerVMInterface;
    }

    public CodeGenerator.Precomputed getCodeGeneratorPrecomputed() {
        return jit.getCodeGeneratorPrecomputed();
    }
    
    public String getStackLayoutAsCFunction() {
	return jit.getStackLayoutAsCFunction();
    }

    /**
     * Compile the given method, set the appropriate fields
     * in the Blueprint/VTBL.
     **/
    public void compile(Method meth, Blueprint bp, VM_Area compileArea) {
        Selector.Method sel = meth.getSelector();

	// If the method implements some pragma, skip it
	Blueprint pbp = meth.getMode().isStatic()
	    ? bp.getSharedState().getBlueprint()
	    : bp;
	if (PragmaTransformCallsiteIR
	    .descendantDeclaredBy(sel, pbp) != null) {
	    //d("Due to its pragma, skipping " + sel);
	    return; // do not compile it
	}

	//d("[JIT] Compiling " + sel);
        try {
        	jit.compile(meth, compileArea);
        } catch (OVMError.Internal ie) {
	    // fatal error: abort the whole compilation
	    ie.printStackTrace();
            throw new OVMError.Internal
		("[SimpleJIT] ERROR : " + ie + 
		 " : Could not compile " + sel);
        } catch (Throwable t) {
            BasicIO.err.println("[SimpleJIT] ERROR : " + t + 
	      " : Could not compile " + sel);
            t.printStackTrace();
        }
    }
}
