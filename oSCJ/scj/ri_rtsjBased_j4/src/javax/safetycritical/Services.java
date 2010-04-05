package javax.safetycritical;

// (annotations turned off to work with Java 1.4) import static javax.safetycritical.annotate.Level.LEVEL_1;
// (annotations turned off to work with Java 1.4) import static javax.safetycritical.annotate.Level.LEVEL_2;

import javax.safetycritical.annotate.Level;
import javax.realtime.AbsoluteTime;
import javax.realtime.Clock;
import javax.realtime.RealtimeThread;
import javax.realtime.RelativeTime;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.Level;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.SCJAllowed;

/**
 * System wide information
 * 
 * TODO: implement this
 */
// (annotations turned off to work with Java 1.4) @SCJAllowed
public class Services {

    private static RelativeTime dest = new RelativeTime();
    private static AbsoluteTime time1 = new AbsoluteTime();
    private static AbsoluteTime time2 = new AbsoluteTime();
    private static RelativeTime ZERO = new RelativeTime(0, 0);

    /**
     * Captures the stack back trace for the current thread into its
     * thread-local stack back trace buffer and remembers that the current
     * contents of the stack back trace buffer is associated with the object
     * represented by the association argument. The size of the stack back trace
     * buffer is determined by the StorageConfigurationParameters object that is
     * passed as an argument to the constructor of the corresponding
     * Schedulable. If the stack back trace buffer is not large enough to
     * capture all of the stack back trace information, the information is
     * truncated in an implementation dependent manner.
     */
// (annotations turned off to work with Java 1.4)     @SCJAllowed
    public static void captureBackTrace(Throwable association) {
        // TODO:
    }

    /**
     * @return the default ceiling priority The value is the highest software
     *         priority.
     */
// (annotations turned off to work with Java 1.4)     @SCJAllowed(LEVEL_1)
    public static int getDefaultCeiling() {
        // TODO: What is the default?
        return 30;
    }

    /**
     * sets the ceiling priority of obj the priority can be in the software or
     * hardware priority range.
     * 
     * @throws IllegalThreadState
     *             if called outside the mission phase
     */
// (annotations turned off to work with Java 1.4)     @SCJAllowed(LEVEL_1)
    public static void setCeiling(Object obj, int priority) {
        if (MissionManager.getCurrentMissionManager().getMission()._phase != Mission.Phase.EXECUTE)
            throw new IllegalThreadStateException();

        // TODO: and then ...
    }

    /**
     * Every interrupt has an implementation-defined integer id.
     * 
     * @return The priority of the code that the first-level interrupts code
     *         executes. The returned value is always greater than
     *         PriorityScheduler.getMaxPriority().
     * @throws IllegalArgument
     *             if unsupported InterruptId
     */
// (annotations turned off to work with Java 1.4)     @SCJAllowed(LEVEL_1)
    public static int getInterruptPriority(int interruptID) {
        // TODO: define interrupt IDs first
        return 33;
    }

// (annotations turned off to work with Java 1.4)     @SCJAllowed(LEVEL_1)
    public static void registerInterruptHandler(int interruptID,
            InterruptHandler handler) {
        // TODO: same as above
    }

    /*
     * The deployment level
     */
    public static Level getDeploymentLevel() {
        // return MissionManager.getCurrentMissionManager().getLevel();
        return Level.LEVEL_1;
    }

    /**
     * This is like sleep except that it is not interruptable and it uses
     * nanoseconds instead of milliseconds.
     * 
     * @param delay
     *            is the number of nanoseconds to suspend
     * 
     *            TBD: should this be called suspend or deepSleep to no have a
     *            ridiculously long name?
     * 
     *            TBD: should not be a long nanoseconds?
     */
// (annotations turned off to work with Java 1.4)     @SCJAllowed(LEVEL_1)
    public static void sleepNonInterruptable(RelativeTime delay) {
        // TODO: Is this implementation good? Can we do it in a simple and
        // better way on low level. We have to allocate new objects here (for
        // time operations)
        Clock clock = delay.getClock();

        while (true) {
            clock.getTime(time1);
            try {
                RealtimeThread.sleep(delay);
            } catch (InterruptedException e) {
                clock.getTime(time2);
                delay.subtract(time2.subtract(time1, dest), delay);
                if (delay.compareTo(ZERO) > 0)
                    continue;
            }
            break;
        }
    }

    /**
     * This is like sleep except that it is not interruptable and it uses
     * nanoseconds instread of milliseconds.
     * 
     * @param delay
     *            is the number of nanoseconds to suspend
     * 
     * @param busy
     *            indicated that the wait is a busy wait when true and a clock
     *            based wait when not.
     * 
     * @throws InterruptedException
     * 
     *             TBD: should this be called suspend or deepSleep to no have a
     *             ridiculously long name?
     * 
     *             TBD: should not be a long nanoseconds?
     */
// (annotations turned off to work with Java 1.4)     @SCJAllowed(LEVEL_1)
    public static void sleepNonInterruptable(RelativeTime delay, boolean busy) {
        if (busy) {
            Clock clock = delay.getClock();

            clock.getTime(time1);
            time1.add(delay, time2);

            do {
                clock.getTime(time1);
            } while (time2.compareTo(time1) > 0);
        } else
            sleepNonInterruptable(delay);
    }

    /**
     * This is like sleep except that it uses nanoseconds instead of
     * milliseconds.
     * 
     * @param delay
     *            is the number of nanoseconds to suspend
     * 
     * @throws InterruptedException
     * 
     *             TBD: should this be called nap to distinguish it from
     *             Thread.sleep?
     * 
     *             TBD: should not be a long nanoseconds?
     */
// (annotations turned off to work with Java 1.4)     @SCJAllowed(LEVEL_2)
    public static void sleep(RelativeTime delay) throws InterruptedException {
        RealtimeThread.sleep(delay);
    }

    /**
     * This is like sleep except that it uses nanoseconds instead of
     * milliseconds.
     * 
     * @param delay
     *            is the number of nanoseconds to suspend
     * 
     * @param busy
     *            indicated that the wait is a busy wait when true and a clock
     *            based wait when not.
     * 
     * @throws InterruptedException
     * 
     *             TBD: should this be called nap to distinguish it from
     *             Thread.sleep?
     * 
     *             TBD: should not be a long nanoseconds?
     */
// (annotations turned off to work with Java 1.4)     @SCJAllowed(LEVEL_2)
    public static void sleep(RelativeTime delay, boolean busy)
            throws InterruptedException {
        if (busy) {
            sleepNonInterruptable(delay);
            if (Thread.interrupted())
                throw new InterruptedException();
        } else
            sleep(delay);
    }
}
