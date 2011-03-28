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

import java.io.Serializable;

import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

import static javax.safetycritical.annotate.Allocate.Area.CURRENT;

@SCJAllowed
public class Double extends Number
   implements Comparable<Double>, Serializable
{

  @SCJAllowed
  public static final double MAX_EXPONENT = 0;
  @SCJAllowed
  public static final double MIN_EXPONENT = 0;
  @SCJAllowed
  public static final double MIN_NORMAL = 0;
  @SCJAllowed
  public static final double MAX_VALUE = 0;
  @SCJAllowed
  public static final double MIN_VALUE = 0;
  @SCJAllowed
  public static final double NaN = 0;
  @SCJAllowed
  public static final double NEGATIVE_INFINITY = 0;
  @SCJAllowed
  public static final double POSITIVE_INFINITY = 0;
  @SCJAllowed
  public static final Class<Double> TYPE = null;
  @SCJAllowed
  public static final int SIZE = 64;


  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public Double(double val) {
  }
  
  /**
   * Allocates no memory. Does not allow "this" or "str" argument to escape
   * local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public Double(String str) throws NumberFormatException {
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public byte byteValue() {
    return (byte) 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" or "other" argument to escape
   * local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public int compareTo(Double other) {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public double doubleValue() {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" or "obj" argument to escape
   * local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public boolean equals(Object obj) {
    return false; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static long doubleToLongBits(double v) {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static int compare(double value1, double value2) {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public float floatValue() {
    return (float) 0.0; // skeleton
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
  public boolean isInfinite() {
    return false; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static boolean isInfinite(double v) {
    return false; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public boolean isNaN() {
    return false;
  }
  
  /**
   * Allocates no memory.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static boolean isNaN(double v) {
    return false;
  }
  
  /**
   * Allocates no memory.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static double longBitsToDouble(long v) {
    return 0.0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public long longValue() {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public short shortValue() {
    return (short) 0; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables. Allocates a String and
   * associated internal "structure" (e.g. char[]) in caller's
   * scope. (Note: this 
   * semantics is desired for consistency with overridden implementation of
   * Object.toString()).
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public String toString() {
    return null; // skeleton
  }
  
  /**
   * Allocates a String and associated internal "structure" (e.g. char[]) in
   * caller's scope.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static String toString(double v) {
    return null; // skeleton
  }
  
  /**
   * Allocates a Double in caller's scope.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static Double valueOf(double val) {
    return null; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables. Allocates a Double in
   * caller's scope.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static Double valueOf(String str) throws NumberFormatException {
    return null; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static double parseDouble(String s) throws NumberFormatException {
    return 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static native long doubleToRawLongBits(double val);
  
}
