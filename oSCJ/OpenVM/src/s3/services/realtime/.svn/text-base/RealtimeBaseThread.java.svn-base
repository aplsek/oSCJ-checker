/*
 * $Header: /p/sss/cvs/OpenVM/src/s3/services/realtime/RealtimeBaseThread.java,v 1.3 2006/04/20 15:48:41 baker29 Exp $
 *
 */
package s3.services.realtime;


/**
 * A convenience class that defines a realtime OVM thread type, 
 * that can utilise
 * {@link java.lang.Runnable runnables}, and has a name, just like a regular
 * Java thread. 
 * <p>Each thread must be given a name.
 * <p>To use this class either create an instance and bind a Runnable to it,
 * or subclass and override {@link #execute}.
 *
 */
public class RealtimeBaseThread extends RealtimeOVMThreadImpl {

    /** The runnable object we are bound to, if any */
    private final Runnable runnable;

    /** The name of this thread (for debugging purposes) */
    private final String name;

    /**
     * Creates a new thread with the given name. This is intended for use by
     * subclasses only.
     * @param name the name for this thread
     */
    protected RealtimeBaseThread(String name){
        this(name, null);
    }

    /**
     * Creates a new thread with the given name and bound to the given runnable
     * @param name the name for this thread
     * @param r the runnable whose run method should be executed by this thread
     */
    public RealtimeBaseThread(String name, Runnable r) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("invalid name");
        }
        this.name = name;
        this.runnable = r;
    }

    /** Returns the name of this thread */
    public String toString() {
        return "[RT]-" + name;
    }

    public final void doRun() {
        assert dispatcher.getCurrentThread() == this : "Current thread not right";

        try {
            execute();
        }
        catch(Throwable t) {
            d("Exception in thread " + name + ": " + t.toString());
            t.printStackTrace();
        }
        finally {
            d("Thread " + name + " terminating");
            dispatcher.terminateCurrentThread();
        }
    }

    public void execute() {
        if (runnable != null) {
            runnable.run();
        }
    }

}
        

        
    








