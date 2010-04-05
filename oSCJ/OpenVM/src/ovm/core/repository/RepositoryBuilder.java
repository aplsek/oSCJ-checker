/**
 * @file ovm/core/repository/RepositoryBuilder.java
 **/
package ovm.core.repository;

import s3.core.S3Base;

/**
 * Abstract builder class for repository objects. Builders subclassing 
 * this abstract class are used to declare properties of a given 
 * repository object (Class, Method, etc) and then produce that an 
 * instance object with the builder. This is the common superclass for
 * builders with functionality for dealing with attributes. Subclasses
 * of this class build specific repository objects.
 *
 * <p>Builders are used to construct instances of other classes. They are
 * stateful (for this reason they are not called Factories, which are
 * usually stateless). Their state can be reset using the
 * <code>reset</code> method; thus they can be reused to build many
 * objects one after another (they are not intended to be thread safe).
 * 
 * @author Krzystof Palacz, Jan Vitek
 **/
/* this used to be nonpublic but apparently javac generated weird code:
   a s3.services.bytecode.bootimage.MutableByteCodeFragment.Builder has method
   build() that calls RepositoryBuilder.getAttributes(). javap showed 
   invokevirtual #9 <Method null> for that entry ....
   Making RepositoryBuilder a public class solved the problem ....
*/
public abstract class RepositoryBuilder extends S3Base {

    /* 
     * Benchmarking suggests that the overwhelming majority
     * of RepositoryBuilder have 1 attributes, then there are a few 2
     * attributes and finally some 3 attributes.
     */

    /**
     * The attribute set for this builder - since the first
     * three elements are explicit fields, this is initially
     * used for attributes beyond the third attribute, but
     * once {@link #getAttributes} has been called, this
     * will actually contain all of the attributes and should
     * be treated as immutable.
     **/
    private Attribute[] attributes = Attribute.EMPTY_ARRAY;

    /**
     * The first attribute slot for this builder
     **/
    private Attribute first_element;

    /**
     * This determines whether or not we are allowed to
     * declare any more attributes. If this is true, we have
     * already called {@link #getAttributes} and are no
     * longer allowed to add more attributes to the builder.
     **/
    protected boolean freeze_attributes_;

    /**
     * Index of the first empty attribute slot in the attribute set
     **/
    private int position_in_attributes;

    /**
     * The second attribute slot for this builder
     **/
    private Attribute second_element;

    /**
     * The third attribute slot for this builder
     **/
    private Attribute third_element;
    // ------------------------------------------------

    /**
     * Constructor - initializes the builder via its {@link #reset()} method
     **/
    protected RepositoryBuilder() {
        this.reset();
    }

    /**
     * Declare an attribute in this builder.
     * <p>
     * Note that if {@link #getAttributes} has already been called, the 
     * attribute set is considered frozen and this will fail.
     * </p>
     * @param attribute the attibute which is to be declared
     **/
    public final void declareAttribute(Attribute attribute) {
        if (freeze_attributes_)
            fail("Attribute declaration on frozen attribute set");
        try {

            // Find the first empty attribute slot
            if (position_in_attributes == 0) {
                first_element = attribute;
            } else if (position_in_attributes == 1) {
                second_element = attribute;
            } else if (position_in_attributes == 2) {
                third_element = attribute;
            } else {
                // The explicit fields were full, so we now add to
                // the attribute array

                // Expand the array if necessary
                if (attributes == Attribute.EMPTY_ARRAY)
                    attributes = new Attribute[10];
                if (position_in_attributes == attributes.length) {
                    Attribute[] tmp = new Attribute[2 * attributes.length + 10];
                    System.arraycopy(attributes, 0, tmp, 0, attributes.length);
                    attributes = tmp;
                }

                // Add the attribute
                attributes[position_in_attributes - 3] = attribute;
            }
            position_in_attributes++;
        } catch (ClassCastException e) {
            throw new Error("Unsupported attribute implementation");
        }
    }

    public void removeAttribute(Attribute attribute) {
	throw new Error("not implemented");
    }

    public void replaceAttribute(Attribute old,
				 Attribute newAttr) {	    
	if (first_element == old) {
	    first_element = newAttr;
	    return;
	}
	if (second_element == old) {
	    second_element = newAttr;
	    return;
	}
	if (third_element == old) {
	    third_element = newAttr;
	    return;
	}
	for (int i=attributes.length-1;i>=0;i--)
	    if (attributes[i] == old) {
		attributes[i] = newAttr;
		return;
	    }
    }


    /**
     * Declare an third-party attribute in this builder.     
     * <p>
     * Note that if {@link #getAttributes} has already been called, the 
     * attribute set is considered frozen and this will fail.
     * @param nameIndex the repository utf8string index of the name of
     *                  this attribute
     * @param contents the byte array containing the contents of the
     *                 third-party attribute   
     * </p>
     */
    public final void declareAttribute(int nameIndex, 
				       byte[] contents) {
	declareAttribute(new Attribute.ThirdParty.Method(nameIndex, contents));
    }

    /**
     * Declare a deprecated attribute for this builder.
     * <p>
     * Note that if {@link #getAttributes} has already been called, the 
     * attribute set is considered frozen and this will fail.
     * </p>
     */
    public abstract void declareDeprecated();

    /**
     * Declare a synthetic attribute for this builder.
     * <p>
     * Note that if {@link #getAttributes} has already been called, the 
     * attribute set is considered frozen and this will fail.
     * </p>
     */
    public abstract void declareSynthetic();

    /**
     * Declare a sourcefile attribute for this builder     
     * <p>
     * Note that if {@link #getAttributes} has already been called, the 
     * attribute set is considered frozen and this will fail.
     * </p>
     */
    public final void declareSourceFile(int sourceFileNameIndex) {
        if (freeze_attributes_)
            fail("Attribute declaration on frozen attribute set");

        declareAttribute(new Attribute.SourceFile(sourceFileNameIndex));
    }

    /**
     * Return the attributes of this builder as an array without 
     * any empty entries (note: this means that the array has the exact
     * size of the number of declared attributes, NOT that an empty array
     * of size 0 will not be returned). If no attributes have
     * been declared, we return the empty array.
     *
     * <p>
     * Once this is called, further attributes cannot be
     * declared without calling {@link #reset()} first.
     * @return array of this builder's attributes
     */
    public final Attribute[] getAttributes() {
        // If we've already called this method before, or
        // if we haven't declared any attributes, just
        // return the attributes array
        if (position_in_attributes == attributes.length) {
            freeze_attributes_ = true;
            return attributes;
        }

        // Otherwise, create a new attributes array containing
        // all of the attributes (including the first 3), 
        // and prevent the addition of new attributes.
        freeze_attributes_ = true;

        Attribute[] tmp = new Attribute[position_in_attributes];

        // copy in the first three elements (there is at least ONE)
        tmp[0] = first_element;
        if (position_in_attributes > 1)
            tmp[1] = second_element;
        if (position_in_attributes > 2)
            tmp[2] = third_element;

        // add the existing attributes array, if non-empty, to the
        // new array
        if (position_in_attributes > 3) {
            System.arraycopy(attributes, 0, tmp, 3, position_in_attributes - 3);
        }
        attributes = tmp;

        return attributes;
    }

    /**
     * Reset initializes all internal fields.
     */
    public void reset() {
        attributes = Attribute.EMPTY_ARRAY;
        position_in_attributes = 0;
        freeze_attributes_ = false;
    }

} 