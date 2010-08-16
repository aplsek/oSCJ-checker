package javax.realtime;

import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.BlockFree;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJProtected;

import static javax.safetycritical.annotate.Level.LEVEL_0;


@SCJAllowed(LEVEL_0)
public class AsyncEventHandler implements Schedulable
{
  /*
   * Not @SCJAllowed
   *
  public AsyncEventHandler(SchedulingParameters scheduling,
               ReleaseParameters release,
               MemoryParameters memory,
               MemoryArea area,
               ProcessingGroupParameters group,
                           boolean noheap,
               Runnable logic)
  {
  }
  */

  // not scj allowed
  public MemoryParameters getMemoryParameters() { return null; }

  /**
   * @return A reference to the associated ReleaseParameter object.
   */
  //@BlockFree
  //@SCJAllowed
  public ReleaseParameters getReleaseParameters() { return null; }

  /**
   *  @return A reference to the associated SchedulingParameter object.
   */
  //@SCJAllowed
  public SchedulingParameters getSchedulingParameters() { return null; }
  
 /**
   * Infrastructure code.
   * Must not be called.
   */
  @SCJProtected
  public final void run() {}

  @SCJAllowed(LEVEL_0)
  public void handleAsyncEvent() {}

  public final void setDaemon(boolean on) {}
}
