

package javax.safetycritical;


import javax.safetycritical.annotate.BlockFree;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public class PriorityScheduler extends
                         javax.realtime.PriorityScheduler {

  public static PriorityScheduler instance() { return null; }


  @BlockFree
  @SCJAllowed
  public int getMaxHardwarePriority() { return 2000; }


  @BlockFree
  @SCJAllowed
  public int getMinHardwarePriority() { return 1000; }
}
