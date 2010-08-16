package javax.realtime;

import javax.safetycritical.annotate.BlockFree;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJProtected;
import static javax.safetycritical.annotate.Level.LEVEL_0;
import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.LEVEL_2;

@SCJAllowed(LEVEL_0)
public abstract class ReleaseParameters {

  @SCJProtected
  @SCJAllowed
  protected ReleaseParameters(RelativeTime cost, RelativeTime deadline,
                              AsyncEventHandler overrunHandler,
                              AsyncEventHandler missHandler)
  { }

  /**
   * TBD: whether SCJ makes any use of deadlines or tries to detect
   * deadline overruns.
   * <p>
   * No allocation because RelativeTime is immutable.
   */ 
  @BlockFree
  public RelativeTime getDeadline() {
    return null;
  }
}
