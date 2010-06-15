package s3.services.memory.mostlyCopying;

import ovm.util.CommandLine;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.core.services.memory.ScopedMemoryContext;
import ovm.core.Executive;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Area;
import ovm.core.domain.Blueprint;
import ovm.core.domain.Oop;

public class ReflexSplitRegionManager extends SplitRegionManager {
    int numAreaKinds;

    static class MyMemContext {
	VM_Area[] kinds;
	
	MyMemContext(int numAreaKinds,VM_Area zerothArea) {
	    this.kinds=new VM_Area[numAreaKinds];
	    this.kinds[0]=zerothArea;
	}
	
	void setKind(int idx,VM_Area kind) throws PragmaNoBarriers {
	    kinds[idx]=kind;
	}
	
	VM_Area getKind(int idx) throws PragmaNoBarriers {
	    return kinds[idx];
	}
    }

    public ReflexSplitRegionManager(String heapSize,
				    String immortalSize,
				    String scopeSize,
				    String disableChecks,
				    String numAreaKinds) {
	super(heapSize,immortalSize,scopeSize,disableChecks);
	this.numAreaKinds=
	    CommandLine.parseSize(numAreaKinds); /* size?  not sure if that
						    makes sense, but what the
						    heck. */
    }
    
    public VM_Address getMem(int size) {
	throw Executive.panic("ReflexSplitRegionManager.getMem(int size) called");
    }
	    
    public VM_Address getMem(Blueprint bp,int size) {
	return ((Area)((MyMemContext)ScopedMemoryContext.getMemoryContext()).getKind(bp.getAllocKind())).getMem(size);
    }
    
    public Oop allocate( Blueprint.Scalar bp) {
        return bp.stamp(getMem(bp,bp.getFixedSize()));
    }

    public Oop allocateArray(Blueprint.Array bp, int len) {
	long size = bp.computeSizeFor(len);
	if (size < Integer.MAX_VALUE)
	    return bp.stamp(getMem(bp,(int) size), len);
	else
	    throw outOfMemory();
    }

    public Oop clone(Oop oop) {
        Blueprint bp = oop.getBlueprint();
        return bp.clone(oop, getMem(bp,bp.getVariableSize(oop)));
    }

    public Object makeMemoryContext() {
	return new MyMemContext(numAreaKinds,heapArea);
    }

    public VM_Area setCurrentArea(VM_Area area) {
	return setCurrentArea(0,area);
    }
    
    public VM_Area setCurrentArea(int idx,VM_Area area) {
	MyMemContext ctx=(MyMemContext)ScopedMemoryContext.getMemoryContext();
	VM_Area result=ctx.getKind(idx);
	ctx.setKind(idx,area);
	return result;
    }

    public VM_Area getCurrentArea() {
	return getCurrentArea(0);
    }
    
    public VM_Area getCurrentArea(int idx) {
	MyMemContext ctx=(MyMemContext)ScopedMemoryContext.getMemoryContext();
	return ctx.getKind(idx);
    }
    
}


