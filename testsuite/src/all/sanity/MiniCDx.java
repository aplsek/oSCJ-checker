package all.sanity;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.THIS;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

import java.util.List;

import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members=true)
public class MiniCDx {


    @Scope(IMMORTAL)
    @SCJAllowed(members=true)
    @DefineScope(name="CDMission", parent=IMMORTAL)
    static public class CDMission extends CyclicExecutive {

        //static PriorityParameters p = new PriorityParameters(18);
        // static StorageParameters s = new StorageParameters(1000L, 1000L, 1000L);

        static int priorityParameter = 18;
        static long totalBackingStore = 1000L;
        static long nativeStackSize = 1000L;
        static long javaStackSize = 1000L;
        static RelativeTime t = new RelativeTime(5, 0);

        public CDMission() {
            super(new PriorityParameters(priorityParameter),
                  new StorageParameters(totalBackingStore, nativeStackSize, javaStackSize));
        }

        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("CDMission")
        public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
              return new CyclicSchedule(
                      new CyclicSchedule.Frame[] { new CyclicSchedule.Frame(t,
                              handlers) });
        }

        @Override
        @SCJRestricted(INITIALIZATION)
        @SCJAllowed(SUPPORT)
        @RunsIn("CDMission")
        protected void initialize() {
            new CDHandler();
            MIRun miRun = new MIRun();

            @Scope(IMMORTAL)
            @DefineScope(name="CDMission", parent=IMMORTAL)
            ManagedMemory m = ManagedMemory.getCurrentManagedMemory();
            m.enterPrivateMemory(2000, miRun);
        }

        /**
         * A method to query the maximum amount of memory needed by this mission.
         *
         * @return the amount of memory needed
         */
        @Override
        public long missionMemorySize() {
            return 5420;
        }

        @SCJRestricted(INITIALIZATION)
        @SCJAllowed(SUPPORT)
        public void setUp() {
        }

        @SCJRestricted(CLEANUP)
        @SCJAllowed(SUPPORT)
        public void tearDown() {
        }

        @Override
        @SCJRestricted(CLEANUP)
        @SCJAllowed(SUPPORT)
        public void cleanUp() {
        }
    }

    @SCJAllowed(members=true)
    @Scope("CDMission")
    @DefineScope(name="CDMissionInit", parent="CDMission")
    static class MIRun implements Runnable {
        @RunsIn("CDMissionInit")
        public void run() {
            // ...
        }
    }


    @SCJAllowed(members=true)
    @Scope("CDMission")
    @DefineScope(name="CDHandler", parent="CDMission")
    static public class CDHandler extends PeriodicEventHandler {
        StateTable st;
        boolean stop = false;

        static int priorityParameter = 13;
        static long totalBackingStore = 1000L;
        static long nativeStackSize = 1000L;
        static long javaStackSize = 1000L;
        static long periodicParameter = 500;

        /*
        static PriorityParameters pri;
        static PeriodicParameters per;
        static StorageParameters stor;

        static {
            pri = new PriorityParameters(13);
            per = new PeriodicParameters(new RelativeTime(0, 0), new RelativeTime(
                    500, 0));
            stor = new StorageParameters(1000L, 1000L, 1000L);
        }
        */

        @SCJRestricted(INITIALIZATION)
        public CDHandler() {
            super(new PriorityParameters(priorityParameter),
                    new PeriodicParameters(new RelativeTime(0, 0), new RelativeTime(
                            periodicParameter, 0)),
                    new StorageParameters(totalBackingStore, nativeStackSize, javaStackSize));
            st = new StateTable();
        }

        @Override
        @RunsIn("CDHandler")
        @SCJAllowed(SUPPORT)
        public void handleAsyncEvent() {
            // ...
            createMotions(null);
            if (stop)
                Mission.getCurrentMission().requestSequenceTermination();
        }

        @RunsIn("CDHandler")
        public List createMotions(Frame fr) {
            // for (Callsign cs : fr.getCallsigns()) {
            Callsign cs = null;
            @Scope("UNKNOWN") Vector3d old_pos = st.get(cs);
            if (old_pos == null) { // add new aircraft
                @Scope("CDMission") Callsign callsign = makeCallsign(cs);
                putCallSign(callsign);
            } else
                old_pos.update(); // update aircraft
            // }
            return null;
        }

        private final CallsignRunnable r = new CallsignRunnable();
        private final PutCallsignRunnable putRun = new PutCallsignRunnable();

        @RunsIn("CDHandler")
        public void putCallSign(@Scope("CDMission") Callsign callsign) {
            putRun.callsign = callsign;

            // ERROR: no @DefineScope!!
            ((ManagedMemory) ManagedMemory.getMemoryArea(st)).executeInArea(putRun);
        }

        @RunsIn("CDHandler") @Scope("CDMission")
        public Callsign makeCallsign(Callsign callsign) {
            try {
                @Scope(IMMORTAL)
                @DefineScope(name="CDMission", parent=IMMORTAL)
                ManagedMemory mem = (ManagedMemory) MemoryArea.getMemoryArea(r);
                r.cs = (byte[]) mem.newArrayInArea(r, byte.class,
                        callsign.cs.length);
                for (int i = 0; i < callsign.length; i++)
                    r.cs[i] = callsign.cs[i];
                ((ManagedMemory) ManagedMemory.getMemoryArea(st)).executeInArea(r);
                return r.result;
            } catch (IllegalAccessException e) {
                //TODO:
                // e.printStackTrace();  //ERROR : is not allowed by SCJ
            }
            return null;
        }

        @Override
        @SCJAllowed(SUPPORT)
        @SCJRestricted(CLEANUP)
        public void cleanUp() {
        }

        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }

        @Scope("CDMission")
        @SCJAllowed(members=true)
        class CallsignRunnable implements Runnable {
            byte[] cs;
            Callsign result;

            @RunsIn("CDMission")
            public void run() {
                result = new Callsign(cs);
            }
        }

        @Scope("CDMission")
        @SCJAllowed(members=true)
        class PutCallsignRunnable implements Runnable {
            Callsign callsign;

            @RunsIn("CDMission")
            public void run() {
                st.put(callsign);
            }
        }
    }


    @SCJAllowed(members=true)
    @Scope("CDMission")
    static public class StateTable {
        Vector3d[] allocatedVectors = new Vector3d[1000];
        int usedVectors = 0;

        final MyHashMap motionVectors = new MyHashMap();

        @RunsIn(CALLER) @Scope("CDMission")
        public Vector3d get(@Scope(UNKNOWN) Callsign cs) {   // TODO: should this be UNKNOWN??
            return (Vector3d) motionVectors.get(cs);
        }

        Callsign css = null;

        public void put(@Scope(UNKNOWN) final Callsign cs) {
            if (ManagedMemory.allocatedInSame(this, cs))
                css = cs; // DYNAMIC GUARD

            if (motionVectors.get(css) != null)
                return;
            Vector3d v = allocatedVectors[usedVectors++];
            motionVectors.put(css, v);
        }
    }


    @SCJAllowed(members=true)
    static public class MyHashMap {

        @RunsIn(CALLER) @Scope(THIS)
        public Object get(@Scope(UNKNOWN) Object key) {
            return null;
        }

        public void put(Object key, Object value) {
        }
    }


    @SCJAllowed(members=true)
    static public class Frame {
        public Object getCallsigns() {
            return null;
        }
    }


    @SCJAllowed(members=true)
    static public class Vector3d {
        @RunsIn(CALLER)
        public void update() {
        }
    }

    @SCJAllowed(members=true)
    static public class Callsign {
        public Callsign(byte[] cs2) {
        }

        byte[] cs;
        public int length;
    }

}
