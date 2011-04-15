package scope.scopeRunsIn.simple;

import javax.realtime.PriorityParameters;

import javax.safetycritical.ManagedThread;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope("D")
@DefineScope(name="D", parent=IMMORTAL)
@SCJAllowed(value=LEVEL_2, members=true)
public class TestNHRTParameters extends ManagedThread
{
    private static final long BackingStoreRequirements = 500;
    private static final long NativeStackRequirements = 2000;
    private static final long JavaStackRequirements = 300;

    public TestNHRTParameters(int priority) {
        super(new PriorityParameters(priority), new StorageParameters(
                BackingStoreRequirements, NativeStackRequirements,
                JavaStackRequirements));
    }

    @Override
    @RunsIn("D")
    @SCJAllowed
    //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_METHOD_RUNS_IN_OVERRIDE
    public void run() { }
}

