package s3.services.bytecode.verifier;

import ovm.core.repository.JavaNames;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.TypeName;
import ovm.services.bytecode.analysis.AbstractValue;
import ovm.services.bytecode.analysis.AbstractValueError;
import ovm.services.bytecode.analysis.ExtensibleValueFactory;
import ovm.services.bytecode.verifier.VerificationError;

/**
 * This factory generates abstract values suitable, e.g., for bytecode verification.
 * The values distinguish between object types, but not between  boolean, short, 
 * int, char or byte.<p>
 * All object types are disjoint and never merged. Type hierarchy information is not 
 * required, except for the "includes" managed by the <tt>Context</tt>.<p>
 *
 * @author Christian Grothoff
 **/
public class VerificationValueFactory
    extends ExtensibleValueFactory 
    implements AbstractValue.Factory {

    protected static final String REFERENCE = "L";
    protected static final String ARRAY = "[";
    protected static final String VALUENULL = "N";
    protected static final String INT = "I";
    protected static final String FLOAT = "F";
    protected static final String LONG = "J";
    protected static final String DOUBLE = "D";
    protected static final String JUMPTARGET = "T";
    protected static final String INVALID = "V";

    /**
     * Hashtable with all the AbstractValues for (Object) types.
     **/
    private final HTTypeName2AbstractValue_Reference ht_;

    /**
     * Hashtable with all the AbstractValues for Array types.
     **/
    private final HTAbstractValue2AbstractValue_Array htArray_;

    /** Singleton for the primitive type  **/
    private final AbstractValue.Int CHAR_;

    /** Singleton for the primitive type  **/
    private final AbstractValue.Int SHORT_;

    /** Singleton for the primitive type  **/
    private final AbstractValue.Int BOOL_;

    /**
     * Singleton for the primitive type 
     **/
    private final AbstractValue.Int BYTE_;

    /**
     * Singleton for the primitive type 
     **/
    private final AbstractValue.Int INT_;

    /**
     * Singleton for the primitive type 
     **/
    private final AbstractValue.Float FLOAT_;

    /**
     * Singleton for the primitive type 
     **/
    private final AbstractValue.Double DOUBLE_;

    /**
     * Singleton for the primitive type 
     **/
    private final AbstractValue.Long LONG_;

    /**
     * Singleton for the primitive type 
     **/
    private final AbstractValue.Invalid INVALID_;

    /**
     * Singleton for NULL.
     */
    private final AbstractValue.Null NULL_;

    private final int REFERENCE_ID;
    private final int ARRAY_ID;
    private final int JUMPTARGET_ID;

    /** Create a new VerificationValueFactory, initializing
     * singletons/datastructures.
     **/
    public VerificationValueFactory() {
        // register AbstractValue types
        this.REFERENCE_ID = registerAbstractValue(new ReferenceRegistry());
        this.ARRAY_ID = registerAbstractValue(new ArrayRegistry());
        this.JUMPTARGET_ID = registerAbstractValue(new JumpTargetRegistry());
        // intitialize Singletons / HashTables
        this.ht_ = new HTTypeName2AbstractValue_Reference();
        this.htArray_ = new HTAbstractValue2AbstractValue_Array();
	this.SHORT_ = 
	    this.CHAR_ =
	    this.BYTE_ =
	    this.BOOL_ =
	    this.INT_ = new AbstractValueInt(this, registerAbstractValue(new IntRegistry()));
        this.FLOAT_ = new AbstractValueFloat(this, registerAbstractValue(new FloatRegistry()));
        this.LONG_ = new AbstractValueLong(this, registerAbstractValue(new LongRegistry()));
        this.DOUBLE_ = new AbstractValueDouble(this, registerAbstractValue(new DoubleRegistry()));
        this.INVALID_ = new AbstractValueInvalid(this, registerAbstractValue(new InvalidRegistry()));
        this.NULL_ = new AbstractValueNull(this, registerAbstractValue(new ValueNullRegistry()));
    }

    /** 
     * Create an abstract value representing a primitive Bool
     **/
    public AbstractValue.Int makePrimitiveBool() {
        return BOOL_;
    }

    /** 
     * Create an abstract value representing a primitive Short
     **/
    public AbstractValue.Int makePrimitiveShort() {
        return SHORT_;
    }

    /** 
     * Create an abstract value representing a primitive Byte
     **/
    public AbstractValue.Int makePrimitiveByte() {
        return BYTE_;
    }
    /** 
     * Create an abstract value representing a primitive  Int
     **/
    public AbstractValue.Int makePrimitiveChar() {
        return CHAR_;
    }
    /** 
     * Create an abstract value representing a primitive  Int
     **/
    public AbstractValue.Int makePrimitiveInt() {
        return INT_;
    }
    /** 
     * Create an abstract value representing a primitive  Float
     **/
    public AbstractValue.Float makePrimitiveFloat() {
        return FLOAT_;
    }
    /** 
     * Create an abstract value representing a primitive  Double
     **/
    public AbstractValue.Double makePrimitiveDouble() {
        return DOUBLE_;
    }
    /** 
     * Create an abstract value representing a primitive  Long
     **/
    public AbstractValue.Long makePrimitiveLong() {
        return LONG_;
    }
    /** 
     * Create an abstract value representing an object reference 
     * of type typeName.
     * @param  typeName  the object's type
     **/
    public AbstractValue.Reference makeReference(TypeName.Compound typeName) {
        AbstractValue.Reference ref = ht_.get(typeName);
        if (ref != null)
            return ref;
        ref = new AbstractValueReference(this, typeName);
        ht_.put(typeName, ref);
        return ref;
    }
    /** 
     * Create an abstract value representing an array 
     * whose component is of type componentType.
     * @param componentType the abstract value representing the type of the
     * array
     **/
    public AbstractValue.Array makeArray(AbstractValue componentType) {
        AbstractValue.Array ref = htArray_.get(componentType);
        if (ref != null)
            return ref;
        ref = new AbstractValueArray(this, componentType);
        htArray_.put(componentType, ref);
        return ref;
    }
    /** 
     * Create an abstract value representing an uninitialized
     * reference whose type is given by typeName.
     * @param  typeName  the uninitialized object's type
     **/
    public AbstractValue.Reference makeUninitialized(
        TypeName.Compound typeName) {
        return new AbstractValueReference(this, typeName, false);
    }
    /** 
     * Create an abstract value representing a "null" reference.
     **/
    public AbstractValue makeNull() {
        return NULL_;
    }
    /** 
     * Create an abstract value representing an invalid value    .
     **/
    public AbstractValue.Invalid makeInvalid() {
        return INVALID_;
    }
    /** 
     * Create an abstract value representing a jump target.
     * @param target the pc where ret will jump to
     **/
    public AbstractValue.JumpTarget makeJumpTarget(int target) {
        return new AbstractValueJumpTarget(this, JUMPTARGET_ID, target);
    }
    /**
     * Create an abstract value from a TypeName.
     **/
    public AbstractValue typeName2AbstractValue(TypeName type) {
        switch (type.getTypeTag()) {
            case TypeCodes.FLOAT :
                return FLOAT_;
            case TypeCodes.BOOLEAN :
                return BOOL_;
            case TypeCodes.CHAR :
                return CHAR_;
            case TypeCodes.SHORT :
                return SHORT_;
            case TypeCodes.BYTE :
                return BYTE_;
            case TypeCodes.INT :
                return INT_;
            case TypeCodes.LONG :
                return LONG_;
            case TypeCodes.DOUBLE :
                return DOUBLE_;
            case TypeCodes.VOID :
                return INVALID_;
            case TypeCodes.OBJECT :
            case TypeCodes.GEMEINSAM :
                return makeReference((TypeName.Compound) type);
            case TypeCodes.RECORD : // science fiction
                throw new VerificationError("science fiction not supported");
            case TypeCodes.ENUM : // science fiction
                throw new VerificationError("science fiction not supported");
            case TypeCodes.ARRAY :
                TypeName.Array atype = (TypeName.Array) type;
                AbstractValue tmp =
                    typeName2AbstractValue(
                        atype.getInnermostComponentTypeName());
                for (int i = 0; i < atype.getDepth(); i++)
                    tmp = makeArray(tmp);
                return tmp;
            default :
                throw new VerificationError(
                    "tag " + type.getTypeTag() + " not supported");
        }
    }

     static public class IntRegistry extends Registry {
        /**
         * Obtain a textual description of the registered type (free-form,
         * but must be recognized by all other types that are registered
         * **later** for interoperabiltiy)
         * @return "I"
         **/
        public String describe() {
            return INT;
        }

    } // end of IntRegistry

   static public class FloatRegistry extends Registry {
        /**Obtain a textual description of the registered type (free-form,
         * but must be recognized by all other types that are registered
         * **later** for interoperabiltiy)
         * @return "F"
         **/
        public String describe() {
            return FLOAT;
        }
    } // end of FloatRegistry

    static public class DoubleRegistry extends Registry {
        /** Obtain a textual description of the registered type (free-form,
         * but must be recognized by all other types that are registered
         * **later** for interoperabiltiy)
         * @return "D"
         **/
        public String describe() {
            return DOUBLE;
        }
    } // end of DoubleRegistry

    static class LongRegistry extends Registry {
        /**
         * Obtain a textual description of the registered type (free-form,
         * but must be recognized by all other types that are registered
         * **later** for interoperabiltiy)
         * @return "J"
         **/
        public String describe() {
            return LONG;
        }
    } // end of LongRegistry

    public static class ReferenceRegistry extends Registry {
        /**
         * Obtain a textual description of the registered
         * type (free-form, but must be recognized by all
         * other types that are registered **later** for
         * interoperabiltiy)
         * @return "L" for References
         **/
        public String describe() {
            return REFERENCE;
        }
        /**
         * Obtain the Includes object for the table at
         * the intersection of these two objects. This
         * default implementation returns true if the
         * descriptions are identical, otherwise false.
         * @param other the Registry object of the other type
         * @return the includes relationship for (this,other).
         **/
        public IncludesEntry getIncludes(Registry other) {
            String od = other.describe();
            if (od == VALUENULL)
                return new TrueIncludesEntry();
            else if (od == REFERENCE)
                return new ReferenceIncludesEntry();
            else
                return new FalseIncludesEntry();
        }
       /**
         * Obtain the Includes object for the table at
         * the intersection of these two objects.
         * @param other the Registry object of the other type
         * @return the includes relationship for (other,this).
         **/
        public IncludesEntry getIncluded(Registry other) {
            String od = other.describe();
            if (od == REFERENCE)
                return new ReferenceIncludesEntry();
            else if (od == ARRAY)
                return new ArrayIncludesEntry();
            else
                return new FalseIncludesEntry();
        }
        /**
         * Obtain the Merge object for the table at the intersection of these two 
         * objects. The default implementation returns "null" (not  mergable) if 
         * the descriptions are different, otherwise the identity-merge (assuming 
         * that  equal descriptions result in equal AbstractValues).
         * @param other the Registry object of the other type
         * @return the merge computation (must be symmetric)
         **/
        public MergeEntry getMerge(Registry other) {
            String od = other.describe();
            if (od == VALUENULL)
                return new IdentityMergeEntry();
            if (describe().equals(od)) {
                return new ReferenceMergeEntry();
            } else
                return new NullMergeEntry();
        }

        /**
         * Obtain the Merge object for the table at
         * the intersection of these two objects.
         * The default implementation returns "null" (not
         * mergable) if the descriptions are different,
         * otherwise the identity-merge (assuming that 
         * equal descriptions result in equal AbstractValues).
         * @param other the Registry object of the other type
         * @return the merge computation (must be symmetric)
         **/
        public MergeEntry getMerged(Registry other) {
            String od = other.describe();
            if (od == VALUENULL)
                return new OtherMergeEntry();
            if (describe().equals(od)) {
                return new ReferenceMergeEntry();
            } else
                return new NullMergeEntry();
        }

        /**
         * Obtain the equals object for the table at
         * the intersection of these two objects.
         * The default implementation returns True if
         * the Registries are equals, otherwise false.
         * @param other the Registry object of the other type
         * @return the equals relationship
         **/
        public EqualsEntry getEquals(Registry other) {
            if (describe().equals(other.describe()))
                return new ReferenceEqualsEntry();
            else
                return new FalseEqualsEntry();
        }

        public class ReferenceEqualsEntry extends EqualsEntry {
            public boolean equals(AbstractValue a, AbstractValue b) {
                AbstractValue.Reference ar = a.getReference();
                AbstractValue.Reference br = b.getReference();
                return ar.getCompoundTypeName().equals(
                    br.getCompoundTypeName());
            }
        } // end of ReferenceEqualsEntry
        public class ReferenceMergeEntry extends MergeEntry {

            public AbstractValue merge(AbstractValue a, AbstractValue b) {
                AbstractValue.Reference ar = a.getReference();
                AbstractValue.Reference br = b.getReference();
                // we may do some non-asserting query to
                // the context to catch the case where we
                // do know the hierarchy relationship.
                // But that's an optimization...
                if (ar.getCompoundTypeName().equals(br.getCompoundTypeName()))
                    return a;
                else
                    return null;
            }
        } // end of ReferenceMergeEntry
        public class ReferenceIncludesEntry extends IncludesEntry {

            public boolean includes(AbstractValue a, AbstractValue b) {
                AbstractValue.Reference ar = a.getReference();
                AbstractValue.Reference br = b.getReference();
                // we may do some non-asserting query to the context to
                // catch the case where we do know the hierarchy
                // relationship.  But that's an optimization...
                return (ar.getCompoundTypeName() == br.getCompoundTypeName());
            }
        } // end of ReferenceIncludesEntry
        public class ArrayIncludesEntry extends IncludesEntry {
            public boolean includes(AbstractValue a, AbstractValue b) {
                AbstractValue.Reference ar = a.getReference();
                AbstractValue.Reference br = b.getReference();
		return ( (ar.getCompoundTypeName() == JavaNames.java_lang_Object) ||
			 (ar.getCompoundTypeName() == br.getCompoundTypeName()) );
            }
        } // end of ArrayIncludesEntry
    } // end of ReferenceRegistry

    /**
     * Registry-object for Arrays
     **/
    public static class ArrayRegistry extends Registry {
        /**
         * Obtain a textual description of the registered
         * type (free-form, but must be recognized by all
         * other types that are registered **later** for
         * interoperabiltiy)
         * @return "[" for Arrays
         **/
        public String describe() {
            return ARRAY;
        }
        /**
         * Obtain the Includes object for the table at
         * the intersection of these two objects. This
         * default implementation returns true if the
         * descriptions are identical, otherwise false.
         * @param other the Registry object of the other type
         * @return the includes relationship for (this,other).
         **/
        public IncludesEntry getIncludes(Registry other) {
            String od = other.describe();
            if (od == ARRAY)
                return new ArrayIncludesEntry();
            else if (od == REFERENCE)
                return new ReferenceIncludesEntry();
            else if (od == VALUENULL)
                return new TrueIncludesEntry();
            else
                return new FalseIncludesEntry();
        }
        /**
         * Obtain the Includes object for the table at
         * the intersection of these two objects.
         * @param other the Registry object of the other type
         * @return the includes relationship for (other,this).
         **/
        public IncludesEntry getIncluded(Registry other) {
            if (describe().equals(other.describe()))
                return new ArrayIncludesEntry();
            else
                return new FalseIncludesEntry();
        }
        /**
         * Obtain the Merge object for the table at the intersection of
         * these two objects.  The default implementation returns "null"
         * (not mergable) if the descriptions are different, otherwise the
         * identity-merge (assuming that equal descriptions result in equal
         * AbstractValues).
         * @param other the Registry object of the other type
         * @return the merge computation (must be symmetric)
         **/
        public MergeEntry getMerge(Registry other) {
            String od = other.describe();
            if (od == VALUENULL)
                return new IdentityMergeEntry();
            else if (describe().equals(od))
                return new ArrayMergeEntry();
            else
                return new NullMergeEntry();
        }
        /**
         * Obtain the Merge object for the table at the intersection of
         * these two objects.  The default implementation returns "null"
         * (not mergable) if the descriptions are different, otherwise the
         * identity-merge (assuming that equal descriptions result in equal
         * AbstractValues).
         * @param other the Registry object of the other type
         * @return the merge computation (must be symmetric)
         **/
        public MergeEntry getMerged(Registry other) {
            String od = other.describe();
            if (od == VALUENULL)
                return new OtherMergeEntry();
            else if (describe().equals(od))
                return new ArrayMergeEntry();
            else
                return new NullMergeEntry();
        }
        /**
         * Obtain the equals object for the table at the intersection of
         * these two objects.  The default implementation returns True if
         * the Registries are equals, otherwise false.
         * @param other the Registry object of the other type
         * @return the equals relationship
         **/
        public EqualsEntry getEquals(Registry other) {
            if (describe().equals(other.describe()))
                return new ArrayEqualsEntry();
            else
                return new FalseEqualsEntry();
        }

        public class ArrayEqualsEntry extends EqualsEntry {
            public boolean equals(AbstractValue a, AbstractValue b) {
                try {
                    AbstractValue.Array ar = a.getReference().getArray();
                    AbstractValue.Array br = b.getReference().getArray();
                    a = ar.getComponentType();
                    b = br.getComponentType();
                    return a.equals(b);
                } catch (AbstractValueError ave) {
                    return false;
                }
            }
        } // end of ArrayEqualsEntry
        public class ArrayMergeEntry extends MergeEntry {

            public AbstractValue merge(AbstractValue a, AbstractValue b) {
                if (a.equals(b))
                    return a;
                else
                    return null;
            }
        } // end of ArrayMergeEntry
        public class ArrayIncludesEntry extends IncludesEntry {

            public boolean includes(AbstractValue a, AbstractValue b) {
                try {
                    AbstractValue.Array ar = a.getReference().getArray();
                    AbstractValue.Array br = b.getReference().getArray();
                    a = ar.getComponentType();
                    b = br.getComponentType();
                    return a.includes(b);
                } catch (AbstractValueError ave) {
                    return false;
                }
            }
        } // end of ArrayIncludesEntry
        public class ReferenceIncludesEntry extends IncludesEntry {

            public boolean includes(AbstractValue a, AbstractValue b) {
                try {
                    AbstractValue.Reference ar = a.getReference();
                    AbstractValue.Reference br = b.getReference();		    
		    return ( (ar.getCompoundTypeName() == br.getCompoundTypeName()) ||
			     (ar.getCompoundTypeName() == JavaNames.java_lang_Object) );
                } catch (AbstractValueError ave) {
                    return false;
                }
            }
        } // end of ReferenceIncludesEntry
    } // end of ArrayRegistry

    public static class InvalidRegistry 
	extends Registry {
        /**
         * Obtain a textual description of the registered
         * type (free-form, but must be recognized by all
         * other types that are registered **later** for
         * interoperabiltiy)
         * @return "V" for Void
         **/
        public String describe() {
            return INVALID;
        }
    } // end of InvalidRegistry

    static class JumpTargetRegistry extends Registry {
        /**
         * Obtain a textual description of the registered type (free-form,
         * but must be recognized by all other types that are registered
         * **later** for interoperabiltiy)
         * @return "T" (for Target)
         **/
        public String describe() {
            return JUMPTARGET;
        }
        /**
         * Obtain the Includes object for the table at the intersection of
         * these two objects. This default implementation returns true if
         * the descriptions are identical, otherwise false.
         * @param other the Registry object of the other type
         * @return the includes relationship for (this,other).
         **/
        public IncludesEntry getIncludes(Registry other) {
            if (describe().equals(other.describe()))
                // both JumpTargets, compare targets!
                return new JumpTargetIncludesEntry();
            else
                return new FalseIncludesEntry();
        }
        /**
         * Obtain the Includes object for the table at
         * the intersection of these two objects.
         * @param other the Registry object of the other type
         * @return the includes relationship for (other,this).
         **/
        public IncludesEntry getIncluded(Registry other) {
            if (describe().equals(other.describe()))
                // both JumpTargets, compare targets!
                return new JumpTargetIncludesEntry();
            else
                return new FalseIncludesEntry();
        }
        /**
         * Obtain the Merge object for the table at the intersection of
         * these two objects.  The default implementation returns "null"
         * (not mergable) if the descriptions are different, otherwise the
         * identity-merge (assuming that equal descriptions result in equal
         * AbstractValues).
         * @param other the Registry object of the other type
         * @return the merge computation (must be symmetric)
         **/
        public MergeEntry getMerge(Registry other) {
            if (describe().equals(other.describe()))
                // both JumpTargets, compare targets!
                return new JumpTargetMergeEntry();
            else
                return new NullMergeEntry();
        }

        /**
         * Obtain the Merge object for the table at the intersection of
         * these two objects.  The default implementation returns "null"
         * (not mergable) if the descriptions are different, otherwise the
         * identity-merge (assuming that equal descriptions result in equal
         * AbstractValues).
         * @param other the Registry object of the other type
         * @return the merge computation (must be symmetric)
         **/
        public MergeEntry getMerged(Registry other) {
            return getMerge(other);
        }

        /**
         * Obtain the equals object for the table at the intersection of
         * these two objects.  The default implementation returns True if
         * the Registries are equals, otherwise false.
         * @param other the Registry object of the other type
         * @return the equals relationship
         **/
        public EqualsEntry getEquals(Registry other) {
            if (describe().equals(other.describe()))
                return new JumpTargetEqualsEntry();
            else
                return new FalseEqualsEntry();
        }

        class JumpTargetIncludesEntry extends IncludesEntry {
            /**
             * Check if this AbstractValue "includes" the other abstract value.<br>
             * JumpTargets must be equal to include each other.
             * @param a the first abstract value	 
             * @param b the second abstract value	 
             * @return true if the targets match
             **/
            public boolean includes(AbstractValue a, AbstractValue b) {
                AbstractValue.JumpTarget at = a.getJumpTarget();
                AbstractValue.JumpTarget bt = b.getJumpTarget();
                return at.getTarget() == bt.getTarget();
            }
        } // end of JumpTargetIncludesEntry

        class JumpTargetMergeEntry extends MergeEntry {
            /**
             * Check if two AbstractValues can be merged.  Compares two
             * JumpTargets for equality.  The targets must match, otherwise
             * a merge is impossible.
             * @param a the first abstract value	 
             * @param b the second abstract value	 
             * @return true if the abstract values are equal
             **/
            public AbstractValue merge(AbstractValue a, AbstractValue b) {
                AbstractValue.JumpTarget at = a.getJumpTarget();
                AbstractValue.JumpTarget bt = b.getJumpTarget();
                if (at.getTarget() == bt.getTarget())
                    return at;
                else
                    return null;
            }
        } // end of JumpTargetEqualsEntry

        class JumpTargetEqualsEntry extends EqualsEntry {
            /**
             * Check if two AbstractValues are equal.
             * Compares two JumpTargets for equality.
             * The targets must match.
             * @param a the first abstract value	 
             * @param b the second abstract value	 
             * @return true if the abstract values are equal
             **/
            public boolean equals(AbstractValue a, AbstractValue b) {
                AbstractValue.JumpTarget at = a.getJumpTarget();
                AbstractValue.JumpTarget bt = b.getJumpTarget();
                return at.getTarget() == bt.getTarget();
            }
        } // end of JumpTargetEqualsEntry

    } // end of JumpTargetRegistry

     static class ValueNullRegistry extends Registry {
        /**
         * Obtain a textual description of the registered type (free-form,
         * but must be recognized by all other types that are registered
         * **later** for interoperabiltiy)
         * @return "N" (for Null)
         **/
        public String describe() {
            return VALUENULL;
        }
        /**
         * Obtain the Includes object for the table at the intersection of
         * these two objects. This default implementation returns true if
         * the descriptions are identical, otherwise false.
         * @param other the Registry object of the other type
         * @return the includes relationship for (this,other).
         **/
        public IncludesEntry getIncludes(Registry other) {
            if (other.describe() == VALUENULL)
                return new TrueIncludesEntry();
            else
                return new FalseIncludesEntry();
        }
        /**
         * Obtain the Includes object for the table at the intersection of
         * these two objects.
         * @param other the Registry object of the other type
         * @return the includes relationship for (other,this).
         **/
        public IncludesEntry getIncluded(Registry other) {
            String od = other.describe();
            if ((od == REFERENCE) || (od == ARRAY) || (od == VALUENULL))
                return new TrueIncludesEntry();
            else
                return new FalseIncludesEntry();
        }
        /**
         * Obtain the Merge object for the table at the intersection of
         * these two objects.  The default implementation returns "null"
         * (not mergable) if the descriptions are different, otherwise the
         * identity-merge (assuming that equal descriptions result in equal
         * AbstractValues).
         * @param other the Registry object of the other type
         * @return the merge computation (must be symmetric)
         **/
        public MergeEntry getMerge(Registry other) {
            String od = other.describe();
            if ((od == VALUENULL) || (od == REFERENCE) || (od == ARRAY))
                return new OtherMergeEntry(); // return "other" on merge
            else
                return new NullMergeEntry(); // can not be merged!
        }
        /**
         * Obtain the Merge object for the table at
         * the intersection of these two objects.
         * The default implementation returns "null" (not
         * mergable) if the descriptions are different,
         * otherwise the identity-merge (assuming that 
         * equal descriptions result in equal AbstractValues).
         * @param other the Registry object of the other type
         * @return the merge computation (must be symmetric)
         **/
        public MergeEntry getMerged(Registry other) {
            String od = other.describe();
            if ((od == VALUENULL) || (od == REFERENCE) || (od == ARRAY))
                return new IdentityMergeEntry(); // return "other" on merge
            else
                return new NullMergeEntry(); // can not be merged!
        }

        /**
         * Obtain the equals object for the table at
         * the intersection of these two objects.
         * The default implementation returns True if
         * the Registries are equals, otherwise false.
         * @param other the Registry object of the other type
         * @return the equals relationship
         **/
        public EqualsEntry getEquals(Registry other) {
            if (describe().equals(other.describe()))
                return new TrueEqualsEntry();
            else
                return new FalseEqualsEntry();
        }

    } // end of ValueNullRegistry

    /* ***************** FINALLY: ABSTRACT VALUES ************ */

    public static abstract class AbstractValuePrimitive
        extends ExtensibleValueFactory.AbstractValueImpl
        implements AbstractValue.Primitive {

	AbstractValuePrimitive(VerificationValueFactory vvf) {
	    super(vvf);
	}

        /**
         * is this abstract value a primitive?
         * @return true if it is a primitive.
         **/
        public boolean isPrimitive() {
            return true;
        }
        /**
         * return a primitive abstract value 
         * if this abstract value is a primitive one.
         * @exception <code>AbstractValueError</code> 
         * if it is not a primitive value, 
         **/
        public AbstractValue.Primitive getPrimitive() {
            return this;
        }
    } // end of AbstractValuePrimitive

    static final class AbstractValueInt
        extends AbstractValuePrimitive
        implements AbstractValue.Int {

	private final int INT_ID;

	AbstractValueInt(VerificationValueFactory vvf,
			 int id) {
	    super(vvf);
	    this.INT_ID = id;
	}

        /**
         * Obtain the unique ID of this AbstractValue type.
         **/
        public int getId() {
            return INT_ID;
        }
        public String toString() {
            return INT;
        }
        /**
         * is this abstract value an integer?
         * @return true if it is an integer.
         **/
        public boolean isInt() {
            return true;
        }
        /**
         * return an integer abstract value 
         * if this abstract value is an integer one.
         * @exception <code>AbstractValueError</code> 
         * if it is not an integer value, 
         **/
        public AbstractValue.Int getInt() {
            return this;
        }
        /**
        * is this abstract value a float?
        * @return true if it is a float.
        **/
        public boolean isFloat() {
            return false;
        }
        /**
         * return a float abstract value 
         * if this abstract value is a float one.
         * @exception <code>AbstractValueError</code> 
         * if it is not a float value, 
         **/
        public AbstractValue.Float getFloat() {
            throw new AbstractValueError();
        }
    } // end of AbstractValueInt

    static final class AbstractValueFloat
        extends AbstractValuePrimitive
        implements AbstractValue.Float {

	private final int FLOAT_ID;
	
	AbstractValueFloat(VerificationValueFactory vvf,
			   int id) {
	    super(vvf);
	    this.FLOAT_ID = id;
	}

        /**
         * Obtain the unique ID of this AbstractValue type.
         **/
        public int getId() {
            return FLOAT_ID;
        }
        public String toString() {
            return FLOAT;
        }
        /**
         * is this abstract value an integer?
         * @return true if it is an integer.
         **/
        public boolean isInt() {
            return false;
        }
        /**
         * return an integer abstract value 
         * if this abstract value is an integer one.
         * @exception <code>AbstractValueError</code> 
         * if it is not an integer value, 
         **/
        public AbstractValue.Int getInt() {
            throw new AbstractValueError();
        }
        /**
        * is this abstract value a float?
        * @return true if it is a float.
        **/
        public boolean isFloat() {
            return true;
        }
        /**
         * return a float abstract value 
         * if this abstract value is a float one.
         * @exception <code>AbstractValueError</code> 
         * if it is not a float value, 
         **/
        public AbstractValue.Float getFloat() {
            return this;
        }
    } // end of AbstractValueFloat

    public static abstract class AbstractValueWidePrimitive
        extends ExtensibleValueFactory.AbstractValueImpl
        implements AbstractValue.WidePrimitive {
  
	AbstractValueWidePrimitive(VerificationValueFactory vvf) {
	    super(vvf);
	}

	/**
         * is this abstract value an instance of a wide primitive?
         * @return true if it is a wide primitive.
         **/
        public boolean isWidePrimitive() {
            return true;
        }

        /**
         * return an instance of a wide primitive abstract value 
         * if this abstract value is an instance of a wide primitive one.
         * @exception <code>AbstractValueError</code> 
         * if it is not an instance of a wide primitive value, 
         **/
        public WidePrimitive getWidePrimitive() {
            return this;
        }
    } // end of AbstractValueWidePrimitive

    static final class AbstractValueLong
        extends AbstractValueWidePrimitive
        implements AbstractValue.Long {

	private final int LONG_ID;

	AbstractValueLong(VerificationValueFactory vvf,
			  int id) {
	    super(vvf);
	    this.LONG_ID = id;
	}

        /**
         * Obtain the unique ID of this AbstractValue type.
         **/
        public int getId() {
            return LONG_ID;
        }
        public String toString() {
            return LONG;
        }
        /**
         * is this abstract value a long?
         * @return true if it is a long.
         **/
        public boolean isLong() {
            return true;
        }
        /**
         * return a long abstract value 
         * if this abstract value is a long one.
         * @exception <code>AbstractValueError</code> 
         * if it is not a long value, 
         **/
        public AbstractValue.Long getLong() {
            return this;
        }
        /**
         * is this abstract value a long?
         * @return true if it is a long.
         **/
        public boolean isDouble() {
            return false;
        }
        /**
         * return a long abstract value 
         * if this abstract value is a long one.
         * @exception <code>AbstractValueError</code> 
         * if it is not a long value, 
         **/
        public AbstractValue.Double getDouble() {
            throw new AbstractValueError();
        }
    } // end of AbstractValueLong

    static final class AbstractValueDouble
        extends AbstractValueWidePrimitive
        implements AbstractValue.Double {
 	private final int DOUBLE_ID;

	AbstractValueDouble(VerificationValueFactory vvf,
			    int id) {
	    super(vvf);
	    this.DOUBLE_ID = id;
	}

       /**
         * Obtain the unique ID of this AbstractValue type.
         **/
        public int getId() {
            return DOUBLE_ID;
        }
        public String toString() {
            return DOUBLE;
        }
        /**
         * is this abstract value a long?
         * @return true if it is a long.
         **/
        public boolean isLong() {
            return false;
        }
        /**
         * return a long abstract value 
         * if this abstract value is a long one.
         * @exception <code>AbstractValueError</code> 
         * if it is not a long value, 
         **/
        public AbstractValue.Long getLong() {
            throw new AbstractValueError();
        }
        /**
         * is this abstract value a long?
         * @return true if it is a long.
         **/
        public boolean isDouble() {
            return true;
        }
        /**
         * return a long abstract value 
         * if this abstract value is a long one.
         * @exception <code>AbstractValueError</code> 
         * if it is not a long value, 
         **/
        public AbstractValue.Double getDouble() {
            return this;
        }
    } // end of AbstractValueDouble

    static final class AbstractValueInvalid
        extends ExtensibleValueFactory.AbstractValueImpl
        implements AbstractValue.Invalid {

	private final int INVALID_ID;

	AbstractValueInvalid(VerificationValueFactory vvf,
			     int id) {
	    super(vvf);
	    this.INVALID_ID = id;
	}

        /**
         * Obtain the unique ID of this AbstractValue type.
         **/
        public int getId() {
            return INVALID_ID;
        }
        public String toString() {
            return INVALID;
        }
        /**
         * is this abstract value an invalid location?
         * @return true if it is an invalid location.
         **/
        public boolean isInvalid() {
            return true;
        }
        /**
         * return this invalid location.
         **/
        public AbstractValue.Invalid getInvalid() {
            return this;
        }
    } // end of AbstractValueInvalid

    public static class AbstractValueReference
        extends ExtensibleValueFactory.AbstractValueImpl
        implements AbstractValue.Reference {
	private final int REFERENCE_ID_;
        /**
         * Obtain the unique ID of this AbstractValue type.
         **/
        public int getId() {
            return REFERENCE_ID_;
        }
        private final TypeName.Compound tn_;
        private boolean initialized_;
        public String toString() {
            return tn_.toString();
        }
        /**
         * Create the abstract value of an INITIALIZED
         * Reference.
         **/
        public AbstractValueReference(VerificationValueFactory vvf,
				      TypeName.Compound tn) {
	    super(vvf);
	    this.REFERENCE_ID_ = vvf.REFERENCE_ID;
            this.tn_ = tn;
            this.initialized_ = true;

            if (this.getClass() == AbstractValueReference.class)
                if (this.toString().charAt(0) == '[')
                    throw new Error("Bad Christian, no cookie");

        }
        AbstractValueReference(VerificationValueFactory vvf,
			       TypeName.Compound tn,
			       boolean init) {
	    super(vvf);
	    this.REFERENCE_ID_ = vvf.REFERENCE_ID;
            this.tn_ = tn;
            this.initialized_ = init;
        }
        /**
         * is this abstract value a reference?
         * @return true if it is a reference.
         **/
        public boolean isReference() {
            return true;
        }
        /**
         * return a reference abstract value 
         * if this abstract value is a reference one.
         * @exception <code>AbstractValueError</code> 
         * if it is not a reference value, 
         **/
        public AbstractValue.Reference getReference() {
            return this;
        }
        /**
        * Get the typename of this Object.
        **/
        public TypeName.Compound getCompoundTypeName() {
            return tn_;
        }
        /**
         * Is this Reference an array?
         **/
        public boolean isArray() {
            return false;
        }
        /**
         * @return the array value if this is an array value, 
         * otherwise throws an exception.
         **/
        public AbstractValue.Array getArray() {
            throw new AbstractValueError();
        }
        /**
         * Has this object been initialized (constructor called?)
         * @return true if the constructor has been called
         *  (or if this is NULL or obtained from arguments or fields)
         **/
        public boolean isInitialized() {
            return initialized_;
        }
        /**
         * Mark this object as initialized
         * @exception <code>AbstractValueError</code> 
         * if the reference is already initialized
         **/
        public void initialize() {
            if (initialized_)
                throw new AbstractValueError();
            else
                initialized_ = true;
        }
        /**
         * is this abstract value a null reference?
         * @return true if it is a null reference.
         **/
        public boolean isNull() {
            return false;
        }
        /**
         * return a null abstract value 
         * if this abstract value is a null one.
         * @exception <code>AbstractValueError</code> 
         * if it is not a null value, 
         **/
        public AbstractValue.Null getNull() {
            throw new AbstractValueError();
        }

    } // end of AbstractValueReference

    public static class AbstractValueArray
        extends AbstractValueReference
        implements AbstractValue.Array {
	private final int ARRAY_ID_;
        /**
         * Obtain the unique ID of this AbstractValue type.
         **/
        public int getId() {
            return ARRAY_ID_;
        }

        private final AbstractValue componentType_;
        public String toString() {
            return ARRAY + componentType_.toString();
        }
        AbstractValueArray(VerificationValueFactory vvf,
			   AbstractValue ct) {
            super(vvf, null);
	    this.ARRAY_ID_ = vvf.ARRAY_ID;
            this.componentType_ = ct;
        }
        public AbstractValue.Array getArray() {
            return this;
        }
        /**
	 * FIXME: why does int[] return null?
         * Get the typename of this Object.
         **/
        public TypeName.Compound getCompoundTypeName() {
            if (componentType_.isReference()) {
                TypeName.Compound comp =
                    componentType_.getReference().getCompoundTypeName();
                if (comp == null)
                    return null;
                return TypeName.Array.make(comp, 1);
            } else
                return null;
        }
        /**
         * Is this Reference an array?
         **/
        public boolean isArray() {
            return true;
        }
        /**
         * @return the abstract value representing the type of the components
         * stored in the array.
         **/
        public AbstractValue getComponentType() {
            return componentType_;
        }
    } // end of AbstractValueArray

    static final class AbstractValueNull
        extends AbstractValueReference
        implements AbstractValue.Null {
 	private final int NULL_ID;

	AbstractValueNull(VerificationValueFactory vvf,
			  int id) {
	    super(vvf, null);
	    this.NULL_ID = id;
	}
       /**
         * Obtain the unique ID of this AbstractValue type.
         **/
        public int getId() {
            return NULL_ID;
        }
        public String toString() {
            return VALUENULL;
        }
        /**
         * is this abstract value a null reference?
         * @return true if it is a null reference.
         **/
        public boolean isNull() {
            return true;
        }
        /**
         * return a null abstract value 
         * if this abstract value is a null one.
         * @exception <code>AbstractValueError</code> 
         * if it is not a null value, 
         **/
        public AbstractValue.Null getNull() {
            return this;
        }
    } // end of AbstractValueNull

    static final class AbstractValueJumpTarget
        extends ExtensibleValueFactory.AbstractValueImpl
        implements AbstractValue.JumpTarget {
 	private final int JUMPTARGET_ID_;

	/**
         * Obtain the unique ID of this AbstractValue type.
         **/
        public int getId() {
            return JUMPTARGET_ID_;
        }
        private final int target_;

        AbstractValueJumpTarget(VerificationValueFactory vvf,
				int id,
				int target) {
            super(vvf);
	    this.JUMPTARGET_ID_ = id;
            this.target_ = target;
        }
        public String toString() {
            return JUMPTARGET + "(" + target_ + ")";
        }

        /**
         * is this abstract value representing a jump target?
         * @return true if it is a jump target.
         **/
        public boolean isJumpTarget() {
            return true;
        }

        /**
         * return a jump target abstract value 
         * if this abstract value is a jump target one.
         * @exception <code>AbstractValueError</code> 
         * if it is not a jump target value, 
         **/
        public AbstractValue.JumpTarget getJumpTarget() {
            return this;
        }

        /**
         * @return the return address
         **/
        public int getTarget() {
            return target_;
        }
    } // end of AbstractValueJumpTarget

} // end of VerificationValueFactory
