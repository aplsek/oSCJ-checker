// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/async/U.java,v 1.1 2004/04/02 16:07:57 pizlofj Exp $

package ovm.services.io.async;

import ovm.core.services.memory.*;

/**
 * Stuff I do frequently.  ('U' stands for 'Util'.)
 * @author Filip Pizlo
 */
public class U {
    /** 'e' stands for 'enter' */
    public static VM_Area e(Object o) {
        return MemoryManager.the().setCurrentArea(
                   MemoryManager.the().areaOf(VM_Address.fromObject(o).asOop()));
    }
    
    /** 'ei' stands for 'enter immortal' */
    public static VM_Area ei() {
        return MemoryManager.the().setCurrentArea(MemoryManager.the().getImmortalArea());
    }
    
    /** 'l' stands for 'leave', which is the opposite of 'enter' */
    public static void l(VM_Area a) {
        MemoryManager.the().setCurrentArea(a);
    }
}

