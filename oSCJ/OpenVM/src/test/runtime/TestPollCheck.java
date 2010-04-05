
package test.runtime;

import ovm.core.execution.Native;
import ovm.core.services.timer.TimerInterruptAction;
import ovm.core.services.memory.*;
import s3.util.PragmaNoPollcheck;
import s3.util.PragmaNoInline;
import s3.util.PragmaInline;

/**
 * A test to see if POLLCHECK is doing anything.  Should fail if POLLCHECK
 * is not being executed or inserted.
 * <p>
 * Note that currently, only POLLCHECKs at back-branches are tested.  Should
 * really have a variant of this test that uses recursion.  That may be
 * difficult though...
 * @author Filip Pizlo
 */
public class TestPollCheck extends TestTimerBase {
    public TestPollCheck() {
        super("Poll Check");
    }
    
    final boolean[] cont=new boolean[1];

    public void runLoopInline(String msg) throws PragmaInline {
      cont[0]=true;
      int i=0;
      
      Native.generateTimerInterrupt();
      while (cont[0] && (i++ < 100) ); 
      
      check_condition( cont[0]==false, msg);
      
    }    

    public void runLoopNoInline(String msg) throws PragmaNoInline {
      cont[0]=true;
      int i=0;
      
      Native.generateTimerInterrupt();
      while (cont[0] && (i++ < 100) ); 
      
      check_condition( cont[0]==false, msg);
      
    }    
    
    public void runAtomicLoopNoInline(String msg) throws PragmaNoInline, PragmaNoPollcheck {
      cont[0]=true;
      int i=0;
      
      Native.generateTimerInterrupt();
      while (cont[0] && (i++ < 100) ); 
      
      check_condition( cont[0]==true, msg);
      
    }    

    public void runAtomicLoopInline(String msg) throws PragmaInline, PragmaNoPollcheck {
      cont[0]=true;
      int i=0;
      
      Native.generateTimerInterrupt();
      while (cont[0] && (i++ < 100) ); 
      
      check_condition( cont[0]==true, msg);
      
    }    
        
    public void testPollcheck1() throws PragmaNoInline, PragmaNoPollcheck {
      
      runLoopNoInline("Pollcheck missing in a loop (1).");
      runLoopInline("Pollcheck missing in a loop of a method inlined to no-pollcheck method");      
      runAtomicLoopInline("Pollcheck present in no-pollcheck method inlined into another no-pollcheck method.");
    }

    public void testPollcheck2() throws PragmaNoInline {
      
      runLoopNoInline("Pollcheck missing in a loop (2).");
      runLoopInline("Pollchedk missing in a loop of inlined method.");      
    }

    public void testPollcheck3() throws PragmaNoInline {
      runAtomicLoopNoInline("Pollcheck present in no-pollcheck method.");
      runAtomicLoopInline("Pollcheck present in no-pollcheck method inlined into method with pollchecks enabled.");
    }    

    public void run() {
        if (timerMan==null) {
            p(" SKIPPED: do not have a timer manager");
            return;
        }

        // do allocation in immortal in case this is a no-heap supporting
        // config and we're watching for heap accesses during event processing

        VM_Area old = MemoryManager.the().setCurrentArea(MemoryManager.the().getImmortalArea());
        try {
        
            TimerInterruptAction tia=new TimerInterruptAction(){
                    public void fire(int ticks) {
                        if (ticks<1) {
                            return;
                        }
                        cont[0]=false;
                    }
                    public String timerInterruptActionShortName() {
                        return "test";
                    }
                };
       
            timerMan.addTimerInterruptAction(tia);

            // FIXME: probably remove this test - as it causes the test process to hang
            
            cont[0]=true;
            while (cont[0]) ; // this loop will be infinite if the timer never fires,
                              // or, more likely, if the POLLCHECK does not work.        

            testPollcheck3();
            testPollcheck1();
            testPollcheck2();            
                        
            timerMan.removeTimerInterruptAction(tia);
        }
        finally {
            MemoryManager.the().setCurrentArea(old);
        }
    }
}

