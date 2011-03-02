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
public abstract class TestBadNewInstanceVariable  extends Mission  {
}

@Scope("Mission")
@DefineScope(name="PEH",parent="Mission")
class PEHandler extends PeriodicEventHandler {

    @SCJRestricted(INITIALIZATION)
    public PEHandler(PriorityParameters priority, PeriodicParameters period,
            StorageParameters storage) {
        super(priority, period, storage);
    }


    @DefineScope(name="PEH",parent="Mission")
    @Scope("Mission")
    ManagedMemory mem1 = null;                              // OK

    public void method() {
        try {
            @DefineScope(name="PEH",parent="Mission")
            @Scope("Mission")
            ManagedMemory mem = null;                              // OK

            //## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE
            mem.newInstance(MyFoo2.class);                // ERROR
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }

    @Override
    public void handleAsyncEvent() {
    }
}


@Scope("Mission")
class MyFoo2 {
}