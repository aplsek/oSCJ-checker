/* ThreadLocal -- a variable with a unique value per thread
   Copyright (C) 2000, 2002, 2003 Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
02111-1307 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */

/* OVM NOTES: This is a modified version of Classpath's ThreadLocal class
   that uses a locally defined identityHashMap based on the thread's hashcode
   and uniqueID field. This avoids the need to store Thread references and
   works for scoped memory in more situations than the default Classpath
   version, or our per-thread-map version.

   David Holmes August 12, 2004
*/

package java.lang;

import java.util.Arrays;

/**
 * ThreadLocal objects have a different state associated with every
 * Thread that accesses them. Every access to the ThreadLocal object
 * (through the <code>get()</code> and <code>set()</code> methods)
 * only affects the state of the object as seen by the currently
 * executing Thread.
 *
 * <p>The first time a ThreadLocal object is accessed on a particular
 * Thread, the state for that Thread's copy of the local variable is set by
 * executing the method <code>initialValue()</code>.
 *
 * <p>An example how you can use this:
 * <pre>
 * class Connection
 * {
 *   private static ThreadLocal owner = new ThreadLocal()
 *     {
 *       public Object initialValue()
 *       {
 *         return("nobody");
 *       }
 *     };
 * ...
 * }
 * </pre></br>
 *
 * Now all instances of connection can see who the owner of the currently
 * executing Thread is by calling <code>owner.get()</code>. By default any
 * Thread would be associated with 'nobody'. But the Connection object could
 * offer a method that changes the owner associated with the Thread on
 * which the method was called by calling <code>owner.put("somebody")</code>.
 * (Such an owner changing method should then be guarded by security checks.)
 *
 * <p>When a Thread is garbage collected all references to values of
 * the ThreadLocal objects associated with that Thread are removed.
 *
 * @author Mark Wielaard <mark@klomp.org>
 * @author Eric Blake <ebb9@email.byu.edu>
 * @since 1.2
 * @status updated to 1.4
 */
public class ThreadLocal {
    /**
     * Placeholder to distinguish between uninitialized and null set by the
     * user. Do not expose this to the public. Package visible for use by
     * InheritableThreadLocal
     */
    static final Object NULL = new Object();

    /**
     * The stored value. Package visible for use by InheritableThreadLocal. 
     */
    Object value;
    
    // also used by InheritableThreadLocal
    final ThreadMap valueMap = new ThreadMap();
	
    /**
     * Creates a ThreadLocal object without associating any value to it yet.
     */
    public ThreadLocal() {
    }

    /**
     * Called once per thread on the first invocation of get(), if set() was
     * not already called. The default implementation returns 
     * <code>null</code>.
     * Often, this method is overridden to create the appropriate initial 
     * object for the current thread's view of the ThreadLocal.
     *
     * @return the initial value of the variable in this thread
     */
    protected Object initialValue() {
        return null;
    }

    /**
     * Gets the value associated with the ThreadLocal object for the currently
     * executing Thread. If this is the first time the current thread has 
     * called get(), and it has not already called set(), the value is 
     obtained by <code>initialValue()</code>.
     *
     * @return the value of the variable in this thread
     */
    public Object get() {
        Thread currentThread = Thread.currentThread();
        Object value = valueMap.get(currentThread.vmThread);
        if (value == null) {
            value = initialValue();
            valueMap.put(currentThread.vmThread, value == null ? NULL : value);
        }
        return value == NULL ? null : value;
    }

    /**
     * Sets the value associated with the ThreadLocal object for the currently
     * executing Thread. This overrides any existing value associated with the
     * current Thread and prevents <code>initialValue()</code> from being
     * called if this is the first access to this ThreadLocal in this Thread.
     *
     * @param value the value to set this thread's view of the variable to
     */
    public void set(Object value) {
        valueMap.put(Thread.currentThread().vmThread,
		     value == null ? NULL : value);
    }


    /** Our special hashmap from Threads to values. We hash based on the
        Identity hash of the Thread and store the Thread's uniqueID to
        resolve collisions. This is a stripped down version of Classpath's
        IdentityHashMap with the table[] split into keys and values to suit
        our needs and with all the remove() related stuff stripped out.
        The resulting hashtable doesn't look much like the original at all.
        :-)

        Note: this map uses synchronized methods for concurrency control.
    */
    static class ThreadMap {
        /** The default capacity. */
        private static final int DEFAULT_CAPACITY = 21;
        
        /**
         * Value used to mark empty slots. 
         */
        static final int emptyslot = -1;
        
        /**
         * The number of mappings in the table. 
         */
        int size;

        /** Key array */
        int[] keys;

        /** value array */
        Object[] values;
        
        /**
         * The threshold for rehashing, which is 75% of (table.length / 2).
         */
        int threshold;

        ThreadMap() {
            keys = new int[DEFAULT_CAPACITY];
            for (int i = 0; i < keys.length; i++)
                keys[i] = -1;

            values = new Object[DEFAULT_CAPACITY];
            threshold = (DEFAULT_CAPACITY >> 2) * 3;
        }


        
        /**
         * Return the value associated with the supplied thread, or
         * <code>null</code> if the thread maps to nothing.
         *
         * @param key the thread for which to fetch an associated value
         * @return what the key maps to, if present
         */
        synchronized Object get(VMThread key) {
            int h = hash(key);
            return keys[h] == key.uniqueID ? values[h] : null;
        }


        /**
         * Puts the supplied value into the map, mapped by the supplied key.
         *
         * @param key the key used to locate the value
         * @param value the value to be stored in the HashMap
         * @return the prior mapping of the key, or null if there was none
         * @see #get(Object)
         */
        synchronized Object put(VMThread key, Object value) {
            // Rehash if the load factor is too high.
            if (size > threshold) {
                int[] oldKeys = keys;
                keys = new int[oldKeys.length*2 +1];
                Object[] oldValues = values;
                values = new Object[oldValues.length*2 +1];

                System.arraycopy(oldKeys, 0, keys, 0, oldKeys.length);
                System.arraycopy(oldValues, 0, values, 0, oldValues.length);
                size = 0;
                threshold = (keys.length >>> 3) * 3;
            }
        
            int h = hash(key);
            if (keys[h] == key.uniqueID) {
                Object r = values[h];
                values[h] = value;
                return r;
            }
        
            // At this point, we add a new mapping.
            size++;
            keys[h] = key.uniqueID;
            values[h] = value;
            return null;
        }

        /**
         * Helper method which computes the hash code, then traverses the table
         * until it finds the key, or the spot where the key would go.
         *
         * @param key the key to check
         * @return the index where the key belongs
         */
        int hash(VMThread key) {
            int h = System.identityHashCode(key) % keys.length;
            if (h < 0)
                h = -h;
            int start = h;
            do {
                if (keys[h] == key.uniqueID ||
                    keys[h] == emptyslot )
                    return h;
                h = (h + 1) % keys.length;
            }
            while (h != start);

            return h;
        }

    }
    
}
