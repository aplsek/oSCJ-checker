package all.sanity;


import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.THIS;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.realtime.AbsoluteTime;
import javax.realtime.Clock;
import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.Safelet;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members=true)
public class MiniCDxBig {

}




@SCJAllowed(members=true)
@Scope("cdx.CollisionDetectorHandler")
class Aircraft implements Comparable {
    /** The callsign. Currently, the only data we hold. */
    private final byte[] callsign;

    /** Construct with a callsign. */
    public Aircraft(final byte[] _callsign) {
        callsign = _callsign;
    }

    /** Construct a copy of an aircraft. */
    public Aircraft(final Aircraft _aircraft) {
        this(_aircraft.getCallsign());
    }

    /** Gives you the callsign. */
    public byte[] getCallsign() {
        return callsign;
    }

    /** Returns a valid hash code for this object. */
    @Override
    public int hashCode() {
        int h = 0;

        for (int i = 0; i < callsign.length; i++) {
            h += callsign[i];
        }

        return h;
    }

    /** Performs a comparison between this object and another. */
    /*
    public boolean equals(final Object other) {
    	if (other == this) return true;
    	else if (other instanceof Aircraft) {
    		final byte[] cs = ((Aircraft) other).callsign;
    		if (cs.length != callsign.length) return false;
    		for (int i = 0; i < cs.length; i++)
    			if (cs[i] != callsign[i]) return false;
    		return true;
    	} else return false;
    }
     */

    /** Performs a comparison between this object and another. */
    @Override
    @RunsIn(CALLER)
    public boolean equals(final Object other) {
        // my suspicion is that this is the problem
        if (other == this) return true;
        else if (other instanceof Aircraft) {
            return (((Aircraft) other).callsign[5] == callsign[5]);
            /*final byte[] cs = ((Aircraft) other).callsign;
            System.out.println(cs.length);
            if (cs.length != callsign.length) return false;
            for (int i = 0; i < cs.length; i++)
            	if (cs[i] != callsign[i]) return false;
            return true; */
        } else return false;
    }

    /** Performs comparison with ordering taken into account. */
    public int compareTo(final Object _other) throws ClassCastException {
        final byte[] cs = ((Aircraft) _other).callsign;
        if (cs.length < callsign.length) return -1;
        if (cs.length > callsign.length) return +1;
        for (int i = 0; i < cs.length; i++)
            if (cs[i] < callsign[i]) return -1;
            else if (cs[i] > callsign[i]) return +1;
        return 0;
    }

    /** Returns a helpful description of this object. */
    @Override
    public String toString() {
        // return new String(callsign, 0, callsign.length);
        return ASCIIConverter.bytesToString(callsign);
    }
}



@SCJAllowed(members=true)
class ASCIIConverter {
    public static String bytesToString(byte[] bytes) {

        char[] chars = new char[bytes.length];

        for (int i = 0; i < bytes.length; i++) {
            chars[i] = (char) bytes[i];
        }

        return new String(chars);
    }

    public static String floatToString(float f) {

        Float flt = new Float(f);
        long intPart = (long) Math.ceil(f);
        long fewDigits = (long) ((f - intPart) * 1000);

        return "" + intPart + "." + fewDigits;
    }
}



@SCJAllowed(members=true)
////@Scope("cdx.Level0Safelet")
class CallSign {

    final private byte[] val;

    public CallSign(final byte[] v) {
        val = v;
    }

    /** Returns a valid hash code for this object. */
    @Override
    public int hashCode() {
        int h = 0;
        for (int i = 0; i < val.length; i++) {
            h += val[i];
        }
        return h;
    }

    /** Performs a comparison between this object and another. */
    @Override
    @RunsIn(CALLER)
    public boolean equals(final Object other) {
        if (other == this) return true;
        else if (other instanceof CallSign) {
            final byte[] cs = ((CallSign) other).val;
            if (cs.length != val.length) return false;
            for (int i = 0; i < cs.length; i++)
                if (cs[i] != val[i]) return false;
            return true;
        } else return false;
    }

    /** Performs comparison with ordering taken into account. */
    public int compareTo(final Object _other) throws ClassCastException {
        final byte[] cs = ((CallSign) _other).val;
        if (cs.length < val.length) return -1;
        if (cs.length > val.length) return +1;
        for (int i = 0; i < cs.length; i++)
            if (cs[i] < val[i]) return -1;
            else if (cs[i] > val[i]) return +1;
        return 0;
    }
}

@SCJAllowed(members=true)
@Scope("cdx.CollisionDetectorHandler")
class Collision {
    /** The aircraft that were involved. */
    // private ArrayList aircraft;

    private Aircraft _one, _two;

    /** The location where the collision happened. */
    private Vector3d location;

    /** Construct a Collision with a given set of aircraft and a location. */
    /*	public Collision(List aircraft, Vector3d location) {
    		this.aircraft = new ArrayList(aircraft);
    		Collections.sort(this.aircraft);
    		this.location = location;
    	} */

    /** Construct a Coollision with two aircraft an a location. */
    public Collision(Aircraft one, Aircraft two, Vector3d location) {
        /*	aircraft = new ArrayList();
        	aircraft.add(one);
        	aircraft.add(two);
        	Collections.sort(aircraft); */
        this.location = location;
        _one = one;
        _two = two;
    }

    public boolean hasAircraft(Aircraft a) {
        if (_one.equals(a)) return true;
        if (_two.equals(a)) return true;
        return false;
    }

    public Aircraft first() {
        return _one;
    }

    public Aircraft second() {
        return _two;
    }

    /*public int aircrafts() {
      return aircraft.size();
    } */

    /** Returns the list of aircraft involved. You are not to modify this list. */
    // public ArrayList getAircraftInvolved() { return aircraft; }

    /** Returns the location of the collision. You are not to modify this location. */
    public Vector3d getLocation() {
        return location;
    }

    /** Returns a hash code for this object. It is based on the hash codes of the aircraft. */

    @Override
    public int hashCode() {
        int ret = 0;
        /*for (Iterator iter = aircraft.iterator(); iter.hasNext();)
        	ret += ((Aircraft) iter.next()).hashCode(); */
        ret += _one.hashCode();
        ret += _two.hashCode();
        return ret;
    }

    /** Determines collision equality. Two collisions are equal if they have the same aircraft. */

    @Override
    @RunsIn(CALLER)
    public boolean equals(Object _other) {
        if (_other == this) return true;
        if (!(_other instanceof Collision)) return false;
        Collision other = (Collision) _other;
        /*ArrayList a = getAircraftInvolved();
        ArrayList b = other.getAircraftInvolved();
        if (a.size() != b.size()) return false;
        Iterator ai = a.iterator();
        Iterator bi = b.iterator();
        while (ai.hasNext())
        	if (!ai.next().equals(bi.next())) return false; */
        // if ((other.hasAircraft(_one)) && (other.hasAircraft(_two))) return true;
        if (_one != other._one) return false;
        if (_two != other._two) return false;
        return true;
    }

    /** Returns a helpful description of this object. */

    @Override
    public String toString() {
        //StringBuffer buf = new StringBuffer("Collision between ");
        //boolean first = true;
        /*for (Iterator iter = getAircraftInvolved().iterator(); iter.hasNext();) {
        	if (first) first = false;
        	else buf.append(", ");
        	buf.append(iter.next().toString());
        }  */

       // buf.append(_one.toString() + ", " + _two.toString());
       // buf.append(" at ");
       // buf.append(location.toString());
       //
       // return buf.toString();
        return null;
    }
}



@SCJAllowed(members=true)
@Scope("cdx.CollisionDetectorHandler")
class CollisionCollector {
    /** A hash set of collisions. */
    private HashSet collisions = new HashSet();

    /** Add some collisions. */
    public void addCollisions(List collisions) {
    // this.collisions.addAll(collisions);
    }

    /** Get the list of collisions. */
    public ArrayList getCollisions() {
        return new ArrayList(collisions);
    }
}

@SCJAllowed(members=true)
@Scope("cdx.Level0Safelet")
@DefineScope(name="cdx.CollisionDetectorHandler",parent="cdx.Level0Safelet")
class CollisionDetectorHandler extends PeriodicEventHandler {
    private final TransientDetectorScopeEntry cd = new TransientDetectorScopeEntry(
            new StateTable(), Constants.GOOD_VOXEL_SIZE);
    public final NoiseGenerator noiseGenerator = new NoiseGenerator();

    public boolean stop = false;

    @SCJRestricted(INITIALIZATION)
    public CollisionDetectorHandler() {

        // these very large limits are reported to work with stack traces... of
        // errors encountered...
        // most likely they are unnecessarily large
        super(null, null, new StorageParameters(Constants.TRANSIENT_DETECTOR_SCOPE_SIZE,0,0));
    }

    @RunsIn("cdx.CollisionDetectorHandler")
    public void runDetectorInScope(final TransientDetectorScopeEntry cd) {

        final RawFrame f = ImmortalEntry.frameBuffer.getFrame();
        if (f == null) {
            ImmortalEntry.frameNotReadyCount++;
            System.out.println("Frame not ready");
            return;
        }

        if ((ImmortalEntry.framesProcessed + ImmortalEntry.droppedFrames) == Constants.MAX_FRAMES) {
            stop = true;
            return;
        } // should not be needed, anyway

        //noiseGenerator.generateNoiseIfEnabled();
        cd.setFrame(f);

        final long timeBefore = NanoClock.now();
        cd.run();
        final long timeAfter = NanoClock.now();
        //final long heapFreeAfter = Runtime.getRuntime().freeMemory();

        if (ImmortalEntry.recordedRuns < ImmortalEntry.maxDetectorRuns) {
            ImmortalEntry.timesBefore[ImmortalEntry.recordedRuns] = timeBefore;
            ImmortalEntry.timesAfter[ImmortalEntry.recordedRuns] = timeAfter;
            //ImmortalEntry.heapFreeBefore[ImmortalEntry.recordedRuns] = heapFreeBefore;
            //ImmortalEntry.heapFreeAfter[ImmortalEntry.recordedRuns] = heapFreeAfter;
            ImmortalEntry.recordedRuns++;
        }

        ImmortalEntry.framesProcessed++;

        if ((ImmortalEntry.framesProcessed + ImmortalEntry.droppedFrames) == Constants.MAX_FRAMES)
            stop = true;
    }

    @Override
    @SCJAllowed(SUPPORT)
    @RunsIn("cdx.CollisionDetectorHandler")
    public void handleAsyncEvent() {

        //BenchMem.setMemUsage(RealtimeThread.getCurrentMemoryArea().memoryConsumed());

        try {
            if (!stop) {
                long now = NanoClock.now();
                ImmortalEntry.detectorReleaseTimes[ImmortalEntry.recordedDetectorReleaseTimes] = now;
                ImmortalEntry.detectorReportedMiss[ImmortalEntry.recordedDetectorReleaseTimes] = false;
                ImmortalEntry.recordedDetectorReleaseTimes++;

                runDetectorInScope(cd);
            } else {
                Mission.getCurrentMission().requestSequenceTermination();
            }
        } catch (Throwable e) {
            System.out.println("Exception thrown by runDetectorInScope: "
                    + e.getMessage());
           // e.printStackTrace();
        }

       // BenchMem.setMemUsage(RealtimeThread.getCurrentMemoryArea().memoryConsumed());
    }


    @Override
    @SCJAllowed(SUPPORT)
    @SCJRestricted(CLEANUP)
    public void cleanUp() {
    }
}

@SCJAllowed(members=true)
final class Constants {
    // I have added this so that we can specify the number of planes at runtime
    public static int           NUMBER_OF_PLANES                         = 6;

    public static final float   MIN_X                                    = 0.0f;
    public static final float   MIN_Y                                    = 0.0f;
    public static final float   MAX_X                                    = 1000.0f;
    public static final float   MAX_Y                                    = 1000.0f;
    public static final float   MIN_Z                                    = 0.0f;
    public static final float   MAX_Z                                    = 10.0f;
    public static final float   PROXIMITY_RADIUS                         = 1.0f;
    public static final float   GOOD_VOXEL_SIZE                          = PROXIMITY_RADIUS * 10.0f;

    public static int           SIMULATOR_PRIORITY                       = 5;
    public static int           SIMULATOR_TIME_SCALE                     = 1;
    public static int           SIMULATOR_FPS                            = 50;
    public static int           DETECTOR_STARTUP_PRIORITY                = 9;
    public static int           DETECTOR_PRIORITY                        = 9;                                       // DETECTOR_STARTUP_PRIORITY
                                                                                                                     // +
                                                                                                                     // 1;
    public static long          PERSISTENT_DETECTOR_SCOPE_SIZE           = 5*1000*1000; //260*1000; // v 245*1000   250     //5*1000*1000;
    public static long          DETECTOR_PERIOD                          = 50;
    public static long          TRANSIENT_DETECTOR_SCOPE_SIZE            = 5*1000*1000;  //63*1000; //63  50     //5*1000*1000;

    public static int           MAX_FRAMES                               = 1000;

    public static int           TIME_SCALE                               = 1;
    public static int           FPS                                      = 50;
    public static int           BUFFER_FRAMES                            = 10;
    public static boolean       PRESIMULATE                              = false;
    public static boolean       SIMULATE_ONLY                            = false;

    public static final String  DETECTOR_STATS                           = "detector.rin";
    public static final String  SIMULATOR_STATS                          = "simulator.rin";
    public static final String  DETECTOR_RELEASE_STATS                   = "release.rin";
    public static final boolean PRINT_RESULTS                            = true;

    // run a SPEC jvm98 benchmark to generate some noise
    public static String        SPEC_NOISE_ARGS                          = "-a -b -g -s100 -m10 -M10 -t _213_javac";
    public static boolean       USE_SPEC_NOISE                           = false;

    public static int           DETECTOR_NOISE_REACHABLE_POINTERS        = 1000000;
    public static int           DETECTOR_NOISE_ALLOCATE_POINTERS         = 10000;
    public static int           DETECTOR_NOISE_ALLOCATION_SIZE           = 64;
    public static boolean       DETECTOR_NOISE_VARIABLE_ALLOCATION_SIZE  = false;
    public static int           DETECTOR_NOISE_ALLOCATION_SIZE_INCREMENT = 13;
    public static int           DETECTOR_NOISE_MIN_ALLOCATION_SIZE       = 128;
    public static int           DETECTOR_NOISE_MAX_ALLOCATION_SIZE       = 16384;
    public static int           DETECTOR_STARTUP_OFFSET_MILLIS           = 3000;
    public static boolean       DETECTOR_NOISE                           = false;

    // write down the FRAMES into the frame.bin file
    public static boolean       FRAMES_BINARY_DUMP                       = false;

    // this is only for debugging of the detector code
    //
    // each frame generated by the simulator is processed exactly once by
    // the detector ; this also turns on some debugging features
    //
    // the results thus should be deterministic
    public static boolean       SYNCHRONOUS_DETECTOR                     = false;

    public static boolean       DUMP_RECEIVED_FRAMES                     = false;
    public static boolean       DUMP_SENT_FRAMES                         = false;
    public static boolean       DEBUG_DETECTOR                           = false;

    public static boolean FRAME_ON_THE_GO = true;

}


@SCJAllowed(members=true)
@Scope(IMMORTAL)
class ImmortalEntry implements Runnable {

    static public Object           initMonitor                  = new Object();
    static public boolean          detectorReady                = false;
    static public boolean          simulatorReady               = false;

    static public int              maxDetectorRuns;

    static public long             detectorFirstRelease         = -1;

    static public long[]           timesBefore;
    static public long[]           timesAfter;
    static public long[]           heapFreeBefore;
    static public long[]           heapFreeAfter;
    static public int[]            detectedCollisions;
    static public int[]            suspectedCollisions;

    static public long             detectorThreadStart;
    static public long[]           detectorReleaseTimes;
    static public boolean[]        detectorReportedMiss;

    static public int              reportedMissedPeriods        = 0;
    static public int              frameNotReadyCount           = 0;
    static public int              droppedFrames                = 0;
    static public int              framesProcessed              = 0;
    static public int              recordedRuns                 = 0;

    static public int              recordedDetectorReleaseTimes = 0;

    static public FrameBuffer      frameBuffer                  = null;

    static public DataOutputStream binaryDumpStream             = null;

    public ImmortalEntry() {
        // super(new PriorityParameters(Constants.DETECTOR_STARTUP_PRIORITY));

        maxDetectorRuns = Constants.MAX_FRAMES;

        timesBefore = new long[maxDetectorRuns];
        timesAfter = new long[maxDetectorRuns];
        heapFreeBefore = new long[maxDetectorRuns];
        heapFreeAfter = new long[maxDetectorRuns];
        detectedCollisions = new int[maxDetectorRuns];
        suspectedCollisions = new int[maxDetectorRuns];

        detectorReleaseTimes = new long[maxDetectorRuns + 10]; // the 10 is for missed deadlines
        detectorReportedMiss = new boolean[maxDetectorRuns + 10];
    }

    /** Called only once during initialization. Runs in immortal memory */
    public void run() {

        System.out.println("immortal entry....?.");
        System.out.println("Detector: detector priority is " + Constants.DETECTOR_PRIORITY);
        System.out.println("Detector: detector period is " + Constants.DETECTOR_PERIOD);

        frameBuffer = new FrameBufferPLDI();

        //frameBuffer = new WorkloadStar();


        /* start the detector at rounded-up time, so that the measurements are not subject
         * to phase shift
         */
    }
}


@SCJAllowed(members=true)
@Scope(IMMORTAL)
@DefineScope(name="cdx.Level0Safelet",parent=IMMORTAL)
class Level0Safelet extends CyclicExecutive {

    public Level0Safelet() {
        super(null);
    }

    private static long memSetup;
    private static long memSetupEnd ;

    @SCJAllowed(SUPPORT)
    @SCJRestricted(INITIALIZATION)
    public void setUp() {

        ////////////////////////////////
        //  MEMORY BENCHMARK INIT
        //BenchMem.init();

        Constants.PRESIMULATE = true;
        new ImmortalEntry().run();
        new Simulator().generate();
    }

    @SCJAllowed(SUPPORT)
    @SCJRestricted(CLEANUP)
    public void tearDown() {
        dumpResults();
    }

    @Override
    @SCJAllowed(SUPPORT)
    @RunsIn("cdx.Level0Safelet")
    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
        CyclicSchedule.Frame[] frames = new CyclicSchedule.Frame[1];
        frames[0] = new CyclicSchedule.Frame(new RelativeTime(Constants.DETECTOR_PERIOD, 0), handlers);
        CyclicSchedule schedule = new CyclicSchedule(frames);
        return schedule;
    }

    @Override
    @RunsIn("cdx.Level0Safelet")
    @SCJAllowed(SUPPORT)
    protected void initialize() {
        try {
            ImmortalEntry.detectorThreadStart = NanoClock.now();
            AbsoluteTime releaseAt = NanoClock.roundUp(Clock.getRealtimeClock().getTime().add(
                Constants.DETECTOR_STARTUP_OFFSET_MILLIS, 0));
            ImmortalEntry.detectorFirstRelease = NanoClock.convert(releaseAt);

            new CollisionDetectorHandler();

            if (Constants.DEBUG_DETECTOR) {
              //  System.out.println("Detector thread is " + Thread.currentThread());
              //  System.out
              //      .println("Entering detector loop, detector thread priority is "
              //              + +Thread.currentThread().getPriority() + " (NORM_PRIORITY is " + Thread.NORM_PRIORITY
              //              + ", MIN_PRIORITY is " + Thread.MIN_PRIORITY + ", MAX_PRIORITY is " + Thread.MAX_PRIORITY
              //              + ")");
            }

        } catch (Throwable e) {
            System.out.println("e: " + e.getMessage());
            //e.printStackTrace();
        }
    }


    @Override
    public long missionMemorySize() {
        return Constants.PERSISTENT_DETECTOR_SCOPE_SIZE;
    }


    public static void dumpResults() {
        /*
        String space = " ";
        String triZero = " 0 0 0 ";

        if (Constants.PRINT_RESULTS) {
            System.out
                .println("Dumping output [ timeBefore timeAfter heapFreeBefore heapFreeAfter detectedCollisions ] for "
                        + ImmortalEntry.recordedRuns + " recorded detector runs, in ns");
        }
        System.out.println("=====DETECTOR-STATS-START-BELOW====");
        for (int i = 0; i < ImmortalEntry.recordedRuns; i++) {
            System.out.print(ImmortalEntry.timesBefore[i]);
            System.out.print(space);
            System.out.print(ImmortalEntry.timesAfter[i]);
            System.out.print(space);
            System.out.print(ImmortalEntry.detectedCollisions[i]);
            System.out.print(space);
            System.out.print(ImmortalEntry.suspectedCollisions[i]);
            System.out.print(triZero);
            System.out.println(i);
        }

        System.out.println("=====DETECTOR-STATS-END-ABOVE====");

        System.out.println("Generated frames: " + Constants.MAX_FRAMES);
        System.out.println("Received (and measured) frames: " + ImmortalEntry.recordedRuns);
        System.out.println("Frame not ready event count (in detector): " + ImmortalEntry.frameNotReadyCount);
        System.out.println("Frames dropped due to full buffer in detector: " + ImmortalEntry.droppedFrames);
        System.out.println("Frames processed by detector: " + ImmortalEntry.framesProcessed);
        // System.out.println("Detector stop indicator set: "
        // + ImmortalEntry.persistentDetectorScopeEntry.stop);
        System.out.println("Reported missed detector periods (reported by waitForNextPeriod): "
                + ImmortalEntry.reportedMissedPeriods);
        System.out.println("Detector first release was scheduled for: "
                + NanoClock.asString(ImmortalEntry.detectorFirstRelease));
        // heap measurements
        Simulator.dumpStats();

        // detector release times
        if (Constants.DETECTOR_RELEASE_STATS != "") {
            System.out.println("=====DETECTOR-RELEASE-STATS-START-BELOW====");
            for (int i = 0; i < ImmortalEntry.recordedDetectorReleaseTimes; i++) {
                System.out.print(ImmortalEntry.detectorReleaseTimes[i]);
                System.out.print(space);
                System.out.print(i * Constants.DETECTOR_PERIOD * 1000000L + ImmortalEntry.detectorReleaseTimes[0]);
                System.out.print(space);
                System.out.print(ImmortalEntry.detectorReportedMiss[i] ? 1 : 0);
                System.out.print(space);
                System.out.println(i);
            }
            System.out.println("=====DETECTOR-RELEASE-STATS-END-ABOVE====");
        }
        */
        //BenchMem.dumpMemoryUsage();
    }
}



@SCJAllowed(members=true)
@Scope("cdx.CollisionDetectorHandler")
class Motion {
    /** The aircraft that we are referring to. */
    private final Aircraft aircraft;
    /**
     * The first position (from the last frame). Will be equal to <code>pos_two</code> if we do not have a record for
     * this aircraft for any previous frames.
     */
    private final Vector3d pos_one;
    /** The second position (from the current frame). */
    private final Vector3d pos_two;

    /** Initialize with an aircraft and two positions. */
    public Motion(final Aircraft _aircraft, final Vector3d _pos_one, final Vector3d _pos_two) {
        aircraft = _aircraft;
        pos_one = _pos_one;
        pos_two = _pos_two;
    }

    /** Initialize with an aircraft and one position. */
    public Motion(final Aircraft _aircraft, final Vector3d _pos) {
        this(_aircraft, _pos, _pos);
    }

    /** Retrieve the aircraft. */
    public Aircraft getAircraft() {
        return aircraft;
    }

    /** Retrieve position #1. */
    public Vector3d getFirstPosition() {
        return pos_one;
    }

    /** Retrieve position #2. */
    public Vector3d getSecondPosition() {
        return pos_two;
    }

    @Override
    public String toString() {
        return "MOTION of " + getAircraft().toString() + " from " + getFirstPosition().toString() + " to "
                + getSecondPosition().toString();
    }

    /**
     * Find an intersection between this Motion and another.
     *
     * @return a Vector3d object with the intersection point if an intersection was found, null otherwise.
     * @author Jeff Hagelberg, Filip Pizlo
     */

    // see the code for checking the (strange) semantics of the returned intersection

    public Vector3d findIntersection(final Motion other) {
        final Vector3d i1 = new Vector3d(), f1 = new Vector3d(), i2 = new Vector3d(), f2 = new Vector3d();
        i1.set(getFirstPosition());
        f1.set(getSecondPosition());
        i2.set(other.getFirstPosition());
        f2.set(other.getSecondPosition());
        float r = Constants.PROXIMITY_RADIUS;
        final float vx1 = f1.x - i1.x;
        final float vx2 = f2.x - i2.x;
        final float vy1 = f1.y - i1.y;
        final float vy2 = f2.y - i2.y;
        final float vz1 = f1.z - i1.z;
        final float vz2 = f2.z - i2.z;

        // this test is not geometrical 3-d intersection test, it takes the fact that the aircraft move
        // into account ; so it is more like a 4d test
        // (it assumes that both of the aircraft have a constant speed over the tested interval)

        // we thus have two points, each of them moving on its line segment at constant speed ; we are looking
        // for times when the distance between these two points is smaller than r

        // V1 is vector of aircraft 1
        // V2 is vector of aircraft 2

        // if a = 0 iff the planes are moving in parallel and have the same speed (can be zero - they may not be moving
        // at all)

        // a = (V2 - V1)^T * (V2 - V1) = < (V2 - V1), (V2 - V1) > = sqrt( || V2 - V1 || )
        final float a = (vx2 - vx1) * (vx2 - vx1) + (vy2 - vy1) * (vy2 - vy1) + (vz2 - vz1) * (vz2 - vz1);

        if (a != 0.0f) {

            // we are first looking for instances of time when the planes are exactly r from each other
            // at least one plane is moving ; if the planes are moving in parallel, they do not have constant speed

            // if the planes are moving in parallel, then
            // if the faster starts behind the slower, we can have 2, 1, or 0 solutions
            // if the faster plane starts in front of the slower, we can have 0 or 1 solutions

            // if the planes are not moving in parallel, then

            // point P1 = I1 + vV1
            // point P2 = I2 + vV2
            // - looking for v, such that dist(P1,P2) = || P1 - P2 || = r

            // it follows that || P1 - P2 || = sqrt( < P1-P2, P1-P2 > )
            // 0 = -r^2 + < P1 - P2, P1 - P2 >
            // from properties of dot product
            // 0 = -r^2 + <I1-I2,I1-I2> + v * 2<I1-I2, V1-V2> + v^2 *<V1-V2,V1-V2>
            // so we calculate a, b, c - and solve the quadratic equation
            // 0 = c + bv + av^2

            // b = 2 * <I1-I2, V1-V2>
            float b = 2.0f * (i2.x * vx2 - i2.x * vx1 - i1.x * vx2 + i1.x * vx1 + i2.y * vy2 - i2.y * vy1 - i1.y * vy2
                    + i1.y * vy1 + i2.z * vz2 - i2.z * vz1 - i1.z * vz2 + i1.z * vz1);

            // c = -r^2 + (I2 - I1)^T * (I2 - I1)
            final float c = -r * r + (i2.x - i1.x) * (i2.x - i1.x) + (i2.y - i1.y) * (i2.y - i1.y) + (i2.z - i1.z)
                    * (i2.z - i1.z);

            final float discr = b * b - 4.0f * a * c;
            if (discr < 0.0f) return null;

            // the left side
            final float v1 = (-b - (float) Math.sqrt(discr)) / (2.0f * a);
            // the right side
            final float v2 = (-b + (float) Math.sqrt(discr)) / (2.0f * a);

            // FIXME: v1 <= v2 always holds, correct ?
            // .. because v1 > v2 only if a < 0, which would mean <V1-V2,V1-V2> < 0, which is impossible

            if (v1 <= v2 && (v1 <= 1.0f && 1.0f <= v2 || v1 <= 0.0f && 0.0f <= v2 || 0.0f <= v1 && v2 <= 1.0f)) {
                // new: calculate the location of the collision; if it is
                // outside of the bounds of the Simulation, don't do anything!
                final float x1col = i1.x + vx1 * (v1 + v2) / 2.0f;
                final float y1col = i1.y + vy1 * (v1 + v2) / 2.0f;
                final float z1col = i1.z + vz1 * (v1 + v2) / 2.0f;
                if (z1col > Constants.MIN_Z && z1col <= Constants.MAX_Z && x1col >= Constants.MIN_X
                        && x1col <= Constants.MAX_X && y1col >= Constants.MIN_Y && y1col <= Constants.MAX_Y) return new Vector3d(
                    x1col, y1col, z1col);
            }
        } else {

            // the planes have the same speeds and are moving in parallel (or they are not moving at all)
            // they thus have the same distance all the time ; we calculate it from the initial point

            // dist = || i2 - i1 || = sqrt( ( i2 - i1 )^T * ( i2 - i1 ) )

            float dist = (i2.x - i1.x) * (i2.x - i1.x) + (i2.y - i1.y) * (i2.y - i1.y) + (i2.z - i1.z) * (i2.z - i1.z);
            dist = (float) Math.sqrt(dist);

            // System.out.println("i1 = "+i1+", i2 = "+i2+", dist = "+dist);
            if (dist <= r)
            // System.out.println("Planes were travelling in parallel. Collision.");
            return getFirstPosition();
        }
        return null;
    }
}


@SCJAllowed(members=true)
@Scope("cdx.Level0Safelet")
class NoiseGenerator {

    private Object[] noiseRoot;
    private int      noisePtr;

    public NoiseGenerator() {
        if (Constants.DETECTOR_NOISE) {
            noiseRoot = new Object[Constants.DETECTOR_NOISE_REACHABLE_POINTERS];
            noisePtr = 0;
        }
    }

    private void generateNoise() {
        for (int i = 0; i < Constants.DETECTOR_NOISE_ALLOCATE_POINTERS; i++) {
            noiseRoot[(noisePtr++) % noiseRoot.length] = new byte[Constants.DETECTOR_NOISE_ALLOCATION_SIZE];
        }

    }

    private void generateNoiseWithVariableObjectSize() {

        int currentIncrement = 0;
        int maxIncrement = Constants.DETECTOR_NOISE_MAX_ALLOCATION_SIZE - Constants.DETECTOR_NOISE_MIN_ALLOCATION_SIZE;

        for (int i = 0; i < Constants.DETECTOR_NOISE_ALLOCATE_POINTERS; i++) {
            noiseRoot[(noisePtr++) % noiseRoot.length] = new byte[Constants.DETECTOR_NOISE_MIN_ALLOCATION_SIZE
                    + (currentIncrement % maxIncrement)];
            currentIncrement += Constants.DETECTOR_NOISE_ALLOCATION_SIZE_INCREMENT;
        }
    }

    public void generateNoiseIfEnabled() {
        if (Constants.DETECTOR_NOISE) {

            if (Constants.DETECTOR_NOISE_VARIABLE_ALLOCATION_SIZE) {
                generateNoiseWithVariableObjectSize();
            } else {
                generateNoise();
            }
        }
    }
}



@Scope(IMMORTAL)
@SCJAllowed(members=true)
class RawFrame {
    static private int   MAX_PLANES = 1000;
    static private int   MAX_SIGNS  = 10 * MAX_PLANES;

    public final int[]   lengths    = new int[MAX_PLANES];
    public final byte[]  callsigns  = new byte[MAX_SIGNS];
    public final float[] positions  = new float[3 * MAX_PLANES];
    public int           planeCnt;

    @SCJRestricted(mayAllocate=false)
    //@RunsIn("cdx.CollisionDetectorHandler")
    @RunsIn(CALLER)
    public void copy(@Scope(UNKNOWN) final int[] lengths_,@Scope(UNKNOWN) final byte[] signs_,@Scope(UNKNOWN) final float[] positions_) {
        for (int i = 0, pos = 0, pos2 = 0, pos3 = 0, pos4 = 0; i < lengths_.length; i++) {
            lengths[pos++] = lengths_[i];
            positions[pos2++] = positions_[3 * i];
            positions[pos2++] = positions_[3 * i + 1];
            positions[pos2++] = positions_[3 * i + 2];
            for (int j = 0; j < lengths_[i]; j++)
                callsigns[pos3++] = signs_[pos4 + j];
            pos4 += lengths_[i];
        }
        planeCnt = lengths_.length;
    }
}


@SCJAllowed(members=true)
/*@Scope("cdx.CollisionDetectorHandler")*/
class Reducer {

    /** Creates a Vector2d that represents a voxel. */
    protected void voxelHash(Vector3d position, Vector2d voxel) {
        int x_div = (int) (position.x / voxel_size);
        voxel.x = voxel_size * (x_div);
        if (position.x < 0.0f) voxel.x -= voxel_size;

        int y_div = (int) (position.y / voxel_size);
        voxel.y = voxel_size * (y_div);
        if (position.y < 0.0f) voxel.y -= voxel_size;
    }

    /** * Puts a Motion object into the voxel map at a voxel. */
    protected void putIntoMap(HashMap voxel_map, Vector2d voxel, Motion motion) {
        if (!voxel_map.containsKey(voxel)) {
            voxel_map.put(new Vector2d(voxel), new ArrayList());
        }
        ((ArrayList) voxel_map.get(voxel)).add(motion);
    }

    /**
     * Given a voxel and a Motion, determines if they overlap.
     */
    protected boolean isInVoxel(Vector2d voxel, Motion motion) {
        if (voxel.x > Constants.MAX_X || voxel.x + voxel_size < Constants.MIN_X || voxel.y > Constants.MAX_Y
                || voxel.y + voxel_size < Constants.MIN_Y) {
            return false;
        }
        // this code detects intersection between a line segment and a square
        // (geometric intersection, it ignores the time and speeds of aircraft)
        //
        // the intuition is that we transform the coordinates such that the line segment
        // ends up being a line from (0,0) to (1,1) ; in this transformed system, the coordinates of
        // the square (becomes rectangle) are (low_x,low_y,high_x,high_y) ; in this transformed system,
        // it is possible to detect the intersection without further arithmetics (just need comparisons)
        //
        // this algorithm is probably of general use ; I have seen too many online posts advising
        // more complex solution to the problem that involved calculating intersections between rectangle
        // sides and the segment/line

        Vector3d init = motion.getFirstPosition();
        Vector3d fin = motion.getSecondPosition();

        float v_s = voxel_size;
        float r = Constants.PROXIMITY_RADIUS / 2.0f;

        float v_x = voxel.x;
        float x0 = init.x;
        float xv = fin.x - init.x;

        float v_y = voxel.y;
        float y0 = init.y;
        float yv = fin.y - init.y;

        float low_x, high_x;
        low_x = (v_x - r - x0) / xv;
        high_x = (v_x + v_s + r - x0) / xv;

        if (xv < 0.0f) {
            float tmp = low_x;
            low_x = high_x;
            high_x = tmp;
        }

        float low_y, high_y;
        low_y = (v_y - r - y0) / yv;
        high_y = (v_y + v_s + r - y0) / yv;

        if (yv < 0.0f) {
            float tmp = low_y;
            low_y = high_y;
            high_y = tmp;
        }
        // ugliest expression ever.
        boolean result = (((xv == 0.0 && v_x <= x0 + r && x0 - r <= v_x + v_s) /* no motion in x */|| ((low_x <= 1.0f && 1.0f <= high_x)
                || (low_x <= 0.0f && 0.0f <= high_x) || (0.0f <= low_x && high_x <= 1.0f)))
                && ((yv == 0.0 && v_y <= y0 + r && y0 - r <= v_y + v_s) /* no motion in y */|| ((low_y <= 1.0f && 1.0f <= high_y)
                        || (low_y <= 0.0f && 0.0f <= high_y) || (0.0f <= low_y && high_y <= 1.0f))) && (xv == 0.0f
                || yv == 0.0f || /* no motion in x or y or both */
                (low_y <= high_x && high_x <= high_y) || (low_y <= low_x && low_x <= high_y) || (low_x <= low_y && high_y <= high_x)));
        return result;
    }

    protected void dfsVoxelHashRecurse(Motion motion, Vector2d next_voxel, HashMap voxel_map, HashMap graph_colors) {
        Vector2d tmp = new Vector2d();

        if (!graph_colors.containsKey(next_voxel) && isInVoxel(next_voxel, motion)) {
            graph_colors.put(new Vector2d(next_voxel), new String(""));
            putIntoMap(voxel_map, next_voxel, motion);

            // left boundary
            VectorMath.subtract(next_voxel, horizontal, tmp);
            dfsVoxelHashRecurse(motion, tmp, voxel_map, graph_colors);

            // right boundary
            VectorMath.add(next_voxel, horizontal, tmp);
            dfsVoxelHashRecurse(motion, tmp, voxel_map, graph_colors);

            // upper boundary
            VectorMath.add(next_voxel, vertical, tmp);
            dfsVoxelHashRecurse(motion, tmp, voxel_map, graph_colors);

            // lower boundary
            VectorMath.subtract(next_voxel, vertical, tmp);
            dfsVoxelHashRecurse(motion, tmp, voxel_map, graph_colors);

            // upper-left
            VectorMath.subtract(next_voxel, horizontal, tmp);
            VectorMath.add(tmp, vertical, tmp);
            dfsVoxelHashRecurse(motion, tmp, voxel_map, graph_colors);

            // upper-right
            VectorMath.add(next_voxel, horizontal, tmp);
            VectorMath.add(tmp, vertical, tmp);
            dfsVoxelHashRecurse(motion, tmp, voxel_map, graph_colors);

            // lower-left
            VectorMath.subtract(next_voxel, horizontal, tmp);
            VectorMath.subtract(tmp, vertical, tmp);
            dfsVoxelHashRecurse(motion, tmp, voxel_map, graph_colors);

            // lower-right
            VectorMath.add(next_voxel, horizontal, tmp);
            VectorMath.subtract(tmp, vertical, tmp);
            dfsVoxelHashRecurse(motion, tmp, voxel_map, graph_colors);
        }
    }

    /**
     * Colors all of the voxels that overla the Motion.
     */
    protected void performVoxelHashing(Motion motion, HashMap voxel_map, HashMap graph_colors) {
        graph_colors.clear();
        Vector2d voxel = new Vector2d();
        voxelHash(motion.getFirstPosition(), voxel);
        dfsVoxelHashRecurse(motion, voxel, voxel_map, graph_colors);
    }

    /**
     * Takes a List of Motions and returns an List of Lists of Motions, where the inner lists implement RandomAccess.
     * Each Vector of Motions that is returned represents a set of Motions that might have collisions.
     */
    public LinkedList reduceCollisionSet(LinkedList motions) {
        HashMap voxel_map = new HashMap();
        HashMap graph_colors = new HashMap();

        for (Iterator iter = motions.iterator(); iter.hasNext();)
            performVoxelHashing((Motion) iter.next(), voxel_map, graph_colors);

        LinkedList ret = new LinkedList();
        for (Iterator iter = voxel_map.values().iterator(); iter.hasNext();) {
            LinkedList cur_set = (LinkedList) iter.next();
            if (cur_set.size() > 1) ret.add(cur_set);
        }
        return ret;
    }
    /** The voxel size. Each voxel is a square, so the is the length of a side. */
    public float    voxel_size;

    /** The horizontal side of a voxel. */
    public Vector2d horizontal;

    /** The vertical side of a voxel. */
    public Vector2d vertical;

    /** Initialize with a voxel size. */
    public Reducer(float voxel_size) {
        this.voxel_size = voxel_size;
        horizontal = new Vector2d(voxel_size, 0.0f);
        vertical = new Vector2d(0.0f, voxel_size);
    }
}




@SCJAllowed(members=true)
@Scope("cdx.Level0Safelet")
class StateTable {

    final private static int MAX_AIRPLANES = 10000;

    private Vector3d[]       allocatedVectors;
    private int              usedVectors;

    /** Mapping Aircraft -> Vector3d. */
    final private HashMap    motionVectors = new HashMap();

    StateTable() {
        allocatedVectors = new Vector3d[MAX_AIRPLANES];
        for (int i = 0; i < allocatedVectors.length; i++)
            allocatedVectors[i] = new Vector3d();

        usedVectors = 0;
    }

    @SCJAllowed(members=true)
    @Scope("cdx.Level0Safelet")
    private class R implements Runnable {
        CallSign callsign;
        float    x, y, z;

        @RunsIn("cdx.Level0Safelet")
        public void run() {
            Vector3d v = (Vector3d) motionVectors.get(callsign);
            if (v == null) {
                v = allocatedVectors[usedVectors++]; // FIXME: What if we exceed MAX?
                motionVectors.put(callsign, v);
            }
            v.x = x;
            v.y = y;
            v.z = z;
        }
    }
    private final R r = new R();

    @RunsIn("cdx.CollisionDetectorHandler")
    public void put(@Scope("cdx.Level0Safelet") final CallSign callsign, final float x, final float y, final float z) {
        r.callsign = callsign;
        r.x = x;
        r.y = y;
        r.z = z;
        ((ManagedMemory) MemoryArea.getMemoryArea(this)).executeInArea(r);
    }

    @RunsIn("cdx.CollisionDetectorHandler") @Scope(THIS)
    public Vector3d get(final CallSign callsign) {
        return (Vector3d) motionVectors.get(callsign);
    }
}



@SCJAllowed(members=true)
@Scope("cdx.Level0Safelet")
class TransientDetectorScopeEntry /*implements SCJRunnable*/ {

    private StateTable state;
    private float voxelSize;
    private RawFrame currentFrame;

    /*
     * public TransientDetectorScopeEntry(final StateTable s, final float
     * voxelSize) { state = s; this.voxelSize = voxelSize; }
     */

    public TransientDetectorScopeEntry(StateTable s, float voxelSize) {
        state = s;
        this.voxelSize = voxelSize;
    }

    //@SCJAllowed(SUPPORT)
    @RunsIn("cdx.CollisionDetectorHandler")
    public void run() {
        if (Constants.SYNCHRONOUS_DETECTOR || Constants.DEBUG_DETECTOR) {
            dumpFrame(new String("CD-PROCESSING-FRAME (indexed as received): "));
        }

        final Reducer reducer = new Reducer(voxelSize);


        int numberOfCollisions = lookForCollisions(reducer, createMotions());

        if (ImmortalEntry.recordedRuns < ImmortalEntry.maxDetectorRuns) {
            ImmortalEntry.detectedCollisions[ImmortalEntry.recordedRuns] = numberOfCollisions;
        }

        if (Constants.SYNCHRONOUS_DETECTOR || Constants.DEBUG_DETECTOR) {
            System.out.println("CD detected  " + numberOfCollisions
                    + " collisions.");
            int colIndex = 0;
            System.out.println("");
        }


        //BenchMem.setMemUsage(RealtimeThread.getCurrentMemoryArea().memoryConsumed());
    }

    @RunsIn("cdx.CollisionDetectorHandler")
    public int lookForCollisions(final Reducer reducer, final List motions) {
        final List check = reduceCollisionSet(reducer, motions);
        int suspectedSize = check.size();
        if (ImmortalEntry.recordedRuns < ImmortalEntry.maxDetectorRuns) {
            ImmortalEntry.suspectedCollisions[ImmortalEntry.recordedRuns] = suspectedSize;
        }
        if ((Constants.SYNCHRONOUS_DETECTOR || Constants.DEBUG_DETECTOR) && !check.isEmpty()) {
            System.out.println("CD found "+suspectedSize+" potential collisions");
            int i=0;
            for(final Iterator iter = check.iterator(); iter.hasNext();) {
                final List col = (List)iter.next();
                for(final Iterator aiter = col.iterator(); aiter.hasNext();) {
                    final Motion m = (Motion)aiter.next();
                    System.out.println("CD: potential collision "+i+" (of "+col.size()+" aircraft) includes motion "+m);
                }
                i++;
            }
        }

        int c = 0;
        final List ret = new LinkedList();
        for (final Iterator iter = check.iterator(); iter.hasNext();)
            c += determineCollisions((List) iter.next(), ret);
        return c;
    }

    /**
     * Takes a List of Motions and returns an List of Lists of Motions, where
     * the inner lists implement RandomAccess. Each Vector of Motions that is
     * returned represents a set of Motions that might have collisions.
     */
    @RunsIn("cdx.CollisionDetectorHandler")
    public List reduceCollisionSet(final Reducer it, final List motions) {
        final HashMap voxel_map = new HashMap();
        final HashMap graph_colors = new HashMap();

        for (final Iterator iter = motions.iterator(); iter.hasNext();)
            it.performVoxelHashing((Motion) iter.next(), voxel_map,
                    graph_colors);

        final List ret = new LinkedList();
        for (final Iterator iter = voxel_map.values().iterator(); iter
                .hasNext();) {
            final List cur_set = (List) iter.next();
            if (cur_set.size() > 1)
                ret.add(cur_set);
        }
        return ret;
    }

    @RunsIn("cdx.CollisionDetectorHandler")
    public boolean checkForDuplicates(final List collisions, Motion one,
            Motion two) {
        // (Peta) I have also changed the comparison employed in this method as
        // it is another major source of overhead
        // Java was checking all the callsign elements, while C just checked the
        // callsign array addresses
        byte c1 = one.getAircraft().getCallsign()[5];
        byte c2 = two.getAircraft().getCallsign()[5];
        for (final Iterator iter = collisions.iterator(); iter.hasNext();) {
            Collision c = (Collision) iter.next();
            if ((c.first().getCallsign()[5] == c1)
                    && (c.second().getCallsign()[5] == c2)) {
                // Benchmarker.done(4);
                return false;
            }
        }
        return true;
    }

    @RunsIn("cdx.CollisionDetectorHandler")
    public int determineCollisions(final List motions, List ret) {
        // (Peta) changed to iterators so that it's not killing the algorithm
        int _ret = 0;
        Motion[] _motions = (Motion[]) motions.toArray(new Motion[motions
                .size()]);
        // Motion[] _motions= (Motion)motions.toArray();
        for (int i = 0; i < _motions.length - 1; i++) {
            final Motion one = _motions[i]; // m2==two, m=one
            for (int j = i + 1; j < _motions.length; j++) {
                final Motion two = _motions[j];
                // if (checkForDuplicates(ret, one, two)) { // This is removed
                // because it is very very slow...
                final Vector3d vec = one.findIntersection(two);
                if (vec != null) {
                    ret.add(new Collision(one.getAircraft(), two.getAircraft(),
                            vec));
                    _ret++;
                }
            }
        }
        return _ret;
    }

    @RunsIn("cdx.CollisionDetectorHandler")
    public void dumpFrame(String debugPrefix) {

        String prefix = debugPrefix + frameno + " ";
        int offset = 0;
        for (int i = 0; i < currentFrame.planeCnt; i++) {

            int cslen = currentFrame.lengths[i];
            System.out.println(prefix
                    + new String(currentFrame.callsigns, offset, cslen) + " "
                    + currentFrame.positions[3 * i] + " "
                    + currentFrame.positions[3 * i + 1] + " "
                    + currentFrame.positions[3 * i + 2] + " ");
            offset += cslen;
        }
    }

    int frameno = -1; // just for debug

    @RunsIn("cdx.CollisionDetectorHandler")
    public void setFrame(final RawFrame f) {
        if (Constants.DEBUG_DETECTOR || Constants.DUMP_RECEIVED_FRAMES
                || Constants.SYNCHRONOUS_DETECTOR) {
            frameno++;
        }
        currentFrame = f;
        if (Constants.DUMP_RECEIVED_FRAMES) {
            dumpFrame(new String("CD-R-FRAME: "));
        }

    }

    /**
     * This method computes the motions and current positions of the aircraft
     * Afterwards, it stores the positions of the aircrafts into the StateTable
     * in the persistentScope
     *
     * @return
     */
    @RunsIn("cdx.CollisionDetectorHandler")
    public List createMotions() {
        final List ret = new LinkedList();
        final HashSet poked = new HashSet();

        Aircraft craft;
        Vector3d new_pos;

        for (int i = 0, pos = 0; i < currentFrame.planeCnt; i++) {

            final float x = currentFrame.positions[3 * i], y = currentFrame.positions[3 * i + 1], z = currentFrame.positions[3 * i + 2];
            final byte[] cs = new byte[currentFrame.lengths[i]];
            for (int j = 0; j < cs.length; j++)
                cs[j] = currentFrame.callsigns[pos + j];
            pos += cs.length;
            craft = new Aircraft(cs);
            new_pos = new Vector3d(x, y, z);

            poked.add(craft);
            @Scope("cdx.Level0Safelet") final Vector3d old_pos = state
                    .get(new CallSign(craft.getCallsign()));

            if (old_pos == null) {
                state.put(mkCallsignInPersistentScope(craft.getCallsign()),
                        new_pos.x, new_pos.y, new_pos.z);

                final Motion m = new Motion(craft, new_pos);
                if (Constants.DEBUG_DETECTOR
                        || Constants.SYNCHRONOUS_DETECTOR) {
                    System.out
                            .println("createMotions: old position is null, adding motion: "
                                    + m);
                }
                ret.add(m);
            } else {
                final Vector3d save_old_position = new Vector3d(old_pos.x,
                        old_pos.y, old_pos.z);
                old_pos.set(new_pos.x, new_pos.y, new_pos.z);

                final Motion m = new Motion(craft, save_old_position, new_pos);
                if (Constants.DEBUG_DETECTOR
                        || Constants.SYNCHRONOUS_DETECTOR) {
                    System.out.println("createMotions: adding motion: " + m);
                }
                ret.add(m);
            }
        }
        return ret;
    }

    /**
     * This Runnable enters the StateTable in order to allocate the callsign in
     * the PersistentScope
     */
    @Scope("cdx.Level0Safelet")
    @SCJAllowed(members=true)
    static class R implements Runnable {
        CallSign c;
        byte[] cs;

        @RunsIn("cdx.Level0Safelet")
        public void run() {
            c = new CallSign(cs);
        }
    }

    private final R r = new R();

    @RunsIn("cdx.CollisionDetectorHandler") @Scope("cdx.Level0Safelet")
    CallSign mkCallsignInPersistentScope(final byte[] cs) {
        try {
            @Scope("cdx.Level0Safelet")
            @DefineScope(name="cdx.CollisionDetectorHandler",parent="cdx.Level0Safelet")
            ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();
            r.cs = (byte[]) mem.newArrayInArea(r, byte.class, cs.length);
        } catch (IllegalAccessException e) {
            e.toString();
            // TODO: print error!
            //e.printStackTrace();
        }
        for (int i = 0; i < cs.length; i++)
            r.cs[i] = cs[i];

        ((ManagedMemory) ManagedMemory.getMemoryArea(state)).executeInArea(r);
        return r.c;
    }
}

@SCJAllowed(members=true)
class NanoClock {

    public static long baseMillis = -1;
    public static int  baseNanos  = -1;

    public static AbsoluteTime roundUp(AbsoluteTime t) { // round up to next or second next period

        long tNanos = t.getNanoseconds();
        long tMillis = t.getMilliseconds();

        long periodMillis = Constants.DETECTOR_PERIOD;

        if (tNanos > 0) {
            tNanos = 0;
            tMillis++;
        }

        if (periodMillis > 0) {
            tMillis = ((tMillis + periodMillis - 1) / periodMillis) * periodMillis;
        }

        return new AbsoluteTime(tMillis, (int) tNanos);
    }

    public static void init() {
        if (baseMillis != -1 || baseNanos != -1) { throw new RuntimeException("NanoClock already initialized."); }

        AbsoluteTime rt = roundUp(Clock.getRealtimeClock().getTime());

        baseNanos = rt.getNanoseconds();
        baseMillis = rt.getMilliseconds();
    }

    public static long now() {

        //AbsoluteTime t = Clock.getRealtimeClock().getTime();

        AbsoluteTime t = Clock.getRealtimeClock().getTimePrecise();


        return convert(t);
    }

    public static long convert(AbsoluteTime t) {

        long millis = t.getMilliseconds() - baseMillis;
        int nanos = t.getNanoseconds();

        return millis * 1000000 + nanos - baseNanos;
    }

    public static int asMicros(long relativeNanos) {
        if (relativeNanos < 0) {
            if (relativeNanos == -1) { return 0; }
        }

        long millis = baseMillis + relativeNanos / 1000000L;
        int nanos = baseNanos + (int) (relativeNanos % 1000000L);
        millis += nanos / 1000000L;
        nanos = nanos % 1000000;
        return nanos / 1000;
    }

    @SCJRestricted(maySelfSuspend = true)
    public static String asString(long relativeNanos) {

        if (relativeNanos < 0) {
            if (relativeNanos == -1) { return new String("NA"); }
        }

        long millis = baseMillis + relativeNanos / 1000000L;
        int nanos = baseNanos + (int) (relativeNanos % 1000000L);

        millis += nanos / 1000000L;
        nanos = nanos % 1000000;

        String ns = Integer.toString(nanos);
        int zeros = 6 - ns.length();
        StringBuilder result = new StringBuilder(Long.toString(millis));

        while (zeros-- > 0) {
            result = result.append(new String("0"));
        }

        result = result.append(ns);

        return result.toString();
    }

}


@SCJAllowed(members=true)
@Scope("cdx.CollisionDetectorHandler")
final class Vector2d {

    public float x, y;

    /**
     * The default constructor for the <code>Vector2d</code> returns an object representing the zero vector.
     */
    public Vector2d() {}

    /**
     * The main constructor for the <code>Vector2d</code> class takes the two coordinates as parameters and produces an
     * object representing that vector.
     *
     * @param x
     *            the coordinate on the x (east-west) axis
     * @param y
     *            the coordinate on the y (north-south) axis
     */
    public Vector2d(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /**
     * The secondary constructor for the <code>Vector2d</code> class takes a vector to copy into this new instance and
     * returns an instance that represents a copy of that vector.
     *
     * @param v
     *            the vale of the vector to copy into this new instance
     */
    public Vector2d(Vector2d v) {
        this.x = v.x;
        this.y = v.y;
    }

    /**
     * The <code>set</code> is basically a convenience method that resets the internal values of the coordinates.
     *
     * @param x
     *            the coordinate on the x (east-west) axis
     * @param y
     *            the coordinate on the y (north-south) axis
     */
    public void set(Vector2d val) {
        this.x = val.x;
        this.y = val.y;
    }

    /**
     * The <code>zero</code> method is a convenience method to zero the coordinates of the vector.
     */
    public void zero() {
        x = y = 0;
    }

    @Override
    @RunsIn(CALLER)
    public boolean equals(Object o) throws ClassCastException {
        try {
            return equals((Vector2d) o);
        } catch (ClassCastException e) {
            return false;
        }
    }

    @RunsIn(CALLER)
    public boolean equals(Vector2d b) {
        if (x != b.x) return false;
        if (y != b.y) return false;
        return true;
    }

    @Override
    public int hashCode() {
        long rawBytes = ((long) Float.floatToIntBits(y) << 32) | Float.floatToIntBits(x);
        int hash = 0xAAAAAAAA;
        for (int i = 0; i < 8; i++, rawBytes >>= 8) {
            byte curByte = (byte) (rawBytes & 0xFF);
            hash ^= ((i & 1) == 0) ? ((hash << 7) ^ curByte * (hash >>> 3)) :
                (~((hash << 11) + curByte ^ (hash >>> 5)));
        }
        return hash;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}



@SCJAllowed(members=true)
final class VectorConstants {

    public static final int NO_QUADRANT = 0;
    public static final int NE_QUADRANT = 1;
    public static final int NW_QUADRANT = 2;
    public static final int SE_QUADRANT = 4;
    public static final int SW_QUADRANT = 3;

    /**
     * The <code>west</code> method is a utility function that will set the destination operand to a unit vector
     * pointing in the "west" direction. This family of functions has been added for convenient access to the common
     * orthogonal directions.
     *
     * @param dest
     *            the vector in which to store the result
     */
    public static void west(Vector3d dest) {
        dest.x = -1;
        dest.y = 0;
        dest.z = 0;
    }

    /**
     * The <code>east</code> method is a utility function that will set the destination operand to a unit vector
     * pointing in the "east" direction. This family of functions has been added for convenient access to the common
     * orthogonal directions.
     *
     * @param dest
     *            the vector in which to store the result
     */
    public static void east(Vector3d dest) {
        dest.x = 1;
        dest.y = 0;
        dest.z = 0;
    }

    /**
     * The <code>north</code> method is a utility function that will set the destination operand to a unit vector
     * pointing in the "north" direction. This family of functions has been added for convenient access to the common
     * orthogonal directions.
     *
     * @param dest
     *            the vector in which to store the result
     */
    public static void north(Vector3d dest) {
        dest.x = 0;
        dest.y = 1;
        dest.z = 0;
    }

    /**
     * The <code>south</code> method is a utility function that will set the destination operand to a unit vector
     * pointing in the "south" direction. This family of functions has been added for convenient access to the common
     * orthogonal directions.
     *
     * @param dest
     *            the vector in which to store the result
     */
    public static void south(Vector3d dest) {
        dest.x = 0;
        dest.y = -1;
        dest.z = 0;
    }

    /**
     * The <code>up</code> method is a utility function that will set the destination operand to a unit vector pointing
     * in the "up" direction. This family of functions has been added for convenient access to the common orthogonal
     * directions.
     *
     * @param dest
     *            the vector in which to store the result
     */
    public static void up(Vector3d dest) {
        dest.x = 0;
        dest.y = 0;
        dest.z = 1;
    }

    /**
     * The <code>down</code> method is a utility function that will set the destination operand to a unit vector
     * pointing in the "down" direction. This family of functions has been added for convenient access to the common
     * orthogonal directions.
     *
     * @param dest
     *            the vector in which to store the result
     */
    public static void down(Vector3d dest) {
        dest.x = 0;
        dest.y = 0;
        dest.z = -1;
    }

    /**
     * The <code>direction</code> method is a utility function that will return a unit vector in the direction
     * specified. It takes an argument in degrees and a vector destination where it stores the result.
     *
     * @param deg
     *            the angle's value in degrees
     * @param dest
     *            the vector in which to store the result
     */
    public static void direction(float deg, Vector3d dest) {
        double radians = Math.PI * deg / 180;
        dest.x = (float) Math.cos(radians);
        dest.y = (float) Math.sin(radians);
        dest.z = 0;
    }

    /**
     * The <code>west</code> method is a utility function that will set the destination operand to a unit vector
     * pointing in the "west" direction. This family of functions has been added for convenient access to the common
     * orthogonal directions.
     *
     * @param dest
     *            the vector in which to store the result
     */
    public static void west(Vector2d dest) {
        dest.x = -1;
        dest.y = 0;
    }

    /**
     * The <code>east</code> method is a utility function that will set the destination operand to a unit vector
     * pointing in the "east" direction. This family of functions has been added for convenient access to the common
     * orthogonal directions.
     *
     * @param dest
     *            the vector in which to store the result
     */
    public static void east(Vector2d dest) {
        dest.x = 1;
        dest.y = 0;
    }

    /**
     * The <code>north</code> method is a utility function that will set the destination operand to a unit vector
     * pointing in the "north" direction. This family of functions has been added for convenient access to the common
     * orthogonal directions.
     *
     * @param dest
     *            the vector in which to store the result
     */
    public static void north(Vector2d dest) {
        dest.x = 0;
        dest.y = 1;
    }

    /**
     * The <code>south</code> method is a utility function that will set the destination operand to a unit vector
     * pointing in the "south" direction. This family of functions has been added for convenient access to the common
     * orthogonal directions.
     *
     * @param dest
     *            the vector in which to store the result
     */
    public static void south(Vector2d dest) {
        dest.x = 0;
        dest.y = -1;
    }

    /**
     * The <code>direction</code> method is a utility function that will return a unit vector in the direction
     * specified. It takes an argument in degrees and a vector destination where it stores the result.
     *
     * @param deg
     *            the angle's value in degrees
     * @param dest
     *            the vector in which to store the result
     */
    public static void direction(float deg, Vector2d dest) {
        double radians = Math.PI * deg / 180;
        dest.x = (float) Math.cos(radians);
        dest.y = (float) Math.sin(radians);
    }
}


@SCJAllowed(members=true)
final class VectorMath {

    /***********************************************************************************************
     * 3 D V e c t o r C o m p u t a t i o n s
     **********************************************************************************************/

    /**
     * The <code>add</code> method takes two vectors and adds them, placing the result in a third vector.
     *
     * @param a
     *            the value of the first vector
     * @param b
     *            the value of the second vector
     * @param dest
     *            the destination Vector3d to store the result
     */
    public static void add(Vector3d a, Vector3d b, Vector3d dest) {
        dest.x = a.x + b.x;
        dest.y = a.y + b.y;
        dest.z = a.z + b.z;
    }

    /**
     * The <code>subtract</code> method takes two vectors and subtracts them, placing the result in a third vector.
     *
     * @param a
     *            the value of the first vector
     * @param b
     *            the value of the second vector
     * @param dest
     *            the destination Vector3d to store the result
     */
    public static void subtract(Vector3d a, Vector3d b, Vector3d dest) {
        dest.x = a.x - b.x;
        dest.y = a.y - b.y;
        dest.z = a.z - b.z;
    }

    /**
     * The <code>scale</code> method takes a <code>Vector3d</code> and a scalar float value multiplies each component of
     * the Vector, storing the result in the third parameter.
     *
     * @param a
     *            the value of the first vector
     * @param scale
     *            the value to scale the vector by
     * @param dest
     *            the destination Vector3d to store the result
     */
    public static void scale(Vector3d a, float scale, Vector3d dest) {
        dest.x = a.x * scale;
        dest.y = a.y * scale;
        dest.z = a.z * scale;
    }

    /**
     * The <code>normalize</code> method takes a <code>Vector3d</code> and if it is non-zero, will normalize it so that
     * its magnitude will be 1.
     *
     * @param a
     *            the value of the vector to normalize
     * @param dest
     *            the destination Vector3d to store the result
     * @throws ZeroVectorException
     *             if the vector is zero
     */
    public static void normalize(Vector3d a, Vector3d dest) {
        float mag = magnitude(a);
        if (mag == 0) throw new ZeroVectorException("undefined");
        scale(a, 1 / mag, dest);
    }

    /**
     * The <code>magnitude</code> method takes a <code>Vector3d</code> and computes its magnitude according the
     * Euclidean norm.
     *
     * @param a
     *            the value of the vector of which to compute the magnitude
     * @returns the magnitude of the vector
     */
    public static float magnitude(Vector3d a) {
        return (float) Math.sqrt(a.x * a.x + a.y * a.y + a.z * a.z);
    }

    /**
     * The <code>distance</code> method takes two vectors and computes their (Euclidean) distance.
     *
     * @param a
     *            the value of the first vector
     * @param b
     *            the value of the second vector
     * @returns the distance between the two vectors
     */
    public static float distance(Vector3d a, Vector3d b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        float dz = a.z - b.z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * The <code>sqDistance</code> method takes two vectors and computes the square of their (Euclidean) distance. This
     * is just an optimization for the <code>distance</code> method that avoids an expensive floating point square root
     * computation.
     *
     * @param a
     *            the value of the first vector
     * @param b
     *            the value of the second vector
     * @returns the square of the distance between the two vectors
     */
    public static float sqDistance(Vector3d a, Vector3d b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        float dz = a.z - b.z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * The <code>dotProduct</code> method computes the dot product between two vectors using the standard inner product
     * formula.
     *
     * @param a
     *            the value of the first vector
     * @param b
     *            the value of the second vector
     * @returns the value of their dot product
     */
    public static float dotProduct(Vector3d a, Vector3d b) {
        return a.x * b.x + a.y * b.y + a.z * b.z;
    }

    /**
     * The <code>rotate</code> method takes a <code>Vector3d</code> and a scalar float value and will rotate the vector
     * in the xy plane.
     *
     * @param a
     *            the value of the first vector
     * @param radians
     *            the value to rotate the vector by
     * @param dest
     *            the destination Vector3d to store the result
     */
    public static void rotate(Vector3d a, float radians, Vector3d dest) {
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        float x = a.x, y = a.y;
        dest.x = x * cos - y * sin;
        dest.y = x * sin + y * cos;
        dest.z = a.z;
    }

    /**
     * The <code>theta</code> method takes a <code>Vector3d</code> and calculates the angle between the X-axis and the
     * vector, ignoring the z component of the vector.
     *
     * @param a
     *            the vector of which to calculate the theta angle
     * @returns the radian value in the range [0, 2*pi] that represents the angle between the x axis and this vector (in
     *          the xy plane)
     * @throws ZeroVectorException
     *             if the vector passed equals the zero vector, for which the theta value is undefined
     */
    public static float theta(Vector3d a) {
        float x = a.x, y = a.y;
        if (x == 0) { // tangent undefined for x = 0
            if (y == 0) throw new ZeroVectorException("undefined");
            if (y < 0) return (float) (1.5 * Math.PI);
            return (float) (0.5 * Math.PI);
        }
        float t = (float) Math.atan(y / x); // calculate theta

        if (x < 0) return (float) Math.PI - t; // adjust quadrant
        if (t < 0) t += 2 * Math.PI; // range adjustment [0, 2*pi]

        return t;
    }

    /**
     * The <code>phi</code> method takes a <code>Vector3d</code> and calculates the elevation between the XY-plane and
     * the vector.
     *
     * @param a
     *            the vector of which to calculate the phi angle
     * @returns the radian value in the range [-pi/2, pi/2] that represents the angle between the x axis and this vector
     *          (in the xy plane)
     * @throws ZeroVectorException
     *             if the vector passed equals the zero vector, for which the phi value is undefined
     */
    public static float phi(Vector3d a) {
        float x = a.x, y = a.y, z = a.z;
        if (x == 0 && y == 0) { // tangent undefined for h = 0
            if (z == 0) throw new ZeroVectorException("undefined");
            if (z < 0) return (float) (-0.5 * Math.PI);
            return (float) (0.5 * Math.PI);
        }
        float h = (float) Math.sqrt(x * x + y * y);
        float t = (float) Math.atan(y / h); // calculate phi

        return t;
    }

    /***********************************************************************************************
     * 2 D V e c t o r C o m p u t a t i o n s
     **********************************************************************************************/

    /**
     * The <code>add</code> method takes two vectors and adds them, placing the result in a third vector.
     *
     * @param a
     *            the value of the first vector
     * @param b
     *            the value of the second vector
     * @param dest
     *            the destination Vector2d to store the result
     */
    public static void add(Vector2d a, Vector2d b, Vector2d dest) {
        dest.x = a.x + b.x;
        dest.y = a.y + b.y;
    }

    /**
     * The <code>subtract</code> method takes two vectors and subtracts them, placing the result in a third vector.
     *
     * @param a
     *            the value of the first vector
     * @param b
     *            the value of the second vector
     * @param dest
     *            the destination Vector2d to store the result
     */
    public static void subtract(Vector2d a, Vector2d b, Vector2d dest) {
        dest.x = a.x - b.x;
        dest.y = a.y - b.y;
    }

    /**
     * The <code>scale</code> method takes a <code>Vector2d</code> and a scalar float value multiplies each component of
     * the Vector, storing the result in the third parameter.
     *
     * @param a
     *            the value of the first vector
     * @param scale
     *            the value to scale the vector by
     * @param dest
     *            the destination Vector2d to store the result
     */
    public static void scale(Vector2d a, float scale, Vector2d dest) {
        dest.x = a.x * scale;
        dest.y = a.y * scale;
    }

    /**
     * The <code>normalize</code> method takes a <code>Vector2d</code> and if it is non-zero, will normalize it so that
     * its magnitude will be 1.
     *
     * @param a
     *            the value of the vector to normalize
     * @param dest
     *            the destination Vector2d to store the result
     * @throws ZeroVectorException
     *             if the vector is zero
     */
    public static void normalize(Vector2d a, Vector2d dest) {
        float mag = magnitude(a);
        if (mag == 0) throw new ZeroVectorException("undefined");
        scale(a, 1 / mag, dest);
    }

    /**
     * The <code>magnitude</code> method takes a <code>Vector2d</code> and computes its magnitude according the
     * Euclidean norm.
     *
     * @param a
     *            the value of the vector of which to compute the magnitude
     * @returns the magnitude of the vector
     */
    public static float magnitude(Vector2d a) {
        return (float) Math.sqrt(a.x * a.x + a.y * a.y);
    }

    /**
     * The <code>distance</code> method takes two vectors and computes their (Euclidean) distance.
     *
     * @param a
     *            the value of the first vector
     * @param b
     *            the value of the second vector
     * @returns the distance between the two vectors
     */
    public static float distance(Vector2d a, Vector2d b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * The <code>sqDistance</code> method takes two vectors and computes the square of their (Euclidean) distance. This
     * is just an optimization for the <code>distance</code> method that avoids an expensive floating point square root
     * computation.
     *
     * @param a
     *            the value of the first vector
     * @param b
     *            the value of the second vector
     * @returns the square of the distance between the two vectors
     */
    public static float sqDistance(Vector2d a, Vector2d b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return dx * dx + dy * dy;
    }

    /**
     * The <code>dotProduct</code> method computes the dot product between two vectors using the standard inner product
     * formula.
     *
     * @param a
     *            the value of the first vector
     * @param b
     *            the value of the second vector
     * @returns the value of their dot product
     */
    public static float dotProduct(Vector2d a, Vector2d b) {
        return a.x * b.x + a.y * b.y;
    }

    /**
     * The <code>quadrant</code> method is a utility function for two dimensional vectors that takes a vector as a
     * parameter and will return an integer describing what quadrant of the xy plane the vector lies in.
     *
     * @param a
     *            the vector to compute the quadrant of
     * @returns the integer VectorConstants.XX_QUADRANT value corresponding to which quadrant the vector lies in
     */
    public static int quadrant(Vector2d a) {
        float x = a.x, y = a.y;
        float xy = x * y;

        if (xy == 0) return VectorConstants.NO_QUADRANT; // lies on axis

        if (xy > 0) {
            if (x > 0) return VectorConstants.NE_QUADRANT;
            else return VectorConstants.SW_QUADRANT;
        } else {
            if (x < 0) return VectorConstants.NW_QUADRANT;
            else return VectorConstants.SE_QUADRANT;
        }
    }

    /***********************************************************************************************
     * 2 D / 3 D M i x e d C o m p u t a t i o n s
     **********************************************************************************************/

    /**
     * The <code>convert</code> methods have been overridden to allow 2d vectors to be converted to 3d vectors and vice
     * versa.
     *
     * @param src
     *            the value of the source vector
     * @param dest
     *            the value of the destination vector
     */
    public static void convert(Vector3d src, Vector2d dest) {
        dest.x = src.x;
        dest.y = src.y;
    }

    /**
     * The <code>convert</code> methods have been overridden to allow 2d vectors to be converted to 3d vectors and vice
     * versa.
     *
     * @param src
     *            the value of the source vector
     * @param dest
     *            the value of the destination vector
     */
    public static void convert(Vector2d src, Vector3d dest) {
        dest.x = src.x;
        dest.y = src.y;
        dest.z = 0;
    }

    /**
     * The <code>distance</code> method takes two vectors and computes their (Euclidean) distance. It has been
     * overloaded to allow the computation of the distance between a 3d vector and a 2d vector.
     *
     * @param a
     *            the value of the first vector
     * @param b
     *            the value of the second vector
     * @returns the distance between the two vectors
     */
    public static float distance(Vector3d a, Vector2d b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * The <code>distance</code> method takes two vectors and computes their (Euclidean) distance. It has been
     * overloaded to allow the computation of the distance between a 3d vector and a 2d vector.
     *
     * @param a
     *            the value of the first vector
     * @param b
     *            the value of the second vector
     * @returns the distance between the two vectors
     */
    public static float distance(Vector2d a, Vector3d b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * The <code>sqDistance</code> method takes two vectors and computes the square of their (Euclidean) distance. This
     * is just an optimization for the <code>distance</code> method that avoids an expensive floating point square root
     * computation. It has been overloaded to allow the computation of the distance between a 3d vector and a 2d vector.
     *
     * @param a
     *            the value of the first vector
     * @param b
     *            the value of the second vector
     * @returns the square of the distance between the two vectors
     */
    public static float sqDistance(Vector3d a, Vector2d b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return dx * dx + dy * dy;
    }

    /**
     * The <code>sqDistance</code> method takes two vectors and computes the square of their (Euclidean) distance. This
     * is just an optimization for the <code>distance</code> method that avoids an expensive floating point square root
     * computation. It has been overloaded to allow the computation of the distance between a 3d vector and a 2d vector.
     *
     * @param a
     *            the value of the first vector
     * @param b
     *            the value of the second vector
     * @returns the square of the distance between the two vectors
     */
    public static float sqDistance(Vector2d a, Vector3d b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return dx * dx + dy * dy;
    }

}


@SCJAllowed(members=true)
class ZeroVectorException extends ArithmeticException {

    /**
	  *
	  */
    private static final long serialVersionUID = 6064932560449189963L;

    /**
     * The only constructor for the <code>ZeroVectorException</code> class takes a string as an argument and simply
     * calls the super constructor.
     *
     * @param msg
     *            a message describing the operation that caused the exception
     **/
    public ZeroVectorException(String msg) {
        super(msg);
    }
}



@SCJAllowed(members=true)
final class Vector3d {
    public float x, y, z;

    /**
     * The default constructor for the <code>Vector3d</code> returns an object representing the zero vector.
     */
    public Vector3d() {}

    /**
     * The main constructor for the <code>Vector3d</code> class takes the three coordinates as parameters and produces
     * an object representing that vector.
     *
     * @param x
     *            the coordinate on the x (east-west) axis
     * @param y
     *            the coordinate on the y (north-south) axis
     * @param z
     *            the coordinate on the z (elevation) axis
     */
    public Vector3d(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * The secondary constructor for the <code>Vector3d</code> class takes a vector to copy into this new instance and
     * returns an instance that represents a copy of that vector.
     *
     * @param v
     *            the vale of the vector to copy into this new instance
     */
    public Vector3d(Vector3d v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }

    /**
     * The <code>set</code> is basically a convenience method that resets the internal values of the coordinates.
     *
     * @param x
     *            the coordinate on the x (east-west) axis
     * @param y
     *            the coordinate on the y (north-south) axis
     * @param z
     *            the coordinate on the z (elevation) axis
     */
    @RunsIn(CALLER)
    public void set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * The <code>set</code> is basically a convenience method that resets the internal values of the coordinates.
     *
     * @param val
     *            the value of the vector
     */
    public void set(Vector3d val) {
        this.x = val.x;
        this.y = val.y;
        this.z = val.z;
    }

    /**
     * The <code>zero</code> method is a convenience method to zero the coordinates of the vector.
     */
    public void zero() {
        x = y = z = 0;
    }

    @Override
    @RunsIn(CALLER)
    public boolean equals(Object o) {
        try {
            return equals((Vector3d) o);
        } catch (ClassCastException e) {
            return false;
        }
    }

    @RunsIn(CALLER)
    public boolean equals(Vector3d b) {
        if (x != b.x) return false;
        if (y != b.y) return false;
        if (z != b.z) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return (int) ((x + y + z) * y + (x - y + z) * x - (x - y - z) * z);
    }

    @Override
    public String toString() {
	// return "(" + ASCIIConverter.floatToString(x) + ", " + ASCIIConverter.floatToString(y) + ", "
	//      + ASCIIConverter.floatToString(z) + ")";
	return "(" + x + ", " + y + ", " + z + ")";
    }
}





@Scope(IMMORTAL)
@SCJAllowed(members=true)
abstract class FrameBuffer {

    // empty buffer ... first == last
    // full buffer .... last + 1 == first
    // - so there is still one empty slot, but we don't want to use it,
    // because we would not then recognize empty from full buffer
    //
    // last .. where the next frame will be stored
    // first .. where the next frame will be read

    public int       first, last;

    float  t;
    int[]  lengths;
    byte[] callsigns;
    RawFrame buf;

    static int frameno = 0;


    public FrameBuffer() {
        t = 0.0f;
        lengths = new int[Constants.NUMBER_OF_PLANES];
        callsigns = new byte[Constants.NUMBER_OF_PLANES * 6];
        buf = new RawFrame();
        for (int k = 0; k < lengths.length; k++)
            lengths[k] = 6;
    }

    public abstract void putFrameInternal(final float[] positions_, final int[] lengths_, final byte[] callsigns_);


    public abstract void putFrame(final float[] positions_, final int[] lengths_, final byte[] callsigns_);

    /*/////@RunsIn("cdx.CollisionDetectorHandler")*/
    @RunsIn(CALLER)
    public abstract RawFrame getFrame();

}


@Scope(IMMORTAL)
@SCJAllowed(members=true)
class FrameBufferPLDI extends FrameBuffer {

	// empty buffer ... first == last
	// full buffer .... last + 1 == first
	//    - so there is still one empty slot, but we don't want to use it,
	//      because we would not then recognize empty from full buffer
	//
	// last .. where the next frame will be stored
	// first .. where the next frame will be read

	public int first, last;
	public RawFrame[] frames;
    protected float t;
    protected int[] lengths; //= new int[] { 6, 6, 6,6, 6, 6,6, 6, 6,6, 6, 6,6, 6, 6,6, 6, 6,6, 6, 6,6, 6, 6,6, 6, 6,6, 6, 6,6, 6, 6,6, 6, 6,6, 6, 6,6, 6, 6,6, 6, 6,6, 6, 6,6, 6, 6,6, 6, 6,6, 6, 6, 6, 6, 6 };
    protected byte[] callsigns; //= new byte[60*6]; // { 112, 108, 97, 110, 101, 48, 112, 108, 97, 110, 101, 49, 112, 108, 97, 110, 101, 50, 112, 108, 97, 110, 101, 52, 48, 112, 108, 97, 110, 101, 52, 49, 112, 108, 97, 110, 101, 52, 50 };


    public FrameBufferPLDI() {
        if (Constants.FRAME_ON_THE_GO) {
            t=0.0f;
            lengths= new int[Constants.NUMBER_OF_PLANES];
            callsigns=new byte[Constants.NUMBER_OF_PLANES*6];
            for (int k=0;k<lengths.length;k++) lengths[k]=6;
        } else {
            frames = new RawFrame[Constants.BUFFER_FRAMES];
            for (int i = 0; i < Constants.BUFFER_FRAMES; i++) {
                frames[i] = new RawFrame();
            }
        }
	}

	@Override
    public void putFrameInternal(final float[] positions_, final int[] lengths_, final byte[] callsigns_) {
        if (Constants.FRAME_ON_THE_GO) return;
        if ( (last + 1) % Constants.BUFFER_FRAMES == first) {
            ImmortalEntry.droppedFrames ++;
            return;
        }
        frames[last].copy(lengths_, callsigns_, positions_);
        last = (last + 1) % Constants.BUFFER_FRAMES;
	}

	static int frameno = 0;

	@Override
    public void putFrame(final float[] positions_, final int[] lengths_, final byte[] callsigns_) {
        if (Constants.FRAME_ON_THE_GO) return;
		if (Constants.SYNCHRONOUS_DETECTOR || Constants.DUMP_SENT_FRAMES) {
			String prefix = "S-FRAME " + frameno + " ";
			int offset = 0;
			for (int i=0;i<lengths_.length;i++) {

				int cslen = lengths_[i];
				System.out.println(prefix+new String( callsigns_, offset, cslen )+" "+
						positions_[3*i]+" "+
						positions_[3*i+1]+" "+
						positions_[3*i+2]+" ");
				offset += cslen;
			}
			frameno++;
		}
		if (Constants.FRAMES_BINARY_DUMP) {
			// the binary format:
			//   nframes <INT>
			//
			//   nplanes <INT> 1
			//   positions <FLOAT> nplanes*3
			//   lengths <INT> nplanes
			//   callsigns_length <INT> 1
			//   callsigns <BYTE> callsigns_length

			DataOutputStream ds = ImmortalEntry.binaryDumpStream;

			try {
				ds.writeInt(lengths_.length);

				for(int i=0; i<lengths_.length; i++) {
					ds.writeFloat(positions_[3*i]);
					ds.writeFloat(positions_[3*i+1]);
					ds.writeFloat(positions_[3*i+2]);
				}

				for(int i=0;i<lengths_.length; i++) {
					ds.writeInt(lengths_[i]);
				}

				ds.writeInt(callsigns_.length);
				ds.write(callsigns_);

			} catch (IOException e) {
				throw new RuntimeException("Error dumping frames to binary file "+e);
			}

		}
		if (Constants.SYNCHRONOUS_DETECTOR) {
			//FrameSynchronizer.produceFrame();
		}
		putFrameInternal(positions_, lengths_, callsigns_);
		if (Constants.SYNCHRONOUS_DETECTOR) {
			//FrameSynchronizer.waitForConsumer();
		}
	}



	@Override
    @RunsIn(CALLER)
    public RawFrame getFrame() {
        if (Constants.FRAME_ON_THE_GO) {
            //RawFrame result=new RawFrame();

        	for (byte k=0;k<Constants.NUMBER_OF_PLANES;k++) {
                callsigns[6*k]=112;
                callsigns[6*k+1]=108;
                callsigns[6*k+2]=97;
                callsigns[6*k+3]=110;
                callsigns[6*k+4]=101;
                callsigns[6*k+5]=(byte)(49+k);
            }
            float positions[] = new float[60*3];

            for (int k=0;k<Constants.NUMBER_OF_PLANES/2;k++) {
                positions[3*k]=(float)(100*Math.cos(t)+500+50*k);
                positions[3*k+1]=100.0f;
                positions[3*k+2]=5.0f;
                positions[Constants.NUMBER_OF_PLANES/2*3+3*k]=(float)(100*Math.sin(t)+500+50*k);
                positions[Constants.NUMBER_OF_PLANES/2*3+3*k+1]=100.0f;
                positions[Constants.NUMBER_OF_PLANES/2*3+3*k+2]=5.0f;
            }
            // increase the time
            t=t+0.25f;
            buf.copy(lengths,callsigns,positions);
            return buf;
        } else {
            if (last == first) {
                return null;
            } else {
                final int f = first;
                first = (first + 1) % Constants.BUFFER_FRAMES;
                return frames[f];   // NOTE: if the simulator could run between this and the previous line,
                                    // it could corrupt the present frame
            }
        }
	}
}

@Scope(IMMORTAL)
@SCJAllowed(members = true)
class WorkloadStar extends FrameBuffer {

    final double   tic      = 0.25f;
    final double   lenght   = 20;
    static double  t        = 0;

    final double[] angels   = new double[Constants.NUMBER_OF_PLANES];

    final double   offset_x = 100;
    final double   offset_y = 100;
    final double   offset_z = 5.0f;

    double[]       last_px  = new double[Constants.NUMBER_OF_PLANES];
    double[]       last_py  = new double[Constants.NUMBER_OF_PLANES];

    boolean        outbound = true;

    int            CYCLES   = (int) (lenght / tic) * 2;

    public WorkloadStar() {
        super();
        init_angels();
    }

    @Override
    @RunsIn(CALLER)
    public RawFrame getFrame() {
        for (byte k = 0; k < Constants.NUMBER_OF_PLANES; k++) {
            callsigns[6 * k] = 112;
            callsigns[6 * k + 1] = 108;
            callsigns[6 * k + 2] = 97;
            callsigns[6 * k + 3] = 110;
            callsigns[6 * k + 4] = 101;
            callsigns[6 * k + 5] = (byte) (49 + k);
        }
        float positions[] = new float[Constants.NUMBER_OF_PLANES * 3];

        // System.out.println("frame " + frameno);
        for (byte k = 0; k < Constants.NUMBER_OF_PLANES; k++) {
            if (frameno != 0) // we dont move in the first frame
            if (outbound) line(k);
            else line_bck(k);

            positions[3 * k] = (float) last_px[k];
            positions[3 * k + 1] = (float) last_py[k];
            positions[3 * k + 2] = (float) offset_z;

            // System.out.println("plane :" + k + " x:" + last_px[k] + "-- y:" + last_py[k] );

        }

        if (frameno != 0 && ((frameno) % CYCLES) == 0) {
            outbound = !outbound;
        }

        // increase the time
        t = t + tic;
        buf.copy(lengths, callsigns, positions);

        frameno++;
        return buf;
    }

    @RunsIn(CALLER)
    private void line(int k) {
        double alpha = angels[k];
        last_px[k] = last_px[k] + tic * Math.cos(alpha);
        last_py[k] = last_py[k] - tic * Math.sin(alpha);
    }

    @RunsIn(CALLER)
    private void line_bck(int k) {
        double alpha = angels[k];
        last_px[k] = last_px[k] - tic * Math.cos(alpha);
        last_py[k] = last_py[k] + tic * Math.sin(alpha);
    }

    @RunsIn(CALLER)
    private void init_angels() {
        double step = 180 / Constants.NUMBER_OF_PLANES;
        double angel = 0;
        for (int k = 0; k < Constants.NUMBER_OF_PLANES; k++) {
            angels[k] = Math.toRadians(angel);
            last_px[k] = offset_x - lenght * Math.cos(angels[k]);
            last_py[k] = offset_y + lenght * Math.sin(angels[k]);

            // System.out.println("angel:" + angel);
            // System.out.println("lenght x:" + lenght * Math.cos(angels[k]));
            // System.out.println("lenght y:" + lenght * Math.sin(angels[k]));

            angel += step;
        }

        // System.out.println("angels" + angels);
    }

    @Override
    public void putFrame(float[] positions, int[] lengths, byte[] callsigns) {}

    @Override
    public void putFrameInternal(float[] positions, int[] lengths, byte[] callsigns) {}

}

@Scope(IMMORTAL)
@SCJAllowed(members=true)
abstract class PrecompiledSimulator {

    public static void dumpStats() {}

    protected Object[] positions;
    protected Object[] lengths;
    protected Object[] callsigns;

    // args .. ignored
    @RunsIn(IMMORTAL)
    public static void generate(final String[] args) {

        (new Simulator()).generate();
    }

    public void generate() {
    /*
    synchronized (ImmortalEntry.initMonitor) {

    /*	if (!immortal.Constants.PRESIMULATE) {
    		ImmortalEntry.simulatorReady = true;
    		ImmortalEntry.initMonitor.notifyAll();
    	}

    	while (!ImmortalEntry.detectorReady) {
    		try {
    			ImmortalEntry.initMonitor.wait();
    		} catch (InterruptedException e) {
    		}
    	} */
    /*}


    if (positions.length < immortal.Constants.MAX_FRAMES) {
    	throw new RuntimeException("Not enough frames in pre-compiled simulator.");
    }

    for(int frameIndex=0; frameIndex<immortal.Constants.MAX_FRAMES;frameIndex++) {

    	immortal.ImmortalEntry.frameBuffer.putFrame( (float[])positions[frameIndex],
    			(int[])lengths[frameIndex],
    			(byte[])callsigns[frameIndex]);
    }
    //System.out.println("Generated "+immortal.Constants.MAX_FRAMES+" frames.");

    /*		if (immortal.Constants.PRESIMULATE) {
    	synchronized (ImmortalEntry.initMonitor) {
    		ImmortalEntry.simulatorReady = true;
    		ImmortalEntry.initMonitor.notifyAll();
    	}
    } */

    }
}


@Scope(IMMORTAL)
@SCJAllowed(members=true)
class Simulator extends PrecompiledSimulator {
    public Simulator() {}
}
