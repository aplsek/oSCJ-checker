/*
 * $Header: /p/sss/cvs/OpenVM/src/ovm/core/stitcher/ServiceFactory.java,v 1.11 2005/03/01 05:31:56 dholmes Exp $
 *
 */
package ovm.core.stitcher;

/**
 * A service factory is an object that is responsible for providing
 * references to the objects that implement specific services within OVM.
 * For example, a {@link ThreadServicesFactory} object provides access to
 * a {@link ovm.core.services.threads.ThreadManager ThreadManager}.
 * <p>Service factories are maintained by the concrete 
 * {@link ServiceConfiguratorBase}
 * that defines the current configuration and can be obtained using the
 * {@link ServiceConfiguratorBase#getServiceFactory} method.
 * <p>Each specific service factory interface should define the name
 * of that service factory as a constant string. See, for example,
 * {@link ThreadServicesFactory#name}.
 * <p>Each service factory exports the set of 
 * {@link ovm.services.ServiceInstance service instances} that it's methods
 * export. This allows all services in the system to be found by querying
 * the configuration for all service factories, and each service factory for
 * its service instances. Note however that the same object could play
 * multiple service roles (such as <tt>ThreadManager</tt> and 
 * <tt>JavaThreadManager</tt>.
 * <p>Service factories are created at image time and so cannot perform any 
 * static, or instance, initialization that requires runtime support. 
 *
 * @author David Holmes
 *
 */
public interface ServiceFactory {

    /**
     * Return references to all of the 
     * {@link ovm.services.ServiceInstance service instances} that this
     * service factory provides.  This array may be dynamically
     * allocated every time you call the method, so call it sparringly.
     * Also, the array that is returned may contain null references;
     * you should just skip over them.
     */
    public ovm.services.ServiceInstance[] getServiceInstances();
}
