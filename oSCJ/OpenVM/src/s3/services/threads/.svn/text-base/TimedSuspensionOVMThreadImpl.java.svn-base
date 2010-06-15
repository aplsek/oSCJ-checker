package s3.services.threads;

import ovm.core.services.threads.OVMThreadContext;

import s3.util.queues.DelayableSingleLinkDeltaElement;
import s3.util.queues.SingleLinkDeltaElement;
import s3.util.PragmaNoPollcheck;
import ovm.core.services.memory.PragmaNoBarriers;
import s3.util.Visitor;
/**
 * An extension to the {@link BasicPriorityOVMThreadImpl} that supports the
 * {@link DelayableSingleLinkDeltaElement} interface for use with the timer 
 * queues maintained by the {@link s3.core.services.timer.TimerManagerImpl 
 * timer manager}.
 * <p>The <tt>delayExpired</tt> method for this class invokes the 
 * <tt>visit</tt> method of any visitor that has currently been set using
 * <tt>setVisitor</tt>. Typically the correct visitor will be set before
 * this thread is submitted for delay.
 * 
 * <p>Subclass this class and override {@link #doRun} to provide the behaviour
 * desired.
 * <p>Ideally this class would be a mix-in class as its functionality is
 * independent of the other thread functions. But as Java does not support
 * mix-ins or multiple-inheritance we have to duplicate this class for each
 * thread type that we want. 
 *
 * <p>This class is not generally thread-safe. It is expected that the caller
 * ensures exclusive access to this thread - typically we are used by the
 * thread manager, indirectly via the dispatcher, and the dispatcher ensures
 * thread safety.
 * 
 * @author David Holmes
 *
 */
public abstract class TimedSuspensionOVMThreadImpl 
    extends BasicPriorityOVMThreadImpl
    implements DelayableSingleLinkDeltaElement {

    /** Next element in the delta queue */
    SingleLinkDeltaElement nextDelta = null;

    /** Delta value used when in a delta queue */
    long delta = 0;


    public TimedSuspensionOVMThreadImpl(OVMThreadContext ctx) {
        super(ctx);
    }

    public TimedSuspensionOVMThreadImpl() {}

    /* Implementation methods for SingleLinkDeltaElement */

    public void setNextDelta(SingleLinkDeltaElement next) 
        throws PragmaNoPollcheck, PragmaNoBarriers {
        assert next != this : "next == this";
        this.nextDelta = next;
    }

    public SingleLinkDeltaElement getNextDelta() throws PragmaNoPollcheck {
        return this.nextDelta;
    }

    public long getDelta() throws PragmaNoPollcheck {
        return this.delta;
    }

    public long subtractDelta(long val) throws PragmaNoPollcheck {
        this.delta -= val;
        return this.delta;
    }

    public long addDelta(long val) throws PragmaNoPollcheck {
        this.delta += val;
        return this.delta;
    }

    public void setDelta(long value) throws PragmaNoPollcheck {
        this.delta = value;
    }


    /** Reference to the Visitor to apply to this when its delay expires
        in a timer queue
    */
    Visitor visitor;

    public Visitor getVisitor() throws PragmaNoPollcheck {
        return visitor;
    }

    public void setVisitor(Visitor v) throws PragmaNoBarriers, 
                                             PragmaNoPollcheck {
        visitor = v;
    }

    public void delayExpired() throws PragmaNoPollcheck {
        visitor.visit(this);
    }
}

    






