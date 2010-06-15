
package javax.realtime;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.ovmj.hw.InterruptController;

public class InterruptAsyncEvent extends AsyncEvent {

    protected IRQDispatcher d = new IRQDispatcher();
    
    static class IRQDispatcher {
    
      protected IdentityArraySet[] events = null;
      protected Object eventsLock;
      
      protected IRQServer[] irqServers = null;
      protected int maxIRQ = -1;    
    
      protected synchronized void initialize() {

        if (events == null) {
          maxIRQ = InterruptController.getMaxInterruptIndex();
          events = new IdentityArraySet[maxIRQ];
          for(int i=0; i<maxIRQ;i++) {
            events[i] = new IdentityArraySet();
          }
          
          eventsLock = events;
          irqServers = new IRQServer[maxIRQ];
        }
      }
      
      private class IRQServer extends Thread {
      
        int irq;
        boolean shutdown = false;
        
        IRQServer(int irq) {
          this.irq = irq;
        }
        
        public void stopServing() {
          shutdown = true;
          InterruptController.stopMonitoringInterrupt(irq);
        }
        
        public void run() {
        
          if (!InterruptController.startMonitoringInterrupt(irq)) {
            throw new RuntimeException("Interrupt "+irq+" is already being monitored.");
          }
          
          for(;;) {
          
            boolean res = InterruptController.waitForInterrupt(irq);

            if (!res) {
              if (shutdown) return ;
              throw new RuntimeException("Interrupt handler for interrupt "+irq+" is already installed.\n");
            }
            
            synchronized( eventsLock ) {
              Iterator i = events[irq].iterator();

              if (shutdown) return ;
                          
              try {
                while (i.hasNext()) {
                  ((AsyncEvent)i.next()).fire();
                }
              } catch (NoSuchElementException ex) {
                throw new RuntimeException(ex);
              }
            }
          }
        }
      }

      public boolean addEvent( int irq, AsyncEvent event ) {
      
        initialize();

        synchronized(eventsLock) {
          boolean wasEmpty = events[irq].isEmpty();
          
          if (!events[irq].add(event)) {
            return false; // already added
          }
          
          if (wasEmpty) {
            irqServers[irq] = new IRQServer(irq);
            irqServers[irq].start();
          }
        }

        return true;
      }
      
      public boolean removeEvent( int irq, AsyncEvent event) {
        initialize(); 
        
        synchronized(eventsLock) {
          if (!events[irq].remove(event)) {
            return false; // already removed
          }     
        
          if (events[irq].isEmpty()) {
            irqServers[irq].stopServing();
          }
        }
        
        return true;
      }
    }

    protected int extractIRQFromHappening(String happening) {
    
      if (!happening.startsWith("INT")) {
        return -1;
      }
      
      try {
        int irq  = Integer.parseInt(happening.substring(3));
        
        if (irq >= 0) return irq;
        
      } catch (NumberFormatException e) {
        throw new RuntimeException("Invalid number after INT in happening string.");
      } catch (IndexOutOfBoundsException e) {
      }
      
      return -1;
    }

    public void bindTo(String happening) {
    
      if (happening == null) {
        throw new IllegalArgumentException();
      }
    
      int irq = extractIRQFromHappening(happening);
      
      if (irq == -1) {
        throw new UnknownHappeningException("Unsupported happening");
      }
      
      d.addEvent( irq, this );
    }
  
    public void unbindTo(String happening) {
    
      if (happening == null) {
        throw new IllegalArgumentException();
      }
      
      int irq = extractIRQFromHappening(happening);
      
      if (irq == -1) {
        throw new UnknownHappeningException("Unsupported happening, this event cannot be bound to it.");
      }
      
      if (!d.removeEvent( irq, this )) {
        throw new UnknownHappeningException("This event is not bound to the specified happening.");
      }
    }

}
