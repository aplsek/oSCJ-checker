// BitSet - A vector of bits.
/* Copyright (C) 1998, 1999, 2000  Free Software Foundation
This file is part of GNU Classpath.
It has been modified slightly to fit the OVM framework.
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
As a special exception, if you link this library with other files to
produce an executable, this library does not by itself cause the
resulting executable to be covered by the GNU General Public License.
This exception does not however invalidate any other reasons why the
executable file might be covered by the GNU General Public License. */
package ovm.util;

import java.io.Serializable;

/* Written using "Java Class Libraries", 2nd edition, ISBN 0-201-31002-3
 * hashCode algorithm taken from JDK 1.2 docs.
 */
/**
 * This class can be thought of in two ways.  You can see it as a
 * vector of bits or as a set of non-negative integers.  The name
 * <code>BitSet</code> is a bit misleading.
 *
 * It is implemented by a bit vector, but its equally possible to see
 * it as set of non-negative integer; each integer in the set is
 * represented by a set bit at the corresponding index.  The size of
 * this structure is determined by the highest integer in the set.
 *
 * You can union, intersect and build (symmetric) remainders, by
 * invoking the logical operations and, or, andNot, resp. xor.
 *
 * This implementation is NOT synchronized against concurrent access from
 * multiple threads. Specifically, if one thread is reading from a bitset
 * while another thread is simultaneously modifying it, the results are
 * undefined.
 *
 * @specnote Historically, there has been some confusion as to whether or not 
 *           this class should be synchronized. From an efficiency perspective,
 *           it is very undesirable to synchronize it because multiple locks 
 *           and explicit lock ordering are required to safely synchronize some
 *           methods. The JCL 1.2 supplement book specifies that as of JDK 
 *           1.2, the class is no longer synchronized.
 *
 * @author Jochen Hoenicke
 * @author Tom Tromey <tromey@cygnus.com>
 * @author Ben L. Titzer
 */
public class BitSet implements Cloneable, Serializable
{
  /**
   * Create a new empty bit set.
   */
  public BitSet()
  {
    this(64);
  }

  /**
   * Create a new empty bit set, with a given size.  This
   * constructor reserves enough space to represent the integers
   * from <code>0</code> to <code>nbits-1</code>.  
   * @param nbits the initial size of the bit set.
   * @throws NegativeArraySizeException if the specified initial
   * size is negative.  
   * @require nbits >= 0
   */
  public BitSet(int nbits)
  {
    if (nbits < 0)
      throw new NegativeArraySizeException();
    int length = nbits / 64;
    if (nbits % 64 != 0)
      ++length;
    bits = new long[length];
  }

  /**
   * Performs the logical AND operation on this bit set and the
   * given <code>set</code>.  This means it builds the intersection
   * of the two sets.  The result is stored into this bit set.
   * @param bs the second bit set.
   * @require bs != null
   */
  public void and(BitSet bs)
  {
    int max = Math.min(bits.length, bs.bits.length);
    int i;
    for (i = 0; i < max; ++i)
      bits[i] &= bs.bits[i];
    for (; i < bits.length; ++i)
      bits[i] = 0;
  }

  /**
   * Performs the logical AND operation on this bit set and the
   * complement of the given <code>set</code>.  This means it
   * selects every element in the first set, that isn't in the
   * second set.  The result is stored into this bit set.  
   * @param bs the second bit set.  
   * @require bs != null
   * @since JDK1.2
   */
  public void andNot(BitSet bs)
  {
    int max = Math.min(bits.length, bs.bits.length);
    int i;
    for (i = 0; i < max; ++i)
      bits[i] &= ~bs.bits[i];
  }

  public void copyInto(BitSet bs) {
    bs.ensure(bits.length - 1);
    for (int i = 0; i < bits.length; i++)
      bs.bits[i] = bits[i];
    for (int i = bits.length; i < bs.bits.length; i++)
      bs.bits[i] = 0;
  }

  /**
   * Removes the integer <code>bitIndex</code> from this set. That is
   * the corresponding bit is cleared.  If the index is not in the set,
   * this method does nothing.
   * @param pos a non-negative integer.
   * @exception ArrayIndexOutOfBoundsException if the specified bit index
   * is negative.
   * @require pos >= 0
   */
  public void clear(int pos)
  {
    if (pos < 0)
      throw new IndexOutOfBoundsException();
    int bit = pos % 64;
    int offset = pos / 64;
    ensure(offset);
    bits[offset] &= ~(1L << bit);
  }

  /**
   * Create a clone of this bit set, that is an instance of the same
   * class and contains the same elements.  But it doesn't change when
   * this bit set changes.
   * @return the clone of this object.
   */
  public Object clone()
  {
    BitSet bs = new BitSet(bits.length * 64);
    System.arraycopy(bits, 0, bs.bits, 0, bits.length);
    return bs;
  }

  /**
   * Returns true if the <code>obj</code> is a bit set that contains
   * exactly the same elements as this bit set, otherwise false.
   * @return true if obj equals this bit set.
   */
  public boolean equals(Object obj)
  {
    if (!(obj instanceof BitSet))
      return false;
    BitSet bs = (BitSet) obj;
    int max = Math.min(bits.length, bs.bits.length);
    int i;
    for (i = 0; i < max; ++i)
      if (bits[i] != bs.bits[i])
	return false;
    // If one is larger, check to make sure all extra bits are 0.
    for (int j = i; j < bits.length; ++j)
      if (bits[j] != 0)
	return false;
    for (int j = i; j < bs.bits.length; ++j)
      if (bs.bits[j] != 0)
	return false;
    return true;
  }

  /**
   * Returns true if the integer <code>bitIndex</code> is in this bit
   * set, otherwise false.
   * @param pos a non-negative integer
   * @return the value of the bit at the specified index.
   * @exception ArrayIndexOutOfBoundsException if the specified bit index
   * is negative.
   * @require pos >= 0
   */
  public boolean get(int pos)
  {
    if (pos < 0)
      throw new IndexOutOfBoundsException();

    int bit = pos % 64;
    int offset = pos / 64;

    if (offset >= bits.length)
      return false;

    return (bits[offset] & (1L << bit)) == 0 ? false : true;
  }

  /**
   * Returns a hash code value for this bit set.  The hash code of 
   * two bit sets containing the same integers is identical.  The algorithm
   * used to compute it is as follows:
   *
   * Suppose the bits in the BitSet were to be stored in an array of
   * long integers called <code>bits</code>, in such a manner that
   * bit <code>k</code> is set in the BitSet (for non-negative values
   * of <code>k</code>) if and only if
   *
   * <pre>
   * ((k/64) < bits.length) && ((bits[k/64] & (1L << (bit % 64))) != 0)
   * </pre>
   *
   * Then the following definition of the hashCode method
   * would be a correct implementation of the actual algorithm:
   *
   * <pre>
   * public int hashCode() {
   *     long h = 1234;
   *     for (int i = bits.length-1; i>=0; i--) {
   *         h ^= bits[i] * (i + 1);
   *     }
   *     return (int)((h >> 32) ^ h);
   * }
   * </pre>
   *
   * Note that the hash code values changes, if the set is changed.
   * @return the hash code value for this bit set.
   */
  public int hashCode()
  {
    long h = 1234;
    for (int i = bits.length - 1; i >= 0; --i)
      h ^= bits[i] * (i + 1);
    return (int) ((h >> 32) ^ h);
  }

  /**
   * Returns the logical number of bits actually used by this bit
   * set.  It returns the index of the highest set bit plus one.
   * Note that this method doesn't return the number of set bits.
   * @return the index of the highest set bit plus one.  
   */
  public int length()
  {
    // Set i to highest index that contains a non-zero value.
    int i;
    for (i = bits.length - 1; i >= 0 && bits[i] == 0; --i)
      ;

    // if i < 0 all bits are cleared.
    if (i < 0)
      return 0;

    // Now determine the exact length.
    long b = bits[i];
    int len = (i + 1) * 64;
    // b >= 0 checks if the highest bit is zero.
    while (b >= 0)
      {
	--len;
	b <<= 1;
      }

    return len;
  }

  /**
   * Performs the logical OR operation on this bit set and the
   * given <code>set</code>.  This means it builds the union
   * of the two sets.  The result is stored into this bit set, which
   * grows as necessary.
   * @param bs the second bit set.
   * @exception OutOfMemoryError if the current set can't grow.
   * @require bs != null
   */
  public void or(BitSet bs)
  {
    ensure(bs.bits.length - 1);
    int i;
    for (i = 0; i < bs.bits.length; ++i)
      bits[i] |= bs.bits[i];
  }

  /**
   * Add the integer <code>bitIndex</code> to this set.  That is 
   * the corresponding bit is set to true.  If the index was already in
   * the set, this method does nothing.  The size of this structure
   * is automatically increased as necessary.
   * @param pos a non-negative integer.
   * @exception ArrayIndexOutOfBoundsException if the specified bit index
   * is negative.
   * @require pos >= 0
   */
  public void set(int pos)
  {
    if (pos < 0)
      throw new IndexOutOfBoundsException();
    int bit = pos % 64;
    int offset = pos / 64;
    ensure(offset);
    bits[offset] |= 1L << bit;
  }

  public void set(int pos, boolean value) 
  {
    if (value)
      set(pos);
    else
      clear(pos);
  }

    /**
     * Set a range of bits.  Jdk-1.4 also defines clear(int, int), and
     * set(int, int, boolean).
     **/
    public void set(int from, int to) {
	if (to < from || from < 0)
	    throw new IndexOutOfBoundsException();
	ensure(to >> 6);
	if (to == 0)
	    return;
	int fword = from >> 6;
	int fbit = from & 0x3f;
	int tword = to >> 6;
	int tbit = to & 0x3f;
	
	long mask = -1L << fbit;
	if (fword == tword)
	    mask &= (-1L >>> (64 - tbit));
	bits[fword++] |= mask;
	while (fword < tword)
	    bits[fword++] = -1;
	if (tbit != 0) {
	    // new BitSet().set(0, 64) will have tword = 1, tbit = 0
	    mask = -1L >>> (64 - tbit);
	    bits[tword] |= mask;
	}
    }

    /**
     * Clear a range of bits.
     **/
    public void clear(int from, int to) {
	if (to < from || from < 0)
	    throw new IndexOutOfBoundsException();
	ensure(to >> 6);
	if (to == 0)
	    return;
	int fword = from >> 6;
	int fbit = from & 0x3f;
	int tword = to >> 6;
	int tbit = to & 0x3f;

	// This may seem like an odd way to compute the mask, but that
	// is only because I copied the code from set(int, int)
	long mask = -1L << fbit;
	if (fword == tword)
	    mask &= (-1L >>> (64 - tbit));
	bits[fword++] &= ~mask;
	while (fword < tword)
	    bits[fword++] = 0;
	if (tbit != 0) {
	    // new BitSet().set(0, 64) will have tword = 1, tbit = 0
	    mask = -1L >>> (64 - tbit);
	    bits[tword] &= ~mask;
	}
    }

  public void set(int from, int to, boolean value)
  {
    while (from <= to)
      set(from++, value);
  }

  /**
   * Returns the number of bits actually used by this bit set.  Note
   * that this method doesn't return the number of set bits.
   * @return the number of bits currently used.  
   */
  public int size()
  {
    return bits.length * 64;
  }

  /**
   * Returns the string representation of this bit set.  This
   * consists of a comma separated list of the integers in this set
   * surrounded by curly braces.  There is a space after each comma.
   * @return the string representation.
   */
  public String toString()
  {
    String r = "{";
    boolean first = true;
    for (int i = 0; i < bits.length; ++i)
      {
	long bit = 1;
	long word = bits[i];
	if (word == 0)
	  continue;
	for (int j = 0; j < 64; ++j)
	  {
	    if ((word & bit) != 0)
	      {
		if (!first)
		  r += ", ";
		r += Integer.toString(64 * i + j);
		first = false;
	      }
	    bit <<= 1;
	  }
      }

    return r += "}";
  }

  /**
   * Performs the logical XOR operation on this bit set and the
   * given <code>set</code>.  This means it builds the symmetric
   * remainder of the two sets (the elements that are in one set,
   * but not in the other).  The result is stored into this bit set,
   * which grows as necessary.  
   * @param bs the second bit set.
   * @exception OutOfMemoryError if the current set can't grow.  
   * @require bs != null
   */
  public void xor(BitSet bs)
  {
    ensure(bs.bits.length - 1);
    int i;
    for (i = 0; i < bs.bits.length; ++i)
      bits[i] ^= bs.bits[i];
  }

  /**
   * A random jdk1.4 method.  It would probably be a good idea to
   * implement the others as well.
   */
  public int nextSetBit(int start)
  {
    int idx = start >> 6;
    while (idx < bits.length)
      {
	long word = bits[idx] >> start;
	if (word == 0)
	  {
	    start = (start + 64) & ~63;
	    idx++;
	  }
	else
	  {
	    while ((word & 1) == 0)
	      {
		start++;
		word >>= 1;
	      }
	    return start;
	  }
      }
    return -1;
  }

  /**
   * A random jdk1.4 method.  It would probably be a good idea to
   * implement the others as well.
   */
  public int nextClearBit(int start)
  {
    int idx = start >> 6;
    while (idx < bits.length)
      {
	long word = bits[idx] >>> start;
	if (word == (-1 >>> start))
	  {
	    start = (start + 64) & ~63;
	    idx++;
	  }
	else
	  {
	    while ((word & 1) == 1)
	      {
		start++;
		word >>>= 1;
	      }
	    return start;
	  }
      }
    return -1;
  }

  /**
   * Random jdk1.4 method
   */
  public int cardinality() {
      int count = 0;
      for (int pos = nextSetBit(0); pos != -1; pos = nextSetBit(pos+1))
	  count++;
      return count;
  }

  // Make sure the vector is big enough.
  private final void ensure(int lastElt)
  {
    if (lastElt + 1 > bits.length)
      {
	long[] nd = new long[lastElt + 1];
	System.arraycopy(bits, 0, nd, 0, bits.length);
	bits = nd;
      }
  }

  // The actual bits.
  long[] bits;

  private static final long serialVersionUID = 7997698588986878753L;
}
