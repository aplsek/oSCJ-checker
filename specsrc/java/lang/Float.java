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
import javax.safetycritical.annotate.BlockFree;
import javax.safetycritical.annotate.SCJAllowed;

import static javax.safetycritical.annotate.Allocate.Area.CURRENT;

@SCJAllowed
public class Float extends Number
   implements Comparable<Float>, Serializable 
{

  @SCJAllowed
  public static final Class<Float> TYPE = null;

  @SCJAllowed
  public static final float MAX_EXPONENT = 0;
  @SCJAllowed
  public static final float MIN_EXPONENT = 0;
  @SCJAllowed
  public static final float MIN_NORMAL = 0;

  @SCJAllowed
  public static final float MAX_VALUE = 0;
  @SCJAllowed
  public static final float MIN_VALUE = 0;
  @SCJAllowed
  public static final float NaN = 0;
  @SCJAllowed
  public static final float NEGATIVE_INFINITY = 0;
  @SCJAllowed
  public static final float POSITIVE_INFINITY = 0;
  
  @SCJAllowed
  public static final int SIZE = 32;

  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public Float(float val) {
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public Float(double val) {
  }
  
  /**
   * Allocates no memory. Does not allow "this" or "str" argument to escape
   * local variables.
   */
  @BlockFree
  @SCJAllowed
  public Float(String str) throws NumberFormatException {
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public byte byteValue() {
    return (byte) 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" or "other" argument to escape
   * local variables.
   */
  @BlockFree
  @SCJAllowed
  public int compareTo(Float other) {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public double doubleValue() {
    return 0.0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" or "obj" argument to escape
   * local variables.
   */
  @BlockFree
  @SCJAllowed
  public boolean equals(Object obj) {
    return false; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static int floatToIntBits(float v) {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static int floatToRawIntBits(float v) {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static int compare(float value1, float value2) {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public float floatValue() {
    return (float) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public int hashCode() {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static float intBitsToFloat(int v) {
    return (float) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public int intValue() {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public boolean isInfinite() {
    return false; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static boolean isInfinite(float v) {
    return false; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public boolean isNaN() {
    return false; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static boolean isNaN(float v) {
    return false; // skeleton
  }

  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public long longValue() {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
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
  @BlockFree
  @SCJAllowed
  public String toString() {
    return null; // skeleton
  }
  
  /**
   * Allocates a String and associated internal "structure" (e.g. char[]) in
   * caller's scope.
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public static String toString(float v) {
    return null; // skeleton
  }

  /**
   * Allocates a String and associated internal "structure" (e.g. char[]) in
   * caller's scope.
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public static String toHexString(float v) {
    return null; // skeleton
  }
  
  /**
   * Allocates a Float in caller's scope.
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public static Float valueOf(float val) {
    return null; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables. Allocates a Float in
   * caller's scope.
   */
  @BlockFree
  @SCJAllowed
  public static Float valueOf(String str) throws NumberFormatException {
    return null; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "s" argument to escape local
   * variables.
   */
  @BlockFree
  @SCJAllowed
  public static float parseFloat(String s) throws NumberFormatException {
    return 0; // skeleton
  }
  
}
