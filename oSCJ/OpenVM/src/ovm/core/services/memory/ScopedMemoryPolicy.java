package ovm.core.services.memory;

import ovm.core.domain.Domain;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.execution.Native;
import ovm.util.CommandLine;

/**
 * This is the base class for VM_Area-aware memory allocation
 * policies.  These policies typically support a per-thread
 * scratchpad, and must provide an implemenation of makeScratchPadArea
 * to the core memory/threading system.
 *
 * @author <a href=mailto://baker29@cs.purdue.edu> Jason Baker </a>
 **/
public class ScopedMemoryPolicy extends DefaultMemoryPolicy {
    final int SCRATCH_PAD_SIZE;

    protected abstract static class Cleanup {
	private VM_Area outer;

	public Cleanup(VM_Area outer) throws PragmaNoBarriers { this.outer = outer; }

	public void leave() {
	    MemoryManager.the().setCurrentArea(outer);
	}
    }

    public ScopedMemoryPolicy(String scSize) {
	SCRATCH_PAD_SIZE = CommandLine.parseSize(scSize);
	//TODO: pass through command line
    }

    public VM_Area makeScratchPadArea() {
    	if(_DEBUG_SCJ){
    		Native.print_string("[SCJ DB] ScopedMemoryPolicy.makeScratchPadArea() - size ");
    		Native.print_int(SCRATCH_PAD_SIZE);
    		Native.print_string("\n");
    	}
	return MemoryManager.the().makeExplicitArea(SCRATCH_PAD_SIZE);
    }

    public Object enterScratchPadArea() {
	final VM_Area sc = ScopedMemoryContext.getScratchPadArea();
	// Well, the scratchpad actually is used recursively, and it
	// is not easy to break the recursion
	// assert(sc.memoryConsumed() == 0);
	final int ptr = sc.memoryConsumed();
	VM_Area outer = MemoryManager.the().setCurrentArea(sc);
	return new Cleanup(outer) {
		public void leave() {
		    super.leave();
		    sc.reset(ptr);
		}
	    };
    }

    public void leave(Object r) {
	if (r instanceof Cleanup)
	    ((Cleanup) r).leave();
	else
	    super.leave(r);
    }

    // Basically, this excludes scratchpad-allocated strings
    public boolean isInternable(Domain _, Oop str) {
	return ((MemoryManager.the().areaOf(str) ==
		 MemoryManager.the().getImmortalArea())
		|| (MemoryManager.the().areaOf(str) ==
		    MemoryManager.the().getHeapArea()));
    }

    /**
     * Well, we will leak an ED exception, but only until we panic.
     **/
    public Object enterExceptionSafeArea() {
	VM_Area ret = MemoryManager.the().getCurrentArea();
	if (ret != MemoryManager.the().getHeapArea())
	    return enterImmortal();
	else
	    return ret;
    }

    public Object enterServiceInstanceArea() {
	return enterImmortal();
    }

    public static ScopedMemoryPolicy theScopedPolicy()
	// throws PragmaRefineSingleton FIXME: see bug #546
    {
	return (ScopedMemoryPolicy) MemoryPolicy.the();
    }

    public static int _DEFAULT_STACKTRACE_BUF_SIZE = 8*1024;//5*1024*1024;
    private boolean _DEBUG_SCJ = true;

    /**
     * For SCJ
     */
	public Object enterStackTraceBufArea() {
		VM_Area ssbuf = ScopedMemoryContext.getStackTraceBufArea();
		return MemoryManager.the().setCurrentArea(ssbuf);
	}
	
    public VM_Area makeStackTraceBufArea(int size) {
        if(size <= 0)
        	size = _DEFAULT_STACKTRACE_BUF_SIZE;        
    	if(_DEBUG_SCJ){
    		Native.print_string("[SCJ DB] ScopedMemoryPolicy.makeStackTraceBufArea() - size ");
    		Native.print_int(size);
    		Native.print_string("\n");
    	}
        return MemoryManager.the().makeExplicitArea(size);
    }
    
    /**
     * All pre-allocated VM exceptions are allocated in MetaData area. 
     * See S3CoreServicesAccess.boot(). Therefore, make sure that area
     * is just immortal area.
     */
    public Object enterMetaDataArea(Type.Context ctx) {
    	return enterImmortal();
    }
}
