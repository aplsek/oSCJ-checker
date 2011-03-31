package java.util;

import java.io.Serializable;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public class BitSet implements Serializable, Cloneable
{
  /**
   * 
   */
  private static final long serialVersionUID = 9059329337302518348L;

  @SCJAllowed
  public BitSet()
  {
  }

  @SCJAllowed
  public BitSet(int nbits)
  {
  }

  @SCJAllowed
  void and(BitSet set)
  {
  }

  @SCJAllowed
  int cardinality()
  {
    return 0;
  }
  
  @SCJAllowed
  void clear()
  {
  }

  @SCJAllowed
  void clear(int bitIndex)
  {
  }

  @SCJAllowed
  void clear(int fromIndex, int toIndex)
  {
  }

  @Override
  @SCJAllowed
  protected
  Object clone()
  {
    return null;
  }

  @Override
  @SCJAllowed
  public
  boolean equals(Object o)
  {
    return false;
  }

  /**
   * If bitIndex is greater than the current length() of this bitSet,
   * throws IllegalArgumentException
   */
  @SCJAllowed
  void flip(int bitIndex)
  {
  }

  /**
   * If fromIndex or toIndex is greater than this.length(),
   * throws IllegalArgumentException
   */
  @SCJAllowed
  void flip(int fromIndex, int toIndex)
  {
  }

  @SCJAllowed
  boolean get(int bitIndex)
  {
    return false;
  }

  @SCJAllowed
  BitSet get(int fromIndex, int toIndex)
  {
    return null;
  }

  @Override
  @SCJAllowed
  public
  int hashCode()
  {
    return 0;
  }

  @SCJAllowed
  boolean intersects(BitSet set)
  {
    return false;
  }

  @SCJAllowed
  boolean isEmpty()
  {
    return false;
  }

  @SCJAllowed
  int length()
  {
    return 0;
  }

  @SCJAllowed
  int nextClearBit(int fromIndex)
  {
    return fromIndex;
  }

  @SCJAllowed
  int nextSetBit(int fromIndex)
  {
    return fromIndex;
  }

  @SCJAllowed
  void or(BitSet set)
  {
  }

  /**
   * If bitIndex is greater than the current length() of this bitSet,
   * throws IllegalArgumentException
   */
  @SCJAllowed
  void set(int bitIndex)
  {
  }

  /**
   * If bitIndex is greater than the current length() of this bitSet,
   * throws IllegalArgumentException
   */
  @SCJAllowed
  void set(int bitIndex, boolean value)
  {
  }

  /**
   * If fromIndex or toIndex is greater than this.length(),
   * throws IllegalArgumentException
   */
  @SCJAllowed
  void set(int fromIndex, int toIndex)
  {
  }

  /**
   * If fromIndex or toIndex is greater than the current length() of
   * this bitSet, throws IllegalArgumentException
   */
  @SCJAllowed
  void set(int fromIndex, int toIndex, boolean value)
  {
  }

  @SCJAllowed
  int size()
  {
    return 0;
  }

  @Override
  @SCJAllowed
  public
  String toString()
  {
    return null;
  }

  /**
   * If set.length() is greater than the this.length(),
   * throws IllegalArgumentException
   */
  @SCJAllowed
  void xor(BitSet set)
  {
  }
}
