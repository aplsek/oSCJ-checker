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
public final class Character implements Comparable<Character>, Serializable 
{
  @SCJAllowed
  public static final int MIN_RADIX = 0; // Dummy values to pacify the
  // compiler -jv
  @SCJAllowed
  public static final int MAX_RADIX = 0;
  @SCJAllowed
  public static final char MIN_VALUE = 0;
  @SCJAllowed
  public static final char MAX_VALUE = 0;
  @SCJAllowed
  public static final Class<Character> TYPE = null;

  @SCJAllowed
  public static final int SIZE = 16;

  @SCJAllowed
  public static final byte UNASSIGNED = 0;
  @SCJAllowed
  public static final byte UPPERCASE_LETTER = 0;
  @SCJAllowed
  public static final byte LOWERCASE_LETTER = 0;
  @SCJAllowed
  public static final byte TITLECASE_LETTER = 0;
  @SCJAllowed
  public static final byte MODIFIER_LETTER = 0;
  @SCJAllowed
  public static final byte OTHER_LETTER = 0;
  @SCJAllowed
  public static final byte NON_SPACING_MARK = 0;
  @SCJAllowed
  public static final byte ENCLOSING_MARK = 0;
  @SCJAllowed
  public static final byte COMBINING_SPACING_MARK = 0;
  @SCJAllowed
  public static final byte DECIMAL_DIGIT_NUMBER = 0;
  @SCJAllowed
  public static final byte LETTER_NUMBER = 0;
  @SCJAllowed
  public static final byte OTHER_NUMBER = 0;
  @SCJAllowed
  public static final byte SPACE_SEPARATOR = 0;
  @SCJAllowed
  public static final byte LINE_SEPARATOR = 0;
  @SCJAllowed
  public static final byte PARAGRAPH_SEPARATOR = 0;
  @SCJAllowed
  public static final byte CONTROL = 0;
  @SCJAllowed
  public static final byte FORMAT = 0;
  @SCJAllowed
  public static final byte PRIVATE_USE = 0;
  @SCJAllowed
  public static final byte SURROGATE = 0;
  @SCJAllowed
  public static final byte DASH_PUNCTUATION = 0;
  @SCJAllowed
  public static final byte START_PUNCTUATION = 0;
  @SCJAllowed
  public static final byte END_PUNCTUATION = 0;
  @SCJAllowed
  public static final byte CONNECTOR_PUNCTUATION = 0;
  @SCJAllowed
  public static final byte OTHER_PUNCTUATION = 0;
  @SCJAllowed
  public static final byte MATH_SYMBOL = 0;
  @SCJAllowed
  public static final byte CURRENCY_SYMBOL = 0;
  @SCJAllowed
  public static final byte MODIFIER_SYMBOL = 0;
  @SCJAllowed
  public static final byte OTHER_SYMBOL = 0;
  @SCJAllowed
  public static final byte INITIAL_QUOTE_PUNCTUATION = 0;
  @SCJAllowed
  public static final byte FINAL_QUOTE_PUNCTUATION = 0;
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public Character(char v) {
  }
  
  /**
   * Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public char charValue() {
    return (char) 0; // skeleton
  }
  
  /**
   * Allocates no memory. Does not allow "this" or "another_character"
   * argument to escape local variables.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public int compareTo(Character another_character) {
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

  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public int hashCode()
  {
    return 0;                   // skeleton
  }

  /**
   * Allocates no memory.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static int getType(char ch) {
    return 0; // skeleton
  }

  /**
   * Allocates no memory.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static int digit(char ch, int radix) {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static boolean isLetter(char ch) {
    return false; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static boolean isLetterOrDigit(char ch) {
    return false; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static boolean isLowerCase(char ch) {
    return false; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static boolean isSpaceChar(char ch) {
    return false; // skeleton
  }

  /**
   * Allocates no memory.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static boolean isUpperCase(char ch) {
    return false; // skeleton
  }

  /**
   * Allocates no memory.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static boolean isWhitespace(char ch) {
    return false; // skeleton
  }

  /**
   * Allocates no memory.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static char toLowerCase(char ch) {
    return (char) 0; // skeleton
  }

  /**
   * Allocates no memory.
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static char toUpperCase(char ch) {
    return (char) 0; // skeleton
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
   * Allocates a String and associated internal "structure"
   * (e.g. char[]) in caller's scope.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static String toString(char c) {
    return null;
  }

  /**
   * Allocates a Character object in caller's scope.
   */
  @Allocate({CURRENT})
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public static Character valueOf(char c) {
    return null; // skeleton
  }
}






