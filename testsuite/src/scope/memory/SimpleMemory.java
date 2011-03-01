package scope.memory;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Level.SUPPORT;

@DefineScope(name="Mission",parent=IMMORTAL)
@Scope("Mission")
public abstract class SimpleMemory extends Mission {
}

@Scope("Mission")
@DefineScope(name="PEH",parent="Mission")
abstract class PEH extends PeriodicEventHandler {

    @SCJRestricted(INITIALIZATION)
    public PEH(PriorityParameters priority, PeriodicParameters period,
            StorageParameters storage) {
        super(priority, period, storage);
    }

    @DefineScope(name="Mission",parent=IMMORTAL)
    @Scope(IMMORTAL)
    ManagedMemory mem;                              // OK

    //@Scope(IMMORTAL)
    //ManagedMemory mem2;

    @DefineScope(name="PEH",parent="Mission")
    @Scope("Mission")
    ManagedMemory mem3;                             // OK

    //@DefineScope(name="PEH",parent="Mission")
    //@Scope(IMMORTAL)                                // ERR
    //ManagedMemory mem4;

    @DefineScope(name="PEH",parent="Mission")
    ManagedMemory mem5;                             // OK

    //@DefineScope(name="Mission",parent=IMMORTAL)
    //ManagedMemory mem6;                             // ERROR

    public void method() {
       mem = ManagedMemory.getCurrentManagedMemory();
    }

    @Override
    @RunsIn("PEH")
    @SCJAllowed(SUPPORT)
    public void handleAsyncEvent() {
           // mem.newInstance(Foo.class);
    }
}

