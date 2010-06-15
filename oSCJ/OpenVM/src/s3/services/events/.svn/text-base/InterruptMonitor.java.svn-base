package ovm.services.events;

import ovm.services.ServiceInstance;
import ovm.util.OVMError;

public interface InterruptMonitor extends ServiceInstance {

  public Runnable registerInterruptHandler(Runnable handler, int interruptIndex);
  public Runnable unregisterInterruptHandler(int interruptIndex);

  public boolean waitForInterrupt(int interruptIndex);
  
  public void interruptServed(int interruptIndex);
  public void interruptServingStarted(int interruptIndex);
  
  public void disableLocalInterrupts();
  public void enableLocalInterrupts();
  public void disableInterrupt(int interruptIndex);
  public void enableInterrupt(int interruptIndex);

  public boolean startMonitoringInterrupt(int interruptIndex);
  public boolean stopMonitoringInterrupt(int interruptIndex);
  public boolean isMonitoredInterrupt(int interruptIndex);

  public int getMaxInterruptIndex();  
}  