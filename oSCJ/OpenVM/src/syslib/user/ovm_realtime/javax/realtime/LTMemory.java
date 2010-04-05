package javax.realtime;

public class LTMemory extends ScopedMemory {

    public LTMemory(long size) {
        super(size); // initSize == maxSize
    }
    public LTMemory(long initSize, long maxSize) {
	super(checkSize(initSize, maxSize));
    }
    public LTMemory(long initSize, long maxSize, Runnable logic) {
	super(checkSize(initSize, maxSize), logic);
    }
    
    public LTMemory(SizeEstimator size) {
        super(size); // initSize == maxSize
    }
    public LTMemory(SizeEstimator initSize, SizeEstimator maxSize) {
	super(checkSize(initSize, maxSize));
    }
    public LTMemory(SizeEstimator initSize, SizeEstimator maxSize,
		    Runnable logic) {
	super(checkSize(initSize, maxSize), logic);
    }

    static long checkSize(long initial, long max) {
        if (initial > max) 
            throw new IllegalArgumentException("initial greater than maximum");
        return max;
    }

    static SizeEstimator checkSize(SizeEstimator initial, SizeEstimator max) {
        if (initial.getEstimate() > max.getEstimate()) 
            throw new IllegalArgumentException("initial greater than maximum");
        return max;
    }

        
}
