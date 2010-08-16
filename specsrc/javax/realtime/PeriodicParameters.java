package javax.realtime;

import javax.safetycritical.annotate.BlockFree;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_1;
import javax.safetycritical.annotate.MemoryAreaEncloses;

@SCJAllowed
public class PeriodicParameters extends ReleaseParameters {

  /**
   * @memory Does not allocate memory. Does not allow this to escape local variables.
   *         Builds links from this to start and period. Thus, start and period must
   *         reside in scopes that enclose this.
   * <p>
   * TBD: If this maintains references to start and period, then we really
   * should make sure that RelativeTime is immutable. Otherwise, we should
   * make internal copies of these parameters.  ****AJW NO -- THE COPY IS DONE
   * on creation of schedulable object
   */
  @MemoryAreaEncloses(inner = {"this", "this"},
                      outer = {"start", "period"})
  @SCJAllowed
  @BlockFree
  public PeriodicParameters(HighResolutionTime start,
                            RelativeTime period)
  { super(null,null,null,null); }


  /**
   * @return Returns the object originally passed in to the constructor, which is
   *         known to reside in a memory area that encloses this.
   */
  @BlockFree
  @SCJAllowed
  public HighResolutionTime getStart() {
    return null;
  }
  
 /**
   * @return  Returns the object originally passed in to the constructor, which is
   *          known to reside in a memory area that encloses this.
   */
  @BlockFree
  @SCJAllowed(LEVEL_1)
  public RelativeTime getPeriod() {
    return null;
  }
}
