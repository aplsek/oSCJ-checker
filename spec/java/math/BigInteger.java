/*---------------------------------------------------------------------*\
 *
 * Copyright (c) 2007, 2008 Aonix, Paris, France
 *
 * This code is provided for educational purposes under the LGPL 2
 * license from GNU.  This notice must appear in all derived versions
 * of the code and the source must be made available with any binary
 * version.
 *
\*---------------------------------------------------------------------*/
package java.math;

import static javax.safetycritical.annotate.Allocate.Area.CURRENT;
import static javax.safetycritical.annotate.Allocate.Area.THIS;

import static javax.safetycritical.annotate.Scope.CALLER;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

@SCJAllowed
public class BigInteger extends Number implements Comparable<BigInteger> {

  @SCJAllowed
  public static final BigInteger ONE = null;

  @SCJAllowed
  public static final BigInteger TEN = null;

  @SCJAllowed
  public static final BigInteger ZERO = null;


  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger(byte[] val) {
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger(int signum, byte[] magnitude) {
  }

  /**
   * Does not allow "this" to escape local variables.
   * <p>
   * Not @SCJAllowed because we don't have Random.  But maybe we should...
   *
  @Allocate({THIS})
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger(int bitLength, int certainty, Random rnd) {
  }
  */


  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger(String val) {
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger(String val, int radix) {
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger abs() {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger add(BigInteger val) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger andNot(BigInteger val) {
    return null; // skeleton
  }

  /**
   * Allocates no memory.  Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public int bitCount() {
    return 0; // skeleton
  }

  /**
   * Allocates no memory.  Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public int bitLength() {
    return 0; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger clearBit(int n) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public int compareTo(BigInteger val) {
    return 0; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger divide(BigInteger val) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger[] divideAndRemainder(BigInteger val) {
    return null; // skeleton
  }

  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public double doubleValue(BigInteger val) {
    return 0.0; // skeleton
  }

  /**
   * Does not allow "this" or "x" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public boolean equals(Object x) {
    return true; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger flipBit(int n) {
    return null; // skeleton
  }

  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public float floatValue(BigInteger val) {
    return (float) 0.0; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger gcd(BigInteger val) {
    return null; // skeleton
  }

  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public int getLowestSetBit() {
    return 0; // skeleton
  }

  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public int hashCode() {
    return 0; // skeleton
  }

  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public int intValue() {
    return 0; // skeleton
  }

  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public boolean isProbablePrime(int certainty) {
    return false; // skeleton
  }

  /**
   * Allocates no memory.  Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public long longValue() {
    return 0L; // skeleton
  }

  /**
   * Does not allow "this" or "max" to escape
   * local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger max(BigInteger val) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" or "val" to escape
   * local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger min(BigInteger val) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" or "val" to escape
   * local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger mod(BigInteger val) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" or "val" to escape
   * local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger modInverse(BigInteger val) {
    return null; // skeleton
  }

  /**
   * Does not allow "this", "exponent", or "m" to
   * escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger modPow(BigInteger exponent, BigInteger m) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" or val to escape local
   * variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  @RunsIn(CALLER)
  public BigInteger multiply(BigInteger val) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  @RunsIn(CALLER)
  public BigInteger negate() {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger nextProbablePrime() {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger not() {
    return null; // skeleton
  }

  /**
   * Does not allow "this" or "val" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger or(BigInteger val) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger pow(int exponent) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   * <p>
   * Not SCJAllowed because we don't have Random, but maybe we should.
   *
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger probablePrime(int bitLength, Random rnd) {
    return null; // skeleton
  }
  */

  /**
   * Does not allow "this" or "val" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger remainder(BigInteger val) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger setBit(int n) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger shiftLeft(int n) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger shiftRight(int n) {
    return null; // skeleton
  }

  /**
   * Allocates no memory.  Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public int signum() {
    return 0; // skeleton
  }

  /**
   * Does not allow "this" or "val" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger subtract(BigInteger val) {
    return null; // skeleton
  }

  /**
   * Allocates no memory.  Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public boolean testBit(int n) {
    return false; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger toByteArray() {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public String toString() {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public String toString(int radix) {
    return null; // skeleton
  }

  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static BigInteger valueOf(long val) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" or "val" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger xor(BigInteger val) {
    return null; // skeleton
  }

  @SCJAllowed
  @Override
  @SCJRestricted(maySelfSuspend = false)
  public double doubleValue() {
    // TODO Auto-generated method stub
    return 0;
  }

  @SCJAllowed
  @Override
  @SCJRestricted(maySelfSuspend = false)
  public float floatValue() {
    // TODO Auto-generated method stub
    return 0;
  }

  @SCJAllowed
  @Override
  @SCJRestricted(maySelfSuspend = false)
  public short shortValue() {
    // TODO Auto-generated method stub
    return 0;
  }
}
