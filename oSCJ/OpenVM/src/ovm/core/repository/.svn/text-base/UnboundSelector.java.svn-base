package ovm.core.repository;

import java.io.IOException;
import java.io.OutputStream;

import ovm.core.services.memory.MemoryPolicy;
import ovm.util.OVMError;
import ovm.util.UnicodeBuffer;

/**
 * Unbound selectors denote references to methods and fields consisting of
 * a pair of a member name and a descriptor.
 *
 * <p>For example the following field definition:
 * <pre>
 * class Ex {
 *   int fi;
 * </pre>
 * is denoted by the unbound selector <code>(fi, I)</code>. Methods are
 * encoded in a similar fashion. 
 *
 * When created or retrieved with a <code>ByteBuffer</code> or
 * <code>String</code>, an <code>UnboundDescriptor</code> has the following
 * format:<p>
 *
 * <pre>
 *     &lt;name&gt; ':' &lt;type&gt;
 * </pre>
 *
 * where <code>&lt;name&gt;</code> is the name of the field or method
 * and <code>&lt;type&gt;</code> is the type denoted by the descriptor.
 * <i>(For method and field descriptor encoding, see the JVM Spec $4.3, or
 * {@link ovm.core.repository.Descriptor})</i></p>
 *
 * <p>UnboundSelectors uniquely identify members within a class but do not
 * necessarily represent all the typing information. <b>Note that
 * <code>UnboundSelectors</code> differ from <code>Selectors</code>
 * in that <code>UnboundSelectors</code> have no binding with their
 * defining classes.</b>.
 *
 * @see ovm.core.repository.Selector
 * @author Jan Vitek
 **/
public abstract class UnboundSelector extends RepositorySymbol.Internable {

	/**
	 * The member name for this selector
	 **/
	protected final int name_;
	// protected final S3Descriptor descriptor_;

	/**
	 * Create a new selector containing a member name
	 * This is a stored as utf8 character sequence
	 * in the repository.
	 *
	 * @param name offset of the member name in the repository
	 **/
	protected UnboundSelector(int name) {
		this.name_ = name;
	}

	/**
	 * Return <code>this</code> as an unbound field selector.
	 * @return UnboundSelector.Field
	 * @throws OVMError.ClassCast error if this unbound selector is
	 *                            not a field selector.
	 */
	public UnboundSelector.Field asField() {
		throw new OVMError.ClassCast();
	}

	/**
	 * Return <code>this</code> as an unbound method selector.
	 * @return UnboundSelector.Method
	 * @throws OVMError.ClassCast error if this unbound selector is
	 *                            not a method selector.
	 */
	public UnboundSelector.Method asMethod() {
		throw new OVMError.ClassCast();
	}

	/**
	 * Returns the name for this selector as a <code>String</code>
	 * @return the <code>String</code> representation of the
	 *         name of this selector.
	 **/
	public String getName() {
		return UTF8Store._.getUtf8(name_).toString();
	}

	/**
	 * Return the repository id of the utf8string of the 
	 * selector's name.
	 * @return the repository index of the utf8string of this
	 *         selector's name
	 **/
	public int getNameIndex() {
		return name_;
	}
	
	/**
	 * Determine if this unbound selector is a field selector
	 * @return boolean true if this is a field selector, else false
	 */
	public boolean isField() {
		return false;
	}
	
	/**
	 * Determine if this unbound selector is a method selector
	 * @return boolean true if this is a method selector, else false
	 */
	public boolean isMethod() {
		return false;
	}

    /**
     * Parse an OVM-format unbound selector <name>:<descriptor>
     * I have no clue why this format is used, since methods come out
     * very ugly this way.
     **/
    public static UnboundSelector parse(UnicodeBuffer buf) {
	int colon_pos = buf.firstPositionOf(':');
	assert(colon_pos > 0);
	buf.setPosition(colon_pos);
	int name = UTF8Store._.installUtf8(buf.sliceBefore());
	buf.getByte();
	return make(name, Descriptor.parse(buf));
    }

    public static UnboundSelector make(int nameIndex, Descriptor desc) {
	if (desc instanceof Descriptor.Field)
	    return Field.make(nameIndex, (Descriptor.Field) desc);
	else
	    return Method.make(nameIndex, (Descriptor.Method) desc);
    }

    private static final SymbolTable map = new SymbolTable();

    /**
     * The implementation of <code>UnboundSelector</code> for fields.
     * This class binds a field name with its 
     * {@link ovm.core.repository.Descriptor descriptor}.
     **/
    public final static class Field extends UnboundSelector {

        /**
         * The descriptor associated with this selector.
         * @see ovm.core.repository.Descriptor
         **/
        private final Descriptor.Field descriptor_;

        /**
         * Create a new <code>UnboundDescriptor</code> for
         * a field object
         * @param name the name of the field
         * @param descriptor the descriptor associated with the field
         **/
        private Field(int name, Descriptor.Field descriptor) {
            super(name);
            assert(descriptor != null);
            this.descriptor_ = descriptor;
        }

	protected Internable copy() {
	    return new Field(name_, descriptor_);
	}
	
        public static Field make(int name, Descriptor.Field desc) {
	    Object r = MemoryPolicy.the().enterRepositoryQueryArea();
	    try { return (Field) new Field(name, desc).intern(map); }
	    finally { MemoryPolicy.the().leave(r); }
	}
	
	public void write(OutputStream str) throws IOException {
	    UTF8Store._.writeUtf8(str, name_);
	    str.write(':');
	    descriptor_.write(str);
	}

        public UnboundSelector.Field asField() {
            return this;
        }

        /**
         * Returns true if the objects have the same name and descriptor,
         * and the input object is an <code>S3UnboundSelector.Field</code>.
         * @param o object to compare against.
         * @return true if the objects have the same name and descriptor,
         *         else false.
         **/
        public boolean equals(Object o) {
            if (o == null)
                return false;
            else if (o instanceof UnboundSelector.Field)
                return equals((UnboundSelector.Field) o);
            else
                return false;
        }

        /**
         * Returns true if the objects have the same name and descriptor.
         * @param o selector to compare against.
         * @return true if the objects have the same name and descriptor,
         *         else false.
         **/
        boolean equals(UnboundSelector.Field o) {
            if (o == null)
                return false;
            return name_ == o.name_ && descriptor_.equals(o.descriptor_);
        }

		/**
		 * Returns the descriptor associated with the field represented by
		 * this selector
		 * @return the descriptor for this field
		 **/
        public Descriptor.Field getDescriptor() {
            return descriptor_;
        }
        /**
         * Returns the hash value of this <code>UnboundSelector</code>
         * @return the hashcode for this unbound selector.
         **/
        public int hashCode() {
            return name_ ^ descriptor_.hashCode();
        }

        public boolean isField() {
            return true;
        }

	/**
	 * Iterator for <code>UnboundSelector.Field</code>
	 * objects.
	 **/
	interface Iterator {
	    /**
	     * Determine if there a next element in the iteration sequence.
	     * @return true if there is a next element, else false
	     **/
	    public boolean hasNext();
	    /**
	     * Get the next element in the iteration sequence.
	     * @return the next method unbound selector in the sequence
	     **/
	    public UnboundSelector.Field next();

	} // end of UnboundSelector.Field.Iterator
		
    } // end of UnboundSelector.Field

    /**
     * The implementation of <code>UnboundSelector</code> for methods.
     * This class binds a method name with its 
     * {@link ovm.core.repository.Descriptor descriptor}.
     **/
    public static final class Method extends UnboundSelector {

        /**
         * The descriptor associated with this selector.
         * @see ovm.core.repository.Descriptor
         **/
        private final Descriptor.Method descriptor_;

        /**
         * Create a new <code>UnboundDescriptor</code> for
         * a method object
         * @param name the name of the method
         * @param descriptor the descriptor associated with the method
         **/
        private Method(int name, Descriptor.Method descriptor) {
            super(name);
            assert descriptor != null : "descriptor should be nonnull";
            this.descriptor_ = descriptor;
        }

	protected Internable copy() {
	    return new Method(name_, descriptor_);
	}

	static public Method make(int name, Descriptor.Method desc) {
	    Object r = MemoryPolicy.the().enterRepositoryQueryArea();
	    try { return (Method) new Method(name, desc).intern(map); }
	    finally { MemoryPolicy.the().leave(r); }
	}

	public void write(OutputStream str) throws IOException {
	    UTF8Store._.writeUtf8(str, name_);
	    str.write(':');
	    descriptor_.write(str);
	}

        public UnboundSelector.Method asMethod() {
            return this;
        }

        /**
         * Returns true if the objects have the same name and descriptor,
         * and the input object is an <code>S3UnboundSelector.Method</code>.
         * @param o object to compare against.
         * @return true if the objects have the same name and descriptor,
         *         else false.
         **/
        public boolean equals(Object o) {
            if (o == null)
                return false;
            else if (o instanceof UnboundSelector.Method)
                return equals((UnboundSelector.Method) o);
            else
                return false;
        }

        /**
         * Returns true if the objects have the same name and descriptor.
         * @param o selector to compare against.
         * @return true if the objects have the same name and descriptor,
         *         else false.
         **/
        boolean equals(UnboundSelector.Method o) {
            if (o == null)
                return false;
            // FIXME shouldn't it be descriptor_ == o.descriptor_
            return name_ == o.name_ && descriptor_.equals(o.descriptor_);
        }
        
		/**
		 * Returns the descriptor associated with the method represented by 
		 * this selector
		 * @return the descriptor for this method
		 **/
        public Descriptor.Method getDescriptor() {
            return descriptor_;
        }

        /**
         * Returns the hash value of this <code>UnboundSelector</code>
         * @return the hashcode for this unbound selector.
         **/
        public int hashCode() {
            return name_ ^ descriptor_.hashCode();
        }

        /**
         * Determine if the method specified by this
         * <code>UnboundSelector</code> is a constructor.
         * @return true if the method is a constructor, else false.
         **/
        public boolean isConstructor() {
            for (int i=0;i<JavaNames.INIT_NAMES.length;i++) 
        		if (JavaNames.INIT_NAMES[i]==name_) return true;
	    return false;
        } 
        
        /**
         * Determine if the method specified by this <code>UnboundSelector</code>
         * is a class initializer.
         */
        public boolean isClassInit() {
            return JavaNames.CLINIT.name_ == name_; //FIXME: is the CLINIT a singleton? Can we compare against 'this'?
        }
        
        public boolean isMethod() {
            return true;
        }

	/**
	 * Iterator for <code>UnboundSelector.Method</code>
	 * objects.
	 **/
	interface Iterator {
	    /**
	     * Determine if there is a next element in the iteration sequence.
	     * @return true if there is a next element, else false
	     **/
	    public boolean hasNext();

	    /**
	     * Get the next element in the iteration sequence.
	     * @return the next method unbound selector in the sequence
	     **/
	    public UnboundSelector.Method next();

	} // end of UnboundSelector.Method.Iterator
		
    } // end of UnboundSelector.Method

} // end of UnboundSelector.java
