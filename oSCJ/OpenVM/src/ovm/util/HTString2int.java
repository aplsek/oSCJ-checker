/** 
 * This hashtable class is generated by TGen 2.0r.
 *
 * PLEASE DO NOT MODIFY THIS SOURCE.
 *
 * @author Ben L. Titzer
 * @author Christian Grothoff
 * @author TGen2
 **/
// HashtableName : HTString2int
// KeyType       : String
// ValueType     : int
// Compare       : .equals
// HashFunction  : .hashCode()
// NotFound      : MININT
// Default size  : 1024
// Synchronized  : no
// Serializable  : no
// Profiling     : no
package ovm.util;


/**
 * This Hashtable is generated by TGen. Its key type is String and its
 * value type is int.
 * @author Ben L. Titzer
 * @author Christian Grothoff
 * @author TGen2
 **/
final public class HTString2int {

    public static final int MININT = -0x7fffffff;
    public static final int NOTFOUND = MININT; 

    private static final int DEFAULT_SIZE = 1024;

    private int mask_;
    private int maskCollisions_;
    private final String[] keys_;
    private final int[] values_;

    /**
     * If there is a collision, we keep the 'collided' elements in here (linked list).
     **/
    private final Binding[] collisions_;

    /**
     * Internal binding class. Stores a key, value, and a link
     * to the next binding in the chain.
     **/
    static private final class Binding {
	Binding link;
	String key;
	int value;
	Binding(Binding link, String key, int value) {
	    this.link = link;
	    this.key = key;
	    this.value = value;
	} 
    }

    /**
     * Public default constructor.
     **/
    public HTString2int() {
	this(DEFAULT_SIZE);
    }

    /**
     * Constructor that specifies a hint size.
     **/
    public HTString2int(int hint) {	    
        int size = 8;
	int sizeCollisions;
        while ( size < hint ) size *= 2;
        mask_ = size-1;
	sizeCollisions = size>>3;
	maskCollisions_ = sizeCollisions-1; /* make collision table 1/8th of the size of the main table */
	this.collisions_ = new Binding[sizeCollisions];
	this.keys_ = new String[size];
	this.values_ = new int[size];
    }

    /**
     * gets something from hashtable and will return NOTFOUND.
     * Does _not_ insert anything into the hashtable.
     **/
    public final int get(String key) {
	int hash = mask_&key.hashCode();
	String keys_hash = keys_[hash];
	if (keys_hash == null)
	   return NOTFOUND;
	if (key.equals(keys_hash))
	   return values_[hash];
	Binding p = getAtOffset(maskCollisions_&hash, key);
	return (p != null) ? p.value : NOTFOUND;
    }

    /**
     * Put something into the hashtable. Checks to see if
     * the key is already in the hashtable, and if so, updates
     * the value associated with the key.
     **/
    public final void put(String key, int value) {
	int hash = mask_&key.hashCode();
   	String keys_hash = keys_[hash];
	if (keys_hash == null) { // simple insert
           keys_[hash] = key;
           values_[hash] = value;
	   return;
        }     
        if (key.equals(keys_hash)) { // replace
	   values_[hash] = value;
           return;
        } 
        // collision
	int hashCollision = hash&maskCollisions_;
	Binding p = getAtOffset(hashCollision, key);
	if ( p == null ) {
	    putAtOffset(hashCollision, key, value);
        }
	else {
	    p.value = value;
        }
    }


    /**
     * private function to get something at a specified offset.
     **/
    private final Binding getAtOffset(int offset, String key) {
	Binding p;

	for (p = collisions_[offset]; p!= null; p = p.link) {
	    if (key.equals(p.key)) break;
        }
	return p;
    }

    private final  void putAtOffset(int offset, 
					 String key,
					 int value) {
	if (getAtOffset(offset, key) == null) {
	    collisions_[offset] = new Binding(collisions_[offset], key, value);
	
    }
}

} // End HTString2int

