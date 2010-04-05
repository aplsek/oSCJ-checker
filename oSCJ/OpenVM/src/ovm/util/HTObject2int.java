// HashtableName : HTObject2int
// KeyType       : Object
// ValueType     : int
// Compare       : .equals
// HashFunction  : .hashCode()
// NotFound      : -1
// Default size  : 16
// Synchronized  : no
// Serializable  : yes
// Iterator      : ovm.util.Iterator
// Profiling     : no 

package ovm.util;

/**
 * This Hashtable is generated by TGen. Its key type is Object and its
 * value type is int.
 * @author Ben L. Titzer
 * @author Christian Grothoff
 * @author TGen2
 **/
public class HTObject2int
    extends ovm.core.OVMBase
    implements java.io.Serializable {

    public static final int MININT = Integer.MIN_VALUE;
    public static final int NOTFOUND = -1;
    public static final HTObject2int EMPTY = new HTObject2int(0) {
        /**
         * Put a key and value into the hashtable. Checks to see if
         * the key is already in the hashtable, and if so, updates
         * the value associated with the key.
         **/
        public void put(Object key, int value) {
            throw new Error("can not put into empty Object");
        }
    }; /* end of EMPTY singleton */

    private static final int DEFAULT_SIZE = 16;

    final private int mask_; // mask for main arrays
    final private int maskCollisions_; // mask for collision table
    private int numElems_; // tracks the size of the hashtable
    Object[] keys_; // array of keys
    int[] values_; // array of value

    /**
     * If there is a collision, we keep the 'collided' elements in here (linked list).
     **/
    Binding[] collisions_;

    public HTObject2int cloneHT() {
        HTObject2int clone = new HTObject2int(this);
        keys_ = (Object[]) keys_.clone();
        values_ = (int[]) values_.clone();
        Binding[] col = new Binding[collisions_.length];
        for (int i = 0; i < collisions_.length; i++)
            if (collisions_[i] != null)
                col[i] = collisions_[i].cloneBinding();
        collisions_ = col;
        return clone;
    }

    /**
     * Internal binding class. Stores a key, value, and a link
     * to the next binding in the chain.
     **/
    static private final class Binding
        extends ovm.core.OVMBase
        implements java.io.Serializable {
        Binding link;
        Object key;
        int value;
        Binding(Binding link, Object key, int value) {
            this.link = link;
            this.key = key;
            this.value = value;
        }
        public String toString() {
            return "B(" + this.key + "," + this.value + ") ->" + link;
        }
        Binding cloneBinding() {
            if (link == null)
                return new Binding(null, key, value);
            else
                return new Binding(link.cloneBinding(), key, value);
        }
    }

    protected HTObject2int complementaryView_;

    public HTObject2int getReadOnlyView() {
        if (complementaryView_ == null) {
            complementaryView_ = new ReadOnly(this);
        }
        return complementaryView_;
    }

    private static class ReadOnly extends HTObject2int {
        ReadOnly(HTObject2int other) {
            super(other);
            complementaryView_ = other;
        }

        public HTObject2int getReadOnlyView() {
            return this;
        }
        public void put(Object key, int value) {
            throw new ovm.util.ReadonlyViewException();
        }
        public int size() {
            return complementaryView_.size();
        }
        public void remove(Object key) {
            throw new ovm.util.ReadonlyViewException();
        }
    }

    /**
     * Public default constructor.
     **/
    public HTObject2int() {
        this(DEFAULT_SIZE);
    }

    protected HTObject2int(HTObject2int other) {
        complementaryView_ = other.complementaryView_;
        this.mask_ = other.mask_;
        this.maskCollisions_ = other.maskCollisions_;
        this.collisions_ = other.collisions_;
        this.keys_ = other.keys_;
        this.values_ = other.values_;
    }

    /**
     * Constructor to build a new hashtable. The size hint passed is used
     * to choose the least power of 2 greater than the hint for the hashtable.
     **/
    public HTObject2int(int hint) {
        int size = 8;
        int sizeCollisions;
        while (size < hint)
            size *= 2;
        mask_ = size - 1;
        sizeCollisions = size >> 3;
        maskCollisions_ = sizeCollisions - 1;
        /* make collision table 1/8th of the size of the main table */
        this.collisions_ = new Binding[sizeCollisions];
        this.keys_ = new Object[size];
        this.values_ = new int[size];

    }

    /**
     * Get a value from hashtable given a key. Returns the value NOTFOUND
     * if the key specified is not found in the hashtable.
     **/
    public final int get(Object key) {
        int hash = mask_ & key.hashCode();
        Object keys_hash = keys_[hash];
        if (keys_hash == null) {

            return NOTFOUND;
        }
        if (key.equals(keys_hash)) {

            return values_[hash];
        }
        return findInCollisions(maskCollisions_ & hash, key);
    }

    /**
     * Put a key and value into the hashtable. Checks to see if
     * the key is already in the hashtable, and if so, updates
     * the value associated with the key. If the {@link #keys_ keys_},
     * {@link #collisions_ collisions_} or {@link #values_ values_} 
     * arrays (or any other internal state) ever change, notify the readonly 
     * view.  
     **/
    public void put(Object key, int value) {
        int hash = mask_ & key.hashCode();
        Object keys_hash = keys_[hash];
        if (keys_hash == null) { // simple insert
            numElems_++;
            keys_[hash] = key;
            values_[hash] = value;

            return;
        }
        if (key.equals(keys_hash)) { // replace
            values_[hash] = value;

            return;
        }
        // collision
        int hashCollision = hash & maskCollisions_;
        insertInCollisions(hashCollision, key, value);

    }

    /**
     * Return the size (number of elements currently present) of the hashtable.
     **/
    public int size() {
        return numElems_;
    }

    /**
     * Private function to search the collision table for a key value.
     * Searches the collisions_ table starting at the given offset until
     * either the correct Binding is found or the end of the list is reached.
     **/
    private final int findInCollisions(int offset, Object key) {
        Binding p;

        for (p = collisions_[offset]; p != null; p = p.link) {
            if (key.equals(p.key)) {
                return p.value;
            }

        }
        return NOTFOUND;
    }

    /**
     * Private function to insert a key and value into the collision table.
     * Searches the collisions_ table starting at the given offset and
     * continues until either the correct Binding is found, or the end of
     * the list is reached. If no Binding is found with the correct key, it
     * will create a new Binding to hold the key and value.
     **/
    private final void insertInCollisions(int offset, Object key, int value) {
        Binding h = collisions_[offset], p;

        for (p = h; p != null; p = p.link) {
            if (key.equals(p.key)) {
                p.value = value;

                return;
            }

        }

        numElems_++;
        collisions_[offset] = new Binding(h, key, value);
    }

    public void remove(Object key) {
        int hash = mask_ & key.hashCode();
        Object result = keys_[hash];
        if (result == null)
            return;
        if (key.equals(result)) { /* found match */
            numElems_--;
            int offset = maskCollisions_ & hash;
            Binding p = collisions_[offset];
            for (; p != null; p = p.link)
                if ((mask_ & p.key.hashCode()) == hash)
                    break;
            if (p == null) {
                keys_[hash] = null;
                values_[hash] = NOTFOUND;
            } else {
                Binding prev = collisions_[offset];
                if (prev == p)
                    collisions_[offset] = p.link;
                else {
                    while (prev.link != p)
                        prev = prev.link;
                    prev.link = p.link;
                }
                keys_[hash] = p.key;
                values_[hash] = p.value;
            }
        } else {
            int offset = maskCollisions_ & hash;
            Binding p = collisions_[offset];
            for (; p != null; p = p.link)
                if (key.equals(p.key))
                    break;
            if (p != null) {
                numElems_--;
                Binding prev = collisions_[offset];
                if (prev == p) {
                    collisions_[offset] = p.link;
                } else {
                    while (prev.link != p)
                        prev = prev.link;
                    prev.link = p.link;
                }
            }
        }
    }

    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append(super.toString());
        result.append("=(" + size() + "){");
        for (int i = 0; i < keys_.length; i++) {
            result.append("(");
            result.append(" " + keys_[i]);
            result.append(",");
            result.append(" " + values_[i]);
            result.append("),\n");
        }
        result.append("Collisions: ");
        for (int i = 0; i < collisions_.length; i++) {
            result.append(collisions_[i]);
            result.append(",\n");
        }
        return result.toString();
    }

    public Iterator getIterator() {
        return new Iterator();
    }

    public final class Iterator implements ovm.util.Iterator {
        private int position_;
        private boolean iteratingOverKeys_;
        private Binding currentBinding_;
        private boolean reachEnd;

        Iterator() {
            currentBinding_ = collisions_[0];
            position_ = -1;
            iteratingOverKeys_ = true;
            reachEnd = false;
            findNext();
        }
        public boolean hasNext() {
            return !reachEnd;
        }
        public Object next() {
            Object res;
            if (iteratingOverKeys_) {
                res = keys_[position_];
            } else {
                res = currentBinding_.key;
            }
            findNext();
            return res;
        }

        public int nextValue() {
            int res;
            if (iteratingOverKeys_) {
                res = values_[position_];
            } else {
                res = currentBinding_.value;
            }
            findNext();
            return res;
        }

        public void remove() {
            throw new Error("Remove Not Implemented");
        }

        private void findNext() {
            if (iteratingOverKeys_) {
                position_++;
                while (position_ < keys_.length && keys_[position_] == null)
                    position_++;
                if (position_ == keys_.length) {
                    iteratingOverKeys_ = false;
                    position_ = 0;
                    while (position_ < collisions_.length
                        && collisions_[position_] == null)
                        position_++;
                    if (position_ == collisions_.length)
                        reachEnd = true;
                    else
                        currentBinding_ = collisions_[position_];
                }
            } else {
                currentBinding_ = currentBinding_.link;
                if (currentBinding_ == null) {
                    position_++;
                    while (position_ < collisions_.length
                        && collisions_[position_] == null)
                        position_++;
                    if (position_ == collisions_.length)
                        reachEnd = true;
                    else
                        currentBinding_ = collisions_[position_];
                }
            }
        }
    } // End Iterator

} // End HTObject2int
