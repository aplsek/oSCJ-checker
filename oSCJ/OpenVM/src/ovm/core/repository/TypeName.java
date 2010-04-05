package ovm.core.repository;

import java.io.IOException;
import java.io.OutputStream;

import ovm.core.services.format.OvmFormat;
import ovm.core.services.memory.MemoryPolicy;
import ovm.services.bytecode.JVMConstants;
import ovm.util.OVMError;
import ovm.util.UnicodeBuffer;

/**
 * TypeName is an abstract class describing the name of a Java object type,
 * class or interface, obtained from a class file. Class, interface and 
 * array fully qualified names are created from the constant pool 
 * <code>CONSTANT_Class</code> entries and stored in the repository encoded 
 * as typenames. 
 *
 * <p>Typenames are singletons interned in the repository.  
 * The string representation of many of the components of type names 
 * (simple names and packages within the typename object) uses utf8strings 
 * stored in the repository.</p>
 *
 * <p>There are two main groupings of typenames: <code>Primitive</code>,
 * and <code>Compound</code>. Within the <code>Compound</code> grouping,
 * there are two typename classes: <code>Scalar</code>, and
 * <code>Array</code>. Additionally, a <code>Scalar</code> type can be
 * <code>Nested</code>, if it has an enclosing class.
 *
 * <p>Unlike the <code>Compound</code> typename class, which is abstract
 * and exists primarily as a grouping of non-primitive typenames, the
 * <code>Primitive</code> typename class is an implementation class which
 * exists both as a self-contained entity and as a superclass for the
 * <code>WidePrimitive</code> typename class. <code>WidePrimitive</code>
 * contains little additional functionality except for its ability to
 * identify itself as a wide (2-word) primitive type.
 *
 * <p>Thus, the hierarchy appears as follows:
 *
 *   <ul><li>TypeName</li>
 *     <ul>
 *          <li>Compound
 *              <ul>
 *                <li>Scalar</li>
 *                       <ul><li>Nested</li></ul>
 *                <li>Array</li>
 *              </ul></li>
 *          <li>Primitive</li>
 *          <li>
 *              <ul>
 *                <li>WidePrimitive</li>
 *              </ul>
 *          </li>
 *     </ul>
 *   </ul>
 * These are all implemented as subclasses of <code>TypeName</code>.
 *
 * <p>The current internal encoding of typename depends upon the kind of
 * typename: <ul>
 *
 *    <li><b><code>Compound</code></b> typenames are simply those typenames
 *        that are not primitive. <code>Scalar</code>, <code>Nested</code>,
 *        and <code>Array</code> typenames are <code>Compound</code>
 *        typenames.<p></li>
 *
 *    <li><b><code>Scalar</code></b> typenames consist primarily of two
 *         indices into the repository utf8 store: one for the 
 *         class/interface simple name, and one for the package name.<p></li>
 *  
 *    <li><b><code>Array</code></b> typenames contain an <code>element</code>
 *        typename and a <code>depth</code> field. The <code>element</code>
 *        type refers to the type of 0th dimension of the array; that is,
 *        the element type is the base type of an array - it is the
 *        type that would be obtained by stripping all of the '<code>[</code>'
 *        characters from a fully-qualified array name string. The
 *        <code>depth</code> field refers to the number of dimensions
 *        of the array (i.e. the number of '[' characters in the fully
 *        qualified name).<p></li>
 *
 *    <li><b><code>Primitive</code></b> typenames, like <code>Scalar</code> 
 *        typenames, contain a repository utf8 index for their names. However,
 *        these names differ from those of <code>Scalar</code> types; instead,
 *        they are the utf8 string representations of their respective
 *        {@link ovm.core.repository.TypeCodes TypeCodes}. 
 *        <code>Primitive</code> typenames are, as the name implies, used for
 *        primitive types such as <code>int</code>, <code>long</code>,
 *        <code>boolean</code>, etc.<p></li>
 *
 *    <li><b><code>WidePrimitive</code></b> typenames are essentially
 *        <code>Primitive</code> typenames which can identify themselves
 *        as typenames for 2-word primitive types.<p></li>
 * </ul>
 *
 * <p>
 * The outer class (<code>TypeName</code>) is abstract, 
 * and the actual type name implementation classes are in the static inner 
 * classes that extend it. 
 * </p>
 *
 * <p> In addition to the implemented functionality from
 * <code>TypeName</code>, a <code>TypeName</code> also contains an
 * innermost component type for arrays (or null for non-arrays); this
 * refers to the <code>element</code> type mentioned above in the
 * discussion of <code>Array</code> typenames.
 *
 * <p> TypeNames are singletons interned in the Repository. 
 *
 * <p><i>N.B.: Please see section $2.7 of the JVM Spec for explanations
 * of simple names, package names, and qualified names.</i></p>
 *
 * <p>KLB: Put something in here about Gemeinsam. These are the old docs.
 *
 * @author Jan Vitek, Christian Grothoff 
 * 
 **/
abstract public class TypeName extends RepositorySymbol.Internable
    implements ConstantClass	// FIXME: shouldn't this be in Compound?
{
    // FIXME:  All these static initializers should really be wrapped
    // in enterRepositoryDataArea.  If we ever want to model VM_Areas
    // at image build time, this code will have to change.
    /**
     * The {@link TypeName type name} object for <code>boolean</code>
     **/
    static public final Primitive BOOLEAN = new Primitive(TypeCodes.BOOLEAN);

    /**
     * The {@link TypeName type name} object for <code>byte</code>
     **/
    static public final Primitive BYTE = new Primitive(TypeCodes.BYTE);

    /**
     * The {@link TypeName type name} object for <code>char</code>
     **/
    static public final Primitive CHAR = new Primitive(TypeCodes.CHAR);

    /**
     * The {@link TypeName type name} object for <code>double</code>
     **/
    static public final WidePrimitive DOUBLE =
	new WidePrimitive(TypeCodes.DOUBLE);

    /**
     * The {@link TypeName type name} object for <code>float</code>
     **/
    static public final Primitive FLOAT = new Primitive(TypeCodes.FLOAT);

    /**
     * The {@link TypeName type name} object for <code>int</code>
     **/
    static public final Primitive INT = new Primitive(TypeCodes.INT);

    /**
     * The {@link TypeName type name} object for <code>long</code>
     **/
    static public final WidePrimitive LONG =
	new WidePrimitive(TypeCodes.LONG);

    /**
     * The {@link TypeName type name} object for <code>short</code>
     **/
    static public final Primitive SHORT = new Primitive(TypeCodes.SHORT);

    /**
     * The {@link TypeName type name} object for <code>void</code>
     **/
    static public final Primitive VOID = new Primitive(TypeCodes.VOID);

    /** 
     * The empty set of typenames
     **/
    public final static TypeName[] EMPTY_ARRAY = Scalar.EMPTY_SARRAY;
    // FIXME: must these be public?  Is INNER_SEPARATOR used at all?
    /**
     * The separator character between outer and inner classes
     **/
    public final static char INNER_SEPARATOR = '$';
    /**
     * The separation character for package names
     **/
    public final static char PACKAGE_SEPARATOR = '/';
    /**
     * The termination character for package names
     **/
    public final static char PACKAGE_TERMINATOR = '/';
    /**
     * The termination character for class names
     **/
    public final static char TERMINATOR = ';';

    /** array types for this scalar/primitive type **/
    protected Array[] arrayTypes_ = Array.EMPTY_AARRAY;

    /**
     * The Gemeinsam of an instance type or the instance of a
     * gemeinsam type.
     **/
    protected TypeName sibling;

    protected TypeName() {
	if (!isGemeinsam())
	    sibling = new Gemeinsam(this);
    }

    /** Only scalars are interned in hash tables, default copy bitches. **/
    protected Internable copy() {
	throw new OVMError("interning non-scalar typename");
    }
    /**
     * Return this <code>TypeName</code> as a <code>TypeName.Array<code>
     * object. Implementations could throw errors when this method is called
     * on a non-<code>Array</code> typename object.
     * @return this object as a array typename object
     */
    public TypeName.Array asArray() {
        throw new OVMError.ClassCast();
    }

    /**
     * Return this <code>TypeName</code> as a <code>TypeName.Compound<code>
     * object. Implementations could throw errors when this method is called
     * on a non-<code>Compound</code> typename object.
     * @return this object as a compound typename object
     */
    public TypeName.Compound asCompound() {
        throw new OVMError.ClassCast();
    }

    /**
     * Return this <code>TypeName</code> as a <code>TypeName.Primitive<code>
     * object. Implementations could throw errors when this method is called
     * on a non-<code>Primitive</code> typename object.
     * @return this object as a primitive typename object
     */
    public TypeName.Primitive asPrimitive() {
        throw new OVMError.ClassCast();
    }

    /**
     * Return this <code>TypeName</code> as a <code>TypeName.Scalar</code>
     * object. Implementations could throw errors when this method is called
     * on a non-<code>Scalar</code> typename object.
     * @return this object as a scalar typename object
     */
    public TypeName.Scalar asScalar() {
        throw new OVMError.ClassCast();
    }

    /**
     * Return this <code>TypeName</code> as a <code>TypeName.WidePrimitive<code>
     * object. Implementations could throw errors when this method is called
     * on a non-<code>WidePrimitive</code> typename object.
     * @return this object as a wide primitive typename object
     */
    public TypeName.WidePrimitive asWidePrimitive() {
        throw new OVMError.ClassCast();
    }

    /**
     * Return this <code>TypeName</code> as a <code>TypeName.Gemeinsam<code>
     * object. Implementations could throw errors when this method is called
     * on a non-<code>Gemeinsam</code> typename object.
     * @return this object as a Gemeinsam typename object
     */
    public TypeName.Gemeinsam asGemeinsam() {
        throw new OVMError.ClassCast();
    }

    public boolean isResolved() {
      return false;
    }
    public byte getTag() {
      return JVMConstants.CONSTANT_Class;
    }
    public TypeName asTypeName() {
      return this;
    }
    
    public String asTypeNameString() {
        return OvmFormat._.format( this);
    }

    /** 
     * <p>Compare an <code>Object</code> to this type name for equality.
     * This is the default <code>equals</code> implementation that
     * relies on dynamic casts to compute equality; since this
     * method is called a lot, the 
     * {@link #equals(TypeName) more specific implementation}
     * is called whenever possible.</p>
     * @param otherType the object upon which equality is to be determined
     * @return true if the input object is equal to this type name
     **/
    abstract public boolean equals(Object otherType);

    /** 
     * <p>Compare an <code>TypeName</code> to this type name for equality
     * by double dispatching. Since <code>equals</code> is frequently
     * called on typenames in order to compute equality of other objects,
     * hash codes, etc., this is an optimized version which avoids type
     * casts by calling the appropriate <code>equalsFrom</code> method via
     * double-dispatch.</p>
     *
     * <p>Checking equality by double dispatch instead of by dynamic type
     * casts incurs a significant performance gain due to the frequency
     * with which the <code>equals</code> method is called on
     * typenames.</p>
     *
     * <h3>Double-dispatch details:</h3>
     *
     * In order for two TypeName objects to be equal, they must have exactly the same 
     * dynamic types. The goal is to use double dispatch to determine that this is the
     * case without having to do dynamic type casts. The following describes the
     * details of how this is done.
     * 
     * <p><code>TypeName</code> declares the methods {@link #equals(TypeName)} and
     * {@link #equals(Object)}. Both of these methods must be given an overriding
     * definition in every subclass of <code>TypeName</code> that exists (this is
     * enforced by making them <code>abstract</code>). The version called with an <code>Object</code> argument only differs from
     * the other version in that it adds an <code>instanceof</code> test on
     * the argument and a typecast to <code>TypeName</code> if the test was
     * passed. Both <code>equals</code> methods then dispatch to the argument
     * <code>TypeName</code> object's <code>equalsFrom</code> method.
     * </p>
     * 
     * <p>
     * Now, in the abstract base class, there is an <code>equalsFrom</code>
     * method of the following form for each subclass <code>Foo</code> of
     * <code>TypeName</code>:
     * 
     * <pre>
boolean equalsFrom(Foo otherType) { return false; }
     * </pre>
     * 
     * 
     * Their signatures differ only in the type of the single argument. Then,
     * in the implementation of each particular subclass of <code>TypeName</code>,
     * the one <code>equalsFrom()</code> method from <code>TypeName</code>
     * whose argument type is the same as the class being implemented is
     * overridden. Thus, <code>Array</code> overrides <code>equalsFrom(Array otherType)</code>,
     * but <b>not</b><code>equalsFrom(Primitive otherType)</code>. This
     * overridden <code>equalsFrom()</code> method contains the comparison
     * details for determining equality of two objects whose type is the same
     * as the implementation class. <i>(Note that a subclass must also provide
     * a definition of equalsFrom with its immediate superclass as the argument
     * type returning <code>false</code> if its superclass overrides the
     * default method defined in <code>TypeName</code>.)</i>
     * 
     * <p>
     * So let's refer to the original argument from the <code>equals</code>
     * call as <code>otherType</code>.<code>otherType</code>'s<code>equalsFrom</code>
     * method is called, then, with <code>this</code> as the argument.
     * Whether the default method is called or the overridden method is called
     * depends upon the type of the argument to the <code>equalsFrom</code>
     * method. If the overridden method is called, equality is determined based
     * upon the TypeName objects themselves. Otherwise, the default method
     * returns false based on type inequality.
     * 
     * <h3>Example</h3>
     * Assume <code>obj1</code> is an instance of <code>Array</code> and
     * <code>obj2</code> is an instance of <code>Scalar</code>. Assume the
     * static types of both are defined only as the base class, <code>TypeName</code>.
     * Now, assume we call <code>obj1.equals(obj2);</code>.
     * 
     * <p>
     * The following should happen:
     * </p>
     * 
     * <ol>
     * <li>The <code>Array</code> class's overridden version of <code>equals(TypeName otherType)</code>
     * should be called, with <code>obj2</code> as the argument, since <code>obj2</code>
     * is a <code>TypeName</code>.</li>
     * <li>The <code>equals</code> method calls obj2.equalsFrom(obj1);</li>
     * <li>Because <code>obj2</code>'s dynamic class, <code>Scalar</code>,
     * does not override <code>equalsFrom(Array otherType)</code>, the
     * default version of <code>equalsFrom(Array otherType)</code> from
     * <code>TypeName</code> is called and <code>false</code> is returned.
     * </li>
     * </ol>
     * Now, if <code>obj2</code> had been of type <code>Array</code>, then
     * an overridden version of the method would have been called in step 2,
     * and step 3 would have compared the actual <code>TypeName</code>
     * objects for equality.
     * 
     * @param otherType
     *                the typename to be compared with this typename
     * @return true if the input object is equal to this type name
     */
    abstract public boolean equals(TypeName otherType);

    /**
     * Compare an <code>TypeName.Array</code> to this type name for equality
     * by double dispatching - this is the default implementation and is
     * overridden in the <code>Array</code> implementation of
     * {@link Array#equalsFrom(TypeName.Array) <code>equalsFrom</code>}. The
     * default implementation always returns false and will be called from
     * <code>equals</code> methods whose parameters are (dynamically) <b>not
     * </b><code>Array</code> types.
     * 
     * @param otherType
     *                the array object to be compared
     * @return false, always, as this is the default. (This will not be called
     *         on <code>Array</code> typenames, so it must be false)
     * @see #equals(TypeName)
     */
    boolean equalsFrom(Array otherType) {
        return false;
    }

    /**
     * Compare an <code>TypeName.Primitive</code> to this type name for
     * equality by double dispatching - this is the default implementation and
     * is overridden in the <code>Primitive</code> implementation of
     * {@link Primitive#equalsFrom(TypeName.Primitive)}.
     * The default implementation always returns false and will be called from
     * the <code>equals</code> methods of <code>TypeName</code> objects
     * which are <b>not</b><code>Primitive</code> types.
     * 
     * @param other the primitive object to be compared
     * @return false, always, as this is the default. (This will not be called
     *         by <code>Primitive</code> typenames, so it must be false)
     * @see #equals(TypeName)
     */
    boolean equalsFrom(Primitive other) {
        return false;
    }

    /**
     * Compare an <code>TypeName.Scalar</code> to this type name for equality
     * by double dispatching - this is the default implementation and is
     * overridden in the <code>Scalar</code> implementation of
     * {@link Scalar#equalsFrom(TypeName.Scalar) <code>equalsFrom</code>}. The
     * default implementation always returns false and will be called from the
     * <code>equals</code> methods of <code>TypeName</code> objects which
     * are <b>not</b><code>Scalar</code> types.
     * 
     * @param other the scalar object to be compared
     * @return false, always, as this is the default. (This will not be called
     *         by <code>Scalar</code> typenames, so it must be false)
     * @see #equals(TypeName)
     */
    boolean equalsFrom(Scalar other) {
        return false;
    }

    /**
     * Compare an <code>TypeName.Scalar.Gemeinsam</code> to this type name
     * for equality by double dispatching - this is the default implementation
     * and is overridden in the <code>Scalar.Gemeinsam</code> implementation
     * of <!-- Change code to link in the next line if bored -->
     * {@code TypeName.Scalar.Gemeinsam#equalsFrom(TypeName.Scalar.Gemeinsam)}.
     * The default implementation always returns false and will be called from
     * the <code>equals</code> methods of <code>TypeName</code> objects
     * which are <b>not</b><code>Scalar.Gemeinsam</code> types.
     * 
     * @param other the gemeinsam object to be compared
     * @return false, always, as this is the default. (This will not be called
     *         by <code>Scalar.Gemeinsam</code> typenames, so it must be
     *         false)
     * @see #equals(TypeName)
     */
    boolean equalsFrom(Scalar.Gemeinsam other) {
        return false;
    }

    /**
     * Returns the <em>element</em> (i.e. innermost) type of an array type
     * name, or <code>null</code> if this object is not an array type. This
     * is the type of the component at the 0th dimension of the array.
     * 
     * For example, given the array type <code>[[[Ljava.lang.Object;</code>,
     * the innermost component type is <code>Ljava.lang.Object;</code>, not
     * <code>[[Ljava.lang.Object;</code>.
     * 
     * <p>
     * (In other words, the type obtained by stripping <em>all</em> layers of
     * brackets)
     * </p>
     * 
     * @return the <code>TypeName</code> of the innermost component type for
     *         this array.
     */
    public TypeName getInnermostComponentTypeName() {
	throw new UnsupportedOperationException();
    }

    /**
     * Returns the repository index of the utf8string of this type's package
     * name. The name does not include a trailing '/'.
     * 
     * @return The repository id of the utf8string or <code>0</code> if the
     *         class lives in the unnamed package.
     */
    public int getPackageNameIndex() {
	throw new UnsupportedOperationException();
    }

    /**
     * Returns the repository index of the utf8string of the simple type name.
     * This name is the unqualified name of the type as specified in its class
     * file - in other words, <code>Object</code>, not <code>Ljava/lang/Object;</code>.
     * See JVM Spec $2.7.
     * 
     * @return repository index of the utf8string for the class name.
     */
    public int getShortNameIndex() {
	throw new UnsupportedOperationException();
    }

    /**
     * Get the type tag for this type. This is a char from
     * {@link ovm.core.repository.TypeCodes TypeCodes}.
     * 
     * @return the type tag character for this type
     */
    public abstract char getTypeTag();

    /**
     * Returns a hash code that is independent of the memory location of the
     * object.
     * 
     * @return this type name's hash code
     */
    abstract public int hashCode();

    /**
     * Returns true if this TypeName is an array type.
     * 
     * @return true if this is an array type, otherwise false.
     */
    public boolean isArray() {
        return false;
    }

    /**
     * Determine if <code>this</code> is a <code>TypeName.Compound</code>
     * object.
     * 
     * @return boolean true if this is a compound typename object, else false
     */
    public boolean isCompound() {
        return false;
    }

    /**
     * Determine if <code>this</code> is a <code>TypeName.Gemeinsam </code>
     * object
     * 
     * @return boolean true if this is a gemeinsam object, else false
     */
    public boolean isGemeinsam() {
        return false;
    }

    /**
     * Returns true if this TypeName is a primitive type.
     * 
     * @return true if this is a primitive type, otherwise false.
     */
    public boolean isPrimitive() {
        return false;
    }

    /**
     * Determine if <code>this</code> is a <cpde>TypeName.Scalar</code>
     * object.
     * 
     * @return boolean true if this is a scalar typename object, else false
     */
    public boolean isScalar() {
        return false;
    }

    /**
     * Returns true if this TypeName is a wide primitive type (long or double).
     * 
     * @return true if this is a wide primitive, otherwise false
     */
    public boolean isWidePrimitive() {
        switch (getTypeTag()) {
            case TypeCodes.LONG :
            case TypeCodes.DOUBLE :
                return true;
            default :
                return false;
        }
    }

    /**
     * Returns the length of this type name's fully-qualified name string. For
     * primitive types, this should always be 1.
     * 
     * @return the length of the fully-qualified name string for this type.
     * @see #toString
     */
    public abstract int length();

    /**
     * Write this TypeName as a fully qualified name to an output stream.
     * 
     * @param out
     *                the output stream to write the typename to. The output
     *                format will contain the full type and package name, and
     *                if this is an array, the proper number of leading
     *                brackets. e.g. <code>[[Ljava/lang/Object;</code>.
     * 
     * <p>
     * For primitive types single letter codes are returned, e.g. <code>I</code>
     * (see {@link ovm.core.repository.TypeCodes TypeCodes}.)
     * </p>
     *
     * toString() is implemented in terms of write.
     *
     * @throws IOException
     *                 if there is an exception thrown by the OutputStream
     *                 object
     */
    public abstract void write(OutputStream out) throws IOException;


    /**
     * Parse a typename from the buffer, advancing the buffer
     * position to the end of the typename.  This method may be used
     * to parse a field descriptor, or a portion of a method
     * descriptor.
     *
     * NOTE: Gemeinsam typenames for array and primitive types cannot
     * be parsed.  What's more, there is no way to differentiate
     * between Gemeinsam typenames for primitives and Gemeinsam
     * typenames for classes of the same name in the anonymous
     * package.
     **/
    public static TypeName parse(UnicodeBuffer buf) {
	int depth = 0;

	char c;
	while ((c = (char) buf.peekByte()) == TypeCodes.ARRAY) {
		buf.getByte();
	    depth++;
	} if (depth > 0) {
	    return Array.make(parse(buf), depth);
	} else if (c == TypeCodes.OBJECT || c == TypeCodes.GEMEINSAM) {
	    buf.getByte();
	    int end = buf.nextPositionOf(TERMINATOR);
	    Scalar ret = Scalar.parseUntagged(buf, end);
	    buf.setPosition(end);
	    buf.getByte();
	    return (c == TypeCodes.GEMEINSAM
		    ? (TypeName) ret.getGemeinsamTypeName()
		    : (TypeName) ret);
	} else {
	    return Primitive.make((char) buf.getByte());
	}
    }

    /**
     * Return the Gemeinsam name for an instance type name, or
     * the instance name for a Gemeinsam type name.
     **/
    public TypeName getSiblingTypeName() { return sibling; }

    /**
     * Convert this TypeName to a <code>Gemeinsame</code>.
     *
     * @return TypeName.Gemeinsam
     */
    public TypeName.Gemeinsam getGemeinsamTypeName() {
	return sibling.asGemeinsam();
    }

    /**
     * Convert this TypeName to an instance
     * (non-<code>Gemeinsam</code>) typename
     **/
    public TypeName getInstanceTypeName() {
	return this;
    }

    /**
     * The type name implementation for primitive types. <code>Primitive</code>
     * typenames, like <code>Scalar</code> typenames, contain a repository
     * utf8 index for their names. However, these names differ from those of
     * <code>Scalar</code> types; instead, they are the utf8 string
     * representations of their respective
     * {@link ovm.core.repository.TypeCodes TypeCodes}.<code>Primitive</code>
     * typenames are, as the name implies, used for primitive types such as
     * <code>int</code>,<code>long</code>,<code>boolean</code>, etc.
     */
    public static class Primitive extends TypeName {
        /**
	 * The repository utf8string index of the name of this type - this is
	 * the utf8string representation of its type code.
	 */
        private final int name_;
        /**
	 * The {@link ovm.core.repository.TypeCodes type code}for this
	 * primitive type.
	 */
        private final char tag_;
        /**
	 * The <code>String</code> representation of this primitive's
	 * {@link ovm.core.repository.TypeCodes type code}.
	 */
        private final String tagAsString_;

	private static final Primitive[] map = new Primitive[128];
	
        /**
	 * Create a new primitive type name.
	 * 
	 * @param tag
	 *                the {@link ovm.core.repository.TypeCodes type code}
	 *                character for this type
	 */
        protected Primitive(char tag) {
            tag_ = tag;
            tagAsString_ = new String("" + tag);
	    UnicodeBuffer b = UnicodeBuffer.factory().wrap(tagAsString_);
            name_ = UTF8Store._.installUtf8(b);
	    map[tag] = this;
        }
        public TypeName.Primitive asPrimitive() {
            return this;
        }
        // primitives are singletons
        public final boolean equals(Object otherType) {
            return otherType == this;
        }
        /**
	 * Returns true if the two names refer to the same package and
	 * class/interface name.
	 */
        public boolean equals(TypeName otherType) {
            return otherType.equalsFrom(this);
        }
        /**
	 * Compare another <code>TypeName.Primitive</code> to this type name
	 * for equality by double dispatching - this is the specific
	 * implementation for <code>TypeName.Primitive</code> receiver
	 * objects with <code>TypeName.Primitive</code> arguments. This
	 * determines equality by checking to see if the internal type names
	 * are equal.
	 * 
	 * @param obj
	 *                the <code>TypeName.Primitive</code> to compare to
	 *                this object
	 * @return true if the internal type names are equal, otherwise false
	 * @see TypeName#equals(TypeName)
	 */
        public final boolean equalsFrom(TypeName.Primitive obj) {
            return this.name_ == obj.name_;
        }
        public char getTypeTag() {
            return tag_;
        }
        public final int hashCode() {
            return name_;
        }
        /**
	 * Determine if this type name represents a primitive type. This always
	 * returns true for objects of this type.
	 * 
	 * @return true, since this is a primitive type.
	 */
        public final boolean isPrimitive() {
            return true;
        }
        // Return the length of this type name's FQN string.
        public final int length() {
            return 1;
        }
        // Write the this type name's FQN into the output stream.
        public final void write(OutputStream str) throws IOException {
            str.write((byte) tag_);
        }

	public static Primitive make(char tag) {
	    Primitive ret;
	    if (tag >= 128 || (ret = map[tag]) == null)
		throw new Error("unknown tag " + tag);
	    return ret;
	}
    } // End of TypeName.Primitive

    /**
     * An extension of the {@link TypeName.Primitive Primitive}class for wide
     * primitives (types such as <code>double</code> and <code>long</code>).
     * <code>WidePrimitive</code> typenames are essentially <code>Primitive</code>
     * typenames which can identify themselves as typenames for 2-word
     * primitive types.
     * 
     * @see TypeName.Primitive
     * @see TypeName
     */
    public static final class WidePrimitive extends Primitive {
        /**
	 * Create a new wide primitive type name.
	 * 
	 * @param tag
	 *                the {@link ovm.core.repository.TypeCodes type code}
	 *                character for this type
	 */
        private WidePrimitive(char tag) {
            super(tag);
        }

        // overrides default
        public TypeName.WidePrimitive asWidePrimitive() {
            return this;
        }
    } // End of TypeName.WidePrimitive

    /**
     * The abstract class to be extended by compound types. Compound types are
     * types that are not primitive types.
     */
    public abstract static class Compound extends TypeName {

        // overrides default
        public Compound asCompound() {
            return this;
        }
        // overrides default
        public boolean isCompound() {
            return true;
        }
        /**
	 * Return the name as required in the CONSTANT_Class_info bytecode
	 * structure (JVM Spec $4.2), i.e. fully qualified but without the
	 * leading 'L' in case of scalars. <strong>CAUTION.</strong> In the
	 * Ovm world we would not see, as in Java, a single class with both
	 * instance and static members. It becomes <em>two distinct types</em>,
	 * one of which has only the original instance members while the other
	 * (the shared state) has the original static members (now transformed
	 * to instance members). These two types, being distinct, have distinct
	 * TypeNames (they will not satisfy equals(), and toString() will
	 * return distinct values). However, <strong><em>this</em> method
	 * will return the <em>same</em> String for both types. You should
	 * use it only for purposes where you can tolerate or account for this
	 * behavior. Otherwise you risk confusing distinct Types.</strong>
	 */
        public abstract String toClassInfoString();

	/**
	 * Parse a classinfo string (per VM spec $4.2).  This method
	 * expects to consume the entire buffer.
	 **/
	public static Compound parseClassInfo(UnicodeBuffer buf) {
	    int first = buf.peekByte();
	    if (first == TypeCodes.ARRAY)
		return parse(buf).asArray();
	    else
		return Scalar.parseUntagged(buf, buf.getEndPosition());
	}
    } // end TypeName.Compound

    /**
     * The scalar type name implementation. <code>Scalar</code> types are
     * compound types which are non-array types. <code>Scalar</code>
     * typenames consist primarily of two indices into the repository utf8
     * store: one for the class/interface simple name, and one for the package
     * name.
     * 
     * @see TypeName
     */
    public static class Scalar extends Compound {

        /**
	 * The empty set of scalar types.
	 */
        public final static TypeName.Scalar[] EMPTY_SARRAY =
            new TypeName.Scalar[0];
        /**
	 * The repository utf8string index of this type's unqualified name (for
	 * example, <code>Object</code>).
	 */
        protected final int name;
        /**
	 * The repository utf8string index of this type's package name
	 */
        protected final int pack;

        /**
	 * Create a new type name for a class or interface.
	 * 
	 * @param pack
	 *                utf8string id for the package name.
	 * @param name
	 *                utf8string of the (unqualified) class name.
	 */
        private Scalar(int pack, int name) {
            this.pack = pack;
            this.name = name;
        }

	private static final SymbolTable map = new SymbolTable();

	protected Internable copy() { 
	    Scalar ret = new Scalar(pack, name);
	    ret.sibling = new Gemeinsam(ret);
	    return ret;
	}

	public static Scalar make(int pkgNameIndex,
				  int shortNameIndex) {
	    Object r1 = MemoryPolicy.the().enterRepositoryQueryArea();
	    try {
		Scalar probe = new Scalar(pkgNameIndex, shortNameIndex);
		return (Scalar) probe.intern(map);
	    } finally { MemoryPolicy.the().leave(r1); }
	}

        public TypeName.Scalar asScalar() {
            return this;
        }
        // Returns true if the two names refer to the same
        // package and class/interface name.
        public boolean equals(Object other) {
            return other instanceof TypeName
                ? ((TypeName) other).equalsFrom(this)
                : false;
        }

        // Returns true if the two names refer to the same
        // package and class/interface name.
        public boolean equals(TypeName other) {
            return other.equalsFrom(this);
        }
        /**
	 * Compare another <code>TypeName.Scalar</code> to this type name for
	 * equality by double dispatching - this is the specific implementation
	 * for <code>TypeName.Scalar</code> receiver objects with <code>TypeName.Scalar</code>
	 * arguments. This determines equality by checking to see that the
	 * package names and declared names for these types are equal.
	 * 
	 * @param other
	 *                the <code>TypeName.Scalar</code> to compare to this
	 *                object
	 * @return true if the typenames and package names of these types are
	 *         equal, else false.
	 * @see TypeName#equals(TypeName)
	 */
        public boolean equalsFrom(TypeName.Scalar other) {
            // assumption: compared types will differ more often in name than
            // in package, so testing name first is a win
            return other.name == this.name && other.pack == this.pack;
        }
        /**
	 * Returns the repository index of the utf8string of this type's
	 * package name. The name does not include a trailing '/'.
	 * 
	 * @return The repository id of the utf8string or <code>0</code> if
	 *         the class lives in the unnamed package.
	 */
        public int getPackageNameIndex() {
            return pack;
        }
        // Returns the id of the utf8string of the unqualified type name,
        // e.g. <code>Object</code>
        public int getShortNameIndex() {
            return name;
        }
        public char getTypeTag() {
            return TypeCodes.OBJECT;
        }
        public int hashCode() {
            return name ^ (pack << 3);
        }
        public boolean isScalar() {
            return true;
        }

        // Return the length of this type name's FQN string.
        public int length() {
            int name_len = UTF8Store._.getUtf8Length(name) + 2; // 'L' and ';'
            if (pack != 0) {
                name_len += UTF8Store._.getUtf8Length(pack) + 1; // and '/'
            }
            return name_len;
        }

        /**
	 * Output the fully qualified version of this type name to a <code>StringBuffer</code>.
	 * 
	 * @param buf
	 *                the <code>StringBuffer</code> to which this type
	 *                name should be written.
	 */
        void outputTo(StringBuffer buf) {
            if (pack != 0) {
                String s = UTF8Store._.getUtf8(pack).toString();
                if (s.equals(""))
                    throw new Error("Bad Repository! pack " + pack);
                buf.append(UTF8Store._.getUtf8(pack));
                buf.append(PACKAGE_TERMINATOR);
            }
            buf.append(UTF8Store._.getUtf8(name));
        }

        public String toClassInfoString() {
            StringBuffer buf = new StringBuffer(length()-2);
            outputTo(buf);
            return buf.toString();
        }

	/**
	 * Parse a slash-seperated class name starting at the
	 * buffer's current position and ending at the supplied
	 * position.  The buffer's current position after this call
	 * is undefined
	 **/
	public static Scalar parseUntagged(UnicodeBuffer buf, int end) {
	    int slash_pos = buf.lastPositionOf(TypeName.PACKAGE_TERMINATOR,
					       end);
	    int name;
	    int pack;

	    // or -1 if not found, or below current pos for something like 
	    // (Lp/C;LD;)V
	    if (slash_pos < buf.getPosition()) {
		pack = 0;
		name = UTF8Store._.installUtf8(buf.slice(end));
	    } else {
		pack = UTF8Store._.installUtf8(buf.slice(slash_pos));
		buf.setPosition(slash_pos);
		assert pack != 0 : "pack is null";
		char slash_char = (char) buf.getByte();
		assert slash_char == TypeName.PACKAGE_TERMINATOR
		    : ("couldn't eat up " +
		       TypeName.PACKAGE_TERMINATOR + ": " +
		       slash_char);
		name = UTF8Store._.installUtf8(buf.slice(end));
	    }
	    return make(pack, name);
	}

        // Write the this type name's FQN into the output stream.
        public void write(OutputStream out) throws IOException {
            out.write((byte) getTypeTag());
            if (pack != 0) {
                UTF8Store._.writeUtf8(out, pack);
                out.write((byte) PACKAGE_TERMINATOR);
            }
            UTF8Store._.writeUtf8(out, name);
            out.write((byte) TERMINATOR);
        }
    } // End of TypeName.Scalar

    /**
     * Typenames representing the type of the shared state of an
     * object.
     * <p>
     * KLB: Describe the whole Gemeinsam thing here, maybe with pictures.
     */
    public static class Gemeinsam extends TypeName.Scalar {

	public Gemeinsam(TypeName sibling) {
	    super(0, 0);
	    this.sibling = sibling;
	}

	public boolean equals(Object other) {
	    return other instanceof TypeName
		? ((TypeName) other).equalsFrom(this)
		: false;
	}

	public boolean equals(TypeName other) {
	    return other.equalsFrom(this);
	}

	public boolean equalsFrom(TypeName.Scalar other) { // uninherit
	    return false;
	}

	public boolean equalsFrom(TypeName.Scalar.Gemeinsam other) {
	    // invariants STYPE==DTYPE(this) and STYPE==DTYPE(other) will
	    // not
	    // hold in this invocation of super.equalsFrom, but that's ok
	    // as
	    // long as they hold here. i.e. have already verified both
	    // types
	    // are tn.s.gemeinsam, so the rest of the test is just that the
	    // tn.s components match, and the super test suffices.
	    return sibling.equals(other.sibling);
	}

	public TypeName.Gemeinsam getGemeinsamTypeName() {
	    return this;
	}
	public TypeName getInstanceTypeName() {
	    return sibling;
	}

	public char getTypeTag() {
	    return TypeCodes.GEMEINSAM;
	}
	public boolean isGemeinsam() {
	    return true;
	}
	public TypeName.Gemeinsam asGemeinsam() {
	    return this;
	}

	public int getShortNameIndex() {
	    return sibling.getShortNameIndex();
	}
	public int getPackageNameIndex() {
	    return sibling.getPackageNameIndex();
	}

	public int hashCode() {
	    return sibling.hashCode() + 1;
	}

	public int length() {
	    return (sibling.isScalar()
		    ? sibling.length()
		    : sibling.length() + 1);
	}

	public void write(OutputStream out) throws IOException {
	    out.write('G');
	    if (sibling.isScalar()) {
		Scalar lt = sibling.asScalar();
		if (lt.pack != 0) {
		    UTF8Store._.writeUtf8(out, lt.pack);
		    out.write((byte) PACKAGE_TERMINATOR);
		}
		UTF8Store._.writeUtf8(out, lt.name);
		out.write((byte) TERMINATOR);
	    } else {
		sibling.write(out);
	    }
	}

	/**
	 * toString for java.lang.Object's gtype is
	 * <code>Gjava/lang/Object;</code>, as before.
	 * <p>
	 * toString for java.lang.Object[]'s gtype is
	 * <code>G[Ljava/lang/Object;</code>.
	 * <p>
	 * toString for int's gtype is <code>GI</code>.
	 * <p>
	 * So, toString builds an unambigious classinfo representation
	 * when it's sibling is a TypeName.Compound, and maybe in all
	 * cases.  (It depends on whether it is OK to peak ahead to
	 * see whether you are looking at <code>GI</code>,
	 * <code>GI;</code>, or <code>GInterface;</code>).
	 **/
	public String toClassInfoString() {
	    return toString();
	}

    } // End of TypeName.Gemeinsam
    /**
     * This is the implementation of type names for array classes. <code>Array</code>
     * typenames contain an <code>element</code> typename and a <code>depth</code>
     * field. The <code>element</code> type refers to the type of 0th
     * dimension of the array; that is, the element type is the base type of an
     * array - it is the type that would be obtained by stripping all of the '
     * <code>[</code>' characters from a fully-qualified array name string.
     * The <code>depth</code> field refers to the number of dimensions of the
     * array (i.e. the number of '[' characters in the fully qualified name)
     */
    public static final class Array extends Compound {
	public static final Array[] EMPTY_AARRAY = new Array[0];
	
        /**
	 * The depth, or dimension of this array (for example, <code>[[[I</code>
	 * has a depth of 3)
	 */
        private final byte depth_;

        /**
	 * The innermost type name of this array (in other words, the type of
	 * this array, minus all the brackets (the 0th dimension))
	 */
        private final TypeName element_;
        /**
	 * Create a new array type name.
	 * 
	 * @param depth
	 *                the dimension of this array
	 * @param elem
	 *                the innermost type name of this array (the type of
	 *                the 0th dimension)
	 */
        private Array(TypeName elem, int depth) {
            assert(elem != null);
            this.depth_ = (byte) depth;
            this.element_ = elem;
        }
        public TypeName.Array asArray() {
            return this;
        }

        public boolean equals(Object otherType) {
            return otherType instanceof TypeName
                ? ((TypeName) otherType).equalsFrom(this)
                : false;
        }
        /**
	 * Returns true if the two names refer to the same package and
	 * class/interface name.
	 */
        public boolean equals(TypeName otherType) {
            return otherType.equalsFrom(this);
        }
        /**
	 * Compare another <code>TypeName.Array</code> to this type name for
	 * equality by double dispatching - this is the specific implementation
	 * for <code>TypeName.Array</code> receiver objects with <code>TypeName.Array</code>
	 * arguments. This determines equality by checking to see that the
	 * array depth (i.e. number of dimensions) and <i>element</i> type
	 * names for this array type name are equal. The <i>element</i> type
	 * names will be the innermost component type of the arrays.
	 * 
	 * @param obj
	 *                the <code>TypeName.Array</code> to compare to this
	 *                object
	 * @return true if the element objects and array depths of these types
	 *         are equal, else false.
	 * 
	 * @see TypeName#equals(TypeName)
	 */
        public boolean equalsFrom(TypeName.Array obj) {
            return obj.depth_ == this.depth_
                && obj.element_.equals(this.element_);
        }

        /**
	 * Get the component type of this array type. As opposed to
	 * {@link #getInnermostComponentTypeName()}, if this is an array of
	 * dimension <code>n</code>, this is the type of the object at
	 * dimension <code>(n - 1)</code>.
	 * 
	 * <p>
	 * So, for example, <code>[Ljava.lang.Object;</code> has a component
	 * type of type <code>Ljava.lang.Object</code>, but <code>[[[I</code>
	 * has a component type of type <code>[[I</code>.
	 * </p>
	 * 
	 * <p>
	 * In other words, this is the type obtained by stripping <em>one</em>
	 * layer of brackets
	 * </p>
	 * 
	 * @return the type name of the component type of this array.
	 */
        public TypeName getComponentTypeName() {
            return (depth_ > 1)
                ? make(element_, depth_ - 1)
                : element_;
        }

        /**
	 * Returns the array nesting depth for the array type corresponding to
	 * this type name (array nesting depth refers to the number of array
	 * dimensions; that is, an array of type <code>[[[I</code> has a
	 * depth of 3.)
	 * 
	 * @return the array depth.
	 */
        public int getDepth() {
            return depth_;
        }

        // Returns the <em>element</em> (i.e. innermost) type of an array type
        // name, or <code>null</code> if this object is not an array type. This
        // is the type of the component at the 0th dimension of the array.
        public TypeName getInnermostComponentTypeName() {
            return element_;
        }
        public char getTypeTag() {
            return TypeCodes.ARRAY;
        }

        public final int hashCode() {
            return element_.hashCode() / (depth_ + 1);
        }
        public boolean isArray() {
            return true;
        }
        // Return the length of this type name's FQN string.
        public int length() {
            return depth_ + element_.length();
        }

        public final String toClassInfoString() {
            return toString();
        }

        // Write the this type name's FQN into the output stream.
        public void write(OutputStream str) throws IOException {
            for (int i = depth_; i != 0; i--)
                str.write((byte) TypeCodes.ARRAY);
            element_.write(str);
        }

	public static Array make(TypeName eltType, int depth) {
	    if (eltType.isArray()) {
		Array eltArray = eltType.asArray();
		eltType = eltArray.element_;
		depth += eltArray.depth_;
	    }
	    Array ret;
	    if (depth >= eltType.arrayTypes_.length) {
		// grow array exponentially, since it may not be
		// subject to GC.
		int len = 1;
		while (len <= depth) len <<= 1;
		Object r = MemoryPolicy.the().enterRepositoryDataArea();
		try {
		    Array[] nt = new Array[len];
		    System.arraycopy(eltType.arrayTypes_, 0, nt, 0,
				     eltType.arrayTypes_.length);
		    eltType.arrayTypes_ = nt;
		} finally { MemoryPolicy.the().leave(r); }
	    }
	    if ((ret = eltType.arrayTypes_[depth]) == null) {
		Object r = MemoryPolicy.the().enterRepositoryDataArea();
		try {
		    ret = eltType.arrayTypes_[depth]
			= new Array(eltType, depth--);
		    while (depth > 0 && eltType.arrayTypes_[depth] == null)
			eltType.arrayTypes_[depth] 
			    = new Array(eltType, depth--);
		} finally { MemoryPolicy.the().leave(r); }
	    }
	    return ret;
	}
    } // End Of Array
} // End of TypeName
