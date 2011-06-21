package all.sanity;

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

@SCJAllowed(members = true)
@Scope(IMMORTAL)
public class IllegalStateEx implements Safelet {

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

        @Scope("MyApp")
        @DefineScope(name="MyPEH1", parent="MyApp")
        public ManagedMemory pri;

        @Scope("MyApp")
        @DefineScope(name="MyPEH2", parent="MyApp")
        public ManagedMemory pri2;

        @Override
        @SCJRestricted(INITIALIZATION)
        @SCJAllowed(SUPPORT)
        public void initialize() {
            new MyPEH1().register();
            new MyPEH2().register();
        }

        @Override
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
                            new StorageParameters(1000L, 1000L, 1000L));
        }

        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("MyPEH1")
        public void handleAsyncEvent() {
            MyMission m1 = (MyMission) Mission.getCurrentMission();
            m1.pri = (ManagedMemory) MemoryArea.getMemoryArea(new int[0]);
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
                  new StorageParameters(1000L, 1000L, 1000L));
        }

        @Scope("MyApp")
        @DefineScope(name="MyPEH2", parent="MyApp")
        public ManagedMemory pri2;

        @Override
        @RunsIn("MyPEH2")
        @SCJAllowed(SUPPORT)
        public void handleAsyncEvent() {
            try {
                MyRunnable run = new MyRunnable();

                MyMission m1 = (MyMission) Mission.getCurrentMission();

                //## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE_TYPE
                m1.pri.newInstance(List.class);     // ERR

                //## checkers.scope.ScopeChecker.ERR_BAD_VARIABLE_SCOPE
                @Scope("MyPEH1") Object obj;        // ERR

                //## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE_REPRESENTED_SCOPE
                obj = m1.pri.newInstance(Object.class);     // ERR

                m1.pri2.newInstance(Object.class);    // OK

                //## checkers.scope.ScopeChecker.ERR_BAD_ENTER_PRIVATE_MEMORY_TARGET
                m1.pri.enterPrivateMemory(500, run);        // ERR

                m1.pri2.enterPrivateMemory(500, run);       // OK

                pri2.enterPrivateMemory(500, run);      // OK

            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
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

}