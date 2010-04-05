package ovm.services.realtime;

/**
 * An interface for identifying those real-time threads that are not
 * permitted to read references to heap allocated objects.
 *
 * @author David Holmes
 *
 */
public interface NoHeapRealtimeOVMThread extends RealtimeOVMThread {

    /**
     * Enables or disables heap checking based on the supplied argument
     */
    public void enableHeapChecks(boolean enabled);

    /**
     * Queries whether heap checks are currently enabled
     */
    public boolean heapChecksEnabled();
}
