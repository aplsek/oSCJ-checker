package javax.realtime;

import javax.safetycritical.annotate.BlockFree;
import javax.safetycritical.annotate.SCJAllowed;

/**
 * TBD: we need additional methods to allow SizeEstimation of thread stacks.
 * In particular, we need to be able to reserve memory for backing
 * store.  Perhaps this belongs in a javax.safetycritical variant of
 * SizeEstimator. 
 */
@SCJAllowed
public final class SizeEstimator {

  @BlockFree
  @SCJAllowed
  public SizeEstimator() {
  }

  /**
   * JSR 302 tightens the semantic requirements on the implementation
   * of getEstimate.  For compliance with JSR 302, getEstimate() must
   * return a conservative upper bound on the amount of memory
   * required to represent all of the memory reservations associated
   * with this SizeEstimator object.
   */
  @BlockFree
  @SCJAllowed
  public long getEstimate() {
    return 0;
  }

  @BlockFree
  @SCJAllowed
  public void reserve(Class clazz, int num) {
  }

  @BlockFree
  @SCJAllowed
  public void reserve(SizeEstimator size) {
  }

  @BlockFree
  @SCJAllowed
  public void reserve(SizeEstimator size, int num) {
  }

  @BlockFree
  @SCJAllowed
  public void reserveArray(int length) {
  }

  @BlockFree
  @SCJAllowed
  public void reserveArray(int length, Class type) {
  }
}
