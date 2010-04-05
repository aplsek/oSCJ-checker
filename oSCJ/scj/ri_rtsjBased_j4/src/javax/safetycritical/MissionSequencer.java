package javax.safetycritical;

// (annotations turned off to work with Java 1.4) import static javax.safetycritical.annotate.Level.LEVEL_2;

import javax.realtime.AsyncEvent;
import javax.realtime.BoundAsyncEventHandler;
import javax.realtime.MemoryArea;
import javax.realtime.PriorityParameters;
import javax.realtime.RealtimeThread;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.MemoryAreaEncloses;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.SCJAllowed;

// (annotations turned off to work with Java 1.4) @SCJAllowed
public abstract class MissionSequencer extends BoundAsyncEventHandler {

    private AsyncEvent _event = new AsyncEvent();

    private MissionMemory _missionMem = new MissionMemory();

    private Object _quitMonitor = new Object();

    private boolean _notified = false;

    private Mission _mission;

// (annotations turned off to work with Java 1.4)     @MemoryAreaEncloses(inner = { "this" }, outer = { "priority" })
// (annotations turned off to work with Java 1.4)     @SCJAllowed
    public MissionSequencer(PriorityParameters priority,
            StorageConfigurationParameters storage) {
        super(priority, null, null, null, null, true, null);
        _event.addHandler(this);

        MemoryArea mem = RealtimeThread.getCurrentMemoryArea();

        // If we are a sub-mission sequencer, let the outer mission manager know
        // us, so that it can take control of us when necessary.
        if (mem instanceof ManagedMemory)
            ((ManagedMemory) mem).getManager().addMissionSequencer(this);
    }

// (annotations turned off to work with Java 1.4)     @SCJAllowed
    public final synchronized void handleAsyncEvent() {
        _mission = getInitialMission();
        while (_mission != null) {
            _missionMem.setSize(_mission.missionMemorySize());
            _missionMem.enter(_mission);

            if (_mission._terminateAll)
                break;
            // if (!_mission._restart)
            _mission = getNextMission();
        }
        synchronized (_quitMonitor) {
            _notified = true;
            _quitMonitor.notifyAll();
        }
    }

// (annotations turned off to work with Java 1.4)     @SCJAllowed(LEVEL_2)
    public synchronized void start() {
        _event.fire();
    }

// (annotations turned off to work with Java 1.4)     @SCJAllowed
    public final void requestTermination() {
        _mission.requestSequenceTermination();
    }

    void waitForTermination() {
        synchronized (_quitMonitor) {
            while (!_notified) {
                try {
                    _quitMonitor.wait();
                } catch (InterruptedException e) {
                    // TODO
                }
            }
        }
    }

// (annotations turned off to work with Java 1.4)     @SCJAllowed
    protected abstract Mission getInitialMission();

// (annotations turned off to work with Java 1.4)     @SCJAllowed
    protected abstract Mission getNextMission();
}
