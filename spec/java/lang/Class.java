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
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

import static javax.safetycritical.annotate.Allocate.Area.CURRENT;

@SCJAllowed
public final class Class<T> implements Serializable
{

  /**
   * This does not cause classes to be loaded. If a particular class has been
   * loaded, this returns a reference to the previously loaded class.
   * <p>
   * Allocates no memory. Does not allow the "name" argument to escape local
   * variables.
   */
  @SCJRestricted(maySelfSuspend = false)
  public static Class<?> forName(String name) throws ClassNotFoundException {
    return null; // skeleton
  }

  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public boolean desiredAssertionStatus() {
    return false; // skeleton
  }
  
  /**
   * Allocates no memory. Returns a reference to a previously existing Class,
   * which resides in the scope of its ClassLoader.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public Class<?> getDeclaringClass() {
    return null; // skeleton
  }
  
  /**
   * Does not alow "this" to escape local variables.
   * <p>
   * Allocates an array of T in the caller's scope. The allocated array holds
   * references to previously allocated T objects. Thus, the existing T
   * objects must reside in a scope that encloses the caller's scope. Note
   * that the existing T objects reside in the scope of the corresponding
   * ClassLoader.
   */
  @Allocate({CURRENT})
  @MemoryAreaEncloses(inner = {"@result"},
                      outer = {"this.getClass().getClassLoader()"})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public T[] getEnumConstants() {
    return null; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   * <p>
   * Returns a reference to a previously allocated Class object, which resides
   * in the scope of its ClassLoader.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public Class<?> getComponentType() {
    return null; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   * <p>
   * Returns a reference to a previously allocated String object, which
   * resides in the scope of this Class's ClassLoader or in some enclosing
   * scope.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public String getName() {
    return null; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   * <p>
   * Returns a reference to a previously allocated Class object, which resides
   * in the scope of its ClassLoader.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public Class<? super T> getSuperclass() {
    return null; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public boolean isArray() {
    return false; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" or argument "c" to escape
   * local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public boolean isAssignableFrom(Class c) {
    return false; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public boolean isEnum() {
    return false;
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public boolean isInstance(Object o) {
    return false; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public boolean isInterface() {
    return false; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public boolean isPrimitive() {
    return false; // skeleton
  }
  
  /**
   * Does not allow "this" to escape local variables. Allocates a single
   * instance of this Class within the caller's scope.
   * 
   * TBD: from where does IllegalAccessExceptiion come from?
   */
  @Allocate({CURRENT})
  @MemoryAreaEncloses(inner = {"@result"}, 
                      outer = {"this.getClass().getClassLoader()"})
  @SCJRestricted(maySelfSuspend = false)
  public T newInstance()
  throws InstantiationException
  {
    return null; // skelton
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
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public boolean isAnnotation() {
    return false; // skeleton
  }
  
}
