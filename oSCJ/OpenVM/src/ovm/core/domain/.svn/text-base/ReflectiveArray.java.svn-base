package ovm.core.domain;

import ovm.core.Executive;
import ovm.core.execution.CoreServicesAccess;
import ovm.core.repository.TypeName;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.MemoryManager;

/**
 * This class wraps array types that are used reflectively.  It caches
 * the reflective lookup and provides convenience methods for
 * allocating arrays as well as accessing array elements.  Accessor
 * methods may or may not implement bounds and store checks depending
 * on the value of {@link #CHECK_STORE} and {@link #CHECK_BOUND}.
 * Store checks are more useful with reflective arrays than plain java
 * arrays, since static type checking does not occur.<p>
 *
 * The consistent use of reflective wrapper objects also permits
 * whole-program analysis.  Reflective wrappers must be created at
 * image build time (before our static analysis completes).  When a
 * <code>ReflectiveArray</code> is created, a static analysis can
 * account for the use of this type in VM-internal code.<p>
 *
 * FIXME: get and set methods have not been implemented for primitive
 * types.  I hope that these will get filled in as needed.  After all,
 * it is not rocket science.
 *
 * @author <a href=mailto://baker29@cs.purdue.edu> Jason Baker </a>
 */
public class ReflectiveArray {
    /** If true, array access operations include a bounds check **/
    public static final boolean CHECK_STORE = true;
    /** If true, setOop operations include an array-store check **/
    public static final boolean CHECK_BOUND = true;
    
    private Type.Context ctx;
    private TypeName.Array arrayTypeName;
    private CoreServicesAccess csa;
    private Blueprint.Array bp;

    /**
     * Wrap an array type in a particular Type.Context
     * @param ctx         the context
     * @param eltTypeName the array's element type.
     **/
    public ReflectiveArray(Type.Context ctx, TypeName eltTypeName) {
	this.ctx = ctx;
	arrayTypeName = TypeName.Array.make(eltTypeName, 1);
	ctx.getDomain().registerNew(arrayTypeName);
    }

    /**
     * Wrap an array type in the system Type.Context of a particular
     * domain.
     * @param d           the array type's domain
     * @param eltTypeName the array's element type.
     * This constructor is equivalent to:
     * <pre>ReflectiveArray(d.getSystemTypeContext(), eltTypeName)</pre>
     **/
    public ReflectiveArray(Domain d, TypeName eltTypeName) {
	this(d.getSystemTypeContext(), eltTypeName);
    }

    public TypeName getName() { return arrayTypeName; }
    
    /**
     * Return the blueprint for this array type.
     **/
    public Blueprint.Array bp() {
	if (bp == null)
	    try {
		Type t = ctx.typeFor(arrayTypeName);
		Domain d = ctx.getDomain();
		bp = (Blueprint.Array) d.blueprintFor(t);
		csa = d.getCoreServicesAccess();
	    } catch (LinkageException e) {
		throw Executive.panicOnException(e);
	    }
	return bp;
    }

    /**
     * Construct an array of <i>size</i> elements.
     **/
    public Oop make(int size) {
	Blueprint.Array _bp = bp(); // force resolution of CSA before call
	return csa.allocateArray(_bp, size);
    }

    /**
     * Return <code>arr[idx]</code>.
     **/
    public Oop getOop(Oop arr, int idx) {
	if (CHECK_BOUND && (idx >= bp().getLength(arr) || idx < 0))
	    throw new ArrayIndexOutOfBoundsException();
//	int off = bp().byteOffset(idx);
//	return VM_Address.fromObject(arr).add(off).getAddress().asOop();

        bp();
        return MemoryManager.the().getReferenceArrayElement( arr, idx );
    }

    /**
     * <code>arr[idx] = val</code>
     **/
    public void setOop(Oop arr, int idx, Oop val) {
	if (CHECK_BOUND && (idx >= bp().getLength(arr) || idx < 0))
	    throw new ArrayIndexOutOfBoundsException();
	if (CHECK_STORE &&
	    !val.getBlueprint().isSubtypeOf(bp().getComponentBlueprint()))
	    throw new ArrayStoreException();
//	int off = bp().byteOffset(idx);
	//VM_Address.fromObject(arr).add(off).setAddress
	//    (VM_Address.fromObject(val));
//	MemoryManager.the().setReferenceArrayElementAtByteOffset(arr, off, val);
        bp();
        MemoryManager.the().setReferenceArrayElement(arr, idx, val);

//        MemoryManager.the().setReferenceArrayElement(arr, idx, val, bp().getComponentSize());
    }

    /**
     * Return <code>arr[idx]</code>.
     **/
    public char getChar(Oop arr, int idx) {
	if (CHECK_BOUND && (idx >= bp().getLength(arr) || idx < 0))
	    throw new ArrayIndexOutOfBoundsException();
//	int off = bp().byteOffset(idx);
//	return VM_Address.fromObject(arr).add(off).getChar();
        bp();
        return MemoryManager.the().getCharArrayElement( arr, idx);
    }

    /**
     * <code>arr[idx] = val</code>
     **/
    public void setChar(Oop arr, int idx, char val) {
	if (CHECK_BOUND && (idx >= bp().getLength(arr) || idx < 0))
	    throw new ArrayIndexOutOfBoundsException();
//	int off = bp().byteOffset(idx);
//	VM_Address.fromObject(arr).add(off).setChar(val);
//        MemoryManager.the().setPrimitiveArrayElementAtByteOffset(arr, off, val);
        bp();
        MemoryManager.the().setPrimitiveArrayElement( arr, idx, val );
    }
}
