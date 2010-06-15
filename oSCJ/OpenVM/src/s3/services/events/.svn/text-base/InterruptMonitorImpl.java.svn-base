
package s3.services.events;

import ovm.core.execution.Native;
import ovm.core.execution.NativeInterface;
import ovm.core.services.events.EventManager;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.core.services.threads.OVMThread;
import ovm.core.stitcher.EventServicesFactory;
import ovm.core.stitcher.IOServiceConfigurator;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.ThreadServicesFactory;
import ovm.services.ServiceInstanceImpl;
import ovm.services.events.InterruptMonitor;
import ovm.services.threads.UserLevelThreadManager;
import ovm.util.OVMError;
import s3.util.PragmaNoPollcheck;

public class InterruptMonitorImpl extends ServiceInstanceImpl
    implements InterruptMonitor, EventManager.EventProcessor {

    // this constant is copied in intmonitor.c, and there is a runtime check 
    public static final int MAX_INTERRUPT_INDEX = 15; 
    
    /** Native interface helper class */
    private static final class InterruptHelper implements NativeInterface {
      
        static native void initInterruptMonitor();
        static native void shutdownInterruptMonitor();

        static native int getPendingInterrupts( int[] pendingInterrupts, int arrayLength );
        static native int getPendingInterrupt();
        
        static native void interruptServed( int interruptIndex );
        static native void interruptServingStarted( int interruptIndex );
        
        static native void disableLocalInterrupts();
        static native void enableLocalInterrupts();
        static native void disableInterrupt(int interruptIndex);
        static native void enableInterrupt(int interruptIndex);
                
        static native boolean startMonitoringInterrupt( int interruptIndex );
        static native boolean stopMonitoringInterrupt( int interruptIndex );
        
        static native boolean isMonitoredInterrupt( int interruptIndex );
        
        static native void interruptsSetEnabled( boolean enabled ); /* this is to disable events ~ pollchecks */
        
        static native int getMaxInterruptIndex();
        
        static native void resignalInterrupt();
    }
    
    private final static InterruptMonitor instance = new InterruptMonitorImpl();
    
    public static InterruptMonitor getInstance() {
        return instance;
    }
    
    /**
     * Are we started?
     */
    protected volatile boolean started = false;

    /**
     * Are we stopped?
     */
    protected volatile boolean stopped = false;

    /** 
     * Reference to the thread manager being used 
     */
    protected UserLevelThreadManager tm;

    /** 
     * Reference to the event manager being used 
     */
    protected EventManager em;

    private Runnable[] handlers;
    private OVMThread[] waitingThreads;
    
    /* the thread interrupted when handler thread was prepared */
    private OVMThread interruptedThread;
    

    public String eventProcessorShortName() {
	return "intmon";
    }


    public InterruptMonitorImpl() {

        handlers = new Runnable[MAX_INTERRUPT_INDEX+1];
        waitingThreads = new OVMThread[MAX_INTERRUPT_INDEX+1];
        
        for (int i=0; i < handlers.length ; i++) {
          handlers[i] = null;
          waitingThreads[i] = null;
        }

        interruptedThread = null;        
    }

    private void processPendingInterrupts() throws PragmaNoPollcheck {
    
      while( InterruptHelper.getPendingInterrupt() != -1 ) {
        eventFired();
      }
    
    }

    public void eventFired() throws PragmaNoPollcheck {

      assert interruptedThread == null : "eventFired called when some thread is already interrupted - error in disabling events/interrupts";
      int pendingInterrupt = InterruptHelper.getPendingInterrupt();

      if (pendingInterrupt == -1) return;
            
      Runnable h = handlers[pendingInterrupt];
        
      if (h!=null) {

        h.run();        
      } else {

        OVMThread t = waitingThreads[pendingInterrupt];
        if ( t!=null ) {

          boolean enabled = tm.setReschedulingEnabled(false);                
          
          try {
            assert interruptedThread == null : "some thread was already interrupted";
            
            OVMThread current = tm.getCurrentThread();
            
            if (tm.isReady(current)) {
              interruptedThread = current;
              tm.removeReady(current);
            }

            waitingThreads[pendingInterrupt] = null;
            t.setInterruptHandlerFlag(true);
            
            tm.makeReady(t);
            
            
            /* we have to do this here and not in interrupt monitor .c, because
               we needed the interrupt part of pollcheck word clear to get here
            */
            InterruptHelper.interruptsSetEnabled(false);
            
            tm.runNextThread();

            /* reached after the waiting handler thread wakes up */

          } finally {
            tm.setReschedulingEnabled(enabled);
          }


        } else {
            // This can mean that some interrupt is being monitored,
            // but no-one cares, which is probably an error.
            // However, it can also be a normal case, when user space
            // code has not yet reached the call to waitForInterrupt. A
            // particular instance of this could be when user code handler
            // is running to act on in fact previous instance of this interrupt.
            // (yes, this can be triggered for an interrupt being already 
            // processed)
            
//            InterruptHelper.resignalInterrupt();
        }        
      }
    }

    // TODO: fix assumption that the caller is really the serving thread
    public void interruptServed( int interruptIndex ) throws PragmaNoBarriers, PragmaNoPollcheck {

      InterruptHelper.interruptServed(interruptIndex);
      enableLocalInterrupts();
      tm.getCurrentThread().setInterruptHandlerFlag(false);
    }

    public void interruptServingStarted( int interruptIndex ) {
      InterruptHelper.interruptServingStarted(interruptIndex);
      tm.getCurrentThread().setInterruptHandlerFlag(true); // we need it here again because of RTSJ's AEH
    }
    
    public boolean isMonitoredInterrupt( int interruptIndex ) {
      return InterruptHelper.isMonitoredInterrupt( interruptIndex );
    }

    public boolean startMonitoringInterrupt( int interruptIndex ) {
      return InterruptHelper.startMonitoringInterrupt(interruptIndex);
    }

    public boolean stopMonitoringInterrupt( int interruptIndex ) throws PragmaNoBarriers, PragmaNoPollcheck {
    
      if (!InterruptHelper.stopMonitoringInterrupt(interruptIndex)) {
        return false;
      }

      /* now, the interrupt would not become pending */
      
      processPendingInterrupts();  // eventFired would probably be enough,
                                   // but this does not hurt, either
      
      /* now the interrupt is not pending */
      
      /* making sure that waitForInterrupt returns, if still waiting for the interrupt */
      
      boolean enabled = tm.setReschedulingEnabled(false);          
      
      try {

        OVMThread t = waitingThreads[interruptIndex];
        
        if (t!=null) {        

          waitingThreads[interruptIndex] = null;
          t.setInterruptHandlerFlag(false);
          OVMThread current = tm.getCurrentThread();
          tm.makeReady(t);
        } 
        
      } finally {
        tm.setReschedulingEnabled(enabled);
      }
      
      return true;
    }
    
    public Runnable registerInterruptHandler(Runnable handler, int interruptIndex) throws PragmaNoPollcheck, PragmaNoBarriers {
    
      Runnable previous = handlers[interruptIndex];
      
      handlers[interruptIndex] = handler;
      if (previous == null) {
        InterruptHelper.startMonitoringInterrupt( interruptIndex );
      }

      return previous;
    }

    public Runnable unregisterInterruptHandler(int interruptIndex) throws PragmaNoPollcheck, PragmaNoBarriers {
    
      Runnable previous = handlers[interruptIndex];
      
      if (previous != null) {
        InterruptHelper.stopMonitoringInterrupt( interruptIndex );
        handlers[interruptIndex] = null;
      }
      
      return previous;
    }

    // monitoring should already be activated by user code,
    // if it converts this semantics to user-level handlers, as 
    // there otherwise might be race-conditions and lost interrupts
    // (i.e. user code would register its handler, write to I/O ports 
    // incorrectly assuming that interrupts were already being caught by
    // OVM)
    
    public boolean waitForInterrupt(int interruptIndex) throws PragmaNoBarriers, PragmaNoPollcheck {
    
        assert waitingThreads[interruptIndex] == null : "waitForInterrupt, some thread is already waiting for the interrupt";
        assert handlers[interruptIndex] == null :"waitForInterrupt, some handler is already waiting for the interrupt";
        
        if (InterruptHelper.getPendingInterrupt() == interruptIndex) {
          return true;
        }
    
        boolean enabled = tm.setReschedulingEnabled(false);
        
        try {


/*
          if ((waitingThreads[interruptIndex]!=null) || (handlers[interruptIndex]!=null) ) {
            // there is already a thread waiting for this interrupt or an installed handler
            return false;
          }
*/          
          OVMThread current = tm.getCurrentThread();
          waitingThreads[interruptIndex] = current;
          InterruptHelper.startMonitoringInterrupt( interruptIndex ); /* does nothing if already being monitored */

          tm.removeReady(current);
          tm.runNextThread();

          /* woken up */
          
          /* make the interrupted thread ready to run (it will let us run, because we have the interrupt 
             handler flag set */
        
          if (interruptedThread != null) {
            tm.setReschedulingEnabled(false); 
            tm.makeReady(interruptedThread);
          }
          
          interruptedThread = null;          
          
          if (!current.getInterruptHandlerFlag()) {
            /* interrupted wait because stopMonitoringInterrupt was called */
            return false;
          } 
          
        } finally {
          tm.setReschedulingEnabled(enabled);
        }
      
        // reached on interrupt or shutdown of monitoring        
        return true;
    }


    /** 
     * Initialisation of the signal manager simply involves grabbing
     * a thread manager and event manager.
     */
    public void init() {
        tm = (UserLevelThreadManager)
            ((ThreadServicesFactory)ThreadServiceConfigurator.config.
             getServiceFactory(ThreadServicesFactory.name)).getThreadManager();
        if (tm == null) {
            throw new OVMError.Configuration("need a configured thread manager");
        }

        em = ((EventServicesFactory)IOServiceConfigurator.config.
         getServiceFactory(EventServicesFactory.name)).getEventManager();
        if (em == null) {
            throw new OVMError.Configuration("need a configured event manager");
        }

        isInited = true;
    }


    public void disableLocalInterrupts() throws PragmaNoPollcheck {

      InterruptHelper.interruptsSetEnabled(false); /* disable pollchecks */

      processPendingInterrupts(); 
                    /* make sure threads potentially waiting for pending
                     * interrupt get to the ready queue this is important ;
                     * there can be a pending interrupt, we do not want to
                     * receive it _after_ returning from this call */
                     
      InterruptHelper.disableLocalInterrupts();
    }
    
    public void enableLocalInterrupts() throws PragmaNoPollcheck {

      InterruptHelper.interruptsSetEnabled(true);  /* enable pollchecks */
      InterruptHelper.enableLocalInterrupts();    
      eventFired(); /* just an optimization */
    }

    public void disableInterrupt(int interruptIndex) throws PragmaNoPollcheck {

    
      processPendingInterrupts(); 
                    /* make sure threads potentially waiting for pending
                     * interrupt get to the ready queue ; this is important,
                     * there can be this interrupt pending, and we do not
                     * want to receive it _after_ returning from this call
                     * */
      InterruptHelper.disableInterrupt(interruptIndex);
    }

    public void enableInterrupt(int interruptIndex) throws PragmaNoPollcheck {
    
      InterruptHelper.enableInterrupt(interruptIndex);    
      eventFired(); /* make sure threads potentially waiting for pending interrupt get to the ready queue 
                     * here it is I think only optimization */    
    }
    
    public int getMaxInterruptIndex() {
      return InterruptHelper.getMaxInterruptIndex();
    }
    
    public void start() {
        if (started) {
            throw new OVMError.IllegalState("interrupt monitor already started");
        }
        
        if (InterruptHelper.getMaxInterruptIndex() != MAX_INTERRUPT_INDEX) {
          throw new OVMError.IllegalState("interrupt index ranges in C and Java code do not match");
        }
        
        InterruptHelper.initInterruptMonitor();

        em.addEventProcessor(this);

        started = true;
        d("InterruptMonitor has been started");
    }

    public void stop() {
        if (!started) {
            return;
        }
        
        for (int i=0; i < MAX_INTERRUPT_INDEX; i++) {
          stopMonitoringInterrupt(i);  /* it does not matter if the interrupt is not really monitored */
        }
        
        em.removeEventProcessor(this);
        InterruptHelper.shutdownInterruptMonitor();
        stopped = true;
    }

    public boolean isRunning() {
        return started & !stopped;
    }

    public void destroy() {
        if (isRunning()) {
            throw new OVMError.IllegalState("must stop interrupt monitor first");
        }
    }
    
}

