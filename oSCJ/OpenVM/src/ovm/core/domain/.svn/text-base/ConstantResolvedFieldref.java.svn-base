package ovm.core.domain;

import ovm.core.repository.ConstantFieldref;
import ovm.core.repository.Selector;

public abstract class ConstantResolvedFieldref implements ConstantFieldref, ResolvedConstant {

    protected Field field;

    public ConstantResolvedFieldref(Field f) {
	field = f;
    }

    public Selector.Field asSelector() {
	return field.getSelector();
    }

    public Field getField() {
	return field;
    }

    public boolean isResolved() {
	return true;
    }

    public String toString() {
	return "Resolved{" + asSelector().toString() + "}";
    }

    /**
     * Called when no further constant pool resolution will be done to
     * discard caches.
     **/
    static public void dropCaches() {
	ConstantResolvedInstanceFieldref.cache_ = null;
	ConstantResolvedStaticFieldref.cache_ = null;
    }
}
