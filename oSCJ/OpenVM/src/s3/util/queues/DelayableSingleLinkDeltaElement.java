package s3.util.queues;

import ovm.core.services.timer.TimerManager;

/**
 * Marker interface that combines <tt>DelayableObject</tt> with
 * <tt>SingleLinkDeltaElement</tt>.
 */
public interface DelayableSingleLinkDeltaElement 
    extends TimerManager.DelayableObject, SingleLinkDeltaElement {
}
