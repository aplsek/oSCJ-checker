
package org.ovmj.hw;

public class InterruptController {

  public static final boolean waitForInterrupt(int interruptIndex ) {
    return LibraryImports.waitForInterrupt(interruptIndex);
  }

  public static final void interruptServed( int interruptIndex ) {
    LibraryImports.interruptServed(interruptIndex);
  }
  
  public static final void interruptServingStarted( int interruptIndex ) {
    LibraryImports.interruptServingStarted(interruptIndex);
  }
  
  public static final void disableLocalInterrupts() {
    LibraryImports.disableLocalInterrupts();
  }
  
  public static final void enableLocalInterrupts() {
    LibraryImports.enableLocalInterrupts();
  }
  
  public static final void disableInterrupt(int interruptIndex) {
    LibraryImports.disableInterrupt(interruptIndex);
  }
  
  public static final void enableInterrupt(int interruptIndex) {
    LibraryImports.enableInterrupt(interruptIndex);
  }

  public static final boolean isMonitoredInterrupt( int interruptIndex ) {
    return LibraryImports.isMonitoredInterrupt(interruptIndex);
  }
  
  public static final boolean stopMonitoringInterrupt( int interruptIndex ) {
    return LibraryImports.stopMonitoringInterrupt(interruptIndex);
  }
  
  public static final boolean startMonitoringInterrupt( int interruptIndex ) {
    return LibraryImports.startMonitoringInterrupt(interruptIndex);
  }

  public static final int getMaxInterruptIndex() {
    return LibraryImports.getMaxInterruptIndex();
  }
  
  public static final void print_int( int value ) {
    LibraryImports.print_int(value);
  }
}
