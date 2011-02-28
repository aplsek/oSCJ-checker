//testsuite/src/scope/field/MyMission.java:67: Cannot assign expression in scope IMMORTAL to variable in scope MyHandler.
//        Foo localFoo = this.foo;
//            ^
//1 error


package scjAllowed.simple;



import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Level.SUPPORT;

@SCJAllowed(members=true)
@DefineScope(name="MyMission",parent=IMMORTAL)
@Scope("MyMission")
class MyMission extends Mission  {

    @Override
    @SCJRestricted(INITIALIZATION)
    protected void initialize() {
        new MyHandler(null, null, null, 0,this);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }
}

@SCJAllowed(members=true)
@Scope("MyMission")
@DefineScope(name="MyHandler",parent="MyMission")
class MyHandler extends PeriodicEventHandler {

    @SCJRestricted(INITIALIZATION)
    public MyHandler(PriorityParameters priority,
            PeriodicParameters parameters, StorageParameters scp, long memSize, MyMission mission) {
        super(priority, parameters, scp);
    }

    @Override
    @RunsIn("MyHandler")
    @SCJAllowed(SUPPORT)
    public void handleAsyncEvent() {
    }

    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }
}

