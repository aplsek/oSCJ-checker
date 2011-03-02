package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="Mission",parent=IMMORTAL)
@Scope("Mission")
public abstract class TestNewInstance  extends Mission  {
}

@Scope("Mission")
@DefineScope(name="PEH",parent="Mission")
abstract class MyPEH extends PeriodicEventHandler {

    @SCJRestricted(INITIALIZATION)
    public MyPEH(PriorityParameters priority, PeriodicParameters period,
            StorageParameters storage) {
        super(priority, period, storage);
    }

    @DefineScope(name="Mission",parent=IMMORTAL)
    @Scope(IMMORTAL)
    ManagedMemory mem;                              // OK

    public void method() {
        try {
            mem.newInstance(MyFoo.class);             // OK
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


@Scope("Mission")
class MyFoo {
}