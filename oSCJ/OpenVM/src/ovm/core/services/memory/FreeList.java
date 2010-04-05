package ovm.core.services.memory;

/** Interface for object models to follow if they want to allow free
    objects to have prev/next pointers for free list management.

    NOTE: currently every object model supports this. */
public interface FreeList {
    
    VM_Address getPrev(VM_Address slot);
    void setPrev(VM_Address slot,VM_Address prev);
    
    VM_Address getNext(VM_Address slot);
    void setNext(VM_Address slot,VM_Address next);
    
    void setCustom(VM_Address slot, VM_Address value);
    VM_Address getCustom(VM_Address slot);
}


