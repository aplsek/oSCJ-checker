package scope.memory;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.SCJRestricted;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

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

    @DefineScope(name="PEH",parent="Mission")
    @Scope("Mission")
    ManagedMemory mem2;                              // OK

    public void method() {
        @DefineScope(name="Mission",parent=IMMORTAL)
        @Scope(IMMORTAL)
        ManagedMemory mem1 = null;                              // OK

        try {
            //mem1.newInstance(Foo.class);
            mem.newInstance(Foo.class);

            mem2.newInstance(Foo.class);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //@RunsIn("PEH")
    //@SCJAllowed(SUPPORT)
    //public void handleAsyncEvent() {
    //       // mem.newInstance(Foo.class);
    //}
}

@Scope("Mission")
class Foo {


}