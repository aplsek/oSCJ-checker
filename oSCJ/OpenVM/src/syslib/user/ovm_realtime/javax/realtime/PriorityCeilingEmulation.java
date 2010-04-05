package javax.realtime;
/**
 * Monitor control class specifying use of the priority ceiling emulation
 * protocol for monitor objects.  Objects under the influence of this
 * protocol have the effect that a thread entering the monitor has its
 * effective priority -- for priority-based dispatching -- raised to the
 * ceiling on entry, and is restored to its previous effective priority when
 * it exits the monitor. See also {@link MonitorControl} and 
 *{@link PriorityInheritance}.
 *
 * <p>To set a priority ceiling <code>n</code> for the monitor of
 * object <code>obj</code> (when this is not the default
 * monitor policy) you would do:
 * <pre><code>    PriorityCeilingEmulation pcm = new PriorityCeilingEmulation(n);
 *    MonitorControl.setMonitorControl(obj, pcm);
 * </code></pre>
 *
 * @since RTSJ V1.0
 */
public class PriorityCeilingEmulation extends MonitorControl {

    /** The priority ceiling of this policy object */
    private final int ceiling;


    /**
     * Create a <code>PriorityCeilingEmulation</code> object with the given 
     * ceiling.
     * @param ceiling the priority ceiling value.
     */
    public PriorityCeilingEmulation(int ceiling) {
	this.ceiling = ceiling;
    }
    
    /**
     * Return the priority ceiling for this 
     * <code>PriorityCeilingEmulation</code> object.
     * @return the priority ceiling for this 
     * <code>PriorityCeilingEmulation</code> object.
     *
     */
    public int getDefaultCeiling() {
	return ceiling;
    }
}


