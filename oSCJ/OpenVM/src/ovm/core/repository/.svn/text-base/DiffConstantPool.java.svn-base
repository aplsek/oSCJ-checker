package ovm.core.repository;


/**
 * Incremental constant pool.
 *
 * @see DiffConstantPoolBuilder
 * @author Christian Grothoff
 **/
public class DiffConstantPool extends ConstantPool {

    private final ConstantPool base_;

    /**
     * Create a new constant pool. <code>DiffConstantPool</code> are created
     * by <code>DiffConstantPoolBuilder</code>s, access to the constructor is
     * thus restricted to the current package.
     *
     * @param base the base of this extended constant pool
     * @param tags the tags of this constant pool
     * @param values the values of this constant pool
     **/
    DiffConstantPool(ConstantPool base, byte[] tags, Object[] values) {
        super(tags, values);
        this.base_ = base;
    }

    /**
     * Check that a constant pool entry has the required tag.  Throws an
     * AccessException if not satisfied.
     * @param expectedTag  tag of the CP entry.
     * @param offset the offset of the entry in the constant pool.
     **/
    public void checkTagAt(byte expectedTag, int offset)
        throws AccessException {
        if (offset < base_.getConstantCount())
            base_.checkTagAt(expectedTag, offset);
        else
            super.checkTagAt(expectedTag, offset - base_.getConstantCount());
    }

    void dumpConstantPool() {
        base_.dumpConstantPool();
        super.dumpConstantPool();
    }

    /**
     * Return the symbolic shared state object at the given index.
     * This is an OVM speciality and does not correspond to anything in the
     * JVM Spec.
     * @throws AccessException if the type of the constant does not match
     *         the type that is requested 
     **/
    public Binder getUnresolvedBinderAt(int cpIndex) throws AccessException {
        if (cpIndex < base_.getConstantCount())
            return base_.getUnresolvedBinderAt(cpIndex);
        else
            return super.getUnresolvedBinderAt(cpIndex - base_.getConstantCount());
    }

    // Return a builder based on this constant pool.
    public ConstantsEditor getBuilder() {
        return new DiffConstantPoolBuilder(this);
    }

    // Get the value of the constant stored at the given offset.
    // Primitive constants are wrapped appropriately.
    public Object getConstantAt(int offset) {
        if (offset < base_.getConstantCount())
            return base_.getConstantAt(offset);
        else
            return super.getConstantAt(offset - base_.getConstantCount());
    }

    // return the number of constants in this pool
    public int getConstantCount() {
        return super.getConstantCount() + base_.getConstantCount();
    }

    /**
     * Return the field selector at the given index.
     * @param cpIndex the index of the field selector to be retrieved
     * @return the field selector at the given index   
     * @throws AccessException if the type of the constant does not match
     *         the type that is requested 
     **/
    public ConstantFieldref getFieldrefAt(int cpIndex)
        throws AccessException {
        if (cpIndex < base_.getConstantCount())
            return base_.getFieldrefAt(cpIndex);
        else
            return super.getFieldrefAt(cpIndex - base_.getConstantCount());
    }

    /**
     * Return the method selector at the given index.
     * @throws AccessException if the type of the constant does not match
     *         the type that is requested 
     **/
    public ConstantMethodref getMethodrefAt(int cpIndex)
        throws AccessException {
        if (cpIndex < base_.getConstantCount())
            return base_.getMethodrefAt(cpIndex);
        else
            return super.getMethodrefAt(
                cpIndex - base_.getConstantCount());
    }

    /**
     * Return the symbolic shared state object at the given index.
     * This is an OVM speciality and does not correspond to anything in the
     * JVM Spec.
     * @throws AccessException if the type of the constant does not match
     *         the type that is requested 
     **/
    public TypeName.Gemeinsam getUnresolvedSharedStateAt(int cpIndex) throws AccessException {
        if (cpIndex < base_.getConstantCount())
            return base_.getUnresolvedSharedStateAt(cpIndex);
        else
            return super.getUnresolvedSharedStateAt(cpIndex - base_.getConstantCount());
    }

    // Return the string at the given index.  Please mind that strings from
    // the constant pool must be copied into the local domain (into the
    // class-object) before using (to avoid inter-domain interactions via
    // String syncs).
    public RepositoryString getUnresolvedStringAt(int cpIndex) {
        if (cpIndex < base_.getConstantCount())
            return base_.getUnresolvedStringAt(cpIndex);
        else
            return super.getUnresolvedStringAt(cpIndex - base_.getConstantCount());
    }

    /**
     * Return the tag of the entry at the given offset in the CP.
     * @param offset the offset of the entry in the constant pool.
     * @return tag of a CP entry.
     **/
    public byte getTagAt(int offset) {
        if (offset < base_.getConstantCount())
            return base_.getTagAt(offset);
        else
            return super.getTagAt(offset - base_.getConstantCount());
    }

    // package-scoped methods for the builder (needs to query a whole block)

    /**
     * Return the tag-array of this constant pool.
     * (used by the ConstantPoolBuilder when extending
     * this CP).
     **/
    public final byte[] getTags() {
        byte[] btags = base_.getTags();
        byte[] stags = super.getTags();
        byte[] rtags = new byte[btags.length + stags.length];
        System.arraycopy(btags, 0, rtags, 0, btags.length);
        System.arraycopy(stags, 0, rtags, btags.length, stags.length);
        return rtags;
    }

    // Return the typename at the given index.
    public ConstantClass getClassAt(int cpIndex)
        throws AccessException {
        if (cpIndex < base_.getConstantCount())
            return base_.getClassAt(cpIndex);
        else
            return super.getClassAt(cpIndex - base_.getConstantCount());
    }

    /**
     * Return the field descriptor at the given index.
     * @throws AccessException if the type of the constant does not match
     *         the type that is requested 
     **/
    public UnboundSelector.Field getUnboundFieldSelectorAt(int cpIndex)
        throws AccessException {
        if (cpIndex < base_.getConstantCount())
            return base_.getUnboundFieldSelectorAt(cpIndex);
        else
            return super.getUnboundFieldSelectorAt(
                cpIndex - base_.getConstantCount());
    }

    /**
     * Return the method descriptor at the given index.
     * @throws AccessException if the type of the constant does not match
     *         the type that is requested 
     **/
    public UnboundSelector.Method getUnboundMethodSelectorAt(int cpIndex)
        throws AccessException {
        if (cpIndex < base_.getConstantCount())
            return base_.getUnboundMethodSelectorAt(cpIndex);
        else
            return super.getUnboundMethodSelectorAt(
                cpIndex - base_.getConstantCount());
    }

    /**
     * Return the method descriptor at the given index.
     * @throws AccessException if the type of the constant does not match
     *         the type that is requested 
     **/
    public UnboundSelector getUnboundSelectorAt(int cpIndex)
        throws AccessException {
        if (cpIndex < base_.getConstantCount())
            return base_.getUnboundSelectorAt(cpIndex);
        else
            return super.getUnboundSelectorAt(
                cpIndex - base_.getConstantCount());
    }

    // Return the entry at the given offset in the CP.
    public int getValueAt(int cpIndex) throws AccessException {
        if (cpIndex < base_.getConstantCount())
            return base_.getValueAt(cpIndex);
        else
            return super.getValueAt(cpIndex - base_.getConstantCount());
    }

    /**
     * Return the values-array of this constant pool.  Used by the
     * ConstantPoolBuilder when extending this CP.
     **/
    public final Object[] getValues() {
        Object[] bvalues = base_.getValues();
        Object[] svalues = super.getValues();
        Object[] rvalues = new Object[bvalues.length + svalues.length];
        System.arraycopy(bvalues, 0, rvalues, 0, bvalues.length);
        System.arraycopy(svalues, 0, rvalues, bvalues.length, svalues.length);
        return rvalues;
    }

    public long getWideValueAt(int cpIndex) throws AccessException {
        if (cpIndex < base_.getConstantCount())
            return base_.getWideValueAt(cpIndex);
        else
            return super.getWideValueAt(cpIndex - base_.getConstantCount());
    }

} // end of DiffConstantPool
