package ovm.util;

class SubList extends AbstractList
{
  private AbstractList backingList;
  private int offset;
  private int size;

  public SubList(AbstractList backing, int fromIndex, int toIndex)
  {
    backingList = backing;
    modCount = backingList.modCount;
    offset = fromIndex;
    size = toIndex - fromIndex;
  }

  /**
   * This method checks the two modCount fields to ensure that there has
   * not been a concurrent modification. It throws an exception if there
   * has been, and otherwise returns normally.
   * Note that since this method is private, it will be inlined.
   *
   * @exception ConcurrentModificationException if there has been a
   *   concurrent modification.
   */
  private void checkMod()
  {
    if (modCount != backingList.modCount)
      throw new ConcurrentModificationException();
  }

  /**
   * This method checks that a value is between 0 and size (inclusive). If
   * it is not, an exception is thrown.
   * Note that since this method is private, it will be inlined.
   *
   * @exception IndexOutOfBoundsException if the value is out of range.
   */
  private void checkBoundsInclusive(int index)
  {
    if (index < 0 || index > size)
      throw new IndexOutOfBoundsException("Index: " + index + ", Size:" + 
                                          size);
  }

  /**
   * This method checks that a value is between 0 (inclusive) and size
   * (exclusive). If it is not, an exception is thrown.
   * Note that since this method is private, it will be inlined.
   *
   * @exception IndexOutOfBoundsException if the value is out of range.
   */
  private void checkBoundsExclusive(int index)
  {
    if (index < 0 || index >= size)
      throw new IndexOutOfBoundsException("Index: " + index + ", Size:" + 
                                          size);
  }

  public int size()
  {
    checkMod();
    return size;
  }

  public Object set(int index, Object o)
  {
    checkMod();
    checkBoundsExclusive(index);
    o = backingList.set(index + offset, o);
    return o;
  }

  public Object get(int index)
  {
    checkMod();
    checkBoundsExclusive(index);
    return backingList.get(index + offset);
  }

  public void add(int index, Object o)
  {
    checkMod();
    checkBoundsInclusive(index);
    backingList.add(index + offset, o);
    this.modCount++;
    size++;
  }

  public Object remove(int index)
  {
    checkMod();
    checkBoundsExclusive(index);
    Object o = backingList.remove(index + offset);
    this.modCount++;
    size--;
    return o;
  }

  public void removeRange(int fromIndex, int toIndex)
  {
    checkMod();
    checkBoundsExclusive(fromIndex);
    checkBoundsInclusive(toIndex);

    // this call will catch the toIndex < fromIndex condition
    backingList.removeRange(offset + fromIndex, offset + toIndex);
    this.modCount = backingList.modCount;
    size -= toIndex - fromIndex;
  }

  public boolean addAll(int index, Collection c)
  {
    checkMod();
    checkBoundsInclusive(index);
    int csize = c.size();
    boolean result = backingList.addAll(offset + index, c);
    this.modCount = backingList.modCount;
    size += csize;
    return result;
  }

  public Iterator iterator()
  {
    return listIterator(0);
  }

  public ListIterator listIterator(final int index)
  {      
    checkMod();
    checkBoundsInclusive(index);

    return new ListIterator() 
    {
      ListIterator i = backingList.listIterator(index + offset);
      int position = index;

      public boolean hasNext()
      {
//        checkMod();
        return position < size;
      }

      public boolean hasPrevious()
      {
//        checkMod();
        return position > 0;
      }

      public Object next()
      {
        if (position < size)
	  {
            Object o = i.next();
            position++;
            return o;
          }
	else
          throw new NoSuchElementException();
      }

      public Object previous()
      {
        if (position > 0)
	  {
            Object o = i.previous();
            position--;
            return o;
          }
	else
          throw new NoSuchElementException();
      }

      public int nextIndex()
      {
        return offset + i.nextIndex();
      }

      public int previousIndex()
      {
        return offset + i.previousIndex();
      }

      public void remove()
      {
        i.remove();
	modCount++;
        size--;
        position = nextIndex();
      }

      public void set(Object o)
      {
        i.set(o);
      }

      public void add(Object o)
      {
        i.add(o);
	modCount++;
        size++;
        position++;
      }

      // Here is the reason why the various modCount fields are mostly
      // ignored in this wrapper listIterator.
      // IF the backing listIterator is failfast, then the following holds:
      //   Using any other method on this list will call a corresponding
      //   method on the backing list *after* the backing listIterator
      //   is created, which will in turn cause a ConcurrentModException
      //   when this listIterator comes to use the backing one. So it is
      //   implicitly failfast.
      // If the backing listIterator is NOT failfast, then the whole of
      //   this list isn't failfast, because the modCount field of the
      //   backing list is not valid. It would still be *possible* to
      //   make the iterator failfast wrt modifications of the sublist
      //   only, but somewhat pointless when the list can be changed under
      //   us.
      // Either way, no explicit handling of modCount is needed.
      // However modCount++ must be executed in add and remove, and size
      // must also be updated in these two methods, since they do not go
      // through the corresponding methods of the subList.
    };
  }
}  // SubList
