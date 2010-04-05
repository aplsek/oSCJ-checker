
package org.ovmj.hw;

public class NativeHelper {

  public static final long getTimeStamp() {
    return LibraryImports.getTimeStamp();
  }
  
  public static final void ovm_outb( int value, int address ) {
    LibraryImports.ovm_outb( value, address );
  }

  public static final int ovm_inb( int address ) {
    return LibraryImports.ovm_inb( address );
  }

}