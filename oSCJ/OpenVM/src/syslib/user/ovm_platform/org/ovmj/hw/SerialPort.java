
package org.ovmj.hw;

public final class SerialPort extends HardwareObject {

  // LSR (Line Status Register)
  
  public static final int MASK_DATA_READY = 1;
  public static final int MASK_OVERRUN_ERROR = 1 << 1 ;
  public static final int MASK_PARITY_ERROR = 1 << 2 ;
  public static final int MASK_FRAMING_ERROR = 1 << 3 ;
  public static final int MASK_BREAK_INTERRUPT = 1 << 4;
  public static final int MASK_EMPTY_TRANSMITTER_HOLDING_REGISTER = 1 << 5;
  public static final int MASK_EMPTY_DATA_HOLDING_REGISTER = 1 << 6;
  public static final int MASK_ERROR_IN_RECEIVED_FIFO = 1 << 7;


  // Martin's abbreviations for the constants
  
  public static final int MASK_TDRE =  0x020; // this is MASK_EMPTY_TRANSMITTER_HOLDING_REGISTER
  public static final int MASK_RDRF = 0x01; // this is MASK_DATA_READY
    
  //aliases
  public volatile int data; // base + 0
  public volatile int status; // base + 5, Line Status Register


  //names by spec
  
  public volatile int rbr; // Receive Buffer Register, base
  public volatile int thr; // Transmitter Holding Register, base
  public volatile int dll; // Divisor Latch LSB, base
  public volatile int ier; // Interrupt Enable Register, base + 0x1
  public volatile int dlm; // Divisor Latch MSB, base + 0x1
  public volatile int fcr; // FIFO Control Register, base + 0x2
  public volatile int iir; // Interrupt Identification Register, base + 0x2
  public volatile int lcr; // Line Control Register, base + 0x3
  public volatile int mcr; // Modem Control Register, base + 0x4
  public volatile int lsr; // Line Status Register, base + 0x5
  public volatile int msr; // Modem Status Register, base + 0x6

  public void write(int ch) {
    data = ch;
  }
  
  public int read() {
    return data;
  }
  
}
