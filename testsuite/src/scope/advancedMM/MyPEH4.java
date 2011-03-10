package scope.advancedMM;

import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.StorageParameters;

@SCJAllowed(members=true)
@Scope("APP")
@DefineScope(name="PEH", parent="APP")
public class MyPEH4 extends PeriodicEventHandler {

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
    public MyPEH4() {
        super(pri, per, stor);
    }

    Tick tock;

    @Override
    @SCJAllowed(Level.SUPPORT)
    @RunsIn("PEH")
    public void handleAsyncEvent() {
        try {
            @Scope(Scope.IMMORTAL)
            @DefineScope(name="APP", parent=Scope.IMMORTAL)
            ManagedMemory m = (ManagedMemory) MemoryArea.getMemoryArea(this);

            Tick time = (Tick) m.newInstance(Tick.class);

            //## ERROR
            m.executeInArea(new SCJRunnable() {
                public void run() {
                    MyPEH4.this.tock = new Tick();
                }
            });

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    @SCJRestricted(CLEANUP)
    public void cleanUp() {
    }

    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }
}
