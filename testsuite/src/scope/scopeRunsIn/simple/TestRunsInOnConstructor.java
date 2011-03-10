package scope.scopeRunsIn.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@Scope(IMMORTAL)
@DefineScope(name="a", parent=IMMORTAL)
public abstract class TestRunsInOnConstructor extends Mission {
    @RunsIn("a")
    //## checkers.scope.ScopeRunsInChecker.ERR_RUNS_IN_ON_CONSTRUCTOR
    public TestRunsInOnConstructor() { }
}
