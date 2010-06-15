package s3.services.java.realtime;

import ovm.core.domain.Oop;
import ovm.services.realtime.NoHeapRealtimeOVMThread;
import ovm.util.OVMError;
/**
 * An extension of {@link RealtimeJavaThreadImpl} that does nothing except
 * apply the {@link NoHeapRealtimeOVMThread} interface to flag this as
 * a no-heap thread. Other than heap access there is no difference between the
 * two types of thread.
 *
 * @author David Holmes
 */
public class NoHeapRealtimeJavaThreadImpl extends RealtimeJavaThreadImpl 
    implements NoHeapRealtimeOVMThread {


    volatile boolean heapChecksEnabled = true;

    /**
     * Construct a new real-time OVM thread bound to the given Java thread.
     * When this thread starts executing it will execute the startup
     * method of the Java thread.
     *
     * @param javaThread the Java thread to which this thread should be bound
     * @throws OVMError.IllegalArgument if <code>javaThread</code> is 
     * <code>null</code>
     *
     */
    protected NoHeapRealtimeJavaThreadImpl(Oop javaThread) {
         super(javaThread);
    }

    public void enableHeapChecks(boolean enabled) {
        heapChecksEnabled = enabled;
    }

    public boolean heapChecksEnabled() {
        return heapChecksEnabled;
    }
}
