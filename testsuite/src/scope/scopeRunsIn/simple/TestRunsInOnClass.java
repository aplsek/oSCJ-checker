package scope.scopeRunsIn.simple;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@RunsIn(Scope.IMMORTAL)
//## checkers.scope.ScopeRunsInChecker.ERR_RUNS_IN_ON_CLASS
public class TestRunsInOnClass { }
