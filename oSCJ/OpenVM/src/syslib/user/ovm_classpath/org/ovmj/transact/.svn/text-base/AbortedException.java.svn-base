package org.ovmj.transact;
/**
 * This exception is reserved for use in Atomic codes. 
 * @author janvitek
 */
public final class AbortedException extends RuntimeException {
  // FIXME: replicate fields of RuntimeException.
  // Use long rather than int for 64-bit saftey.  Use long rather than
  // pointer types to save memory.
  private long cause;
  private long stackTrace;
  private long vmState;
  
  public  AbortedException() {}
}
