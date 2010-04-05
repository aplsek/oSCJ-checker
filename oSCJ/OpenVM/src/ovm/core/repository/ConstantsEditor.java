
package ovm.core.repository;


/**
 * General interface for a class that can be used to modify constant
 * pools.
 *
 * @author Christian Grothoff
 * @see ConstantPool
 * @see ovm.core.domain.ConstantPool
 **/
public interface ConstantsEditor {

    /**
     * Throw away existing contents and realloc for size <code>n</code>.
     * @param n the new size of the constant pool in number of constants.
     **/
    public void realloc(int n);
    
    /**
     * Clears a builder.
     **/
    public void reset();

    /**
     * Add a Binder constant.
     * This is an OVM speciality and does not correspond to anything in the
     * JVM Spec.
     * @param rb the bunder to be added
     **/
    public int addUnresolvedBinder(Binder rb);

    /**
     * Add a double to the constant pool. (JVM Spec 4.4.5)
     * @param value the value to be added to the constant pool
     * @return the offset of the new entry	 
     **/
    public int addConstantDouble(double value);

    /**
     * Add a float to the constant pool. (JVM Spec 4.4.4)
     * @param value the value to be added to the constant pool
     * @return the offset of the new entry	 
     **/
    public int addConstantFloat(float value);
    
    /**
     * Add an int to the constant pool. (JVM Spec 4.4.4)
     * @param value the value to be added to the constant pool     
     * @return the offset of the new entry	 
     **/
    public int addConstantInt(int value);
    
    /**
     * Add a long to the constant pool. (JVM Spec 4.4.5)
     * @param value the value to be added to the constant pool
     * @return the offset of the new entry	 
     **/
    public int addConstantLong(long value);

    /**
     * Add a bound field selector to the constant pool.
     * See JVM Spec 4.4.2
     * @param fieldsel the selector to be added to the constant pool
     * @return the offset of the new entry
     **/
    public int addFieldref(ConstantFieldref fieldsel);

    /**
     * Add an interface method selector to the constant pool.
     * See JVM Spec 4.4.2
     * @param methodsel the selector to be added to the constant pool
     * @return the offset of the new entry
     **/
    public int addInterfaceMethodref(ConstantMethodref methodsel);

    /**
     * Add a method selector to the constant pool.	 
     * See JVM Spec 4.4.2
     * @param methodsel the selector to be added to the constant pool
     * @return the offset of the new entry
     **/
    public int addMethodref(ConstantMethodref methodsel);
    
    /**
     * Add a symbolic shared state constant.
     * This is an OVM speciality and does not correspond to anything in the
     * JVM Spec.
     * @param tn the symbolic shared state constant to be added to the constant pool
     * @return the offset of the new entry
     **/
    public int addUnresolvedSharedState(TypeName.Gemeinsam tn);

    /**
     * Add a string to the constant pool. (JVM Spec 4.4.3)
     * @param str the string to be added
     * @return the offset of the new entry	 
     **/
    public int addUnresolvedString(RepositoryString str);

    /**
     * Add a <code>TypeName</code> to the constant pool.
     * See JVM Spec 4.4.1.
     * @param name the TypeName object to be added
     * @return the offset of the new entry
     **/
    public int addClass(ConstantClass name);

    /**
     * Add an <code>UnboundSelector</code> to the constant pool.
     * See JVM Spec 4.4.6
     * @param usel the unbound selector to be added
     * @return the offset of the new entry 
     **/
    public int addUnboundSelector(UnboundSelector usel);

    /* *
     * Add a UTF8 string. The stream position IS updated. 
     * See JVM Spec 4.4.7.
     *
     * This method doesn't have any clients, but it could be used by
     * a more efficient parser that avoided putting intermediate
     * classinfo and nameandtype UTF8s into the UTF8 store.
     * @param length length of the string.
     * @return the offset of the new entry
     *
     * But really, if we don't do makeUtf8At, we probably want to
     * keep a shadow constant pool full of UnicodeBuffers or
     * ByteBuffers corresponding to every Constant_UTF8, and do
     * addUtf8(UTF8Store._.installUtf8()) as needed.
     *
     * Maybe this was used by the dumper at one point, but no longer.
    public int addUtf8(ByteBuffer buf, int length);
     * */

    /**
     * Add a UTF8 string to the constant pool via its UTF8 index
     * See JVM Spec 4.4.7.
     * @param utf_index the index into the UTF8 table of the repository
     * @return the offset of the new entry
     **/
    public int addUtf8(int utf_index);

    /**
     * Note that this method may not be valid for all types of constant
     * pools!
     * @throws ovm.util.OVMError if this is a builder for an unlinked CP
     */
    public int addResolvedConstant(Object o);

    /**
     * Build the corresponding constants object.
     */
    public Constants unrefinedBuild();

} // end of ConstantsEditor
