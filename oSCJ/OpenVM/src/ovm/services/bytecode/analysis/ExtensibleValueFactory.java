package ovm.services.bytecode.analysis;


/**
 * FIXME: comment is not fully up-to-speed wrt to split between VerificationValueFactory
 * and ExtensibleValueFactory!
 *
 * For the sake of extensibility (and performance)  actions that depend on
 * two AbstractValue types, such as merge, include and equals are selected
 * using a table based form of double dispatch.
 * 
 * Any factory implementation must provide several groups of objects. For each
 * AbstractValue type X there is a class XRegistry which is responsible
 * for initializing the double-dispatch tables for that class. This
 * Registry class typically has a couple of inner classes defining the
 * merge/include/equals behavior of this class in relation to the other
 * classes. <p>
 * 
 * The merge/include/equals behavior is specified using Entry classes.  The
 * Factory has 4 two-dimensional tables of entries used to perform
 * double-dispatch. Those are initialized in the constructor by registering
 * all known types. <p>
 * 
 * To extend the factory, subclass it, add new AbstractValues, provide
 * Registry implementations giving out Entry objects that define the
 * merge/include/equals behavior in combination with all so-far-known
 * AbstractValue types.
 *
 * @author Christian Grothoff
 **/
public class ExtensibleValueFactory
    extends s3.core.S3Base {

    /**
     * Table for double-dispatch results for "equals".
     **/
    public EqualsEntry[][] EQUALS_TABLE = new EqualsEntry[0][0];

    /**
     * Table for double-dispatch results for "includes"
     **/
    public IncludesEntry[][] INCLUDES_TABLE = new IncludesEntry[0][0];

    /** Table for double-dispatch results for "merge" **/
    public MergeEntry[][] MERGE_TABLE = new MergeEntry[0][0];

    /** Registry objects for all types of AbstractValues supported in
     * combination with this ValueFactory.  **/
    private Registry[] registered_ = new Registry[0];

    public ExtensibleValueFactory() {
    }

    /**
     * Accept the Registry for an AbstractValue type and update the
     * double-dispatch TALBES accordingly.
     **/
    public int registerAbstractValue(Registry r) {
        int count = registered_.length;
        int ncount = count + 1;
        Registry[] newreg = new Registry[ncount];

        System.arraycopy(registered_, 0, newreg, 0, count);

        newreg[count] = r;
        registered_ = newreg;
        // now: update all tables!

        EqualsEntry[][] et = new EqualsEntry[ncount][ncount];
        for (int i = 0; i < count; i++)
            for (int j = 0; j < count; j++)
                et[i][j] = EQUALS_TABLE[i][j];

        for (int i = 0; i <= count; i++) {
            EqualsEntry val = r.getEquals(registered_[i]);
            et[count][i] = val;
            et[i][count] = val;
        }
        EQUALS_TABLE = et;

        IncludesEntry[][] it = new IncludesEntry[ncount][ncount];
        for (int i = 0; i < count; i++)
            for (int j = 0; j < count; j++)
                it[i][j] = INCLUDES_TABLE[i][j];
        for (int i = 0; i <= count; i++) {
            it[i][count] = r.getIncluded(registered_[i]);
	    // System.out.println("IT["+i+","+count+"]: " + it[i][count].getClass());
	}
        for (int i = 0; i < count; i++) {
            it[count][i] = r.getIncludes(registered_[i]); 
	    // System.out.println("IT["+count+","+i+"]: " + it[count][i].getClass());
	}
        INCLUDES_TABLE = it;

        MergeEntry[][] me = new MergeEntry[ncount][ncount];
        for (int i = 0; i < count; i++)
            for (int j = 0; j < count; j++)
                me[i][j] = MERGE_TABLE[i][j];
        for (int i = 0; i <= count; i++)
            me[i][count] = r.getMerged(registered_[i]);
        for (int i = 0; i < count; i++)
            me[count][i] = r.getMerge(registered_[i]);
        MERGE_TABLE = me;

        return count;
    }

    public static abstract class EqualsEntry {
        /**
         * check if two AbstractValues are equal
         * @param a the first abstract value	 
         * @param b the second abstract value	 
         * @return true if the abstract values are equal
         **/
        public abstract boolean equals(AbstractValue a, AbstractValue b);
    } // end of EqualsEntry

    public static class FalseEqualsEntry extends EqualsEntry {
        public boolean equals(AbstractValue a, AbstractValue b) {
            return false;
        }
    }
    
    public static class TrueEqualsEntry extends EqualsEntry {
        public boolean equals(AbstractValue a, AbstractValue b) {
            return true;
        }
    }

    public static abstract class IncludesEntry {
        /**
         * check if this AbstractValue "includes" the other abstract value.<br>
         * Includes is defined in terms of being more critical. E.g. a
         * reference is more critical then a null pointer, thus
         * Reference.includes(NullPointer).
         * @param a the first abstract value	 
         * @param b the second abstract value	 
         * @return true if the other element is less critical, and thus the
         * analysis of v is included in the analysis of this.  
         **/
        public abstract boolean includes(AbstractValue a,
					 AbstractValue b);
    } // end of IncludesEntry    

    public static class FalseIncludesEntry
	extends IncludesEntry {
        public boolean includes(AbstractValue a, 
				AbstractValue b) {
            return false;
        }
    }
    public static class TrueIncludesEntry 
	extends IncludesEntry {
        public boolean includes(AbstractValue a,
				AbstractValue b) {
            return true;
        }
    }
 
    public static abstract class MergeEntry {
        /**
        * combine this AbstractValue with another AbstractValue.  Returns
        * the combination (most critical common SE).
        * @param a the first abstract value	 
        * @param b the second abstract value	 
        * @return null for errors, otherwise the merged  AbstractValue
        **/
        public abstract AbstractValue merge(AbstractValue a, AbstractValue b);

    } // end of MergeEntry

    public static class NullMergeEntry extends MergeEntry {
        public AbstractValue merge(AbstractValue a, AbstractValue b) {
            return null;
        }
    } // end of NullMergeEntry

    public static class IdentityMergeEntry extends MergeEntry {
        public AbstractValue merge(AbstractValue a, AbstractValue b) {
            return a; // a should be equal to b
        }
    } // end of IdentityMergeEntry

    // FIXME Explain the name OtherMergeEntry???
    public static class OtherMergeEntry extends MergeEntry {
        public AbstractValue merge(AbstractValue a, AbstractValue b) {
            return b; // a is contained in b
        }
    } // end of OtherMergeEntry

    public static abstract class Registry {

        /**
         * @return a textual description of the registered type (free-form,
         * but must be recognized by all other types that are registered
         * **later** for interoperabiltiy)
         **/
        public abstract String describe();

        /**
         * Obtain the Includes object for the table at the intersection of
         * these two objects. This default implementation returns true if
         * the descriptions are identical, otherwise false.
         * @param other the Registry object of the other type
         * @return the includes relationship for (this,other).
         **/
        public IncludesEntry getIncludes(Registry other) {
            if (describe().equals(other.describe()))
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
            if (describe().equals(other.describe()))
                return new IdentityMergeEntry();
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
            if (describe().equals(other.describe()))
                return new IdentityMergeEntry();
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
                return new TrueEqualsEntry();
            else
                return new FalseEqualsEntry();
        }
    } // end of Registry

    /* ***************** FINALLY: ABSTRACT VALUES ************ */

    public static abstract class AbstractValueImpl
	implements AbstractValue {

	protected final ExtensibleValueFactory evf_;

	protected AbstractValueImpl(ExtensibleValueFactory evf) {
	    this.evf_ = evf;
	}

        /**
         * is this abstract value representing a jump target?
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
        public WidePrimitive getWidePrimitive() {
            throw new AbstractValueError();
        }
        /**
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
          * @return true if it is a reference.
         **/
        public boolean isReference() {
            return false;
        }
        /**
         * return a reference abstract value if this abstract value is a reference one.
         * @exception <code>AbstractValueError</code> 
         * if it is not a reference value, 
         **/
        public AbstractValue.Reference getReference() {
            throw new AbstractValueError();
        }
        /**
           * @return true if it is an invalid stack location.
         **/
        public boolean isInvalid() {
            return false;
        }
        /**
         * return an invalid abstract value if this abstract value is an invalid one.
         * @exception <code>AbstractValueError</code> 
         * if it is not an invalid value, 
         **/
        public AbstractValue.Invalid getInvalid() {
            throw new AbstractValueError();
        }

        /**
         * check if two AbstractValues are equal
         * @param v the other abstract value	 
         * @return true if the abstract values are equal
         **/
        public final boolean equals(AbstractValue v) {
            return evf_.EQUALS_TABLE[getId()][v.getId()].equals(this, v);
        }

        /**
         * check if this AbstractValue "includes" the other abstract value.<br>
         * Includes is defined in terms of being more critical. E.g. a
         * reference is more critical then a null pointer, thus
         * Reference.includes(NullPointer).
         * @param v the other abstract value
         * @return true if the other element is less critical, and thus the
         * analysis of v is included in the analysis of this.  
         **/
        public final boolean includes(AbstractValue v) {
            return evf_.INCLUDES_TABLE[getId()][v.getId()].includes(this, v);
        }

        /**
         * combine this AbstractValue with another AbstractValue.  Returns
         * the combination (most critical common SE).
         * @return null for errors, otherwise the merged
         *          AbstractValue
         **/
        public AbstractValue merge(AbstractValue v) {
            return evf_.MERGE_TABLE[getId()][v.getId()].merge(this, v);
        }

    } // end of AbstractValueImpl

} // end of ExtensibleValueFactory
