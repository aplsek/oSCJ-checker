package ovm.services.memory.scopes;

import ovm.core.services.memory.*;
import ovm.core.*;

public class PrimordialScope extends VM_ScopedArea {
    
    final boolean buildRanges;

    public PrimordialScope(boolean buildRanges) {
	super();
	this.root=this;
	this.buildRanges=buildRanges;
    }
    
    void recomputeRanges() {
	if (buildRanges) {
	    walk((short)0);
	}
    }
    
    public int size() { return 0; }
    public int memoryConsumed() { return 0; }
	
    public VM_Address getMem(int size) {
	throw Executive.panic("attempt to allocate in primordial scope");
    }
    
    public VM_Address getBaseAddress() {
	throw Executive.panic("attempt to get base address of primordial scope");
    }
    
}

