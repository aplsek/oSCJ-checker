package ovm.core.repository;

import ovm.core.OVMBase;
import ovm.core.services.memory.MemoryPolicy;
import ovm.util.UnicodeBuffer;
import s3.core.S3Base;
import s3.util.PragmaTransformCallsiteIR.BCdead;

/**
 * The <code>UTF8Store</code> is a data structure used for storing
 * UTF8 character strings within the {@link Repository}. It provides a
 * high-level interface to retrieve and install UTF8 character strings.
 * Any given string is installed only once. When a string is installed, the
 * offset of that string in the data store is returned, and that offset can be
 * used for future access to the string characters.
 * It is a highly
 * concurrent data structure designed for direct lock-free reading of the
 * data store, requiring synchronization only for performing updates (see
 * below).
 * <p>Internally the data store consists of a simple array for the character
 * data and a specialised hashtable for maintaining an index into that array.
 *
 * <h3>Synchronization of the UTF8 data store</h3>
 *
 * All UTF8 character strings are stored in a large array, and
 * accessed by using a index (hashtable) to find where in the
 * array a given string is. When a character string is installed
 * its offset is returned, and that offset can be used later to
 * retrieve that character string.
 *
 *<p>This data structure allows for highly concurrent read access,
 * without the need to acquire a lock, while all writes are done
 * under locking. The index (hashtable) also allows for concurrent
 * <code>get</code> operations without the need for locking, but
 * must be updated (using <code>put</code>) only when the main lock
 * is held. Updates to the data store and the index <em>must</em> be done
 * atomically within a single synchronized region.
 *
 * <p>Lock free reading is possible due to the way in which this
 * data store is used:
 * <ul>
 * <li>No data written into the array is ever modified.
 * <li>No information in the index is ever deleted, or changed
 * <li>The data store is only ever accessed using an offset obtained
 * from the index.
 * </ul>
 * Because of these properties we need only ensure that reads are
 * correctly ordered and visible across threads. This is achieved by
 * using <code>volatile</code> variables in key places:
 * <ul>
 * <li>The {@link #utf8s_} reference to the data array
 * <li>A entry counter within the index hashtable (any flag would do, but
 * maintaining a count of the number of entries is useful in its own right -
 * thanks to Doug Lea for the suggestion.)
 * </ul>
 * <p>Correct reading is then ensured by the following protocol:
 * <ol>
 * <li>After writing any data into the array we write to {@link #utf8s_}
 * (setting it to its own value) to establish a &quot;happens before&quot;
 * relationship. This ensures that subsequent reads of the array will
 * return the values just written.
 * <li>When reading from the array we use the <code>volatile</code>
 * {@link #utf8s_} reference. This establishes a &quot;happens after&quot;
 * relationship with any previous write, and so ensures that previously
 * written elements will be returned.
 * <p>Note that it is only necessary to use the {@link #utf8s_} reference
 * once per set of reads, to establish the relationship, and further that
 * is it more efficient to not use the <code>volatile</code> reference for
 * every array access. For example, you should use this:
 * <pre><code>    byte[] local = utf8s_;
 *     byte[] myCopy = new byte[len];
 *     for (int i = 0; i < myCopy.length; i++) {
 *         myCopy[i] = local[offset+i];
 *     }
 * </code></pre>
 * Rather than:
 * <pre><code>    byte[] myCopy = new byte[len];
 *     for (int i = 0; i < myCopy.length; i++) {
 *         myCopy[i] = utf8s_[offset+i];
 *     }
 * </code></pre>
 * Also note that using <code>System.arraycopy</code> a local copy of the
 * <code>volatile</code> variable is implicitly made. 
 * </li>
 * <li>The <code>get</code> and <code>put</code> operations of the 
 * hashtable use a similar technique to allow correct reading of keys
 * and values by always reading/writing the <code>volatile</code> flag
 * variable, as appropriate.
 * <li>We ensure that we write into the data store before updating the 
 * index to ensure that noone can find a string before it has actually
 * been stored.
 * </ol>
 * <p><b>NOTE:</b> The idiom of assigning a <code>volatile</code> variable
 * to itself establishes the relationships necessary to allow reads to
 * occur without locking, <b>but</b> the idiom itself is not thread-safe
 * (unless the same value is always held by the reference - which it is
 * not in this case) and so must be done within a locked region.
 * <p>It is also very important to note that these relationships
 * established using <code>volatile</code> variables only apply to uses of
 * the <b>same</b> variable. For example, passing a <code>volatile</code>
 * reference to a constructor which stores it into a <code>volatile</code>
 * field of that object, does not establish any relationship between uses
 * of those two variables. This is why the index must be an inner class so
 * it can directly access the {@link #utf8s_} reference.
 * <p>Finally, note that the semantics of <code>volatile</code> variables that
 * we rely upon here are those defined in the forthcoming amendment to the
 * Java Memory Model. To be strictly correct on all current VM's we should
 * replace the volatile accesses with lock usage. This should not be 
 * necessary in our current environment because execution under the JDK VM 
 * only occurs in a single-threaded manner, while execution in a multi-threaded
 * manner only occurs in our own OVM, under a user-level thread manager, which
 * trivialy enforces the memory model requirements of the improved JMM.
 * We should as note the following:
 * <ul>
 * <li>The HotSpot server virtual machine also enforces the new Java Memory
 * model
 * <li>We need to ensure that OVM does not attempt to do any inappropriate
 * optimisations on <code>volatile</code> variables
 * <li>We need to take care when moving OVM to a native threading
 * implementation.
 * </ul>
 * <h3>Performance Issues</h3>
 * <p>The HTUtf8 class will probably exhibit better performance if a single
 * array with alternating keys and values is used. This gives better locality.
 * Given that these arrays are very large a given key and value will reside in
 * different memory pages. See the coments in the JDK 1.4 IdentityHashmap 
 * class by Doug Lea and Josh Bloch.
 *
 * @author Jan Vitek, David Holmes - synchronization
 *
 */
public class UTF8Store extends S3Base {
    /**
     * log_2 {@link #UTF_SIZE}
     */
    static final int UTF_SHIFT = 17;
    
    /**
     * The size of each block in {@link #utf8s_}.  This should be large
     * enough to accomidate a maximum-sized utf8 constant and its
     * length.  The maximum size is 2^{16}-1, and a utf8 length
     * occupies 2 bytes
     */
    static final int UTF_SIZE = 1 << UTF_SHIFT;

    /**
     * UTF_MASK can be used to extract a byte[] index from a UTF
     * index.
     */
    static final int UTF_MASK = UTF_SIZE - 1;

    public static final UTF8Store _;
    static {
	Object r = MemoryPolicy.the().enterRepositoryDataArea();
	try {  _ = new UTF8Store(16*UTF_SIZE, 62000); }
	finally { MemoryPolicy.the().leave(r); }
    }

    // ------------------------ Nested Classes -----------------------

    /** 
     * A specialised hashtable mapping byte sequences to the offsets of 
     * those byte sequences in an underlying data store. Although somewhat
     * generic in design this particular inner class hooks into the UTF8
     * character string store of the enclosing UTF8Store instance. 
     * <p>The purpose of this hashtable is to provide an indexing facility 
     * for the UTF8 store. Given a character string, it will return the 
     * offset of that character string in the store. Rather than storing
     * the character string as the key (which would result in the string
     * existing both here and in the store) the hashkey of the string is 
     * stored instead. When we want to compare &quot;keys&quot; we use
     * the value stored in the hashtable to directly compare the given
     * character string with the character string located at the returned
     * offset in the UTF8 store - if any.
     * <h3>Synchronization Description</h3>
     * <p>Based on the TGEN generated version, this class has been adapted
     * to provide lock-free access for {@link #get get} operations. And to
     * allow reading of the underlying UTF8 store without locking.
     * All {@link #put put} operations must be done holding the appropriate
     * external lock.
     * See the documentation in the {@link UTF8Store} class for 
     * details of how the overall synchronization control works.
     * <p>The local synchronization scheme is as follows. As mentioned, all
     * {@link #put put} operations must be done holding the external lock.
     * Further, the way in which this hashtable is used means that data in 
     * the UTF8 store is guaranteed to be valid when it is compared against
     * inside the hashtable.
     * <p>When storing into the hashtable we must store both a key (the hash
     * of the original character string) and a value (the offset of that 
     * string in the UTF8 store). This is done either in the primary table 
     * bin (if it is empty) or in a collision bin. 
     * In both cases we can either use sufficient volatile accesses to
     * ensure that the key and value are always seen as a valid pair, or
     * we can allow one or the other to be invalid. Because we need
     * to compare &quot;keys&quot; using the stored offset to access the
     * UTF8 data store, we tend to read a value before we read the associated
     * key. We could use volatile accesses to ensure that the value and key
     * are stored in the right order, but in practice this is not necessary.
     * Due to the default initialisation of the underlying arrays
     * (guaranteed by the VM), and the fact that array elements only ever 
     * hold one value (other than the zero default), we know that we will
     * either read zero or the correct value - we can never read junk.
     * For hashtable values, zero is not valid and indicates that the entry
     * is not present - so if we read zero we don't look at the key and just
     * report the entry as not found. If we read a non-zero value, then we 
     * will read the key. A zero key is possible so finding zero does not
     * mean that the entry is not present - instead we will do the actual
     * character string comparison and so return the correct result.
     * This means that we do not have to force a strict ordering on the
     * way that the key and value are written. 
     * <p>In simple terms, given the concurrent nature of {@link #get get}
     * and {@link #put put}, if an entry is being stored at the same time as
     * we are searching for it, then it is not predictable as to whether
     * the entry will be found - a basic race condition. 
     * Note that correct
     * usage of this hashtable requires that {@link #put put} only be invoked
     * for entries that are not present, and so a {@link #get get} that 
     * indicates
     * &quot;not found&quot; must be retried under synchronization before the
     * {@link #put put} is performed.
     * <p>It would take a lot of profiling and benchmarking to determine 
     * whether the use of additional volatile accesses was worth the cost.
     * Given the current usage scenario is for a predominant write phase
     * followed by a mostly reading phase, there seems little point trying
     * to improve the behaviour of a concurrent read and write. Rather we 
     * want to optimise the performance of read operations.
     *
     * @see UTF8Store
     * @author Jan Vitek, David Holmes - for synchronization modifications
     *
     **/
    private class HTUtf8 extends ovm.core.OVMBase {

        // NOTE: Given the size of these arrays it might be better to use
        // a single array with alternating keys and values, as that may
        // provide better data locality and thus better performance.
        // See the JDK 1.4 IdentityHashMap implementation - DH

        /** The collision table. Each element array consists of an array
         *  of alternating keys and values for a given index into the
         *  primary table.
         */
        private final int[][] collisions_;

        /** 
         * Synchronisation flag used to ensure that read operations will
         * see the results of write operations, without the need for
         * acquiring a lock. Any flag would do, but maintaining a count
         * provides useful information that might be needed during
         * debugging and testing. This value should only be written when
         * the lock is held.
         */
        private volatile int count = 0;

        /** The primary &quot;bin&quot; for storing keys */
        private final int[] keys_;
        /** 
         * Based on the size of the key/value arrays, this mask is used 
         * to extract the lower n-bits of a hashed value, to use as an index 
         * into those arrays.
         */
        private final int mask_;
        /** 
         * Based on the size of the collisions array, this mask is used 
         * to extract the lower n-bits of a hashed value, to use as an index 
         * into that arrays.
         */
        private final int maskCollisions_;
        /** The primary &quot;bin&quot; for storing values */
        private final int[] values_;

        /**
         * Construct the hashtable with a size equal to the first power of
         * two greater than the requested size.
         * 
         * @param minSize the minimum size of the hashtable
         **/
        HTUtf8(int minSize) {
            int size = 8;
            while (size < minSize)
                size *= 2;
            mask_ = size - 1;
            /* make collision table 1/8th of the size of the main table */
            int sizeCollisions = size >> 3;
            maskCollisions_ = sizeCollisions - 1;
            this.collisions_ = new int[sizeCollisions][];
            this.keys_ = new int[size];
            this.values_ = new int[size];
        }

        /**
         * Compares the UTF8 character string stored in <code>data</code>
         * with that stored at offset <code>s_pos</code> in the UTF8 store.
         * This method is thread-safe and requires no locks to be held.
         * @param s_pos the offset in the UTF8 store to start comparison
         * @param t     the UnicodeBuffer being searched for
         *
         * @return <code>true</code> if the specified string exactly matches
         * that stored from position <code>s_pos</code> in the UTF8 store
         *
         */
        private boolean compareBytes(int s_pos,
				     UnicodeBuffer t) {
	     // volatile read ensures valid elements
            byte[] localUtf = utf8s_[s_pos >> UTF_SHIFT];
	    s_pos &= UTF_MASK;

            // using non-volatile is more efficient for the array traversal

            // find length of string stored from s_pos
            int s_len =
                (((localUtf[s_pos + 0] & 0xFF) << 8)
                | (localUtf[s_pos + 1] & 0xFF));
            if (s_len != t.byteCount())
                return false;

            // compare backwards as many strings will have common starts
            // eg. java.lang.
            int s_curr = s_pos + 2;
	    t.rewind();
	    while (t.hasMore())
		if (localUtf[s_curr++] != (byte) t.getByte())
		    return false;
	    return true;
        }

        /**
         * Return the number of entries stored in this table.
         * @return the number of entries stored in this table.
         */
        int count() {
            return count;
        }

        /**
         * Return the index of the specified UTF8 character string.
         * This method is thread-safe and requires no locks to be
         * held.
	 * @param buf the UTF8 string being searched for
         *
         * @return the index of the specified string, or -1 if not found.
         */
        int get(UnicodeBuffer buf) {
	    int key = buf.hashCode();

            int hash = count; // volatile read to ensure visibility

            hash = mask_ & key; // map the hash key to a bin
            int offset = values_[hash];

            // If we see a value for values_[hash] then we know that the
            // corresponding data has been written to the byte array.
            // Otherwise this key is not present.

            if (offset == 0)
                return -1; // not present

            // see if it matches
            if (key == keys_[hash] && compareBytes(offset, buf)) {
                return offset; // found it
            } else if (key == 0 && offset == 0) { // odd case when hash==0
                return -1; // not present
            } else {
                // this key is not in the primary table, so search
                // the collision tables
		return getCollision(key, buf);
            }
        }

        /** 
	 * Searches for the given character string in the collision tables.
	 * This method is thread-safe and requires no locks to be held.
	 * @param key the hash key representing the UTF8 character string
	 * @param buf the character sequence being searched for
	 *
	 * @return the index of the specified string, or -1 if not found.
	 */
        private int getCollision(int key, UnicodeBuffer buf) {
            // reading the count is not essential as we've already
            // synced in the caller. This acts as a "refresh" and allows us to
            // see an entry we might otherwise have missed.
            int hash = count;

            hash = maskCollisions_ & key; // map to a collision bin

            // if we get a non-null value then we know that all elements of
            // the array are valid
            int[] collisions = collisions_[hash];

            if (collisions == null)
                return -1; // not present

            // iterate down through the key values of each (key,value) pair

            for (int i = collisions[0] - 2; i > 0; i -= 2) {
                int collKey = collisions[i];
                if (collKey == key) {
                    int offset = collisions[i + 1];
                    // assert: offset != 0
                    if (compareBytes(offset, buf)) {
                        return offset;
                    }
                }
            }
            return -1;
        }

        /** 
         * Stores the given key/value pair into the hashtable.
         * This method is <b>not</b> thread-safe and must only be invoked
         * when holding the lock that protects writes to the UTF8 data store.
         * Additionally, the entry being put into the hashtable must be known
         * not to exist. This means that a {@link #get get} must be performed
         * under synchronisation before deciding to invoke <code>put</code>.
         *
         * @param key the key to be stored (actually a hash of the character
         * string that is notionally being stored)
         * @param value the offset in the UTF8 data store where the character
         * string can be found.
         *
         */
        void put(int key, int value) {
            int hash = mask_ & key;
            int offset = values_[hash];
            if (offset == 0) {
                // storing the key first in theory increases the chances of
                // a reader seeing the complete entry if it sees the value.
                // To guarantee this would require an additional volatile
                // write between the two stores.
                keys_[hash] = key;
                values_[hash] = value;
                count++; // volatile write to force visibility
                return;
            }
            // Primary table is in use so use a collision table

            // Each entry in a collision array is a (key,value) pair
            // which is stored as coll[i] and coll[i+1].  coll[0] is
            // a pointer to the next free pair, and we grow the coll
            // array exponentially.

	    // If the repository data area is not garbage collected,
	    // growing this array linearly leads to unbounded leaks,
	    // but growing it exponentially should be more-or-less OK.

            hash = maskCollisions_ & key;
            int[] collisions = collisions_[hash];
	    if (collisions == null) { // make new array
		collisions = new int[3];
		collisions[0] = 1;
	    } else if (collisions[0] == collisions.length) {
		// grow by factor of 2
		int[] oldcol = collisions;
		collisions = new int[(collisions.length << 1) - 1];
		System.arraycopy(oldcol, 0, collisions, 0, oldcol.length);
	    }
            int i = collisions[0];
            // fill in array elements before making array accessible
            collisions[i] = key;
            collisions[i + 1] = value;
	    collisions[0] = i + 2;
            // Doing a volatile write to "flush" the above will help
            // (in theory) a reader see the new entry correctly. If we
            // don't do it then we have the same situaton as for the primary
            // and the reader might not see the entry. 

            collisions_[hash] = collisions;
            count++; // volatile write so readers can see
        }

    } // End HTUtf8

    /** 
     *  The next position to insert into
     *  <code>{@link #utf8s_}[{@link #curblock_}]</code>,This is only
     *  accessed by code that writes into {@link #utf8s_} and so is
     *  only accessed when the lock is held.  The absolute Utf8 index
     *  is never zero (either cursor_ and curblock_) and so zero
     *  can be used as an invalid offset flag. 
     */
    private int cursor_;

    /**
     * Index into the fixed size block of bytes currently being
     * filled.
     */
    private int curblock_;
    
    /** 
     * Index for the utf8 strings. This hashtable's keys are the hash of the
     * string that is stored, and the value is the offset of that string
     * within {@link #utf8s_} 
     */
    private final HTUtf8 utf8Index_;

    /** 
     * The locking object used for writing UTF8 values 
     */
    private final Object utf8Lock_;

    /**
     * s3.services.bootimage.GC needs access to this internal field.
     * Other code should pretend it is private.
     * <p>
     * All the utf8strings of the VM are stored in this array.
     * Each element is a large fixed-sized block of bytes.
     * Utf8strings include all
     * class, field and method names, descriptors. 
     * <p>Both this variable, and the data in the array, should only be 
     * written when the lock is held.
     **/
    public volatile byte[][] utf8s_;

    
    // ----- Construction
    /**
     * Construct a new UTF8 data store with the given data size and
     * index size.
     *
     * @param dataSize the size of the data store in bytes
     * @param indexSize the minimum size of the index, in entries. This will
     * be rounded up to the nearest power-of-two.
     *
     */
    UTF8Store(int dataSize, int indexSize) {
	if (dataSize < UTF_SIZE)
	    dataSize = UTF_SIZE;
        utf8s_ = new byte[dataSize >> UTF_SHIFT][];
	utf8s_[0] = new byte[UTF_SIZE];
        utf8Index_ = new UTF8Store.HTUtf8(indexSize);
        utf8Lock_ = utf8Index_;
	curblock_ = 0;
        cursor_ = 1; // index 0 must never be used
    }

    /**
     * Checks if the storage array has sufficient capacity for another
     * string of length <code>len</code>. If not, the array is grown so
     * that there is sufficient capacity.
     *
     * @param len the length of the character sequence we need capacity for
     *
     */
    private void checkUtf8Capacity(int len) {
        // assert: utf8Index_ is locked
        if (len + cursor_ >= UTF_SIZE) {
	    curblock_++;
	    cursor_ = 0;
	    if (curblock_ == utf8s_.length) {
		byte[][] b = new byte[utf8s_.length * 2][];
		System.arraycopy(utf8s_, 0, b, 0, utf8s_.length);
		utf8s_ = b;
	    }
	    utf8s_[curblock_] = new byte[UTF_SIZE];
        }
    }

    // ------ Methods -----------------

    public void dumpStats() throws BCdead {
	int totBytes = 0;
	for (int i = 0; i < utf8s_.length; i++) {
	    if (utf8s_[i] != null)
		totBytes += utf8s_[i].length;
	}

	int[][] coll = utf8Index_.collisions_;
	int totColl = 0;
	int collWords = 0;
	int[] hist = new int[0];
	for (int i = 0; i < coll.length; i++) {
	    if (coll[i] != null) {
		// coll[i][0] is the index of the next free entry.  If
		// the next free entry is 5, we have two entries at
		// indexes 1,2 and 3,4.
		int nEntries = (coll[i][0] - 1)/2;
		if (nEntries >= hist.length) {
		    int[] newHist = new int[coll[i].length+1];
		    System.arraycopy(hist, 0, newHist, 0, hist.length);
		    hist = newHist;
		}
		hist[nEntries]++;
		totColl++;
		collWords += coll[i].length;
	    }
	}

	System.out.println("UTF8Store contains " + utf8Index_.count() +
			   " entries in " + totBytes + " bytes");
	System.out.println("There are " + totColl +
			   " hash buckets with collisions:");
	System.out.println("collision length\t# occurrences");
	for (int i = 0; i < hist.length; i++)
	    if (hist[i] != 0)
		System.out.println(i + "\t\t\t" + hist[i]);
	System.out.println("There are " + collWords +
			   " words of collision data in " + totColl +
			   " collision arrays");
    }
		     
    /**
     * Return the id of a utf8 character string stored in the repository.
     * @param b the string to search for 
     * @return The id of the given string in the repository, or -1 if
     * the string is not in the repository.
     **/
    public int findUtf8(UnicodeBuffer b) {
	return utf8Index_.get(b);
    }

    /**
     * Return an immutable reference to the a character sequence
     * stored in the repository.
     * @param pos index of the character sequnce
     **/
    public UnicodeBuffer getUtf8(int pos) {
	int len = getUtf8Length(pos);
	int block = pos >> UTF_SHIFT;
	int off = pos & UTF_MASK;
	return UnicodeBuffer.factory().wrap(utf8s_[block], off+2, len);
    }

    public int getUtf8Offset(int pos) {
	return (pos+2) & UTF_MASK;
    }

    public byte[] getUtf8Store(int pos) {
	return utf8s_[pos >> UTF_SHIFT];
    }

    /**
     * Return length of an utf8string. <p> 
     * A utf8string's length is currently encoded in the first two bytes
     * of the string. We assume the length to be bound by 
     * <code>MAX_CHAR</code>. 
     * @param pos id of the utf8string.
     * @return The length of the utf8string.
     **/
    public int getUtf8Length(int pos) {
	byte[] b = utf8s_[pos >> UTF_SHIFT];
	pos &= UTF_MASK;
        int retval = (((b[pos + 0] & 0xFF) << 8) | (b[pos + 1] & 0xFF));
        return retval;
    }

    /**
     * Install the given UTF8 string.
     * @param b the string to install
     * @return  The unique id of the utf8string.
     */
    public int installUtf8(UnicodeBuffer b) {
        // First see if its already present. This is safe to do
        // without locking
        int present = utf8Index_.get(b);
        if (present >= 0)
            return present;

	Object r = MemoryPolicy.the().enterRepositoryDataArea();
	try {
	    int len = b.byteCount();
            checkUtf8Capacity(len + 2); // Grow if needed.

            int index = cursor_; // save the insertion point
	    byte[] block = utf8s_[curblock_];

            // 1. We write the string into the byte array, starting
            //    with its length. This has to be done before any
            //    one can see that the string is present

            writeInt16(block, len); // advances the cursor_
	    b.rewind();
	    int i;
	    for (i = cursor_; b.hasMore(); i++) {
		block[i] = (byte) b.getByte();
	    }
        
	    assert(i == cursor_ + len);
	    cursor_ = i;

            // 2. We do a "dummy" volatile write to ensure that anyone
            //    reading utf8s_ will see the values just written

            utf8s_ = utf8s_;

	    // Make it an absolute utf8 index
	    index += curblock_ << UTF_SHIFT;
            //   System.out.println("installUtf8("+new String(bc));

            // 3. We update the index for the new string. This is done
            //    last so that finding it in the index guarantees that
            //    the data is in the array.

            utf8Index_.put(b.hashCode(), index);

            return index;
        } finally { MemoryPolicy.the().leave(r); }
    }

    // write the 16-bit arg into the byte store: high-byte then low-byte
    private void writeInt16(byte[] b, int v) {
        int high = (v >> 8) & 0xFF;
        b[cursor_++] = (byte) high;
        int low = v & 0xFF;
        b[cursor_++] = (byte) low;
    }

    /**
     * Write a utf8 string to an output stream
     * @param pos the id of the UTF8 character string
     * @param out the output stream  to write to
     */
    public void writeUtf8(java.io.OutputStream out, int pos)
        throws java.io.IOException {
        int length = getUtf8Length(pos);
	byte[] b = getUtf8Store(pos);
	int off  = getUtf8Offset(pos);
        out.write(b, off, length);
    }

}
