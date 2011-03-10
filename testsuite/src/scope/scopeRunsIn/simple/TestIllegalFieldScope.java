package scope.scopeRunsIn.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

@Scope(IMMORTAL)
@DefineScope(name="a", parent=IMMORTAL)
public abstract class TestIllegalFieldScope extends Mission {
    //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_FIELD_SCOPE
    @Scope("a") Object o;
    int[] o2;
}
