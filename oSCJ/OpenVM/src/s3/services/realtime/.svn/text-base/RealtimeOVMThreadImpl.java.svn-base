
package s3.services.realtime;

import ovm.core.services.threads.OVMThreadContext;
import ovm.services.realtime.RealtimeOVMThread;
import s3.services.threads.TimedSuspensionOVMThreadImpl;
/**
 * A realtime thread implementation building on the 
 * {@link TimedSuspensionOVMThreadImpl} class. This class provides no new
 * functionality but provides a base class for all real-time threads.
 * We extend the {@link TimedSuspensionOVMThreadImpl} simply because typical
 * real-time thread usage will require the sleep/delay capabilities provided
 * by that class.
 * <p>Override the {@link #doRun} method to provide specific behaviour.
 * @author David Holmes
 *
 */
public abstract class RealtimeOVMThreadImpl 
    extends TimedSuspensionOVMThreadImpl 
    implements RealtimeOVMThread {

    public RealtimeOVMThreadImpl() {}

    public RealtimeOVMThreadImpl(OVMThreadContext ctx) {
        super(ctx);
    }

}

    










