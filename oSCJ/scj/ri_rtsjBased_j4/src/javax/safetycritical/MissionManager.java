package javax.safetycritical;

import java.util.ArrayList;
import java.util.List;
//import java.util.Collection;
import java.util.Iterator;

import javax.realtime.RealtimeThread;
import javax.safetycritical.AperiodicEventHandler;
import javax.safetycritical.ExternalEvent;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PeriodicEventHandler;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.SCJAllowed;

// (annotations turned off to work with Java 1.4) @SCJAllowed
// public abstract class MissionManager {
public class MissionManager {

    Mission _mission;

    /* FIXME: we probably need a real-time collection implementation */

    /* Added by vhs for Java 1.4 support */
    List _NHRTs = new ArrayList();
    List _eEvents = new ArrayList();
    List _intHandlers = new ArrayList();
    List _aEvents = new ArrayList();
    List _aeHandlers = new ArrayList();
    List _peHandlers = new ArrayList();
    List _sequencers = new ArrayList();
    
    /* Removed by vhs for Java 1.4 support
    Collection<ManagedThread> _NHRTs = new ArrayList<ManagedThread>();
    Collection<ExternalEvent> _eEvents = new ArrayList<ExternalEvent>();
    Collection<InterruptHandler> _intHandlers = new ArrayList<InterruptHandler>();
    Collection<AperiodicEvent> _aEvents = new ArrayList<AperiodicEvent>();
    Collection<AperiodicEventHandler> _aeHandlers = new ArrayList<AperiodicEventHandler>();
    Collection<PeriodicEventHandler> _peHandlers = new ArrayList<PeriodicEventHandler>();
    Collection<MissionSequencer> _sequencers = new ArrayList<MissionSequencer>();
     */

    public MissionManager(Mission mission) {
        _mission = mission;
    }

    public Mission getMission() {
        return _mission;
    }

    void startAll() {
        /* Added by vhs for Java 1.4 support */
        for (Iterator i = _peHandlers.iterator(); i.hasNext();) {
            PeriodicEventHandler peh = (PeriodicEventHandler) i.next();
            peh.start();
        }
        
        for (Iterator i = _eEvents.iterator(); i.hasNext();) {
            ExternalEvent event = (ExternalEvent) i.next();
            event.start();
        }
        
        /* Removed by vhs for Java 1.4 support
        for (PeriodicEventHandler peh : _peHandlers)
            peh.start();

        for (ExternalEvent event : _eEvents)
            event.start();
         */

        // TODO: interrupt handler? What else?
    }

    void cleanAll() {
        // stop all submissions
        
        /* Added by vhs for Java 1.4 support */
        for (Iterator i = _sequencers.iterator(); i.hasNext();) {
            MissionSequencer sequencer = (MissionSequencer) i.next();
            sequencer.requestTermination();
            sequencer.waitForTermination();
        }
        for (Iterator i = _peHandlers.iterator(); i.hasNext();) {
            PeriodicEventHandler peh = (PeriodicEventHandler) i.next();
            peh.cleanup();
            peh.join();
        }
        for (Iterator i = _eEvents.iterator(); i.hasNext();) {
            ExternalEvent event = (ExternalEvent) i.next();
            event.cleanup();
        }
        for (Iterator i = _aEvents.iterator(); i.hasNext();) {
            AperiodicEvent event = (AperiodicEvent) i.next();
            event.cleanup();
        }
        for (Iterator i = _aeHandlers.iterator(); i.hasNext();) {
            AperiodicEventHandler aeh = (AperiodicEventHandler) i.next();
            aeh.cleanup();
            aeh.join();
        }

        /* Removed by vhs for Java 1.4 support
        for (MissionSequencer sequencer : _sequencers) {
            sequencer.requestTermination();
            sequencer.waitForTermination();
        }
        for (PeriodicEventHandler peh : _peHandlers) {
            peh.cleanup();
            peh.join();
        }
        for (ExternalEvent event : _eEvents)
            event.cleanup();
        for (AperiodicEvent event : _aEvents)
            event.cleanup();
        for (AperiodicEventHandler aeh : _aeHandlers) {
            aeh.cleanup();
            aeh.join();
        }
         */
    }

     void addInterruptHandler(InterruptHandler handler) {
        _intHandlers.add(handler);
    }

    void addExternalEvent(ExternalEvent event) {
        _eEvents.add(event);
    }

    void addEventHandler(ManagedEventHandler handler) {
        if (handler instanceof PeriodicEventHandler)
            _peHandlers.add((PeriodicEventHandler) handler);
        else
            _aeHandlers.add((AperiodicEventHandler) handler);
    }

    void addAperiodicEvent(AperiodicEvent event) {
        _aEvents.add(event);
    }

    void addMissionSequencer(MissionSequencer sequencer) {
        _sequencers.add(sequencer);
    }

    static MissionManager getCurrentMissionManager() {
        return ((ManagedMemory) RealtimeThread.getCurrentMemoryArea())
                .getManager();
    }
}
