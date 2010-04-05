package javax.safetycritical;

import javax.realtime.LTMemory;
import javax.realtime.SizeEstimator;

public class MissionMemory extends LTMemory implements ManagedMemory {

    private static SizeEstimator _estimator;

    private Mission _mission;

    private long _size;

    static {
        _estimator = new SizeEstimator();
        _estimator.reserve(BackingStore.class, 10);
    }

    private Runnable _missionWrapper = new Runnable() {
        // running in the backing store
        public void run() {
            _mission.run();
        }
    };

    private Runnable _helper = new Runnable() {
        // running in the mission memory wrapper
        public void run() {
            (new BackingStore(_size)).enter(_missionWrapper);
        }
    };

    MissionMemory() {
        super(_estimator);
    }

    // only used by BackingStore
    protected MissionMemory(long sizeInByte) {
        super(sizeInByte);
    }

    void enter(Mission mission) {
        _mission = mission;
        super.enter(_helper);
    }

    void setSize(long size) {
        _size = size;
    }

    /*
     * The following two methods must never be invoked because this class is
     * just a wrapper. They are overrided in sub-class BackingStore, which
     * really function.
     */
    public MissionManager getManager() {
        return null;
    }
    void setManager(MissionManager mngr) {
    }
}
