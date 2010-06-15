package ovm.core.repository;

import ovm.util.OVMError;

/**
 * Interface for accessing common constant pool features shared
 * between the Repository ConstantPool and the domain specific
 * S3Constants.
 *
 * @author Christian Grothoff
 */
public interface Constants {

    /**
     * Check that a constant pool entry has the required tag.  
     * Throws an <code>AccessException</code> if not.
     * @param expectedTag  tag of the CP entry.
     * @param offset the offset of the entry in the constant pool.
     **/
    public void checkTagAt(byte expectedTag, int offset);
    
    /**
     * Return the int or float constant value of the entry in the
     * constant pool.  floats are typed as "int" for convenience.
     * See JVM Spec 4.4.4.
     * @param cpIndex the offset of the entry in the constant pool.
     * @return        the entry.
     * @throws        ConstantPool.AccessException if cpIndex is out of bounds.
     **/
    public int getValueAt(int cpIndex);
    
    /**
     * Return the tag of the entry at the given offset in the constant pool.
     * A tag is a constant that indicates the kind of constant pool
     * entry that will follow. See JVM Spec $4.4.
     * @param index the offset of the entry in the constant pool.
     * @return      tag of a CP entry.
     */
    public byte getTagAt(int index);
    
    /**
     * Return the double or long entry at the given offset in the CP.
     * doubles are typed as long for convenience. 
     * See JVM Spec 4.4.5.
     * @param cpIndex the offset of the entry in the constant pool.
     * @return the entry.
     * @throws AccessException if cpIndex is out of bounds.
     **/
    public long getWideValueAt(int cpIndex);

    /**
     * Get the number of constants in this constant pool
     * @return the number of constants in this pool
     */
    public int getConstantCount();

    /**
     * Return the field reference at the given index.
     * See JVM Spec 4.4.2.
     * @param cpIndex the index of the field selector to be retrieved
     * @return the field reference at the given index   
     * @throws AccessException if the type of the constant does not match
     *         the type that is requested 
     **/
    public ConstantFieldref getFieldrefAt(int cpIndex) 
	throws AccessException;

    /**
     * Return the method reference at the given index.
     * See JVM Spec 4.4.2.
     * @param cpIndex the index of the method selector to be retrieved
     * @return the method reference at the given index   	 
     * @throws AccessException if the type of the constant does not match
     *         the type that is requested 
     **/
    public ConstantMethodref getMethodrefAt(int cpIndex)
	throws AccessException;
     

    /**
     * Get the value of the constant stored at the given offset.
     * Primitive constants are wrapped appropriately.
     *
     * @param offset the constant pool offset of the constant to be
     *               retrieved
     * @return the value of the constant stored at the given offset
     **/
    public Object getConstantAt(int offset);
   
    /**
     * Return a builder based on this constant pool.
     * @return a <code>ConstantPool.Builder</code> for this
     *         constant pool
     */
    public ConstantsEditor getBuilder();
   
    /**
     * Return the class info at the given index.
     * See JVM Spec 4.4.1.
     * @param cpIndex the index of the ConstantClass to be retrieved
     * @return the ConstantClass at the given index
     * @throws AccessException if the type of the constant does not match
     *         the type that is requested 
     **/
    public ConstantClass getClassAt(int cpIndex)
	throws AccessException;

    /**
     * Exception for invalid constant pool accesses.
     **/
    public static class AccessException extends OVMError {
    	
	/**
	 * Constructor.
	 * @param m Error message for access exception
	 **/    	
        public AccessException(String m) {
            super(m);
        }
        
        /**
         * Called when an invalid constant pool access causes a fatal error
         * @return OVMError a new error for this access exception
         */
        public OVMError fatal() {
            return new OVMError(this);
        }
    } // end of Constants.AccessException

} // end of Constants
