package java.lang;

import ovm.core.domain.Type;
import ovm.core.services.format.JavaFormat;
import ovm.core.services.memory.VM_Address;
import ovm.core.domain.DomainDirectory;
/**
 * A minimal representation of java.lang.Class for the OVM Executive Domain.
 * Ideally this class should not be needed as ED objects do not have associated
 * Class objects. But unfortunately toString() methods often do 
 * getClass().getName() which means we need to support them just-in-case.
 *
 * <p>If we properly separated build-time and run-time classes we could ensure
 * that this class is never used, without generating hundreds of build time
 * warnings.
 *
 * <p>DH 23 Jan 2004
 */
public final class Class {

    /** invoked by the VM */
    private Class() {
    }

    // Noone can ever call this because the calling code would have to be
    // compiled against this version of java.lang.Class - and we don't do
    // that - DH 23 Jan 2004
    public Type getType() {
	return toOop().getBlueprint().getType().getInstanceType();
    }

    /** Return the name of the Class or interface represented by this
        Class object
    */
    public String getName() {
        return JavaFormat._.format(getType().getUnrefinedName()); 
    }

    /** Return the Class object for the class with the given name.
     * <p>This is only here to keep the rewriter quiet when it encounters the
     * synthetic methods generated for class literals that 
     * invoke Class.forName().
     * @throws ClassNotFoundException
     */
    public static Class forName(String clazz) throws ClassNotFoundException {
        //            return (Class) (Object) DomainDirectory.getExecutiveDomain().getCoreServicesAccess().classForName(VM_Address.fromObject(clazz).asOop());
        throw new ClassNotFoundException("Shouldn't use Class.forName in the ED");
    }
}

