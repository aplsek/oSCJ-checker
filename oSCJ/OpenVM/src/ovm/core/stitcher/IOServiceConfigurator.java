// $Header: /p/sss/cvs/OpenVM/src/ovm/core/stitcher/IOServiceConfigurator.java,v 1.3 2005/03/01 06:30:14 dholmes Exp $

package ovm.core.stitcher;

/**
 * Service configurator for the I/O subsystem.
 * @author Filip Pizlo
 */
public abstract class IOServiceConfigurator
    extends ServiceConfiguratorBase {
    
    public static IOServiceConfigurator config =
        (IOServiceConfigurator) InvisibleStitcher.singletonFor(
            IOServiceConfigurator.class.getName());

    /**
     * Prints the current configuration
     */
    public abstract void printConfiguration();

}

