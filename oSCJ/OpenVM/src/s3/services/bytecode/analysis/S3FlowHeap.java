/**
 * @file s3/services/bytecode/analysis/S3FlowHeap.java 
 **/
package s3.services.bytecode.analysis;

import ovm.services.bytecode.analysis.Heap;
import ovm.util.HTint2Object;

/**
 * Trivial heap model (that models nothing).  In this heap model,
 * the heap is considered immutable.  It never changes.  
 *
 * @author Christan Grothoff
 **/
public class S3FlowHeap 
    implements Heap {

    private static S3FlowHeap[] 
	EMPTY_ARR = new S3FlowHeap[0];

    /**
     * List of all heaps that can directly
     * follow this one. 
     */
    private S3FlowHeap[] successors_;

    /**
     * The per-method intern map of all Heaps.
     * The internMap is *shared* between all
     * heaps of the same method.
     */
    private final HTint2Object internMap_;

    private S3FlowHeap(HTint2Object internMap) {
	this.internMap_ = internMap;
	this.successors_ = EMPTY_ARR;
    }

    /**
     * Make a successor heap for the given pc.
     * @param pc the pc of the successor heap, used for
     *    interning heaps of the same method to avoid
     *    unbounded recursions.
     */
    public S3FlowHeap makeSuccessor(int pc) {
	Object o = internMap_.get(pc);
	if (o != null) {
	    S3FlowHeap f = (S3FlowHeap)o;
	    addSuccessor(f);
	    return f;
	} else {
	    S3FlowHeap f = new S3FlowHeap(internMap_);
	    internMap_.put(pc, f);
	    addSuccessor(f);
	    return f;
	}
    }    

    public S3FlowHeap[] getSuccessors() {
	return successors_;
    }

    private void addSuccessor(S3FlowHeap fh) {    
	S3FlowHeap[] tmp
	    = new S3FlowHeap[successors_.length+1];
	System.arraycopy(successors_, 0,
			 tmp, 0,
			 successors_.length);
	tmp[successors_.length] = fh;
	successors_ = tmp;
    }

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
	return this; // we're immutable (except for successors, but that's ok)
    }

    public String toString() {
	return "FLOWHEAP";
    }

    /**
     * Factory for Heaps.
     **/
    public static class Factory 
	implements Heap.Factory {

	/**
	 * Create a new heap abstraction.  This method
	 * is called when the initial frame for the abstract
	 * execution of a method is created.
	 **/
	public Heap makeHeap() {
	    return new S3FlowHeap(new HTint2Object(64));
	}

    } // end of S3FlowHeap.Factory


} // end of S3FlowHeap
