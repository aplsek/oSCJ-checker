package javax.realtime;
/**
 * Abstract superclass for all monitor control policy objects.
 *
 * <p>This class defines the interface for setting the monitor control
 * policy for the system, or individual objects within the system, via a
 * set of static methods.
 * <p><b>NOTE:</b> This class is currently under-specified by the RTSJ and
 * the dynamic semantics of the methods in this class are far from clear.
 * <h3>To-Do</h3>
 * <p>Connect this class to the underlying VM monitor control class(es).
 *
 * @see PriorityInheritance
 * @see PriorityCeilingEmulation
 *
 * @since RTSJ V1.0
 * @author David Holmes
 */
public abstract class MonitorControl {

    /** The current, default, monitor control policy. */
    private static volatile MonitorControl currentPolicy = PriorityInheritance.instance();

    /**
     * Return the current, default, system monitor control policy object.
     * <p>The initial, default system monitor control policy is
     * {@link PriorityInheritance priority inheritance}.
     * @return the current, default, system monitor control policy object.
     *
     */
    public static MonitorControl getMonitorControl() {
        return currentPolicy;
    }


    /**
     * Return the monitor control policy that applies to the given object.
     * @param obj the object to be queried. 
     * @return the monitor control policy that applies to the given object.
     * If <code>obj</code> is <code>null</code> then the current default
     * monitor control policy is returned.
     *
     */
    public static MonitorControl getMonitorControl(Object obj) {
        if (obj == null) {
            return currentPolicy;
        }
        else {
            // we don't support PCE monitors in OVM at this time
            // WE are not required to by the spec
            return currentPolicy;
        }
    }

    /**
     * Control the default monitor behavior for object monitors
     * used by synchronized statements and methods in the system.
     * The type of the policy object determines the type of behavior.
     * Conforming implementations must support 
     * {@link PriorityCeilingEmulation priority ceiling emulation}
     * and {@link PriorityInheritance priority inheritance} for fixed 
     * priority preemptive threads.
     * <p>The initial, default system monitor control policy is
     * {@link PriorityInheritance priority inheritance}.
     *
     * <h3>Note</h3><p>The RTSJ does not specify the dynamic effects of this
     * method. This method might change the monitor control policy for all
     * objects in the system, or only for those using the default policy.
     * However, keeping track of which objects use which policy would be a
     * great burden on the implementation. Further, given that a number of
     * objects in the system may have their monitors locked prior to an
     * application reaching the point where the default policy can be set, it
     * would not be unreasonable to change those existing objects to use the
     * applications default policy - otherwise the application will have
     * great trouble trying to enforce its requirements
     *
     * @param policy The new default monitor control policy.  If 
     * <code>null</code> then the current default policy is unchanged.
     */
    public static void setMonitorControl(MonitorControl policy) {
        if (policy == null) {
            return;
        }
        else if (policy != currentPolicy) {
            currentPolicy = policy;
            // now inform the VM of the new policy
            throw new Error("not implemented yet");
        }
    }
    
    /**
     * Has the same effect as <code>setMonitorControl()</code>, except that 
     * the policy only affects the indicated object monitor.
     * @param obj The object whose monitor the new policy will apply to.
     * The policy will take effect on the first attempt to lock the monitor 
     * after the completion of this method. 
     * If <code>null</code> nothing will happen.
     * @param policy The new policy for the object. 
     * If <code>null</code> nothing will happen.
     */
    public static void setMonitorControl(Object obj, MonitorControl policy) {
	if (obj == null || policy == null) {
	    return;
        }
        else {
            throw new Error("not implemented yet");
        }
    }

    /**
     * Create a <code>MonitorControl</code> object. 
     *
     */
    public MonitorControl() { }

}
