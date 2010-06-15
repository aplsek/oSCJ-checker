package ovm.core.repository;

import ovm.services.bytecode.JVMConstants;
import ovm.util.UnicodeBuffer;
import s3.core.S3Base;

/**
 * Implementation of a constant pool editor, which creates a (repository)
 * ConstantPool.  ConstantPools NEVER contain resolved Strings,
 * SharedStates or Binders.  They always contain unresolved, unlinked
 * symbolic representations of these special entries.  The
 * domain-Constants (s3.core.domain.S3Constants) may contain both
 * linked and unlinked Strings/SharedStates and Binders.
 *
 * @author Christian Grothoff
 * @see ovm.core.repository.RepositoryBuilder
 **/
public class ConstantPoolBuilder 
    extends S3Base 
    implements ConstantsEditor, JVMConstants {

    /**
     * For every entry in the constant pool, we keep its tag.  Tags are
     * constants which indicate the type of constant pool entry that
     * follows. See JVM Spec 4.4.
     **/
    protected byte[] tags_;

    /**
     * For every entry in the Constant Pool, the result of parsing is
     * an object stored in this array.
     **/
    protected Object[] values_;

    /**
     * Create a new ConstantPoolBuilder. 
     **/
    public ConstantPoolBuilder() {
        reset();
    }

    /**
     * Create a new ConstantPoolBuilder by extending a base constant pool. 
     * @param base the basic constant pool that should be extended with
     *        more constants
     **/
    public ConstantPoolBuilder(ConstantPool base) {
        values_ = base.getValues();
        tags_ = base.getTags();
    }

    /**
     * Commit all changes made and return the completed constant pool.
     * @return the offset of the new entry
     **/
    public ConstantPool build() {
	return new ConstantPool(tags_, values_);
    }
    
    /**
     * Throw away existing contents and realloc for size <code>n</code>.
     * @param n the new size of the constant pool in number of constants.
     **/
    public void realloc(int n) {
	tags_ = new byte[n];
	values_ = new Object[n];
    }
    
    /**
     * Make sure there is room for n constants.
     * @param n the new number of constants supported (0...n-1)
     **/
    public void reserveTagNumbers(int n) {
	if (n <= tags_.length)
	    return; // got enough space
	byte[] oldTags = tags_;
	Object[] oldValues = values_;
	realloc(n);
	System.arraycopy(oldTags, 0, tags_, 0, oldTags.length);
	System.arraycopy(oldValues, 0, values_, 0, oldValues.length);
    }
    
    /**
     * Clears a builder.
     **/
    public void reset() {
	realloc(1); // constant 0 is reserved!
    }


    /**
     * Build the corresponding constants object.
     */
    public Constants unrefinedBuild() {
	return build();
    }

    
    /* **************** build methods with auto-index allocation ********* */

    /**
     * Add a Binder constant.
     * This is an OVM speciality and does not correspond to anything in the
     * JVM Spec.
     * @param rb the bunder to be added
     **/
    public int addUnresolvedBinder(Binder rb) {
	int n = testPresent(rb, CONSTANT_Binder);
	if (-1 != n)
	    return n;
	return put(allocateIndex(), CONSTANT_Binder, rb);
    }

    /**
     * Add a double to the constant pool. (JVM Spec 4.4.5)
     * @param value the value to be added to the constant pool
     * @return the offset of the new entry	 
     **/
    public int addConstantDouble(double value) {
	Double d = new Double(value);
	int n = testPresent(d, CONSTANT_Double);
	if (-1 != n)
	    return n;
	int index = allocateIndex();
	put(allocateIndex(), 0, null); // allocate 2nd index (wide!)
	return put(index, CONSTANT_Double, d);
    }

    /**
     * Add a float to the constant pool. (JVM Spec 4.4.4)
     * @param value the value to be added to the constant pool
     * @return the offset of the new entry	 
     **/
    public int addConstantFloat(float value) {
	Float f = new Float(value);
	int n = testPresent(f, CONSTANT_Float);
	if (-1 != n)
	    return n;
	return put(allocateIndex(), CONSTANT_Float, f);
    }
    
    /**
     * Add an int to the constant pool. (JVM Spec 4.4.4)
     * @param value the value to be added to the constant pool     
     * @return the offset of the new entry	 
     **/
    public int addConstantInt(int value) {
	Integer val = new Integer(value);
	int n = testPresent(val, CONSTANT_Integer);
	if (-1 != n)
	    return n;
	return put(allocateIndex(), CONSTANT_Integer, val);
    }
    
    /**
     * Add a long to the constant pool. (JVM Spec 4.4.5)
     * @param value the value to be added to the constant pool
     * @return the offset of the new entry	 
     **/
    public int addConstantLong(long value) {
	Long l = new Long(value);
	int n = testPresent(l, CONSTANT_Long);
	if (-1 != n)
	    return n;
	int index = allocateIndex();
	put(allocateIndex(), 0, null); // allocate 2nd index (wide!)
	return put(index, CONSTANT_Long, l);
    }

    /**
     * Add a bound field selector to the constant pool.
     * See JVM Spec 4.4.2
     * @param fieldsel the selector to be added to the constant pool
     * @return the offset of the new entry
     **/
    public int addFieldref(ConstantFieldref fieldsel) {
	int n = testPresent(fieldsel, fieldsel.getTag());
	if (-1 != n)
	    return n;
	return put(allocateIndex(), fieldsel.getTag(), fieldsel);
    }
    
    /**
     * Add an interface method selector to the constant pool.
     * See JVM Spec 4.4.2
     * @param methodsel the selector to be added to the constant pool
     * @return the offset of the new entry
     **/
    public int addInterfaceMethodref(ConstantMethodref methodsel) {
	int n = testPresent(methodsel, CONSTANT_InterfaceMethodref);
	if (-1 != n)
	    return n;
	return put(allocateIndex(), CONSTANT_InterfaceMethodref, methodsel);
    }

    /**
     * Add a method selector to the constant pool.	 
     * See JVM Spec 4.4.2
     * @param methodsel the selector to be added to the constant pool
     * @return the offset of the new entry
     **/
    public int addMethodref(ConstantMethodref methodsel) {
	int n = testPresent(methodsel, methodsel.getTag());
	if (-1 != n)
	    return n;
	return put(allocateIndex(), methodsel.getTag(), methodsel);
    }
    
    /**
     * Add a symbolic shared state constant.
     * This is an OVM speciality and does not correspond to anything in the
     * JVM Spec.
     * @param tn the shared-state typename
     * @return   the offset of the new entry
     **/
    public int addUnresolvedSharedState(TypeName.Gemeinsam tn) {
        int n = testPresent(tn, CONSTANT_SharedState);
        if (-1 != n)
            return n;
	return put(allocateIndex(), CONSTANT_SharedState, tn);
    }

    /**
     * Add a string to the constant pool. (JVM Spec 4.4.3)
     * @param str the string to be added
     * @return the offset of the new entry	 
     **/
    public int addUnresolvedString(RepositoryString str) {
        int n = testPresent(str, CONSTANT_String);
        if (-1 != n)
            return n;
	return put(allocateIndex(), CONSTANT_String, str);
    }

    /**
     * Add a <code>TypeName</code> to the constant pool.
     * See JVM Spec 4.4.1.
     * @param name the TypeName object to be added
     * @return the offset of the new entry
     **/
    public int addClass(ConstantClass name) {
        if (name == null)
            throw new NullPointerException();
	int n = testPresent(name, CONSTANT_Class);
        if (-1 != n)
            return n;
	return put(allocateIndex(), name.getTag(), name);
    }

    /**
     * Add an <code>UnboundSelector</code> to the constant pool.
     * See JVM Spec 4.4.6
     * @param usel the unbound selector to be added
     * @return the offset of the new entry 
     **/
    public int addUnboundSelector(UnboundSelector usel) {
        int n = testPresent(usel, CONSTANT_NameAndType);
        if (-1 != n)
            return n;
	return put(allocateIndex(), CONSTANT_NameAndType, usel);
    }

    /**
     * Add a UTF8 string. The stream position IS updated. 
     * See JVM Spec 4.4.7.
     * @param length length of the string.
     * @return the offset of the new entry
    public int addUtf8(ByteBuffer buf, int length) {
        return addUtf8(Repository._.installUtf8(buf, length));
    }
     **/

    /**
     * Add a UTF8 string to the constant pool via its UTF8 index
     * See JVM Spec 4.4.7.
     * @param utf_index the index into the UTF8 table of the repository
     * @return the offset of the new entry
     **/
    public int addUtf8(int utf_index) {
        ConstantPool.UTF8Index utf = new ConstantPool.UTF8Index(utf_index);
        int n = testPresent(utf, CONSTANT_Utf8);
        if (-1 != n)
            return n;
	return put(allocateIndex(), CONSTANT_Utf8, utf);
    }

    /**
     * Note that this method should never be called on ConstantPoolBuilders!
     * @throws OVMError 
     */
    public int addResolvedConstant(Object o) {
	throw new ovm.util.OVMError("FATAL: Attempt to add resolved constant to a repository ConstantPool!");
    }


    /* ************* another family of methods to set 
       constant pool entries at a specific index ************ */

    private TypeName.Compound getClassInfo(int idx) {
	UnicodeBuffer b = UTF8Store._.getUtf8(idx);
        return TypeName.Compound.parseClassInfo(b);
    }
    /**
     * Make a Binder at the given offset in the CP and install it.
     *
     * This is an OVM speciality and does not correspond to anything in the
     * JVM Spec.
     *
     * @param utf_typename_index
     * @param string_indices
     * @param index the index at which to install the new Binder.
     */
    public void makeUnresolvedBinderAt(int utf_typename_index,
				       int[] string_indices,
				       int index) {
	TypeName tn = getClassInfo(utf_typename_index);
        RepositoryString[] strings = new RepositoryString[string_indices.length];
        for (int i = 0; i < strings.length; i++)
            strings[i] = new RepositoryString(string_indices[i]);
	put(index, CONSTANT_Binder, new Binder(tn.asScalar(), strings));
    }

    /**
     * Make a double value at the given index in the CP and
     * install it.
     * See JVM Spec 4.4.5.
     * @param value the double to be installed
     * @param index the index at which to install the new value 
     */
    public void makeDoubleValueAt(double value, int index) {
	put(index, CONSTANT_Double, new Double(value));
    }

    /**
     * Make a field at the given offset in the CP composed of a
     * defining class name, a member name and a descriptor. The
     * position must have been reserved first!
     * See JVM Spec 4.4.2
     * @param defClass the name of the class
     * @param usel the unbound selector of the field
     * @param index the constant pool index to to put the field reference
     */
    public void makeFieldAt(TypeName.Compound defClass,
			    UnboundSelector.Field usel,
			    int index) {
	put(index, CONSTANT_Fieldref, Selector.Field.make(usel, defClass));
    }

    /**
     * Make a float value at the given index in the CP and
     * install it.
     * See JVM Spec 4.4.4.
     * @param value the float to be installed
     * @param index the index at which to install the new value 
     */
    public void makeFloatValueAt(float value, int index) {
	put(index, CONSTANT_Float, new Float(value));
    }

    /**
     * Make an int value at the given index in the CP and
     * install it.
     * See JVM Spec 4.4.4.
     * @param value the int to be installed
     * @param index the index at which to install the new value 
     */
    public void makeIntValueAt(int value, int index) {
	put(index, CONSTANT_Integer, new Integer(value));
    }

    /**
     * Make a long value at the given index in the CP and
     * install it.
     * See JVM Spec 4.4.4.
     * @param value the long to be installed
     * @param index the index at which to install the new value 
     **/
    public void makeLongValueAt(long value, int index) {
	put(index, CONSTANT_Long, new Long(value));
    }

    /**
     * Make a member (method, field, or interface method) at the given
     * offset in the CP composed of a defining clas name, a member
     * name and a descriptor. The position must have been reserved first!
     * See JVM Spec 4.4.2
     * @param defClass the name of the class
     * @param usel the unbound selector of the field
     * @param index the constant pool index in which to put the field reference
     * @param tag the type of the constant, either CONSTANT_InterfaceMethodref or CONSTANT_Methodref
     **/
    public void makeMethodAt(TypeName.Compound defClass,
			     UnboundSelector.Method usel,
			     int index,
			     byte tag) {
        assert(
            (tag == CONSTANT_InterfaceMethodref)
                || (tag == CONSTANT_Methodref));
	put(index, tag, Selector.Method.make(usel, defClass));
    }

    /**
     * Make a symbolic shared state at the given offset in the CP and install
     * it.  The stream position is not updated. 
     * This is an OVM speciality and does not correspond to anything in the
     * JVM Spec.
     * @param utf_typename_index the utf8 index of the TypeName of the 
     *                           SharedState object
     * @param index the constant pool index at which to install the new value
     **/
    public void makeUnresolvedSharedStateAt(int utf_typename_index, int index) {
        TypeName.Gemeinsam tn = getClassInfo(utf_typename_index).asGemeinsam();
	put(index, CONSTANT_SharedState, tn);
    }

    /**
     * Create a new String value at the given offset in the CP and install
     * it into the repository. The position must have been reserved first! 
     * See JVM Spec 4.4.3.
     * @param utfIndex  the value of the CP entry.
     * @param offset    the offset of the entry in the constant pool.
     **/
    public void makeUnresolvedStringAt(int utfIndex, int offset) {
	put(offset, CONSTANT_String, new RepositoryString(utfIndex));
    }

    /* ************* another family of methods to set 
       constant pool entries at a specific index ************ */

    /**
     * Create a new TypeName at the given offset in the CP using the
     * current position in the stream as starting point and
     * <code>nameLength</code> as length. The TypeName is automatically
     * installed in the repository. The position must have been reserved
     * first!
     * See JVM Spec 4.4.1.
     * @param utfIndex the index into the UTF8 intern table
     * @param index the offset of the entry in the constant pool.
     *
     * FIXME: why is this the only make method that returns a value?
     **/
    public TypeName.Compound makeTypeNameAt(int utfIndex, int index) {
        TypeName.Compound tn = getClassInfo(utfIndex);
	put(index,  CONSTANT_Class, tn);
        return tn;
    }

    /**
     * Make an unbound selector from the given UTF8 Strings and
     * install it in the CP at the given index.
     * @see UnboundSelector
     * @see Descriptor
     * @param utf_name_index the utf8 index of the string corresponding to the name
     *                       of the method or field
     * @param utf_descriptor_index the utf8 index of the string representation of
     *                             this field or method's descriptor
     * @param index the constant pool index at which to install the unbound
     *        selector
     **/
    public void makeUnboundSelectorAt(int utf_name_index,
				      int utf_descriptor_index,
				      int index) {
	Descriptor desc = Descriptor.parse(UTF8Store._.getUtf8(utf_descriptor_index)); 
    put(index, CONSTANT_NameAndType,
	    UnboundSelector.make(utf_name_index, desc));
    }

    /**
     * Make a UTF8 string at the given offset in the CP and install it.
     * The stream position is not updated. The position must have been
     * reserved first! See JVM Spec 4.4.7.
     * @param buf   Wraps the Utf8 data within the input stream
     * @param index the offset of the entry in the constant pool.
     **/
    public void makeUtf8At(UnicodeBuffer buf, int index) {
	int utf8Index = UTF8Store._.installUtf8(buf);
	put(index, CONSTANT_Utf8, new ConstantPool.UTF8Index(utf8Index));
    }

    /**
     * Delete the entry at the given constant pool index.
     * @param index the index of the entry to be deleted.
     */
    public void deleteEntryAt(int index) {
	put(index, 0, null);
    }

    /* ************** little convenience method ***************** */
    
    /**
     * Allocate a new index into the constant pool.
     * @return the new index.
     **/
    protected int allocateIndex() {
	int n = tags_.length;
	reserveTagNumbers(n + 1);
	return n;
    }

    /**
     * Set the entry at a specific index, and return the index.
     **/
    protected int put(int index, int tag, Object value) {
	tags_[index] = (byte) tag;
	values_[index] = value;
	return index;
    }

    /**
     * Test if a constant equal to "o" with the expected tag is
     * already in the constant pool.  If yes, the respective index is
     * returned.
     * @param o the object whose presence is being tested 
     * @return -1 if o was not found
     */
    private int testPresent(Object o, byte expectedTag) {
        for (int i = 1; i < values_.length; i++)
            if (expectedTag == tags_[i]
		&& o.equals(values_[i]))
                return i;
        return -1;
    }

} // end of ConstantPoolBuilder
