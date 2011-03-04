package checkers.scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.Scope;

public class ScopeInfo {
    public static final ScopeInfo CURRENT = new ScopeInfo(Scope.CURRENT);
    public static final ScopeInfo IMMORTAL = new ScopeInfo(Scope.IMMORTAL);
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
    public static final ScopeInfo UNKNOWN = new ScopeInfo(Scope.UNKNOWN);
    protected final String scope;

    /**
     * This field is:
     * <ul>
     * <li>null in general case
     * <li>non-null when this field/variable type implements AllocationContext
     * </ul>
     * Variables that are subtypes of AllocationContext must have DefineScope
     * annotations on them. This field is used to propagate that information
     * during checking.
     */
    protected DefineScopeInfo defineScope = null;

    public ScopeInfo(String scope) {
        this.scope = scope;
    }

    public ScopeInfo(String scope, DefineScopeInfo defineScope) {
        this.scope = scope;
        this.defineScope = defineScope;
    }

    public String getScope() {
        return scope;
    }

    public boolean isCurrent() {
        return equals(CURRENT);
    }

    public boolean isImmortal() {
        return equals(IMMORTAL);
    }

    public boolean isNull() {
        return equals(NULL);
    }

    public boolean isPrimitive() {
        return equals(PRIMITIVE);
    }

    public boolean isUnknown() {
        return equals(UNKNOWN);
    }

    public boolean isReservedScope() {
        return isCurrent() || isImmortal() || isNull() || isUnknown()
                || isPrimitive();
    }

    public boolean isFieldScope() {
        return false;
    }

    public boolean isValidParentScope() {
        return !(isCurrent() || isNull() || isUnknown() || isPrimitive());
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

    public DefineScopeInfo getDefineScope() {
        return defineScope;
    }
}
