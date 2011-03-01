package scope.scopeRunsIn.simple;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

@Scope(Scope.IMMORTAL)
@DefineScope(name="a", parent=Scope.IMMORTAL)
public abstract class TestIllegalFieldScope extends Mission {
    //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_FIELD_SCOPE
    @Scope("a") Object o;
    int[] o2;
}
