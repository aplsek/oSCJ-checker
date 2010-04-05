package ovm.core.services.memory;

import ovm.core.domain.Domain;
import ovm.core.domain.Oop;
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

    protected static abstract class Cleanup {
	private VM_Area outer;

	public Cleanup(VM_Area outer) throws PragmaNoBarriers { this.outer = outer; }

	public void leave() {
	    MemoryManager.the().setCurrentArea(outer);
	}
    }

    public ScopedMemoryPolicy(String scSize) {
	SCRATCH_PAD_SIZE = CommandLine.parseSize(scSize);
    }

    public VM_Area makeScratchPadArea() {
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
}
