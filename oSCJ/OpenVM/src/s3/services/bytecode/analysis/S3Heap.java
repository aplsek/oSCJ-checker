/**
 * @file s3/services/bytecode/verifier/Heap.java 
 **/
package s3.services.bytecode.analysis;

import ovm.services.bytecode.analysis.Heap;

/**
 * Trivial heap model (that models nothing).  In this heap model,
 * the heap is considered immutable.  It never changes.  
 *
 * @author Christan Grothoff
 **/
public class S3Heap implements Heap {

    /**
     * Is this heap equal to another heap?
     **/
    public boolean equals(Heap h) {
	return h==this;
    }

    /**
     * Does this heap abstraction cover the other
     * given heap abstraction (in the abstract domain)?
     **/
    public boolean includes(Heap h) {
	return h == this;
    }

    /**
     * What is the result of merging this heap abstraction
     * with the given heap abstraction?
     * 
     * @return null if the abstractions can not be merged
     **/
    public Heap merge(Heap h) {
	if (h == this)
	    return this;
	else
	    return null;
    }

    /**
     * Make a clone of this Heap for concurrent modification.
     * Used at braches.
     **/
    public Heap cloneHeap() {
	return this; // _stateless_ SINGLETON!
    }

    public String toString() {
	return "IMMUTABLE";
    }

    /**
     * Factory for Heaps.
     **/
    public static class Factory 
	implements Heap.Factory {

	private Heap singleton_ = new S3Heap();

	/**
	 * Create a new heap abstraction.  This method
	 * is called when the initial frame for the abstract
	 * execution of a method is created.
	 **/
	public Heap makeHeap() {
	    return singleton_;
	}

    } // end of S3Heap.Factory


} // end of S3Heap
