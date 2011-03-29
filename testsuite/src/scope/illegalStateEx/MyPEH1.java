package scope.illegalStateEx;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.PrivateMemory;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.ManagedMemory;

@SCJAllowed(members=true)
@Scope("MyApp")
@DefineScope(name="MyPEH1", parent="MyApp")
class MyPEH1 extends PeriodicEventHandler {

    @SCJRestricted(INITIALIZATION)
    public MyPEH1() {
        super(new PriorityParameters(13),
                new PeriodicParameters(new RelativeTime(0, 0), new RelativeTime(
                        500, 0)),
                        new StorageParameters(1000L, 1000L, 1000L));
    }

    @Override
    @SCJAllowed(SUPPORT)
    @RunsIn("MyPEH1")
    public void handleAsyncEvent() {
        MyApp m1 = (MyApp) Mission.getCurrentMission();
        m1.pri = (ManagedMemory) MemoryArea.getMemoryArea(new int[0]);
    }

    @Override
    @SCJRestricted(CLEANUP)
    public void cleanUp() {
    }

    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }
}
