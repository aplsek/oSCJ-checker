

import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import java.util.List;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.SCJAllowed;

import static javax.safetycritical.annotate.Level.SUPPORT;

@SCJAllowed(members = true)
@Scope("MyApp")
@DefineScope(name="MyPEH2", parent="MyApp")
class MyPEH2 extends PeriodicEventHandler {

    @SCJRestricted(INITIALIZATION)
    public MyPEH2() {
        super(new PriorityParameters(13),
              new PeriodicParameters(new RelativeTime(0, 0), new RelativeTime(
                        500, 0)),
              new StorageParameters(1000L, 1000L, 1000L));
    }

    @Override
    @RunsIn("MyPEH2")
    public void handleAsyncEvent() {
        try {
            MyRunnable run = new MyRunnable();

            MyApp m1 = (MyApp) Mission.getCurrentMission();
            m1.pri.newInstance(List.class);
            m1.pri.enterPrivateMemory(500, run);

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

@SCJAllowed(members = true)
@DefineScope(name = "new-scope", parent = "MyPEH1")
class MyRunnable implements Runnable {

    @SCJAllowed(SUPPORT)
    @RunsIn("new-scope")
    public void run() {
    }

}
