package javax.safetycritical;

import java.util.Iterator;

import javax.realtime.AbsoluteTime;
import javax.realtime.Clock;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.PeriodicEventHandler;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.SCJAllowed;

// (annotations turned off to work with Java 1.4) @SCJAllowed()
public abstract class CyclicExecutive extends Mission implements Safelet {

    private MissionSequencer _sequencer;
    private static RelativeTime _ZERO = new RelativeTime(0, 0);

// (annotations turned off to work with Java 1.4)     @SCJAllowed()
    public CyclicExecutive(StorageConfigurationParameters storage) {
        int priority = PriorityScheduler.instance().getNormPriority();
        _sequencer = new SingleMissionSequencer(
                new PriorityParameters(priority), storage, this);
    }

// (annotations turned off to work with Java 1.4)     @SCJAllowed()
    public abstract CyclicSchedule getSchedule(PeriodicEventHandler[] peh);

// (annotations turned off to work with Java 1.4)     @SCJAllowed()
    public MissionSequencer getSequencer() {
        return _sequencer;
    }

    protected void exec(MissionManager manager) {
        PeriodicEventHandler[] handlers = new PeriodicEventHandler[manager._peHandlers
                .size()];
        
        /* Added by vhs for Java 1.4 support */
        int counter = 0;
        for (Iterator i = manager._peHandlers.iterator(); i.hasNext();) {
            handlers[counter] = (PeriodicEventHandler) i.next();
            counter++;
        }
        CyclicSchedule schedule = getSchedule(handlers);

        /* Removed by vhs for Java 1.4 support
        Removed by vhs for Java 1.4 support          
        CyclicSchedule schedule = getSchedule(manager._peHandlers
                .toArray(handlers));
         */
 
        CyclicSchedule.Frame[] frames = schedule.getFrames();
        AbsoluteTime end = Clock.getRealtimeClock().getTime();
        RelativeTime delta = new RelativeTime();
        while (_phase == Mission.Phase.EXECUTE)
            /* Added by vhs for Java 1.4 support. */
            for (int i = 0; i < frames.length; i++) {
                end.add(frames[i].getDuration(), end);
                for (int j = 0; j < frames[i].getHandlers().length; j++) {
                    frames[i].getHandlers()[j].handleAsyncEvent();
                }
                waitForNextFrame(end, delta);
            }

            
          /* Removed by vhs for Java 1.4 support          
          for (CyclicSchedule.Frame frame : frames) {
              end.add(frame.getDuration(), end);
              for (PeriodicEventHandler handler : frame.getHandlers())
                  handler.handleAsyncEvent();
              waitForNextFrame(end, delta);
          }
           */
    }

    private void waitForNextFrame(AbsoluteTime end, RelativeTime delta) {
        int comparision = end.relative(Clock.getRealtimeClock(), delta)
                .compareTo(_ZERO);
        if (comparision > 0)
            Services.sleepNonInterruptable(delta);
        else if (comparision < 0) {
            // TODO: Overrun!
        }
    }
}
