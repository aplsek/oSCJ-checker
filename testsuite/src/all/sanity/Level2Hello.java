package all.sanity;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.ManagedThread;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.PriorityScheduler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members = true, value = LEVEL_2)
public class Level2Hello {

    @SCJAllowed(members = true, value = LEVEL_2)
    @Scope(IMMORTAL)
    @DefineScope(name = "PrimaryMission", parent = IMMORTAL)
    static public class MainMissionSequencer extends MissionSequencer {

        private boolean initialized, finalized;

        @SCJRestricted(INITIALIZATION)
        MainMissionSequencer(PriorityParameters priorityParameters,
                StorageParameters storageParameters) {
            super(priorityParameters, storageParameters);
            initialized = finalized = false;

        }

        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("PrimaryMission")
        protected Mission getNextMission() {
            if (finalized)
                return null;
            else if (initialized) {
                finalized = true;
                return new CleanupMission();
            } else {
                initialized = true;
                return new PrimaryMission();
            }
        }
    }

    static final private int PRIORITY = PriorityScheduler.instance()
            .getNormPriority();

    @SCJAllowed(members = true, value = LEVEL_2)
    @Scope("PrimaryMission")
    static public class PrimaryMission extends Mission {
        final int MISSION_MEMORY_SIZE = 10000;

        @Override
        @SCJRestricted(INITIALIZATION)
        @SCJAllowed(SUPPORT)
        public void initialize() {
            ManagedMemory.getCurrentManagedMemory().resize(MISSION_MEMORY_SIZE);
            PriorityParameters pp = new PriorityParameters(PRIORITY);
            StorageParameters sp = new StorageParameters(100000L, null, 1000, 1000);
            SubMissionSequencer sms = new SubMissionSequencer(pp, sp);
            // sms.register();
            (new PrimaryPeriodicEventHandler("AEH A", new RelativeTime(0, 0),
                    new RelativeTime(500, 0))).register();

            // (new PrimaryPeriodicEventHandler("AEH B", new RelativeTime(0, 0),
            // new RelativeTime(1000, 0))).register();
            // (new PrimaryPeriodicEventHandler("AEH C", new RelativeTime(500,
            // 0),
            // new RelativeTime(500, 0))).register();

        }

        @Override
        @SCJAllowed(SUPPORT)
        public long missionMemorySize() {
            return MISSION_MEMORY_SIZE;
        }

    }

    @SCJAllowed(members = true, value = LEVEL_2)
    @Scope("StageMission")
    static public class StageOneMission extends Mission {
        private static final int MISSION_MEMORY_SIZE = 10000;

        @Override
        @SCJRestricted(INITIALIZATION)
        @SCJAllowed(SUPPORT)
        public void initialize() {
            ManagedMemory.getCurrentManagedMemory().resize(MISSION_MEMORY_SIZE);
            (new SecondaryPeriodicEventHandler("stage1.eh1", new RelativeTime(
                    0, 0), new RelativeTime(1000, 0))).register();
        }

        @Override
        @SCJAllowed(SUPPORT)
        public long missionMemorySize() {
            return MISSION_MEMORY_SIZE;
        }
    }

    @SCJAllowed(members = true, value = LEVEL_2)
    @Scope("StageMission")
    static public  class StageTwoMission extends Mission {
        private static final int MISSION_MEMORY_SIZE = 10000;

        @Override
        @SCJRestricted(INITIALIZATION)
        @SCJAllowed(SUPPORT)
        public void initialize() {
            ManagedMemory.getCurrentManagedMemory().resize(MISSION_MEMORY_SIZE);
            (new SecondaryPeriodicEventHandler("stage2.eh1", new RelativeTime(
                    0, 0), new RelativeTime(500, 0))).register();
        }

        @Override
        @SCJAllowed(SUPPORT)
        public long missionMemorySize() {
            return MISSION_MEMORY_SIZE;
        }
    }

    @SCJAllowed(members = true, value = LEVEL_2)
    @Scope("PrimaryMission")
    @DefineScope(name = "StageMission", parent = "PrimaryMission")
    static public class SubMissionSequencer extends MissionSequencer {

        private boolean initialized, finalized;

        @SCJRestricted(INITIALIZATION)
        SubMissionSequencer(PriorityParameters priorityParameters,
                StorageParameters storageParameters) {
            super(priorityParameters, storageParameters);
            initialized = finalized = false;
        }

        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("StageMission")
        protected Mission getNextMission() {
            if (finalized)
                return null;
            else if (initialized) {
                finalized = true;
                return new StageTwoMission();
            } else {
                initialized = true;
                return new StageOneMission();
            }
        }
    }

    @SCJAllowed(members = true, value = LEVEL_2)
    @Scope("PrimaryMission")
    @DefineScope(name = "PrimaryPeriodicEventHandler", parent = "PrimaryMission")
    static public class PrimaryPeriodicEventHandler extends PeriodicEventHandler {
        private static final int _priority = 17;
        private static final int _memSize = 5000;
        private int _eventCounter;

        @SCJRestricted(INITIALIZATION)
        public PrimaryPeriodicEventHandler(String aehName,
                RelativeTime startTime, RelativeTime period) {
            super(new PriorityParameters(_priority), new PeriodicParameters(
                    startTime, period), new StorageParameters(10000, null, 10000,
                    10000));
        }

        @Override
        @RunsIn("PrimaryPeriodicEventHandler")
        @SCJAllowed(SUPPORT)
        public void handleAsyncEvent() {
            ++_eventCounter;
        }

        @Override
        @SCJRestricted(CLEANUP)
        @SCJAllowed(SUPPORT)
        public void cleanUp() {
        }
    }

    @SCJAllowed(members = true, value = LEVEL_2)
    @Scope("StageMission")
    @DefineScope(name = "SecondaryPeriodicEventHandler", parent = "StageMission")
    static public class SecondaryPeriodicEventHandler extends PeriodicEventHandler {
        private static final int _priority = 17;
        private static final int _memSize = 5000;
        private int _eventCounter;

        @SCJRestricted(INITIALIZATION)
        public SecondaryPeriodicEventHandler(String aehName,
                RelativeTime startTime, RelativeTime period) {
            super(new PriorityParameters(_priority), new PeriodicParameters(
                    startTime, period), new StorageParameters(10000, null, 10000,
                    10000));
        }

        @Override
        @RunsIn("SecondaryPeriodicEventHandler")
        @SCJAllowed(SUPPORT)
        public void handleAsyncEvent() {
            ++_eventCounter;
        }

        @Override
        @SCJRestricted(CLEANUP)
        @SCJAllowed(SUPPORT)
        public void cleanUp() {
        }
    }

    @SCJAllowed(members = true, value = LEVEL_2)
    @Scope("PrimaryMission")
    static public class CleanupMission extends Mission {
        static final private int MISSION_MEMORY_SIZE = 10000;

        @Override
        @SCJRestricted(INITIALIZATION)
        @SCJAllowed(SUPPORT)
        public void initialize() {
            ManagedMemory.getCurrentManagedMemory().resize(MISSION_MEMORY_SIZE);
            PriorityParameters pp = new PriorityParameters(PRIORITY);
            StorageParameters sp = new StorageParameters(100000L, null, 1000, 1000);
            MyCleanupThread t = new MyCleanupThread(pp, sp);
        }

        @Override
        @SCJAllowed(SUPPORT)
        public long missionMemorySize() {
            return MISSION_MEMORY_SIZE;
        }
    }

    @SCJAllowed(members = true, value = LEVEL_2)
    @Scope("PrimaryMission")
    @DefineScope(name = "MyCleanupThread", parent = "PrimaryMission")
    static public class MyCleanupThread extends ManagedThread {

        @SCJRestricted(INITIALIZATION)
        public MyCleanupThread(PriorityParameters priority,
                StorageParameters storage) {
            super(priority, storage);
        }

        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("MyCleanupThread")
        public void run() {
            cleanupThis();
            cleanupThat();
        }

        @SCJAllowed
        @RunsIn("MyCleanupThread")
        void cleanupThis() {
            // code not shown
        }

        @SCJAllowed
        @RunsIn("MyCleanupThread")
        void cleanupThat() {
            // code not shown
        }

    }
}
