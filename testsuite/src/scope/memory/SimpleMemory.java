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

    public void method() {
        @DefineScope(name="Mission",parent=IMMORTAL)
        @Scope(IMMORTAL)
        ManagedMemory mem;                              // OK

        @Scope(IMMORTAL)
        //## checkers.scope.ScopeChecker.ERR_MEMORY_AREA_NO_DEFINE_SCOPE_ON_VAR
        ManagedMemory mem2;                             // ERR

        @DefineScope(name="PEH",parent="Mission")
        @Scope("Mission")
        ManagedMemory mem3;                             // OK

        @DefineScope(name="PEH",parent="Mission")
        @Scope(IMMORTAL)
        //## checkers.scope.ScopeChecker.ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE
        ManagedMemory mem4;                              // ERROR

        @DefineScope(name="PEH",parent="Mission")
        ManagedMemory mem5;                             // OK

        @DefineScope(name="Mission",parent=IMMORTAL)
        //## checkers.scope.ScopeChecker.ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE
        ManagedMemory mem6;                             // ERROR

        @DefineScope(name="Mission",parent=IMMORTAL)
        @Scope("Mission")
        //## checkers.scope.ScopeChecker.ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE
        ManagedMemory mem7;                     // ERROR

        @DefineScope(name="Mission",parent="PEH")
        @Scope("Mission")
        //## checkers.scope.ScopeChecker.ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT
        ManagedMemory mem8;                     // ERROR


    }

    //@RunsIn("PEH")
    //@SCJAllowed(SUPPORT)
    //public void handleAsyncEvent() {
    //       // mem.newInstance(Foo.class);
    //}
}

