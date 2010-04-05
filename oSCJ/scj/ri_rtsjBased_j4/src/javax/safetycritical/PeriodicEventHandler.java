package javax.safetycritical;

// (annotations turned off to work with Java 1.4) import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.realtime.PeriodicParameters;
import javax.realtime.PeriodicTimer;
import javax.realtime.PriorityParameters;
import javax.realtime.Timer;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.MemoryAreaEncloses;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.SCJAllowed;

// (annotations turned off to work with Java 1.4) @SCJAllowed
public abstract class PeriodicEventHandler extends ManagedEventHandler {

    private Timer _timer;

// (annotations turned off to work with Java 1.4)     @MemoryAreaEncloses(inner = { "this", "this", "this" }, outer = {
//            "priority", "parameters", "memSize" })
// (annotations turned off to work with Java 1.4)     @SCJAllowed
    public PeriodicEventHandler(PriorityParameters priority,
            PeriodicParameters period, StorageConfigurationParameters storage,
            long size) {
        this(priority, period, storage, size, null);
    }

// (annotations turned off to work with Java 1.4)     @MemoryAreaEncloses(inner = { "this", "this", "this", "this" }, outer = {
//            "priority", "parameters", "memSize", "name" })
// (annotations turned off to work with Java 1.4)     @SCJAllowed(LEVEL_1)
    public PeriodicEventHandler(PriorityParameters priority,
            PeriodicParameters period, StorageConfigurationParameters storage,
            long size, String name) {
        super(priority, period, storage, size, name);
        _timer = new PeriodicTimer(period.getStart(), period.getPeriod(), this);
    }

    void start() {
        _timer.start();
    }

    void join() {
        _timer.destroy();
        super.join();
    }
}
