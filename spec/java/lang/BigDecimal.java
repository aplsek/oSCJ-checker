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
package java.lang;

import static javax.safetycritical.annotate.Allocate.Area.*;
import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.SCJAllowed;

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
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal(BigInteger val) {
  }

  /**
   * Does not allow "this" or "val" to escape local variables.
   */
  @Allocate({THIS})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal(BigInteger val, int scale) {
  }

  /**
   * Does not allow "this" or "val" to escape local variables.
   * <p>
   * Not SCJAllowed because we don't have MathContext
   *
  @Allocate({THIS})
  @SCJRestricted(maySelfSuspend=false)
  public BigDecimal(BigInteger val, int scale, MathContext mc) {
  }
  */

  /**
   * Does not allow "this" or "val" to escape local variables.
   * <p>
   * Not SCJAllowed because we don't have MathContext
   *
  @Allocate({THIS})
  @SCJRestricted(maySelfSuspend=false)
  public BigDecimal(BigInteger val, MathContext mc) {
  }
  */

  /**
   * Does not allow "this" or "in" to escape local variables.
   */
  @Allocate({THIS})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal(char[] in) {
  }

  /**
   * Does not allow "this" or "in" to escape local variables.
   */
  @Allocate({THIS})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal(char[] in, int offset, int len) {
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({THIS})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal(double val) {
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({THIS})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal(int val) {
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({THIS})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal(long val) {
  }

  /**
   * Does not allow "this" or "val" to escape local variables.
   */
  @Allocate({THIS})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal(String val) {
  }
  
  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal abs() {
    return null; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal add(BigDecimal val) {
    return null; // skeleton
  }
  
  
  /**
   * Does not allow "this" to escape local variables.
   */
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public byte byteValueExact() {
    return 0; // skeleton
  }
  
  /**
   * Does not allow "this" or "val" to escape local variables.
   */
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public int compareTo(BigDecimal val) {
    return 0; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal divide(BigDecimal val) {
    return null; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal[] divideAndRemainder(BigDecimal val) {
    return null; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal[] divideToIntegralValue(BigDecimal val) {
    return null; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public double doubleValue(BigDecimal val) {
    return 0.0; // skeleton
  }
  
  /**
   * Does not allow "this" or "x" to escape local variables.
   */
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public boolean equals(Object x) {
    return true; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public float floatValue(BigDecimal val) {
    return (float) 0.0; // skeleton
  }

  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public int hashCode() {
    return 0; // skeleton
  }

  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public int intValue() {
    return 0; // skeleton
  }

  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public int intValueExact() {
    return 0; // skeleton
  }

  /**
   * Allocates no memory.  Does not allow "this" to escape local variables.
   */
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public long longValue() {
    return 0; // skeleton
  }

  /**
   * Allocates no memory.  Does not allow "this" to escape local variables.
   */
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public int longValueExact() {
    return 0; // skeleton
  }

  /**
   * Does not allow "this" or "max" to escape
   * local variables. 
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal max(BigDecimal val) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" or "val" to escape
   * local variables. 
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal min(BigDecimal val) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape
   * local variables. 
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal movePointLeft(int n) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape
   * local variables. 
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal movePointRight(int n) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" or "val" to escape local
   * variables. 
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal multiply(BigDecimal val) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal negate() {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal plus() {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal pow(int exponent) {
    return null; // skeleton
  }

  /**
   * Allocates no memory.  Does not allow "this" to escape local variables.
   */
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public int precision() {
    return 0; // skeleton
  }

  /**
   * Does not allow "this" or "val" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal remainder(BigDecimal val) {
    return null; // skeleton
  }

  /**
   * Allocates no memory.  Does not allow "this" to escape local variables.
   */
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public int scale() {
    return 0; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal scaleByPowerOfTen(int n) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal setScale(int newScale) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal setScale(int newScale, int roundingMode) {
    return null; // skeleton
  }

  /**
   * Allocates no memory.  Does not allow "this" to escape local variables.
   */
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public short shortValueExact() {
    return 0; // skeleton
  }

  /**
   * Allocates no memory.  Does not allow "this" to escape local variables.
   */
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public int signum() {
    return 0; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal stripTrailingZeros() {
    return null; // skeleton
  }

  /**
   * Does not allow "this" or "val" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal subtract(BigDecimal val) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" or "val" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigInteger toBigInteger() {
    return null; // skeleton
  }

  /**
   * Does not allow "this" or "val" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public String toEngineeringString() {
    return null; // skeleton
  }

  /**
   * Does not allow "this" or "val" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public String toPlainString() {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public String toString() {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigDecimal ulp() {
    return null; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public BigInteger unscaledValue() {
    return null; // skeleton
  }

  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public static BigDecimal valueOf(double val) {
    return null; // skeleton
  }

  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public static BigDecimal valueOf(long val) {
    return null; // skeleton
  }

  @Allocate({CURRENT})
  @SCJRestricted(maySelfSuspend=false)
  @SCJAllowed
  public static BigDecimal valueOf(long unscaledVal, int scale) {
    return null; // skeleton
  }

  // JAN: FIX
@Override
public double doubleValue() {
	// TODO Auto-generated method stub
	return 0;
}

@Override
public float floatValue() {
	// TODO Auto-generated method stub
	return 0;
}

@Override
public short shortValue() {
	// TODO Auto-generated method stub
	return 0;
}
}
