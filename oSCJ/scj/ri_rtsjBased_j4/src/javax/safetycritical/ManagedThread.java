package javax.safetycritical;

// (annotations turned off to work with Java 1.4) import static javax.safetycritical.annotate.Level.LEVEL_2;

import javax.realtime.HighResolutionTime;
import javax.realtime.PriorityParameters;
import javax.realtime.RealtimeThread;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.MemoryAreaEncloses;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.SCJAllowed;

// TODO: unclear semantics
// (annotations turned off to work with Java 1.4) @SCJAllowed(LEVEL_2)
public class ManagedThread extends RealtimeThread {

// (annotations turned off to work with Java 1.4)     @MemoryAreaEncloses(inner = { "this" }, outer = { "scheduling" })
// (annotations turned off to work with Java 1.4)     @SCJAllowed(LEVEL_2)
    public ManagedThread(PriorityParameters priority,
            StorageConfigurationParameters scp) {
        this(priority, scp, null);
    }

// (annotations turned off to work with Java 1.4)     @MemoryAreaEncloses(inner = { "this", "this", "this" }, outer = {
//            "schedule", "mem_info", "logic" })
// (annotations turned off to work with Java 1.4)     @SCJAllowed(LEVEL_2)
    public ManagedThread(PriorityParameters priority,
            StorageConfigurationParameters scp, Runnable logic) {
        // TODO: memory size?
        super(priority, null, null, new PrivateMemory(0), null, logic);
    }

// (annotations turned off to work with Java 1.4)     @SCJAllowed(LEVEL_2)
    public void start() {
        super.start();
    }

// (annotations turned off to work with Java 1.4)     @SCJAllowed(LEVEL_2)
    public void delay(HighResolutionTime time) {
        // TODO:
    }

// (annotations turned off to work with Java 1.4)     @SCJAllowed(LEVEL_2)
    public boolean terminationPending() {
        return false; // TODO:
    }
}
