package scope.scope.simple;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import java.util.Arrays;
import java.util.List;

import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.Safelet;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;

@SCJAllowed(members=true)
@Scope(IMMORTAL)
public class TestVariableScope implements Safelet {

    @Override
    @SCJAllowed(SUPPORT)
    @SCJRestricted(INITIALIZATION)
    public MissionSequencer getSequencer() {
        return new MS();
    }

    @SCJRestricted(INITIALIZATION)
    @SCJAllowed(SUPPORT)
    public void setUp() {
    }

    @SCJRestricted(CLEANUP)
    @SCJAllowed(SUPPORT)
    public void tearDown() {
    }

    @Scope(IMMORTAL)
    @SCJAllowed(members=true)
    @DefineScope(name="MyApp", parent=IMMORTAL)
    public static class MS extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public MS() {
            super(null, null);
        }

        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("MyApp")
        protected Mission getNextMission() {
            return new MyMission();
        }
    }

    @Scope("MyApp")
    @SCJAllowed(members=true)
    public static class MyMission extends Mission {

        @Override
        @SCJRestricted(INITIALIZATION)
        @SCJAllowed(SUPPORT)
        public void initialize() {
            new MyPEH2().register();
        }

        @Override
        @SCJAllowed(SUPPORT)
        public long missionMemorySize() {
            return 0;
        }
    }


    @SCJAllowed(members=true)
    @Scope("MyApp")
    @DefineScope(name="MyPEH1", parent="MyApp")
    public static class MyPEH1 extends PeriodicEventHandler {

        @SCJRestricted(INITIALIZATION)
        public MyPEH1() {
            super(new PriorityParameters(13),
                    new PeriodicParameters(new RelativeTime(0, 0), new RelativeTime(
                            500, 0)),
                            new StorageParameters(1000L, null, 100, 1000));
        }

        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("MyPEH1")
        public void handleAsyncEvent() {
        }

        @Override
        @SCJRestricted(CLEANUP)
        @SCJAllowed(SUPPORT)
        public void cleanUp() {
        }

        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }

    @SCJAllowed(members = true)
    @Scope("MyApp")
    @DefineScope(name="MyPEH2", parent="MyApp")
    public static class MyPEH2 extends PeriodicEventHandler {

        @SCJRestricted(INITIALIZATION)
        public MyPEH2() {
            super(new PriorityParameters(13),
                  new PeriodicParameters(new RelativeTime(0, 0), new RelativeTime(
                            500, 0)),
                  new StorageParameters(1000L, null, 1000, 1000));
        }

        @Scope("MyApp")
        @DefineScope(name="MyPEH2", parent="MyApp")
        public ManagedMemory pri2;

        @Override
        @RunsIn("MyPEH2")
        @SCJAllowed(SUPPORT)
        public void handleAsyncEvent() {

            //## checkers.scope.ScopeChecker.ERR_BAD_VARIABLE_SCOPE
            @Scope("MyPEH1") Object obj;

            //## checkers.scope.ScopeChecker.ERR_BAD_VARIABLE_SCOPE
            @Scope("new-scope") Object obj2;
        }

        @Override
        @SCJAllowed(SUPPORT)
        @SCJRestricted(CLEANUP)
        public void cleanUp() {
        }

        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }

    @SCJAllowed(members = true)
    @DefineScope(name = "new-scope", parent = "MyPEH2")
    public static class MyRunnable implements Runnable {

        @RunsIn("new-scope")
        public void run() {
        }
    }

    @Override
    @SCJAllowed(SUPPORT)
    public long immortalMemorySize() {
        // TODO Auto-generated method stub
        return 0;
    }
}