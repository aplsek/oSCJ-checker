package javax.safetycritical;

// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.BlockFree;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.SCJAllowed;

// (annotations turned off to work with Java 1.4) @SCJAllowed
public class PriorityScheduler extends javax.realtime.PriorityScheduler {
    
    private static PriorityScheduler _instance = new PriorityScheduler();

    private PriorityScheduler() {
    }

    public static javax.realtime.PriorityScheduler instance() {
        return  _instance;
    }

// (annotations turned off to work with Java 1.4)     @BlockFree
// (annotations turned off to work with Java 1.4)     @SCJAllowed
    public int getMaxHardwarePriority() {
        return 2000;
    }

// (annotations turned off to work with Java 1.4)     @BlockFree
// (annotations turned off to work with Java 1.4)     @SCJAllowed
    public int getMinHardwarePriority() {
        return 1000;
    }
}
