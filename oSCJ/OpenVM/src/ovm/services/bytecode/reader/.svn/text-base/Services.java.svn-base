package ovm.services.bytecode.reader;

import ovm.core.stitcher.InvisibleStitcher;

/**
 * A service factory for providing the configuration objects relating to
 * the bytecode analysis and manipulation subsystem. The configuration 
 * objects provide the necessary factories, singletons, etc.
 * to give access to bytecode services. This interface also contains the name 
 * which will be mapped to the implementation of these service objects, as 
 * well as the specification of what functionality they should provide. This 
 * interface will be implemented by a <code>PackageEnvironment</code> class in
 * the bytecode implementation package which will provide access to the actual 
 * implemented factories and singletons
 * @author Jan Vitek
 **/
public abstract class Services {

    /**
     * Get a parser factory
     * @return a parser factory
     **/
    public abstract Parser.Factory getOVMIRParserFactory();

    /**
     * Get a parser factory
     * @return a parser factory
     **/
    public abstract Parser.Factory getParserFactory();


    public static Services getServices()
        throws InvisibleStitcher.PragmaStitchSingleton {
        
        return (Services) InvisibleStitcher.singletonFor(
            Services.class.getName());
    }
}
