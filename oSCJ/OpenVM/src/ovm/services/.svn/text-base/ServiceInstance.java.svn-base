/*
 * ServiceInstance.java
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/ServiceInstance.java,v 1.11 2007/06/03 01:25:48 baker29 Exp $
 *
 */
package ovm.services;

/**
 * The <code>ServiceInstance</code> interface defines an administrative
 * interface for controlling a particular service within the OVM. Services
 * that implement this interface adhere to the following protocol:
 * <ul>
 * <li> The service is either a singleton, that can be accessed via the
 * implementations static <code>getInstance</code> method, or has a 
 * trivial (often empty-bodied) no-arg constructor.
 * The actual class name, and thus creation of an instance, is typically 
 * only known by the
 * appropriate {@link ovm.core.stitcher.ServiceFactory ServiceFactory}
 * object obtained from the current
 * {@link ovm.core.stitcher.ServiceConfiguratorBase configuration}.
 * <li>Construction of an instance uses a trivial (often empty-bodied) 
 * no-arg constructor.
 * <li>No static, or instance initialization should access either the
 * {@link ovm.core.stitcher.ServiceConfiguratorBase current configuration} 
 * or other OVM services.
 * <li>All non-trivial initialization is performed using 
 * the {@link #init} method of the service instance, or the {@link #start}
 * method.
 * This will typically involve querying the
 * {@link ovm.core.stitcher.ServiceConfiguratorBase current configuration} 
 * to obtain references to other services
 * This method will typically be invoked during the bootstrapping
 * of the OVM at runtime.
 * <li>Configuration of a service may be performed using &quot;set&quot; 
 * methods, after the service is initialized, but before it is started.
 * </ul>
 * <p>By following this protocol we simplify the initialisation process of
 * an OVM configuration at image writing time. By making static initialization
 * and construction trivial we can ensure that the stitcher can be configured
 * without dealing with awkward service dependencies. By doing all real 
 * initialisation at boot-time we avoid the need for the image stitcher to
 * contain references to all of the service instances.
 *
 * <p>Services themselves will typically support three API's:
 * <ul>
 * <li>This management API</li>
 * <li>A set of &quot;configuration&quot; methods that can modify the 
 * behaviour of the service once it starts, and a set of methods to query
 * that behaviour (for example, the timer interrupt interval for the timer
 * manager.</li>
 * <li>The actual service methods used by other parts of the OVM.
 * Some of these methods will only be used after the service has started,
 * while others will be used after initialisation (there are no hard and
 * fast rules here except to defer to as late as possible anything that 
 * involves using another service - such as registering yourself with it,
 * or querying its runtime properties etc.)
 * </li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <p>A service instance must be {@link #init initialized} before any other
 * method is invoked upon it (excluding {@link #isInited} and 
 * {@link #isStarted}, and can only be initialized once. 
 * Before initializing a service you should always check if the
 * service has already been initialized by invoking {@link #isInited}.
 * <p>After initialization, and usually before {@link #start starting}, 
 * a service can be configured by invoking methods that &quot;set&quot; 
 * various properties of the service.
 * <p>If service A's initialization depends on service B being initialized
 * then it should explicitly initialize service B (after checking that it
 * wasn't already initialized). The order in which services are initialized
 * and started is not defined, other than that all services are initialized
 * before any services are started - see {@link ovm.core.Executive#startup}.
 *
 * <p>If a service needs to read the configuration of another service, then
 * it must defer that read until the {@link #start} method, because until
 * services are started their configuration information may not be 
 * <p>A service instance must then be {@link #start started} and 
 * eventually {@link #stop stopped} - the latter is typically done during
 * an orderly shutdown of the VM.
 * The exact semantics of starting and stopping a service will be depend upon
 * the service instance itself. Typically a service will allow runtime 
 * configuration between initialization and starting of the the service, but 
 * may prohibit further configuration once the service has started.
 * <p>Eventually a service instance may be {@link #destroy destroyed}. This
 * releases resources associated with the service and allows it to be 
 * &quot;unloaded&quot;. Many services exist for the lifetime of the OVM.
 *
 * <p>It is possible that some services are not part of the static 
 * configuration and are &quot;loaded&quot; on demand.
 *
 * @see ServiceInstanceImpl
 * @see ovm.core.stitcher.ServiceFactory
 * @see ovm.core.stitcher.ServiceConfiguratorBase
 * @see ovm.core.stitcher.ThreadServiceConfigurator
 * @see ovm.core.stitcher.IOServiceConfigurator
 * @see ovm.core.Executive
 *
 * @author David Holmes
 *
 */
public interface ServiceInstance {

    /**
     * Initializes this service instance.
     *
     */
    void init();

    /**
     * Queries if this service has been initialized
     * @return <code>true</code> if this service's {@link #init} method has
     * been invoked and completed successfully, else <code>false</code>.
     *
     */
    boolean isInited();

    /**
     * Starts this service instance.
     *
     */
    void start();

    /**
     * Queries if this service has been started.
     * @return <code>true</code> if this service's {@link #start} method has
     * been invoked and completed successfully, else <code>false</code>.
     */
    boolean isStarted();

    /**
     * a hook that is called when the VM is about to shut down.  this
     * will be called on all services before stop() and destroy().
     */
    void aboutToShutdown();
    
    /**
     * Stops this service.
     * 
     */
    void stop();

    /**
     * Releases all resources used by the service, allowing the service to
     * be safely &quot;un-loaded&quot;.
     *
     */
    void destroy();

}










