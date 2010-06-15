package ovm.core.repository;

import java.io.IOException;
import java.io.OutputStream;

import ovm.core.services.memory.MemoryPolicy;
import ovm.util.List;
import ovm.util.OVMError;
import ovm.util.UnicodeBuffer;

/**
 * A descriptor instance stands for the unique type descriptor
 * of a method type or field type.<p>
 *
 * Field descriptors consist of a typename denoting the field's type, 
 * while method descriptors consist of the method's return type along
 * with a variable length list of argument types.<p>
 *
 * A Descriptor does not uniquely identify a member of a
 * <code>RepositoryClass</code> as it only represents the
 * typing information for a class member. In order to uniquely
 * identify the member, it must be bound with an
 * {@link ovm.core.repository.UnboundSelector unbound selector}.<p> 
 *
 * When creating and dealing with descriptors as <code>ByteBuffers</code>
 * or <code>Strings</code>, the expected input or output representation
 * will correspond to the grammar described in section 4.3 of the JVM
 * Specification. This grammar is listed below:
 *
 * <pre>
 * For fields:
 *     FieldDescriptor: FieldType
 *     ComponentType: FieldType
 *     FieldType: BaseType | ObjectType | ArrayType
 *     BaseType: B | C | D | F | I | J | S | Z
 *     ObjectType: L&lt;classname&gt;;
 *     ArrayType: [ComponentType
 *     &lt;classname&gt;: (fully qualified class or interface name)
 *
 * For methods:
 *     MethodDescriptor: (ParameterDescriptor*)ReturnDescriptor
 *     ParameterDescriptor: FieldType
 *     ReturnDescriptor: FieldType | V
 *                       (where V is for void return types)
 * </pre>    
 * 
 * This class also contains convenience methods to inspect the sizes of the 
 * types.
 *
 * This class is abstract; the real functionality is implemented
 * by its two subclasses.
 * 
 * @author Jan Vitek 
 **/
public abstract class Descriptor extends RepositorySymbol.Internable {
    
    // Fields ------------------------------------------------------------
    
    /**
     * The return type name if this is a method or the field type name 
     * if it is a field.
     **/
    final TypeName return_or_field_type_;
    
    // Constructors ------------------------------------------------------
    
    /**
     * Create a new descriptor of the given field type or method
     * return-type.
     * @param tp the field's type or method's return type.
     **/
    protected Descriptor(TypeName tp) {
	return_or_field_type_ = tp;
    }
    
    // *****************************************************************
    // *****************************************************************
    // *****************************************************************
    
    /**
     * Determine whether this Descriptor is equal to the input object
     * @return true if equal, else false
     */
    public abstract boolean equals(Object other);
    
    /**
     * Get the field type (if this is a Field descriptor) or the return
     * type (if this is a Method descriptor) of this object.
     * @return TypeName the field or return type of this desriptor
     */
    final public TypeName getType() {
	return return_or_field_type_;
    }

	// Methods ------------------------------------------------------------

	/**
	 * Return this descriptor's hash value.
	 * @return the hash value of this descriptor
	 **/
	public int hashCode() {
		return return_or_field_type_.hashCode();
	}

	/**
	 * Determine if the type of this field (or return type of this method)
	 * is a reference type.
	 * @return true if this is a reference type, or false if it is a primitive 
	 *         type.
	 **/
	public boolean isReference() {
		return return_or_field_type_.isPrimitive() == false;
	}

	/** 
	 * Return the length of the string representing this descriptor.
	 * @return the length of the string representation of this descriptor
	 **/
	public final int length() {
		return return_or_field_type_.length();
	}

    private static SymbolTable map = new SymbolTable();

    /**
     * Parse the descriptor starting at the current position in buf.
     **/
    public static Descriptor parse(UnicodeBuffer buf) {
	if (buf.peekByte() != '(') {
	    return Field.make(TypeName.parse(buf));
	}
	buf.getByte();

	// Parse a method descriptor
	TypeName arg0 = null;
	TypeName arg1 = null;
	TypeName arg2 = null;
	TypeName[] argv = null;
	final int delta = 3;
	int argc = 0;
	while (buf.peekByte() != ')') {
	    TypeName tn = TypeName.parse(buf);
	    switch (argc) {
	    case 0 :
		arg0 = tn;
		break;
	    case 1 :
		arg1 = tn;
		break;
	    case 2 :
		arg2 = tn;
		break;
	    default : // argc >= 3
		if (argv == null)
		    argv = new TypeName[2 * delta];
		if (argc >= argv.length) {
		    TypeName[] tmp = new TypeName[argv.length + delta];
		    System.arraycopy(argv, 0, tmp, 0, argv.length);
		    argv = tmp;
		}
		argv[argc] = tn;
	    }
	    argc++;
	}
	buf.getByte();
	TypeName ret = TypeName.parse(buf);
	assert(!buf.hasMore());
	assert(ret != null);
	// Let's never allocate descriptors on the heap.
	Object r = MemoryPolicy.the().enterRepositoryQueryArea();
	try {
	    Descriptor desc;
	    if (argc < 4) {
		desc = new SmallMethod(arg0, arg1, arg2, ret);
	    } else {
		TypeName[] tp = new TypeName[argc - 3];
		for (int i = 3; i < argc; i++) {
		    tp[i - 3] = argv[i];
		}
		desc = new BigMethod(arg0, arg1, arg2, tp, ret);
	    }
	    return (Descriptor) desc.intern(map);
	} finally {  MemoryPolicy.the().leave(r); }
    }
	    
    // abstract void verify() throws ClassFormatError;

    /** 
     * Write the fully-qualified name of this descriptor's method
     * return type or field type into the output stream.
     * @param out the <code>OutputStream</code> to which the
     *            name should be written
     **/
    public void write(OutputStream out) throws IOException {
	return_or_field_type_.write(out);
    }

    /**
     * This is the field representation of <code>Descriptor</code>. Field 
     * descriptors consist of a typename denoting the field's type.
     * 
     * <p>When creating and dealing with field descriptors as 
     * <code>ByteBuffers</code> or <code>Strings</code>, the expected input 
     * or output representation will correspond to the grammar described in 
     * section 4.3 of the JVM Specification. This grammar is listed below:
     *
     * <pre>
     *     FieldDescriptor: FieldType
     *     ComponentType: FieldType
     *     FieldType: BaseType | ObjectType | ArrayType
     *     BaseType: B | C | D | F | I | J | S | Z
     *     ObjectType: L&lt;classname&gt;;
     *     ArrayType: [ComponentType
     *     &lt;classname&gt;: (fully qualified class or interface name)
     * </pre>    
     * @see Descriptor
     **/
    public final static class Field extends Descriptor {
        /**
         * Create a new field descriptor
         * @param type the type name of the field's type
         */
        private Field(TypeName type) {
            super(type);
        }

	protected Internable copy() {
	    return new Field(return_or_field_type_);
	}
	
	public static Field make(TypeName type) {
	    Object r = MemoryPolicy.the().enterRepositoryQueryArea();
	    try { return (Field) new Field(type).intern(map); }
	    finally { MemoryPolicy.the().leave(r); }
	}

        /**
         * Compare an object with this field descriptor for equality. 
         * They are considered equal if they have the same types.
         *
         * @param o the object to compare against this descriptor
         *          for equality
         * @return true if the descriptors are considered equal,
         *         else false.
         */
        public boolean equals(Object o) {
            if (o == this)
                return true;
            else if (o instanceof Descriptor.Field)
                return equalsFrom((Descriptor.Field) o);
            else
                return false;
        }
        
        /**
         * Compare this descriptor with another field descriptor.
         * Return true if they have the same types.
         * @return true if the input descriptor has the same
         *         type has this descriptor, else false.
         */
        public boolean equalsFrom(Descriptor.Field d) {
            if (return_or_field_type_ != d.return_or_field_type_) {
                return false;
            }
            return true;
        }

		/**
		 * Determine if this field's type is a primitive.
		 * @return true if this field's type is primitive, else false
		 */
        public boolean isPrimitive() {
            return return_or_field_type_.isPrimitive();
        }

		/**
		 * Determine if this field's type is a wide primitive (2 words).
		 * @return true if this field's type is a wide primitive, else false
		 */
        public boolean isWidePrimitive() {
            return return_or_field_type_.isWidePrimitive();
        }

		/**
		 * Determine the size of this field in words.
		 * @return the size of the field in words (either 1 or 2)
		 */
        public int wordSize() {
            switch (return_or_field_type_.getTypeTag()) {
                case TypeCodes.LONG :
                case TypeCodes.DOUBLE :
                    return 2;
                default :
                    return 1;
            }
        }
    } // End of Descriptor.Field

    /**
     * This is the method representation of <code>Descriptor</code>. Method 
     * descriptors consist of the method's return type along with a variable 
     * length list of argument types. 
     * 
     * <p>When creating and dealing with method descriptors as 
     * <code>ByteBuffers</code> or <code>Strings</code>, the expected input 
     * or output representation will correspond to the grammar described in 
     * section 4.3 of the JVM Specification. This grammar is listed below:
     *
     * <pre>
     *     MethodDescriptor: (ParameterDescriptor*)ReturnDescriptor
     *     ParameterDescriptor: FieldType
     *     ReturnDescriptor: FieldType | V
     *                       (where V is for void return types)
     * </pre>    
     * 
     * <p>
     * <code>FieldType</code> is defined in the grammar for fields described
     * in {@link Descriptor} and {@link Descriptor.Field}.
     * <p>
     * This is an abstract class. See specific implementations for further
     * details.
     * 
     * @see Descriptor
     **/
    public static abstract class Method extends Descriptor {

        // --- First three argument typenames --------------
        /**
         * The first argument to this descriptor's method
         **/
        protected final TypeName argument0;

        /**
         * The second argument to this descriptor's method
         **/
        protected final TypeName argument1;

        /**
         * The third argument to this descriptor's method
         **/
        protected final TypeName argument2;

        // volatile ?
        /**
         * The cached value of this descriptor's hashcode. It
         * is initialized to -1 and is set to the correct value
         * when {@link #hashCode} is called for the first time.
         * Subsequent calls to {@link #hashCode} will return
         * this value instead of computing it again.
         **/
        protected int precomputedHashCode_ = -1;
        // ----------------------------------------------------

        /**
         * Constructor - creates a new method descriptor given the
         * first three argument type names and the return type name.
         * Methods with more than three arguments should be handled
         * by a class which extends this one and explicitly deals with
         * the additional arguments.
         * @param arg0 the first method argument
         * @param arg1 the second method argument
         * @param arg2 the third method argument
         **/
        protected Method(TypeName arg0,
			 TypeName arg1,
			 TypeName arg2,
			 TypeName ret) {
            super(ret);
            this.argument0 = arg0;
            this.argument1 = arg1;
            this.argument2 = arg2;
        }

	public static Method make(List args, TypeName ret)  {
	    Object r = MemoryPolicy.the().enterRepositoryQueryArea();
	    try {
		// FIXME:? we may allocate an array of up to 252 objects!
		int length;
		length = args == null ? 0 : args.size();

		TypeName arg0 = null;
		TypeName arg1 = null;
		TypeName arg2 = null;
		if (length > 0) {
		    arg0 = (TypeName) args.get(0);
		    if (length > 1) {
			arg1 = (TypeName) args.get(1);
			if (length > 2) {
			    arg2 = (TypeName) args.get(2);
			}
		    }
		}

		Method desc;
		if (length < 4) {
		    desc = new SmallMethod(arg0, arg1, arg2, ret);
		} else {
		    TypeName[] tp = new TypeName[length - 3];
		    for (int i = 3; i < length; i++) {
			tp[i - 3] = (TypeName) args.get(i);
		    }
		    desc = new BigMethod(arg0, arg1, arg2, tp, ret);
		}
		return (Method) desc.intern(map);
	    } finally { MemoryPolicy.the().leave(r); }
	}

	/**
	 * Get the size of the argument type in words (either 1 or 2)
	 * @param n the number of the argument in this method
	 * @return the size of the argument in words
	 */
        public int argumentWordSize(int n) {
            switch (getArgumentType(n).getTypeTag()) {
                case TypeCodes.VOID :
                    throw new OVMError(this +" arg " + n + " has void type?!");
                case TypeCodes.LONG :
                case TypeCodes.DOUBLE :
                    return 2;
                default :
                    return 1;
            }
        }

	/**
	 * Get the number of arguments for this method
	 * @return  number of arguments of the method
	 */
        public int getArgumentCount() {
            int count = 0;
            if (argument0 != null) {
                count++;
                if (argument1 != null) {
                    count++;
                    if (argument2 != null) {
                        count++;
                    }
                }
            }
            return count;
        }
	
	/**
	 * Return the length in byte of the method argument vector. A plain
	 * field is 4 bytes long, while wide fields are 8 bytes. 
	 * @return the length of the method argument vector, in bytes
	 */
        public int getArgumentLength() {
            int length = 0;
            if (argument0 != null) {
                length += (argument0.isWidePrimitive()) ? 8 : 4;
                if (argument1 != null) {
                    length += (argument1.isWidePrimitive()) ? 8 : 4;
                    if (argument2 != null) {
                        length += (argument2.isWidePrimitive()) ? 8 : 4;
                    }
                }
            }
            return length;
        }

	/**
	 * Returns the typename of the nth argument of this method.
	 * @param n the number of the argument, 
         *        n < <code>getArgumentCount()</code>
	 * @return typename or <code>null</code> if either n out of bounds or 
	 *         this is a field descriptor.
	 **/
        public TypeName getArgumentType(int n) {
            TypeName tn = null;
            switch (n) {
                case 0 :
                    tn = argument0;
                    break;
                case 1 :
                    tn = argument1;
                    break;
                case 2 :
                    tn = argument2;
                    break;
            }
            if (tn == null) {
                throw new ArrayIndexOutOfBoundsException(n);
            }
            return tn;
        }

		/**
		 * Get the number of wide arguments for this method
		 * @return  number of arguments with wide (long or double) type
		 */
		public int getWideArgumentCount() {
            return getArgumentLength() / 4 - getArgumentCount();
        }


        public int hashCode() {
            // 53, according to Christian, is used because
            // it was empirically shown to cause the fewest
            // collisions in hash tables.
            if (precomputedHashCode_ == -1) {
                precomputedHashCode_ = super.hashCode() * 53;
                if (argument0 != null) {
                    precomputedHashCode_ += argument0.hashCode();
                    if (argument1 != null) {
                        precomputedHashCode_ += argument1.hashCode();
                        if (argument2 != null) {
                            precomputedHashCode_ += argument2.hashCode();
                        }
                    }
                }
            }
            return precomputedHashCode_;
        }

		/**
		 * Determine if the <code>n</code>-th argument is a wide primitive 
		 * (2 words)
		 * @param n the number of the argument in this method
		 * @return true if the argument is a wide primitive, else false
		 **/
        public boolean isArgumentWidePrimitive(int n) {
            return argumentWordSize(n) == 2;
        }

        /**
         * Determine if this method's return type is a primitive.
         * Matching the behavior of {@link TypeName#isPrimitive()}, this method
         * returns true for any primitive type (including those for which
         * {@link #isReturnValueWidePrimitive()} also returns true).
         * @return true if return type is primitive, else false
         **/
        public boolean isReturnValuePrimitive() {
            return return_or_field_type_.isPrimitive();
        }

        /**
         * Determine whether this descriptor's method's return type is void.
         * @return true if the method's return type is void, else false
         **/
        public boolean isReturnValueVoid() {
            return return_or_field_type_.getTypeTag() == TypeCodes.VOID;
        }

        /**
         * Determine if this method's return type is a wide primitive 
         * (2 words).
         * Matching the behavior of {@link TypeName#isPrimitive()}, for any
         * type where this method returns true, {@link #isReturnValuePrimitive()} will
         * return true also.
         * @return true if return type is a wide primitive, else false
         **/
        public boolean isReturnValueWidePrimitive() {
            return return_or_field_type_.isWidePrimitive();
        }

		/**
		 * Get the size of this method's return value in words 
		 * (either 0,1 or 2)
		 * @return the return value's size in words
		 **/
        public int returnValueWordSize() {
            switch (return_or_field_type_.getTypeTag()) {
                case TypeCodes.VOID :
                    return 0;
                case TypeCodes.LONG :
                case TypeCodes.DOUBLE :
                    return 2;
                default :
                    return 1;
            }
        }

        /** 
         * Write this descriptor into an output stream. This will be written 
         * in the method descriptor format that appears as specified by the 
         * JVM Spec, section 4.3.
         * @param out the output stream to write the descriptor to
         * @throws IOException if there was an IOException thrown while
         *                     writing one of the descriptor components
         *                     to the stream
        	 * @see ovm.core.repository.Descriptor
         **/
        public void write(OutputStream out) throws IOException {
            out.write((byte) '(');
            if (argument0 != null) {
                argument0.write(out);
                if (argument1 != null) {
                    argument1.write(out);
                    if (argument2 != null) {
                        argument2.write(out);
                    }
                }
            }
            writeRest(out);
            out.write((byte) ')');
            super.write(out);
        }

        /**
         * Writes all arguments after the 3rd argument to the stream if 
         * this descriptor's method contains more than three arguments;
         * if it does not, this method just returns.
         * @param out the stream to write the arguments to
         * @throws IOException if writing any of the arguments to
         *                     the stream causes an <code>IOException</code>
         *                     to be thrown
         **/
        protected void writeRest(OutputStream out) throws IOException {
        }

    } // End of Descriptor.Method

	/**
	 * Method descriptor implementation class for methods with fewer 
	 * than three arguments. Method descriptors consist of the method's 
	 * return type along with a variable length list of argument types. 
	 * Methods with 3 or more arguments should use the 
	 * {@link Descriptor.BigMethod} class.
	 **/
	public final static class SmallMethod extends Method {

	    /**
	     * Create a new method descriptor for a method with 3 or
	     * fewer arguments.
	     * @param arg0 the typename of the first method argument for this
	     *             descriptor (can be null)
	     * @param arg1 the typename of the second method argument for this
	     *             descriptor (can be null)
	     * @param arg2 the typename of the third method argument for the 
	     *             this descriptor (can be null)
	     * @param retType the typename of the method return type for this
	     *                descriptor
	     **/
	    SmallMethod(
			TypeName arg0,
			TypeName arg1,
			TypeName arg2,
			TypeName retType) {
		super(arg0, arg1, arg2, retType);
	    }

	    protected Internable copy() {
		return new SmallMethod(argument0, argument1, argument2,
				       return_or_field_type_);
	    }
	    
	    /**
	     * Compare an object with this method descriptor for equality. 
	     * They are considered equal if they are both <code>SmallMethod</code>
	     * descriptors and have the same return and argument types.
	     *
	     * @param o the object to compare against this descriptor
	     *          for equality
	     * @return true if the descriptors are considered equal,
	     *         else false.
	     **/
	    public boolean equals(Object o) {
		if (o == this) {
		    return true;
		} else if (o instanceof Descriptor.SmallMethod) {
		    // only makes sense if the classes are the same. --jv
		    return ((Descriptor.SmallMethod) o).equalsFrom(this);
		} else {
		    return false;
		}
	    }
	    /**
	     * Compare this <code>SmallMethod</code> descriptor with another 
	     * <code>SmallMethod</code> descriptor and see if they have the
	     * same return and argument types.
	     * @param d the method descriptor to compare against
	     **/
	    public boolean equalsFrom(Descriptor.SmallMethod d) {
		return return_or_field_type_ == d.return_or_field_type_
		    && argument0 == d.argument0
		    && argument1 == d.argument1
		    && argument2 == d.argument2;
	    }
	} // End of Descriptor.SmallMethod

	/**
	 * Method descriptor implementation class for methods with more than three
	 * arguments. Method descriptors consist of the method's 
	 * return type along with a variable length list of argument types.
	 * Methods with 3 or fewer arguments should use
	 * the {@link Descriptor.SmallMethod} class.
	 **/
	public final static class BigMethod extends Method {
	    /**
	     * Array of the typenames of this descriptor's method's arguments.
	     * This array is non-null.
	     */
	    private final TypeName[] arguments_;

	    /**
	     * Create a new method descriptor for a method with more than
	     * three arguments.	 
	     * @param argument0 typename of the first method argument for this
	     *             descriptor (should not be null).
	     * @param argument1 typename of the second method argument for this
	     *             descriptor (should not be null).
	     * @param argument2 typename of the third method argument for the 
	     *             this descriptor (should not be null).
	     * @param rest the 4th - <code>n</code>th method arguments for this
	     *             descriptor (should contain at least one element).
	     * @param retType the typename of the method return type for this
	     *                descriptor
	     **/
	    private BigMethod(TypeName argument0,
			      TypeName argument1,
			      TypeName argument2,
			      TypeName[] rest,
			      TypeName retType) {
		super(argument0, argument1, argument2, retType);
		assert(rest[0] != null);
		arguments_ = rest;

	    }

	    protected Internable copy() {
		return new BigMethod(argument0, argument1, argument2,
				     (TypeName[]) arguments_.clone(),
				     return_or_field_type_);
	    }
	    
	    /**
	     * Compare an object with this method descriptor for equality. 
	     * They are considered equal if they are both <code>BigMethod</code>
	     * descriptors and have the same return and argument types.
	     *
	     * @param o the object to compare against this descriptor
	     *          for equality
	     * @return true if the descriptors are considered equal,
	     *         else false.
	     **/
	    public boolean equals(Object o) {
		if (o == this) {
		    return true;
		} else if (o instanceof BigMethod) {
		    return ((BigMethod) o).equalsFrom(this);
		}
		return false;
	    }

	    /**
	     * Compare this <code>BigMethod</code> descriptor with another 
	     * <code>BigMethod</code> descriptor and see if they have the
	     * same return and argument types.
	     * @param d the method descriptor to compare against
	     **/
	    public boolean equalsFrom(BigMethod d) {
		if (!(return_or_field_type_ == d.return_or_field_type_
		      && argument0 == d.argument0
		      && argument1 == d.argument1
		      && argument2 == d.argument2
		      && arguments_.length == d.arguments_.length))
		    return false;

		for (int i = 0; i < arguments_.length; i++) {
		    if (arguments_[i] != d.arguments_[i])
			return false;
		}
		return true;
	    }

	    public int getArgumentCount() {
		return arguments_.length + 3;
	    }

	    // Return the length in byte of the method argument vector. A plain
	    // field is 4 bytes long, while wide fields are 8 bytes.
	    public int getArgumentLength() {
		int len = super.getArgumentLength();
		for (int i = 0; i < arguments_.length; i++)
		    len += (arguments_[i].isWidePrimitive()) ? 8 : 4;
		return len;
	    }

	    // Returns the typename of the nth argument of this method.
	    public TypeName getArgumentType(int n) {
		if (n < 3) {
		    return super.getArgumentType(n);
		} else {
		    return arguments_[n - 3];
		}
	    }

	    public int hashCode() {
		if (precomputedHashCode_ == -1) {
		    precomputedHashCode_ = super.hashCode();
		    for (int i = 0; i < arguments_.length; i++) {
			assert arguments_[i] != null:
			    "i = " + i + " length " + arguments_.length;
			precomputedHashCode_ += arguments_[i].hashCode();
		    }
		}
		return precomputedHashCode_;
	    }

	    // write the remaining arguments (> 3) to an output stream.
	    protected final void writeRest(OutputStream out) throws IOException {
		for (int i = 0; i < arguments_.length; i++)
		    arguments_[i].write(out);
	    }

	} // End of Descriptor.BigMethod


} // End of Descriptor
