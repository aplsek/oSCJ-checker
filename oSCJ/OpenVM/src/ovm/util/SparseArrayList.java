package ovm.util;

/**
 * An ArrayList of 2^31 - 1 elements where any element that has not
 * been explicitly set is null.  This ArrayList is implemented as a
 * simple array that may contain many null entries.
 **/
public class SparseArrayList extends ArrayList {
    public Object get(int idx) {
	return idx >= size ? null : super.get(idx);
    }

    public Object set(int idx, Object elt) {
	if (idx >= size) {
	    int newSz = Math.max(2*size, idx+1);
	    ensureCapacity(newSz);
	    size = newSz;
	}
	return super.set(idx, elt);
    }
}
