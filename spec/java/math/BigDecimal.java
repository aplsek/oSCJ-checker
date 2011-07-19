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

import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

@SCJAllowed
public class BigDecimal extends Number implements Comparable<BigDecimal> {

  @SCJAllowed
  public static final BigDecimal ONE = null;

  @SCJAllowed
  public static final BigDecimal TEN = null;

  @SCJAllowed
  public static final BigDecimal ZERO = null;

  @SCJAllowed
  public static final int ROUND_CELING = 0;

  @SCJAllowed
  public static final int ROUND_DOWN = 0;

  @SCJAllowed
  public static final int ROUND_FLOOR = 0;

  @SCJAllowed
  public static final int ROUND_HALF_DOWN = 0;

  @SCJAllowed
  public static final int ROUND_HALF_EVEN = 0;

  @SCJAllowed
  public static final int ROUND_HALF_UP = 0;

  @SCJAllowed
  public static final int ROUND_UNNECESSARY = 0;

  @SCJAllowed
  public static final int ROUND_UP = 0;

  /**
   * Does not allow "this" or "val" to escape local variables.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal(BigInteger val) {
  }

  /**
   * Does not allow "this" or "val" to escape local variables.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal(BigInteger val, int scale) {
  }

  /**
   * Does not allow "this" or "val" to escape local variables.
   * <p>
   * Not SCJAllowed because we don't have MathContext
   *
  @Allocate({THIS})
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal(BigInteger val, int scale, MathContext mc) {
  }
  */

  /**
   * Does not allow "this" or "val" to escape local variables.
   * <p>
   * Not SCJAllowed because we don't have MathContext
   *
  @Allocate({THIS})
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal(BigInteger val, MathContext mc) {
  }
  */

  /**
   * Does not allow "this" or "in" to escape local variables.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal(char[] in) {
  }

  /**
   * Does not allow "this" or "in" to escape local variables.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal(char[] in, int offset, int len) {
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal(double val) {
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal(int val) {
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal(long val) {
  }

  /**
   * Does not allow "this" or "val" to escape local variables.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal(String val) {
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal abs() {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal add(BigDecimal val) {
    return null; // skeleton
  }


  /**
   * Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public byte byteValueExact() {
    return 0; // skeleton
  }

  /**
   * Does not allow "this" or "val" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public int compareTo(BigDecimal val) {
    return 0; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal divide(BigDecimal val) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal[] divideAndRemainder(BigDecimal val) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal[] divideToIntegralValue(BigDecimal val) {
    return null; // skeleton
  }

  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false, mayAllocate = false)
  public double doubleValue(BigDecimal val) {
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
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false, mayAllocate = false)
  public float floatValue(BigDecimal val) {
    return (float) 0.0; // skeleton
  }

  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false, mayAllocate = false)
  public int hashCode() {
    return 0; // skeleton
  }

  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false, mayAllocate = false)
  public int intValue() {
    return 0; // skeleton
  }

  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false, mayAllocate = false)
  public int intValueExact() {
    return 0; // skeleton
  }

  /**
   * Allocates no memory.  Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false, mayAllocate = false)
  public long longValue() {
    return 0; // skeleton
  }

  /**
   * Allocates no memory.  Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false, mayAllocate = false)
  public int longValueExact() {
    return 0; // skeleton
  }

  /**
   * Does not allow "this" or "max" to escape
   * local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal max(BigDecimal val) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" or "val" to escape
   * local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal min(BigDecimal val) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape
   * local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal movePointLeft(int n) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape
   * local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal movePointRight(int n) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" or "val" to escape local
   * variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  @RunsIn(CALLER)
  public BigDecimal multiply(BigDecimal val) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  @RunsIn(CALLER)
  public BigDecimal negate() {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal plus() {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal pow(int exponent) {
    return null; // skeleton
  }

  /**
   * Allocates no memory.  Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false, mayAllocate = false)
  public int precision() {
    return 0; // skeleton
  }

  /**
   * Does not allow "this" or "val" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal remainder(BigDecimal val) {
    return null; // skeleton
  }

  /**
   * Allocates no memory.  Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false, mayAllocate = false)
  public int scale() {
    return 0; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal scaleByPowerOfTen(int n) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal setScale(int newScale) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal setScale(int newScale, int roundingMode) {
    return null; // skeleton
  }

  /**
   * Allocates no memory.  Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false, mayAllocate = false)
  public short shortValueExact() {
    return 0; // skeleton
  }

  /**
   * Allocates no memory.  Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false, mayAllocate = false)
  public int signum() {
    return 0; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal stripTrailingZeros() {
    return null; // skeleton
  }

  /**
   * Does not allow "this" or "val" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigDecimal subtract(BigDecimal val) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" or "val" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger toBigInteger() {
    return null; // skeleton
  }

  /**
   * Does not allow "this" or "val" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public String toEngineeringString() {
    return null; // skeleton
  }

  /**
   * Does not allow "this" or "val" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public String toPlainString() {
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
  public BigDecimal ulp() {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public BigInteger unscaledValue() {
    return null; // skeleton
  }

  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static BigDecimal valueOf(double val) {
    return null; // skeleton
  }

  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static BigDecimal valueOf(long val) {
    return null; // skeleton
  }

  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static BigDecimal valueOf(long unscaledVal, int scale) {
    return null; // skeleton
  }

  // JAN: FIX
  @Override
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public double doubleValue() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public float floatValue() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public short shortValue() {
    // TODO Auto-generated method stub
    return 0;
  }
}
