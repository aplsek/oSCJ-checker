package edu.purdue.scjtck;

import javax.realtime.AperiodicParameters;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.PriorityScheduler;
import javax.realtime.RealtimeThread;
import javax.realtime.RelativeTime;
import javax.safetycritical.AperiodicEventHandler;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.ManagedThread;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.Safelet;
import javax.safetycritical.SingleMissionSequencer;
import javax.safetycritical.StorageConfigurationParameters;
import javax.safetycritical.Terminal;
import javax.safetycritical.annotate.Level;

import edu.purdue.scj.PropFileReader;

public abstract class MainSafelet implements Safelet {

    protected Properties _prop = new Properties();

    protected Thread _launcher;

    /* Parameters generated from properties */
    protected PriorityParameters _priorityParam;
    protected AperiodicParameters _aperiodicParam;
    protected PeriodicParameters _periodicParam;
    protected StorageConfigurationParameters _storageParam;

    /* ----------------- Methods ------------------- */

    public void setup() {
        _launcher = Thread.currentThread();
        _prop.parseArgs(PropFileReader.readAll());
        _priorityParam = new PriorityParameters(_prop._priority);
        _periodicParam = new PeriodicParameters(new RelativeTime(_prop._iDelay,
                0), new RelativeTime(_prop._period, 0));
        _aperiodicParam = new AperiodicParameters();
        _storageParam = new StorageConfigurationParameters(0, 0, 0);

        Terminal.getTerminal().writeln(getInfo());
    }

    public void teardown() {
        Terminal.getTerminal().writeln(report());
    }

    public Level getLevel() {
        return _prop._level;
    }

    protected abstract String getInfo();

    protected abstract String report();

    /* -------------- Wrapped Classes -------------- */
    public abstract class GeneralMission extends Mission {
        public long missionMemorySize() {
            return _prop._missionMemSize;
        }

        @Override
        protected void cleanup() {
            super.cleanup();
            _launcher.interrupt();
        }
    }

    public class GeneralSingleMissionSequencer extends SingleMissionSequencer {
        public GeneralSingleMissionSequencer(Mission mission) {
            super(new PriorityParameters(_prop._priority),
                    new StorageConfigurationParameters(0, 0, 0), mission);
        }
    }

    public abstract class GeneralMissionSequencer extends MissionSequencer {
        public GeneralMissionSequencer() {
            super(new PriorityParameters(_prop._priority),
                    new StorageConfigurationParameters(0, 0, 0));
        }
    }

    public abstract class GeneralPeriodicEventHandler extends
            PeriodicEventHandler {

        public GeneralPeriodicEventHandler() {
            super(new PriorityParameters(_prop._priority),
                    new PeriodicParameters(new RelativeTime(_prop._iDelay, 0),
                            new RelativeTime(_prop._period, 0)),
                    new StorageConfigurationParameters(0, 0, 0),
                    _prop._schedObjMemSize);
        }
    }

    public abstract class GeneralAperiodicEventHandler extends
            AperiodicEventHandler {

        public GeneralAperiodicEventHandler() {
            super(new PriorityParameters(_prop._priority),
                    new AperiodicParameters(),
                    new StorageConfigurationParameters(0, 0, 0),
                    _prop._schedObjMemSize);
        }

        public GeneralAperiodicEventHandler(String name) {
            super(new PriorityParameters(_prop._priority),
                    new AperiodicParameters(),
                    new StorageConfigurationParameters(0, 0, 0),
                    _prop._schedObjMemSize, name);
        }
    }

    public class GeneralManagedThread extends ManagedThread {

        public GeneralManagedThread() {
            super(new PriorityParameters(_prop._priority),
                    new StorageConfigurationParameters(0, 0, 0), null);
        }
    }

    public class Terminator extends PeriodicEventHandler {

        public Terminator() {
            super(new PriorityParameters(PriorityScheduler.instance()
                    .getMaxPriority()), new PeriodicParameters(
                    new RelativeTime(_prop._duration, 0), new RelativeTime(
                            Long.MAX_VALUE, 0)),
                    new StorageConfigurationParameters(0, 0, 0), 0);
        }

        @Override
        public void handleEvent() {
            ((ManagedMemory) RealtimeThread.getCurrentMemoryArea())
                    .getManager().getMission().requestSequenceTermination();
        }
    }
}
