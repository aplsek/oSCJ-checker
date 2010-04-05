/*
 * $Header: /p/sss/cvs/OpenVM/src/s3/services/threads/BaseThread.java,v 1.6 2006/04/20 15:48:41 baker29 Exp $
 *
 */
package s3.services.threads;


/**
 * A convenience class that defines an OVM thread type, that can utilise
 * {@link java.lang.Runnable runnables}, and has a name, just like a regular
 * Java thread. It is also a priority OVM thread.
 * <p>Each thread must be given a name.
 * <p>To use this class either create an instance and bind a Runnable to it,
 * or subclass and override {@link #run}.
 *
 * @deprecated use {@link JLThread} instead
 *
 */
public class BaseThread extends BasicPriorityOVMThreadImpl {

    /** The runnable object we are bound to, if any */
    private final Runnable runnable;

    /** The name of this thread (for debugging purposes) */
    private final String name;

    /**
     * Creates a new thread with the given name. This is intended for use by
     * subclasses only.
     * @param name the name for this thread
     */
    protected BaseThread(String name){
        this(name, null);
    }

    /**
     * Creates a new thread with the given name and bound to the given runnable
     * @param name the name for this thread
     * @param r the runnable whose run method should be executed by this thread
     */
    public BaseThread(String name, Runnable r) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("invalid name");
        }
        this.name = name;
        this.runnable = r;
    }

    /** Returns the name of this thread */
    public String toString() {
        return name;
    }

    public final void doRun() {
        assert dispatcher.getCurrentThread() == this : "Current thread not right";

        try {
            run();
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

    public void run() {
        if (runnable != null) {
            runnable.run();
        }
    }

}
        

        
    








