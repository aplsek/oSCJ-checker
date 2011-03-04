package scope.scopeReturn;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.MemoryArea;
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
public class NewInstance extends Mission {

    @Override
    protected void initialize() {
        new PEH(null, null, null, 0,this);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }
}

@Scope("MyMission")
@DefineScope(name="MyHandler",parent="MyMission")
class PEH extends PeriodicEventHandler {

    NewInstance mission;

    public PEH(PriorityParameters priority,
            PeriodicParameters parameters, StorageParameters scp, long memSize, NewInstance mission) {
        super(priority, parameters, scp);

        this.mission = mission;
    }

    @Override
    @RunsIn("MyHandler")
    public void handleAsyncEvent() {
        try {
            @Scope("MyMission") MemoryArea mem =  MemoryArea.getMemoryArea(mission);
            MissionFoo missionFoo = (MissionFoo) mem.newInstance(MissionFoo.class);

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }
}

@Scope("MyMission")
class MissionFoo {

}