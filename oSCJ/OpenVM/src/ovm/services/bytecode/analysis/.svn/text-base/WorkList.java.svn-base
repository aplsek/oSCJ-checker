package ovm.services.bytecode.analysis;

/**
 * Worklist of abstract states (frames and pcs).
 * @author Christian Grothoff, jv
 */
public final class WorkList {
    
    Entry first;

    Entry cache = null; // size 1 pool of Entry object

    private final Initializer init;
    
    public WorkList(Initializer init) {
	this.init = init;
	first = null; 
    }
    public String toString() {
	return "WORKLIST[\n" + first + "]\n";
    }
    /**
     * Add an entry to the worklist.
     * @param pc the program counter, >= 0
     * @param f the frame, may not be null
     * @param merge should we merge?
     */
    public final void add(int pc, 
			  Frame f, 
			  int merge) {
	if (cache != null) {
	    cache.pc = pc;
	    cache.tryMerge = merge;
	    cache.frame = f;
	    cache.next = first;
	    first = cache;
	    cache = null;
	} else 
	    first = new Entry(pc, f, merge, first);
    }

    /** add a work only if the same work is not in the list -HY */
    public final void addIfNew(int pc,
			       Frame f,
			       int merge) {
	Entry e = first;
	while(e != null) {
	    if (e.pc == pc
		&& e.frame.compareFrames(f)
		&& e.tryMerge == merge) {
		return; // there is a duplicate in the list
	    }
	    e = e.next;
	}
	add(pc, f, merge);
    }

    private boolean isFirst = true;

    public final boolean hasNext() {
	return (first != null) || isFirst;
    }
    
    public final Entry remove() {
	if (isFirst) {
	    add(0, 
		init.makeInitialFrame(),
		NOMERGE);
	    isFirst = false;
	}
	if (cache == null)
	    cache = first;
	Entry res = first;
	first = first.next;
	res.next = null;
	return res;
    }
    
    public interface Initializer {
	public Frame makeInitialFrame(); 	
    }
    
    public static final int NOMERGE = 0;
    public static final int MERGE = 2;
    public static final int MERGECLONE = 3;



    // Use MERGE with extreme (!) caution. If in doubt, use the slightly  less efficient 
    // MERGECLONE. If you use MERGE, you *must* guarantee that all aliased frames (which
    // all have typically MERGECLONE set) are processed before the MERGE frame is modified. 
    // Thus you need to take  the order of processing in the Worklist into account.
    
    /**
     * Each  worklist entry keeps the following information:
     * <ul>
     *  <li>the PC of the abstract interpreter for the next instruction</li>
     *  <li>the frame (abstract state of operand stack and local variables)
     *      at the current point of exeuction</li>
     *  <li>after "next()" was called, the Instruction object representing
     *      the Instruction at the given PC</li>
     *  <li>the next Entry in the worklist</li>
     *  <li>a flag indicating if the Driver should try to merge the frame
     *      with the current State before running the instruction. The
     *      flag can have 3 values. NOMERGE for not merging at all,
     *      MERGE for simple merging and MERGECLONE for merging with
     *      the requirement to make a copy of the frame before changing it
     *      (for aliased frames). The flag is set by the MergeStrategyVisitor.</li>
     * </ul>
     * 
     * @see s3.services.bytecode.analysis.FixpointIterator.MergeStrategyVisitor
     * @author Christian Grothoff, jv
     **/
    public static class Entry {
	public int tryMerge;
	public int pc;
	public Frame frame;
	public Entry next;

        Entry(int pc, Frame f, int merge, Entry next) {
            this.pc = pc;
            this.frame = f;
            this.tryMerge = merge;
            this.next = next;
        }
        public String toString() {
            String result = "At PC " + pc + 
		" is " + frame.toString();
            result += " (" + tryMerge + ")\n";
            return result + ((next != null) ? next.toString() : "");
        }

    } // end of Worklist.Entry


} // end of WorkList
