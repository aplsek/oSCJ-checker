package scope.scopeRunsIn.simple;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=Scope.IMMORTAL)
public abstract class TestIllegalStaticFieldScope extends Mission {
    //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_STATIC_FIELD_SCOPE
    static X x = null;
    static Y y = null;
    static Z z = null;

    @Scope("a")
    static class X { }

    @Scope(Scope.IMMORTAL)
    static class Y { }

    @Scope(Scope.CALLER)
    static class Z { }
}
