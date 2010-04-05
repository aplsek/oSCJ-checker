package ovm.core.domain;

import ovm.core.repository.TypeName;
import ovm.core.services.memory.VM_Address;

/**
 * Blueprint contains information about fields and method of a group of
 * objects. A blueprint contains enough information to allocate a new
 * instance.  Blueprints are meant to be immutable data structures;
 * however, we can conceive that objects may change their blueprint during their
 * lifecycle.  <p>For the time being, there is a one-to-one correspondence
 * between <code>Type</code> and <code>Blueprint</code>.
 *
 * @author Palacz, Grothoff, Vitek, Flack
 **/

public interface Blueprint {//extends ConstantResolvedClass {
    byte[] get_dbg_string();

    /**
     * Return an integer that uniquely identifiers this blueprint
     * within it's Type.Context (as returned by
     * getType().getContext()).
     **/
    int getUID();

    /**
     * Return the unique identifier for a blueprint's context.
     * This is equivalent to getType().getContext().getUID(), and the
     * (CID,UID) pair uniquely identifies a blueprint
     **/
    int getCID();
    
    int getAllocKind();
    void setAllocKind(int allocKind);

    
    boolean isArray();
    boolean isPrimitive();
    boolean isScalar();
    Array asArray();
    Primitive asPrimitive();
    Scalar asScalar();

    /**
     * Class hierarchy access: return the first child class of this
     * blueprint.
     **/
    Blueprint firstChildBlueprint();
    /**
     * Class hierarchy access: return the first child class of this
     * blueprint.
     **/
    Blueprint nextSiblingBlueprint();
    
    /**
     * Perform any work needed to ensure that isSubtypeOf will work on
     * this blueprint.  In some implemenations of isSubtypeOf, this
     * may involve updating data structures for the entire class
     * hierarchy.
     **/
    void ensureSubtypeInfo();
    boolean isSubtypeOf(Blueprint other);

    /**
     * Shorthand for <code>getType().getUnrefinedName()</code>
     **/
    TypeName getName();
    
    /**
     * Get the shared state of this <code>Blueprint</code>. In Java terms,
     * the shared state consists of the static values of a class.
     * @return a reference to stated shared by all objects referring to
     *         this blueprint.
     **/
    Oop getSharedState();
    
    /**
     * Go "backwards" from a shared state blueprint. (jcf idea, can be
     * removed if offensive - it can be done with the right combination of
     * getType/getContext/getDomain/getName/blah/blueprintFor but that seems
     * such an involved way to express such a simple idea that it deserved to
     * be made a method.
     */
    Blueprint getInstanceBlueprint();
    
    /**
     * Return the type associated to this blueprint. 
     */
    Type getType();
    
    /**
     * Convenience method, equivalent to getType().getDomain().
     **/
    Domain getDomain();

    /**
     * It might appear that this method belongs in Blueprint.Scalar,
     * but Blueprint.Record also presumably contains pointers.
     */
    int[] getRefMap();
    void setRefMap(int[] map);    

    /**
     * Return if this is either a Scalar or an Array.
     */
    boolean isReference();
    
    /** 
     * True if this is the blueprint for the shared state of a class.
     * NB: used to be depracted but I could not find out why -- jv
     */
    boolean isSharedState();

    /**
     * Return the fixed part of the size of instances described by
     * this blueprint.  For fixed size objects, this is the size of the
     * whole shebang.  For arrays, this size includes the fixed metainformation
     * (including length) of the array, omitting only the size occupied by
     * the array's elements.  This size takes padding and alignment
     * requirements into account. The result is padded to a multiple
     * of 8 bytes (2 words).
     * <p>
     * FIXME: This method should return VM_Word.
     * @return size in bytes
     **/
    int getFixedSize();

    /**
     * Same as getFixedSize(), but without the additional padding
     * at the end. For Arrays, it corresponds to the size of the
     * part of the object that precedes the array data. For primitives
     * it corresponds to the actual length of the primitive.
     * <p>
     * FIXME: This method should return VM_Word.
     * @return size in bytes
     **/
    int getUnpaddedFixedSize();

    /**
     * Return the size of an instance described by this
     * blueprint. This size inlucdes variable-sized fields such as the
     * contents of an array.  This size also takes alignment
     * requirements into account. The result is padded to a multiple
     * of 8 bytes (2 words).
     * @assert getVariableSize(o) >= getFixedSize()
     * @param oop target object
     * @return size in bytes
     *
     * FIXME:  This method should return VM_Word.
     **/
    int getVariableSize(Oop oop);

    /**
     * Clone the oop, allocating using the supplied allocator.
     * @assert oop.getBlueprint() == this
     * @param original the original oop
     * 
     * @return the newly created instance
     **/
    Oop clone(Oop original, VM_Address target);

    /**
     * Return the index of a virtual method, m, in this blueprint's
     * vtable.
     **/
    public int getVirtualMethodOffset(Method m);

    /**
     * Return the index of an interface method, m, in this blueprint's
     * interface dispatch table.
     * FIXME: interface dispatch doesn't have to use simple tables.
     **/
    public int getInterfaceMethodOffset(Method m);

    //-----------------------------------------------------------------

    /** 
     * Blueprint for primitive types.
     * @see Blueprint
     **/
    interface Primitive extends Blueprint {
    }

    //-----------------------------------------------------------------

    /**
     * Blueprints for scalar objects.
     * @see Blueprint
     **/
    interface Scalar extends Blueprint {
        Oop stamp(VM_Address obj);
    } // end of Blueprint.Scalar

    /**
     * Records are structures similiar to C structs of fixed size and
     * without a type field (in particular, the
     * <code>Blueprint.Record</code> can not be obtained directly from a
     * reference to the record). Records have a flat type hierarchy with no
     * subtyping (not even for <code>null</code>). Primitive types are
     * simple records. Records are typically contained in other objects
     * (for example arrays). Thus <code>Blueprint.Record</code> does not
     * have an allocate method.<p>
     *
     * The meaning of shared state for records has not been finalized at the
     * moment (usually null). The parent of a record is always <tt>null</tt>.
     **/
    interface Record extends Blueprint {
    } // end of Blueprint.Record

    /**
     * This is the Blueprint for arrays.
     **/
    interface Array extends Blueprint {
        /**
         * Get the <code>Blueprint</code> for the component type
         * of the array type represented by this <code>Blueprint</code>
         * @return the component-type's <code>Blueprint</code>
         **/
        Blueprint getComponentBlueprint();

	/**
	 * Return the number of byte of memory that each component
	 * uses.  This method differs from
	 * getComponentBlueprint().getFixedSize() in that it does the
	 * right thing with respect to reference types, that is, it
	 * returns the size of a pointer.
	 **/
	int getComponentSize();
	
        /**
         * Get the value of the length field for the array located at a
         * given <code>Oop</code>.
         * @param addr the address of the array object
         * @return the value of the array's length field
         **/
        int getLength(Oop addr);
        
        /*
         * Set the value of the length field for the array located at a
         * given <code>Oop</code>.
         * <p>
         * <strong>Use with care.</strong>
	 * <p>
	 * <i>What exactly is "use with care" supposed to mean?  Is
	 * there any safe way to use this method?</i>
         * 
         * @param addr the address of the array object
         * @param len the length to set the length field to
	 void setLength(Oop addr, int len);
         */

        Oop stamp(VM_Address adr, int length);
       /**
        * Return the address of the element at <tt>index</tt> of
        * <tt>array</tt>.  You can use this address to get or set
        * elements with the get/set methods of VM_Address.  Arrays are
        * currently packed and you may safely use the VM_Address
        * get/set{any data type} methods.  But if we later move to a
        * cleaner reflective or snippet-based interface for fields, it
        * will probably be best to use it for array elements also, and
        * avoid depending on array format choices.
	*
	* I challenge you to communicate with the underlying system in
	* any way if byte arrays aren't packed. -jb
	**/
        VM_Address addressOfElement(Oop array, int index);

        /**
         * Returns the allocation size of an array of the requested
         * length.  The architecture and heap size renders certain
         * array sizes impossible, but this method does not take such
         * limitiations into account.  This method does, however, take
         * the Ovm's internal padding and alignment requirements into
         * account.
         */
        long computeSizeFor(int length);

	/**
	 * Return the offset of an element from the start of an array
	 * of this type.  This method peforms no bounds checking.  In
	 * particular, it does not check that the array index could
	 * possibly exist given the machine word size, and will return
	 * meaningless results when called with impossible array
	 * indexes.
	 * <p>
	 * FIXME: This method should return VM_Word.
	 **/
	int byteOffset(int index);
    } // end of Blueprint.Array

    /**
     * Factory to create blueprints.
     **/
    interface Factory {
        /**
         * Based on a <code>Type</code> create the appropriate
         * <code>Blueprint</code> if it already exists; otherwise create a
         * new one. Note that this assumes a one-to-one correspondance
         * between types and blueprint objects. See {@link Blueprint} for
         * further explanation.
         * @param type the <code>Type</code> whose <code>Blueprint</code>
         *             should be retrieved
         **/
        Blueprint blueprintFor(Type type);
        /**
         * Based on a <code>Context</code> and the <code>TypeName</code>
         * get the appropriate <code>Blueprint</code> if it already exists;
         * otherwise create a new one.  Note that this assumes a one-to-one
         * correspondance between <code>Type</code> objects and
         * <code>Blueprint</code> objects. See {@link Blueprint} for
         * further explanation.
         * @param name <code>TypeName</code> of the <code>Type</code> whose
         *             <code>Blueprint</code> should be retrieved
         * @param context the <code>Context</code> of the <code>Type</code>
         *                whose <code>Blueprint</code> should be retrieved
         * @throws LinkageException if linking failed
         **/
        Blueprint blueprintFor(TypeName.Compound name, Type.Context context)
            throws LinkageException;
    } // end of Blueprint.Factory

//    public boolean refMaskGet(int offset);

} // end of Blueprint
