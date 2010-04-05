package org.ovmj.hw;

public class IOFactory {

  private SerialPort sp1;
  private DelayPort dp;
  
  // package access only
  
  IOFactory() {
    sp1 = createSerialPort(0x3f8);
    dp = createDelayPort(0x80);
  }
  
  private static SerialPort createSerialPort( int baseAddress ) throws PragmaNoHWIORegistersAccess {
  
    SerialPort sp = new SerialPort();
    
    // aliases
    sp.data = baseAddress;
    sp.status = baseAddress + 0x5;
    
    // names by spec
    
    sp.rbr = baseAddress;
    sp.thr = baseAddress;
    sp.dll = baseAddress;
    sp.ier = baseAddress + 0x1;
    sp.dlm = baseAddress + 0x1;
    sp.fcr = baseAddress + 0x2;
    sp.iir = baseAddress + 0x2;
    sp.lcr = baseAddress + 0x3;
    sp.mcr = baseAddress + 0x4;
    sp.lsr = baseAddress + 0x5;
    sp.msr = baseAddress + 0x6;

    return sp;  
  }
  
  private static DelayPort createDelayPort( int baseAddress) throws PragmaNoHWIORegistersAccess {
  
    DelayPort dp = new DelayPort();
    dp.dummy = baseAddress;
    
    return dp;
  }
  
  private static IOFactory single = new IOFactory();
  
  public static IOFactory getFactory() {
    return single;
  }
  
  private class IRQServer implements Runnable {
  
    int nr;
    Runnable handler;
    
    IRQServer(Runnable handler, int nr) {
      this.handler = handler;
      this.nr = nr;
    }
    
    public void run() {

      for(;;) {
      
        boolean res = InterruptController.waitForInterrupt(nr);

        if (!res) {

          if (InterruptController.isMonitoredInterrupt(nr)) {
            throw new RuntimeException("Interrupt handler for interrupt "+nr+" is already installed.\n");
          }
          /* someone called stopMonitoringInterrupt */
          return ;
        }
        
        
        handler.run();        

      }
    }
  } 
  
  public SerialPort getSerialPort() {
    return sp1;
  }
  
  public DelayPort getDelayPort() {
    return dp;
  }

  // when this call returns, the interrupt must already
  // being monitored and directed to the handler
  
  public void registerInterruptHandler(int nr, Runnable logic) {

    if (!InterruptController.startMonitoringInterrupt(nr)) {
      System.err.println("WARNING: Interrupt "+nr+" was already being monitored.");
    }
    // now we know that the OVM runtime is already monitoring incomming
    // interrupt nr
    
    new Thread(new IRQServer(logic,nr),"handler for interrupt "+nr).start();
    
  }
  
  public void deregisterInterruptHandler(int nr) {
    InterruptController.stopMonitoringInterrupt(nr);
    // the shutdown of the handler thread continues asynchronously
  }
  
}
  