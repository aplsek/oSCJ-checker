package ovm.core.repository;

import ovm.util.ArrayList;
import ovm.util.Collections;
import ovm.util.List;
import ovm.util.OVMError;
import ovm.util.UnicodeBuffer;
import s3.core.S3Base;

/**
 * RepositoryMember is the abstract base class for methods, fields, 
 * and other repository class members.
 **/
public abstract class RepositoryMember extends S3Base {

    /**The base class for fields in the repository
     */
    public static class Field extends RepositoryMember {
    	
        /** Builder methods for <code>Field</code> objects
         */
        public static class Builder extends RepositoryBuilder {
        	
            /**
             * The default access mode for fields built with this builder
             **/
            final static Mode.Field DEFAULT_ACCESS_MODE =
                Mode.makeDefaultField();

            /**
             * The constant value bits for the field to be built; this is
             * called <code>constantValueBits</code> because this constant
             * value could, for example, be a float.
             **/
            private long constantValueBits;
	    private boolean hasConstantValue = false;

            /**
             * The descriptor for the field to be built
             **/
            private Descriptor.Field descriptor;

            /**
             * The access modifiers for the field to be built
             **/
            private Mode.Field mode;

            /**
             * The repository utf8string index of the name of the field to be
             * built.
             **/
            private int nameIndex;

	    /**
	     * The TypeName of the class defining the Field
	     **/
	    private TypeName.Compound typeName;
            
	    /**
	     * Build a field object corresponding to this Builder
	     * @return a field object based upon this builder
	     */
            public RepositoryMember.Field build() {
		if (typeName == null)
		    throw new Error("You must set a typename for the field!");
                UnboundSelector.Field uselector =
		    UnboundSelector.Field.make(nameIndex, descriptor);
                return new Field(Selector.Field.make(uselector, typeName),
				 mode,
				 constantValueBits,
				 hasConstantValue,
				 super.getAttributes());
            }

	    /**
	     * Used to set the TypeName of this field to the TypeName of the 
	     * class declaring this field
	     **/
	    public final void setTypeName(TypeName.Compound tn) {
		this.typeName = tn;
	    }

            public final void declareDeprecated() {
                declareAttribute(Attribute.deprecatedField);
            }

            public final void declareSynthetic() {
                declareAttribute(Attribute.Synthetic.Field.SINGLETON);
            }

            // initialize this builder
            public void reset() {
                super.reset();
                descriptor = null;
                constantValueBits = 0L;
		hasConstantValue = false;
                mode = DEFAULT_ACCESS_MODE;
                nameIndex = 0;
            }

	    /**
	     * Set this builder's field's constant value
	     * @param constantValue the constant value object to be set
	     */
            public void setConstantValue(Object constantValue) {
                if (constantValue == null)
                    return;
		hasConstantValue = true;

                TypeName type = descriptor.getType();
                switch (type.getTypeTag()) {
                    case TypeCodes.GEMEINSAM :
                        fail("Can this happen?");
                    case TypeCodes.OBJECT :
                        {
               		    if (type == JavaNames.java_lang_String) {
				// for convenience, allow both Strings and
				// RepositoryStrings to be passed.  Clients
				// should give preference to RepositoryStrings.
				if (constantValue instanceof String) {
				    UnicodeBuffer buf =
					UnicodeBuffer.factory().wrap
					((String) constantValue);
				    constantValueBits
					= UTF8Store._.installUtf8(buf);
				} else {
				    constantValueBits
					= ((RepositoryString)constantValue).getUtf8Index();
				}
				return;
			    }
			    assert(false);
			}
                    case TypeCodes.ARRAY :
                        assert(false);
                    case TypeCodes.BOOLEAN :
                    case TypeCodes.INT :
                    case TypeCodes.SHORT :
                    case TypeCodes.BYTE :
                    case TypeCodes.CHAR :
                        constantValueBits =
                            ((Integer) constantValue).intValue();
                        return;
                    case TypeCodes.LONG :
                        constantValueBits = ((Long) constantValue).longValue();
                        return;
                    case TypeCodes.FLOAT :
                        constantValueBits =
                            Float.floatToIntBits(
                                ((Float) constantValue).floatValue());
                        return;
                    case TypeCodes.DOUBLE :
                        constantValueBits =
                            Double.doubleToLongBits(
                                ((Double) constantValue).doubleValue());
                        return;

                    default :
                        assert(false);
                        return;
                }
            }

            /**
             * Set the bits of a constant value as an int (for
             * Strings it is the utf8 index). This is called
             * <code>constantValueBits</code> because the constant
             * value may have some non-integer representation
             * (i.e. <code>float</code>, <code>double</code>, etc).
             * @param constantValueBits the long representation of the
             *                          constant value bits to be set
             */
            public void setConstantValueBits(int constantValueBits) {
		hasConstantValue = true;
                this.constantValueBits = constantValueBits;
            }

			/**
			 * Set the bits of a constant value as a long (for
			 * Strings it is the utf8 index). This is called
			 * <code>constantValueBits</code> because the constant
			 * value may have some non-integer representation
			 * (i.e. <code>float</code>, <code>double</code>, etc.).
			 * @param constantValueBits the long representation of the
			 *                          constant value bits to be set
			 */
            public void setConstantValueBits(long constantValueBits) {
		hasConstantValue = true;
                this.constantValueBits = constantValueBits;
            }


	    /**
	     * Set this builder's field descriptor
	     * @param descriptor field descriptor for this field
	     */
            public void setDescriptor(Descriptor.Field descriptor) {
                this.descriptor = descriptor;
            }

	    /**
	     * Set this builder's field modifiers
	     * @param mode the modifiers for this field
	     */
            public void setMode(Mode.Field mode) {
                this.mode = mode;
            }

	    public Mode.Field getMode() { return mode; }

	    /**
	     * Set this builder's field name
	     * @param nameIndex the repository index of
	     *                  the utf8string of the field name
	     */
            public void setName(int nameIndex) {
                assert this.nameIndex == 0
		    : "name index already set to " + this.nameIndex;
                this.nameIndex = nameIndex;
            }
        } // end RepositoryMember.Field.Builder

        /**
         * Iterator interface for <code>Field</code> objects
         **/
        interface Iterator {
            /**
             * Determines if there is a next element in the iteration
             * sequence
             * @return true if there is a next element, false otherwise
             **/
            public boolean hasNext();

            /**
             * Retrieves the next element in the iteration sequence
             * @return the next <code>Field</code> element in the iteration 
             *         sequence
             **/
            public RepositoryMember.Field next();

        } // end RepositoryMember.Field.Iterator

        /**
        * The empty set of fields
        **/
        public static final Field[] EMPTY_ARRAY = new Field[0];

        /**
         * The array of attributes associated with this field
         **/
        private Attribute[] attributes_;

        /**
         * The constant value of this field (this is a long, but it
         * may contain the binary representation of a float, double,
         * etc.)
         **/
        private long constantValue_;

	private boolean hasConstantValue;

        /**
         * The access modifiers object for this field
         **/
        private final Mode.Field mode_;

        /**
         * The unbound selector for this field
         **/
        private Selector.Field selector_;

        /**
         * Create a new field object, initializing its constant value
         * bits and attributes to 0 and null, respectively.
         * @param mode the field's access modifiers object
         * @param selector the unbound selector for this field
         */
        Field(Mode.Field mode,
	      Selector.Field selector) {
            this(selector, mode, 0L, false, null);
        }

        /**
         * Create a new field object, setting its constant value
         * bits and attributes.
         * @param selector         the new field's unbound selector
         * @param mode             the field's access modifers object
         * @param constantValue    constant value for this field, or 0.
	 * @param hasConstantValue true if constantValue means anything
         * @param attributes       attributes for this field, or null.
         */
        Field(
            Selector.Field selector,
            Mode.Field mode,
            long constantValue,
	    boolean hasConstantValue,
            Attribute[] attributes) {
            this.mode_ = mode;
            this.constantValue_ = constantValue;
	    this.hasConstantValue = hasConstantValue;
            this.attributes_ = attributes;
            this.selector_ = selector;
        }

        // A visitor pattern accept method.
        public void accept(RepositoryProcessor v) {
            v.visitField(this);
        }

        /**
         * Get the attributes associated with this <code>Field</code>
         * @return an array containing this field's attributes
         **/
        public Attribute[] getAttributes() {
            return attributes_;
        }
        
        /**
	 * Return this field's constant value as an object, or null if
	 * no constant value is defined.<p>
         *
         * If this value is a <code>String</code>, the constant value
         * repository utf8string index is used to return a <code>String</code>
         * containing a copy of the utf8string.</p><p>
         *
         * Primitive fields that are not of type <code>long</code>, 
         * <code>float</code>, or <code>double</code> will return
         * an <code>Integer</code> representation of their constant value.
         * Fields of type <code>long</code>, <code>float</code>, or 
         * <code>double</code> will return the wrapped value of the corresponding
         * wrapper type.</p><p>
         *
         * @return this field's constant value
         */
        public Object getConstantValue() {
	    if (!hasConstantValue)
		return null;
	    
            // get the field's type
            TypeName type = selector_.getDescriptor().getType();
            switch (type.getTypeTag()) {
                case TypeCodes.OBJECT :
                    {
                        if (type == JavaNames.java_lang_String)
                            return new RepositoryString((int) constantValue_);
			else
			    assert false: "non-string constant value";
                    }
                case TypeCodes.ARRAY :
		    assert false: "array constant value";
                case TypeCodes.BOOLEAN :
                case TypeCodes.INT :
                case TypeCodes.SHORT :
                case TypeCodes.BYTE :
                case TypeCodes.CHAR :
                    return new Integer((int) constantValue_);
                case TypeCodes.LONG :
                    return new Long(constantValue_);
                case TypeCodes.FLOAT :
                    return new Float(
                        Float.intBitsToFloat((int) constantValue_));
                case TypeCodes.DOUBLE :
                    return new Double(Double.longBitsToDouble(constantValue_));
                default :
                    throw failure("getConstantValue of a " + type);
            }
        }

	/**
	 * Return true if this field has a constant value.
	 **/
	public boolean hasConstantValue() {
	    return hasConstantValue;
	}
	    /**
	     * Return the bits of the constant value as a long (for
	     * Strings it is the utf8 index)
	     * @return the bits of this field's constant value as a
	     *         long
		 */         
        public long getConstantValueBits() {
            return constantValue_;
        }

	public Selector.Field getSelector() {
	    return selector_;
	}

  	public Selector getUnrefinedSelector() {
	    return selector_;
	}
	
	/**
	 * Return this field descriptor (from the unbound selector).
	 * @return this field's descriptor
	 */
        public Descriptor.Field getDescriptor() {
            return selector_.getDescriptor();
        }

		/**
		 * Return this field's modifiers.
		 * @return this field's modifiers
		 */
        public Mode.Field getMode() {
            return mode_;
        }

        // Return the field name as String (from the unbound selector).
        public String getName() {
            return selector_.getName();
        }

        // Return the field name as repository utf8 index (from the
        // unbound selector).
        public int getNameIndex() {
            return selector_.getNameIndex();
        }

		/**
		 * Return this field's unbound selector
		 * @see UnboundSelector
		 * @return UnboundSelector.Field the unbound selector 
		 *         corresponding to this field.
		 *        
		 */
        public UnboundSelector.Field getUnboundSelector() {
            return selector_.getUnboundSelector();
        }

        // Return the field descriptor (from the unbound selector).
        public Descriptor getUnrefinedDescriptor() {
            return selector_.getDescriptor();
        }
        // Return the field's access modifiers.
        public Mode.Member getUnrefinedMode() {
            return mode_;
        }

        /**
         * Return the string representation of this field. This is actually
         * the string representation of its unbound selector.
         * @return the String representation of this field
         * @see ovm.core.repository.Selector.Field#toString
         **/
        public String toString() {
            if (selector_ != null)
                return selector_.toString();
            else
                return "S3Field";
        }

        /**
         * Visit attributes of this <code>Field</code>.
         * @param x a visitor for this <code>Field</code>.
         **/
        public void visitAttributes(RepositoryProcessor x) {
            for (int i = 0; i < attributes_.length; i++)
                attributes_[i].accept(x);
        }

        public void visitComponents(RepositoryProcessor v) {
            v.visitFieldMode(mode_);
        }

    } // end RepositoryMember.Field

    /**
     * The base interface for methods in the repository
     **/
    public static class Method extends RepositoryMember {

        public static class Builder extends RepositoryBuilder {

            /**                         
             * This makes the default access mode object
             * for methods to be built with this builder class
             **/
            final static Mode.Method DEFAULT_ACCESS_MODE =
                Mode.makeDefaultMethod();

            /**              
             * The code fragment of the method
             * to be built of this builder
             **/
            private Bytecode codeFragment;

            /**                         
             * The descriptor of the method to be built
             **/
            private Descriptor.Method descriptor;

            /**                         
             * The access modifiers of the method to be built
             **/
            private Mode.Method mode;

            /**                         
             * The repository utf8string index of the name
             * of the method to be built with this builder
             **/
            private int nameIndex;

	    /**
	     * The TypeName of the class defining the Method
	     **/
	    private TypeName.Compound typeName;

            /**                         
             * The list of exceptions the method to be
             * built may throw
             **/
            private List thrownExceptions;

	    public Builder() {
	    }

	    public Builder(RepositoryMember.Method orig) {
		this.descriptor = orig.getDescriptor();
		this.mode = orig.getMode();
		this.nameIndex = orig.getNameIndex();
		this.codeFragment = orig.getCodeFragment();
		this.typeName = orig.getSelector().getDefiningClass();
		this.thrownExceptions = Collections.EMPTY_LIST;
		TypeName.Scalar[] te = orig.getThrownExceptions();
		for (int i=0;i<te.length;i++)
		    declareThrownException(te[i]);
	    }

	    /**
	     * Set the code fragment to be attached to <code>Method</code>
	     * objects built with this builder.
	     * @param codeF the code fragment to be added to the Method
	     */
            public void addFragment(Bytecode codeF) {
                this.codeFragment = codeF;
            }

            /**
             * Build a Method object according to this builder's settings.
             * @return RepositoryMember.Method the new Method
             */
            public RepositoryMember.Method build() {

                TypeName.Scalar[] exceptionsArr;
                if (thrownExceptions == Collections.EMPTY_LIST)
                    exceptionsArr = TypeName.Scalar.EMPTY_SARRAY;
                else
                    exceptionsArr =
                        (TypeName.Scalar[]) thrownExceptions.toArray(
                            new TypeName.Scalar[thrownExceptions.size()]);
//PARBEGIN --the code fragment can be null.
                Bytecode codeF = null;
                if (codeFragment!= null)
                	codeF = new Bytecode.Builder(codeFragment,true).build();
//PAREND
                UnboundSelector.Method uselector =
                    UnboundSelector.Method.make(nameIndex, descriptor);
                verify();
                return new Method(Selector.Method.make(uselector, typeName),
				  mode,
				  codeF,//PARBEGIN PAREND
				  exceptionsArr,
				  super.getAttributes());
            }

	    /**
	     * Used to set the TypeName of this method to the TypeName of the 
	     * class declaring this method
	     **/
    	    public final void setTypeName(TypeName.Compound tn) {
		this.typeName = tn;
	    }

            public final void declareDeprecated() {
                declareAttribute(Attribute.deprecatedMethod);
            }

            public final void declareSynthetic() {
                declareAttribute(Attribute.Synthetic.Method.SINGLETON);
            }

            /**
             * Declare an exception to be thrown by methods built with
             * this builder.
             * @param exName the TypeName of the exception to be thrown
             */
            public void declareThrownException(TypeName.Scalar exName) {
                if (thrownExceptions == Collections.EMPTY_LIST)
                    thrownExceptions = new ArrayList();
                thrownExceptions.add(exName);
            }

            /**
             * Get the descriptor associated with methods that are built
             * with this builder
             * @return Descriptor.Method this method's descriptor
             */
            public Descriptor.Method getDescriptor() {
                if (descriptor == null)
                    throw new OVMError("descriptor not set yet");
                return descriptor;
            }

            /**
             * Get the set of modifiers for methods to be built with this
             * builder.
             * @return Mode.Method the modifiers for this method
             */
            public Mode.Method getMode() {
                return mode;
            }

            /**
             * Get the utf8 index of the name of the method being built
             * @return int this method's name index
             */
            public int getName() {
                if (nameIndex == 0)
                    throw new OVMError("name not set yet");
                return nameIndex;
            }

            // reset this builder's internal state
            public void reset() {
                super.reset();
                descriptor = null;
                mode = DEFAULT_ACCESS_MODE;
                codeFragment = null;
                thrownExceptions = Collections.EMPTY_LIST;
                nameIndex = 0;
            }

            /**
             * Set this builder's method descriptor.
             * @param d the descriptor for this method
             */
            public void setDescriptor(Descriptor.Method d) {
                assert descriptor == null:
		    "setDescriptor(): should not be nonnull";
                descriptor = d;
            }

            /**
             * Get this builder's method modifiers
             * @param m the modifiers for this method
             */
            public void setMode(Mode.Method m) {
                mode = m;
            }

            /**
             * Set the name of this builder's method
             * @param n the repository index of the utf8string 
             *          of the method name
             */
            public void setName(int n) {
                nameIndex = n;
            }

            /**
             * Verify that this method can actually be built (in other words,
             * its code is non-null, it is non-abstract and non-native - it
             * has a method body)
             * @throws ClassFormatError if the code is null, or the method is
             *                          abstract or native
             **/
            public void verify() throws ClassFormatError {
                if (typeName == null)
                    throw new Error("You must set a typename for the method!");
                if (codeFragment == null && !(mode.isAbstract() || mode.isNative()))
                    throw new ClassFormatError( "Method body required for " + 
                            UTF8Store._.getUtf8(nameIndex) + ":" + descriptor + " in " + typeName);
            }

        } // end RepositoryMember.Method.Builder

        /**
         * Iterator interface for <code>Method</code> objects
         **/
        interface Iterator {
            /**
             * Determines if there is a next element in the iteration
             * sequence
             * @return true if there is a next element, false otherwise
             **/
            public boolean hasNext();

            /**
             * Retrieves the next element in the iteration sequence
             * @return the next <code>Method</code> element in the iteration 
             *         sequence
             **/
            public RepositoryMember.Method next();

        } // end RepositoryMember.Method.Iterator

        /**
         * The empty set of methods
         **/
        static final Method[] EMPTY_ARRAY = new Method[0];
        /** 
         * The attributes associated with the method
         **/
        private final Attribute[] attributes_;
        /** 
         * The code fragment associated with this method
         **/
        private final Bytecode code_;

        /** 
         * The class in which this method was defined
         **/
       // private RepositoryClass definingClass_;
        /** 
         * The access modifiers for this method
         **/
        private final Mode.Method mode_;
        /** 
         * This method's unbound selector
         **/
        private Selector.Method selector_;
        /** 
         * The exceptions declared to be thrown by this method
         **/
        private final TypeName.Scalar[] thrownExceptions_;

        // Constructors --------------------------------------

        /**
         * Create a new method definition. Each method belongs to a class and
         * has a selector, access modifiers, a code fragment and optionally
         * exceptions. The method name and type is encoded in the selector.
         *
         * @param selector this method's selector.
         * @param mode this method's access modifiers.
         * @param code this method's code fragment.
         * @param exceptions the exceptions thrown by this method.
         * @param attributes the attributes associated with this method
         **/
        Method(
            Selector.Method selector,
            Mode.Method mode,
            Bytecode code,
            TypeName.Scalar[] exceptions,
            Attribute[] attributes) 
	{
            this.selector_ = selector;
            this.mode_ = mode;
            this.code_ = code;
            this.thrownExceptions_ = exceptions;
            this.attributes_ = attributes;

        }

        /**
         * A visitor pattern accept method.
         * @param v the visitor to accept
         */
        public void accept(RepositoryProcessor v) {
            v.visitMethod(this);
        }
        /**
         * Get the attributes associated with this <code>Method</code>
         * @return an array containing this method's attributes
         **/
        public Attribute[] getAttributes() {
            return attributes_;
        }

	public Selector.Method getSelector() {
	    return selector_;
	}

  	public Selector getUnrefinedSelector() {
	    return selector_;
	}

        /**
         * Return the method's code.
         * @return the bytecode of this method
         */
        public Bytecode getCodeFragment() {
            return code_;
        }

        /** 
         * Return this method's descriptor (from the unbound selector).
         * @return the descriptor for this method
         */
        public Descriptor.Method getDescriptor() {
            return selector_.getDescriptor();
        }

        /**
         * Return this method's access modifiers.
         * @return the access modifiers object for this method.
         */
        public Mode.Method getMode() {
            return mode_;
        }
 
        public String getName() {
            return selector_.getName();
        }

        public int getNameIndex() {
            return selector_.getNameIndex();
        }
        
        /**
         * Return an array of the type names of the exceptions thrown
         * by this method. This array is not null.
         * @return TypeName.Scalar[] a non-null array of the type names of
         *                           exceptions thrown by this method.
         */
        public TypeName.Scalar[] getThrownExceptions() {
            return thrownExceptions_;
        }

        /**
         * Return an array of type names of the exceptions thrown by this
         * method.  Note: never null.
         * @return TypeName.Scalar[] a non-null array of the type names of
         *                           exceptions thrown by this method.
         */
        TypeName[] getThrownS3Exceptions() {
            return thrownExceptions_;
        }

        /** 
         * Return this method's unbound selector.
         * @return UnboundSelector.Method the unbound selector for this method.
         */
        public UnboundSelector.Method getUnboundSelector() {
            return selector_.getUnboundSelector();
        }

        // Return the method descriptor (from the unbound selector).
        public Descriptor getUnrefinedDescriptor() {
            return selector_.getDescriptor();
        }
        // Return the access modifiers.
        public Mode.Member getUnrefinedMode() {
            return mode_;
        }

        /**
         * Determine if this method is a constructor.
         * @return boolean true if this method is a constructor, else false.
         */
        public boolean isConstructor() {
            return selector_.isConstructor();
        }

        /**
         * Return the string representation of this method. This is actually
         * the string representation of its selector.
         * @return the String representation of this method
         * @see ovm.core.repository.Selector.Method#toString
         **/
        public String toString() {
            return selector_.toString();
        }
        
        /**
         * Visit attributes of this <code>Method</code>.
         * @param x a visitor for this method object.
         **/
        public void visitAttributes(RepositoryProcessor x) {
            for (int i = 0; i < attributes_.length; i++)
                attributes_[i].accept(x);
        }

        /**
         * Visit components of the method (Mode, Selector, CodeFragments, 
         * Exceptions, Attributes), not the method object itself.
         * The order IS important!
         * @param v the visitor which will visit the components
         **/
        public void visitComponents(RepositoryProcessor v) {
            v.visitMethodMode(mode_);
            if (code_ != null)
                code_.accept(v);
            visitExceptions(v);
            visitAttributes(v);
        }

        /**
         * Visit the exceptions thrown by this method.
         * @param v the visitor to visit the exceptions of this method.
         */
        public void visitExceptions(RepositoryProcessor v) {
            for (int i = 0; i < thrownExceptions_.length; i++)
                v.visitThrowsDeclaration(thrownExceptions_[i]);
        }

    } // end RepositoryMember.Method

    /**
     * A visitor pattern accept method.
     * @param v a visitor for this <code>RepositoryMember</code>
     **/
    public abstract void accept(RepositoryProcessor v);

    /**
     * Get the array of attributes associated with this member
     * @return the array of attributes associated with this member
     **/
    public abstract Attribute[] getAttributes();

    /**
     * Return the member name as String (from the unbound selector).
     * @return the name of this member
     **/
    public abstract String getName();

    /**
     * Return the member name as repository utf8 index (from the
     * unbound selector).
     * @return the repository utf8 index of the member name
     **/
    public abstract int getNameIndex();

    /**
     * Return this member's descriptor (from the unbound selector).
     * @return the descriptor for this member
     **/
    public abstract Descriptor getUnrefinedDescriptor();

    public abstract Selector getUnrefinedSelector();

    /**
     * Return this member's modifiers.
     * @return this member's modifiers
     **/
    public abstract Mode.Member getUnrefinedMode();

    /**
     * Visit attributes of this RepositoryMember
     * @param x a visitor for the attributes
     **/
    public abstract void visitAttributes(RepositoryProcessor x);

    /**
     * Visit the object graph reachable from <code>this</code>,
     * except for <code>this</code> itself.
     * @param v a visitor for the components
     **/
    public abstract void visitComponents(RepositoryProcessor v);

} // end RepositoryMember
