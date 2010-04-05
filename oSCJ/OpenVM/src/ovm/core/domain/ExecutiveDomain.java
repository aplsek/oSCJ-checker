package ovm.core.domain;

/**
 * The executive domain is the execution-engine, or kernel of the OVM.
 * There is only one executive domain in the OVM and it provides all
 * the kernel services: memory management, threading, I/O, reflection,
 * etc.
 *
 * <p>This is primarily a marker interface.
 */
public interface ExecutiveDomain extends JavaDomain {
    /**
     * Execute every method of the form static void boot_() in this
     * domain in an arbitrary order.  These methods are used to
     * perform runtime initialization, while normal static
     * initializers are run at image-build time.
     * <p>
     * Actually, the order used to be arbitrary but is no longer.
     * These methods will be run in the order in which
     * {@link #addBootMethod}
     * was called on them, and addBootMethod will be called on
     * supertypes before subtypes.
     **/
    public void runAllBootMethods();
    /**
     * Register m to be called by {@link #runAllBootMethods}.
     **/
    public void addBootMethod(Method m);
}
