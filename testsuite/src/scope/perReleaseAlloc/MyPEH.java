package scope.perReleaseAlloc;

import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import java.util.Arrays;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members=true)
@Scope("MyApp")
@DefineScope(name="MyPEH", parent="MyApp")
public class MyPEH extends PeriodicEventHandler {

    static PriorityParameters pri;
    static PeriodicParameters per;
    static StorageParameters stor;

    static {
        pri = new PriorityParameters(13);
        per = new PeriodicParameters(new RelativeTime(0, 0), new RelativeTime(
                500, 0));
        stor = new StorageParameters(1000L, 1000L, 1000L);
    }

    @SCJRestricted(INITIALIZATION)
    public MyPEH() {
        super(pri, per, stor);
    }

    long median;

    @Override
    @RunsIn("MyPEH")
    public void handleAsyncEvent() {
        final long times[] = new long[1000];
        SCJRunnable r = new SCJRunnable() {
            @RunsIn("RunScope")
            //## checkers.scope.ScopeRunsInChecker.ERR_BAD_SCOPE_NAME
            public void run() {
                long[] copy = new long[1000];
                for (int i = 0; i < 1000; i++)
                    copy[i] = times[i];
                Arrays.sort(copy);
                median = copy[500];
            }
        };

        @Scope("MyApp")
        @DefineScope(name="RunScope", parent="MyPEH")
        ManagedMemory m = ManagedMemory.getCurrentManagedMemory();
        m.enterPrivateMemory(8000, r);

        Mission.getCurrentMission().requestSequenceTermination();
    }

    @Override
    @SCJRestricted(CLEANUP)
    public void cleanUp() {
    }

    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }
}
