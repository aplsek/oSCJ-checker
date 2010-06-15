package s3.services.simplejit.dynamic;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Method;
import ovm.core.domain.Type;
import ovm.core.repository.Attribute;
import ovm.core.repository.ExceptionHandler;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Area;
import s3.core.domain.S3ByteCode;
import s3.core.domain.S3Constants;
import s3.core.domain.S3Domain;
import s3.services.simplejit.SimpleJITCode;
import s3.services.simplejit.CodeGenContext;
import s3.util.PragmaAtomic;
import ovm.core.domain.Code;
import s3.util.PragmaMayNotLink;

/**
 * @author yamauchi
 */
public class SimpleJITDynamicJITCompiler {
    private static int compileCount = 0;
	
    public static Code dynamicCompile(Code code)
	throws PragmaMayNotLink /*, PragmaAtomic*/
    {
	Method meth = code.getMethod();
	Type.Compound declaringType = meth.getDeclaringType();
	S3Domain dom = (S3Domain) declaringType.getDomain();

	if (dom.sj.verbose()) {
	    VM_Area prev = MemoryManager.the().setCurrentArea(dom.compileArea);
	    try {
		BasicIO.out.println("Compiling " + meth);
	    } finally {
		MemoryManager.the().setCurrentArea(prev);
	    }
	}

	Blueprint bp = dom.blueprintFor(declaringType);
	if (meth.getCode(SimpleJITCode.KIND) != null) {
	    BasicIO.err.println/*throw new Error*/("Warning: tried to compile an already compiled method : " + meth);
	    return meth.getCode(SimpleJITCode.KIND);
	}

	dom.sj.compile(meth, bp, dom.compileArea);
	compileCount++;
	SimpleJITCode nc = (SimpleJITCode) meth.getCode();
	// Setting SimpleJITCode.codeEntry and pinning
	if (MemoryManager.the().shouldPinCrazily()) {
	    MemoryManager.the().pin(nc);
	    MemoryManager.the().pin(nc.constants_);
	    Object[] constants = ((S3Constants)nc.constants_).constants();
	    MemoryManager.the().pin(constants);
	    for(int i = 0; i < constants.length; i++) {
		Object c = constants[i];
		if (c != null)
		    MemoryManager.the().pin(c);
	    }

	    S3ByteCode bc = meth.getByteCode();
	    if (bc != null) {
		MemoryManager.the().pin(bc);
		MemoryManager.the().pin(bc.getConstantPool());
		constants = ((S3Constants)bc.getConstantPool()).constants();
		MemoryManager.the().pin(constants);
		for(int i = 0; i < constants.length; i++) {
		    Object c = constants[i];
		    if (c != null)
			MemoryManager.the().pin(c);
		}
		Attribute[] attributes = bc.getAttributes();
		if (attributes != null) {
		    MemoryManager.the().pin(attributes);
		    for(int i = 0; i < attributes.length; i++) {
			MemoryManager.the().pin(attributes[i]);
		    }
		}		    	
		ExceptionHandler[] exceptions = bc.getExceptionHandlers();
		if (exceptions != null) {
		    MemoryManager.the().pin(exceptions);
		    for(int i = 0; i < exceptions.length; i++) {
			MemoryManager.the().pin(exceptions[i].getCatchTypeName());
		    }
		}
	    }     
	}

	//SimpleJIT.printCompileCounter();
	    
	return nc;
    }
}
