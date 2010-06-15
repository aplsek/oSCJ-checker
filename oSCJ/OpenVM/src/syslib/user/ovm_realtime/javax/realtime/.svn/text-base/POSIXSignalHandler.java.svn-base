package javax.realtime;
import org.ovmj.java.OVMSignals;
import org.ovmj.java.Opaque;
/** 
 * Use instances of {@link AsyncEvent} to handle POSIX signals. Usage:
 * <pre>POSIXSignalHandler.addHandler(SIGINT, intHandler);</pre>
 * This class is required to be implemented only if the underlying
 * operating system supports POSIX signals. 
 *
 * <h3>OVM Notes</h3>
 * <p>The signals defined here are not necessarily POSIX signals and a number
 * will be deprecated in the RTSJ 1.0.1 update. This API is deficient because
 * it doesn't take into account the fact that it is impossible to install a
 * handler for a number of signals (and impractical for many others), or that
 * a given signal may not even exist on a particular platform. 
 * If a handler can't be installed we print a diagnostic and ignore the
 * requesty. In the future this should throw an exception.
 *
 * @author David Holmes
 */

public final class POSIXSignalHandler {
	
    /** Hangup. */
    public static final int SIGHUP = OVMSignals.OVM_SIGHUP;
    /** Interrupt (rubout). */
    public static final int SIGINT = OVMSignals.OVM_SIGINT;
    /** Quit (ASCII FS). */
    public static final int SIGQUIT = OVMSignals.OVM_SIGQUIT;
    /** Illegal instruction (not reset when caught). */
    public static final int SIGILL = OVMSignals.OVM_SIGILL;
    /** Trace trap (not reset when caught). */
    public static final int SIGTRAP = OVMSignals.OVM_SIGTRAP;
    /** IOT instruction. */
    public static final int SIGIOT = OVMSignals.OVM_SIGIOT;
    /** Used by abort, replace SIGIOT in the future. */
    public static final int SIGABRT = OVMSignals.OVM_SIGABRT;
    /** EMT instruction. */
    public static final int SIGEMT = OVMSignals.OVM_SIGEMT;
    /** Floating point exception. */
    public static final int SIGFPE = OVMSignals.OVM_SIGFPE;
    /** Kill (cannot be caught or ignored). */
    public static final int SIGKILL = OVMSignals.OVM_SIGKILL;
    /** Bus error. */
    public static final int SIGBUS = OVMSignals.OVM_SIGBUS;
    /** Segmentation violation. */
    public static final int SIGSEGV = OVMSignals.OVM_SIGSEGV;
    /** Bad argument to system call. */
    public static final int SIGSYS = OVMSignals.OVM_SIGSYS;
    /** Write on a pipe with no one to read it. */
    public static final int SIGPIPE = OVMSignals.OVM_SIGPIPE;
    /** Alarm clock. */
    public static final int SIGALRM = OVMSignals.OVM_SIGALRM;
    /** Software termination signal from kill. */
    public static final int SIGTERM = OVMSignals.OVM_SIGTERM;
    /** User defined signal	= OVMSignals.OVM_SIG 1. */
    public static final int SIGUSR1 = OVMSignals.OVM_SIGUSR1;
    /** User defined signal	= OVMSignals.OVM_SIG 2. */
    public static final int SIGUSR2 = OVMSignals.OVM_SIGUSR2;
    /** Child status change. */
    public static final int SIGCLD = OVMSignals.OVM_SIGCLD;
    /** Child status change alias (POSIX). */
    public static final int SIGCHLD = OVMSignals.OVM_SIGCHLD;
    /** Power-fail restart. */
    public static final int SIGPWR = OVMSignals.OVM_SIGPWR;
    /** Window size change. */
    public static final int SIGWINCH = OVMSignals.OVM_SIGWINCH;
    /** Urgent socket condition. */
    public static final int SIGURG = OVMSignals.OVM_SIGURG;
    /** Pollable event occured. */
    public static final int SIGPOLL = OVMSignals.OVM_SIGPOLL;
    /** Socket I/O possible (SIGPOLL alias). */
    public static final int SIGIO = OVMSignals.OVM_SIGIO;
    /** Stop (cannot be caught or ignored). */
    public static final int SIGSTOP = OVMSignals.OVM_SIGSTOP;
    /** User stop requested from tty. */
    public static final int SIGTSTP = OVMSignals.OVM_SIGTSTP;
    /** Stopped process has been continued. */
    public static final int SIGCONT = OVMSignals.OVM_SIGCONT;
    /** Background tty read attempted. */
    public static final int SIGTTIN = OVMSignals.OVM_SIGTTIN;
    /** Background tty write attempted. */
    public static final int SIGTTOU = OVMSignals.OVM_SIGTTOU;
    /** Virtual timer expired. */
    public static final int SIGVTALRM = OVMSignals.OVM_SIGVTALRM;
    /** Profiling timer expired. */
    public static final int SIGPROF = OVMSignals.OVM_SIGPROF;
    /** Exceeded cpu limit. */
    public static final int SIGXCPU = OVMSignals.OVM_SIGXCPU;
    /** Exceeded file size limit. */
    public static final int SIGXFSZ = OVMSignals.OVM_SIGXFSZ;
    /** Process's lwps are blocked. */
    public static final int SIGWAITING = OVMSignals.OVM_SIGWAITING;
    /** Special signal used by thread library. */
    public static final int SIGLWP = OVMSignals.OVM_SIGLWP;
    /** Special signal used by CPR. */
    public static final int SIGFREEZE = OVMSignals.OVM_SIGFREEZE;
    /** Special signal used by CPR. */
    public static final int SIGTHAW = OVMSignals.OVM_SIGTHAW;
    /** Thread cancellation signal used by libthread. */
    public static final int SIGCANCEL = OVMSignals.OVM_SIGCANCEL;
    /** Resource lost (e.g., record-lock lost). */
    public static final int SIGLOST = OVMSignals.OVM_SIGLOST;



    /** 
     * Array of events corresponding to each signal value. We don't expect
     * many to actually be used. Creating actual AsyncEvents is the easiest
     * way to get consistent treatment of AsyncEventHandlers regardless of how
     * they are released. We use our internal thread to watch for signals and
     * then fire() the corresponding event. Once an event is installed we
     * don't bother removing it.
     * <p>We also use this as our synchronization lock.
     */
    static volatile AsyncEvent[] events = new AsyncEvent[OVMSignals.NSIGNALS];

    /**
     * Count of the number of handlers installed per signal. When it drops
     * back to zero we remove the watch.
     */
    static volatile int[] handlers = new int[OVMSignals.NSIGNALS];


    /**
     * Total number of handlers installed. The internal thread blocks while
     * it is zero.
     */
    static int nHandlers = 0;

    /**
     * Our signal watcher
     */
    static final Opaque watcher = LibraryImports.createSignalWatcher();

    /** Our thread that waits for signals */
    static volatile SignalThread signalThread;

    /** Thread class for threads that waits for signals */
    static class SignalThread extends RealtimeThread.VMThread {

        SignalThread() {
            // this is the safest default until we figure out the exact
            // rules for this in the RTSJ. It's easy for an application to
            // create a thread to keep the VM alive. In contrast it's
            // effectively impossible for an application to know that it has
            // to call System.exit to terminate.
            this.setDaemon(true);
        }

        public void run() {
            int[] counts = new int[OVMSignals.NSIGNALS];
            // typical autonomous loop, even though at this stage nothing
            // can interrupt us
            while(!Thread.interrupted()) {
                LibraryImports.waitForSignal(watcher, counts);
                synchronized(events) {
                    for (int i = 0; i < counts.length; i++) {
                        if (counts[i] > 0) {
                            //                            System.out.println("Processing count of " + counts[i]);
                            for (int j = counts[i]; j > 0; j--) {
                                events[i].fire();
                            }
                        }
                    }

                    // it could be that all handlers have been removed now
                    try {
                        while (nHandlers == 0) {
                            events.wait();
                        }
                    }
                    catch(InterruptedException ex) {
                        return;
                    }
                }
                // Our priority means the last handler can't have been
                // removed since we released the lock. But in anycase
                // waitForSignal will just return immediately and we'll
                // hit the wait() again.
            }
        }
    }


    
    // only called within synchronized(events)
    static void addWatch(int sig) {
        if (nHandlers++ ==0) {
            events.notify();
        }
        if (handlers[sig]++ == 0) {
            LibraryImports.addSignalWatch(watcher, sig);
            if (signalThread == null) {
                signalThread = new SignalThread();
                signalThread.start();
            }
        }
    }

    // only called within synchronized(events)
    static void removeWatch(int sig) {
        if (--handlers[sig] == 0) {
            LibraryImports.removeSignalWatch(watcher, sig);
        }
        nHandlers--;
    }

    // only called within synchronized(events)
    static void clearWatch(int sig) {
        nHandlers -= handlers[sig];
        handlers[sig] = 0;
        LibraryImports.removeSignalWatch(watcher, sig);
    }


    /**
     * Add the given {@link AsyncEventHandler} to the list of handlers of the 
     * {@link AsyncEvent} of the given signal.
     * @param signal One of the POSIX signals from this  
     * (e.g., <code>this.SIGLOST</code>).
     * @param handler An {@link AsyncEventHandler} which will be scheduled 
     * when the given signal occurs.
     */
    public static void addHandler(int signal, AsyncEventHandler handler) {
        if (!LibraryImports.canMonitorSignal(signal)) {
            System.err.println("WARNING: Handlers can not be installed for "
                               + OVMSignals.sigNames[signal] + " - IGNORED");
            return;
        }
        if (handler == null) {
            return;
        }
        synchronized(events) {
            // lazily construct events
            if (events[signal] == null) {
                events[signal] = new AsyncEvent();
            }
            if (events[signal].handlers.add(handler)) {
                addWatch(signal);
            }
        }
    }
	    
    /**
     * Remove the given {@link AsyncEventHandler} from the list of handlers 
     * of the {@link AsyncEvent} of the given signal.
     * @param signal One of the POSIX signals from this 
     * (e.g., <code>this.SIGLOST</code>).
     * @param handler the {@link AsyncEventHandler} to be removed
     */
    public static void removeHandler(int signal, AsyncEventHandler handler) {
        if (signal < 0 || signal > events.length) {
            throw new IllegalArgumentException("invalid signal value");
        }
        if (handler == null) {
            return;
        }
        synchronized(events) {
            if (events[signal] != null) {
                if (events[signal].handlers.remove(handler)) {
                    removeWatch(signal);
                }
            }
        }
    }
    
    /**
     *  Set the given {@link AsyncEventHandler} as the handler of the 
     * {@link AsyncEvent} of the given signal.
     * @param signal One of the POSIX signals from this  
     * (e.g., <code>this.SIGLOST</code>).
     * @param handler An {@link AsyncEventHandler} which will be scheduled 
     * when the given signal occurs.
     * If h is null then no handler will be associated with this 
     * (i.e., remove all handlers).
     */
    public static void setHandler(int signal,  AsyncEventHandler handler) {
        if (!LibraryImports.canMonitorSignal(signal)) {
            throw new IllegalArgumentException("invalid signal value");
        }
        synchronized(events) {
            // ignore if no handler previously set
            if (events[signal] != null) {
                events[signal].handlers.clear();
                if (handler != null) {
                    events[signal].handlers.add(handler);
                    handlers[signal] = 1;
                }
                else {
                    clearWatch(signal);
                }
            }
        }
    }

}
