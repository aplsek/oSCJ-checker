package scope.scopeRunsIn.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.annotate.RunsIn;

@RunsIn(IMMORTAL)
//## checkers.scope.ScopeRunsInChecker.ERR_RUNS_IN_ON_CLASS
public class TestRunsInOnClass { }
