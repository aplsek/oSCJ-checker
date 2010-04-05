package s3.services.memory.mostlyCopying;

import ovm.core.services.memory.*;
import ovm.core.domain.*;
import ovm.services.memory.scopes.*;

public class FakeScopeManagerWithAreaOf extends FakeScopeManager {
    public FakeScopeManagerWithAreaOf(String heapSize) {
	super(heapSize);
	
	if (!(ObjectModel.getObjectModel() instanceof ScopePointer.Model)) {
	    throw new Error("You need to use a model that supports ScopePointer.");
	}
    }
    
    // YUCK: More code duplication, this time with triPizlo.FakeScopeManWithAreaOf!
    
    Oop stampScope(Oop oop) {
	ScopePointer soop=(ScopePointer)oop.asAnyOop();
	soop.setScopePointer((VM_Area)ScopedMemoryContext.getMemoryContext());
	return soop;
    }
    
    public Oop allocate(Blueprint.Scalar bp) {
	return stampScope(bp.stamp(getMem(bp.getFixedSize())));
    }
    
    public Oop allocateArray(Blueprint.Array bp, int len) {
	long size = bp.computeSizeFor(len);
	if (size < Integer.MAX_VALUE)
	    return stampScope(bp.stamp(getMem((int) size), len));
	else
	    throw outOfMemory();
    }
    
    public Oop clone(Oop oop) {
        Blueprint bp = oop.getBlueprint();
        return stampScope(bp.clone(oop, getMem(bp.getVariableSize(oop))));
    }
    
    public VM_Area areaOf(Oop oop) {
	ScopePointer soop=(ScopePointer)oop.asAnyOop();
	VM_Area result=soop.getScopePointer();
	if (result==null) {
	    return getImmortalArea();
	} else {
	    return result;
	}
    }
    
    // FIXME: implement supportScopeAreaOf() to return true
}

