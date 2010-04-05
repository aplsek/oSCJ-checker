package ovm.core.domain;

import ovm.core.repository.TypeName;
import ovm.core.stitcher.InvisibleStitcher;
import ovm.core.stitcher.InvisibleStitcher.PragmaStitchSingleton;
import ovm.util.Iterator;

// various implementations for single domain/cluster/whatnot configs
// FIXME this class effectively thwarts InvisibleStitcher's call elimination
public class DomainDirectory {

    public interface Interface extends InvisibleStitcher.CoreComponent {
	ExecutiveDomain getExecutiveDomain();
	UserDomain getUserDomain(int /* hmmm */ id);
	Iterator domains();
	/** use default bootclasspath */
	UserDomain createUserDomain(TypeName.Scalar mainClassName);

	int maxContextID();
	Type.Context getContext(int id);

    }
    // The pragma ensures this method body cannot be live at run time, so
    // the reference to class.getName() is safe; also the rewriting of the
    // method will be no less efficient than a reference to a private static
    // final field.
    public final static Interface the() throws PragmaStitchSingleton {  
	return (Interface)
	      	InvisibleStitcher.singletonFor(Interface.class.getName());
    }

    public static ExecutiveDomain getExecutiveDomain() {
	return the().getExecutiveDomain();
    }
    
    public static UserDomain getUserDomain(int id) {
	return the().getUserDomain(id);
    }

    public static Iterator domains() {
	return the().domains();
    }
    public static UserDomain createUserDomain(TypeName.Scalar mainClassName) {
	return the().createUserDomain(mainClassName);
    }

    public static int maxContextID() {
	return the().maxContextID();
    }
    public static Type.Context getContext(int id) {
	return the().getContext(id);
    }

}
