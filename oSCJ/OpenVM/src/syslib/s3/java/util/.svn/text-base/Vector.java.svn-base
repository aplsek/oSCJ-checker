package java.util;
public class Vector {
  /**
   * Compatible with JDK 1.0+.
   */
  private static final long serialVersionUID = -2767605614048989439L;

  /**
   * The internal array used to hold members of a Vector. The elements are
   * in positions 0 through elementCount - 1, and all remaining slots are null.
   * @serial the elements
   */
  protected Object[] elementData;

  /**
   * The number of elements currently in the vector, also returned by
   * {@link #size}.
   * @serial the size
   */
  protected int elementCount;

  /**
   * The amount the Vector's internal array should be increased in size when
   * a new element is added that exceeds the current size of the array,
   * or when {@link #ensureCapacity} is called. If &lt;= 0, the vector just
   * doubles in size.
   * @serial the amount to grow the vector by
   */
  protected int capacityIncrement;

    int modCount;

  /**
   * Constructs an empty vector with an initial size of 10, and
   * a capacity increment of 0
   */
  public Vector()
  {
    this(10, 0);
  }


  
  /**
   * Compare two objects according to Collection semantics.
   *
   * @param o1 the first object
   * @param o2 the second object
   * @return o1 == null ? o2 == null : o1.equals(o2)
   */
  // Package visible for use throughout java.util.
  // It may be inlined since it is final.
  static final boolean equals(Object o1, Object o2)
  {
    return o1 == null ? o2 == null : o1.equals(o2);
  }
 

  /**
   * Constructs a Vector with the initial capacity and capacity
   * increment specified.
   *
   * @param initialCapacity the initial size of the Vector's internal array
   * @param capacityIncrement the amount the internal array should be
   *        increased by when necessary, 0 to double the size
   * @throws IllegalArgumentException if initialCapacity &lt; 0
   */
  public Vector(int initialCapacity, int capacityIncrement)
  {
    if (initialCapacity < 0)
      throw new IllegalArgumentException();
    elementData = new Object[initialCapacity];
    this.capacityIncrement = capacityIncrement;
  }

  /**
   * Constructs a Vector with the initial capacity specified, and a capacity
   * increment of 0 (double in size).
   *
   * @param initialCapacity the initial size of the Vector's internal array
   * @throws IllegalArgumentException if initialCapacity &lt; 0
   */
  public Vector(int initialCapacity)
  {
    this(initialCapacity, 0);
  }

  /**
   * Copies the contents of a provided array into the Vector.  If the
   * array is too large to fit in the Vector, an IndexOutOfBoundsException
   * is thrown without modifying the array.  Old elements in the Vector are
   * overwritten by the new elements.
   *
   * @param a target array for the copy
   * @throws IndexOutOfBoundsException the array is not large enough
   * @throws NullPointerException the array is null
   * @see #toArray(Object[])
   */
  public synchronized void copyInto(Object[] a)
  {
    System.arraycopy(elementData, 0, a, 0, elementCount);
  }

  /**
   * Trims the Vector down to size.  If the internal data array is larger
   * than the number of Objects its holding, a new array is constructed
   * that precisely holds the elements. Otherwise this does nothing.
   */
  public synchronized void trimToSize()
  {
    // Don't bother checking for the case where size() == the capacity of the
    // vector since that is a much less likely case; it's more efficient to
    // not do the check and lose a bit of performance in that infrequent case

    Object[] newArray = new Object[elementCount];
    System.arraycopy(elementData, 0, newArray, 0, elementCount);
    elementData = newArray;
  }

  /**
   * Ensures that <code>minCapacity</code> elements can fit within this Vector.
   * If <code>elementData</code> is too small, it is expanded as follows:
   * If the <code>elementCount + capacityIncrement</code> is adequate, that
   * is the new size. If <code>capacityIncrement</code> is non-zero, the
   * candidate size is double the current. If that is not enough, the new
   * size is <code>minCapacity</code>.
   *
   * @param minCapacity the desired minimum capacity, negative values ignored
   */
  public synchronized void ensureCapacity(int minCapacity)
  {
    if (elementData.length >= minCapacity)
      return;

    int newCapacity;
    if (capacityIncrement <= 0)
      newCapacity = elementData.length * 2;
    else
      newCapacity = elementData.length + capacityIncrement;

    Object[] newArray = new Object[Math.max(newCapacity, minCapacity)];

    System.arraycopy(elementData, 0, newArray, 0, elementCount);
    elementData = newArray;
  }

  /**
   * Returns the size of the internal data array (not the amount of elements
   * contained in the Vector).
   *
   * @return capacity of the internal data array
   */
  public synchronized int capacity()
  {
    return elementData.length;
  }

  /**
   * Returns the number of elements stored in this Vector.
   *
   * @return the number of elements in this Vector
   */
  public synchronized int size()
  {
    return elementCount;
  }

  /**
   * Returns true if this Vector is empty, false otherwise
   *
   * @return true if the Vector is empty, false otherwise
   */
  public synchronized boolean isEmpty()
  {
    return elementCount == 0;
  }

  /**
   * Returns an Enumeration of the elements of this Vector. The enumeration
   * visits the elements in increasing index order, but is NOT thread-safe.
   *
   * @return an Enumeration
   * @see #iterator()
   */
  // No need to synchronize as the Enumeration is not thread-safe!
  public Enumeration elements()
  {
    return new Enumeration()
    {
      private int i = 0;

      public boolean hasMoreElements()
      {
        return i < elementCount;
      }

      public Object nextElement()
      {
        if (i >= elementCount)
          throw new NoSuchElementException();
        return elementData[i++];
      }
    };
  }

  /**
   * Returns true when <code>elem</code> is contained in this Vector.
   *
   * @param elem the element to check
   * @return true if the object is contained in this Vector, false otherwise
   */
  public boolean contains(Object elem)
  {
    return indexOf(elem, 0) >= 0;
  }

  /**
   * Returns the first occurrence of <code>elem</code> in the Vector, or -1 if
   * <code>elem</code> is not found.
   *
   * @param elem the object to search for
   * @return the index of the first occurrence, or -1 if not found
   */
  public int indexOf(Object elem)
  {
    return indexOf(elem, 0);
  }

  /**
   * Searches the vector starting at <code>index</code> for object
   * <code>elem</code> and returns the index of the first occurrence of this
   * Object.  If the object is not found, or index is larger than the size
   * of the vector, -1 is returned.
   *
   * @param e the Object to search for
   * @param index start searching at this index
   * @return the index of the next occurrence, or -1 if it is not found
   * @throws IndexOutOfBoundsException if index &lt; 0
   */
  public synchronized int indexOf(Object e, int index)
  {
    for (int i = index; i < elementCount; i++)
      if (equals(e, elementData[i]))
        return i;
    return -1;
  }

  /**
   * Returns the last index of <code>elem</code> within this Vector, or -1
   * if the object is not within the Vector.
   *
   * @param elem the object to search for
   * @return the last index of the object, or -1 if not found
   */
  public int lastIndexOf(Object elem)
  {
    return lastIndexOf(elem, elementCount - 1);
  }

  /**
   * Returns the index of the first occurrence of <code>elem</code>, when
   * searching backwards from <code>index</code>.  If the object does not
   * occur in this Vector, or index is less than 0, -1 is returned.
   *
   * @param e the object to search for
   * @param index the index to start searching in reverse from
   * @return the index of the Object if found, -1 otherwise
   * @throws IndexOutOfBoundsException if index &gt;= size()
   */
  public synchronized int lastIndexOf(Object e, int index)
  {
    checkBoundExclusive(index);
    for (int i = index; i >= 0; i--)
      if (equals(e, elementData[i]))
        return i;
    return -1;
  }

  /**
   * Returns the Object stored at <code>index</code>.
   *
   * @param index the index of the Object to retrieve
   * @return the object at <code>index</code>
   * @throws ArrayIndexOutOfBoundsException index &lt; 0 || index &gt;= size()
   * @see #get(int)
   */
  public synchronized Object elementAt(int index)
  {
    checkBoundExclusive(index);
    return elementData[index];
  }

  /**
   * Returns the first element (index 0) in the Vector.
   *
   * @return the first Object in the Vector
   * @throws NoSuchElementException the Vector is empty
   */
  public synchronized Object firstElement()
  {
    if (elementCount == 0)
      throw new NoSuchElementException();

    return elementData[0];
  }

  /**
   * Returns the last element in the Vector.
   *
   * @return the last Object in the Vector
   * @throws NoSuchElementException the Vector is empty
   */
  public synchronized Object lastElement()
  {
    if (elementCount == 0)
      throw new NoSuchElementException();

    return elementData[elementCount - 1];
  }

  /**
   * Changes the element at <code>index</code> to be <code>obj</code>
   *
   * @param obj the object to store
   * @param index the position in the Vector to store the object
   * @throws ArrayIndexOutOfBoundsException the index is out of range
   * @see #set(int, Object)
   */
  public void setElementAt(Object obj, int index)
  {
    set(index, obj);
  }

  /**
   * Removes the element at <code>index</code>, and shifts all elements at
   * positions greater than index to their index - 1.
   *
   * @param index the index of the element to remove
   * @throws ArrayIndexOutOfBoundsException index &lt; 0 || index &gt;= size();
   * @see #remove(int)
   */
  public void removeElementAt(int index)
  {
    remove(index);
  }

  /**
   * Inserts a new element into the Vector at <code>index</code>.  Any elements
   * at or greater than index are shifted up one position.
   *
   * @param obj the object to insert
   * @param index the index at which the object is inserted
   * @throws ArrayIndexOutOfBoundsException index &lt; 0 || index &gt; size()
   * @see #add(int, Object)
   */
  public synchronized void insertElementAt(Object obj, int index)
  {
    checkBoundInclusive(index);
    if (elementCount == elementData.length)
      ensureCapacity(elementCount + 1);
    modCount++;
    System.arraycopy(elementData, index, elementData, index + 1,
                     elementCount - index);
    elementCount++;
    elementData[index] = obj;
  }

  /**
   * Adds an element to the Vector at the end of the Vector.  The vector
   * is increased by ensureCapacity(size() + 1) if needed.
   *
   * @param obj the object to add to the Vector
   */
  public synchronized void addElement(Object obj)
  {
    if (elementCount == elementData.length)
      ensureCapacity(elementCount + 1);
    modCount++;
    elementData[elementCount++] = obj;
  }

  /**
   * Removes the first (the lowestindex) occurance of the given object from
   * the Vector. If such a remove was performed (the object was found), true
   * is returned. If there was no such object, false is returned.
   *
   * @param obj the object to remove from the Vector
   * @return true if the Object was in the Vector, false otherwise
   * @see #remove(Object)
   */
  public synchronized boolean removeElement(Object obj)
  {
    int idx = indexOf(obj, 0);
    if (idx >= 0)
      {
        remove(idx);
        return true;
      }
    return false;
  }

  /**
   * Creates a new Vector with the same contents as this one. The clone is
   * shallow; elements are not cloned.
   *
   * @return the clone of this vector
   */
  public synchronized Object clone()
  {
    try
      {
        Vector clone = (Vector) super.clone();
        clone.elementData = (Object[]) elementData.clone();
        return clone;
      }
    catch (CloneNotSupportedException ex)
      {
        // Impossible to get here.
        throw new InternalError(ex.toString());
      }
  }

  /**
   * Returns an Object array with the contents of this Vector, in the order
   * they are stored within this Vector.  Note that the Object array returned
   * is not the internal data array, and that it holds only the elements
   * within the Vector.  This is similar to creating a new Object[] with the
   * size of this Vector, then calling Vector.copyInto(yourArray).
   *
   * @return an Object[] containing the contents of this Vector in order
   * @since 1.2
   */
  public synchronized Object[] toArray()
  {
    Object[] newArray = new Object[elementCount];
    copyInto(newArray);
    return newArray;
  }

  /**
   * Returns the element at position <code>index</code>.
   *
   * @param index the position from which an element will be retrieved
   * @return the element at that position
   * @throws ArrayIndexOutOfBoundsException index &lt; 0 || index &gt;= size()
   * @since 1.2
   */
  public Object get(int index)
  {
    return elementAt(index);
  }

  /**
   * Puts <code>element</code> into the Vector at position <code>index</code>
   * and returns the Object that previously occupied that position.
   *
   * @param index the index within the Vector to place the Object
   * @param element the Object to store in the Vector
   * @return the previous object at the specified index
   * @throws ArrayIndexOutOfBoundsException index &lt; 0 || index &gt;= size()
   * @since 1.2
   */
  public synchronized Object set(int index, Object element)
  {
    checkBoundExclusive(index);
    Object temp = elementData[index];
    elementData[index] = element;
    return temp;
  }

  /**
   * Adds an object to the Vector.
   *
   * @param o the element to add to the Vector
   * @return true, as specified by List
   * @since 1.2
   */
  public boolean add(Object o)
  {
    addElement(o);
    return true;
  }

  /**
   * Removes the given Object from the Vector.  If it exists, true
   * is returned, if not, false is returned.
   *
   * @param o the object to remove from the Vector
   * @return true if the Object existed in the Vector, false otherwise
   * @since 1.2
   */
  public boolean remove(Object o)
  {
    return removeElement(o);
  }

  /**
   * Adds an object at the specified index.  Elements at or above
   * index are shifted up one position.
   *
   * @param index the index at which to add the element
   * @param element the element to add to the Vector
   * @throws ArrayIndexOutOfBoundsException index &lt; 0 || index &gt; size()
   * @since 1.2
   */
  public void add(int index, Object element)
  {
    insertElementAt(element, index);
  }

  /**
   * Removes the element at the specified index, and returns it.
   *
   * @param index the position from which to remove the element
   * @return the object removed
   * @throws ArrayIndexOutOfBoundsException index &lt; 0 || index &gt;= size()
   * @since 1.2
   */
  public synchronized Object remove(int index)
  {
    checkBoundExclusive(index);
    Object temp = elementData[index];
    modCount++;
    elementCount--;
    if (index < elementCount)
      System.arraycopy(elementData, index + 1, elementData, index,
                       elementCount - index);
    elementData[elementCount] = null;
    return temp;
  }

  public void clear()
  {
      while (size() > 0)
	  remove(elementAt(0));
  }

  /**
   * Compares this to the given object.
   *
   * @param o the object to compare to
   * @return true if the two are equal
   * @since 1.2
   */
  public synchronized boolean equals(Object o)
  {
    // Here just for the sychronization.
    return super.equals(o);
  }

  /**
   * Computes the hashcode of this object.
   *
   * @return the hashcode
   * @since 1.2
   */
  public synchronized int hashCode()
  {
    // Here just for the sychronization.
    return super.hashCode();
  }

  /**
   * Returns a string representation of this Vector in the form
   * "[element0, element1, ... elementN]".
   *
   * @return the String representation of this Vector
   */
  public synchronized String toString()
  {
    // Here just for the sychronization.
    return super.toString();
  }

  /**
   * Checks that the index is in the range of possible elements (inclusive).
   *
   * @param index the index to check
   * @throws ArrayIndexOutOfBoundsException if index &gt; size
   */
  private void checkBoundInclusive(int index)
  {
    // Implementation note: we do not check for negative ranges here, since
    // use of a negative index will cause an ArrayIndexOutOfBoundsException
    // with no effort on our part.
    if (index > elementCount)
      throw new ArrayIndexOutOfBoundsException(index + " > " + elementCount);
  }

  /**
   * Checks that the index is in the range of existing elements (exclusive).
   *
   * @param index the index to check
   * @throws ArrayIndexOutOfBoundsException if index &gt;= size
   */
  private void checkBoundExclusive(int index)
  {
    // Implementation note: we do not check for negative ranges here, since
    // use of a negative index will cause an ArrayIndexOutOfBoundsException
    // with no effort on our part.
    if (index >= elementCount)
      throw new ArrayIndexOutOfBoundsException(index + " >= " + elementCount);
  }
}
