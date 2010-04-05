package s3.services.simplejit.dynamic;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Method;
import ovm.core.domain.Type;
import ovm.core.repository.Attribute;
import ovm.core.repository.ExceptionHandler;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.VM_Area;
import ovm.util.HashSet;
import ovm.util.Iterator;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3ByteCode;
import s3.core.domain.S3Constants;
import s3.core.domain.S3Domain;
import s3.core.domain.S3Method;
import s3.services.simplejit.CodeGenContext;
import s3.services.simplejit.SimpleJIT;
import s3.services.simplejit.SimpleJITCode;
import s3.util.PragmaMayNotLink;

public class SimpleJITDynamicDomainCompiler {

    public static void run(S3Domain dom) {
        if (dom.sj.lazyCompilation()) {
	    SimpleJITDynamicDomainCompiler.prepareForDynamicCompilation(dom);
        } else {
	    SimpleJITDynamicDomainCompiler.batchCompile(dom);
        }
    }

    private static void prepareForDynamicCompilation(S3Domain dom) {
	dom.compileArea = MemoryManager.the().makeExplicitArea(20*1024*1024);
	if (dom.compileArea == null) {
	    BasicIO.out.println("Cannot allocate compile area, " +
				"using heap and pinning");
	} else {
	    BasicIO.out.println("compile area's size = " +
				dom.compileArea.size());
	}
    }

    // FIXME: This code should not be a part of purely static builds
    private static void batchCompile(S3Domain dom) throws PragmaMayNotLink {
        int compileCount = 0;
	HashSet compiledMethods = new HashSet();
	dom.compileArea = MemoryManager.the().makeExplicitArea(20*1024*1024);
	if (dom.compileArea == null) {
	    BasicIO.out.println("Cannot allocate compile area, " +
				"using heap and pinning");
	} else {
	    BasicIO.out.println("compile area's size = " +
				dom.compileArea.size());
	}
	// compileArea is null if the memory manager is not SplitRegionManager...
	for (Iterator it = dom.getBlueprintIterator(); it.hasNext();) {
	    Blueprint bp = (Blueprint) it.next();
	    if (bp.isSharedState())
		continue;
	    Type.Class t;
	    if (bp.getType() instanceof Type.Class) {
		t = (Type.Class) bp.getType();
		Method.Iterator mit = t.localMethodIterator();
		while (mit.hasNext()) {
		    Method meth = mit.next();
		    if (!((S3Method)meth).toCompile) {
			continue;
		    }
		    if (meth.getCode(SimpleJITCode.KIND) != null) {
			continue;
		    }
		    //BasicIO.out.println("Compiling " + meth.toString());
		    dom.sj.compile(meth, bp, dom.compileArea);
		    compiledMethods.add(meth);
		    compileCount++;
		}
	    }
	    t = bp.getType().getSharedStateType();
	    bp = dom.blueprintFor(t);
	    Method.Iterator mit = t.localMethodIterator();
	    while (mit.hasNext()) {
		Method meth = mit.next();
		if (!((S3Method)meth).toCompile) {
		    continue;
		}
		if (meth.getCode(SimpleJITCode.KIND) != null) {
		    //BasicIO.out.println("Skipping already compiled method : " + meth);
		    continue;
		}
		//BasicIO.out.println("Compiling " + meth.toString());
		dom.sj.compile(meth, bp, dom.compileArea);
		compiledMethods.add(meth);
		compileCount++;
	    }
	}

	if (compileCount == 0)
	    return;
        for (Iterator it = compiledMethods.iterator(); it.hasNext(); ) {
	    Method meth = (Method) it.next();
	    SimpleJITCode nc = (SimpleJITCode) meth.getCode(SimpleJITCode.KIND);
	    if (nc != null && MemoryManager.the().shouldPinCrazily()) {
		MemoryManager.the().pin(nc);
		MemoryManager.the().pin(nc.constants_);
		Object[] constants = ((S3Constants)nc.constants_).constants();
		MemoryManager.the().pin(constants);
		for(int i = 0; i < constants.length; i++) {
		    Object c = constants[i];
		    if (c != null)
			MemoryManager.the().pin(c);
		}
	    }
	    S3ByteCode bc = meth.getByteCode();
	    if (bc != null && MemoryManager.the().shouldPinCrazily()) {
		MemoryManager.the().pin(bc);
		MemoryManager.the().pin(bc.getConstantPool());
		Object[] constants = ((S3Constants)bc.getConstantPool()).constants();
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

	SimpleJIT.printCompileCounter();
	    
	if (dom.compileArea != null)
	    BasicIO.out.println("Compile area memory used : " +
				dom.compileArea.memoryConsumed());
    }
}
