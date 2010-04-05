package ovm.core.services.memory;

import ovm.core.domain.Blueprint;

/** separate interface for image allocation.  this is separated from
    MemoryManager to make it easier to have implementations of this
    stuff shared by multiple MMs */
public class ImageAllocator {
    
    /**
     * Allocate space for an object within the bootimage.
     * 
     * @param end       the first address after the current bootimage contents
     * @param size      the object's size
     * @param bp        the object's type
     * @param arrayLength if it is an array then this is the array length
     *
     * @return the object's address
     *
     * If this function returns a value &gt;= end, Ovm's notion of the
     * bootimage end is adjusted appropriately.
     * <p>
     * The default implementation of this method lays out the
     * bootimage as a contiguous sequence of objects without any
     * special alignment constraints and without segragating objects
     * by type.
     */
    public static int allocateInImage(int end,int size,Blueprint bp,int arrayLength) {
	return impl.allocateInImage(end,size,bp,arrayLength);
    }
    
    public static interface Implementation {
	public int allocateInImage(int end,int size,Blueprint bp,int arrayLength);
    }
    
    private static Implementation defaultImpl=
	new Implementation(){
	    public int allocateInImage(int end,int size,Blueprint bp,int arrayLength) {
		return end;
	    }
	};
    
    private static Implementation impl;
    
    public static Implementation the() {
	if (impl==null) {
	    return defaultImpl;
	} else {
	    return impl;
	}
    }
    
    /** a memory manager or engine or something else may call this to
	override the mechanism for image allocation. */
    public static void override(Implementation impl) {
	if (ImageAllocator.impl!=null) {
	    throw new Error("Cannot override ImageAllocator since it is "+
			    "already overridden.");
	}
	ImageAllocator.impl=impl;
    }
    
}


