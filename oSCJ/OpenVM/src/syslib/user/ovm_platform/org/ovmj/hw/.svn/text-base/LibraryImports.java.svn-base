
package org.ovmj.hw;

class LibraryImports {

  static final native boolean waitForInterrupt(int interruptIndex );

  static final native void interruptServed( int interruptIndex );
  static final native void interruptServingStarted( int interruptIndex );
  
  static native void disableLocalInterrupts();
  static native void enableLocalInterrupts();
  static native void disableInterrupt(int interruptIndex);
  static native void enableInterrupt(int interruptIndex);

  static final native boolean isMonitoredInterrupt( int interruptIndex );
  static final native boolean stopMonitoringInterrupt( int interruptIndex );
  static final native boolean startMonitoringInterrupt( int interruptIndex );

  static final native long getTimeStamp();
  static final native void ovm_outb( int value, int address );
  static final native int ovm_inb( int address );
  static final native void print_int( int value );

  static final native void createInterruptHandlerMonitor(Object o, int interruptIndex);
  static final native int getMaxInterruptIndex();  
}
