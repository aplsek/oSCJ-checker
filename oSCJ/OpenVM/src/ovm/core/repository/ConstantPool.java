/**
 * Implementation of a constant pool.
 * @file ovm/core/repository/ConstantPool.java
 **/
package ovm.core.repository;

import ovm.services.bytecode.JVMConstants;
import ovm.services.bytecode.reader.ByteCodeConstants;
import ovm.util.ByteBuffer;
import s3.core.S3Base;

/**
 * A constant pool with full type information for the constants.
 * ConstantPools NEVER contain resolved Strings, SharedStates or
 * invisible stitcher generated Singetons.  They always contain
 * unresolved, unlinked symbolic representations of these special
 * entries as represented by TypeName.Gemeinsam, RepositoryString and
 * Binder.
 *
 * This is an implementation of the Constant Pool as specified
 * in JVM Spec $4.4. A Constant Pool is a table of symbolic
 * information. Each entry consists of a tag/information pair: the tag
 * indicates what constant type is being referred to, and its
 * information is constant-specific information which varies depending
 * upon the tag. These tags are located in {@link
 * ovm.services.bytecode.JVMConstants}, and the information that must
 * be associated with each entry type is described in the JVM
 * Specification as mentioned above.
 * 
 * This implementation does not require that constants be sorted by
 * type (e.g. primitives, strings, etc.). We wrap primitives in the
 * respective wrapper classes.
 *
 * The ConstantPool supports OVM extensions such as SharedStates and
 * Binders, but may not store resolved entities.
 *
 * @author Christian Grothoff
 * @see s3.core.domain.S3Constants
 **/
public class ConstantPool extends S3Base 
    implements Constants, JVMConstants {

    /**
     * For every entry in this constant pool, we keep its tag.  Tags are
     * constants which indicate the type of constant pool entry that
     * follows. See JVM Spec 4.4.
     **/
    private final byte[] tags_;

    /**
     * For every entry in this Constant Pool, the result of parsing is an
     * object stored in this array.
     **/
    private final Object[] values_;

    /**
     * Create a new constant pool. <code>ConstantPool</code> are created
     * by <code>ConstantPoolBuilder</code>s, access to the constructor is
     * thus restricted to the current package.
     * @param tags the tags of this constant pool
     * @param values the values of this constant pool
     **/
    ConstantPool(byte[] tags, Object[] values) {
        this.tags_ = tags;
        this.values_ = values;
    }

    /**
     * Check that a constant pool entry has the required tag.  
     * Throws an <code>Constants.AccessException</code> if not.
     * @param expectedTag  tag of the CP entry.
     * @param offset the offset of the entry in the constant pool.
     **/
    public void checkTagAt(byte expectedTag, int offset)
        throws Constants.AccessException {
        if (tags_[offset] != expectedTag) {
            String message =
                "Invalid Constant Pool Entry "
                    + ByteCodeConstants.CONSTANT_NAMES[tags_[offset]]
                    + " instead of "
                    + ByteCodeConstants.CONSTANT_NAMES[expectedTag]
                    + ", CP index "
                    + offset
                    + ", was "
                    + getContents(offset);
            throw new Constants.AccessException(message);
        }
    }

    void dumpConstantPool() {
        for (int i = 1; i < values_.length; i++) {
            d(i + ": " + getContents(i));
        }
    }

    /**
     * Return the Binder object at the given index.
     * This is an OVM speciality and does not correspond to anything in the
     * JVM Spec.
     * @throws Constants.AccessException if the type of the constant does not match
     *         the type that is requested 
     **/
    public Binder getUnresolvedBinderAt(int cpIndex) throws Constants.AccessException {
        try {
            assert(values_[cpIndex] != null);
            return (Binder) values_[cpIndex];
        } catch (ClassCastException e) {
            throw new Constants.AccessException("At CP index " + cpIndex + ": " + e);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new Constants.AccessException(
				      "Invalid CP index " + cpIndex + " >= " + values_.length);
        }
    }
    
    /**
     * Return a builder based on this constant pool.
     * @return a <code>ConstantPool.Builder</code> for this
     *         constant pool
     */
    public ConstantsEditor getBuilder() {
        return new ConstantPoolBuilder(this);
    }
    
    /**
     * Get the value of the constant stored at the given offset.
     * Primitive constants are wrapped appropriately.
     * @param offset the constant pool offset of the constant to be
     *               retrieved
     * @return the value of the constant stored at the given offset
     **/
    public Object getConstantAt(int offset) {
        if ((offset >= 0)
            && (offset < values_.length)
            && ((offset > 0) || (tags_[offset] != 0)))
            return values_[offset];
        // offset 0 is ok if tags_[0] != 0 because then we
        // are the upper part of a diff-constant pool!
        throw failure(
            "Illegal tag for access to getConstantAt: "
                + offset
                + " valid range is [1,"
                + values_.length
                + "[");
    }

    /**
     * Get the number of constants in this constant pool
     * @return the number of constants in this pool
     */
    public int getConstantCount() {
        return values_.length;
    }

    /**
     * Return the field reference at the given index.
     * See JVM Spec 4.4.2.
     * @param cpIndex the index of the field selector to be retrieved
     * @return the field reference at the given index   
     * @throws AccessException if the type of the constant does not match
     *         the type that is requested 
     **/
    public ConstantFieldref getFieldrefAt(int cpIndex) 
	throws Constants.AccessException {
	try {
	    return (ConstantFieldref) values_[cpIndex];
	} catch (ClassCastException e) {
	    throw new Constants.AccessException("At CP index " + cpIndex + ": " + e);
	} catch (ArrayIndexOutOfBoundsException e) {
	    throw new Constants.AccessException(
				      "Invalid CP index " + cpIndex + " >= " + values_.length);
	}
    }
    
    /**
     * Return the method reference at the given index.
     * See JVM Spec 4.4.2.
     * @param cpIndex the index of the method selector to be retrieved
     * @return the method reference at the given index   	 
     * @throws AccessException if the type of the constant does not match
     *         the type that is requested 
     **/
    public ConstantMethodref getMethodrefAt(int cpIndex)
	throws Constants.AccessException {
	try {
	    return (ConstantMethodref) values_[cpIndex];
	} catch (ClassCastException e) {
	    throw new Constants.AccessException("At CP index " + cpIndex + ": " + e);
	} catch (ArrayIndexOutOfBoundsException e) {
	    throw new Constants.AccessException(
				      "Invalid CP index " + cpIndex + " >= " + values_.length);
	}
    }


    /**
     * Return the symbolic shared state object at the given index.
     * This is an OVM speciality and does not correspond to anything in the
     * JVM Spec.
     * @param cpIndex the index of the symbolic shared state object to 
     *                be retrieved
     * @return the shared state object at the given index
     * @throws Constants.AccessException if the type of the constant does not match
     *         the type that is requested 
     **/
    public TypeName.Gemeinsam getUnresolvedSharedStateAt(int cpIndex) 
	throws Constants.AccessException {
	try {
	    assert(values_[cpIndex] != null);
	    return (TypeName.Gemeinsam) values_[cpIndex];
	} catch (ClassCastException e) {
	    throw new Constants.AccessException("At CP index " + cpIndex + ": " + e);
	} catch (ArrayIndexOutOfBoundsException e) {
	    throw new Constants.AccessException(
				      "Invalid CP index " + cpIndex + " >= " + values_.length);
	}
    }

    /**
     * Return the string at the given index.
     * Please mind that strings from the constant pool must
     * be copied into the local domain (into the class-object)
     * before using (to avoid inter-domain interactions via
     * String syncs). 
     * See JVM Spec 4.4.3.
     * @param cpIndex the offset of the desired String in the constant pool
     * @return the String at this index
     */
    public RepositoryString getUnresolvedStringAt(int cpIndex) {
	return (RepositoryString) values_[cpIndex];
    }
    
    /**
     * Return the class info at the given index.
     * See JVM Spec 4.4.1.
     * @param cpIndex the index of the ConstantClass to be retrieved
     * @return the ConstantClass at the given index
     * @throws Constants.AccessException if the type of the constant does not match
     *         the type that is requested 
     **/
    public ConstantClass getClassAt(int cpIndex)
	throws Constants.AccessException {
	try {
	    return (ConstantClass) values_[cpIndex];
	} catch (ClassCastException e) {
	    throw new Constants.AccessException("At CP index " + cpIndex + ": " + e);
	} catch (ArrayIndexOutOfBoundsException e) {
	    throw new Constants.AccessException(
				      "Invalid CP index " + cpIndex + " >= " + values_.length);
	}
    }
    
    /**
     * Return the UnboundSelector at the given index.
     * @throws Constants.AccessException if the type of the constant does not match
     *         the type that is requested 
     **/
    public UnboundSelector getUnboundSelectorAt(int cpIndex)
	throws Constants.AccessException {
	try {
	    assert(values_[cpIndex] != null);
	    return (UnboundSelector) values_[cpIndex];
	} catch (ClassCastException e) {
	    throw new Constants.AccessException("At CP index " + cpIndex + ": " + e);
	} catch (ArrayIndexOutOfBoundsException e) {
	    throw new Constants.AccessException(
				      "Invalid CP index " + cpIndex + " >= " + values_.length);
	}
    }
    
    
    /**
     * Return the unbound field selector at the given index.
     * See JVM Spec 4.4.6
     * @param cpIndex the index of the unbound field selector to 
     *                be retrieved
     * @throws Constants.AccessException if the type of the constant does not match
     *         the type that is requested 
     **/
    public UnboundSelector.Field getUnboundFieldSelectorAt(int cpIndex)
	throws Constants.AccessException {
	try {
	    assert(values_[cpIndex] != null);
	    return (UnboundSelector.Field) values_[cpIndex];
	} catch (ClassCastException e) {
	    throw new Constants.AccessException("At CP index " + cpIndex + ": " + e);
	} catch (ArrayIndexOutOfBoundsException e) {
	    throw new Constants.AccessException(
				      "Invalid CP index " + cpIndex + " >= " + values_.length);
	}
    }
    
    /**
     * Return the unbound method selector at the given index.
     * See JVM Spec 4.4.6
     * @param cpIndex the index of the unbound method selector to 
     *                be retrieved
     * @return the desired unbound method selector
     * @throws Constants.AccessException if the type of the constant does not match
     *         the type that is requested 
     **/
    public UnboundSelector.Method getUnboundMethodSelectorAt(int cpIndex)
	throws Constants.AccessException {
	
	try {
	    assert(values_[cpIndex] != null);
	    return (UnboundSelector.Method) values_[cpIndex];
	} catch (ClassCastException e) {
	    throw new Constants.AccessException("At CP index " + cpIndex + ": " + e);
	} catch (ArrayIndexOutOfBoundsException e) {
	    throw new Constants.AccessException(
				      "Invalid CP index " + cpIndex + " >= " + values_.length);
	}
    }

    /**
     * Return the UTF index of the string at the given index.
     * See JVM Spec 4.4.7.
     * @param cpIndex the index of the utf8 string to be retrieved
     * @return the UTF8 index of the desired string
     * @throws Constants.AccessException if the type of the constant does not match
     *         the type that is requested 
     **/
    public int getUtf8IndexAt(int cpIndex) throws Constants.AccessException {
	try {
	    return ((UTF8Index) values_[cpIndex]).getIndex();
	} catch (ClassCastException cce) {
	    throw new Constants.AccessException(
				      cpIndex
				      + " is not a UTF index: "
				      + values_[cpIndex]
				      + " is of type "
				      + values_[cpIndex].getClass());
	} catch (ArrayIndexOutOfBoundsException aeiob) {
	    throw new Constants.AccessException("constant pool index invalid");
	} catch (NullPointerException npe) {
	    throw new Constants.AccessException("no valid entry in slot " + cpIndex);
	}
    }
    
    /**
     * Return the int or float constant value of the entry in the
     * constant pool. floats are typed as "int" for convenience.
     * See JVM Spec 4.4.4.
     * @param cpIndex the offset of the entry in the constant pool.
     * @return        the entry.
     * @throws        Constants.AccessException if cpIndex is out of bounds.
     **/
    public int getValueAt(int cpIndex) throws Constants.AccessException {
	if ((cpIndex < 0) || (cpIndex >= tags_.length))
	    throw new Constants.AccessException("CP index " + cpIndex + " out of bounds");
	byte t = tags_[cpIndex];
	switch (t) {
	case CONSTANT_Long :
	case CONSTANT_Double :
	    throw new Constants.AccessException("use getWideValueAt for that");
	case CONSTANT_Integer :
	    return ((Integer) values_[cpIndex]).intValue();
	case CONSTANT_Float :
	    return floatBitsToInt(((Float) values_[cpIndex]).floatValue());
	default :
	    throw new Constants.AccessException("getValueAt only works for int or float!");
	}
    }
    
    /**
     * Return the double or long entry at the given offset in the CP.
     * doubles are typed as long for convenience. 
     * See JVM Spec 4.4.5.
     * @param cpIndex the offset of the entry in the constant pool.
     * @return the entry.
     * @throws Constants.AccessException if cpIndex is out of bounds.
     **/
    public long getWideValueAt(int cpIndex) throws Constants.AccessException {
	if ((cpIndex < 0) || (cpIndex >= tags_.length))
	    throw new Constants.AccessException("CP index " + cpIndex + " out of bounds");
	byte t = tags_[cpIndex];
	switch (t) {
	case CONSTANT_Integer :
	case CONSTANT_Float :
	    throw new Constants.AccessException("use getValueAt for that");
	case CONSTANT_Long :
	    return ((Long) values_[cpIndex]).longValue();
	case CONSTANT_Double :
	    return doubleBitsToLong(((Double) values_[cpIndex]).doubleValue());
	default :
	    throw new Constants.AccessException("getWideValueAt only works for long or double!");
	}
    }
    
    /**
     * Return the tag of the entry at the given offset in the constant pool.
     * A tag is a constant that indicates the kind of constant pool
     * entry that will follow. See JVM Spec $4.4.
     * @param offset the offset of the entry in the constant pool.
     * @return tag of a CP entry.
     */
    public byte getTagAt(int offset) {
	return tags_[offset];
    }

    /* ************************ debugging support ****************** */

    /**
     * Debug method. Get the contents of the constant pool at index
     * <code>cpIndex</code> as a <code>String</code>.
     */
    String getContents(int cpIndex) {
        String contents = "\"(" + tags_[cpIndex] + ")";
        switch (tags_[cpIndex]) {
            case CONSTANT_Utf8 :
                contents += "Utf8: " + values_[cpIndex];
                break;
            case CONSTANT_NameAndType :
                contents += "NameAndType";
                break;
            case CONSTANT_Integer :
                contents += "INTEGER: " + values_[cpIndex];
                break;
            case CONSTANT_Float :
                contents += "FLOAT:" + values_[cpIndex];
                break;
            case CONSTANT_Double :
                contents += "DOUBLE" + values_[cpIndex];
                cpIndex++;
                break;
            case CONSTANT_Long :
                contents += "LONG" + values_[cpIndex];
                cpIndex++;
                break;
            case 0 :
                contents += "<EMPTY>";
                break;
            default :
                contents += "OTHER"
                    + values_[cpIndex]
                    + ", "
                    + values_[cpIndex].getClass();
        }
        return contents + "\"";
    }

    /* ******************** ConstantPoolBuilder support **************** */

    /**
     * Return the tag-array of this constant pool.
     * (used by the <code>ConstantPoolBuilder</code> when extending
     * this CP).
     * @return the tag-array of this constant pool
     */
    public byte[] getTags() {
        return (byte[]) tags_.clone();
    }

    /**
     * Return the values-array of this constant pool.  Used by the
     * ConstantPoolBuilder when extending this CP.
     **/
    public Object[] getValues() {
        return (Object[]) values_.clone();
    }


    /**
     * Wrapper class around an index into the Repository UTF8 table.
     * We may want to move this class up to the ovm.core.repository
     * level at some point (if we want to type UTF8 indices throughout
     * the VM; for now, they are just typed in the ConstantPool
     * code).
     *
     * @author Christian Grothoff
     **/
    public static class UTF8Index extends S3Base {

        private final int idx;

        public UTF8Index(int index) {
            this.idx = index;
            assert(index != 0);
        }

        public boolean equals(Object o) {
            if (o == null)
                return false;
            try {
                return (idx == ((UTF8Index) o).idx);
            } catch (ClassCastException cce) {
                return false;
            }
        }

        public int getIndex() {
            return idx;
        }

        public int hashCode() {
            return idx;
        }

        public String toString() {
            return "Index into Repository UTF table "
                + idx
                + " with value >>"
                + ovm.core.repository.RepositoryUtils.utfIndexAsString(idx)
                + "<<";

        }

    } // end of ConstantPool.UTF8Index

    public static int floatBitsToInt(float f) {
	ByteBuffer conv_ = ByteBuffer.allocate(4);
	conv_.putFloat(0, f);
	return conv_.getInt(0);
    }
    
    public static long doubleBitsToLong(double d) {
	ByteBuffer conv_ = ByteBuffer.allocate(8);
	conv_.putDouble(0, d);
	return conv_.getLong(0);
    }

} // end of ConstantPool
