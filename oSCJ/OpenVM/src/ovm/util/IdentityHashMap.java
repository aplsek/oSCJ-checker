/* HashMap.java -- a class providing a basic hashtable data structure,
   mapping Object --> Object
   Copyright (C) 1998, 1999, 2000 Free Software Foundation, Inc.
This file is part of GNU Classpath.
It has been modified slightly to fit the OVM framework.
 */
package ovm.util;

import java.io.Serializable;

import ovm.core.domain.ObjectModel;

/**
 * This class provides a hashtable-backed implementation of the Map
 * interface.  It uses a hash-bucket approach; that is, hash
 * collisions are handled by linking the new node off of the
 * pre-existing node (or list of nodes).  In this manner, techniques
 * such as linear probing (which can casue primary clustering) and
 * rehashing (which does not fit very well with Java's method of
 * precomputing hash codes) are avoided.  Under ideal circumstances
 * (no collisions, HashMap offers O(1) performance on most operations
 * (<pre>containsValue()</pre> is, of course, O(n)).  In the worst
 * case (all keys map to the same hash code -- very unlikely), most
 * operations are O(n).
 *
 * HashMap is part of the JDK1.2 Collections API.  It differs from
 * Hashtable in that it accepts the null key and null values, and it
 * does not support "Enumeration views."
 *
 * @author         Jon Zeppieri
 * @author         Ben L. Titzer
 * @version        $Revision: 1.8 $
 * @modified $Id: IdentityHashMap.java,v 1.8 2004/01/19 14:47:23 jv Exp $ */
public class IdentityHashMap
    extends AbstractMap
    implements Map, Cloneable, Serializable {
    // STATIC (CLASS) VARIABLES ------------------------------------------

    /**
    * the default capacity for an instance of HashMap -- I think this
    * is low, and perhaps it shoudl be raised; Sun's documentation mildly
    * suggests that this (11) is the correct value, though
    */
    private static final int DEFAULT_CAPACITY = 11;

    /** the default load factor of a HashMap */
    private static final float DEFAULT_LOAD_FACTOR = 0.75F;

    /** used internally to parameterize the creation of set/collection views */
    private static final int KEYS_ = 0;

    /** used internally to parameterize the creation of set/collection views */
    private static final int VALUES_ = 1;

    /** used internally to parameterize the creation of set/collection views */
    private static final int ENTRIES_ = 2;

    // INSTANCE VARIABLES -------------------------------------------------

    /** the capacity of this HashMap:  denotes the size of the bucket array */
    int capacity;

    /** the size of this HashMap:  denotes the number of key-value pairs */
    private int size;

    /** the load factor of this HashMap:  used in computing the threshold
     * @serial  */
    float loadFactor;

    /* the rounded product of the capacity and the load factor; when the number of
    * elements exceeds the threshold, the HashMap calls <pre>rehash()</pre>
    * @serial
    */
    private int threshold;

    /**
     * this data structure contains the actual key-value mappings; a
     * <pre>BucketList</pre> is a lightweight linked list of
     * "Buckets", which, in turn, are linked nodes containing a
     * key-value mapping and a reference to the "next" Bucket in the
     * list */
    private  Bucket[] buckets;

    /**
    * counts the number of modifications this HashMap has undergone;
    * used by Iterators to know when to throw
    * ConcurrentModificationExceptions (idea ripped-off from Stuart
    * Ballard's AbstractList implementation) */
    private  int modCount;

    // CONSTRUCTORS ---------------------------------------------------------

    /**
     * construct a new HashMap with the default capacity and the default
     * load factor
     */
    public IdentityHashMap() {
        init(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    /**
    * construct a new IdentityHashMap with a specific inital capacity and load factor
    *
    * @param   initialCapacity     the initial capacity of this HashMap (>=0)
    * @param   initialLoadFactor   the load factor of this HashMap
    *                              (a misnomer, really, since the load factor of
    *                              a HashMap does not change)
    *
    * @throws   IllegalArgumentException    if (initialCapacity < 0) ||
    *                                          (initialLoadFactor > 1.0) ||
    *                                          (initialLoadFactor <= 0.0)
    */
    public IdentityHashMap(int initialCapacity, float initialLoadFactor)
        throws IllegalArgumentException {
        if (initialCapacity < 0
            || initialLoadFactor <= 0
            || initialLoadFactor > 1)
            throw new IllegalArgumentException();
        else
            init(initialCapacity, initialLoadFactor);
    }

    /**
    * construct a new HashMap with a specific inital capacity
    *
    * @param   initialCapacity     the initial capacity of this HashMap (>=0)
    * @throws   IllegalArgumentException    if (initialCapacity < 0)
    */
    public IdentityHashMap(int initialCapacity)
        throws IllegalArgumentException {
        if (initialCapacity < 0)
            throw new IllegalArgumentException();
        else
            init(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
    * construct a new HashMap from the given Map
    *
    * every element in Map t will be put into this new HashMap
    *
    * @param     t        a Map whose key / value pairs will be put into
    *                     the new HashMap.  <b>NOTE: key / value pairs
    *                     are not cloned in this constructor</b>
    */
    public IdentityHashMap(Map t) {
        int mapSize = t.size() * 2;
        init(
            ((mapSize > DEFAULT_CAPACITY) ? mapSize : DEFAULT_CAPACITY),
            DEFAULT_LOAD_FACTOR);
        putAll(t);
    }

    // PUBLIC METHODS ---------------------------------------------------------

    /** returns the number of kay-value mappings currently in this Map */
    public int size() {
        return size;
    }

    /** returns true if there are no key-value mappings currently in this Map */
    public boolean isEmpty() {
        return size == 0;
    }

    /** empties this HashMap of all elements */
    public void clear() {
        size = 0;
        modCount++;
        buckets = new Bucket[capacity];
    }

    /**
    * returns a shallow clone of this HashMap (i.e. the Map itself is cloned, but
    * its contents are not)
    */
    public Object clone() {
        Map.Entry entry;
        Iterator it = entrySet().iterator();
        IdentityHashMap clone = new IdentityHashMap(capacity, loadFactor);
        while (it.hasNext()) {
            entry = (Map.Entry) it.next();
            clone.internalPut(entry.getKey(), entry.getValue());
        }
        return clone;
    }

    /** returns a "set view" of this HashMap's keys */
    public Set keySet() {
        return new HashMapSet(KEYS_);
    }

    /** returns a "set view" of this HashMap's entries */
    public Set entrySet() {
        return new HashMapSet(ENTRIES_);
    }

    /** returns a "collection view" (or "bag view") of this HashMap's values */
    public Collection values() {
        return new HashMapCollection();
    }

    /**
    * returns true if the supplied object equals (<pre>equals()</pre>) a key
    * in this HashMap
    *
    * @param       key        the key to search for in this HashMap
    */
    public boolean containsKey(Object key) {
        return (internalGet(key) != null);
    }

    /**
    * returns true if this HashMap contains a value <pre>o</pre>, such that
    * <pre>o.equals(value)</pre>.
    *
    * @param      value       the value to search for in this Hashtable
    */
    public boolean containsValue(Object value) {
        int i;
        Bucket list;

        for (i = 0; i < capacity; i++) {
            list = buckets[i];
            if (list != null && list.containsValue(value))
                return true;
        }
        return false;
    }

    /*
     * return the value in this Hashtable associated with the supplied
     * key, or <pre>null</pre> if the key maps to nothing
     *
     * @param key the key for which to fetch an associated value */
    public Object get(Object key) {
        Map.Entry oResult = internalGet(key);
        return (oResult == null) ? null : oResult.getValue();
    }

    /**
    * puts the supplied value into the Map, mapped by the supplied key
    *
    * @param       key        the HashMap key used to locate the value
    * @param       value      the value to be stored in the HashMap
    */
    public Object put(Object key, Object value) {

        return internalPut(key, value);
    }

    /**
     * removes from the HashMap and returns the value which is mapped
     * by the supplied key; if the key maps to nothing, then the
     * HashMap remains unchanged, and <pre>null</pre> is returned
     *
     * @param key the key used to locate the value to remove from the HashMap */
    public Object remove(Object key) {
        Bucket list;
        int index;
        Object result = null;
        if (size > 0) {
            index = hash(key);
            list = buckets[index];
            if (list != null) {
                result = list.removeByKey(key);
                if (result != Bucket.NO_ENTRY) {
                    size--;
                    modCount++;
                    if (list.first == null)
                        buckets[index] = null;
                }
            }
        }
        return result == Bucket.NO_ENTRY ? null : result;
    }

    // PRIVATE METHODS -----------------------------------------------------------

    /**
     * puts the given key-value pair into this HashMap; a private method
     * is used because it is called by the rehash() method as well as
     * the put() method, and if a subclass overrides put(), then rehash
     * would do funky things if it called put()
     *
     * @param       key        the HashMap key used to locate the value
     * @param value the value to be stored in the HashMap */
    private Object internalPut(Object key, Object value) {
        HashMapEntry entry;
        Bucket list;
        int hashIndex;
        Object oResult;

        entry = new HashMapEntry(key, value);
        hashIndex = hash(key);
        list = buckets[hashIndex];
        if (list == null) {
            list = new IdentityBucket();
            buckets[hashIndex] = list;
        }
        oResult = list.add(entry);
        if (oResult == Bucket.NO_ENTRY) {
            modCount++;
            if (size++ == threshold)
                rehash();
            return null;
        } else {
            // SEH: if key already exists, we don't rehash & we don't
            // update the modCount because it is not a "structural"
            // modification
            return oResult;
        }
    }

    /**
    * a private method, called by all of the constructors to initialize
    * a new HashMap
    *
    * @param   initialCapacity     the initial capacity of this HashMap (>=0)
    * @param   initialLoadFactor   the load factor of this HashMap
    *                              (a misnomer, really, since the load factor of
    *                              a HashMap does not change) */
    private void init(int initialCapacity, float initialLoadFactor) {
        size = 0;
        modCount = 0;
        capacity = initialCapacity;
        loadFactor = initialLoadFactor;
        threshold = (int) (capacity * loadFactor);
        buckets = new Bucket[capacity];
    }

    /** private -- simply hashes a (possibly null) Object to its array index */
    private int hash(Object key) {
        return Math.abs(
            ObjectModel.getObjectModel().identityHash(key) % capacity);
    }

    /**
    * increases the size of the HashMap and rehashes all keys to new
    * array indices; this is called when the addition of a new value
    * would cause size() > threshold */
    private void rehash() {
        int i;
        Bucket[] data = buckets;
        Bucket.Node node;

        modCount++;
        capacity = (capacity * 2) + 1;
        size = 0;
        threshold = (int) (capacity * loadFactor);
        buckets = new Bucket[capacity];
        for (i = 0; i < data.length; i++) {
            if (data[i] != null) {
                node = data[i].first;
                while (node != null) {
                    internalPut(node.getKey(), node.getValue());
                    node = node.next;
                }
            }
        }
    }

    /**
    * a private method which does the "dirty work" (or some of it
    * anyway) of fetching a value with a key
    *
    *  @param key the key for which to fetch an associated value */
    private Map.Entry internalGet(Object key) {
        Bucket list;
        if (size == 0)
            return null;
        else {
            list = buckets[hash(key)];
            return (list == null) ? null : list.getEntryByKey(key);
        }
    }

    /**
    * a private method used by inner class HashMapSet to implement its
    * own <pre>contains(Map.Entry)</pre> method; returns true if the
    * supplied key / value pair is found in this HashMap (again, using
    * <pre>equals()</pre>, rather than <pre>==</pre>)
    *
    * @param      entry      a Map.Entry to match against key / value pairs in
    *                        this HashMap */
    boolean containsEntry(Map.Entry entry) {
        Map.Entry oInternalEntry;
        if (entry == null)
            return false;
        else {
            oInternalEntry = internalGet(entry.getKey());
            return (oInternalEntry != null && oInternalEntry.equals(entry));
        }
    }

    // INNER CLASSES -------------------------------------------------------------
    // ---------------------------------------------------------------------------

    /**
    * an inner class providing a Set view of a HashMap; this implementation is
    * parameterized to view either a Set of keys or a Set of Map.Entry objects
    *
    * Note:  a lot of these methods are implemented by AbstractSet, and would work
    * just fine without any meddling, but far greater efficiency can be gained by
    * overriding a number of them.  And so I did.
    *
    * @author      Jon Zeppieri
    * @version     $Revision: 1.8 $
    * @modified    $Id: IdentityHashMap.java,v 1.8 2004/01/19 14:47:23 jv Exp $
    */
    private class HashMapSet extends AbstractSet implements Set {
        /** the type of this Set view:  KEYS or ENTRIES */
        private int setType;

        /** construct a new HashtableSet with the supplied view type */
        HashMapSet(int type) {
            setType = type;
        }

        /**
        * adding an element is unsupported; this method simply throws an exception
        * @throws       UnsupportedOperationException
        */
        public boolean add(Object o) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
        * adding an element is unsupported; this method simply throws an exception
        * @throws       UnsupportedOperationException
        */
        public boolean addAll(Collection c)
            throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
        * clears the backing HashMap; this is a prime example of an
        * overridden implementation which is far more efficient than its
        * superclass implementation (which uses an iterator and is O(n)
        * -- this is an O(1) call) */
        public void clear() {
            IdentityHashMap.this.clear();
        }

        /**
        * returns true if the supplied object is contained by this Set
        * @param     o       an Object being testing to see if it is in this Set
        */
        public boolean contains(Object o) {
            if (setType == KEYS_)
                return IdentityHashMap.this.containsKey(o);
            else
                return (o instanceof Map.Entry)
                    ? IdentityHashMap.this.containsEntry((Map.Entry) o)
                    : false;
        }

        /**
        * returns true if the backing HashMap is empty (which is the only
        * case either a KEYS Set or an ENTRIES Set would be empty) */
        public boolean isEmpty() {
            return IdentityHashMap.this.isEmpty();
        }

        /**
        * removes the supplied Object from the Set
        *
        * @param      o       the Object to be removed
        */
        public boolean remove(Object o) {
            if (setType == KEYS_)
                return (IdentityHashMap.this.remove(o) != null);
            else
                return (o instanceof Map.Entry)
                    ? (IdentityHashMap.this.remove(((Map.Entry) o).getKey())
                        != null)
                    : false;
        }

        /** returns the size of this Set (always equal to the size of the
        backing Hashtable) */
        public int size() {
            return IdentityHashMap.this.size();
        }

        /** returns an Iterator over the elements of this Set */
        public Iterator iterator() {
            return new HashMapIterator(setType);
        }
    }

    /**
    * Like the above Set view, except this one if for values, which are not
    * guaranteed to be unique in a Map; this prvides a Bag of values
    * in the HashMap
    *
    * @author       Jon Zeppieri
    * @version      $Revision: 1.8 $
    * @modified     $Id: IdentityHashMap.java,v 1.8 2004/01/19 14:47:23 jv Exp $
    */
    private class HashMapCollection
        extends AbstractCollection
        implements Collection {
        /** a trivial contructor for HashMapCollection */
        HashMapCollection() {
        }

        /**
        * adding elements is not supported by this Collection;
        * this method merely throws an exception
        *
        * @throws     UnsupportedOperationException
        */
        public boolean add(Object o) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
        * adding elements is not supported by this Collection;
        * this method merely throws an exception
        *
        * @throws     UnsupportedOperationException
        */
        public boolean addAll(Collection c)
            throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /** removes all elements from this Collection (and from the backing HashMap) */
        public void clear() {
            IdentityHashMap.this.clear();
        }

        /**
        * returns true if this Collection contains at least one Object
        * which equals() the supplied Object
        *
        * @param o the Object to compare against those in the Set */
        public boolean contains(Object o) {
            return IdentityHashMap.this.containsValue(o);
        }

        /** returns true IFF the Collection has no elements */
        public boolean isEmpty() {
            return IdentityHashMap.this.isEmpty();
        }

        /** returns the size of this Collection */
        public int size() {
            return IdentityHashMap.this.size();
        }

        /** returns an Iterator over the elements in this Collection */
        public Iterator iterator() {
            return new HashMapIterator(VALUES_);
        }
    }

    /**
    * a class which implements the Iterator interface and is used for
    * iterating over HashMaps;
    * this implementation is parameterized to give a sequential view of
    * keys, values, or entries; it also allows the removal of elements,
    * as per the Javasoft spec.
    *
    * @author       Jon Zeppieri
    * @version      $Revision: 1.8 $
    * @modified     $Id: IdentityHashMap.java,v 1.8 2004/01/19 14:47:23 jv Exp $
    */
    class HashMapIterator implements Iterator {
        /** the type of this Iterator: KEYS, VALUES, or ENTRIES */
        private int myType;
        /**
         * the number of modifications to the backing Hashtable for which
         * this Iterator can account (idea ripped off from Stuart Ballard)
         */
        private int knownMods;
        /** the location of our sequential "cursor" */
        private int position;
        /** the current index of the BucketList array */
        private int bucketIndex;
        /** a reference, originally null, to the specific Bucket our
        "cursor" is pointing to */
        private Bucket.Node currentNode;
        /** a reference to the current key -- used fro removing elements
        via the Iterator */
        private Object currentKey;

        /** construct a new HashtableIterator with the supllied type:
        KEYS, VALUES, or ENTRIES */
        HashMapIterator(int type) {
            myType = type;
            knownMods = IdentityHashMap.this.modCount;
            position = 0;
            bucketIndex = -1;
            currentNode = null;
            currentKey = null;
        }

        /**
        * Stuart Ballard's code: if the backing HashMap has been altered
        * through anything but <i>this</i> Iterator's <pre>remove()</pre>
        * method, we will give up right here, rather than risking
        * undefined behavior
        *
        * @throws ConcurrentModificationException */
        private void checkMod() {
            if (knownMods != IdentityHashMap.this.modCount)
                throw new ConcurrentModificationException();
        }

        /** returns true if the Iterator has more elements */
        public boolean hasNext() {
//            checkMod();
            return position < IdentityHashMap.this.size();
        }

        /** returns the next element in the Iterator's sequential view */
        public Object next() {
            Bucket list = null;
            Object result;
            checkMod();
            try {
                while (currentNode == null) {
                    while (list == null)
                        list = IdentityHashMap.this.buckets[++bucketIndex];
                    currentNode = list.first;
                }
                currentKey = currentNode.getKey();
                result =
                    (myType == KEYS_)
                        ? currentKey
                        : ((myType == VALUES_)
                            ? currentNode.getValue()
                            : currentNode);
                currentNode = currentNode.next;
            } catch (Exception e) // FIXME this is irresponsible
                {
                throw new NoSuchElementException();
            }
            position++;
            return result;
        }

        /**
        * removes from the backing HashMap the last element which was
        * fetched with the <pre>next()</pre> method */
        public void remove() {
            checkMod();
            if (currentKey == null) {
                throw new IllegalStateException();
            } else {
                IdentityHashMap.this.remove(currentKey);
                knownMods++;
                position--;
                currentKey = null;
            }
        }
    }

    /**
     * a HashMap version of Map.Entry -- one thing in this implementation is
     * IdentityHashMap-specific: ObjectModel.identityHash() is used on
     * the key.
     *
     * Simply, a key / value pair
     *
     * @author      Jon Zeppieri
     * @version     $Revision: 1.8 $
     * @modified    $Id: IdentityHashMap.java,v 1.8 2004/01/19 14:47:23 jv Exp $
     */
    private static class HashMapEntry
        extends Bucket.Node
        implements Map.Entry {

        public HashMapEntry(Object key, Object value) {
            super(key, value);
        }

        public String toString() {
            return (
                key == null
                    ? "null"
                    : key.getClass().getName()
                        + "@"
                        + ObjectModel.getObjectModel().identityHash(key))
                + "="
                + value;
        }
        public boolean equals(Object o) {
            Map.Entry tester;
            Object oTestingKey, oTestingValue;
            Object oKey, oValue;
            if (o instanceof Map.Entry) {
                tester = (Map.Entry) o;
                oKey = getKey();
                oValue = getValue();
                oTestingKey = tester.getKey();
                oTestingValue = tester.getValue();
                return (
                    ((oTestingKey == null)
                        ? (oKey == null)
                        : oTestingKey == oKey)
                && // IDENTITY comparison!
                ((oTestingValue == null)
                    ? (oValue == null)
                    : oTestingValue.equals(oValue)));
            }
            return false;
        }
        public int hashCode() {
            Object oKey = getKey();
            Object oValue = getValue();
            return (ObjectModel.getObjectModel().identityHash(oKey))
                ^ ((oValue == null) ? 0 : oValue.hashCode());
        }
    } // End of HashMapEntry
    private static class IdentityBucket extends Bucket {
        protected boolean equals(Object key1, Object key2) {
            return key1 == key2;
        }
    }
} // End of HashMap
