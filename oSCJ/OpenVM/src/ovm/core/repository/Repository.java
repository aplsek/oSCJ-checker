package ovm.core.repository;

import ovm.core.services.memory.MemoryPolicy;
import ovm.services.io.ResourcePath;
import ovm.util.ReadSafeHashMap;
import ovm.util.ReadonlyViewException;
import s3.core.S3Base;

/**
 * This is the class for repository objects, which contain 
 * the bytecode of all classes loaded in the virtual machine. 
 * The Repository retains an object-oriented representation of the 
 * information contained in Java <code>.class</code> files. 
 * This representation is shared across domains, to allow the bytecode 
 * of a class to be used by several applications without having to reload the 
 * class definition. Modifications of this shared data are not allowed. The
 * Repository interface does not support class unloading (unloading can
 * only be done after a GC of the repository, which is currently not
 * supported). New definitions can be added to the repository by calling
 * the appropriate install method. Implementations must ensure that install
 * methods are thread safe.<p>
 *
 * @author Jan Vitek, Krzysztof Palacz, Christian Grothoff
 **/

public class Repository
    extends S3Base
    implements Bundle.Factory
{

    // -------------Fields --------------------------------------

    /**
     * The singleton repository (in other words, <em>the</em> repository).
     **/
    public static Repository _ = new Repository(null);

    /**
     * Determines whether or not the repository is mutable
     **/
    private final boolean isReadonly;

    //
    // Repository data structures
    //

    // NOTE: Access to any of the HT objects should only be done within a
    // synchronized region, holding the lock of that object.  Similarly
    // these two must only be accessed when bundles_ is locked

    /**
     * All bundles other than the system bundle.<p> 
     * <i>Access should only be done within a synchronized region, 
     * holding the lock associated with the otherBundles field.  (We
     * can't lock down the array directly, since it may be
     * reallocated.)</i> 
     **/
    private Object obLock_ = new Object();
    private Bundle[] bundles_;

    /**
     * The number size of <code>bundles_</code>. 
     * <p><i>Access should only
     * be done within a synchronized region, holding the lock of
     * {@link #bundles_}.</i>
     **/
    private int bundlesCount_;

    /**
     * The hashtable of <code>Strings</code> in this repository.<p>
     * <i> Access should only be done within a synchronized region,
     * holding the lock of this object.</i></p>
     *
     * FIXME: What the hell is this for?
     */
   //  deprecated -- jv
    final private ReadSafeHashMap strings_ = new ReadSafeHashMap();
    
    // */
    // -------------Methods --------------------------------------

    /**
     * Creates a read-only version of the <code>Repository</code>
     * input object; if the input is null, a new, non-read-only
     * <code>Repository</code> object is created.
     * @param other the repository object to create
     *              a read-only view of, or null
     **/
    protected Repository(Repository other) {
        bundles_ = null;
        bundlesCount_ = 0;

        if (other != null) {
            isReadonly = true;
        } else {
	    Object r = MemoryPolicy.the().enterRepositoryDataArea();
	    try {
		Repository._ = this;
		bundles_ = new Bundle[3];
		bundlesCount_ = 0;
		isReadonly = false;
	    } finally { MemoryPolicy.the().leave(r); }
        }
    }

    /**
     * Return a read-only view of this repository
     * @return an immutable copy of this repository
     **/
    public Repository makeReadonlyView() {
	return new Repository(this);
    }

    //////////////////////////////////////////////////////////////////
    // Bundle methods
    //

    /**
     * Create a new Bundle with no parent bundles.
     * If this repository is readonly, then throw an exception.	
     * @return Bundle the new, parentless bundle.
     */
    
    public Bundle makeBundle(ResourcePath rpath) {
        if (isReadonly) { // 
            throw new ReadonlyViewException();
        }
        return makeBundle(Bundle.EMPTY_ARRAY, rpath);
    }
    

    /**
     * Create a new Bundle, given an array of parent bundles.
     * @param parents the parent bundles of the new bundle
     * @return Bundle the new bundle
     */
    public Bundle makeBundle(Bundle[] parents, ResourcePath rpath) {
        if (isReadonly) {
            throw new ReadonlyViewException();
        }
	Object r = MemoryPolicy.the().enterRepositoryDataArea();
	try {
	    Bundle bundle = new Bundle(parents, rpath);
	    synchronized (obLock_) {
		if (bundles_.length == bundlesCount_) {
		    // FIXME: RTSJ memory leak of bundlesCount^2
		    Bundle[] arr = new Bundle[bundlesCount_ + 3];
		    System.arraycopy(
				     bundles_,
				     0,
				     arr,
				     0,
				     bundles_.length);
		    bundles_ = arr;
		}
		bundles_[bundlesCount_] = bundle;
		bundlesCount_++;
	    }
	    return bundle;
	} finally { MemoryPolicy.the().leave(r); }
    }

    ///////////////////////////////////////////////////////////

	// Warning: none of these unsychronized accesses are guaranteed to
	// return any meaningful results
	/**
	 * This does absolutely nothing at the moment.
	 **/
	public void reportStatistics() {
	    /*// currently does not compile as 'size' is not in HTX template.
	      d("scalarTypeNames_ " + scalarTypeNames_.countElements()
	      + '/' + scalarTypeNames_.size());
	      d("arrayTypeNames_ " + arrayTypeNames_.countElements()
	      + '/' + arrayTypeNames_.size());
	      d("strings_ " + strings_.countElements()
	      + '/' + strings_.size());
	      d("descriptors_ " + descriptors_.countElements()
	      + '/' + descriptors_.size());
	      d("selectors_ " + selectors_.countElements()
	      + '/' + selectors_.size());
	      d("boundSelectors_ " + boundSelectors_.countElements()
	      + '/' + boundSelectors_.size());
	      d("utf8s_ " + utf8s_.length);
	      d("bundles 1 + " + bundlesCount_ + '/' + bundles_.length);
	    */
	}

} // End of Repository
