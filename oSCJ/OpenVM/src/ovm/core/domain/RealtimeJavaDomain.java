package ovm.core.domain;

/**
 * The <tt>RealtimeJavaDomain</tt> encapsulates aspects of real-time
 * functionality needed in both the executive and user domains for real-time
 * support. The requirements naturally flow from the user domain notion of
 * real-time support.
 *
 */
public interface RealtimeJavaDomain extends JavaDomain {

    /**
     * Invoked by the <tt>MemoryManager</tt> when a read barrier (such
     * as checking for a heap reference) has failed. The domain naturally
     * needs to know exactly what the barrier is doing and what the
     * appropriate response will be - typically throwing an exception.
     */
    void readBarrierFailed();

    /**
     * Invoked by the <tt>MemoryManager</tt> when a store barrier (such
     * as checking for a scoped reference) has failed. The domain naturally
     * needs to know exactly what the barrier is doing and what the
     * appropriate response will be - typically throwing an exception.
     */
    void storeBarrierFailed();

    
}
