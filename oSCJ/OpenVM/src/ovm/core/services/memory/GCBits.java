package ovm.core.services.memory; // SYNC PACKAGE NAME TO STRING BELOW

import ovm.core.domain.Oop;
import ovm.core.services.memory.VM_Address;
import ovm.core.domain.ObjectModel.PragmaModelOp;

/**
 * Provides functions for collectors that need to steal bits from the object header.
 *
 * @author Filip Pizlo
 *
 */
public interface GCBits extends Oop {
    String ID = "Lovm/core/services/memory/GCBits;";
    
    // ok, check this out.  this interface allows the GC to steal two bits
    // from the object header.  these two bits are in the form of an int
    // in the range 0..3.  when not set using setGCBits(), these bits are
    // guaranteed to remain 0.  in particular, when the blueprint is stamped
    // on a region of memory, the GC bits become 0.  this means that if the
    // collector wishes to set these bits to something other than 0 after
    // allocation, it must make sure that it calls setGCBits() after the
    // stamp method is called.

    int getGCBits() throws PragmaModelOp;
    
    void setGCBits(int bits) throws PragmaModelOp;
    
    /** Interface for getting at the GC bits given a VM_Address.  Use this
	when dealing with a non-object region of memory. */
    public interface Model {
	// these methods need to be PragmaNoPollcheck
	int getGCBits(VM_Address slot);
	void setGCBits(VM_Address slot,int bits);
    }
}

