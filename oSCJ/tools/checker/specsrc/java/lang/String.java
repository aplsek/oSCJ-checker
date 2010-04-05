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
import javax.safetycritical.annotate.MemoryAreaEncloses;
import javax.safetycritical.annotate.SCJAllowed;

import static javax.safetycritical.annotate.Allocate.Area.CURRENT;
import static javax.safetycritical.annotate.Allocate.Area.THIS;

@SCJAllowed
public final class String
  implements CharSequence, Comparable<String>, Serializable {

  /**
   * Does not allow "this" argument to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public String() { // skeleton
  }
  
  /**
   * Does not allow "this" or "b" argument to escape local variables.
   * Allocates internal structure to hold the contents of b within the
   * same scope as "this".
   */
  @Allocate({THIS})
  @BlockFree
  @SCJAllowed
  public String(byte[] b) { // skeleton
  }
  
  /**
   * Does not allow "this" or "s" argument to escape local variables.
   * Allocates internal structure to hold the contents of s within the
   * same scope as "this".
   */
  @Allocate({THIS})
  @BlockFree
  @SCJAllowed
  public String(String s) { // skeleton
  }
  
  /**
   * Does not allow "this" or "b" argument to escape local variables.
   * Allocates internal structure to hold the contents of b within the
   * same scope as "this".
   */
  @Allocate({THIS})
  @BlockFree
  @SCJAllowed
  public String(byte[] b, int offset, int length) { // skeleton
  }
  
  /**
   * Does not allow "this" or "c" argument to escape local variables.
   * Allocates internal structure to hold the contents of c within the
   * same scope as "this".
   */
  @Allocate({THIS})
  @BlockFree
  @SCJAllowed
  public String(char[] c) { // skeleton
  }
  
  /**
   * Allocates no memory.
   * <p>
   * Does not allow "this" to escape local variables.  Requires that argument
   * "b" reside in a scope that encloses the scope of "this". Builds a link
   * from "this" to the internal structure of argument b.
   * <p>
   * Note that the subset implementation of StringBuilder does not mutate
   * existing buffer contents.
   */
  @BlockFree
  @MemoryAreaEncloses(inner = {"this"}, outer = {"b"})
  @SCJAllowed
  public String(StringBuilder b) { // skeleton
  }
  
  /**
   * Does not allow "this" or "c" argument to escape local variables.
   * Allocates internal structure to hold the contents of c within the
   * same scope as "this".
   */
  @Allocate({THIS})
  @BlockFree
  @SCJAllowed
  public String(char[] c, int offset, int length) { // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public char charAt(int index) {
    return 0;
  }
  
  /**
   * Allocates no memory. Does not allow "this" or "str" argument to escape
   * local variables.
   */
  @BlockFree
  @SCJAllowed
  public int compareTo(String str) {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" or "str" argument to escape
   * local variables.
   */
  @BlockFree
  @SCJAllowed
  public int compareToIgnoreCase(String str) {
    return 0; // skeleton
  }
  
  /**
   * Does not allow "this" or "str" argument to escape local variables.
   * Allocates a String and internal structure to hold the catenation
   * result in the caller's scope.
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public String concat(String arg) {
    return null; // skeleton
  }

  /**
   * Does not allow "this" or "str" argument to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public boolean contains(CharSequence arg) {
    return false; // skeleton
  }

  /**
   * Does not allow "this" or "str" argument to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public boolean contentEquals(CharSequence cs) {
    return false; // skeleton
  }

  /**
   * Allocates no memory. Does not allow "this" or "suffix" argument to escape
   * local variables.
   */
  @BlockFree
  @SCJAllowed
  final public boolean endsWith(String suffix) {
    return false; // skeleton
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
   * Allocates no memory. Does not allow "this" or "str" argument to escape
   * local variables.
   */
  @BlockFree
  @SCJAllowed
  public boolean equalsIgnoreCase(String str) {
    return false; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables. Allocates a byte array
   * in the caller's context.
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public byte[] getBytes() {
    return null; // skeleton
  }

  /**
   * Allocates no memory. Does not allow "this" or "dst" argument to escape
   * local variables.
   */
  @BlockFree
  @SCJAllowed
  public void getChars(int src_begin, int src_end, char[] dst, int dst_begin)
  { // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public int hashCode() {
    return 0;
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public int indexOf(int ch) {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public int indexOf(int ch, int from_index) {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" or "str" argument to escape
   * local variables.
   */
  @BlockFree
  @SCJAllowed
  public int indexOf(String str) {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" or "str" argument to escape
   * local variables.
   */
  @BlockFree
  @SCJAllowed
  public int indexOf(String str, int from_index) {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" argument to escape
   * local variables.
   */
  @BlockFree
  @SCJAllowed
  public boolean isEmpty() {
    return false; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public int lastIndexOf(int ch) {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public int lastIndexOf(int ch, int from_index) {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" or "str" argument to escape
   * local variables.
   */
  @BlockFree
  @SCJAllowed
  public int lastIndexOf(String str) {
    return 0;
  }
  
  /**
   * Allocates no memory. Does not allow "this" or "str" argument to escape
   * local variables.
   */
  @BlockFree
  @SCJAllowed
  public int lastIndexOf(String str, int from_index) {
    return 0;
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed
  public int length() {
    return 0; // skeleton
  }


  /**
   * Allocates no memory. Does not allow "this" or "str" argument to escape
   * local variables.
   */
  @BlockFree
  @SCJAllowed
  public boolean regionMatches(boolean ignore_case, int myoffset, String str,
                               int offset, int len) {
    return false; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" or "str" argument to escape
   * local variables.
   */
  @BlockFree
  @SCJAllowed
  public boolean regionMatches(int myoffset, String str, int offset, int len) {
    return false; // skeleton
  }

  /**
   * Does not allow "this" argument to escape local variables.
   * Allocates a String and internal structure to hold the
   * result in the caller's scope.
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public String replace(char oldChar, char newChar)
  {
    return null;                // skeleton
  }

  /**
   * Does not allow "this", "target", or "replacement" arguments to
   * escape local variables. 
   * Allocates a String and internal structure to hold the
   * result in the caller's scope.
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public String replace(CharSequence target, CharSequence replacement)
  {
    return null;                // skeleton
  }

  /**
   * Allocates no memory. Does not allow "this" or "prefix" argument to escape
   * local variables.
   */
  @BlockFree
  @SCJAllowed
  final public boolean startsWith(String prefix) {
    return false; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" or "prefix" argument to escape
   * local variables.
   */
  @BlockFree
  @SCJAllowed
  final public boolean startsWith(String prefix, int toffset) {
    return false; // skeleton
  }

  /**
   * Allocates a String object in the caller's scope. Requires that "this"
   * reside in a scope that encloses the caller's scope, since the the
   * returned String retains a reference to the internal structure
   * of "this" String.
   */
  @Allocate({CURRENT})
  @BlockFree
  @MemoryAreaEncloses(inner = {"@result"}, outer = {"this"})
  @SCJAllowed
  public String subSequence(int start, int end) {
    return null; // skeleton
  }
  
  /**
   * Allocates a String object in the caller's scope. Requires that "this"
   * reside in a scope that encloses the caller's scope, since the the
   * returned String retains a reference to the internal structure
   * of "this" String.
   */
  @Allocate({CURRENT})
  @BlockFree
  @MemoryAreaEncloses(inner = {"@result"}, outer = {"this"})
  @SCJAllowed
  public String substring(int begin_index) {
    return null;
  }
  
  /**
   * Allocates a String object in the caller's scope. Requires that "this"
   * reside in a scope that encloses the caller's scope, since the the
   * returned String retains a reference to the internal structure 
   * of "this" String.
   */
  @Allocate({CURRENT})
  @BlockFree
  @MemoryAreaEncloses(inner = {"@result"}, outer = {"this"})
  @SCJAllowed
  public String substring(int begin_index, int end_index) {
    return null; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables. Allocates a char array
   * to hold the result of this method in the caller's scope.
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public char[] toCharArray() {
    return null; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables. Allocates a String and
   * internal structure to hold the result of this method in the
   * caller's scope. 
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public String toLowerCase() {
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
  @BlockFree
  @SCJAllowed
  public String toString() {
    return null;
  }
  
  /**
   * Does not allow "this" to escape local variables. Allocates a String and
   * internal structure to hold the result of this method in the
   * caller's scope. 
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public String toUpperCase() {
    return null; // skeleton
  }
  
  /**
   * Allocates a String object in the caller's scope. Requires that "this"
   * reside in a scope that encloses the caller's scope, since the 
   * returned String retains a reference to the internal structure 
   * of "this" String.
   */
  @Allocate({CURRENT})
  @BlockFree
  @MemoryAreaEncloses(inner = {"@result"}, outer = {"this"})
  @SCJAllowed
  final public String trim() {
    return null; // skeleton
  }
  
  /**
   * Allocates no memory. Returns a preallocated String residing in the scope
   * of the String class's ClassLoader.
   */
  @BlockFree
  @SCJAllowed
  public static String valueOf(boolean b) {
    return null; // skeleton
  }
  
  /**
   * Allocates a String and associated internal "structure" (e.g. char[]) in
   * caller's scope.
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public static String valueOf(char c) {
    return null; // skeleton
  }
  
  /**
   * Does not allow "data" argument to escape local variables. Allocates a
   * String and associated internal "structure" (e.g. char[]) in
   * caller's scope. 
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public static String valueOf(char[] data) {
    return null; // skeleton
  }
  
  /**
   * Does not allow "data" argument to escape local variables. Allocates a
   * String and associated internal "structure" (e.g. char[]) in
   * caller's scope. 
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public static String valueOf(char[] data, int offset, int count) {
    return null; // skeleton
  }
  
  /**
   * Allocates a String and associated internal "structure"
   * (e.g. char[]) in caller's scope.
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public static String valueOf(double d) {
    return null; // skeleton
  }
  
  /**
   * Allocates a String and associated internal "structure" (e.g. char[]) in
   * caller's scope.
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public static String valueOf(float f) {
    return null; // skeleton
  }
  
  /**
   * Allocates a String and associated internal "structure" (e.g. char[]) in
   * caller's scope.
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public static String valueOf(int i) {
    return null; // skeleton
  }
  
  /**
   * Allocates a String and associated internal "structure" (e.g. char[]) in
   * caller's scope.
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public static String valueOf(long l) {
    return null; // skeleton
  }
  
  /**
   * Allocates a String object in the caller's scope. 
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public static String valueOf(Object o) {
    return null; // skeleton
  }
}
