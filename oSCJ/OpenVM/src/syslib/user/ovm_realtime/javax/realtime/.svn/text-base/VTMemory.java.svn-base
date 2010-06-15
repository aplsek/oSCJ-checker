package javax.realtime;

public class VTMemory extends ScopedMemory {
    public VTMemory(long initSize, long maxSize) {
	super(checkSize(initSize, maxSize));
    }
    public VTMemory(long initSize, long maxSize, Runnable logic) {
	super(checkSize(initSize, maxSize), logic);
    }
    public VTMemory(SizeEstimator initSize, SizeEstimator maxSize) {
	super(checkSize(initSize, maxSize));
    }
    public VTMemory(SizeEstimator initSize, SizeEstimator maxSize,
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
