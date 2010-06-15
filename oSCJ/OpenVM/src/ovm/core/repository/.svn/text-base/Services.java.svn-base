package ovm.core.repository;

import ovm.core.repository.RepositoryClass.Builder;
import ovm.core.stitcher.InvisibleStitcher;

/**
* A service factory for providing the configuration objects which provide
* the necessary factories, singletons, etc.  to give access to repository
* services. This interface also contains the name which will be mapped to
* the implementation of these service objects, as well as the
* specification of what functionality they should provide. This interface
* will be implemented by a <code>PackageEnvironment</code> class in the
* repository implementation package which will provide access to the
* actual implemented factories and singletons.
**/
/**
 * Implementation class for repository services specified by the 
 * <code>Services</code> service factory interface. This
 * contains the implementation-specific factories and singletons necessary to
 * provide the specified services. This implementation-specific class is mapped
 * to the generic name for these services in the
 * ovm.core.stitcher.OVMStitcher object, which hides these
 * implementation details from the user.
 * KLB: not found: ovm.core.OVMStitcher - what is this now?
 * @see ovm.core.repository.Services
 **/
final public class Services {
    public static Services getServices()
        throws InvisibleStitcher.PragmaStitchSingleton {

        return (Services) InvisibleStitcher.singletonFor(Services.class.getName());
    }
    /** 
     * Get a builder for a bytecode fragment  
     */
    public Bytecode.Builder getByteCodeFragmentBuilder() {
        return new Bytecode.Builder();
    }
    /** 
     * Get a builder for a class.  
     */
    public RepositoryClass.Builder getClassBuilder() {
        return new Builder();
    }
    /** 
     * Return a fresh builder for an untagged constant pool.
     * @return a builder for an untagged constant pool  
     */
    public ConstantPoolBuilder getConstantPoolBuilder() {
        return new ovm.core.repository.ConstantPoolBuilder();
    }

    /** 
     * Get a builder for a field 
     */
    public RepositoryMember.Field.Builder getFieldBuilder() {
        return new RepositoryMember.Field.Builder();
    }
    public RepositoryMember.Method.Builder getMethodBuilder() {
        return new RepositoryMember.Method.Builder();
    }
    /**
     * Create a repository. (singleton!)
     * @return the repository
     **/
    public Repository getRepository() {
        return Repository._;
    }

    /** 
     * Get a builder for a method 
     */

    public RepositoryMember.Method.Builder makeMethodBuilder() {
        return new RepositoryMember.Method.Builder();
    }
}
