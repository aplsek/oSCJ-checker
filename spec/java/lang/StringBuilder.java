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
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

import static javax.safetycritical.annotate.Allocate.Area.CURRENT;
import static javax.safetycritical.annotate.Allocate.Area.THIS;

@SCJAllowed
public final class StringBuilder
   implements Appendable, CharSequence, Serializable {

  /**
   * Does not allow "this" to escape local variables. Allocates
   * internal structure of sufficient size to represent 16 characters
   * in the scope of "this".
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public StringBuilder() { // skeleton
  }

  /**
   * Does not allow "this" to escape local variables. Allocates
   * internal structure of sufficient size to represent 
   * length characters within the scope of "this".
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public StringBuilder(int length) { // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables. Allocates a character
   * internal structure of sufficient size to represent str.length() + 16
   * characters within the scope of "this".
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public StringBuilder(String str) { // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables. Allocates a character
   * internal structure of sufficient size to represent seq.length() + 16
   * characters within the scope of "this".
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public StringBuilder(CharSequence seq) { // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables. If expansion of "this"
   * StringBuilder's internal character buffer is necessary, a new char array
   * is allocated within the scope of "this". The new array will be twice the
   * length of the existing array, plus 1.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public StringBuilder append(boolean b) {
    return this; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables. If expansion of "this"
   * StringBuilder's internal character buffer is necessary, a new char array
   * is allocated within the scope of "this". The new array will be twice the
   * length of the existing array, plus 1.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public StringBuilder append(char c) {
    return this; // skeleton
  }

  /**
   * Does not allow "this" or "buf" to escape local variables. If
   * expansion of "this" 
   * StringBuilder's internal character buffer is necessary, a new char array
   * is allocated within the scope of "this". The new array will be twice the
   * length of the existing array, plus 1.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public StringBuilder append(char[] buf) {
    return this; // skeleton
  }

  /**
   * Does not allow "this" or "buf" to escape local variables. If
   * expansion of "this" 
   * StringBuilder's internal character buffer is necessary, a new char array
   * is allocated within the scope of "this". The new array will be twice the
   * length of the existing array, plus 1.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public StringBuilder append(char[] buf, int offset, int length) {
    return this; // skeleton
  }

  /**
   * Does not allow "this" or argument "cs" to escape local variables. If
   * expansion of "this" StringBuilder's internal character buffer is
   * necessary, a new char array is allocated within the scope of "this". The
   * new array will be twice the length of the existing array, plus 1.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public StringBuilder append(CharSequence cs) {
    return this; // skeleton
  }
  
  /**
   * Does not allow "this" or argument "cs" to escape local variables. If
   * expansion of "this" StringBuilder's internal character buffer is
   * necessary, a new char array is allocated within the scope of "this". The
   * new array will be twice the length of the existing array, plus 1.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public StringBuilder append(CharSequence cs, int start, int end) {
    return this; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables. If expansion of "this"
   * StringBuilder's internal character buffer is necessary, a new char array
   * is allocated within the scope of "this". The new array will be twice the
   * length of the existing array, plus 1.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public StringBuilder append(double d) {
    return this; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables. If expansion of "this"
   * StringBuilder's internal character buffer is necessary, a new char array
   * is allocated within the scope of "this". The new array will be twice the
   * length of the existing array, plus 1.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public StringBuilder append(float f) {
    return this; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables. If expansion of "this"
   * StringBuilder's internal character buffer is necessary, a new char array
   * is allocated within the scope of "this". The new array will be twice the
   * length of the existing array, plus 1.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public StringBuilder append(int i) {
    return this; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables. If expansion of "this"
   * StringBuilder's internal character buffer is necessary, a new char array
   * is allocated within the scope of "this". The new array will be twice the
   * length of the existing array, plus 1.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public StringBuilder append(long l) {
    return this; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables. If expansion of "this"
   * StringBuilder's internal character buffer is necessary, a new char array
   * is allocated within the scope of "this". The new array will be twice the
   * length of the existing array, plus 1.
   * <p>
   * Requires that argument "o" reside in a scope that encloses "this"
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public StringBuilder append(Object o) {
    return this; // skeleton
  }

  /**
   * Does not allow "this" or argument "s" to escape local variables. If
   * expansion of "this" StringBuilder's internal character buffer is
   * necessary, a new char array is allocated within the scope of "this". The
   * new array will be twice the length of the existing array, plus 1.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  @RunsIn(CALLER)
  public StringBuilder append(@Scope(UNKNOWN)String str) {
    return this; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public int capacity() {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public char charAt(int index) {
    return (char) 0; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables. If expansion of "this"
   * StringBuilder's internal character buffer is necessary, a new char array
   * is allocated within the scope of "this". The new array will be twice the
   * length of the existing array, plus 1.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public void ensureCapacity(int minimum_capacity) { // skeleton
  }

  /**
   * Does not allow "this" or "dst" to escape local variables. 
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin)
  {
  }

  /**
   * Does not allow "this" or "dst" to escape local variables. 
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public void indexOf(String str)
  {
  }

  /**
   * Does not allow "this" or "dst" to escape local variables. 
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public void indexOf(String str, int fromIndex)
  {
  }

  /**
   * Does not allow "this" or "dst" to escape local variables. 
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public void lastIndexOf(String str)
  {
  }

  /**
   * Does not allow "this" or "dst" to escape local variables. 
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public void lastIndexOf(String str, int fromIndex)
  {
  }

  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @Allocate({THIS})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public int length() {
    return 0; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public void setLength(int new_length) throws IndexOutOfBoundsException
  {
  }

  /**
   * Does not allow "this" to escape local variables. Allocates a String in
   * caller's scope. 
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public CharSequence subSequence(int start, int end) {
    return null; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables. Allocates a String in
   * caller's scope. 
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = true)
  public String toString() {
    return null;
  }
  
}
