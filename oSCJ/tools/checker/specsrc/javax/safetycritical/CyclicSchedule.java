package javax.safetycritical;

import javax.realtime.RelativeTime;
import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.MemoryAreaEncloses;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Allocate.Area;

@SCJAllowed
public class CyclicSchedule {

    @SCJAllowed
    final public static class Frame {
        /**
         * Allocates private copies of the handlers within the same memory
         * area as this. The elements within the copy of the handlers array are
         * the exact same elements as in the handlers array. Thus, it is
         * essential that the elements of the handlers array reside in memory
         * areas that enclose this.
         */
        @Allocate( { Area.THIS })
        @MemoryAreaEncloses(inner = { "this", "this" }, outer = { "start",
                "handlers" })
        @SCJAllowed
        public Frame(RelativeTime duration, PeriodicEventHandler[] handlers) {
        }

        /**
         * Performs no allocation. Returns a reference to the internal
         * representation of the frame duration.
         */
        @SCJAllowed
        public RelativeTime getDuration() {
            return null;
        }

        /**
         * @return an array allocated in the memory area of the caller. This
         * array holds references to the same PeriodicEventHandler objects that
         * represent
         * <p>
         * It's not safe to return internal representation of handlers, because
         * that would allow application code to compromise the infrastructure.
         */
        @Allocate( { Area.CURRENT })
        @SCJAllowed
        public PeriodicEventHandler[] getHandlers() {
            return null;
        }
    }

    /**
     * Constructs a cyclic schedule.
     * <p>
     * Does not allow frames to escape local variables. Allocates
     * and initializes private copies of frames within the same
     * scope as this.
     */
    @Allocate( { Area.THIS })
    @MemoryAreaEncloses(inner = { "this"}, outer = { "frames" })
    @SCJAllowed
    public CyclicSchedule(Frame[] frames) {
    }

    /**
     * @return a newly allocated RelativeTime object, taken from the current
     * memory area of the caller. This is the sum of the duration
     * of all the frames' durations
     *
    @Allocate( { Area.CURRENT })
    @SCJAllowed
    public RelativeTime getCycleDuration() {
        return null;
    }
    */
    
    /**
     * The returned array will contain references to the same Frame
     * objects that are used internally by the infrastructure. Thus, this object
     * must enclose the caller's memory area.
     */
    protected Frame[] getFrames() {
        return null;
    }
}
