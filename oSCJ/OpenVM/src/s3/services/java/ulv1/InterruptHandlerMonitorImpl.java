
package s3.services.java.ulv1;

import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.services.java.JavaMonitor;
import ovm.services.monitors.Monitor;
import ovm.services.events.InterruptMonitor; 
import s3.util.PragmaAtomic;
import s3.util.PragmaNoPollcheck;
import s3.core.domain.S3Domain;

public class InterruptHandlerMonitorImpl extends JavaMonitorImpl implements JavaMonitor {
    
    protected int interruptIndex;
    protected InterruptMonitor intMon;
    
    public InterruptHandlerMonitorImpl( int interruptIndex, InterruptMonitor intMon ) {
      super();
      this.interruptIndex = interruptIndex;
      this.intMon = intMon;
    }

    /**
     *  Returns the actual size of an instance of this class, including the
     *  space needed for the object header and all fields, plus the space
     *  needed for creating referenced objects (and transitively the space
     *  they need to create referenced objects) during construction.
     */
    static int sizeOf() {
        return 
            S3Domain.sizeOfInstance("s3/services/java/ulv1/InterruptHandlerMonitorImpl")
            + constructionSizeOf();
    }


    /**
     * Returns the maximum space allocated during the execution of the
     * constructor of an instance of this class, and transitively the space
     * needed by any object allocation performed in this constructor.
     * Note this doesn't include "temporary" allocations like debug strings
     * etc, but it does include super constructors. Hence for any class the
     * total space needed to do "new" is the base size plus the construction
     * size.
     */
    protected static int constructionSizeOf() {
        // there is no additional allocation in this class so just return
        // whatever our super class construction requirements are
        return JavaMonitorImpl.constructionSizeOf();
    }

    
    public void enter() throws PragmaNoPollcheck, PragmaNoBarriers {

//      BasicIO.out.println("In IH monitor, enter");
      JavaOVMThreadImpl current = (JavaOVMThreadImpl)jtm.getCurrentThread();
      
      if (current.getInterruptHandlerFlag()) {
//        BasicIO.out.println("IH monitor, in interrupt handling, ignoring...");
        return ;
      }
      
      if (entryCount == 0) {
        intMon.disableInterrupt(interruptIndex);
      }
      entryCount++;
             
    }
    
    public void exit()  {
      exitInner();
    }
    
    protected void exitInner() throws PragmaNoPollcheck, PragmaNoBarriers {
    
//      BasicIO.out.println("In IH monitor, exit");
      JavaOVMThreadImpl current = (JavaOVMThreadImpl)jtm.getCurrentThread();      
      
      if (current.getInterruptHandlerFlag()) {
//        BasicIO.out.println("IH monitor, in interrupt handling, ignoring...");
//        return ;
      }
      
      if (entryCount==1) {
        intMon.enableInterrupt(interruptIndex);
      }
      
      entryCount --;
    }
    
    public boolean waitAbortable(Monitor ignored) {
      throw new RuntimeException("Operation not supported.");
    }
    
    public int waitTimedAbortable(Monitor ignored, long timeout) {
      throw new RuntimeException("Operation not supported.");
    }
    
    public void signal() {
      throw new RuntimeException("Operation not supported.");
    }
    
    public void signalAll() {
      throw new RuntimeException("Operation not supported.");
    }
    
    public int setInterruptIndex(int interruptIndex) {

      int previous = this.interruptIndex;
      this.interruptIndex = interruptIndex;
      return previous;
    }
    
}

