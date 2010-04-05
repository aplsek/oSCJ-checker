package ovm.core.repository;

import ovm.services.bytecode.JVMConstants;

/**
 * Implementation of an incremental ConstantPoolBuilder. This builder
 * builds a DiffConstantPool which is a constant pool that was extended
 * with a couple of additional entries.
 *
 * Note that some methods of the original ConstantPoolBuilder are not
 * implemented and that it is not possible to update entries of the
 * original constant pool either (you get an ArrayIndexOutOfBoundsException).
 *
 * @author Christian Grothoff
 * @see ovm.core.repository.ConstantPool
 **/
public class DiffConstantPoolBuilder
    extends ConstantPoolBuilder
    implements JVMConstants {
    static public boolean DEBUGFLAG = false;

    private final ConstantPool base_;

    //private ByteBuffer conv_ = ByteBuffer.allocate(8);

    /**
     * Create a new ConstantPoolBuilder. 
     **/
    public DiffConstantPoolBuilder(ConstantPool base) {
	super();
        this.base_ = base;
    }

    /**
     * Allocate a new index into the constant pool.
     * @return the new index.
     **/
    protected int allocateIndex() {
        int n = base_.getConstantCount() + tags_.length;
        reserveTagNumbers(n + 1);
        return n;
    }

    protected int put(int index, int tag, Object value) {
	int bc = base_.getConstantCount();
	if (index < bc)
	    throw new IllegalArgumentException();
	tags_[index - bc] = (byte) tag;
	values_[index - bc] = value;
	return index;
    }
    /**
     * Commit all changes made and return the completed constant pool.
     * @return the offset of the new entry
     **/
    public ConstantPool build() {
        return new DiffConstantPool(base_, tags_, values_);
    }

    public void realloc(int n) {
        throw new Error("can not realloc DiffConstantPoolBuilder (not implemented)");
    }

    /**
     * Make sure there is room for n constants.
     * @param n the new number of constants supported (0...n-1)
     **/
    public void reserveTagNumbers(int n) {
        n -= base_.getConstantCount();
        if (n <= tags_.length)
            return; // got enough space
        byte[] tags = tags_;
        Object[] values = values_;
        tags_ = new byte[n];
        values_ = new Object[n];
        System.arraycopy(tags, 0, tags_, 0, tags.length);
        System.arraycopy(values, 0, values_, 0, values.length);
    }

    public void reset() {
	if (tags_ != null)
	    throw new Error("can not reset DiffConstantPoolBuilder (not implemented)");
	tags_ = new byte[0];
	values_ = new Object[0];
    }

    /**
     * Test if a constant equal to "o" is already in the constant pool.
     * If yes, the respective index is returned. 
     * @return -1 if o was not found
     **/
    private int testPresent(Object o) {
        for (int i = 1; i < base_.getConstantCount(); i++) {
            Object o2 = base_.getConstantAt(i);
            if (o.equals(o2))
                return i;
        }
        for (int i = 0; i < tags_.length; i++)
            if (o.equals(values_[i]))
                return i + base_.getConstantCount();
        return -1;
    }

} // end of DiffConstantPoolBuilder
