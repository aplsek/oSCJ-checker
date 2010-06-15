package s3.services.memory.triPizlo;

import ovm.core.services.memory.*;
import ovm.core.domain.*;
import ovm.services.memory.scopes.*;

public class FakeScopeManWithAreaOf extends FakeScopeMan {
    public FakeScopeManWithAreaOf(String memSize,
				  String gcThreshold,
				  String fixedHeap,
				  String noBarrier) {
	super(memSize,gcThreshold,fixedHeap,noBarrier);
	
	if (!(ObjectModel.getObjectModel() instanceof ScopePointer.Model)) {
	    throw new Error("You need to use a model that supports ScopePointer.");
	}
    }
    
    Oop stampGCBits(Oop oop) {
	ScopePointer soop=(ScopePointer)super.stampGCBits(oop).asAnyOop();
	soop.setScopePointer((VM_Area)ScopedMemoryContext.getMemoryContext());
	return soop;
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


