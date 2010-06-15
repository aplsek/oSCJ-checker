// $Header: /p/sss/cvs/OpenVM/src/ovm/services/bytecode/analysis/AbstractValue.java,v 1.5 2007/06/03 01:25:48 baker29 Exp $
package ovm.services.bytecode.analysis;

import ovm.core.repository.TypeName;

/**
 * Every object on the stack or in the local variables must extend this
 * class.
 * @author Vitek, Grothoff, Razafimahefa, Pawlak
 **/
public interface AbstractValue  {    

    /** Get an identification number.  **/
    public int getId();

    /**is this abstract value representing a jump target?
     * @return true if it is a jump target.
     **/
    boolean isJumpTarget();

    /**return a jump target abstract value if this abstract value is a jump
     * target one.
     * @exception <code>AbstractValueError</code> if it is not a jump
     * target value,
     **/
    AbstractValue.JumpTarget getJumpTarget();

    /**is this abstract value an instance of a wide primitive?
     * @return true if it is a wide primitive.
     **/
    boolean isWidePrimitive();

    /**return an instance of a wide primitive abstract value if this
     * abstract value is an instance of a wide primitive one.
     * @exception <code>AbstractValueError</code> if it is not an instance
     * of a wide primitive value,
     **/
    WidePrimitive getWidePrimitive();

    /**is this abstract value a primitive?
     * @return true if it is a primitive.
     **/
    boolean isPrimitive();

    /**return a primitive abstract value if this abstract value is a
     * primitive one.
     * @exception <code>AbstractValueError</code> if it is not a primitive
     * value,
     **/
    AbstractValue.Primitive getPrimitive();

    /**is this abstract value a reference?
     * @return true if it is a reference.
     **/
    boolean isReference();

    /**
     * return a reference abstract value 
     * if this abstract value is a reference one.
     * @exception <code>AbstractValueError</code> 
     * if it is not a reference value, 
     **/
    AbstractValue.Reference getReference();

    /**
     * is this abstract value an invalid stack location?
     * @return true if it is an invalid stack location.
     **/
    boolean isInvalid();

    /**return an invalid abstract value if this abstract value is an
     * invalid one.
     * @exception <code>AbstractValueError</code> 
     * if it is not an invalid value, 
     **/
    AbstractValue.Invalid getInvalid();
 
    /**
     * check if two AbstractValues are equal
     * @param v the other abstract value

     * @return true if the abstract values are equal
     **/
    public boolean equals(AbstractValue v);

    /**
     * check if this AbstractValue "includes" the other abstract value.<br>
     * Includes is defined in terms of being more critical. E.g. a
     * reference is more critical then a null pointer, thus
     * Reference.includes(NullPointer).
     * @param v the other abstract value
     * @return true if the other element is less critical, and thus the
     * analysis of v is included in the analysis of this.  
     **/
    public boolean includes (AbstractValue v);
    
    /**
     * combine this AbstractValue with another AbstractValue.  Returns the
     * combination (most critical common SE).  Uses double dispatch with
     * "mergedFrom".
     * @return null for errors, otherwise the merged
     *          AbstractValue
     **/
    public AbstractValue merge (AbstractValue e);


    /**
     * represent an abstraction of primitive types residing on the stack or
     * in local variables.
     **/
    interface Primitive extends AbstractValue {

	/**
	 * is this abstract value an integer?
	 * @return true if it is an integer.
	 **/
	boolean isInt();
	/**
	 * return an integer abstract value 
	 * if this abstract value is an integer one.
	 * @exception <code>AbstractValueError</code> 
	 * if it is not an integer value, 
	 **/
	AbstractValue.Int getInt();
	/**
	 * is this abstract value a float?
	 * @return true if it is a float.
	 **/
	boolean isFloat();
	/**
	 * return a float abstract value 
	 * if this abstract value is a float one.
	 * @exception <code>AbstractValueError</code> 
	 * if it is not a float value, 
	 **/
	AbstractValue.Float getFloat();

    } // end of Primitive
    
    /**
     * represent an abstraction of integer variable residing on the stack or
     * in local variables.
     **/
    interface Int extends Primitive { }
    
    /**
     * represent an abstraction of float variable residing on the stack or
     * in local variables.
     **/
    interface Float extends Primitive { }
    
    /**
     * represent an abstraction of wide primitive types (long, double) 
     * residing on the stack or in local variables.
     **/
    interface WidePrimitive extends AbstractValue {
	/**
	 * is this abstract value a long?
	 * @return true if it is a long.
	 **/
	boolean isLong();
	/**
	 * return a long abstract value 
	 * if this abstract value is a long one.
	 * @exception <code>AbstractValueError</code> 
	 * if it is not a long value, 
	 **/
	AbstractValue.Long getLong();
	/**
	 * is this abstract value a double?
	 * @return true if it is a double.
	 **/
	boolean isDouble();
	/**
	 * return a double abstract value 
	 * if this abstract value is a double one.
	 * @exception <code>AbstractValueError</code> 
	 * if it is not a double value, 
	 **/
	AbstractValue.Double getDouble();
    }
    
    /**
     * represent an abstraction of long variable residing on the stack or
     * in local variables.
     **/
    interface Long extends WidePrimitive { }

    /**
     * represent an abstraction of double variable on the stack or
     * in local variables.
     **/
    interface Double extends WidePrimitive { }

    /**
     * represent the return address pushed onto the stack after a jump
     * subroutine instruction.
     **/
    interface JumpTarget extends AbstractValue {
	/**
	 * @return the return address
	 **/
	public int getTarget();
    }

    /**
     * represent a reference. A reference can either be an 
     * array reference, an object reference, a null reference
     * or an unitialized reference. Uninitialized references
     * can not be singletons as we must keep track of all 
     * references to the uninitialized object to set them to
     * initialized once the constructor is called. Objects that
     * are not created in the code but obtained (e.g. arguments,
     * fields) can be shared instances if the types match.
     **/
    interface Reference extends AbstractValue{

	/**
	 * Get the typename of this Object.
	 **/
	TypeName.Compound getCompoundTypeName();

	/**
	 * Is this Reference an array?
	 **/
	boolean isArray();

	/**
	 * @return the array value if this is an array value, 
	 * otherwise throws an exception.
	 **/
	public AbstractValue.Array getArray();

	/**
	 * Has this object been initialized (constructor called?)
	 * @return true if the constructor has been called
	 *  (or if this is NULL or obtained from arguments or fields)
	 **/
	boolean isInitialized();

	/**
	 * Mark this object as initialized
	 * @exception <code>AbstractValueError</code> 
	 * if the reference is already initialized
	 **/
	void initialize();

	/**
	 * is this abstract value a null reference?
	 * @return true if it is a null reference.
	 **/
	boolean isNull();

	/**
	 * return a null abstract value 
	 * if this abstract value is a null one.
	 * @exception <code>AbstractValueError</code> 
	 * if it is not a null value, 
	 **/
	AbstractValue.Null getNull();
    }

    /**
     * represent an abstraction of array references residing on the stack or
     * in local variables.
     **/
    interface Array extends Reference { 
	/**
	 * @return the abstract value representing the type of the components
	 * stored in the array (i.e. array type stripped of one [).
	 * FIXME correct?
	 **/
	public AbstractValue getComponentType();
    }

    /**
     * represent a Null reference residing on the stack or in local variables.
     **/
    interface Null extends Reference {
    }

    /**
     * represent locations on the stack that starts in an invalid state
     * and turns out later on into a valid state.
     **/
    interface Invalid extends AbstractValue { 
	/**
	 * is this abstract value an invalid location?
	 * @return true if it is an invalid location.
	 **/
	boolean isInvalid();

	/**
	 * return this invalid location.
	 **/
	AbstractValue.Invalid getInvalid();

    }

    /**
     * This factory is used to generate implementations of the
     * AbstractValues used for verification.
     **/
    public interface Factory {
	
	/** 
	 * Create an abstract value representing a primitive Bool
	 **/
	public AbstractValue.Int makePrimitiveBool();
	
	/** 
	 * Create an abstract value representing a primitive Byte
	 **/
	public AbstractValue.Int makePrimitiveByte();
	
	/** 
	 * Create an abstract value representing a primitive Short
	 **/
	public AbstractValue.Int makePrimitiveShort();
	
	/** 
	 * Create an abstract value representing a primitive Char
	 **/
	public AbstractValue.Int makePrimitiveChar();
	
	/** 
	 * Create an abstract value representing a primitive  Int
	 **/
	public AbstractValue.Int makePrimitiveInt();

	/** 
	 * Create an abstract value representing a primitive  Float
	 **/
	public AbstractValue.Float makePrimitiveFloat();

	/** 
	 * Create an abstract value representing a primitive  Double
	 **/
	public AbstractValue.Double makePrimitiveDouble();

	/** 
	 * Create an abstract value representing a primitive  Long
	 **/
	public AbstractValue.Long makePrimitiveLong();
	
	/** 
	 * Create an abstract value representing an object reference 
	 * of type typeName.
	 * @param  typeName  the object's type
	 **/
	public AbstractValue.Reference makeReference(TypeName.Compound typeName);
	
	/** 
	 * Create an abstract value representing an array 
	 * whose component is of type componentType.
	 * @param  componentType  the abstract value representing the type of the array  
	 **/
	public AbstractValue.Array makeArray(AbstractValue componentType);
	
	/** 
	 * Create an abstract value representing an uninitialized
	 * reference whose type is given by typeName.
	 * @param  typeName  the uninitialized object's type
	 **/
	public AbstractValue.Reference makeUninitialized(TypeName.Compound typeName);
	
	/** 
	 * Create an abstract value representing a "null" reference.
	 **/
	public AbstractValue makeNull();
	
	/** 
	 * Create an abstract value representing an invalid reference.
	 **/
	public AbstractValue.Invalid makeInvalid();
	
	/** 
	 * Create an abstract value representing a jump target.
	 * @param target the pc where ret will jump to
	 **/
	public AbstractValue.JumpTarget makeJumpTarget(int target);
	
	/**
	 * Create an abstract value from a TypeName.
	 **/
	public AbstractValue typeName2AbstractValue
	    (TypeName type);

    } // end of AbstractValue.Factory


} // End of AbstractValue


