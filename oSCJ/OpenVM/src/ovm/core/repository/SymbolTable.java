/* -*- c-basic-offset: 2 -*-
   HashMap.java -- a class providing a basic hashtable data structure,
   mapping Object --> Object
   Copyright (C) 1998, 1999, 2000, 2001, 2002 Free Software Foundation, Inc.

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


package ovm.core.repository;

import ovm.core.repository.RepositorySymbol.Internable;


/**
 * A compact intern table for {@link Internable}
 * constant pool symbols.  We save space by avoiding
 * {@code HashMap.Entry} or similar wrappers around the objects this
 * set actually contains.  We don't need a wrapper object, because
 * {@code Internable} has been nice enough to supply us with the
 * {@link Internable#next} field.<p>
 *
 * All accesses to a symbol table should be protected by a lock.  This
 * is currently implemented in {@link Internable#intern}.<p>
 *
 * Adapted from an old version of Classpath's {@link ovm.util.HashMap HashMap}.
 *
 * @author Jon Zeppieri
 * @author Jochen Hoenicke
 * @author Bryce McKinlay
 * @author Eric Blake <ebb9@email.byu.edu>
 */
public class SymbolTable
{
  /**
   * Default number of buckets. This is the value the JDK 1.3 uses. Some
   * early documentation specified this value as 101. That is incorrect.
   * Package visible for use by HashSet.
   */
  static final int DEFAULT_CAPACITY = 11;

  /**
   * The default load factor; this is explicitly specified by the spec.
   * Package visible for use by HashSet.
   */
  static final float DEFAULT_LOAD_FACTOR = 0.75f;

  /**
   * The rounded product of the capacity and the load factor; when the number
   * of elements exceeds the threshold, the HashMap calls
   * <code>rehash()</code>.
   * @serial the threshold for rehashing
   */
  private int threshold;

  /**
   * Load factor of this HashMap:  used in computing the threshold.
   * Package visible for use by HashSet.
   * @serial the load factor
   */
  final float loadFactor;

  /**
   * Array containing the actual key-value mappings.
   * Package visible for use by nested and subclasses.
   */
  transient Internable[] buckets;

  /**
   * The size of this HashMap:  denotes the number of key-value pairs.
   * Package visible for use by nested and subclasses.
   */
  transient int size;

  /**
   * Construct a new HashMap with the default capacity (11) and the default
   * load factor (0.75).
   */
  public SymbolTable()
  {
    this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
  }

  /**
   * Construct a new HashMap with a specific inital capacity and
   * default load factor of 0.75.
   *
   * @param initialCapacity the initial capacity of this HashMap (&gt;=0)
   * @throws IllegalArgumentException if (initialCapacity &lt; 0)
   */
  public SymbolTable(int initialCapacity)
  {
    this(initialCapacity, DEFAULT_LOAD_FACTOR);
  }

  /**
   * Construct a new HashMap with a specific inital capacity and load factor.
   *
   * @param initialCapacity the initial capacity (&gt;=0)
   * @param loadFactor the load factor (&gt; 0, not NaN)
   * @throws IllegalArgumentException if (initialCapacity &lt; 0) ||
   *                                     ! (loadFactor &gt; 0.0)
   */
  public SymbolTable(int initialCapacity, float loadFactor)
  {
    if (initialCapacity < 0)
      throw new IllegalArgumentException("Illegal Capacity: "
                                         + initialCapacity);
    if (! (loadFactor > 0)) // check for NaN too
      throw new IllegalArgumentException("Illegal Load: " + loadFactor);

    if (initialCapacity == 0)
      initialCapacity = 1;
    buckets = new Internable[initialCapacity];
    this.loadFactor = loadFactor;
    threshold = (int) (initialCapacity * loadFactor);
  }

  /**
   * Returns the number of kay-value mappings currently in this Map.
   *
   * @return the size
   */
  public int size()
  {
    return size;
  }

  /**
   * Returns true if there are no key-value mappings currently in this Map.
   *
   * @return <code>size() == 0</code>
   */
  public boolean isEmpty()
  {
    return size == 0;
  }

  /**
   * Return the interned symbol structurally equal to the supplied
   * symbol, or null if this symbol has not been interned.
   */
  public Internable get(Internable key)
  {
    Internable[] buckets_ = this.buckets;
    int idx = hash(key, buckets_);
    Internable e = buckets_[idx];
    while (e != null)
      {
        if (key.equals(e))
          return e;
        e = e.next;
      }
    return null;
  }

  /**
   * Insert a symbol into the table.  It is an error to insert two
   * symols that are structurally equal.
   * @param sym the symbol to insert
   */
  public void put(Internable sym)
  {
    assert (get(sym) == null);
    if (++size > threshold)
      buckets = rehash();

    int idx = hash(sym, buckets);
    sym.next = buckets[idx];
    buckets[idx] = sym;
  }

  /**
   * Helper method that returns an index in the buckets array for `key'
   * based on its hashCode().  Package visible for use by subclasses.
   *
   * @param key the key
   * @return the bucket number
   */
  final int hash(Internable key, Internable[] _buckets)
  {
    return Math.abs(key.hashCode() % _buckets.length);
  }

  /**
   * Increases the size of the HashMap and rehashes all keys to new
   * array indices; this is called when the addition of a new value
   * would cause size() &gt; threshold. Note that the existing Entry
   * objects are <b>NOT<b> reused in the new hash table.
   *
   * <p>This is not specified, but the new size is twice the current size
   * plus one; this number is not always prime, unfortunately.
   */
  private Internable[] rehash()
  {
    Internable[] oldBuckets = buckets;

    int newcapacity = (buckets.length * 2) + 1;
    threshold = (int) (newcapacity * loadFactor);
    Internable[] _buckets = new Internable[newcapacity];

    for (int i = oldBuckets.length - 1; i >= 0; i--)
      {
        Internable inOld = oldBuckets[i];
        while (inOld != null)
          {
	    Internable inNew = inOld;
	    inOld = inNew.next;
	    int idx = hash(inNew, _buckets);
	    inNew.next = _buckets[idx];
	    _buckets[idx] = inNew;
          }
      }
    return this.buckets = _buckets;
  }
}
