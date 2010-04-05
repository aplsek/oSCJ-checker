package javax.safetycritical;

// (annotations turned off to work with Java 1.4) import static javax.safetycritical.annotate.Level.LEVEL_0;

import javax.realtime.BoundAsyncEventHandler;
import javax.realtime.PriorityParameters;
import javax.realtime.ReleaseParameters;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.SCJAllowed;

// (annotations turned off to work with Java 1.4) @SCJAllowed(LEVEL_0)
public abstract class ManagedEventHandler extends BoundAsyncEventHandler {

    private String _name;

    private PrivateMemory _initMem;

    private Runnable _runner = new Runnable() {
        public void run() {
            handleEvent();
        }
    };

    public ManagedEventHandler(PriorityParameters priority,
            ReleaseParameters release, StorageConfigurationParameters storage,
            long sizeInByte, String name) {
        super(priority, release, null, null, null, true, null);
        _name = name;
        _initMem = new PrivateMemory(sizeInByte);
        setDaemon(false);
        MissionManager.getCurrentMissionManager().addEventHandler(this);
    }

    /**
     * Application developers override this method with code to be executed
     * whenever the event(s) to which this event handler is bound is fired.
     */
// (annotations turned off to work with Java 1.4)     @SCJAllowed(LEVEL_0)
    public abstract void handleEvent();

    /**
     * This is overridden to ensure entry into the local scope for each release.
     * This may change for RTSJ 1.1, where a provided scope is automatically
     * entered at each release.
     */
// (annotations turned off to work with Java 1.4)     @Override
// (annotations turned off to work with Java 1.4)     @SCJAllowed(LEVEL_0)
    public final void handleAsyncEvent() {
        _initMem.enter(_runner);
    }

// (annotations turned off to work with Java 1.4)     @SCJAllowed(LEVEL_0)
    public void cleanup() {
    }

    public String getName() {
        return _name;
    }

    void join() {
        try {
            _initMem.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
