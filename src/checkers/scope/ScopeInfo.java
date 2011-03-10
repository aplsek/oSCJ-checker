package checkers.scope;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import javax.safetycritical.annotate.Scope;

import checkers.Utils;

public class ScopeInfo {
    public static final ScopeInfo CALLER = new ScopeInfo(Scope.CALLER);
    public static final ScopeInfo IMMORTAL = new ScopeInfo(Scope.IMMORTAL);
    /**
     * Represents an error in the checker. In order for the checker to keep
     * going, some things are expected to be non-null. This fills that gap.
     */
    public static final ScopeInfo INVALID = new ScopeInfo("invalid");
    /**
     * Represents a null literal in the program being checked. Can be coerced to
     * any other scope.
     */
    public static final ScopeInfo NULL = new ScopeInfo("null");
    /**
     * Represents a primitive. Primitives don't actually have scopes, but this
     * is used so that a null scope actually represents an error in the program.
     */
    public static final ScopeInfo PRIMITIVE = new ScopeInfo("primitive");
    public static final ScopeInfo THIS = new ScopeInfo(Scope.THIS);
    public static final ScopeInfo UNKNOWN = new ScopeInfo(Scope.UNKNOWN);
    private final String scope;

    /**
     * This field is used to indicate that this ScopeInfo object is utilized by
     * an object that represents a named scope. For all other objects, this is a
     * null value.
     */
    private final ScopeInfo represented;

    public ScopeInfo(String scope) {
        this(scope, null);
    }

    public ScopeInfo(String scope, ScopeInfo represented) {
        this.scope = scope;
        this.represented = represented;
    }

    public String getScope() {
        return scope;
    }

    public boolean isCaller() {
        return equals(CALLER);
    }

    public boolean isImmortal() {
        return equals(IMMORTAL);
    }

    public boolean isInvalid() {
        return equals(INVALID);
    }

    public boolean isNull() {
        return equals(NULL);
    }

    public boolean isPrimitive() {
        return equals(PRIMITIVE);
    }

    public boolean isThis() {
        return equals(THIS);
    }

    public boolean isUnknown() {
        return equals(UNKNOWN);
    }

    public boolean isReservedScope() {
        return isCaller() || isImmortal() || isInvalid() || isNull()
                || isUnknown() || isPrimitive() || isThis();
    }

    public boolean isFieldScope() {
        return false;
    }

    /**
     * Check that a field has a valid Scope annotation.
     * <p>
     * Fields must live in the same scope or parent scope of the objects which
     * refer to them. UNKNOWN annotations are accepted, since assignments to
     * UNKNOWN fields are checked by a dynamic guard.
     */
    boolean isValidInstanceFieldScope(ScopeInfo classScope, ScopeTree scopeTree) {
        return isThis() || isUnknown() || isPrimitive()
                || scopeTree.isAncestorOf(classScope, this);
    }

    public boolean isValidParentScope() {
        return !(isCaller() || isInvalid() || isNull() || isUnknown()
                || isPrimitive() || isThis());
    }

    public boolean isValidRunsIn(ScopeTree scopeTree) {
        return scopeTree.hasScope(this) || isCaller() || isThis();
    }

    public boolean isValidVariableScope(VariableElement v, ScopeTree scopeTree) {
        if (Utils.isStatic(v) && isThis())
            return false;
        if (v.getKind() == ElementKind.FIELD && isCaller())
            return false;
        return isPrimitive() || isUnknown() || isCaller() || isThis()
                || scopeTree.hasScope(this);
    }

    /**
     * Determine whether or not this scope is valid on a static field.
     *
     * CALLER is allowed because the class name is the receiver, which is
     * treated as IMMORTAL in ScopeVisitor.
     */
    public boolean isValidStaticScope() {
        return isCaller() || isImmortal() || isPrimitive();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && scope.equals(((ScopeInfo) obj).scope);
    }

    @Override
    public int hashCode() {
        return scope.hashCode();
    }

    @Override
    public String toString() {
        return scope;
    }

    /**
     * Get the scope represented by the object this ScopeInfo object is
     * associated with. For example:
     * <p>
     * <code>
     * Scope("a") DefineScope(name="b", parent="a")
     * ManagedMemory mem;
     * </code>
     * <p>
     * In this case, the current ScopeInfo object represents named scope a, but
     * a ScopeInfo object representing scope b will be returned by this method,
     * if it is the one attached to the mem variable.
     */
    public ScopeInfo getRepresentedScope() {
        return represented;
    }

    /**
     * Create a new ScopeInfo object representing the scope of the current
     * object, but which also represents a named scope declared by an
     * AllocationContext.
     */
    public ScopeInfo representing(ScopeInfo represented) {
        return new ScopeInfo(scope, represented);
    }
}
