

package ovm.core.stitcher;
import ovm.core.OVMBase;
/**
 * <tt>ServiceConfiguratorBase</tt> serves as the base for the various service
 * configurators.
 * @author David Holmes, Filip Pizlo
 **/
public abstract class ServiceConfiguratorBase extends OVMBase {

    /** 
     * The hashtable used for storing service factory references.
     * This should be populated in {@link #initFactories}.
     */
    protected final HTString2ServiceFactory factories =
      	new HTString2ServiceFactory(8);


    /**
     * Default constructor that invokes {@link #initFactories}.
     */
    protected ServiceConfiguratorBase() {
        initFactories();
    }

    /**
     * Returns the {@link ServiceFactory} object for the named service,
     * within the current configuration.
     *
     * @param serviceName the name of the service for which a factory is 
     *                    required
     * @return the service factory for the named service, or
     * <code>null</code> if the named service does not form part of the
     * current configuration.
     *
     */
    public final ServiceFactory getServiceFactory(String serviceName) {
        return factories.get(serviceName);
    }

    /**
     * Return an array of references to the service factories defined
     * in the current configuration.
     */
    public ServiceFactory[] getServiceFactories() {
        String[] names = factories.keys();
        ServiceFactory[] allfactories = new ServiceFactory[names.length];

        for (int i = 0; i < allfactories.length; i++) {
            allfactories[i] = factories.get(names[i]);
        }
        return allfactories;
    }

    /**
     * Initializes the <tt>factories</tt> set with the required service
     * factories for this configuration.
     * <p>To avoid creation of service factories that are immediately replaced
     * in the set, the following protocol should always be used:
     * <ul>
     * <li>Each class only sets a factory if it is not already set
     * <li>Each class calls <tt>super.initFactories</tt> as its <em>last</em> 
     * action
     * </ul>
     * That way the most specialised factories are defined first by the
     * subclasses, and any missing factories are filled in as we invoke
     * the super versions.
     */
    protected abstract void initFactories();


}





