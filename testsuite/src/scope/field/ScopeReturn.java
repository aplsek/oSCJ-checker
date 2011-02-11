//testsuite/src/scope/field/ScopeReturn.java:47: Variable is annotated with @Scope annotation that does not correspond to @Scope annotation of its type.
//    @Scope("IMMORTAL") ScopeReturn mission2 = null;     // ERROR
//                                   ^
//testsuite/src/scope/field/ScopeReturn.java:65: Cannot assign expression in scope IMMORTAL to variable in scope MyHandler.
//        MyFoo localFoo = this.foo;                     // ERROR
//              ^
//testsuite/src/scope/field/ScopeReturn.java:71: Variable is annotated with @Scope annotation that does not correspond to @Scope annotation of its type.
//        @Scope(IMMORTAL) Bar barBar;              // ERROR
//                             ^
//testsuite/src/scope/field/ScopeReturn.java:76: Cannot assign expression in scope MyHandler to variable in scope IMMORTAL.
//        immFoo2 = new MyFoo();                        // ERROR
//                ^
//testsuite/src/scope/field/ScopeReturn.java:79: Cannot assign expression in scope IMMORTAL to variable in scope MyHandler.
//        localFoo2 = this.foo;                          // ERROR
//                  ^
//testsuite/src/scope/field/ScopeReturn.java:84: Cannot assign expression in scope UNKNOWN to variable in scope IMMORTAL.
//        @Scope(IMMORTAL) Mission mission2 = Mission.getCurrentMission();   // ERROR
//                                 ^
//6 errors

package scope.field;

import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;


@DefineScope(name="MyMission",parent=IMMORTAL)
@Scope("MyMission") 
public class ScopeReturn extends Mission {

    //@Scope(IMMORTAL) static Foo foo = new Foo();
    
    protected void initialize() { 
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }
}




@Scope("MyMission")
@DefineScope(name="MyHandler",parent="MyMission")
class PEH extends PeriodicEventHandler {

    ScopeReturn mission = null;
    
    @Scope("IMMORTAL") ScopeReturn mission2 = null;     // ERROR
    
    @Scope("IMMORTAL") MyFoo foo;                         // OK

    public PEH(PriorityParameters priority,
            PeriodicParameters parameters, StorageParameters scp, long memSize, ScopeReturn m) {
        super(priority, parameters, scp);
        
        // this.mission = m;           // IDENTIFIER
        mission = m; 
        
        
        @Scope(UNKNOWN) Mission mission = Mission.getCurrentMission();
    }

    @RunsIn("MyHandler") 
    public void handleAsyncEvent() {
        
        MyFoo localFoo = this.foo;                     // ERROR
        
        @Scope(IMMORTAL) MyFoo immFoo = this.foo;       // OK
        
        Bar bar;                                     // OK
        
        @Scope(IMMORTAL) Bar barBar;              // ERROR
        
        
        @Scope(IMMORTAL) MyFoo immFoo2 ;
        immFoo2 = this.foo;                          // OK
        immFoo2 = new MyFoo();                        // ERROR
        
        MyFoo localFoo2 ;
        localFoo2 = this.foo;                          // ERROR
        
        @Scope(UNKNOWN) Mission mission = Mission.getCurrentMission();      // OK
        
        
        @Scope(IMMORTAL) Mission mission2 = Mission.getCurrentMission();   // ERROR
        
    }

    @Override
    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }
}

class MyFoo {
}


@Scope("MyHandler")
class Bar extends MyFoo {
    
}


