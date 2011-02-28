//testsuite/src/scope/field/MyMission.java:67: Cannot assign expression in scope IMMORTAL to variable in scope MyHandler.
//        Foo localFoo = this.foo;
//            ^
//1 error


package scope.field;



import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;


@DefineScope(name="MyMission",parent=IMMORTAL)
@Scope("MyMission")
class MyMission /* extends Mission */ {

    Foo foo;

    protected void initialize() {
        new MyHandler(null, null, null, 0,this);
    }

    public long missionMemorySize() {
        return 0;
    }
}


@Scope("MyMission")
@DefineScope(name="MyHandler",parent="MyMission")
class MyHandler /*extends PeriodicEventHandler*/ {

    MyMission mission = null;

    @Scope("IMMORTAL") Foo foo;

    public MyHandler(PriorityParameters priority,
            PeriodicParameters parameters, StorageParameters scp, long memSize, MyMission mission) {
        //super(priority, parameters, scp);
        this.mission = mission;
    }

    @RunsIn("MyHandler")
    public void handleAsyncEvent() {
        Foo localFoo = this.foo;
    }

    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }
}

class Foo {
}
