
package javax.realtime;

/*
Inspired by RTSJ's POSIXSignalHandler. The differences are:

 * only one handler per interrupt is supported, thus, AsyncEvent object is
   not needed 
   
 * the VM interrupt monitor is used to wait for individual interrupts as
   opposed to a set of interrupts ; this requires more threads, but makes
   the implementation simpler and most likely faster (see
   InterruptMonitor.java vs. SignalMonitor.java)  ; also, reception of
   multiple interrupts at a time is not expected (once, when run on real
   hardware, we should still be running with local interrupts disabled
   
 * the handler must do end of interrupt call (LibraryImports.interruptServed())
*/   
   
public class HardwareInterruptHandler {

  private static class IRQServer implements Runnable {
  
    int interrupt;
    AsyncEventHandler handler;
    
    IRQServer(AsyncEventHandler handler, int interrupt) {
      this.handler = handler;
      this.interrupt = interrupt;
    }
    
    public void run() {

      for(;;) {
      
        boolean res = LibraryImports.waitForInterrupt(interrupt);
        if (!res) {
          throw new RuntimeException("Someone is already waiting for the interrupt.");
        }
        
        if (!LibraryImports.isMonitoredInterrupt(interrupt)) {
          // IRQ server is shutting down
          return;
        }
        
        handler.releaseHandler();
      }
    }
  } 
  

  public static void setHandler(int interrupt, AsyncEventHandler handler) {

    if (!LibraryImports.startMonitoringInterrupt(interrupt)) {
      throw new RuntimeException("Interrupt is already being monitored.");
    }
    
    new Thread(new IRQServer(handler,interrupt),"RTSJ handler for interrupt "+interrupt).start();
  }
  
  public static void removeHandler(int interrupt) {
    LibraryImports.stopMonitoringInterrupt( interrupt );
  }
  
}
  