package s3.services.memory.triPizlo;

import ovm.core.services.memory.*;
import ovm.core.stitcher.InvisibleStitcher.*;
import ovm.services.memory.scopes.*;
import ovm.core.*;
import ovm.core.domain.*;
import ovm.core.execution.Context;

/** Implements fake scoped memory.  Objects are allocate in the heap, but
    scope hierarchy management still works, so RTSJ programs will
    basically work as expected. */
public class FakeScopeMan extends TheMan implements Component {
    
    public FakeScopeMan(String memSize,
			String gcThreshold,
			String fixedHeap,
			String noBarrier) {
	super(memSize,gcThreshold,fixedHeap,noBarrier);
    }
    
    public void initialize() {
	if (!(Context.factory() instanceof ScopedMemoryContext.Factory))
	    throw new MisconfiguredException("realtime java memory manager "+
					     "configured without threading "+
					     "support");
    }
	
    PrimordialScope primordial=new PrimordialScope(false);
    
    static class Area extends VM_ScopedArea {
	Area(PrimordialScope primordial, Oop mirror) {
	    super(primordial);
	    this.mirror=mirror;
	}
	
	public int size() {
	    return TheMan.heap.size();
	}
	
	public int memoryConsumed() {
	    return TheMan.heap.memoryConsumed();
	}
	
	public void addDestructor(Destructor d) {}
	public void removeDestructor(Destructor d) {}
	public Oop revive(Destructor d) { throw Executive.panic("revive() not implemented"); }
	public int destructorCount(int kind) { return 0; }
	public void walkDestructors(int kind, DestructorWalker w) {}
	public void walkDestructors(int kind, DestructorWalker w, int max) {}

	public void reset() {}
	public void reset(int _) {}
	public void destroy() {}
    }
    
    public VM_Area makeExplicitArea(int size) {
	return new Area(primordial, null);
    }
    
    public VM_ScopedArea makeScopedArea(Oop mirror, int size) {
	return new Area(primordial, mirror);
    }
    
    VM_ScopedArea immortal=new Area(primordial, null);
    
    public VM_Area getImmortalArea() {
	return immortal;
    }

    public VM_Area getCurrentArea() {
	return (VM_Area) ScopedMemoryContext.getMemoryContext();
    }
    public VM_Area setCurrentArea(VM_Area area) {
	// ignore boot-time calls
	if (memory == null)
	    return null;

	VM_Area ret = (VM_Area) ScopedMemoryContext.getMemoryContext();
	ScopedMemoryContext.setMemoryContext(area);
	return ret;
    }

    public Object makeMemoryContext() {
	return TheMan.heap;
    }
}


