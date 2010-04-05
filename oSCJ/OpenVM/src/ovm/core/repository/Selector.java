/**
 * @file ovm/core/Selector.java
 **/
package ovm.core.repository;

import java.io.OutputStream;
import java.io.IOException;

import ovm.core.services.memory.MemoryPolicy;
import ovm.services.bytecode.JVMConstants;
import ovm.util.OVMError;

/**
 * The abstract base class for <b>bound</b> <code>Selector</code> 
 * objects, which represent a binding between an unbound selector and a 
 * defining class. 
 *
 * Selectors denote references to methods and fields consisting of 
 * a defining class {@link ovm.core.repository.TypeName typename} and an 
 * {@link ovm.core.repository.UnboundSelector unbound selector}.
 * This <code>UnboundSelector</code>, in turn, is a pair which binds a 
 * method or field name to its type information (via its
 * {@link ovm.core.repository.Descriptor descriptor}). Thus,
 * effectively, a <code>Selector</code> is a triple representing
 * a method or field's defining class, member name and descriptor.<p>
 *
 * For example the following field definition:<p>
 *
 * <pre>
 * class Ex {
 *   int fi;
 * </pre>
 *
 * is denoted by the selector <code>(Ex, fi, I)</code>. Methods are
 * encoded in a similar fashion. In all cases, the type name associated
 * with a selector refers to the defining type for that reference.<p>
 *
 * Because of the binding between defining type and member information,
 * a <code>Selector</code> uniquely identifies a method or field.
 * 
 * <code>Selector</code> contains the basic functionality for the 
 * implementation of bound selectors; method-specific and 
 * field-specific functionality are implemented by its subclasses.<p>
 *
 * @author Jan Vitek 
 * @see UnboundSelector
 * @see TypeName 
 * @see Selector.Method
 * @see Selector.Field
 **/
public abstract class Selector extends RepositorySymbol.Internable {

    /**
     * The type name of the class that defined this selector's
     * field or method
     **/
    protected final TypeName.Compound definingClassName_;
    
    /**
     * The unbound selector this bound selector binds to the defining
     * class of the associated field/method
     **/
    protected final UnboundSelector selector_;
    
    /**
     * Constructor - binds an unbound selector to its defining class
     * @param selector the selector to be bound
     * @param typeName the class name the selector is bound to 
     **/
    protected Selector(UnboundSelector selector,
		       TypeName.Compound typeName) {
	this.definingClassName_ = typeName;
	this.selector_ = selector;
    }
    
    /**
     * Checks if the input object is equal to this selector; that
     * is, they have the same defining class and <code>UnboundSelector</code>.
     * @param o selector to compare against.
     * @return <code>true</code> if the two compared objects are equal; 
     * <code>false</code> otherwise.
     **/
    public boolean equals(Object o) {
	if (o == null)
	    return false;
	if (o == this)
	    return true;
	if (o instanceof Selector)
	    return ((Selector) o).equalsFrom(this);
	else
	    return false;
    }
    
    /**
     * Checks if the input object is equal to this selector; that
     * is, they have the same defining class and <code>UnboundSelector</code>.
     * @param o selector to compare against.
     * @return <code>true</code> if the two compared objects are equal; 
     * <code>false</code> otherwise.
     **/
    public boolean equals(Selector o) {
	if (o == null)
	    return false;
	if (o == this)
	    return true;
	return o.equalsFrom(this);
    }
    
    /**
     * Checks that input the <code>Selector</code> object is equal to
     * <code>this</code> by comparing the defining class name and 
     * unbound selector. 
     * @param o the selector to compare to this selector
     * @return boolean true if the input selector is equal, else false
     */
    public boolean equalsFrom(Selector o) {
	Selector sel = o;
	return definingClassName_ == sel.definingClassName_
	    && selector_ == sel.selector_;
    }
    
    /**
     * Returns the repository id of the typename of the defining class.
     * @return the repository id of the defining class typename
     **/
    public TypeName.Compound getDefiningClass() {
	return definingClassName_;
    }
    
    /**
     * Return the name of the field/method associated with this selector 
     * in its <code>String</code> representation.
     * @return the <code>String</code> representation of the name of this 
     *         field/method
     **/
    public String getName() {
	return selector_.getName();
    }
    
    /**
     * Return the repository id of the utf8string of the field/method name.
     * @return the repository utf8string index of the field/method name
     **/
    public int getNameIndex() {
	return selector_.getNameIndex();
    }
    
    /**
     * Computes the hash code associated with this selector.
     * FIXME: when and why is definingClassName_ null.  (Kacheck is one case).
     * @return hash value
     **/
    public int hashCode() {
	if (definingClassName_ != null)
	    return (selector_.hashCode() << 3) ^ definingClassName_.hashCode();
	else
	    return (selector_.hashCode() << 2);
    }
    
    /**
     * Write a selector to an output stream.
     **/
    public void write(OutputStream str) throws IOException {
	if (definingClassName_ != null)
	    definingClassName_.write(str);
	else
	    str.write('*');
	str.write('.');
	selector_.write(str);
    }
    
    /**
     * Return this selector as a method selector.
     * Since this is not a method selector, throw an error.
     * @throws OVMError.ClassCast
     */
    public Selector.Method asMethod() {
	throw new OVMError.ClassCast();
    }
	
    /**
     * Return this selector as a field selector.
     * Since this is not a field selector, throw an error.
     * @throws OVMError.ClassCast
     */
    public Selector.Field asField() {
	throw new OVMError.ClassCast();
    }
	
    private static final SymbolTable map = new SymbolTable();
    
    /**
     * Implementation of <b>bound</b> field selectors. Field 
     * <code>Selectors</code> bind an 
     * {@link UnboundSelector <i>unbound</i> selector} 
     * for a field to a defining class typename (represented as a 
     * <code>TypeName</code> object).
     * @see ovm.core.repository.Selector
     * @see TypeName
     **/
    public static class Field extends Selector implements ConstantFieldref {
	
	/**
	 * Interface for iterators over <code>Field</code> selectors
	 * @author Jan Vitek
	 */
	public interface Iterator {
	    /**
	     * Determine if there is a next element.
	     * @return boolean
	     */
	    boolean hasNext();
            
	    /**
	     * Get the next element in the sequence
	     * @return Selector.Field
	     */
	    Selector.Field next();
	} // end of Selector.Field.Iterator
        
	/**
	 * Constructor - binds an unbound field selector with the field's 
	 *               defining class
	 * @param sel the unbound selector for this field
	 * @param name the type name of the defining class
	 **/
	private Field(UnboundSelector.Field sel, TypeName.Compound name) {
	    super(sel, name);
	}

	protected Internable copy() {
	    return new Field(selector_.asField(), definingClassName_);
	}
	/**
	 * Return the canonical selector for a given name, type and
	 * definign class
	 * @param sel     the field's name and type
	 * @param definer the field's definer
	 **/
	public static Field make(UnboundSelector.Field sel,
				 TypeName.Compound definer) {
	    Object r = MemoryPolicy.the().enterRepositoryQueryArea();
	    try { return (Field) new Field(sel, definer).intern(map); }
	    finally { MemoryPolicy.the().leave(r); }
	}
		
	/**
	 * Return this selector as a {@link Selector.Field Selector.Field}
	 * object.
	 * @return Selector.Field
	 */
	public Selector.Field asField() {
	    return this;
	}

      public byte getTag() {
          return JVMConstants.CONSTANT_Fieldref;
      }
      public Selector.Field asSelector() {
          return this;
      }
      public boolean isResolved() {
          return false;
      }

	public Selector.Field makeLBound() {
	    if (definingClassName_.isGemeinsam()) {
		TypeName itn = definingClassName_.getSiblingTypeName();
		return make(getUnboundSelector(), itn.asCompound());
	    } else
		return this;
	}
	
	/** 
	 * Get the descriptor from this bound selector
	 * @return this field's descriptor
	 **/
	public Descriptor.Field getDescriptor() {
	    return ((UnboundSelector.Field) selector_)
		.getDescriptor();
	}
	
	/**
	 * Get the unbound selector from this bound selector
	 * @return this field's unbound selector
	 **/
	public UnboundSelector.Field getUnboundSelector() {
	    return (UnboundSelector.Field) selector_;
	}
    } // end of Selector.Field
    
    /**
     * Implementation of <b>bound</b> method selectors. Method
     * <code>Selectors</code> bind an 
     * {@link UnboundSelector <i>unbound</i> selector} 
     * for a method to a defining class typename (represented as a 
     * <code>TypeName</code> object).
     * @see ovm.core.repository.Selector
     * @see TypeName
     **/
    public static class Method extends Selector implements ConstantMethodref {
	
	/**
	 * Interface for iterators over <code>Method</code> selectors
	 * @author Jan Vitek
	 */
	public interface Iterator {
	    /**
	     * Determine if there is a next element.
	     * @return boolean
	     */
	    boolean hasNext();
	    /**
	     * Get the next element in the sequence
	     * @return Selector.Field
	     */
	    Selector.Method next();
	}
	/**
	 * Constructor - binds an unbound method selector with the defining
	 *               class
	 * @param sel the unbound selector for this field
	 * @param name the type name of the defining class
	 **/
	private Method(UnboundSelector.Method sel, 
		      TypeName.Compound name) {
	    super(sel, name);
	}

	protected Internable copy() {
	    return new Method(selector_.asMethod(), definingClassName_);
	}

	/**
	 * Return the canonical selector object for a given method
	 * signature in a declaring class.
	 * @param sel     the name and type of the method
	 * @param definer the defining class
	 **/
	public static Method make(UnboundSelector.Method sel,
				  TypeName.Compound definer) {
	    Object r = MemoryPolicy.the().enterRepositoryQueryArea();
	    try { return (Method) new Method(sel, definer).intern(map); }
	    finally { MemoryPolicy.the().leave(r); }
	}

	public Selector.Method makeLBound() {
	    if (definingClassName_.isGemeinsam()) {
		TypeName itn = definingClassName_.getSiblingTypeName();
		return make(getUnboundSelector(), itn.asCompound());
	    } else
		return this;
	}
	
	public byte getTag() {
	    return JVMConstants.CONSTANT_Methodref;
	}
	public Selector.Method asSelector() {
	    return this;
	}
	public boolean isResolved() {
	    return false;
	}

	/**
	 * Return this selector as a {@link Selector.Method Selector.Method}
	 * object.
	 * @return Selector.Method
	 */
	public Selector.Method asMethod() {
	    return this;
	}
	
	/** 
	 * Get the descriptor from this {@link Selector bound selector}
	 * @return this method's descriptor
	 **/		
	public Descriptor.Method getDescriptor() {
	    return ((UnboundSelector.Method) selector_)
		.getDescriptor();
	}
	
	/**
	 * Get the unbound selector from this {@link Selector bound selector}
	 * @return this methods's unbound selector
	 **/		
	public UnboundSelector.Method getUnboundSelector() {
	    return (UnboundSelector.Method) selector_;
	}
	
	/**
	 * Determine if the method corresponding to this bound
	 * selector is a constructor.
	 * @return true if the method is a constructor; otherwise, false.
	 **/		
	public boolean isConstructor() {
	    return ((UnboundSelector.Method) selector_)
		.isConstructor();
	}
	/**
	 * Determine if the method corresponding to this bound
	 * selector is a class initializer.
	 **/		
	public boolean isClassInit() {
	    return ((UnboundSelector.Method) selector_).isClassInit();
	}
	
    } // end of Selector.Method
} // end of Selector
