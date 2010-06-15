package ovm.core.services.memory; // SYNC PACKAGE NAME TO STRING BELOW

import ovm.core.domain.Oop;
import ovm.core.services.memory.VM_Address;
import ovm.core.domain.ObjectModel.PragmaModelOp;

public interface GCData extends Oop {
    String ID = "Lovm/core/services/memory/GCData;";
    
    int getColor() throws PragmaModelOp;
    void setColor(int color) throws PragmaModelOp;
    
    int getOld() throws PragmaModelOp;
    void markOld() throws PragmaModelOp;    
    
    /** Interface for getting at the GC bits given a VM_Address.  Use this
	when dealing with a non-object region of memory. */
    public interface Model {
	// these methods need to be PragmaNoPollcheck
	int getColor(VM_Address slot);
	void setColor(VM_Address slot,int color);
	int getOld(VM_Address slot);
	void markOld(VM_Address slot);
    }
}

