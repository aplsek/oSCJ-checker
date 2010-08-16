package javax.safetycritical;


import javax.realtime.InaccessibleAreaException;
import javax.realtime.LTMemory;
import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.BlockFree;
import javax.safetycritical.annotate.MemoryAreaEncloses;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public class PrivateMemory extends LTMemory implements ManagedMemory
{
  @SCJAllowed
  public PrivateMemory(long size) { super(size); }

  /**
   * @return the manager for this memory area.
   */
  @SCJAllowed
  public MissionManager getManager() { return null; }

  @SCJAllowed
  public void enter(Runnable logic) {}
}
