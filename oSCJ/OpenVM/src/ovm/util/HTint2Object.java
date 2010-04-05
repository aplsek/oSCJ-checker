// FromType : int
// ToType : Object
// Compare : ==
// Hash :
// NotFound : null
// Default size: 16
// Synchronized:
package ovm.util;

/**
 * Hashtable generated by HTgen.
 */
final public class HTint2Object {
    protected int mask;
    private final static int DEFAULT_SIZE = 16;
    private final Binding[] buckets;
    private static final Object NOTFOUND = null;

    /**
     * Internal binding class. Stores a key, value, and a link to the next
     * binding in the chain.
     */
    static private final class Binding {
        Binding link;
        int key;
        Object value;
        Binding(Binding link, int key, Object value) {
            this.link = link;
            this.key = key;
            this.value = value;
        }
    }

    /**
     * Public default constructor.
     */
    public HTint2Object() {
        this(DEFAULT_SIZE);
    }

    /**
     * Constructor that specifies a hint size.
     */
    public HTint2Object(int hint) {
        int size = 8;
        while (size < hint)
            size *= 2;
        mask = size - 1;
        this.buckets = new Binding[size];
    }

    /**
     * gets something from hashtable and will return NOTFOUND. Does _not_
     * insert anything into the hashtable.
     */
    public final Object get(int key) {
        Binding p = getAtOffset(mask & key, key);
        return (p != null) ? p.value : NOTFOUND;
    }

    /**
     * Put something into the hashtable. Checks to see if the key is already in
     * the hashtable, and if so, updates the value associated with the key.
     */
    public final void put(int key, Object value) {

        int offset = mask & key;
        Binding p = getAtOffset(offset, key);
        if (p == null)
            putAtOffset(offset, key, value);
        else
            p.value = value;
    }

    /**
     * Put something into the hashtable only if it is absent. This will _not_
     * update the value of the specified key.
     */
    public final void putIfAbsent(int key, Object value) {

        int offset = mask & key;
        Binding p = getAtOffset(offset, key);
        if (p == null)
            putAtOffset(offset, key, value);
    }

    /**
     * Return all keys in this table.
     */
    public int[] keys() {
        int length = 0;
        for (int i = 0; i < buckets.length; i++) {
            Binding b = buckets[i];
            while (b != null) {
                length++;
                b = b.link;
            }
        }
        int[] keys = new int[length];
        int index = 0;
        for (int i = 0; i < buckets.length; i++) {
            Binding b = buckets[i];
            while (b != null) {
                keys[index++] = b.key;
                b = b.link;
            }
        }
        return keys;
    }

    /**
     * Get something from the hashtable, and if not present, insert it.
     */
    public final Object getPut(int key, Object value) {

        int offset = mask & key;
        Binding p = getAtOffset(offset, key);
        if (p == null) {
            putAtOffset(offset, key, value);
            return NOTFOUND;
        }

        return p.value;
    }

    /**
     * Insert something into the hashtable, even if it creates a duplicate.
     */
    public final void forcePut(int key, Object value) {
        int offset = mask & key;
        buckets[offset] = new Binding(buckets[offset], key, value);
    }

    /**
     * private function to get something at a specified offset.
     */
    private final Binding getAtOffset(int offset, int key) {
        for (Binding p = buckets[offset]; p != null; p = p.link)
            if (key == (p.key))
                return p;
        return null;
    }

    private final void putAtOffset(int offset, int key, Object value) {
        if (getAtOffset(offset, key) == null)
            buckets[offset] = new Binding(buckets[offset], key, value);
    }

} // End HTint2Object
