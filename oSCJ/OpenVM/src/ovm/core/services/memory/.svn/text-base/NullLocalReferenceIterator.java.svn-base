package ovm.core.services.memory;

import ovm.core.execution.Context;
import ovm.core.Executive;

/** this is the iterator that you use if you don't actually ever have to walk stacks */
public class NullLocalReferenceIterator extends LocalReferenceIterator {
    protected boolean moreReferencesInThread() {
	return false;
    }
    
    protected VM_Address nextReferenceInThread() {
	throw Executive.panic("nextReferenceInThread() called in NullLocalReferenceIterator");
    }
    
    protected void setThread(Context _) {}
    
    public int supportedModes() { return 0; }
    
    public boolean expectsEngineSupport() {
	return false;
    }
}

