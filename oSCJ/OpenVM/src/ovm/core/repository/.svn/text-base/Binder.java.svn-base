package ovm.core.repository;

import ovm.core.repository.TypeName;
/**
 * @author Christian Grothoff
 */
public class Binder {

    private final RepositoryString[] args;
    private final TypeName.Scalar tn;

    Binder(TypeName.Scalar t, RepositoryString[] args) {
        this.tn = t;
        this.args = args;
    }

    public String toString() {
        String arg = "";
        for (int i = 0; i < args.length; i++)
            arg = arg + args[i].toString() + ",";
        return "BINDER(" + tn + ":" + arg + ")";
    }

    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o == this)
            return true;
        try {
            Binder b = (Binder) o;
            if ((b.tn != tn) || (b.args.length != args.length))
                return false;
            for (int i = 0; i < args.length; i++)
                if (!args[i].equals(b.args[i]))
                    return false;
            return true;
        } catch (ClassCastException cce) {
            return false;
        }
    }
    public RepositoryString[] getArguments() {
        return args;
    }
    /**
     * Get the name of the class for which this shared state object (once linked)
     * will hold the static fields.
     **/
    public TypeName.Scalar getTypeName() {
        return tn;
    }
    public int hashCode() {
        return tn.hashCode() + args.length;
    }

} // end of Binder
