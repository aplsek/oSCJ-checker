package ovm.core.repository;

import ovm.core.repository.TypeName;
import ovm.core.OVMBase;
import ovm.services.io.ResourcePath; // FIXME undesirable package dependency

import ovm.util.OVMException;

/**
 * <p>A <code>Bundle</code> is a logical group of 
 * <code>RepositoryClasses</code> used by the
 * <code>Type.Context</code> to determine which is the correct
 * <code>RepositoryClass</code> to load. This is particularly
 * useful when there are multiple classes with the same
 * name but different implementations in the system.</p>
 *
 * @see ovm.core.domain.Type.Context
 **/
public class Bundle extends OVMBase {

    /**
     * The loader that services this bundle
     **/
    RepositoryLoader loader;

    // yes, package access
    /**
     * The empty set of S3Bundles
     **/
    static final Bundle[] EMPTY_ARRAY = new Bundle[0];

    /**
     * A hash table containing the <code>RepositoryClasses</code> which are 
     * already in this bundle
     **/
    private HTRepositoryClass classes;

    /**
     * The parent bundles for this bundle.
     **/
    Bundle[] parents;

    /**
     * Makes a new Bundle. Its parent bundles will be the empty set, 
     * and its initial capacity will default to 1024.
     * @param rpath the resource path in which this bundle will
     *        look for classes.
     **/
    Bundle(ResourcePath rpath) {
        this(EMPTY_ARRAY, rpath);
    }

    /**
     * Make a readonly view of the bundle
     **/
    Bundle(Bundle other) {
        this.parents = other.parents;
        this.classes = other.classes;
        seal();
    }

    public String toString() {
        if (loader == null)
            return "Bundle[Sealed]";
        else
            return "Bundle[" + loader + "]";
    }
    /**
     * Makes a new Bundle, given its parent bundles. Its initial
     * capacity will default to 1024.
     * @param parents the array of parent bundles
     * @param rpath the resource path in which this bundle will
     *        look for classes.
     **/
    Bundle(Bundle[] parents, ResourcePath rpath) {
        this(parents, 1024, rpath);
    }

    /**
     * Makes a new Bundle, given its parent bundles and initial
     * capacity.
     * @param parents the array of parent bundles
     * @param initialCapacity the initial class capacity of this bundle
     * @param rpath the resource path in which this bundle will
     *        look for resources.
     **/
    Bundle(Bundle[] parents, int initialCapacity, ResourcePath rpath) {
        this.parents = parents;
        classes = new HTRepositoryClass(initialCapacity);
        loader = new RepositoryLoader(rpath);
    }

    /**
     * Makes a new Bundle, given its initial capacity. Its parent bundles
     * will be the empty set.
     * @param initialCapacity the initial class capacity of this bundle.
     * @param rpath the resource path in which this bundle will
     *        look for resources.
     **/
    Bundle(int initialCapacity, ResourcePath rpath) {
        this(EMPTY_ARRAY, initialCapacity, rpath);
    }

    /**
    * Determine whether further class additions to this bundle
    * are prevented or not.
    * @return true if the bundle is sealed (i.e. more classes cannot be
    *         added), otherwise false
    */
    public synchronized boolean isSealed() {
        return loader == null;
    }

    /**
     * Determine whether the class specified by the <code>TypeName</code>
     * indicated is contained in this bundle.
     * @param name the scalar typename of the class to look for
     * @return boolean true if the class is contained in this bundle;
     *         otherwise, false.
     */
    public boolean containsClass(TypeName.Scalar name) {
        RepositoryClass probe = RepositoryClass.createClassProbe(name);
        synchronized (this) {
            return classes.get(probe) != null;
        }
    }

    /**
     * Find the <code>RepositoryClass</code> in this bundle associated with 
     * a particular type name; return null if not found in this bundle.
     * @param name the class name to be searched for
     * @return the Repository class searched for, or null if this bundle
     *         doesn't contain the given class.
     * @throws RepositoryException if malformed class file encountered
     **/
    public RepositoryClass lookupClass(TypeName.Scalar name)
        throws RepositoryException {
        if (name.isGemeinsam())
            name = name.asGemeinsam().getInstanceTypeName().asScalar();
        RepositoryClass key = RepositoryClass.createClassProbe(name);
        synchronized (this) {
            RepositoryClass value = classes.get(key);
            if (value == null && (loader != null)) {
                value = loader.load(name);
                if (value != null) {
                    classes.put(value);
                }
            }
            return value;
        }
    }

    /**
     * @param action what to do on every RepositoryClass
     */
    public void forAll(RepositoryClass.Action action) throws OVMException {
        HTRepositoryClass.Iterator iter = classes.new Iterator();
        for (; iter.hasNext();) {
            action.process(iter.next());
        }
    }

    /**
     * Seals this bundle in order to prevent more classes from being
     * installed in it.
     **/
    public synchronized void seal() {
        loader = null;
    }

    public void loadAll() throws RepositoryException {
	loader.loadAll(this);
    }

    public static class Path {
        private Path parent;
        private Bundle[] bundles;
        public Path(Path parent, Bundle[] bundles) {
            this.parent = parent;
            this.bundles = bundles;
        }
        public String toString() {
            StringBuffer buf = new StringBuffer("BundlePath[");
            if (parent != null)
                buf.append(parent).append(',');
            for (int i = 0; i < bundles.length; i++) {
                buf.append(bundles[i]);
                if (i != bundles.length - 1)
                    buf.append(',');
                else
                    buf.append(']');
            }
            return buf.toString();
        }

	public void loadAll(boolean shouldRecurseToParent)
	    throws RepositoryException
	{
	    if (parent != null && shouldRecurseToParent)
		parent.loadAll(true);
	    for (int i = 0; i < bundles.length; i++)
		bundles[i].loadAll();
	}
	    
        public void sealAll() {
            if (parent != null)
                parent.sealAll();
            for (int i = 0; i < bundles.length; i++) {
                bundles[i].seal();
            }
        }
        public RepositoryClass lookupClass(TypeName.Scalar name)
            throws RepositoryException {
            return lookupClass(name, true);
        }

        public RepositoryClass lookupClass(
            TypeName.Scalar name,
            boolean shouldSearchParent)
            throws RepositoryException {
            RepositoryClass answer;
            if (shouldSearchParent && parent != null) {
                answer = parent.lookupClass(name, true);
                if (answer != null) {
                    return answer;
                }
            }
            for (int i = 0; i < bundles.length; i++) {
                answer = bundles[i].lookupClass(name);
                if (answer != null) {
                    return answer;
                }
            }
            return null;
        }

        public void forAll(RepositoryClass.Action action) throws OVMException {
            forAll(action, true);
        }

        public void forAll(
            RepositoryClass.Action action,
            boolean shouldAskParent)
            throws OVMException {
            if (shouldAskParent && parent != null) {
                parent.forAll(action);
            }

            for (int i = 0; i < bundles.length; i++) {
                bundles[i].forAll(action);
            }
        }
    }

    /**
     * Interface for bundle factory methods
     **/
    public interface Factory {
        /**
         * Create a new bundle, given an resource path in which
         * to search for classes
         * @param resources the resource path in which to search for classes
         * @return          the new bundle
         */
        Bundle makeBundle(ResourcePath resources);
    }
}
