package javax.realtime;

import java.util.BitSet;

import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;


/**
 * This class is the API for all processor-affinity-related aspects of SCJ.
 * 
 * @author plsek
 *
 */
@SCJAllowed(Level.LEVEL_2)
public final class AffinitySet {

	/**
	 * Returns an AffinitySet representing a subset of the processors in the system.
	 *  The re- turned object may be dynamically created in the current memory area 
	 *  or preallocated in immortal memory.
	 * 
	 * Throws NullPointerException if bitSet is null, 
	 * and java.lang.IllegalArgumentException if bitSet is not a valid set of 
	 * processors.
	 *
	 * 
	 * @param bitSet
	 * @return
	 */
	@SCJAllowed(Level.LEVEL_2)
	public static AffinitySet generate(BitSet bitSet) {
		return null;
	}
	
	/**
	 * 
	 * @param handler
	 * @return Returns an AffinitySet representing the set of processors on 
	 * which handler can be scheduled. The returned object may be dynamically 
	 * created in the current memory area or preallocated in immortal memory.
	 *
	 *  @throws NullPointerException if handler is null. @SCJAllowed(LEVEL 2)
     *	
	 */
	/*@SCJAllowed(Level.LEVEL_1)*/ 
	public static AffinitySet getAffinitySet(BoundAsyncEventHandler handler) {
		return null;
	}
	
	/**
	 * 
	 * @param thread
	 * @return Returns an AffinitySet representing the set of processors on which
	 *  thread can be scheduled. The returned object may be dynamically created 
	 *  in the current memory area or preallocated in immortal memory.
	 *  
	 *  Throws NullPointerException if thread is null.
	 */
	/*@SCJAllowed(Level.LEVEL_2)*/
	public static AffinitySet getAffinitySet(Thread thread) {
		return null;
		
	}
	
	/**
	 * Equivalent to getAvailableProcessors(BitSet dest) with a null argument.
	 * 
	 * @return
	 */
	@SCJAllowed(Level.LEVEL_1)
	public static BitSet getAvailableProcessors() {	
		return null;
	}
	
	/**
	 * 
	 * @param dest
	 * @return Returns the set of processors available to the SCJ application either in dest, or if dest is null, the returned object may be dynamically created in the current memory area or preallocated in immortal memory.
	 *
	 */
	@SCJAllowed(Level.LEVEL_1)
	public static BitSet getAvailableProcessors(BitSet dest) {	
		return null;
	}
	
	/**
	 * Returns the default AffinitySet representing the set of processors on which no-heap schedulable objects can be scheduled. The returned object may be dynamically cre- ated in the current memory area or preallocated in immortal memory.
	 *
	 * @return
	 */
	/*@SCJAllowed(Level.LEVEL_1) */
	public static AffinitySet getNoHeapSoDefaultAffinity() {	
		return null;
	}
	
	/**
	 * 
	 * @return Returns the size of the predefined affinity sets.
	 *
	 */
	/*@SCJAllowed(Level.LEVEL_1) */
	public static int getPredefinedAffinitySetCount()  {
		return 0;
	}
	
	/**
	 * Equivalent to getPredefinedAffinitySets(AffinitySet[] dest) with a null argument.
	 *
	 */
	/*@SCJAllowed(Level.LEVEL_1)*/
	public static AffinitySet[] getPredefinedAffinitySets() {
		return null;
	}
	
	/**
	 * 
	 * @Returns an array of predefined AffinitySet, either in dest, 
	 * or if dest is null, the returned object may be dynamically created in the
	 *  current memory area or preallocated in immortal memory.
	 *  
	 * @Throws java.lang.IllegalArgumentException if dest is not large
	 *  enough to hold the set.
	 * 
	 * @param dest
	 * @return
	 */
	/*@SCJAllowed(Level.LEVEL_1)*/
	public static AffinitySet[] getPredefinedAffinitySets(AffinitySet[] dest) {
		return null;
	}
	
	/**
	 * Set the set of processors on which aeh can be scheduled to that represented by set.
	 */
	/*@SCJAllowed(Level.LEVEL_1) */
	public static void setProcessorAffinity(AffinitySet set, BoundAsyncEventHandler handler) {
	}
	
	/**
	 * Set the set of processors on which thread can be scheduled to that represented
	 *  by set.
	 *  
	 *  @Throws ProcessorAffinityException if set is not a valid processor set, 
	 *  and NullPointer- Exception if thread is null
	 * @param set
	 * @param thread
	 */
	@SCJAllowed(Level.LEVEL_2)
	public static void setProcessorAffinity(AffinitySet set, Thread thread) {
	}
	
	
	/**
	 * METHODS
	 * 
	 */
	
	/**
	 * Equivalent to getProcessors(BitSet dest) with a null argument.
	 */
	/*@SCJAllowed(Level.LEVEL_1) */
	public final BitSet getBitSet() {
		return null;
	}
	
	/**
	 * Returns the set of processors associated with this Affinity set, either in dest, or if dest is null, the returned object may be dynamically created in the current memory area or preallocated in immortal memory.
	 */
	@SCJAllowed(Level.LEVEL_1)
	public final BitSet getProcessors(BitSet dest) {
		return null;
	}
	
	/**
	 * Returns true if if and only if the processorNumber is in this affinity set.
	 */
	/*@SCJAllowed(Level.LEVEL_1) */ 
	public final boolean isProcessorInSet(int processorNumber) {
		return false;
	}
	
}
