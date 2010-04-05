package javax.realtime;

import javax.safetycritical.annotate.BlockFree;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJProtected;

import static javax.safetycritical.annotate.Level.LEVEL_1;

@SCJAllowed
public class LTMemory extends ScopedMemory
{
  /**
   * This is not SCJAllowed because we don't want to instantiate
   * these.  Safety-critical Java developers should instantiate
   * SafetyMemory instead.
   */
  @SCJProtected
  public LTMemory(long size) { super(size); }

  @SCJProtected
  public LTMemory(SizeEstimator estimator) { super(estimator); }

  /**
   * In vanilla RTSJ, enter() is not necessarily block-free because
   * entering an LTMemory region may have to wait for the region to be
   * finalized.  However, a compliant implementation of JSR 302 shall
   * provide a block-free implementation of enter.  Note that JSR 302
   * specifies that finalization of LTMemory regions is not performed.
   */
  @BlockFree
  // ALes: @SCJAllowed(LEVEL_1)
  @SCJAllowed
  public void enter(Runnable logic) {}

  @BlockFree
  @SCJAllowed
  public long memoryConsumed()
  {
    return 0L; // dummy return
  }

  @BlockFree
  @SCJAllowed
  public long memoryRemaining()
  {
    return 0L; // dummy return
  }

  @BlockFree
  @SCJAllowed
  public long size()
  {
    return 0L; // dummy return
  }
}
