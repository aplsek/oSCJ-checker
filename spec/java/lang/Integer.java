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
public class Integer extends Number
   implements Comparable<Integer>, Serializable 
{
  @SCJAllowed
  public static final Class<Integer> TYPE = null;// Dummy value
  @SCJAllowed
  public static final int MAX_VALUE = 0;
  @SCJAllowed
  public static final int MIN_VALUE = 0;
  @SCJAllowed
  public static final int SIZE = 32;
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public Integer(int val) {
  }
  
  /**
   * Allocates no memory. Does not allow "this" or "str" argument to escape
   * local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public Integer(String str) throws NumberFormatException {
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static int bitCount(int i) {
    return 0; // skeleton
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
  public int compareTo(Integer other) {
    return 0; // skeleton
  }
  
  /**
   * Does not allow "str" argument to escape local variables. Allocates an
   * Integer in caller's scope.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static Integer decode(String str) throws NumberFormatException {
    return null; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public double doubleValue() {
    return 0.0; // skeleton
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
    return (float) 0.0; // sksleton
  }
  
  /**
   * Does not allow "str" argument to escape local variables. Allocates
   * Integer in caller's scope.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static Integer getInteger(String str) {
    return null; // skeleton
  }
  
  /**
   * Does not allow "str" argument to escape local variables. Allocates
   * Integer in caller's scope.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static Integer getInteger(String str, int v) {
    return null; // skeleton
    
  }
  
  /**
   * Does not allow "str" or "v" arguments to escape local variables.
   * Allocates Integer in caller's scope.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static Integer getInteger(String str, Integer v) {
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
  public static int highestOneBit(int i) {
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
  public long longValue() {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. 
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static int lowestOneBit(int i) {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. 
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static int numberOfLeadingZeros(int i) {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "str" argument to escape local
   * variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static int parseInt(String str) throws NumberFormatException {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "str" argument to escape local
   * variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static int parseInt(String str, int radix)
    throws NumberFormatException {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. 
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static int reverse(int i) {
    return 0; // skeleton
  }

  /**
   * Allocates no memory. 
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static int reverseBytes(int i) {
    return 0; // skeleton
  }
    
  /**
   * Allocates no memory. 
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static int rotateLeft(int i, int distance) {
    return 0; // skeleton
  }
    
  /**
   * Allocates no memory. 
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static int rotateRight(int i, int distance) {
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
   * Allocates no memory. 
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static int sigNum(int i) {
    return 0; // skeleton
  }
    
  /**
   * Does not allow "this" to escape local variables. Allocates a String and
   * associated internal "structure" (e.g. char[]) in caller's scope.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static String toBinaryString(int v) {
    return null; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables. Allocates a String and
   * associated internal "structure" (e.g. char[]) in caller's scope.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static String toHexString(int v) {
    return null; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables. Allocates a String and
   * associated internal "structure" (e.g. char[]) in caller's scope.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static String toOctalString(int v) {
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
  public static String toString(int v) {
    return null; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables. Allocates a String and
   * associated internal "structure" (e.g. char[]) in caller's scope.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static String toString(int v, int base) {
    return null; // skeleton
  }
  
  /**
   * Allocates an Integer in caller's scope.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static Integer valueOf(int val) {
    return null; // skeleton
  }
  
  /**
   * Does not allow "str" argument to escape local variables. Allocates an
   * Integer in caller's scope.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static Integer valueOf(String str) throws NumberFormatException {
    return null;
  }
  
  /**
   * Does not allow "str" argument to escape local variables. Allocates an
   * Integer in caller's scope.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static Integer valueOf(String str, int base)
    throws NumberFormatException {
    return null;
  }
  
  private int value;
}
