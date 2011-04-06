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
import javax.safetycritical.annotate.MemoryAreaEncloses;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Allocate.Area.CURRENT;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.THIS;

@SCJAllowed
@Scope(IMMORTAL)
public abstract class Enum<E extends Enum<E>>
   implements Comparable<E>, Serializable
{

  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   * Requires that "name" argument reside in a scope that enclosees the scope
   * of "this".
   */
  @MemoryAreaEncloses(inner = {"this"}, outer = {"name"})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  protected Enum(String name, int ordinal) {
  }

  /**
   * Allocates no memory. Does not allow "this" or "o" argument to escape
   * local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  @RunsIn(CALLER)
  public final boolean equals(Object o) {
    return false; // skeleton
  }


  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  @RunsIn(CALLER)
  public final int hashCode() {
    return 0; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables. Allocates a String and
   * associated internal "structure" (e.g. char[]) in caller's
   * scope. (Note: this semantics is desired for consistency with
   * overridden implementation of Object.toString()).
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  @RunsIn(CALLER)
  @Scope(THIS)
  public String toString() {
    return null; // skeleton
  }

  /**
   * Allocates no memory. Does not allow "this" or "o" argument to escape
   * local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  @RunsIn(CALLER)
  public final int compareTo(E o) {
    return 0; // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  @RunsIn(CALLER)
  @Scope(THIS)
  protected final Object clone() throws CloneNotSupportedException {
    return null; // skeleton
  }

  /**
   * Allocates no memory. Returns a reference to this enumeration constant's
   * previously allocated String name. The String resides in the corresponding
   * ClassLoader scope.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  @Scope(IMMORTAL)
  @RunsIn(CALLER)
  public final String name() {
    return null; // skeleton
  }

  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  @RunsIn(CALLER)
  public final int ordinal() {
    return 0; // skeleton
  }

  /**
   * Allocates no memory. Returns a reference to a previously allocated Class,
   * which resides in its ClassLoader scope.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public final Class<E> getDeclaringClass() {
    return null; // skeleton
  }

  /**
   * Allocates no memory. Does not allow "enumType" or "name" arguments to
   * escape local variables. Returns a reference to a previsouly allocated
   * enumeration constant, residing within its ClassLoader scope.
   */
  @SCJRestricted(maySelfSuspend = false)
  @RunsIn(CALLER)
  public static <T extends Enum<T>> T valueOf(Class<T> enumType, String name) {
    return null; // skeleton
  }
}
