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
public class Boolean implements Comparable<Boolean>, Serializable
{
  // Dummy values to passify the compiler -jv
  @SCJAllowed
  public static final Class<Boolean> TYPE = null; 
  @SCJAllowed
  public static final Boolean FALSE  = null;
  @SCJAllowed
  public static final Boolean TRUE = null;

  /**
   * Allocates no memory.  Does not allow "this" to escape local
   * variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public Boolean(final boolean v)
  {
  }

  /**
   * Allocates no memory.  Does not allow "this" or "str" argument to
   * escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public Boolean(final String str)
  {
  }

  /**
   * Allocates no memory.  Does not allow "this" to escape local
   * variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public boolean booleanValue()
  {
    return false;               // skeleton
  }

  /**
   * Allocates no memory.  Does not allow "this" or argument "b" to
   * escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public int compareTo(final Boolean b)
  {
    return 0;                   // skeleton
  }

  /**
   * Allocates no memory.  Does not allow "this" or argument "obj" to
   * escape local variables.
   */
  @Override
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public boolean equals(final Object obj)
  {
    return false;               // skeleton
  }

  /**
   * Allocates no memory.  Does not allow argument "str" to escape
   * local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static boolean getBoolean(final String str)
  {
    return false;               // skeleton
  }


  /**
   * Allocates no memory.  Does not allow "this" to escape local
   * variables.
   */
  @Override
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public int hashCode()
  {
    return 0;                   // skeleeton
  }

  /**
   * Allocates no memory.  Does not allow argument "str" to escape
   * local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static boolean parseBoolean(final String str)
  {
    return false;               // skeleton
  }

  /**
   * Does not allow "this" to escape local variables.  Allocates a
   * String and associated internal "structure" (e.g. char[]) in caller's
   * scope.  (Note: this semantics is desired for consistency with
   * overridden implementation of Object.toString()).
   */
  @Allocate({CURRENT})
  @Override
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public String toString()
  {
    return null;                // skeleton
  }

  /**
   * Allocates no memory.  Returns a String literal which resides at
   * the scope of the Classloader that is responsible for loading the
   * Boolean class.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static String toString(final boolean value)
  {
    return null;                // skeleton
  }

  /**
   * Allocates no memory.  Does not allow argument "str" to escale
   * local variables. Returns a Boolean literal which resides at
   * the scope of the Classloader that is responsible for loading the
   * Boolean class.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static Boolean valueOf(final String str)
  {
    return null;                // skeleton
  }

  /**
   * Allocates no memory.  Returns a Boolean literal which resides at
   * the scope of the Classloader that is responsible for loading the
   * Boolean class.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static Boolean valueOf(final boolean b)
  {
    return null;
  }
}

