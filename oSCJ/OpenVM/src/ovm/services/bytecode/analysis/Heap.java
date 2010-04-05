/**
 * @file ovm/services/bytecode/analysis/Heap.java 
 **/
package ovm.services.bytecode.analysis;


/**
 * Interface for an abstract model of the heap.
 *
 * @author Christan Grothoff
 **/
public interface Heap {

    /**
     * Is this heap equal to another heap?
     **/
    public boolean equals(Heap h);

    /**
     * Does this heap abstraction cover the other
     * given heap abstraction (in the abstract domain)?
     **/
    public boolean includes(Heap h);

    /**
     * What is the result of merging this heap abstraction
     * with the given heap abstraction?
     * 
     * @return null if the abstractions can not be merged
     **/
    public Heap merge(Heap h);

    /**
     * Make a clone of this Heap for concurrent modification.
     * Used at braches.
     **/
    public Heap cloneHeap();

    /**
     * Factory for Heaps.
     **/
    public interface Factory {

	/**
	 * Create a new heap abstraction.  This method
	 * is called when the initial frame for the abstract
	 * execution of a method is created.
	 **/
	public Heap makeHeap();

    } // end of Heap.Factory


} // end of Heap
