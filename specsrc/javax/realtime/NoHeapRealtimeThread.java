package javax.realtime;

import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJProtected;
import javax.safetycritical.annotate.BlockFree;

import static javax.safetycritical.annotate.Level.LEVEL_2;


@SCJAllowed(LEVEL_2)
public class NoHeapRealtimeThread extends RealtimeThread
{
  /**
   * TBD: do we use this constructor, which expects a MemoryArea argument?
   */
  @SCJAllowed(LEVEL_2)
  public NoHeapRealtimeThread(SchedulingParameters schedule, MemoryArea area)
  {
    // super(schedule, null, null, area, null, null);
  }

  /**
   * TBD: do we use this constructor, which expects a
   * ReleaseParameters argument? 
   */
  @SCJAllowed(LEVEL_2)
  public NoHeapRealtimeThread(SchedulingParameters schedule,
                              ReleaseParameters release)
  {
      // super(schedule, release, null, null, null, null);
  }

  /**
   * Creation of thread may block, but starting shall not
   */
  @SCJAllowed(LEVEL_2) 
  @BlockFree
  public void start()
  {
  }
}
