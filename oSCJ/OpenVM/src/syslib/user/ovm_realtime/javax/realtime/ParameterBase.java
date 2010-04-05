/*
 * $Header: /p/sss/cvs/OpenVM/src/syslib/user/ovm_realtime/javax/realtime/ParameterBase.java,v 1.5 2006/04/08 21:08:16 baker29 Exp $
 *
 */
package javax.realtime;

/**
 * Package-private abstract base class for all parameter objects which
 * maintains the binding between the parameter object and the schedulables it
 * is associated with.
 * <P>Note that any concrete Schedulable class must use the methods here to
 * register. This implies that those concrete classes must be in this package.
 * Altghough the RTSJ makes no explicit mention of this, discussion with the
 * RTSJ EG members and consideration of the various API's makes it fairly
 * apparent that the interfaces in the javax.realtime package are not meant 
 * to be implemented by anything outside of that package - ie. only the RTSJ 
 * implementation should be providing such subclasses, not application code.
 * <p>While synchronization of public methods is the responsibility of the 
 * caller, the registration methods take care of their own synchronization.
 *
 * <p>We also provide a convenient &quot;clone&quot; method.
 *
 * @author David Holmes
 *
 */
/* package-private */ abstract class ParameterBase implements Cloneable {

    final int DEFAULT_INITIAL_ARRAY_SIZE = 8;

    /** the set of schedulables associated with this parameter object */
    IdentityArraySet schedulables = new IdentityArraySet(DEFAULT_INITIAL_ARRAY_SIZE);

    /**
     * Register the given schedulable with this parameter object.
     * <p>
     * All concrete Schedulable's should be registered when they are created.
     * Hence this can only be called once per Schedulable and we should be 
     * able to assert that the schedulable is not present.
     *
     * @param s the Schedulable to be registered
     *
     */
    synchronized void register(Schedulable s) {
        if (!schedulables.add(s))
            throw new Error("Schedulable " + s + " already associated with "
                            + "parameter object " + this);
    }


    /**
     * Remove the association between the Schedulable and this parameters
     * object.
     *
     * @param s the schedulable to de-register
     */
    synchronized void deregister(Schedulable s) {
        schedulables.remove(s);
    }

    /**
     * Return a cloned-copy of <code>this</code> as if the returned object
     * had been created by a copy constructor.
     * @return a cloned copy of <code>this</code>
     */
    public final synchronized Object clone() {
        try {
            if (System.getProperty("javax.realtime.debug") != null) {
                // report all allocation contexts
                System.out.print("DEBUG ParameterBase.clone: allocation context of this is ");
                System.out.println(MemoryArea.getMemoryArea(this));
                System.out.print("DEBUG Parameterbase.clone: current allocation context is ");
                System.out.println(RealtimeThread.getCurrentMemoryArea());
                for (int i = 0; i < schedulables.size(); i++) {
                    System.out.print("DEBUG: Associated SO has allocation context: ");
                    System.out.println(MemoryArea.getMemoryArea(schedulables.data[i]));
                }
            }
            ParameterBase copy = (ParameterBase)super.clone();
            // clear the registration set
            copy.schedulables = 
                new IdentityArraySet(DEFAULT_INITIAL_ARRAY_SIZE);

            return copy;
        }
        catch(CloneNotSupportedException ex) {
            throw new InternalError("Clone not supported????");
        }
    }
}



