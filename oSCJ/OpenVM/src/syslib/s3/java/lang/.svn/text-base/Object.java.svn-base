/**
 * @author Titzer, Holmes, Baker
 *
 * This class represents an internal implementation of java.lang.Object for
 * the Executive Domain of the OVM. 
 * It is not meant to represent the java.lang.Object class
 * for user-domain application code or offer a complete replacement for
 * java.lang.Object as normally defined for application code.  
 **/
package java.lang;

import ovm.core.domain.DomainDirectory;
import ovm.core.domain.ExecutiveDomain;
import ovm.core.domain.Oop;
import ovm.core.execution.Context;
import ovm.core.services.format.JavaFormat;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.services.memory.VM_Address;
import ovm.services.java.JavaMonitor;
import ovm.services.monitors.MonitorMapper;
import ovm.core.execution.CoreServicesAccess;

public class Object {

    /**
     * Equals implementation for Object just compares references.
     **/
    public boolean equals(Object o) {
	return this == o;
    }

    /**
     * HashCode for the Object (obtained from the object-model)
     **/
    public int hashCode() {
	return toOop().getHash();
    }

    
    protected Object clone() throws CloneNotSupportedException {
	if (this instanceof Cloneable) {
            Context ctx = Context.getCurrentContext();
            return MemoryManager.the().clone(toOop());
	}
	throw new CloneNotSupportedException();
    }

    /**
     * Return a String of the form "<name of type of this>@<hashcode of this>"
     */
    public String toString() {
        /* Bug #650
        return getNameOfTypeFor(this) + "@" + hashCode();
        */
        return getNameOfType() + "@" + hashCode();
    }

    /**
     * Package private method to get a type name for an instance in regular
     * Java class name format
     * @param obj the object
     * @return the type name of the given object
     */
    // the odd name is to avoid confusion with TypeName
    static String getNameOfTypeFor(Object obj) {
        return JavaFormat._.
            format(obj.toOop().getBlueprint().getType().getUnrefinedName()); 
    }

    // instance method to avoid bug #650
    String getNameOfType() {
        return JavaFormat._.
            format(toOop().getBlueprint().getType().getUnrefinedName()); 
    }


    private static CoreServicesAccess getCSA() {
	return DomainDirectory.getExecutiveDomain().getCoreServicesAccess();
    }

    public void wait()      { getCSA().monitorWaitAbortable(toOop()); }
    public void notify()    { getCSA().monitorSignal(toOop()); }
    public void notifyAll() { getCSA().monitorSignalAll(toOop()); }

    /**
     * Finalize method for Object does nothing.
     **/
    protected void finalize() throws Throwable {
    }


    final ExecutiveDomain myDomain() {
	return DomainDirectory.getExecutiveDomain();
    }

    // FIXME: Due to DispatchBuilder bugs, package-private methods
    // called asOop are a bad idea.
    final Oop toOop() {
	return VM_Address.fromObject(this).asOop();
    }
    
    /**
     * Returns the runtime class of an object. 
     * 
     * <p>In the ED we should not need this or use it. It remains only as
     * a concession to places that use it for debug info when they should
     * not really be doing so, and because we can don't cleanly seperate
     * build-time and run-time classes in OVM.
     * <p> - DH 27 jan 2004
     *
     * <p><code>getClass()</code> is also needed to write equals
     * methods sanely, and is used in runabouts.
     * <p> - JB  9 mar 2004
     *
     * @return  the object of type <code>Class</code> that represents the
     *          runtime class of the object.
     */
    public final Class getClass() {
	return (Class) (Object)
	    myDomain().getRuntimeExports().classFor(toOop());
    }


}


