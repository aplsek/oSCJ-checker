package javax.realtime;

public abstract class Timer extends AsyncEvent {

  protected Timer(HighResolutionTime time, Clock cclock,
                  AsyncEventHandler handler) {
  }
  
  public boolean isRunning() {
    return false; // dummy return
  }
  
  public void start() {
  }
  
  public void start(boolean disabled) {
  }
  
  public boolean stop() {
    return false; // dummy return
  }
  
  public ReleaseParameters createReleaseParameters() {
    return null; // dummy return
  }
  
  public void enable() {
  }
  
  public void disable() {
  }
  
  public void destroy() {
  }
  
  public void fire() throws UnsupportedOperationException {
  }
  
  public Clock getClock() {
    return null; // dummy return
  }
  
  public AbsoluteTime getFireTime() {
    return null; // dummy return
  }
  
  public void reschedule(HighResolutionTime time) {
  }
  
  void fireIt() {
  }
}
