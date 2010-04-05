package javax.realtime;

import javax.safetycritical.annotate.SCJProtected;

public class PeriodicTimer extends Timer
{
  @SCJProtected
  public PeriodicTimer(HighResolutionTime start,
                       RelativeTime interval,
                       Clock cclock,
                       AsyncEventHandler handler)
  {
    super(start, cclock, handler);
    // TODO Auto-generated constructor stub
  }

  @SCJProtected
  public PeriodicTimer(HighResolutionTime start,
                       RelativeTime interval,
                       AsyncEventHandler handler)
  {
    this(start, interval, Clock.getRealtimeClock(), handler);
    // TODO Auto-generated constructor stub
  }
}
