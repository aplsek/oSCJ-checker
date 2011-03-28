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
public class Long extends Number
   implements Comparable<Long>, Serializable 
{

  @SCJAllowed
  public static final long MAX_VALUE = 0;
  @SCJAllowed
  public static final long MIN_VALUE = 0;
  @SCJAllowed
  public static final int SIZE = 64;
  @SCJAllowed
  public static final Class<Long> TYPE = null;
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public Long(long val) {
  }
  
  /**
   * Allocates no memory. Does not allow "this" or "str" argument to escape
   * local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public Long(String str) throws NumberFormatException {
  }

  /**
   * Allocates no memory. 
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static int bitCount(long i)
  {
    return 0;
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
  public int compareTo(Long other) {
    return 0; // skeleton
  }
  
  /**
   * Does not allow "str" argument to escape local variables. Allocates a Long
   * result object in the caller's scope.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static Long decode(String str) throws NumberFormatException {
    return null; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public double doubleValue() {
    return (double) 0.0; // skeleton
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
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public float floatValue() {
    return (float) 0; // skeleton
  }
  
  /**
   * Does not allow "str" argument to escape local variables. Allocates a Long
   * result object in the caller's scope.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static Long getLong(String str) {
    return null; // skeleton
  }
  
  /**
   * Does not allow "str" argument to escape local variables. Allocates a Long
   * result object in the caller's scope.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static Long getLong(String str, long v) {
    return null; // skeleton
  }
  
  /**
   * Does not allow "str" argument to escape local variables. Allocates a Long
   * result object in the caller's scope.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static Long getLong(String str, Long v) {
    return null; // skeleton
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
   * Allocates no memory. 
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static long highestOneBit(long i)
  {
    return 0L;
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
  public long longValue() {
    return (long) 0; // skeleton
  }
  
  /**
   * Allocates no memory. 
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static long lowestOneBit(long i)
  {
    return 0L;
  }

  /**
   * Allocates no memory. 
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static int numberOfLeadingZeros(long i)
  {
    return 0;
  }

  /**
   * Allocates no memory. 
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static int numberOfTrailingZeros(long i)
  {
    return 0;
  }

  /**
   * Allocates no memory. Does not allow "this" or "other" argument to escape
   * local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static long parseLong(String str) throws NumberFormatException {
    return (long) 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" or "other" argument to escape
   * local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static long parseLong(String str, int base)
    throws NumberFormatException {
    return (long) 0; // skeleton
  }
  
  /**
   * Allocates no memory. 
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static long reverse(long i)
  {
    return 0L;
  }

  /**
   * Allocates no memory. 
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static long reverseBytes(long i)
  {
    return 0L;
  }

  /**
   * Allocates no memory. 
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static long rotateLeft(long i, int distance)
  {
    return 0L;
  }

  /**
   * Allocates no memory. 
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static long rotateRight(long i, int distance)
  {
    return 0L;
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
   * Allocates no memory. 
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static int signum(long i)
  {
    return 0;
  }

  /**
   * Does not allow "this" to escape local variables. Allocates a String and
   * associated internal "structure" (e.g. char[]) in caller's scope.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static String toBinaryString(long v) {
    return null; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables. Allocates a String and
   * associated internal "structure" (e.g. char[]) in caller's scope.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static String toHexString(long v) {
    return null; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables. Allocates a String and
   * associated internal "structure" (e.g. char[]) in caller's scope.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static String toOctalString(long v) {
    return null; // skeleton
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
  public static String toString(long v) {
    return null; // skeleton
  }
  
  /**
   * Allocates a String and associated internal "structure" (e.g. char[]) in
   * caller's scope.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static String toString(long v, int base) {
    return null; // skeleton
  }
  
  /**
   * Allocates a Long in caller's scope.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static Long valueOf(long val) {
    return null; // skeleton
  }
  
  /**
   * Does not allow "str" argument to escape local variables. Allocates a Long
   * in caller's scope.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static Long valueOf(String str) throws NumberFormatException {
    return null; // skeleton
  }
  
  /**
   * Does not allow "str" argument to escape local variables. Allocates a Long
   * in caller's scope.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static Long valueOf(String str, int base)
    throws NumberFormatException {
    return null; // skeleton
  }
}
