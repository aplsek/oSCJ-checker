package javax.realtime;

import static javax.safetycritical.annotate.Level.LEVEL_1;
import javax.safetycritical.annotate.BlockFree;
import javax.safetycritical.annotate.SCJAllowed;

public abstract class MemoryArea {
    public Object newInstance(Class clazz) { return null; }
    public void executeInArea(Runnable r) {
    }
    public void enter() {
    }
    public void enter(Runnable r) {
    }
    public long memoryConsumed() {
        return 0;
    }
    public long memoryRemaining() {
        return 0;
    }
    public static MemoryArea getMemoryArea(Object o) {
        return null;
    }
}
