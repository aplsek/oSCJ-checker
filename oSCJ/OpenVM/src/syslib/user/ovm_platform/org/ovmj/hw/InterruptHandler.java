
package org.ovmj.hw;

abstract public class InterruptHandler implements Runnable {

  int interruptIndex;
  
  protected InterruptHandler(int interruptIndex) {
  
    this.interruptIndex = interruptIndex;
    /* inflate the monitor to allow special synchronization in interrupt handlers */
    LibraryImports.createInterruptHandlerMonitor(this, interruptIndex);    
  }
  
  protected int getInterruptIndex() {
    return interruptIndex;
  }
  
  public void register() {
    IOFactory.getFactory().registerInterruptHandler( interruptIndex, this );      
  }
  
  protected void disableInterrupt() {
    InterruptController.disableInterrupt(interruptIndex);
  }
  
  protected void enableInterrupt() {
    InterruptController.enableInterrupt(interruptIndex);
  }
  
  protected void interruptServingStarted() {
    InterruptController.interruptServingStarted(interruptIndex);
  }
  
  protected void interruptServed() {
    InterruptController.interruptServed(interruptIndex);
  }
  
  public void unregister() {
    IOFactory.getFactory().deregisterInterruptHandler( interruptIndex );
  }
  
  abstract public void handle();
  
  public void run() {
    interruptServingStarted();
    handle();
    interruptServed();
  }
  
};
