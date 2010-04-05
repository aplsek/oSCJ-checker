package edu.purdue.scjtck.tck;

import javax.realtime.Clock;
import javax.realtime.RealtimeThread;
import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.Services;
import javax.safetycritical.StorageConfigurationParameters;
import javax.safetycritical.annotate.Level;

/**
 * Level 0
 * 
 * - Test classes CyclicExecutive and CyclicSchecule to make sure all frames in
 * a mission are issued sequentially according to the order of their creations
 * 
 * - Only one server thread of control shall be provided by the RTVM; The
 * handlers shall be executed non preemptively on Level 0
 * 
 * - Synchronized code is not allowed to self suspend on Level 0 and 1. An
 * IllegalMonitorStateException is thrown if this constraint is violated
 * 
 */
public class TestSchedule403 extends TestCase {

    private CyclicExecutive ce = new MyCyclicExecutive();

    class MyCyclicExecutive extends CyclicExecutive {

        public MyCyclicExecutive() {
            super(new StorageConfigurationParameters(0, 0, 0));
        }

        private int _preFrame;
        private int _counter;
        private long _rtthreadID = -1;
        private volatile long _now;
        private final int _nFrames = 5;
        private final int _nHandlersPerFrame = 3;
        private final RelativeTime _perFrameDuration = new RelativeTime(100, 0);

        public void initialize() {
            for (int i = 1; i <= _nFrames; i++) {

                final int theFrame = i;

                for (int j = 0; j < _nHandlersPerFrame; j++) {

                    final int theHandler = j;

                    new GeneralPeriodicEventHandler() {
                        public void handleEvent() {
                            long snapshot = _now = Clock.getRealtimeClock()
                                    .getTime().getNanoseconds();

                            // make sure all handlers are served by only one
                            // real-time thread
                            if (_rtthreadID == -1)
                                _rtthreadID = RealtimeThread
                                        .currentRealtimeThread().getId();
                            else if (_rtthreadID != RealtimeThread
                                    .currentRealtimeThread().getId())
                                fail("More than one realtime server thread running");

                            // make sure frames are issued sequentially
                            if (theHandler == 0)
                                if (_preFrame == 0
                                        || theFrame == _preFrame % _nFrames + 1)
                                    _preFrame = theFrame;
                                else
                                    fail("Frames not executed sequentially");

                            // ---- self suspension issue ----

                            MyLock lock = new MyLock();

                            try {
                                lock.aSelfSuspendedSyncMethod();
                                if (_prop._level != Level.LEVEL_2)
                                    fail("Self-suspension illegally allowed in synchronized code on Level 0 and 1");
                            } catch (IllegalMonitorStateException e) {
                                // aSelfSuspendedSyncMethod() is self-suspended
                                // via sleep(); exception expected
                            }

                            try {
                                lock.aNonSelfSuspendedSyncMethod();
                            } catch (IllegalMonitorStateException e) {
                                fail("Requesting a lock (via the synchronized method) should not be considered self-suspension");
                                /*
                                 * aNonSelfSuspendedSyncMethod() just requires
                                 * lock (via sync methods), not considered as
                                 * self-suspended method
                                 */
                            } catch (Throwable t) {
                                fail(t.getMessage());
                            }

                            // make sure no preemption (by our own threads)
                            // if there is a preempter, _now would be set to its
                            // start time
                            if (snapshot != _now)
                                fail("Level 0 execution illegally preempted");

                            if (++_counter >= _nFrames * _nHandlersPerFrame)
                                requestSequenceTermination();
                        }
                    };
                }
            }
        }

        public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
            CyclicSchedule.Frame[] frames = new CyclicSchedule.Frame[_nFrames];

            for (int i = 0; i < _nFrames; i++) {
                PeriodicEventHandler[] PEHs = new PeriodicEventHandler[_nHandlersPerFrame];
                for (int j = 0; j < _nHandlersPerFrame; j++) {
                    PEHs[j] = handlers[(i * _nHandlersPerFrame) + j];
                }
                frames[i] = new CyclicSchedule.Frame(_perFrameDuration, PEHs);
            }

            return new CyclicSchedule(frames);
        }

        public long missionMemorySize() {
            return _nFrames * _nHandlersPerFrame * 5000;
        }

        public void setup() {
        }

        public void teardown() {
        }

        public Level getLevel() {
            return _prop._level;
        }

        @Override
        protected void cleanup() {
            _launcher.interrupt();
        }

    }

    public MissionSequencer getSequencer() {
        return ce.getSequencer();
    }

    public Level getLevel() {
        return ce.getLevel();
    }

    class MyLock {
        public synchronized void aSelfSuspendedSyncMethod() {
            try {
                Services.sleep(new RelativeTime(10, 0));
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }
        }

        public synchronized void aNonSelfSuspendedSyncMethod() {
            enterThisMonitor();
        }

        private synchronized void enterThisMonitor() {
            // to enter this monitor
        }
    }
}
