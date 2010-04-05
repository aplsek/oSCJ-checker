package s3.services.memory.mostlyCopying;

import ovm.core.domain.*;
import ovm.core.services.memory.*;
import ovm.services.memory.scopes.*;
import ovm.core.*;
import ovm.util.*;

public class FakeScopeManager extends Manager {
    public FakeScopeManager(String heapSize) {
	super(heapSize);
    }
    
    // YUCK!  Code duplication with triPizlo.FakeScopeMan!
    
    PrimordialScope primordial=new PrimordialScope(false);
    
    class FakeHeapArea extends VM_Area {
	public VM_Address getMem(int size) { throw new OVMError.Unimplemented(); }
	public int size() { return gcThreshold<<blockShift; }
	public int memoryConsumed() { return allocated<<blockShift; }
	protected VM_Address getBaseAddress() { throw new OVMError.Unimplemented(); }
	public void addDestructor(Destructor d) {}
	public void removeDestructor(Destructor d) {}
	public Oop revive(Destructor d) { throw Executive.panic("revive() not implemented"); }
	public int destructorCount(int kind) { return 0; }
	public void walkDestructors(int kind, DestructorWalker w) {}
	public void walkDestructors(int kind, DestructorWalker w, int max) {}
    }
    
    protected VM_Area makeHeapArea() {
	return new FakeHeapArea();
    }

    class Area extends VM_ScopedArea {
	Area(PrimordialScope primordial, Oop mirror) {
	    super(primordial);
	    this.mirror=mirror;
	}
	
	public VM_Address getMem(int size) {
	    throw Executive.panic("Area.getMem() not supported");
	}
	
	public int size() {
	    return heapArea.size();
	}
	
	public int memoryConsumed() {
	    return heapArea.memoryConsumed();
	}
	
	protected VM_Address getBaseAddress() {
	    throw Executive.panic("Area.getBaseAddress() not supported");
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
	VM_Area ret = (VM_Area) ScopedMemoryContext.getMemoryContext();
	ScopedMemoryContext.setMemoryContext(area);
	return ret;
    }

    public Object makeMemoryContext() {
	return heapArea;
    }
    
    public boolean supportsDestructors() { return false; }
}

