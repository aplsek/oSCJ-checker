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

import javax.safetycritical.annotate.BlockFree;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public final class Math {

  @SCJAllowed
  public static final double E = 0;
  @SCJAllowed
  public static final double PI = 0;
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static int abs(int a) {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static long abs(long a) {
    return (long) 0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static float abs(float a) {
    return (float) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double abs(double a) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double acos(double a) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double asin(double a) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double atan(double a) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double atan2(double a, double b) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double cbrt(double a) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double ceil(double a) {
    return (double) 0.0;
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double copySign(double magnitude, double sign) {
    return (double) 0.0;
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double copySign(float magnitude, float sign) {
    return (double) 0.0;
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double cos(double a) {
    return (double) 0.0;
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double cosh(double a) {
    return (double) 0.0;
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double exp(double a) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double expm1(double a) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double floor(double a) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static int getExponent(double a) {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static int getExponent(float a) {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double hypot(double x, double y) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double IEEEremainder(double f1, double f2) {
    return (double) 0.0;
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double log(double a) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double log10(double a) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double log1p(double a) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static int max(int a, int b) {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static long max(long a, long b) {
    return (long) 0;
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static float max(float a, float b) {
    return (float) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double max(double a, double b) {
    return 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static int min(int a, int b) {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static long min(long a, long b) {
    return (long) 0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static float min(float a, float b) {
    return (float) 0.0;
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double min(double a, double b) {
    return 0.0;
  }

  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double nextAfter(double start, double direction) {
    return 0.0;
  }

  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static float nextAfter(float start, float direction) {
    return 0.0F;
  }

  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double nextUp(double d) {
    return 0.0;
  }

  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static float nextUp(float d) {
    return 0.0F;
  }

  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double pow(double a, double b) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double random() {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double rint(double a) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static int round(float a) {
    return 0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static long round(double a) {
    return (long) 0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double scalb(double d, int scaleFactor) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static float scalb(float f, int scaleFactor) {
    return 0.0F; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double signum(double d) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static float signum(float f) {
    return 0.0F; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double sin(double a) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double sinh(double a) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double sqrt(double a) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double tan(double a) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double tanh(double a) {
    return (double) 0.0; // skeleton
  }

  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double toDegrees(double val) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double toRadians(double val) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static double ulp(double d) {
    return (double) 0.0; // skeleton
  }
  
  /**
   * Allocates no memory.
   */
  @BlockFree
  @SCJAllowed
  public static float ulp(float d) {
    return 0.0F; // skeleton
  }
  
}

