package scope.scopeRunsIn.simple;

import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=IMMORTAL)
public abstract class TestIllegalStaticFieldScope extends Mission {
    //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_STATIC_FIELD_SCOPE
    static X x = null;
    static Y y = null;
    static Z z = null;

    @Scope("a")
    static class X { }

    @Scope(IMMORTAL)
    static class Y { }

    @Scope(CALLER)
    static class Z { }
}
