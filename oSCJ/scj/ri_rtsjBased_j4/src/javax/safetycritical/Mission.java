package javax.safetycritical;

// (annotations turned off to work with Java 1.4) import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.realtime.MemoryArea;
import javax.realtime.RealtimeThread;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.Allocate;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.SCJAllowed;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.Allocate.Area;

// (annotations turned off to work with Java 1.4) @SCJAllowed
public abstract class Mission {

    // boolean _restart;
    boolean _terminateAll;
    volatile int _phase = Phase.INACTIVE;
    private Object _quitMonitor = new Object();

    public static class Phase {
        public final static int INITIAL = 0;
        public final static int EXECUTE = 1;
        public final static int CLEANUP = 2;
        public final static int INACTIVE = -1;
        public final static int TERMINATED = -2;
    }

// (annotations turned off to work with Java 1.4)     @Allocate( { Area.THIS })
// (annotations turned off to work with Java 1.4)     @SCJAllowed(LEVEL_1)
    public Mission() {
    }

// (annotations turned off to work with Java 1.4)     @SCJAllowed
    public static Mission getCurrentMission() {
        MemoryArea mem = RealtimeThread.getCurrentMemoryArea();
        if (!(mem instanceof ManagedMemory))
            throw new Error("Cannot get current mission in non-managed memory");

        return ((ManagedMemory) mem).getManager().getMission();
    }

// (annotations turned off to work with Java 1.4)     @SCJAllowed()
    public void requestTermination() {
        if (_phase != Phase.TERMINATED) {
            _phase = Phase.TERMINATED;
            synchronized (_quitMonitor) {
                _quitMonitor.notify();
            }
        }
    }

// (annotations turned off to work with Java 1.4)     @SCJAllowed()
    public void requestSequenceTermination() {
        _terminateAll = true;
        requestTermination();
    }

    final void run() {
        // _terminateAll = _restart = false;
        // we are sure we run in backing store
        MissionMemory mem = (MissionMemory) RealtimeThread
                .getCurrentMemoryArea();

        MissionManager mngr = new MissionManager(this);
        mem.setManager(mngr);

        _terminateAll = false;

        _phase = Phase.INITIAL;
        initialize();
        _phase = Phase.EXECUTE;
        exec(mngr);
        _phase = Phase.CLEANUP;
        cleanup();
        _phase = Phase.INACTIVE;
    }

    void exec(MissionManager mngr) {
        mngr.startAll();
        synchronized (_quitMonitor) {
            try {
                _quitMonitor.wait();
            } catch (InterruptedException e) {
                // TODO:
            }
        }
        mngr.cleanAll();
    }

// (annotations turned off to work with Java 1.4)     @SCJAllowed()
    public abstract long missionMemorySize();

// (annotations turned off to work with Java 1.4)     @SCJAllowed()
    protected abstract void initialize();

// (annotations turned off to work with Java 1.4)     @SCJAllowed(LEVEL_1)
    protected void cleanup() {
    }
}
