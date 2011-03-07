package scope.illegalStateEx;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import java.util.List;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.SCJAllowed;


@SCJAllowed(members=true)
@Scope("MyApp")
class MyPEH2 extends PeriodicEventHandler {

    static PriorityParameters pri;
    static PeriodicParameters per;
    static StorageParameters stor;

    static {
        pri = new PriorityParameters(13);
        per = new PeriodicParameters(new RelativeTime(0, 0),
                new RelativeTime(500, 0));
        stor = new StorageParameters(1000L, 1000L, 1000L);
    }

    @SCJRestricted(INITIALIZATION)
    public MyPEH2() {
        super(pri, per, stor);
    }

    @Override
    @RunsIn("MyPEH2")
    public void handleAsyncEvent() {
        try {
            MyApp m1 = (MyApp) Mission.getCurrentMission();
            m1.pri.newInstance(List.class);
            m1.pri.enterPrivateMemory(500, new Runnable() {
                public void run() {
                }
            });

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void cleanUp() {
    }

    public StorageParameters getThreadConfigurationParameters() {
        return null;
    }
}
