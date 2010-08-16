package javax.realtime;

import javax.safetycritical.annotate.BlockFree;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public final class ImmortalMemory extends MemoryArea
{
  @BlockFree
  @SCJAllowed
  public static ImmortalMemory instance()
  {
    return null; // dummy return
  }

  @BlockFree
  @SCJAllowed
  public void enter(Runnable logic)
  {
  }

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
