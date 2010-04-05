
package ovm.core.stitcher;
/**
 * The <tt>ThreadServiceConfigurator</tt> defines the suppliers of various services 
 * within the OVM configuration. These are services needed at runtime but not 
 * at image creation time - things like threading config, event management,
 * user-domain configuration etc. Any service within the OVM which needs 
 * access to another service should access that service via the 
 * <tt>ThreadServiceConfigurator</tt>.
 * <p> The <tt>ThreadServiceConfigurator</tt> is created at image creation time,
 * as are the factory objects that it uses (tyically). The actual service
 * instances need not be created at image time as they may rely on
 * the actual runtime configuration themselves - though 
 * the {@link ovm.services.ServiceInstance ServiceInstance} mechanism was 
 * defined in such as way that services have trivial constructors that do
 * allow construction at boot image time.
 *
 * <p>To support the unlimited number of potential OVM services, the
 * <tt>ThreadServiceConfigurator</tt> can store references to {@link ServiceFactory}
 * objects which provide factory methods for a particular service. For
 * example, the {@link JavaServicesFactory} provides methods to obtain the Java
 * Virtual Machine instance and the Java thread dispatcher. These service 
 * factory
 * objects are retrieved using the {@link #getServiceFactory} method, by
 * passing in the name of a service.  
 * Subclasses of <tt>ThreadServiceConfigurator</tt> populate this set by
 * overriding {@link #initFactories}, which is called by the constructor
 * of <tt>ThreadServiceConfigurator</tt>. Because <tt>initFactories</tt> will be
 * called before the subclass constructors execute, the subclasses must have
 * trivial construction requirements that are not relied upon by the factories
 * they define.
 *
 * <p> In the future, a tool is hoped for that can discover couplings between
 * services, give the user the opportunity to choose a conflict-free
 * customization for the OVM and then automatically generate an
 * implementation of the <tt>ThreadServiceConfigurator</tt>.
 *
 * @author David Holmes, Filip Pizlo
 **/
public abstract class ThreadServiceConfigurator
    extends ServiceConfiguratorBase {

    /**
     * Global access to the installed service configurator. This
     * is initialised from the configuration specified to the
     * {@link InvisibleStitcher}.
     * 
     */
    public static ThreadServiceConfigurator config =
    (ThreadServiceConfigurator) InvisibleStitcher.singletonFor(
            ThreadServiceConfigurator.class.getName());


    /**
     * Prints the current configuration
     */
    public abstract void printConfiguration();
}





