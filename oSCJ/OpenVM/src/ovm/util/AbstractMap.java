/* AbstractMap.java -- Abstract implementation of most of Map
   Copyright (C) 1998, 1999, 2000 Free Software Foundation, Inc.
This file is part of GNU Classpath.
It has been modified slightly to fit the OVM framework.
 */
// TO DO:
// comments
// test suite
package ovm.util;

import ovm.core.OVMBase;

public abstract class AbstractMap extends OVMBase
    implements Map
{
  // Package visible for use by subclasses.
  static final int KEYS = 0,
                   VALUES = 1,
                   ENTRIES = 2;

/**
   * The cache for {@link #keySet()}.
   */
  // Package visible for use by subclasses.
  Set keys;

  /**
   * The cache for {@link #values()}.
   */
  // Package visible for use by subclasses.
  Collection values;


  /**
   * Remove all entries from this Map. This default implementation calls
   * entrySet().clear().
   *
   * @throws UnsupportedOperationException
   * @specnote The JCL book claims that this implementation always throws
   *           UnsupportedOperationException, while the online docs claim it
   *           calls entrySet().clear(). We take the later to be correct.
   */
  public void clear()
  {
    entrySet().clear();
  }

  public boolean containsKey(Object key)
  {
    Object k;
    Set es = entrySet();
    Iterator entries = es.iterator();
    int size = size();
    for (int pos = 0; pos < size; pos++)
      {
        k = ((Map.Entry) entries.next()).getKey();
        if (key == null ? k == null : key.equals(k))
          return true;
      }
    return false;
  }

  public boolean containsValue(Object value)
  {
    Object v;
    Set es = entrySet();
    Iterator entries = es.iterator();
    int size = size();
    for (int pos = 0; pos < size; pos++)
      {
        v = ((Map.Entry) entries.next()).getValue();
        if (value == null ? v == null : value.equals(v))
          return true;
      }
    return false;
  }

  public abstract Set entrySet();

  public boolean equals(Object o)
  {
    if (o == this)
      return true;
    if (!(o instanceof Map))
      return false;

    Map m = (Map) o;
    Set s = m.entrySet();
    Iterator itr = entrySet().iterator();
    int size = size();

    if (m.size() != size)
      return false;

    for (int pos = 0; pos < size; pos++)
      {
        if (!s.contains(itr.next()))
          return false;
      }
    return true;
  }

  public Object get(Object key)
  {
    Set s = entrySet();
    Iterator entries = s.iterator();
    int size = size();

    for (int pos = 0; pos < size; pos++)
      {
        Map.Entry entry = (Map.Entry) entries.next();
        Object k = entry.getKey();
        if (key == null ? k == null : key.equals(k))
          return entry.getValue();
      }

    return null;
  }

    /**
     * This method does not terminate if the AbstractMap has a self
     * reference. This behavior is consistent with the JDK1.4
     * specification.  */
  public int hashCode() {
    int hashcode = 0;
    Iterator itr = entrySet().iterator();
    int size = size();
    for ( int pos = 0; pos < size; pos++) {
        Object nxt = itr.next();
        //        if (((Map.Entry)nxt).getKey()==this)continue;
        // FIXME half a solution for avoiding recursion
        hashcode += nxt.hashCode();
    }

    return hashcode;
  }

  public boolean isEmpty()
  {
    return size() == 0;
  }

  public Set keySet()
  {
    if (this.keys == null)
      {
        this.keys = new AbstractSet()
        {
          public int size()
          {
            return AbstractMap.this.size();
          }

          public boolean contains(Object key)
          {
            return AbstractMap.this.containsKey(key);
          }

          public Iterator iterator()
          {
            return new Iterator()
            {
              Iterator map_iterator = AbstractMap.this.entrySet().iterator();

              public boolean hasNext()
              {
                return map_iterator.hasNext();
              }

              public Object next()
              {
                return ((Map.Entry) map_iterator.next()).getKey();
              }

              public void remove()
              {
                map_iterator.remove();
              }
            };
          }
        };
      }

    return this.keys;
  }

  public Object put(Object key, Object value)
  {
    throw new UnsupportedOperationException();
  }

  public void putAll(Map m)
  {
    Map.Entry entry;
    Iterator entries = m.entrySet().iterator();
    int size = m.size();

    for (int pos = 0; pos < size; pos++)
      {
        entry = (Map.Entry) entries.next();
        put(entry.getKey(), entry.getValue());
      }
  }

  public Object remove(Object key)
  {
    Iterator entries = entrySet().iterator();
    int size = size();

    for (int pos = 0; pos < size; pos++)
      {
        Map.Entry entry = (Map.Entry) entries.next();
        Object k = entry.getKey();
        if (key == null ? k == null : key.equals(k))
          {
            Object value = entry.getValue();
            entries.remove();
            return value;
          }
      }

    return null;
  }

  public int size()
  {
    return entrySet().size();
  }

  public String toString()
  {
    Iterator entries = entrySet().iterator();
    int size = size();
    String r = "{";
    for (int pos = 0; pos < size; pos++)
      {
        r += entries.next();
        if (pos < size - 1)
          r += ", ";
      }
    r += "}";
    return r;
  }

  public Collection values()
  {
    if (this.values == null)
      {
        this.values = new AbstractCollection()
        {
          public int size()
          {
            return AbstractMap.this.size();
          }

          public Iterator iterator()
          {
            return new Iterator()
            {
              Iterator map_iterator = AbstractMap.this.entrySet().iterator();

              public boolean hasNext()
              {
                return map_iterator.hasNext();
              }

              public Object next()
              {
                return ((Map.Entry) map_iterator.next()).getValue();
              }

              public void remove()
              {
                map_iterator.remove();
              }
            };
          }
        };
      }

    return this.values;
  }


  static class BasicMapEntry implements Map.Entry
  {
    /**
     * The key. Package visible for direct manipulation.
     */
    Object key;

    /**
     * The value. Package visible for direct manipulation.
     */
    Object value;

    /**
     * Basic constructor initializes the fields.
     * @param newKey the key
     * @param newValue the value
     */
    BasicMapEntry(Object newKey, Object newValue)
    {
      key = newKey;
      value = newValue;
    }

    /**
     * Compares the specified object with this entry. Returns true only if
     * the object is a mapping of identical key and value. In other words,
     * this must be:<br>
     * <pre>(o instanceof Map.Entry)
     *       && (getKey() == null ? ((HashMap) o).getKey() == null
     *           : getKey().equals(((HashMap) o).getKey()))
     *       && (getValue() == null ? ((HashMap) o).getValue() == null
     *           : getValue().equals(((HashMap) o).getValue()))</pre>
     *
     * @param o the object to compare
     * @return <code>true</code> if it is equal
     */
    public final boolean equals(Object o)
    {
      if (! (o instanceof Map.Entry))
        return false;
      // Optimize for our own entries.
      if (o instanceof BasicMapEntry)
        {
          BasicMapEntry e = (BasicMapEntry) o;
          return (AbstractMap.equals(key, e.key)
                  && AbstractMap.equals(value, e.value));
        }
      Map.Entry e = (Map.Entry) o;
      return (AbstractMap.equals(key, e.getKey())
              && AbstractMap.equals(value, e.getValue()));
    }

   /**
     * Get the key corresponding to this entry.
     *
     * @return the key
     */
    public final Object getKey()
    {
      return key;
    }

    /**
     * Get the value corresponding to this entry. If you already called
     * Iterator.remove(), the behavior undefined, but in this case it works.
     *
     * @return the value
     */
    public final Object getValue()
    {
      return value;
    }


    /**
     * Returns the hash code of the entry.  This is defined as the exclusive-or
     * of the hashcodes of the key and value (using 0 for null). In other
     * words, this must be:<br>
     * <pre>(getKey() == null ? 0 : getKey().hashCode())
     *       ^ (getValue() == null ? 0 : getValue().hashCode())</pre>
     *
     * @return the hash code
     */
    public final int hashCode()
    {
      return (AbstractMap.hashCode(key) ^ AbstractMap.hashCode(value));
    }

    /**
     * Replaces the value with the specified object. This writes through
     * to the map, unless you have already called Iterator.remove(). It
     * may be overridden to restrict a null value.
     *
     * @param newVal the new value to store
     * @return the old value
     * @throws NullPointerException if the map forbids null values
     */
    public Object setValue(Object newVal)
    {
      Object r = value;
      value = newVal;
      return r;
    }

    /**
     * This provides a string representation of the entry. It is of the form
     * "key=value", where string concatenation is used on key and value.
     *
     * @return the string representation
     */
    public final String toString()
    {
      return key + "=" + value;
    }
  } // class BasicMapEntry



  /**
   * Hash an object according to Collection semantics.
   *
   * @param o the object to hash
   * @return o1 == null ? 0 : o1.hashCode()
   */
  // Package visible for use throughout java.util.
  // It may be inlined since it is final.
  static final int hashCode(Object o)
  {
    return o == null ? 0 : o.hashCode();
  }


    /**
         * Compare two objects according to Collection semantics.
            *
               * @param o1 the first object
                  * @param o2 the second object
                     * @return o1 == null ? o2 == null : o1.equals(o2)
                        */
    // Package visible for use throughout java.util.
    //   // It may be inlined since it is final.
         static final boolean equals(Object o1, Object o2)
           {
               return o1 == null ? o2 == null : o1.equals(o2);
                 }
}
