package all.sanity;

import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.LinearMissionSequencer;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.Safelet;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope(IMMORTAL)
@SCJAllowed(value = LEVEL_1, members = true)
@DefineScope(name = "MISSION", parent = IMMORTAL)
public class HelloSCJ extends Mission implements Safelet {

    // From Mission
    @SCJRestricted(INITIALIZATION)
    @Override
    @SCJAllowed(SUPPORT)
    @RunsIn("MISSION")
    protected void initialize() {

        PeriodicEventHandler peh = new MyHandler(new PriorityParameters(11),
                new PeriodicParameters(new RelativeTime(0, 0),
                        new RelativeTime(1000, 0)), new StorageParameters(
                        10000, 1000, 1000));
        peh.register();
    }

    // @Override
    @SCJRestricted(INITIALIZATION)
    @SCJAllowed(SUPPORT)
    public MissionSequencer getSequencer() {
        // we assume this method is invoked only once
        StorageParameters sp = new StorageParameters(1000000, 0, 0);
        return new LinearMissionSequencer(new PriorityParameters(13), sp, this);
    }

    // @Override
    @Override
    @SCJAllowed(SUPPORT)
    public long missionMemorySize() {
        return 100000;
    }

    @SCJAllowed(SUPPORT)
    @SCJRestricted(INITIALIZATION)
    public void setUp() {
    }

    @SCJAllowed(SUPPORT)
    @SCJRestricted(CLEANUP)
    public void tearDown() {
    }

    @SCJAllowed(value = LEVEL_1, members = true)
    @Scope("MISSION")
    @DefineScope(name = "HANDLER", parent = "MISSION")
    public static class MyHandler extends PeriodicEventHandler {

        @SCJRestricted(INITIALIZATION)
        public MyHandler(PriorityParameters prio, PeriodicParameters peri,
                StorageParameters stor) {
            super(prio, peri, stor);
        }

        int cnt;

        @Override
        @RunsIn("HANDLER")
        @SCJAllowed(SUPPORT)
        public void handleAsyncEvent() {
            ++cnt;
        }
    }
}
