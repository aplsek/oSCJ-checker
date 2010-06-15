package s3.services.bytecode.analysis;

import ovm.services.bytecode.analysis.AbstractValue;
import ovm.services.bytecode.analysis.AbstractValueSet;
import ovm.services.bytecode.analysis.ExtensibleValueFactory;
import ovm.services.bytecode.analysis.ExtensibleValueFactory.AbstractValueImpl;
import ovm.services.bytecode.analysis.ExtensibleValueFactory.EqualsEntry;
import ovm.services.bytecode.analysis.ExtensibleValueFactory.FalseEqualsEntry;
import ovm.services.bytecode.analysis.ExtensibleValueFactory.FalseIncludesEntry;
import ovm.services.bytecode.analysis.ExtensibleValueFactory.IncludesEntry;
import ovm.services.bytecode.analysis.ExtensibleValueFactory.MergeEntry;
import ovm.services.bytecode.analysis.ExtensibleValueFactory.Registry;
import s3.services.bytecode.verifier.VerificationValueFactory;

public class LabeledAbstractValue extends AbstractValueImpl {

    public static final String LABELED = "LABELED";

    private final Label label_;

    private final int LABEL_ID;

    LabeledAbstractValue(Factory lsf, Label label) {
	super(lsf);
	this.label_ = label;
	this.LABEL_ID = lsf.LABELED_ID;
	if (label == null) throw new NullPointerException();
    }

    public int getId() {
	return LABEL_ID;
    }

    public String toString() {
	return "" + label_;
    }

    public Label getLabel() {
	return label_;
    }

    /**
     * This Factory provides an generic implementation of the
     * ObjectFlowAnalysis.Factory in which merging abstract values is
     * defined as merging the sets.  The basic abstract values are all
     * defined in terms of the label that specifies their respective
     * creation site (i.e. the allocation site, the
     * MethodArgumentSelector, etc.).<p>
     *
     * In other words, the LabeledAbstractValue.Factory provides
     * abstract values that allow the client software to directly find out
     * all possible sources of the abstract value.<p>
     *
     * Note that this factory is only helping in building intraprocedual
     * sets.  For an interprocedual analysis, the client code will have to
     * link the data flow graph together (under consideration of virtual
     * dispatch!).
     *
     * The parent class, VerificationValueFactory, is used to provide
     * abstract values for the primitive data types (and jump targets).
     * @see ovm.services.bytecode.analysis.AbstractValueSet
     * @author Christian Grothoff
     */
    public static class Factory extends VerificationValueFactory {

	final AbstractValueSet.SetRegistry sets_;

	final int LABELED_ID;

	private final AbstractValueSet MY_NULL;

	public Factory() {
	    this.sets_ = new AbstractValueSet.SetRegistry(this);
	    this.LABELED_ID = registerAbstractValue(new LabeledAbstractValue.LAVRegistry(sets_));
	    this.MY_NULL = sets_.makeSet(super.makeNull());
	}

	public AbstractValue makeNull() {
	    return MY_NULL;
	}

	public AbstractValue makeAVFor(Handle h) {
	    return new LabeledAbstractValue(this, (Label) h);
	}

    } // end of LabeledAbstractValue.Factory

    /**
     * @author Christian Grothoff
     */
    public static class LAVRegistry extends ExtensibleValueFactory.Registry {

	private final AbstractValueSet.SetRegistry sets_;

	LAVRegistry(AbstractValueSet.SetRegistry sets) {
	    this.sets_ = sets;
	}

	AbstractValueSet.SetRegistry getSets() {
	    return sets_;
	}

	/** Obtain a textual description of the registered type (free-form,
	 * but must be recognized by all other types that are registered
	 * **later** for interoperabiltiy)
	 * @return "@" for This pointer
	 **/
	public String describe() {
	    return LABELED;
	}

	/**
	 * Obtain the Includes object for the table a7t the intersection of
	 * these two objects. This default implementation returns true if
	 * the descriptions are identical, otherwise false.
	 * @param other the Registry object of the other type
	 * @return the includes relationship for (this,other).
	 **/
	public IncludesEntry getIncludes(Registry other) {
	    String od = other.describe();
	    if (od == LABELED) return new LabelTestIncludesEntry();
	    else if (od == AbstractValueSet.TYPESET) return new SetTestIncludesEntry();
	    else return new FalseIncludesEntry();
	}

	public static class LabelTestIncludesEntry extends IncludesEntry {
	    public boolean includes(AbstractValue a, AbstractValue b) {
		LabeledAbstractValue la = (LabeledAbstractValue) a;
		LabeledAbstractValue lb = (LabeledAbstractValue) b;
		return la.getLabel().equals(lb.getLabel());
	    }
	}

	public static class SetTestIncludesEntry extends IncludesEntry {
	    public boolean includes(AbstractValue a, AbstractValue b) {
		AbstractValueSet lb = (AbstractValueSet) b;
		AbstractValue[] set = lb.getSet();
		return ((set.length == 1) && (set[0].equals(a)));
	    }
	}

	public static class SetTestIncludedEntry extends IncludesEntry {
	    public boolean includes(AbstractValue a, AbstractValue b) {
		AbstractValueSet la = (AbstractValueSet) a;
		return la.contains(b);
	    }
	}

	/**
	 * Obtain the Includes object for the table at the intersection of
	 * these two objects.
	 * @param other the Registry object of the other type
	 * @return the includes relationship for (other,this).
	 **/
	public IncludesEntry getIncluded(Registry other) {
	    String od = other.describe();
	    if (od == LABELED) return new LabelTestIncludesEntry();
	    else if (od == AbstractValueSet.TYPESET) return new SetTestIncludedEntry();
	    else return new FalseIncludesEntry();
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
	    if (od == LABELED) return new LabelLabelMergeEntry(this);
	    else if (od == AbstractValueSet.TYPESET) return new LabelSetMergeEntry(this);
	    else return new AnyAnySetMergeEntry(this);
	}

	public static class LabelLabelMergeEntry extends MergeEntry {
	    private final AbstractValueSet.SetRegistry sets_;

	    LabelLabelMergeEntry(LAVRegistry lr) {
		this.sets_ = lr.getSets();
	    }

	    public AbstractValue merge(AbstractValue a, AbstractValue b) {
		LabeledAbstractValue la = (LabeledAbstractValue) a;
		LabeledAbstractValue lb = (LabeledAbstractValue) b;
		if (la.getLabel().equals(lb.getLabel())) return a;
		else return sets_.makeSet(a, b);
	    }
	}

	public static class AnyAnySetMergeEntry extends MergeEntry {
	    private final AbstractValueSet.SetRegistry sets_;

	    AnyAnySetMergeEntry(LAVRegistry lr) {
		this.sets_ = lr.getSets();
	    }

	    public AbstractValue merge(AbstractValue a, AbstractValue b) {
		return sets_.makeSet(a, b);
	    }
	}

	public static class LabelSetMergeEntry extends MergeEntry {
	    private final AbstractValueSet.SetRegistry sets_;

	    LabelSetMergeEntry(LAVRegistry lr) {
		this.sets_ = lr.getSets();
	    }

	    public AbstractValue merge(AbstractValue a, AbstractValue b) {
		LabeledAbstractValue la = (LabeledAbstractValue) a;
		AbstractValueSet lb = (AbstractValueSet) b;
		return sets_.makeSet(lb, la);
	    }
	}

	public static class SetLabelMergeEntry extends MergeEntry {
	    private final AbstractValueSet.SetRegistry sets_;

	    SetLabelMergeEntry(LAVRegistry lr) {
		this.sets_ = lr.getSets();
	    }

	    public AbstractValue merge(AbstractValue a, AbstractValue b) {
		LabeledAbstractValue la = (LabeledAbstractValue) b;
		AbstractValueSet lb = (AbstractValueSet) a;
		return sets_.makeSet(lb, la);
	    }
	}

	public MergeEntry getMerged(Registry other) {
	    String od = other.describe();
	    if (od == LABELED) return new LabelLabelMergeEntry(this);
	    else if (od == AbstractValueSet.TYPESET) return new SetLabelMergeEntry(this);
	    else return new AnyAnySetMergeEntry(this);
	}

	public static class LabelLabelEqualsEntry extends EqualsEntry {
	    public boolean equals(AbstractValue a, AbstractValue b) {
		LabeledAbstractValue la = (LabeledAbstractValue) a;
		LabeledAbstractValue lb = (LabeledAbstractValue) b;
		return la.getLabel().equals(lb.getLabel());
	    }
	}

	public static class SetLabelEqualsEntry extends EqualsEntry {
	    public boolean equals(AbstractValue a, AbstractValue b) {
		AbstractValueSet la = (AbstractValueSet) a;
		AbstractValue[] set = la.getSet();
		return ((set.length == 1) && (set[0].equals(b)));
	    }
	}

	public static class LabelSetEqualsEntry extends EqualsEntry {
	    public boolean equals(AbstractValue a, AbstractValue b) {
		AbstractValueSet lb = (AbstractValueSet) b;
		AbstractValue[] set = lb.getSet();
		return ((set.length == 1) && (set[0].equals(a)));
	    }
	}

	/**
	 * Obtain the equals object for the table at the intersection of
	 * these two objects.  The default implementation returns True if
	 * the Registries are equals, otherwise false.
	 * @param other the Registry object of the other type
	 * @return the equals relationship
	 **/
	public EqualsEntry getEquals(Registry other) {
	    String od = other.describe();
	    if (LABELED == od) return new LabelLabelEqualsEntry();
	    else if (LABELED == AbstractValueSet.TYPESET) return new LabelSetEqualsEntry();
	    else return new FalseEqualsEntry();
	}

    } // end of Registry

} // end of LabeledAbstractValue

