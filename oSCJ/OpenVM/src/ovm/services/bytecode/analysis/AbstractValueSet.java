package ovm.services.bytecode.analysis;

import java.util.HashMap;

/**
 * Set of abstract values.  The common use is something along the lines
 * of:
 *
 * <code>
 *  AbstractValueSet.SetRegistry reg = new AbstractValueSet.SetRegistry(my_evf);
 *  AbstractValueSet set1 = reg.makeSet(someAV1); // { someAV1 } 
 *  AbstractValueSet set2 = reg.makeSet(someAV2); // { someAV2 } 
 *  AbstractValueSet set3 = reg.makeEmptySet();   // { } 
 *  AbstractValueSet set4 = set1.merge(set2);     // { someAV1, someAV2 } 
 *  ASSERT(set4.includes(set3) && 
 *         set4.includes(set2) &&
 *         set4.includes(set1));
 * </code>
 *
 * In other words, the Registry also serves as the factory for the
 * abstract value sets.
 *
 * @see ExtensibleValueFactory 
 * @author Christian Grothoff
 */
public class AbstractValueSet 
    extends ExtensibleValueFactory.AbstractValueImpl {
   	
    private static final boolean DEBUG = false;

    private final SetRegistry reg_;

    private final AbstractValue[] set;

    private AbstractValueSet(SetRegistry reg,
			     AbstractValue[] set) {
	super(reg.getEVF());
	this.reg_ = reg;
	this.set = set;	
	if (DEBUG) {
	    for (int i=0;i<set.length;i++) {
		if (set[i] == null)
		    throw new Error("Entry in set is null!");
		for (int j=i+1;j<set.length;j++) 
		    if (set[i] == set[j])
			throw new Error("Duplicate entries in set: " + this);
	    }
	}
    }
       
    public AbstractValue[] getSet(){ 
	return set; 
    }

    public boolean contains(AbstractValue av) {
	if (av == null)
	    return true; /* I believe this is correct... -- CG */
	for (int i=set.length-1;i>=0;i--)
	    if (set[i].equals(av))
		return true;
	return false;
    }

    /**
     * Obtain the unique ID of this AbstractValue type.
     **/
    public int getId() {
	return reg_.getId();
    }
    
    /**
     * Is this abstract value representing a jump target?
     * @return true if it is a jump target.
     **/
    public boolean isJumpTarget() {
	return false;
    }
    
    /**
     * return a jump target abstract value 
     * if this abstract value is a jump target one.
     * @exception <code>AbstractValueError</code> 
     * if it is not a jump target value, 
     **/
    public AbstractValue.JumpTarget getJumpTarget() {
	throw new AbstractValueError();
    }
    
    /**
     * is this abstract value an instance of a wide primitive?
     * @return true if it is a wide primitive.
     **/
    public boolean isWidePrimitive() {
	return false;
    }
    
    /**
     * return an instance of a wide primitive abstract value 
     * if this abstract value is an instance of a wide primitive one.
     * @exception <code>AbstractValueError</code> 
     * if it is not an instance of a wide primitive value, 
     **/
    public AbstractValue.WidePrimitive getWidePrimitive() {
	throw new AbstractValueError();
    }
    
    /**
     * is this abstract value a primitive?
     * @return true if it is a primitive.
     **/
    public boolean isPrimitive() {
	return false;
    }
    
    /**
     * return a primitive abstract value 
     * if this abstract value is a primitive one.
     * @exception <code>AbstractValueError</code> 
     * if it is not a primitive value, 
     **/
    public AbstractValue.Primitive getPrimitive() {
	throw new AbstractValueError();
    }
    
    /**
     * is this abstract value a reference?
     * @return true if it is a reference.
     **/
    public boolean isReference() {
	return false;
    }
	
    /**
     * return a reference abstract value 
     * if this abstract value is a reference one.
     * @exception <code>AbstractValueError</code> 
     * if it is not a reference value, 
     **/
    public AbstractValue.Reference getReference() {
	throw new AbstractValueError();
    }
    
    /**
     * is this abstract value an invalid stack location?
     * @return true if it is an invalid stack location.
     **/
    public boolean isInvalid() {
	return false;
    }
    
    /**
     * return an invalid abstract value 
     * if this abstract value is an invalid one.
     * @exception <code>AbstractValueError</code> 
     * if it is not an invalid value, 
     **/
    public AbstractValue.Invalid getInvalid() {
	throw new AbstractValueError();
    }
    
    public boolean equals(Object o) {
	if (o == this)
	    return true;
	if (o == null)
	    return false;
	try {
	    return ((AbstractValue)this).equals((AbstractValue)o);
	} catch (ClassCastException cce) {
	    return false;
	}
    }

    /**
     * We know the types, avoid some casts, etc.
     **/
    public AbstractValueSet merge(AbstractValueSet ts) {
	return (AbstractValueSet) merge((AbstractValue)ts);
    }

    public AbstractValue[] without(AbstractValueSet typeSet){
	int diff = set.length - typeSet.set.length;
	if (diff == 0)
	    return (reg_.makeEmptySet()).set;
	AbstractValue[] ret = new AbstractValue[diff];
	OUTER:
	for (int i=set.length-1;i>=0; i--) {
	    AbstractValue tnc1 = set[i];
	    for (int j=typeSet.set.length-1;j>=0;j--){
		if (tnc1.equals(typeSet.set[j]))
		    continue OUTER;
	    }
	    ret[--diff] = tnc1;	    
	}
	// assert (diff == 0);
	return ret;
    }

    public String toString() {
	String s = "";
	for (int i=0;i<set.length;i++)
	    s = s + " " + i + ": " + set[i];
	return "AV-SET {" + s + "}";
    }

    /**
     * Is this abstract value a null reference?
     * @return true if it is a null reference.
     **/
    public boolean isNull() { 
	return false; 
    }
    
    /**
     * return a null abstract value if this abstract value is a null one.
     * @exception <code>AbstractValueError</code>  if non null.
     **/
    public AbstractValue.Null getNull() { 
	throw new AbstractValueError(); 
    }

    /* (non-Javadoc)
     * @see ovm.core.repository.TypeName#hashCode()
     */
    public int hashCode() {
        return 0;
    }

    /*** Unique string for the this ptr.   **/
    public static final String TYPESET = "SET";

    /**
     * Registry-object for Reference Sets
     **/
    public static class SetRegistry 
	extends ExtensibleValueFactory.Registry {

	
	private final int id_;

	private final ExtensibleValueFactory evf_;

	private final AbstractValueSet EMPTY_SET_SINGLETON_;

	/**
	 * Hashtable that maps an AbstractValue to a set that just contains 
	 * that one AbstractValue.
	 **/
	private final HashMap abstractValue2SetMap = new HashMap();
	

	public SetRegistry(ExtensibleValueFactory evf) {
	    this.evf_ = evf;
	    this.id_ = evf.registerAbstractValue(this);
	    this.EMPTY_SET_SINGLETON_ = new AbstractValueSet(this,
							     new AbstractValue[0]);
	}

	final int getId() {
	    return id_;
	}

	final ExtensibleValueFactory getEVF() {
	    return evf_;
	}

	public AbstractValueSet makeEmptySet() {
	    return EMPTY_SET_SINGLETON_;
	}
 
	/** 
	 * Create an abstract value representing the given set of typenames.
	 *
	 * @param  av the abstract value to put into a set
	 **/
	public AbstractValueSet makeSet(AbstractValue av){
	    if (av instanceof AbstractValueSet) 
		throw new Error("oops: not allowed!"); // should not happen! fix call-site!
	    Object o = abstractValue2SetMap.get(av);
	    if (o != null)
		return (AbstractValueSet)o;
	    AbstractValueSet avs = new AbstractValueSet(this, 
							new AbstractValue[] {av});
	    abstractValue2SetMap.put(av, avs);	
	    return avs;
	}   	

	/** 
	 * Create an abstract value representing the given set of typenames.
	 *
	 * @param av1 the first abstract value to put into a set
	 * @param av2 the second abstract value to put into a set
	 **/
	public AbstractValueSet makeSet(AbstractValue av1,
					AbstractValue av2){
	    if (av1 instanceof AbstractValueSet) 
		throw new Error("oops: not allowed!"); // should not happen! fix call-site!
	    if (av2 instanceof AbstractValueSet) 
		throw new Error("oops: not allowed!"); // should not happen! fix call-site!
	    return new AbstractValueSet(this, 
					new AbstractValue[] {av1, av2});
	}   	

	/**
	 * Create a new AVS that contains one additional value.
	 */
	public AbstractValueSet makeSet(AbstractValueSet as,
					AbstractValue av) {
	    return makeSet(av).merge(as);
	}

	/**
	 * Obtain a textual description of the registered type (free-form,
	 * but must be recognized by all other types that are registered
	 * **later** for interoperabiltiy)
	 * @return "@" for This pointer
	 **/
	public String describe() {   return TYPESET; }

	/**
	 * Obtain the Includes object for the table at the intersection of
	 * these two objects. This default implementation returns true if
	 * the descriptions are identical, otherwise false.
	 * @param other the Registry object of the other type
	 * @return the includes relationship for (this,other).
	 **/
	public ExtensibleValueFactory.IncludesEntry getIncludes(ExtensibleValueFactory.Registry other) {
	    String od = other.describe();
	    if (od == TYPESET) 
		return new SubsetTestEntry(true);
	    else
		return new ExtensibleValueFactory.FalseIncludesEntry();

	}

	/**
	 * Obtain the Includes object for the table at the intersection of
	 * these two objects.
	 * @param other the Registry object of the other type
	 * @return the includes relationship for (other,this).
	 **/
	public ExtensibleValueFactory.IncludesEntry getIncluded(ExtensibleValueFactory.Registry other) {
	    String od = other.describe();
	    if (od == TYPESET) 
		return new SubsetTestEntry(false);
	    else 	    
		return new ExtensibleValueFactory.FalseIncludesEntry();
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
	public ExtensibleValueFactory.MergeEntry getMerge(ExtensibleValueFactory.Registry other) {
	    if (other.describe() == TYPESET) 
		return new UnionSetEntry(this);
	    else
		return new ExtensibleValueFactory.NullMergeEntry();	    
	}

	public ExtensibleValueFactory.MergeEntry getMerged(ExtensibleValueFactory.Registry other) {
	    if (other.describe() == TYPESET) 
		return new UnionSetEntry(this);
	    else
		return new ExtensibleValueFactory.NullMergeEntry();	    
	}

	/**
	 * Obtain the equals object for the table at the intersection of
	 * these two objects.  The default implementation returns True if
	 * the Registries are equals, otherwise false.
	 * @param other the Registry object of the other type
	 * @return the equals relationship
	 **/
	public ExtensibleValueFactory.EqualsEntry getEquals(ExtensibleValueFactory.Registry other) {
	    if (TYPESET == other.describe())
		return new SetEqualityEntry();
	    else 
		return new ExtensibleValueFactory.FalseEqualsEntry();
	}
	
    } // end of SetRegistry

	
    static class SubsetTestEntry 
	extends ExtensibleValueFactory.IncludesEntry {
	
	private final boolean invert;
	
	SubsetTestEntry(boolean inv) {
	    this.invert = inv;
	}
	
	/**
	 * check if this AbstractValue "includes" the other abstract value.<br>
	 * Includes is defined in terms of being more critical. E.g. a
	 * reference is more critical then a null pointer, thus
	 * Reference.includes(NullPointer).
	 * @param a the first abstract value	 
	 * @param b the second abstract value	 
	 * @return true iff a >= b for invert==true or b >= a for invert==false
	 **/
	public boolean includes(AbstractValue a,
				AbstractValue b) {
	    if (a == b)
		return true;
	    AbstractValueSet as;
	    AbstractValueSet bs;
	    if (invert) {
		as = (AbstractValueSet) a;
		bs = (AbstractValueSet) b;
	    } else {
		as = (AbstractValueSet) b;
		bs = (AbstractValueSet) a;
	    }
	    AbstractValue[] a1 = as.set;
	    AbstractValue[] b1 = bs.set;
	    for (int i=0;i<a1.length;i++) {
		boolean found = false;
		for (int j=0;(j<b1.length) && (! found);j++)
		    found = found || (a1[i].equals(b1[j]));
		if (! found)
		    return false;
	    }
	    return true;
	}
	
    } // end of SubsetTestEntry
    
    static class UnionSetEntry 
	extends ExtensibleValueFactory.MergeEntry {
	
	private final SetRegistry registry_;
	
	UnionSetEntry(SetRegistry registry) {
	    this.registry_ = registry;
	}
	
	/**
	 * combine this AbstractValue with another AbstractValue.  Returns
	 * the combination (most critical common SE).
	 * @param a the first abstract value	 
	 * @param b the second abstract value	 
	 * @return null for errors, otherwise the merged  AbstractValue
	 **/
	public AbstractValue merge(AbstractValue a, 
				   AbstractValue b) {
	    if (a == b)
		return a;
	    AbstractValueSet as = (AbstractValueSet) a;
	    AbstractValueSet bs = (AbstractValueSet) b;
	    AbstractValue[] a1 = as.set;
	    AbstractValue[] b1 = bs.set;
	    AbstractValue[] u = union(a1, b1);
	    if (u == a1)
		return as;
	    if (u == b1)
		return bs;
	    return new AbstractValueSet(registry_,
					u);
	}       	
	    
	private AbstractValue[] union(AbstractValue[] s1,
				      AbstractValue[] s2) {
	    if (s1 == null)
		return s2;
	    if (s2 == null)
		return s1;
	    if (s1.length == 0)
		return s2;
	    if (s2.length == 0)
		return s1;	    
	    if ( (s1.length == 1) &&
		 (s2.length == 1) ) {
		if (s1[0].equals(s2[0]))
		    return s1;
		else
		    return new AbstractValue[] {s1[0], s2[0]};
	    }    
	    
	    int grow = s2.length;
	    for (int i=s1.length-1;i>=0;i--)
		for (int j=s2.length-1;j>=0;j--) 
		    if (s1[i].equals(s2[j])) 
			grow--;	       	    
	    if (grow == 0)
		return s1;
	    if (grow < 0) 
		    throw new Error("assertion violated (element twice in set?)");		
	    AbstractValue[] res 
		= new AbstractValue[s1.length + grow];	    
	    System.arraycopy(s1, 0,
			     res, 0,
			     s1.length);
	    int k = s1.length;
	    for (int j=s2.length-1;j>=0;j--){
		boolean present = false;
		for (int i=s1.length-1;i>=0;i--)
		    if (s1[i].equals(s2[j])) {
			present = true;
			break;
		    }
		if (!present)
		    res[k++] = s2[j];
	    }
		return res;
	}
	    
    } // end of UnionSetEntry
    
    static class SetEqualityEntry
	extends ExtensibleValueFactory.EqualsEntry {
	
	/**
	 * check if two AbstractValues are equal
	 * @param a the first abstract value	 
	 * @param b the second abstract value	 
	 * @return true if the abstract values are equal
	 **/
	public boolean equals(AbstractValue a,
			      AbstractValue b) {
	    if (a == b)
		return true;
	    AbstractValueSet as = (AbstractValueSet) a;
	    AbstractValueSet bs = (AbstractValueSet) b;
	    AbstractValue[] a1 = as.set;
	    AbstractValue[] b1 = bs.set;
	    if (a1.length != b1.length)
		return false;
	    int grow = b1.length;
	    for (int i=a1.length-1;i>=0;i--)
		for (int j=b1.length-1;j>=0; j--) 
		    if (a1[i].equals(b1[j])) grow --;
	    return (grow == 0);
	}
	
    }  // end of SetEqualityEntry

    /**
     * If ref is an AbstractValueSet, call fe
     * on each element.  Otherwise just call
     * fe on ref itself.
     */
    public static void foreach(AbstractValue ref,
			       FE fe) {
	if (ref instanceof AbstractValueSet) {
	    AbstractValue[] set = ((AbstractValueSet)ref).getSet();
	    for (int i=set.length-1;i>=0;i--)
		fe.DO(set[i]);
	} else
	    fe.DO(ref);
    }

    public interface FE {
	public void DO(AbstractValue av);
    }

} // end of AbstractValueSet
