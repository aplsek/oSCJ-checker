
package scope.scopeReturn;



import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;


@DefineScope(name="MyMission",parent=IMMORTAL)
@Scope("MyMission") 
class MyMission extends Mission {

    static Foo foo = new Foo();
    
    protected void initialize() { 
        new MyHandler(null, null, null, 0);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }
    
    @Scope(IMMORTAL)
    public Foo getFoo() {
        return this.foo;
    }

}


@Scope("MyMission")
@DefineScope(name="MyHandler",parent="MyMission")
class MyHandler extends PeriodicEventHandler {

    public MyHandler(PriorityParameters priority,
            PeriodicParameters parameters, StorageParameters scp, long memSize) {
        super(priority, parameters, scp);
    }

    @RunsIn("MyHandler") 
    public void handleAsyncEvent() {
        
        //A aObj = new A();                                                
       // B bObj = new B(); // OK
        
        MyMission mission = (MyMission) Mission.getCurrentMission();
        
        @Scope(IMMORTAL) Foo immFoo = mission.getFoo();
    }

    @Override
    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }
}


class Foo {
}