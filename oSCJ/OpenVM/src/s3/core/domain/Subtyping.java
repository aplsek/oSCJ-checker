package s3.core.domain;

import ovm.core.Executive;
import ovm.core.domain.Blueprint;
import ovm.core.domain.Domain;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Type;
import ovm.util.ArrayList;
import ovm.util.HTObject2int;
import ovm.util.HashSet;
import ovm.util.Iterator;
import ovm.util.LinkedList;
import ovm.util.List;
import s3.core.S3Base;
import ovm.core.services.io.BasicIO;
import s3.util.PragmaAtomic;
import ovm.core.services.memory.VM_Area;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.OVMBase;

/**
 * Provides internal functions necessary for creating and
 * maintaining data structures used in subtype tests.
 * <blockquote>Inferred by Chap: each type has an array of
 * buckets, a bucket index, and a bucketID. Desired invariant:
 * given types A and B, B's bucketID equals the entry in A's
 * bucket array [at B's bucket index] if and only if A is
 * assignable to B.</blockquote>
 **/
class Subtyping extends S3Base {

    /**
     * The typeBuckedID for classes whose typeDisplay has not been
     * computed.  These IDs disappear after a call to
     * {@link #recomputeSubtypeInfo}.  Until a valid typeBucketID is
     * assigned, isSubtypeOf will return false when comparing this
     * type to any other
     */
    static final int UNCOMPUTED = 256;
    /**
     * The typeBucketID for any array type.  This number is arbitrary,
     * since subtype tests on arrays do not use the typeDisplay
     **/
    static final int ARRAY = 257;
    /**
     * The typeBucketID for a shared state object.  Shared state types
     * are not part of the java type system, but all shared-state
     * objects are java.lang.Class instances as well.
     **/
    static final int SHARED_STATE = 258;

    private final S3Domain domain;
    private ArrayList bucketDescriptors = new ArrayList();
    private HTObject2int bucketAssignment = new HTObject2int();

    /**
     * Number of types added since the last call to
     * {@link #recomputeSubtypeInfo}.
     **/
    private int added;

    /**
     * Total number of non-aliased class types in the system
     **/
    private int classTypes;
    
    Subtyping(S3Domain domain) {
	this.domain = domain;
    }

    /**
     * Get the bucket descriptor for the bucket at the given index.
     * @param bucket the bucket index
     * @return the bucket descriptor
     **/
    private BucketDescriptor getDescriptor(int bucket) {
        return (BucketDescriptor) bucketDescriptors.get(bucket);
    }
    /**
     * Given a <code>Type</code>, return its bucketID, i.e. the
     * type's order of addition to its assigned bucket (0 if it is not
     * found as a member of that bucket?). The value can equal but never exceed
     * {@link #MAX_BUCKET_SIZE} (S1). -jcf (inferred)
     * @param t the type whose bucket ID is wanted
     * @return the bucket ID
     **/
    private int getBucketID(Type t) {
        // S1. MAX_BUCKET_SIZE is the maximum number of members. It is one greater
        // than the maximum index of any member. It is therefore the maximum possible
        // value of index+1.
        return getDescriptor(getBucket(t)).members.indexOf(t) + 1;
    }
    /**
     * Return the bucket index assigned to a type.
     * @param t a Type
     **/
    private int getBucket(Type t) {
        return bucketAssignment.get(t);
    }
    /** 
     * @return the number of buckets a display must accommodate **/
    private int displaySize() {
        return bucketDescriptors.size();
    }

    private static class BucketDescriptor {
        HashSet marked = new HashSet();
        LinkedList members = new LinkedList();

        void mark(Type t) { // needs a split
            assert(members.contains(t));
            marked.add(t);
        }
        boolean isSplitNeeded() {
            return marked.size() > 1;
        }
        void clear() {
            marked.clear();
        }
        int markCount() {
            return marked.size();
        }
        void add(Type t) {
            if ( members.size() < MAX_BUCKET_SIZE )
                members.add(t); // S1. Size may now == but cannot > MAX_BUCKET_SIZE.
            else
                failUnimplemented( "Grow a subtyping bucket.");
        }
    }
    
    /** Dump the contents of the descriptor buckets (for debugging). */
    private void dump() {
        for (int i = 0; i < bucketDescriptors.size(); i++)
            d(i + ": " + getDescriptor(i).members);
    }

    /** The area in which all type-display arrays are allocated **/
    private VM_Area area;
    private byte[] arrayDisplay = new byte[0];
    private byte[] emptyDisplay;
    private byte[] classDisplay;
    
    private void makeStandardDisplays() {
	// First, allocate a new VM_Area to hold subtyping data.  We
	// cannot afford to leak all these arrays every time subtyping
	// information is recomputed, nor can we afford to allocate
	// these arrays in the heap (they may be touched by no-heap
	// realtime threads).
	int oldSz;
	if (area == null || OVMBase.isBuildTime())
	    oldSz = (classTypes + 4) * (displaySize() + 20);
	else {
	    oldSz = area.memoryConsumed();
	    MemoryManager.the().freeArea(area);
	    // Make sure to fail if makeExplicitArea runs out of memory
	    area = null;
	}
	area = MemoryManager.the().makeExplicitArea(2 * oldSz);

	// Next, use this area to allocate standard displays
	VM_Area r = null;
	if (area != null)
	    // If area is null, everything is in the heap or bootimage
	    r = MemoryManager.the().setCurrentArea(area);
	try {
	    emptyDisplay = new byte[displaySize()];
	    arrayDisplay = new byte[displaySize()];
	    classDisplay = new byte[displaySize()];
	    arrayDisplay[0] = (byte) getBucketID(domain.getHierarchyRoot());
	    Type.Interface[] ifaces = domain.getArrayInterfaces();
	    for (int i = 0; i < ifaces.length; i++) {
		int bucket = getBucket(ifaces[i]);
		arrayDisplay[bucket] = (byte) (getBucketID(ifaces[i]) & 0xFF);
	    }
	} finally {
	    if (area != null)
		MemoryManager.the().setCurrentArea(r);
	}

	// Finally, create the display for shared-state types
	S3Blueprint classBp =
	    (S3Blueprint) domain.blueprintFor(domain.getMetaClass());
	computeDisplay(classBp);
	classDisplay = classBp.typeDisplay_;
    }

    private byte[] makeDisplay(Type t) {
        byte[] arr = new byte[displaySize()];
        for (int bucket = 0; bucket < arr.length; bucket++) {
            List members = getDescriptor(bucket).members;
            for (int j = members.size() - 1; j >= 0; j--) {
                Type anc = (Type) members.get(j);
                if (!t.isSubtypeOf(anc))
                    continue;
                if ((t == anc) && (arr[bucket] != 0)) {
                    d("duh " + t); // you don't say...
                    continue;
                }
                byte id = (byte) ((j + 1) & 0xFF);
                assert  (arr[bucket] == 0) || (arr[bucket] == id):
		    t + ": disp[" + bucket + "]=" + arr[bucket] + " != "
		    + id + ", anc " + anc;
                arr[bucket] = id;
            }
        }
        return arr;
    }

    final private static int EXTENDS_CUTOFF = 8;
    /*The bucket index of any <em>sui generis</em> type, that is, a type for which
     * no subtype test returns true except against the exact same type.
     * @deprecated   unused!
     *
    final private static int SUI_GENERIS = Integer.MAX_VALUE;
*/
    /** The number of bucket members, and therefore a bucketID, can equal but
     *  never exceed this value. (S1)
     **/
    final private static int MAX_BUCKET_SIZE = 255;

    private int getHierarchyDepth(Type.Class cls) {
        int count = -1;
        do {
            count++;
            cls = cls.getSuperclass();
        } while (cls != null);
        return count;
    }

    // that does not contain a related class
    private int smallestBucketAtTheEnd(int span, Type t) {
        int lastBucket = bucketDescriptors.size() - 1;
        int smallestSize = Integer.MAX_VALUE;
        int choice = lastBucket;
        int i = Math.max(lastBucket - span, EXTENDS_CUTOFF + 1);
        for (; i <= lastBucket; i++) {
            BucketDescriptor bd = getDescriptor(i);
            for (Iterator iter = bd.members.iterator(); iter.hasNext();) {
                Type member = (Type) iter.next();
                //				don't put related guys together!
                if (t.isSubtypeOf(member) || member.isSubtypeOf(t))
                    continue;
            }
            if (bd.members.size() < smallestSize) {
                choice = i;
                smallestSize = bd.members.size();
            }
        }
        return choice;
    }

    /** returns max int for shared state types */
    private int pickInitialBucket(Type type) {
        if (type.isClass()) {
            int depth = getHierarchyDepth((Type.Class) type);
            if (depth <= EXTENDS_CUTOFF)
		return depth;
        }
        if (bucketDescriptors.size() == 0)
            bucketDescriptors.add(new BucketDescriptor());
        int choice =
            Math.max(smallestBucketAtTheEnd(25, type), EXTENDS_CUTOFF + 1);
        // 25?
        return choice;
    }

    private void addTypeToBucket(Type type, int bucket) {
        assert(!type.isSharedState());
        while (bucket >= bucketDescriptors.size())
            bucketDescriptors.add(new BucketDescriptor());
	BucketDescriptor bd = getDescriptor(bucket);
	if (bd.members.size() == MAX_BUCKET_SIZE) {
	    // If a bucket is full, it is probably full of class
	    // descriptors.  We want to keep class descriptors tightly
	    // packed, so we move the full bucket to the end, and
	    // start throwing new class descriptors into an empty
	    // bucket at the original index.  Interfaces will never be
	    // added to the full descriptor, because it is already
	    // full.
	    while (EXTENDS_CUTOFF >= bucketDescriptors.size())
		bucketDescriptors.add(new BucketDescriptor());
	    int newBucket = bucketDescriptors.size();
	    bucketDescriptors.add(bd);
	    for (Iterator it = bd.members.iterator(); it.hasNext(); ) {
		Object t = it.next();
		bucketAssignment.remove(t);
		bucketAssignment.put(t, newBucket);
	    }
	    bd = new BucketDescriptor();
	    bucketDescriptors.set(bucket, bd);
	}
        getDescriptor(bucket).add(type);
        bucketAssignment.remove(type); // in case it was already assigned
        bucketAssignment.put(type, bucket);
    }

    private boolean isArrayOfKnown(Type t) {
	if (!t.isArray())
	    return false;
	Blueprint bpt = domain.blueprintFor(t.getInnermostComponentType());
	return ((S3Blueprint) bpt).hasSubtypeInfo();
    }

    /**
     * When adding a new type to the domain we assign it some type information.
     * In the process we may have to split some buckets and reassign some 
     * other types
     * @param t the newly added type
     */
    // NB: there used to be a recursive check. After tracing through the external
    // methods used here I could not find a case where adding a type would trigger
    // class loading. If I am wrong retrieve the previous version of the class.
    // --jv 5/7/3
    // The method is now synchronized to prevent against concurrent updates.
    synchronized public void addType(Type t) {
	added++;
	if (!(t.isSharedState() || t.isArray() || t.isPrimitive())) {
	    Object r = MemoryPolicy.the().enterHeap();
	    try {
		classTypes++;
		int bucket = pickInitialBucket(t);
		addTypeToBucket(t, bucket);
		getDescriptor(bucket).mark(t);
		if (markAncestorsDescriptors(t))
		    splitBuckets(t);
		unmarkDescriptors();
	    } finally {
		MemoryPolicy.the().leave(r);
	    }
	}
	S3Blueprint bpt = (S3Blueprint) domain.blueprintFor(t);
	if (arrayDisplay.length == bucketDescriptors.size()
	    || t.isSharedState()
	    || isArrayOfKnown(t))
	    // If bucketDescriptors is the same size as an
	    // existing typeDisplay, then we have not moved any
	    // existing types from one bucket to another, we can
	    // generate a new typeDisplay for bpt that is
	    // consistent with all others.
	    //
	    // If we are calling computeDisplay for a shared-state
	    // object, it will simply reuse the typeDisplay for
	    // java.lang.Class.  Because shared-state types are
	    // instantiated during class loading, we do well to
	    // avoid calling recomputeSubtypeInfo every time a
	    // shared-state object is created.
	    //
	    // If t is an array type, we can do subtype tests on
	    // it if both arrayDisplay is valid (it is after the
	    // first call to makeStandardDisplays), and the
	    // innermost component display is valid (this display
	    // is used in subtype tests as well).
	    try {
		computeDisplay(bpt);
	    } catch (OutOfMemoryError e) {
		if (area == null)
		    throw e;
		bpt.setSubtypeInfo(0, UNCOMPUTED, emptyDisplay);
	    }
	else
	    // The layout of bucketDescriptors has changed, we
	    // must regenerate all type displays
	    bpt.setSubtypeInfo(0, UNCOMPUTED, emptyDisplay);
    }

    private void splitBuckets(Type t) {
        for (Iterator i = bucketDescriptors.iterator(); i.hasNext();) {
            BucketDescriptor bd = (BucketDescriptor) i.next();
            if (!bd.isSplitNeeded())
                continue;
            int newBucketCount = bd.markCount() - 1;
            int lastBucket = bucketDescriptors.size() - 1;

            int conflictingCount = 0;
            int nonconflictingCount = 0;
            for (Iterator j = bd.members.iterator(); j.hasNext();) {
                Type anc = (Type) j.next();
                int newBucket;
                if (bd.marked.contains(anc)) {
                    if (conflictingCount == 0) { // keep it where it was
                        conflictingCount++;
                        continue;
                    }
                    newBucket = lastBucket + conflictingCount;
                    conflictingCount++;
                } else {
                    int where = nonconflictingCount % (newBucketCount + 1);
                    if (where == 0) {
                        nonconflictingCount++;
                        continue;
                    }
                    newBucket = lastBucket + where;
                    nonconflictingCount++;
                }
                j.remove();
                addTypeToBucket(anc, newBucket);
            }
        }
    }

    private void unmarkDescriptors() {
        for (int i = bucketDescriptors.size() - 1; i >= 0; i--)
            getDescriptor(i).clear();
    }

    private boolean markAncestorsDescriptors(Type t) {
        boolean isSplitNeeded = false;
        for (Type.Class superclass = t.getSuperclass();
            superclass != null;
            superclass = superclass.getSuperclass()) {
            int bucket = getBucket(superclass);
            BucketDescriptor desc = getDescriptor(bucket);
            desc.mark(superclass);
            isSplitNeeded = isSplitNeeded || desc.isSplitNeeded();
        }
        Type.Interface[] allIfaces = ((S3Type) t).getAllInterfaces();
        for (int i = 0; i < allIfaces.length; i++) {
            int bucket = getBucket(allIfaces[i]);
            BucketDescriptor desc = getDescriptor(bucket);
            desc.mark(allIfaces[i]);
            isSplitNeeded = isSplitNeeded || desc.isSplitNeeded();
        }
        return isSplitNeeded;
    }

    void computeDisplay(S3Blueprint bpt) {
	VM_Area r = null;
	if (area != null)
	    r = MemoryManager.the().setCurrentArea(area);
	try {
	    Type type = bpt.getType();
	    assert(bpt == domain.blueprintFor(type));
	    if (type.isPrimitive()) {
		bpt.setSubtypeInfo(0, type.getUnrefinedName().getTypeTag(),
				   emptyDisplay);
	    } else if (type.isArray()) {
		bpt.setSubtypeInfo(0, ARRAY, arrayDisplay);
	    } else if (type.isSharedState()) {
		bpt.setSubtypeInfo(0, SHARED_STATE, classDisplay);
	    } else {
		int bucket = getBucket(type);
		assert(bucket >= 0);
		int id = (byte) getBucketID(type); // obligatory cast (sign ext)
		bpt.setSubtypeInfo(bucket, id, makeDisplay(type));
		// dom.bucket2bp_[bucket][id & 0xff] = bpt;
	    }
	} finally {
	    if (area != null)
		MemoryManager.the().setCurrentArea(r);
	}
    }

    /**
     * Recompute the type information for a whole domain. Root types
     * are allocated in bucket 0. Primitive types have a tid equal to
     * their type tag (which we assume to be unique), shared states
     * are allocated a tid of 0 (a value that is reserved) and a
     * bucket of MAXINT (guaranteed to fail).<p>
     *
     * See "Java Subtype Tests in Real-Time", ECOOP03, for more
     * details.<p>
     *
     * Any global changes to the subtype data structures of all
     * classes must be done with all other threads stopped.
     */
    synchronized public void recomputeSubtypeInfo() throws PragmaAtomic {
        if (false) dump();

	BasicIO.out.println("recomputing subtype info after adding "
			    + added);
// 	if (added < 200)
// 	    throw new Error("early compuation for " + domain);
	Object r = MemoryPolicy.the().enterHeap();
	try {
	    while (true) {
		try {
		    //dom.bucket2bp_ = new S3Blueprint[displaySize()][256];
		    makeStandardDisplays();
		    for (Iterator iter = domain.getBlueprintIterator();
			 iter.hasNext();)
			{
			    S3Blueprint bpt = (S3Blueprint) iter.next();
			    computeDisplay(bpt);
			}
		    break;
		} catch (OutOfMemoryError e) {
		    if (area == null)
			throw e;
		    BasicIO.out.println("regrowing subtype data");
		}
	    }
	    added = 0;
	} finally {
	    MemoryPolicy.the().leave(r);
	}
    }
}
