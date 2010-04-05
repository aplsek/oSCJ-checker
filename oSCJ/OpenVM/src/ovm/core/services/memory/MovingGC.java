package ovm.core.services.memory; // SYNC PACKAGE NAME TO STRING BELOW

import ovm.core.domain.Oop;
import ovm.core.services.memory.VM_Address;
import ovm.core.domain.ObjectModel.PragmaModelOp;

/**
 * Provides functions for moving garbage collection algorithms.
 *
 * @author Phil McGachey
 *
 */
public interface MovingGC extends Oop.WithUpdate {
    String ID = "Lovm/core/services/memory/MovingGC;";
    
    /**
     * Stores an address which will be supplied by getForwardAddress
     */
    void markAsForwarded(VM_Address fwdaddr) throws PragmaModelOp;

    /**
     * Checks if the object has been marked as forwarded
     */
    boolean isForwarded() throws PragmaModelOp;
    
    /**
     * Returns the forwarded address of the object
     */
    VM_Address getForwardAddress() throws PragmaModelOp;
}
