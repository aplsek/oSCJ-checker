import java.util.Vector;

import javax.realtime.MemoryArea;
import javax.realtime.MemoryParameters;
import javax.realtime.NoHeapRealtimeThread;
import javax.realtime.ProcessingGroupParameters;
import javax.realtime.RealtimeThread;
import javax.realtime.ReleaseParameters;
import javax.realtime.SchedulingParameters;


/**
 * @author marek prochazka
 */
public class ThreadUtils {

    protected Vector threads;
    protected Configuration config;

    public ThreadUtils(Configuration config) {
        threads = new Vector(100);
        this.config = config;
    }

    /**
	 * Gets RealtimeThread or NoHeapRealtimeThread depending on the <code>isNHRT</code>
	 * flag.
	 * 
	 * @param isNHRT
	 *            if true NHRT, else RT
	 * @param logic
	 * @return newly created RealtimeThread or NoHeapRealtimeThread
	 */
    public static RealtimeThread getRealtimeThread(
        boolean isNHRT,
        Runnable logic) {
        return getRealtimeThread(
            isNHRT,
            null,
            null,
            null,
            RealtimeThread.getCurrentMemoryArea(),
            null,
            logic);
    }

    /**
	 * Gets RealtimeThread or NoHeapRealtimeThread depending on the <code>isNHRT</code>
	 * flag.
	 * 
	 * @param isNHRT
	 *            if true NHRT, else RT
	 * @param scheduling
	 * @param release
	 * @param logic
	 * @return newly created RealtimeThread or NoHeapRealtimeThread
	 */
    public static RealtimeThread getRealtimeThread(
        boolean isNHRT,
        SchedulingParameters scheduling,
        ReleaseParameters release,
        Runnable logic) {
        return getRealtimeThread(
            isNHRT,
            scheduling,
            release,
            null,
            RealtimeThread.getCurrentMemoryArea(),
            null,
            logic);
    }

    /**
	 * Gets RealtimeThread or NoHeapRealtimeThread depending on the <code>isNHRT</code>
	 * flag.
	 * 
	 * @param isNHRT
	 *            if true NHRT, else RT
	 * @param scheduling
	 * @param release
	 * @param memory
	 * @param area
	 * @param group
	 * @param logic
	 * @return newly created RealtimeThread or NoHeapRealtimeThread
	 */
    public static RealtimeThread getRealtimeThread(
        boolean isNHRT,
        SchedulingParameters scheduling,
        ReleaseParameters release,
        MemoryParameters memory,
        MemoryArea area,
        ProcessingGroupParameters group,
        Runnable logic) {
//        Util.pln("Scheduling parameters in " + MAUtils.getMA(scheduling));
//        Util.pln("Release parameters in " + MAUtils.getMA(release));
//        Util.pln("Memory in " + MAUtils.getMA(memory));
//        Util.pln("Memory area is " + area);
//        Util.pln("Logic in " + MAUtils.getMA(logic));
        RealtimeThread t =
            isNHRT
                ? new NoHeapRealtimeThread(
                    scheduling,
                    release,
                    memory,
                    area,
                    group,
                    logic)
                : new RealtimeThread(
                    scheduling,
                    release,
                    memory,
                    area,
                    group,
                    logic);
        return t;
    }
    /**
	 * Gets RealtimeThread or NoHeapRealtimeThread depending on current
	 * configuration.
	 * 
	 * @param logic
	 * @return newly created RealtimeThread or NoHeapRealtimeThread
	 */
    public RealtimeThread getRealtimeThread(Runnable logic) {
        return getRealtimeThread(
            config.nhrt,
            null,
            null,
            null,
            RealtimeThread.getCurrentMemoryArea(),
            null,
            logic);
    }
    /**
	 * Gets RealtimeThread or NoHeapRealtimeThread depending on current
	 * configuration.
	 * 
	 * @param scheduling
	 * @param release
	 * @param logic
	 * @return newly created RealtimeThread or NoHeapRealtimeThread
	 */
    public RealtimeThread getRealtimeThread(
        SchedulingParameters scheduling,
        ReleaseParameters release,
        Runnable logic) {
        return getRealtimeThread(
            config.nhrt,
            scheduling,
            release,
            null,
            RealtimeThread.getCurrentMemoryArea(),
            null,
            logic);
    }
    /**
	 * Gets RealtimeThread or NoHeapRealtimeThread depending on current
	 * configuration.
	 * 
	 * @param scheduling
	 * @param release
	 * @param memory
	 * @param area
	 * @param group
	 * @param logic
	 * @return newly created RealtimeThread or NoHeapRealtimeThread
	 */
    public RealtimeThread getRealtimeThread(
        SchedulingParameters scheduling,
        ReleaseParameters release,
        MemoryParameters memory,
        MemoryArea area,
        ProcessingGroupParameters group,
        Runnable logic) {
        return getRealtimeThread(
            config.nhrt,
            scheduling,
            release,
            memory,
            area,
            group,
            logic);
    }
    /**
	 * Creates and starts RealtimeThread or NoHeapRealtimeThread according to
	 * the configuration.
	 * 
	 * @param logic
	 * @return newly created RealtimeThread or NoHeapRealtimeThread
	 */
    public void startRealtimeThread(Runnable logic) {
        start(getRealtimeThread(logic));
    }

    /**
	 * Creates and starts RealtimeThread or NoHeapRealtimeThread according to
	 * the configuration.
	 * 
	 * @param scheduling
	 * @param release
	 * @param logic
	 * @return newly created RealtimeThread or NoHeapRealtimeThread
	 */
    public void startRealtimeThread(
        SchedulingParameters scheduling,
        ReleaseParameters release,
        Runnable logic) {
        start(getRealtimeThread(scheduling, release, logic));
    }

    /**
	 * Creates and starts RealtimeThread or NoHeapRealtimeThread according to
	 * the configuration.
	 * 
	 * @param scheduling
	 * @param release
	 * @param memory
	 * @param area
	 * @param group
	 * @param logic
	 * @return newly created RealtimeThread or NoHeapRealtimeThread
	 */
    public void startRealtimeThread(
        SchedulingParameters scheduling,
        ReleaseParameters release,
        MemoryParameters memory,
        MemoryArea area,
        ProcessingGroupParameters group,
        Runnable logic) {
        start(
            getRealtimeThread(scheduling, release, memory, area, group, logic));
    }
    /**
	 * Starts given thread.
	 * 
	 * @param thread
	 */
    public void start(Thread thread) {
        System.err.println("Starting thread " + thread);
        threads.add(thread);
        thread.start();
       }
    /**
	 * Joins given thread started via one of the <code>startRealtimeThread</code>
	 * methods.
	 */
    public void join(Thread thread) {
        if (threads.remove(thread))
            try {
                thread.join();
            } catch (InterruptedException e) {
                // TODO: What to do here?
            }
    }
    /**
	 * Joins all threads started via one of the <code>startRealtimeThread</code>
	 * methods.
	 */
    public void join() {
        while (!threads.isEmpty()) {
            try {
                ((Thread) threads.elementAt(0)).join();
                threads.remove(0);
            } catch (InterruptedException e) {
                // TODO: What to do here?
            }
        }
    }
    /**
	 * Destroys all threads started via one of the <code>startRealtimeThread</code>
	 * methods.
	 */
    public void stop() {
        while (!threads.isEmpty()) {
            ((Thread) threads.elementAt(0)).destroy();
            threads.remove(0);
        }
    }
}