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
public class Byte extends Number implements Comparable<Byte>, Serializable 
{

  @SCJAllowed
  public static final Class<Byte> TYPE = null; // Dummy value to passify
  // the compiler
  @SCJAllowed
  public static final byte MAX_VALUE = 0;
  @SCJAllowed
  public static final byte MIN_VALUE = 0;
  @SCJAllowed
  public static final int SIZE = 8;
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public Byte(byte val) {
  }
  
  /**
   * Allocates no memory. Does not allow "this" or "str" argument to escape
   * local variables.
   */
  @BlockFree
  @SCJAllowed
  public Byte(String str) throws NumberFormatException {
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public byte byteValue() {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" or "other" argument to escape
   * local variables.
   */
  @BlockFree
  @SCJAllowed
  public int compareTo(Byte other) {
    return 0; // skeleton
  }
  
  /**
   * Does not allow "str" argument to escape local variables. Allocates a Byte
   * result object in the caller's scope.
   */
  @BlockFree
  @SCJAllowed
  public static Byte decode(String str) throws NumberFormatException {
    return null; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public double doubleValue() {
    return 0; // skeleton
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
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public float floatValue() {
    return 0; // skeleton
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
  public long longValue() {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "str" argument to escape local
   * variables.
   */
  @BlockFree
  @SCJAllowed
  public static byte parseByte(String str) throws NumberFormatException {
    return (byte) 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "str" argument to escape local
   * variables.
   */
  @BlockFree
  @SCJAllowed
  public static byte parseByte(String str, int base)
    throws NumberFormatException {
    return (byte) 0; // skeleton
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
  public static String toString(byte v) {
    return null; // skeleton
  }
  
  /**
   * Does not allow "str" argument to escape local variables. Allocates one
   * Byte object in the caller's scope.
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public static Byte valueOf(String str) throws NumberFormatException {
    return null; // skeleton
  }
  
  /**
   * Allocates one Byte object in the caller's scope.
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public static Byte valueOf(byte val) {
    return null;
  }
  
  /**
   * Does not allow "str" argument to escape local variables. Allocates one
   * Byte object in the caller's scope.
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public static Byte valueOf(String str, int base)
    throws NumberFormatException {
    return null;
  }
}
