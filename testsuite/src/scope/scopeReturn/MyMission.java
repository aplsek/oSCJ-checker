//scope/scopeReturn/MyMission.java:53: Cannot return expression in scope MyHandler in a method that has @Scope annotation: IMMORTAL.
//        return new Bar();           // ERR
//        ^
//scope/scopeReturn/MyMission.java:63: Cannot return expression in scope MyMission in a method that has @Scope annotation: MyHandler.
//        return field;           // ERR
//        ^
//scope/scopeReturn/MyMission.java:68: Cannot return expression in scope MyHandler in a method that has @Scope annotation: MyMission.
//        return new BarScope();           // ERR
//        ^
//3 errors


package scope.scopeReturn;



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

@Scope(IMMORTAL)
class ImmortalClass {
    
    @Scope(IMMORTAL)
    public Foo getFoo() {
        return new Foo();           // OK
    }
    
}

@DefineScope(name="MyMission",parent=IMMORTAL)
@Scope("MyMission") 
class MyMission extends Mission {

    //@Scope(IMMORTAL) static Foo foo = new Foo();
    
    protected void initialize() { 
        new MyHandler(null, null, null, 0,this);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }
    
    MyField field;
    
    @Scope(IMMORTAL) @RunsIn("MyHandler")
    public Bar getFooErr() {
        return new Bar();           // ERR
    }
    
    @Scope("MyHandler") @RunsIn("MyHandler")
    public BarScope getBarScope() {
        return new BarScope();           // OK
    }
    
    @Scope("MyHandler") @RunsIn("MyHandler")
    public MyField getField() {
        return field;           // ERR
    }
    
    @Scope("MyMission") @RunsIn("MyHandler")
    public BarScope getBarScope2() {
        return new BarScope();           // ERR
    }
    
    @Scope(UNKNOWN) @RunsIn("MyHandler")
    public BarScope getBarScopeUNKNOWN() {
        return new BarScope();           // OK
    }
    
}


@Scope("MyMission")
@DefineScope(name="MyHandler",parent="MyMission")
class MyHandler extends PeriodicEventHandler {

    MyMission mission = null;
    
    public MyHandler(PriorityParameters priority,
            PeriodicParameters parameters, StorageParameters scp, long memSize, MyMission mission) {
        super(priority, parameters, scp);
        
        this.mission = mission;
        
    }

    @RunsIn("MyHandler") 
    public void handleAsyncEvent() {
        //MyMission mission = (MyMission) Mission.getCurrentMission();
        
       // @Scope(IMMORTAL) Foo immFoo = mission.getFoo();
        
       // @Scope(IMMORTAL) Foo immFoo2 = mission.getFooGeneric();
    }

    @Override
    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }
}

@Scope(IMMORTAL)
class Foo {
}

class Bar {}

@Scope("MyHandler")
class BarScope {}

class MyField {}