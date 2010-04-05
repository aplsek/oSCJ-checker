package javax.realtime;
/**
 * Monitor control class specifying use of the priority inheritance
 * protocol for object monitors.  Objects under the influence of this
 * protocol have the effect that a thread entering the monitor will boost
 * the effective priority of the thread in the monitor to its own effective
 * priority.  When that thread exits the monitor, its effective priority
 * will be restored to its previous value. See also {@link MonitorControl} 
 * and {@link PriorityCeilingEmulation}
 * 
 * @since RTSJ V1.0
 */
public class PriorityInheritance extends MonitorControl {

    /** The singleton instance of this class */
    private static PriorityInheritance instance = new PriorityInheritance();
  
    /**
     * Construct an instance of <code>PriorityInheritance</code>.
     * <p>This constructor should never be used as this is a singleton
     * class and the instance should be retrieved using the
     * {@link #instance} method.
     *
     */
    public PriorityInheritance() {}

    /**
     * Return a reference to the singleton <code>PriorityInheritance</code>
     * instance.
     * @return reference to the singleton <code>PriorityInheritance</code>
     * instance
     */
    public static PriorityInheritance instance() {
        // due to our superclass invoking this method, if this class
        // was being initialized first, our superclass would see null
        // for the instance field, hence in that case we do the
        // initialization here - which means the field can't be final
        if (instance == null) {
            instance = new PriorityInheritance();
        }
        return instance;
    }
}
