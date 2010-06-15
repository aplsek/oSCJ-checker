package ovm.services.memory.scopes; // SYNC PACKAGE NAME TO STRING BELOW

import ovm.core.domain.Oop;
import ovm.core.services.memory.VM_Area;
import ovm.core.domain.ObjectModel.PragmaModelOp;

/** Interface for Oops and models in which there is a scoped memory 
    pointer in the header.  Note, most memory managers that support
    scoped memory do not need this object model.
    @author Filip Pizlo
*/
public interface ScopePointer extends Oop {
    String ID = "Lovm/services/memory/scopes/ScopePointer;";
    
    VM_Area getScopePointer() throws PragmaModelOp;
    void setScopePointer(VM_Area a) throws PragmaModelOp;
    
    public interface Model {}
}


